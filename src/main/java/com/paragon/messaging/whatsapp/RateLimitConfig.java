package com.paragon.messaging.whatsapp;

import java.time.Duration;

/**
 * Configuração para rate limiting híbrido (Token Bucket + Sliding Window).
 *
 * <p>Combina duas estratégias:</p>
 * <ul>
 *   <li><b>Token Bucket</b>: Permite bursts controlados (rate limiting suave)</li>
 *   <li><b>Sliding Window</b>: Anti-flood rígido (proteção contra spam)</li>
 * </ul>
 *
 * <p>Mensagem é aceita APENAS se AMBAS as estratégias permitirem.</p>
 *
 * <p><b>Exemplo Leniente:</b></p>
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.lenient();
 * // Token Bucket: 20 tokens/min, capacity 30
 * // Sliding Window: max 10 msgs em 30s
 * }</pre>
 *
 * <p><b>Exemplo Estrito:</b></p>
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.strict();
 * // Token Bucket: 10 tokens/min, capacity 15
 * // Sliding Window: max 5 msgs em 10s
 * }</pre>
 *
 * @param tokensPerMinute     taxa de reabastecimento de tokens (estado steady)
 * @param bucketCapacity      capacidade máxima do bucket (permite bursts)
 * @param maxMessagesInWindow máximo de mensagens na janela deslizante
 * @param slidingWindow       duração da janela deslizante
 * @author Agentle Team
 * @since 1.0
 */
public record RateLimitConfig(
        int tokensPerMinute,
        int bucketCapacity,
        int maxMessagesInWindow,
        Duration slidingWindow
) {

  public RateLimitConfig {
    if (tokensPerMinute <= 0) {
      throw new IllegalArgumentException("tokensPerMinute must be positive");
    }
    if (bucketCapacity <= 0) {
      throw new IllegalArgumentException("bucketCapacity must be positive");
    }
    if (maxMessagesInWindow <= 0) {
      throw new IllegalArgumentException("maxMessagesInWindow must be positive");
    }
    if (slidingWindow == null || slidingWindow.isNegative() || slidingWindow.isZero()) {
      throw new IllegalArgumentException("slidingWindow must be positive");
    }
  }

  /**
   * Configuração leniente (padrão).
   *
   * <p>Boa para maioria dos casos. Permite bursts moderados.</p>
   * <ul>
   *   <li>20 tokens/minuto</li>
   *   <li>Capacity 30 (50% extra para bursts)</li>
   *   <li>Max 10 msgs em 30 segundos</li>
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
   * <p>Para cenários sensíveis ou usuários problemáticos.</p>
   * <ul>
   *   <li>10 tokens/minuto</li>
   *   <li>Capacity 15</li>
   *   <li>Max 5 msgs em 10 segundos</li>
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
   * <p><b>Atenção:</b> Permite bursts muito altos.</p>
   * <ul>
   *   <li>60 tokens/minuto</li>
   *   <li>Capacity 100</li>
   *   <li>Max 30 msgs em 60 segundos</li>
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
   * <p><b>Aviso:</b> Use apenas para testes!</p>
   *
   * @return config sem limites
   */
  public static RateLimitConfig disabled() {
    return new RateLimitConfig(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Duration.ofDays(365)
    );
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int tokensPerMinute = 20;
    private int bucketCapacity = 30;
    private int maxMessagesInWindow = 10;
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
              tokensPerMinute,
              bucketCapacity,
              maxMessagesInWindow,
              slidingWindow
      );
    }
  }
}