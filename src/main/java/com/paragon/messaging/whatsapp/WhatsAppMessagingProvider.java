package com.paragon.messaging.whatsapp;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import okhttp3.*;

import com.paragon.messaging.core.MessagingException;
import com.paragon.messaging.core.MessagingProvider;
import com.paragon.messaging.core.MessageResponse;
import com.paragon.messaging.core.OutboundMessage;
import com.paragon.messaging.core.Recipient;
import com.paragon.messaging.whatsapp.config.WhatsAppConfig;
import com.paragon.messaging.whatsapp.messages.*;

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
 * @author Your Name
 * @since 2.0
 */
public class WhatsAppMessagingProvider implements MessagingProvider {

  private static final String API_VERSION = "v22.0";
  private static final String BASE_URL = "https://graph.facebook.com/" + API_VERSION;
  private static final MediaType JSON = MediaType.get("application/json");

  /**
   * Validador compartilhado (thread-safe, criado uma vez).
   */
  private static final Validator VALIDATOR = createValidator();

  private final Semaphore httpRateLimiter;
  private final String phoneNumberId;
  private final String accessToken;
  private final OkHttpClient httpClient;
  private final String baseUrl;
  private final WhatsAppMessageSerializer serializer = new WhatsAppMessageSerializer();

  /**
   * Constructor from WhatsAppConfig.
   */
  public WhatsAppMessagingProvider(WhatsAppConfig config) {
    Objects.requireNonNull(config, "WhatsAppConfig cannot be null");
    this.phoneNumberId = config.phoneNumberId();
    this.accessToken = config.accessToken();
    this.baseUrl = config.apiBaseUrl() + "/" + config.apiVersion();
    this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(config.requestTimeout(), TimeUnit.MILLISECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(false)
            .build();
    this.httpRateLimiter = new Semaphore(config.maxConcurrentRequests());
  }

  /**
   * Construtor com rate limiting customizável.
   */
  public WhatsAppMessagingProvider(
          String phoneNumberId,
          String accessToken,
          OkHttpClient httpClient,
          int maxConcurrentRequests) {

    this.phoneNumberId = Objects.requireNonNull(phoneNumberId, "Phone number ID cannot be null");
    this.accessToken = Objects.requireNonNull(accessToken, "Access token cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "OkHttpClient cannot be null");
    this.baseUrl = BASE_URL;

    if (maxConcurrentRequests <= 0) {
      throw new IllegalArgumentException("maxConcurrentRequests must be positive");
    }

    this.httpRateLimiter = new Semaphore(maxConcurrentRequests);
  }

  /**
   * Construtor com rate limiting padrão (80 requisições simultâneas).
   */
  public WhatsAppMessagingProvider(
          String phoneNumberId,
          String accessToken,
          OkHttpClient httpClient) {
    this(phoneNumberId, accessToken, httpClient, 80);
  }

  /**
   * Factory method para criar OkHttpClient com configurações recomendadas.
   */
  public static OkHttpClient createDefaultHttpClient() {
    return new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(false)
            .build();
  }

  private static Validator createValidator() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }

  @Override
  public ProviderType getProviderType() {
    return ProviderType.WHATSAPP;
  }

  @Override
  public boolean isConfigured() {
    return !phoneNumberId.isBlank() && !accessToken.isBlank();
  }

  @Override
  public MessageResponse sendMessage(
          @Valid Recipient recipient,
          @Valid OutboundMessage message) throws MessagingException {

    // Validar inputs
    validateInput(recipient);
    validateInput(message);

    // Validar que é número de telefone
    if (!recipient.isPhoneNumber() && !recipient.isUserId()) {
      throw new MessagingException("WhatsApp only supports phone numbers and WhatsApp IDs");
    }

    // Adquirir permissão do rate limiter
    try {
      httpRateLimiter.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MessagingException("Interrupted while waiting for rate limit", e);
    }

    try {
      // Criar payload JSON via serializer
      String jsonPayload = serializer.serialize(recipient, message);

      // Criar requisição HTTP
      Request request = new Request.Builder()
              .url(baseUrl + "/" + phoneNumberId + "/messages")
              .addHeader("Authorization", "Bearer " + accessToken)
              .post(RequestBody.create(jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8), JSON))
              .build();

      // Enviar requisição
      try (Response response = httpClient.newCall(request).execute()) {
        return parseResponse(response);
      }

    } catch (IOException e) {
      return new MessageResponse(null, MessageResponse.MessageStatus.FAILED, Instant.now(), "HTTP request failed: " + e.getMessage());
    } finally {
      httpRateLimiter.release();
    }
  }

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
   * Processa a resposta HTTP.
   */
  private MessageResponse parseResponse(Response response) {
    int statusCode = response.code();
    String body;

    try {
      ResponseBody responseBody = response.body();
      body = responseBody != null ? responseBody.string() : "";
    } catch (IOException e) {
      return new MessageResponse(null, MessageResponse.MessageStatus.FAILED, Instant.now(), "Failed to read response: " + e.getMessage());
    }

    if (statusCode >= 200 && statusCode < 300) {
      String messageId = extractMessageId(body);
      return new MessageResponse(messageId, MessageResponse.MessageStatus.SENT, Instant.now());
    } else {
      return new MessageResponse(null, MessageResponse.MessageStatus.FAILED, Instant.now(), "API error: " + statusCode + " - " + body);
    }
  }

  /**
   * Extrai message ID da resposta (parsing simplificado).
   */
  private String extractMessageId(String responseBody) {
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
   * Retorna estatísticas de uso do rate limiter.
   */
  public RateLimiterStats getRateLimiterStats() {
    return new RateLimiterStats(
            httpRateLimiter.availablePermits(),
            httpRateLimiter.getQueueLength()
    );
  }

  public record RateLimiterStats(int availablePermits, int queuedThreads) {
    public boolean isUnderPressure() {
      return queuedThreads > 0 || availablePermits == 0;
    }
  }
}
