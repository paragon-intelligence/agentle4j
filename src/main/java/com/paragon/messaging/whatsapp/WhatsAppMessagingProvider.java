package com.paragon.messaging.whatsapp;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Implementação do provedor WhatsApp Business Cloud API usando Virtual Threads.
 *
 * <p><b>Virtual Thread Best Practices Aplicadas:</b></p>
 * <ul>
 *   <li>Código síncrono/bloqueante - sem CompletableFuture</li>
 *   <li>Rate limiting via Semaphore (limita recursos, não threads)</li>
 *   <li>OkHttpClient injetado para reutilização de connection pool</li>
 *   <li>Validação com Bean Validation</li>
 * </ul>
 *
 * <p><b>Uso recomendado:</b></p>
 * <pre>{@code
 * // Criar uma vez, reutilizar
 * OkHttpClient httpClient = new OkHttpClient.Builder()
 *     .connectTimeout(Duration.ofSeconds(10))
 *     .readTimeout(Duration.ofSeconds(30))
 *     .build();
 *
 * MessagingProvider provider = new WhatsAppMessagingProvider(
 *     phoneNumberId,
 *     accessToken,
 *     httpClient
 * );
 *
 * // Usar em virtual threads
 * try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
 *     for (Recipient recipient : recipients) {
 *         executor.submit(() -> {
 *             MessageResponse r = provider.sendMessage(recipient, message);
 *             process(r);
 *         });
 *     }
 * }
 * }</pre>
 *
 * @author Your Name
 * @since 2.0
 */
public class WhatsAppMessagingProvider implements MessagingProvider {

  private static final String PROVIDER_TYPE = "WHATSAPP_CLOUD";
  private static final String API_VERSION = "v21.0";
  private static final String BASE_URL = "https://graph.facebook.com/" + API_VERSION;
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  /**
   * Validador compartilhado (thread-safe, criado uma vez).
   */
  private static final Validator VALIDATOR = createValidator();

  /**
   * Semaphore para rate limiting de conexões HTTP.
   *
   * <p><b>Virtual Thread Best Practice:</b> Não limite a criação de virtual threads.
   * Em vez disso, limite o recurso escasso (conexões HTTP à API do WhatsApp).</p>
   *
   * <p>WhatsApp Cloud API limites:</p>
   * <ul>
   *   <li>Padrão: 80 mensagens/segundo</li>
   *   <li>Após escala: 1,000 mensagens/segundo</li>
   *   <li>Pair rate limit: ~10 mensagens/minuto por destinatário</li>
   * </ul>
   *
   * <p>Este semaphore limita conexões simultâneas, não throughput total.
   * Ajuste baseado na sua tier e infraestrutura.</p>
   */
  private final Semaphore httpRateLimiter;

  private final String phoneNumberId;
  private final String accessToken;
  private final OkHttpClient httpClient;

  /**
   * Construtor com rate limiting customizável.
   *
   * @param phoneNumberId         ID do número de telefone WhatsApp Business
   * @param accessToken           token de acesso permanente (System User Token)
   * @param httpClient            cliente HTTP configurado (injetado)
   * @param maxConcurrentRequests máximo de requisições HTTP simultâneas (padrão: 80)
   */
  public WhatsAppMessagingProvider(
          String phoneNumberId,
          String accessToken,
          OkHttpClient httpClient,
          int maxConcurrentRequests) {

    this.phoneNumberId = Objects.requireNonNull(phoneNumberId, "Phone number ID cannot be null");
    this.accessToken = Objects.requireNonNull(accessToken, "Access token cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "OkHttpClient cannot be null");

    if (maxConcurrentRequests <= 0) {
      throw new IllegalArgumentException("maxConcurrentRequests must be positive");
    }

    this.httpRateLimiter = new Semaphore(maxConcurrentRequests);
  }

  /**
   * Construtor com rate limiting padrão (80 requisições simultâneas).
   *
   * @param phoneNumberId ID do número de telefone
   * @param accessToken   token de acesso
   * @param httpClient    cliente HTTP configurado
   */
  public WhatsAppMessagingProvider(
          String phoneNumberId,
          String accessToken,
          OkHttpClient httpClient) {
    this(phoneNumberId, accessToken, httpClient, 80);
  }

  /**
   * Factory method para criar OkHttpClient com configurações recomendadas.
   *
   * <p><b>Virtual Thread Optimization:</b> OkHttpClient usa seu próprio connection pool
   * e dispatcher. Configuração otimizada para uso com virtual threads.</p>
   */
  public static OkHttpClient createDefaultHttpClient() {
    return new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Connection pool para reutilização
            .connectionPool(new ConnectionPool(
                    50,  // max idle connections
                    5,   // keep alive duration
                    TimeUnit.MINUTES
            ))
            // Retry automático desabilitado (controlamos manualmente)
            .retryOnConnectionFailure(false)
            .build();
  }

  /**
   * Cria validador Bean Validation (compartilhado, thread-safe).
   */
  private static Validator createValidator() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }

  @Override
  public String getProviderType() {
    return PROVIDER_TYPE;
  }

  @Override
  public boolean isConfigured() {
    return !phoneNumberId.isBlank() && !accessToken.isBlank();
  }

