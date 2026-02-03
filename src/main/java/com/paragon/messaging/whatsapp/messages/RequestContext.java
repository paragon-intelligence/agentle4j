package com.paragon.messaging.whatsapp.messages;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Contexto de requisição usando ScopedValues (Java 25).
 *
 * <p><b>Por que ScopedValues em vez de ThreadLocal?</b></p>
 * <ul>
 *   <li>ThreadLocal com milhões de virtual threads = explosão de memória</li>
 *   <li>ScopedValues são imutáveis e automaticamente limpos</li>
 *   <li>Integração nativa com Structured Concurrency</li>
 *   <li>Herdados automaticamente por subtasks</li>
 * </ul>
 *
 * <p><b>Uso:</b></p>
 * <pre>{@code
 * // Definir contexto para a requisição
 * var context = new RequestContext("user-123", "api-key-456");
 *
 * ScopedValue.where(RequestContext.CURRENT, context)
 *     .run(() -> {
 *         // Tudo dentro deste scope tem acesso ao contexto
 *         processRequest();
 *
 *         // Até tarefas paralelas herdam o contexto!
 *         try (var scope = StructuredTaskScope.open(...)) {
 *             scope.fork(() -> {
 *                 var ctx = RequestContext.get();
 *                 sendMessage(ctx.userId());
 *             });
 *         }
 *     });
 * }</pre>
 *
 * @author Arthur Brenno
 * @since 2.0
 */
public record RequestContext(
        String requestId,
        String userId,
        String apiKey,
        Instant timestamp,
        String clientIp,
        String userAgent
) {

  /**
   * ScopedValue para o contexto da requisição atual.
   *
   * <p>ScopedValues são thread-safe, imutáveis e automaticamente limpos.
   * Diferente de ThreadLocal, não causam memory leaks com virtual threads.</p>
   */
  public static final ScopedValue<RequestContext> CURRENT = ScopedValue.newInstance();

  /**
   * Construtor compacto com valores padrão.
   */
  public RequestContext {
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }
    if (clientIp == null) {
      clientIp = "unknown";
    }
    if (userAgent == null) {
      userAgent = "unknown";
    }
  }

  /**
   * Construtor conveniente apenas com userId e apiKey.
   */
  public RequestContext(String userId, String apiKey) {
    this(null, userId, apiKey, null, null, null);
  }

  /**
   * Obtém o contexto da requisição atual.
   *
   * @return contexto atual
   * @throws IllegalStateException se não houver contexto definido
   */
  public static RequestContext get() {
    return CURRENT.get();
  }

  /**
   * Obtém o contexto da requisição atual, ou Optional.empty() se não houver.
   *
   * @return contexto opcional
   */
  public static Optional<RequestContext> getOptional() {
    return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
  }

  /**
   * Verifica se há um contexto ativo.
   *
   * @return true se há contexto
   */
  public static boolean isPresent() {
    return CURRENT.isBound();
  }

  /**
   * Executa uma operação dentro de um contexto.
   *
   * @param context   contexto a ser usado
   * @param operation operação a executar
   */
  public static void runInContext(RequestContext context, Runnable operation) {
    ScopedValue.where(CURRENT, context).run(operation);
  }

  /**
   * Executa uma operação dentro de um contexto, retornando um valor.
   *
   * @param context   contexto a ser usado
   * @param operation operação a executar
   * @param <T>       tipo do retorno
   * @return resultado da operação
   */
}