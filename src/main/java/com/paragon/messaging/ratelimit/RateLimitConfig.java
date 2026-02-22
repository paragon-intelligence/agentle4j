package com.paragon.messaging.ratelimit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * Configuração para rate limiting híbrido (Token Bucket + Sliding Window).
 *
 * <p>Combina duas estratégias:
 *
 * <ul>
 *   <li><b>Token Bucket</b>: Permite bursts controlados (rate limiting suave)
 *   <li><b>Sliding Window</b>: Anti-flood rígido (proteção contra spam)
 * </ul>
 *
 * <p>Mensagem é aceita APENAS se AMBAS as estratégias permitirem.
 *
 * <p><b>Exemplo Leniente:</b>
 *
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.lenient();
 * // Token Bucket: 20 tokens/min, capacity 30
 * // Sliding Window: max 10 msgs em 30s
 * }</pre>
 *
 * <p><b>Exemplo Estrito:</b>
 *
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.strict();
 * // Token Bucket: 10 tokens/min, capacity 15
 * // Sliding Window: max 5 msgs em 10s
 * }</pre>
 *
 * @param tokensPerMinute taxa de reabastecimento de tokens (estado steady)
 * @param bucketCapacity capacidade máxima do bucket (permite bursts)
 * @param maxMessagesInWindow máximo de mensagens na janela deslizante
 * @param slidingWindow duração da janela deslizante
 * @author Agentle Team
 * @since 1.0
 */
public record RateLimitConfig(
    @Positive(message = "Tokens per minute must be positive")
        @Min(value = 1, message = "Tokens per minute must be at least 1")
        @Max(value = 10000, message = "Tokens per minute cannot exceed 10,000")
        int tokensPerMinute,
    @Positive(message = "Bucket capacity must be positive")
        @Min(value = 1, message = "Bucket capacity must be at least 1")
        @Max(value = 1000, message = "Bucket capacity cannot exceed 1,000")
        int bucketCapacity,
    @Positive(message = "Max messages in window must be positive")
        @Min(value = 1, message = "Max messages must be at least 1")
        @Max(value = 10000, message = "Max messages cannot exceed 10,000")
        int maxMessagesInWindow,
    @NotNull(message = "Sliding window duration cannot be null") Duration slidingWindow) {

  public RateLimitConfig {
    if (slidingWindow == null) {
      throw new IllegalArgumentException("slidingWindow cannot be null");
    }
  }

  /**
   * Configuração leniente (padrão).
   *
   * <p>Boa para maioria dos casos. Permite bursts moderados.
   *
   * <ul>
   *   <li>20 tokens/minuto
   *   <li>Capacity 30 (50% extra para bursts)
   *   <li>Max 10 msgs em 30 segundos
   * </ul>
   *
   * @return config leniente
   */
  public static RateLimitConfig lenient() {
    return new RateLimitConfig(20, 30, 10, Duration.ofSeconds(30));
  }

  /**
   * Configuração estrita.
   *
   * <p>Para cenários sensíveis ou usuários problemáticos.
   *
   * <ul>
   *   <li>10 tokens/minuto
   *   <li>Capacity 15
   *   <li>Max 5 msgs em 10 segundos
   * </ul>
   *
   * @return config estrita
   */
  public static RateLimitConfig strict() {
    return new RateLimitConfig(10, 15, 5, Duration.ofSeconds(10));
  }

  /**
   * Configuração permissiva (para testes ou usuários VIP).
   *
   * <p><b>Atenção:</b> Permite bursts muito altos.
   *
   * <ul>
   *   <li>60 tokens/minuto
   *   <li>Capacity 100
   *   <li>Max 30 msgs em 60 segundos
   * </ul>
   *
   * @return config permissiva
   */
  public static RateLimitConfig permissive() {
    return new RateLimitConfig(60, 100, 30, Duration.ofSeconds(60));
  }

  /**
   * Desabilita rate limiting (sem limites).
   *
   * <p><b>Aviso:</b> Use apenas para testes!
   *
   * @return config sem limites
   */
  public static RateLimitConfig disabled() {
    return new RateLimitConfig(
        Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Duration.ofDays(365));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int tokensPerMinute = 20;
    private int bucketCapacity = 30;
    private int maxMessagesInWindow = 100;
    private Duration slidingWindow = Duration.ofSeconds(30);

    public Builder tokensPerMinute(int tokensPerMinute) {
      this.tokensPerMinute = tokensPerMinute;
      return this;
    }

    public Builder bucketCapacity(int bucketCapacity) {
      this.bucketCapacity = bucketCapacity;
      return this;
    }

    public Builder maxMessagesInWindow(int maxMessagesInWindow) {
      this.maxMessagesInWindow = maxMessagesInWindow;
      return this;
    }

    public Builder slidingWindow(Duration slidingWindow) {
      this.slidingWindow = slidingWindow;
      return this;
    }

    public RateLimitConfig build() {
      return new RateLimitConfig(
          tokensPerMinute, bucketCapacity, maxMessagesInWindow, slidingWindow);
    }
  }
}
