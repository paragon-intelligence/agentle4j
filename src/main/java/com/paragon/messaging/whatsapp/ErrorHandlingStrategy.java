package com.paragon.messaging.whatsapp;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Configuração para tratamento de erros durante processamento.
 *
 * <p>Controla retry, backoff exponencial, dead letter queue e notificações
 * ao usuário quando processamento falha.</p>
 *
 * <p><b>Exemplo com Retry:</b></p>
 * <pre>{@code
 * ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
 *     .maxRetries(3)
 *     .retryDelay(Duration.ofSeconds(2))
 *     .exponentialBackoff(true)
 *     .notifyUserOnFailure(true)
 *     .build();
 * }</pre>
 *
 * <p><b>Exemplo com DLQ:</b></p>
 * <pre>{@code
 * ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
 *     .maxRetries(0)
 *     .deadLetterHandler((userId, messages) -> {
 *         dlqService.store(userId, messages);
 *     })
 *     .notifyUserOnFailure(true)
 *     .build();
 * }</pre>
 *
 * @param maxRetries              número máximo de tentativas de retry (0 = sem retry)
 * @param retryDelay              delay inicial entre retries
 * @param exponentialBackoff      se usa backoff exponencial (2^n * retryDelay)
 * @param notifyUserOnFailure     se envia mensagem de erro ao usuário
 * @param userNotificationMessage mensagem customizada de erro (opcional)
 * @param deadLetterHandler       handler para mensagens que falharam permanentemente
 * @author Agentle Team
 * @since 1.0
 */
public record ErrorHandlingStrategy(
        int maxRetries,
        Duration retryDelay,
        boolean exponentialBackoff,
        boolean notifyUserOnFailure,
        Optional<String> userNotificationMessage,
        Optional<BiConsumer<String, List<Message>>> deadLetterHandler
) {

  public ErrorHandlingStrategy {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries cannot be negative");
    }
    if (retryDelay == null || retryDelay.isNegative()) {
      throw new IllegalArgumentException("retryDelay must be non-negative");
    }
    if (userNotificationMessage == null) {
      userNotificationMessage = Optional.empty();
    }
    if (deadLetterHandler == null) {
      deadLetterHandler = Optional.empty();
    }
  }

  /**
   * Estratégia padrão.
   *
   * <ul>
   *   <li>3 retries com exponential backoff</li>
   *   <li>2 segundos de delay inicial</li>
   *   <li>Notifica usuário em falha permanente</li>
   * </ul>
   *
   * @return estratégia padrão
   */
  public static ErrorHandlingStrategy defaults() {
    return builder()
            .maxRetries(3)
            .retryDelay(Duration.ofSeconds(2))
            .exponentialBackoff(true)
            .notifyUserOnFailure(true)
            .build();
  }

  /**
   * Sem retry (fail fast).
   *
   * @return estratégia sem retry
   */
  public static ErrorHandlingStrategy noRetry() {
    return builder()
            .maxRetries(0)
            .notifyUserOnFailure(true)
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Calcula delay para tentativa específica.
   *
   * @param attemptNumber número da tentativa (1-based)
   * @return duração do delay
   */
  public Duration calculateDelay(int attemptNumber) {
    if (attemptNumber <= 0) {
      return Duration.ZERO;
    }

    if (exponentialBackoff) {
      // 2^(attempt-1) * retryDelay
      // Tentativa 1: 1x delay
      // Tentativa 2: 2x delay
      // Tentativa 3: 4x delay
      long multiplier = (long) Math.pow(2, attemptNumber - 1);
      return retryDelay.multipliedBy(multiplier);
    } else {
      return retryDelay;
    }
  }

  /**
   * Retorna mensagem de notificação ou padrão.
   *
   * @return mensagem de notificação
   */
  public String getNotificationMessage() {
    return userNotificationMessage.orElse(
            "Desculpe, estou com dificuldades técnicas. Por favor, tente novamente mais tarde."
    );
  }

  /**
   * Verifica se tem handler de dead letter queue configurado.
   *
   * @return true se DLQ handler está presente
   */
  public boolean hasDLQHandler() {
    return deadLetterHandler.isPresent();
  }

  public static class Builder {
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(2);
    private boolean exponentialBackoff = true;
    private boolean notifyUserOnFailure = true;
    private String userNotificationMessage;
    private BiConsumer<String, List<Message>> deadLetterHandler;

    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder retryDelay(Duration retryDelay) {
      this.retryDelay = retryDelay;
      return this;
    }

    public Builder exponentialBackoff(boolean exponentialBackoff) {
      this.exponentialBackoff = exponentialBackoff;
      return this;
    }

    public Builder notifyUserOnFailure(boolean notifyUserOnFailure) {
      this.notifyUserOnFailure = notifyUserOnFailure;
      return this;
    }

    public Builder userNotificationMessage(String message) {
      this.userNotificationMessage = message;
      return this;
    }

    public Builder deadLetterHandler(
            BiConsumer<String, List<Message>> handler) {
      this.deadLetterHandler = handler;
      return this;
    }

    public ErrorHandlingStrategy build() {
      return new ErrorHandlingStrategy(
              maxRetries,
              retryDelay,
              exponentialBackoff,
              notifyUserOnFailure,
              Optional.ofNullable(userNotificationMessage),
              Optional.ofNullable(deadLetterHandler)
      );
    }
  }
}