  @Override
  public MessageResponse sendMessage(
          @Valid Recipient recipient,
          @Valid Message message) throws MessagingException {

    // Validar inputs
    validateInput(recipient);
    validateInput(message);

    // Validar que é número de telefone
    if (recipient.type() != Recipient.RecipientType.PHONE_NUMBER) {
      throw new MessagingException("WhatsApp only supports phone numbers");
    }

    // Adquirir permissão do rate limiter
    // IMPORTANTE: Isto BLOQUEIA a virtual thread até uma permissão estar disponível
    // A virtual thread será desmontada do carrier thread enquanto espera
    try {
      httpRateLimiter.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MessagingException("Interrupted while waiting for rate limit", e);
    }

    try {
      // Criar payload JSON
      String jsonPayload = createPayload(recipient, message);

      // Criar requisição HTTP
      Request request = new Request.Builder()
              .url(BASE_URL + "/" + phoneNumberId + "/messages")
              .addHeader("Authorization", "Bearer " + accessToken)
              .post(RequestBody.create(jsonPayload, JSON))
              .build();

      // Enviar requisição (BLOQUEIA a virtual thread - isso é OK!)
      // OkHttp faz I/O bloqueante, perfeito para virtual threads
      try (Response response = httpClient.newCall(request).execute()) {
        return parseResponse(response);
      }

    } catch (IOException e) {
      throw new MessagingException("HTTP request failed: " + e.getMessage(), e);
    } finally {
      // SEMPRE liberar o semaphore
      httpRateLimiter.release();
    }
  }

  /**
   * Valida um objeto usando Bean Validation (thread-safe).
   */
  private <T> void validateInput(T object) throws MessagingException {
    var violations = VALIDATOR.validate(object);
    if (!violations.isEmpty()) {
      String errors = violations.stream()
              .map(v -> v.getPropertyPath() + ": " + v.getMessage())
              .reduce((a, b) -> a + "; " + b)
              .orElse("Validation failed");
      throw new MessagingException("Validation error: " + errors);
    }
  }

  /**
   * Cria o payload JSON baseado no tipo de mensagem.
   */
  private String createPayload(Recipient recipient, Message message) {
    String to = recipient.identifier();

    return switch (message) {
      case TextMessage text -> String.format("""
              {
                "messaging_product": "whatsapp",
                "to": "%s",
                "type": "text",
                "text": {
                  "body": "%s",
                  "preview_url": %b
                }
              }
              """, escapeJson(to), escapeJson(text.body()), text.previewUrl());

      case MediaMessage.Image img -> {
        String sourceField = img.source() instanceof MediaMessage.MediaSource.Url url
                ? "\"link\": \"" + escapeJson(url.url()) + "\""
                : "\"id\": \"" + escapeJson(((MediaMessage.MediaSource.MediaId) img.source()).id()) + "\"";

        String captionField = img.caption()
                .map(c -> ",\n        \"caption\": \"" + escapeJson(c) + "\"")
                .orElse("");

        yield String.format("""
                {
                  "messaging_product": "whatsapp",
                  "to": "%s",
                  "type": "image",
                  "image": {
                    %s%s
                  }
                }
                """, escapeJson(to), sourceField, captionField);
      }

      case LocationMessage loc -> {
        String nameField = loc.name()
                .map(n -> ",\n        \"name\": \"" + escapeJson(n) + "\"")
                .orElse("");
        String addressField = loc.address()
                .map(a -> ",\n        \"address\": \"" + escapeJson(a) + "\"")
                .orElse("");

        yield String.format("""
                        {
                          "messaging_product": "whatsapp",
                          "to": "%s",
                          "type": "location",
                          "location": {
                            "latitude": %f,
                            "longitude": %f%s%s
                          }
                        }
                        """, escapeJson(to), loc.latitude(), loc.longitude(),
                nameField, addressField);
      }

      case TemplateMessage tmpl -> String.format("""
                      {
                        "messaging_product": "whatsapp",
                        "to": "%s",
                        "type": "template",
                        "template": {
                          "name": "%s",
                          "language": {"code": "%s"}
                        }
                      }
                      """, escapeJson(to), escapeJson(tmpl.name()),
              escapeJson(tmpl.languageCode()));

      default -> throw new IllegalArgumentException(
              "Unsupported message type: " + message.getClass().getSimpleName()
      );
    };
  }

  /**
   * Processa a resposta HTTP.
   */
  private MessageResponse parseResponse(Response response) throws MessagingException {
    int statusCode = response.code();
    String body;

    try {
      ResponseBody responseBody = response.body();
      body = responseBody != null ? responseBody.string() : "";
    } catch (IOException e) {
      throw new MessagingException("Failed to read response body", e);
    }

    if (statusCode >= 200 && statusCode < 300) {
      // Extrair message ID (parsing simplificado)
      String messageId = extractMessageId(body);
      return new MessageResponse(
              messageId,
              MessageResponse.MessageStatus.SENT,
              Instant.now()
      );
    } else {
      throw new MessagingException(
              "API error: " + statusCode + " - " + body,
              statusCode
      );
    }
  }

  /**
   * Extrai message ID da resposta (parsing simplificado).
   */
  private String extractMessageId(String responseBody) {
    // Parsing JSON simplificado (em produção use Jackson/Gson)
    int idStart = responseBody.indexOf("\"id\":\"") + 6;
    if (idStart > 5) {
      int idEnd = responseBody.indexOf("\"", idStart);
      if (idEnd > idStart) {
        return responseBody.substring(idStart, idEnd);
      }
    }
    return "unknown_id";
  }

  /**
   * Escapa caracteres especiais para JSON.
   */
  private String escapeJson(String value) {
    if (value == null) return "";
    return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
  }

  /**
   * Retorna estatísticas de uso do rate limiter (útil para monitoramento).
   */
  public RateLimiterStats getRateLimiterStats() {
    return new RateLimiterStats(
            httpRateLimiter.availablePermits(),
            httpRateLimiter.getQueueLength()
    );
  }

  /**
   * Estatísticas do rate limiter.
   */
  public record RateLimiterStats(int availablePermits, int queuedThreads) {
    /**
     * Verifica se o rate limiter está sob pressão.
     */
    public boolean isUnderPressure() {
      return queuedThreads > 0 || availablePermits == 0;
    }
  }
}
