package com.paragon.messaging.ratelimit;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter híbrido: Token Bucket + Sliding Window.
 *
 * <p>Combina duas estratégias:
 *
 * <ul>
 *   <li><b>Token Bucket:</b> Rate limiting suave com bursts controlados
 *   <li><b>Sliding Window:</b> Anti-flood rígido
 * </ul>
 *
 * <p>Mensagem é aceita APENAS se AMBAS estratégias permitirem.
 *
 * <p><b>Token Bucket:</b> Tokens são reabastecidos continuamente. Permite bursts até capacidade
 * máxima.
 *
 * <p><b>Sliding Window:</b> Conta mensagens em janela deslizante. Bloqueia spam agressivo.
 *
 * @author Agentle Team
 * @since 1.0
 */
public class HybridRateLimiter {

  private final TokenBucket tokenBucket;
  private final SlidingWindow slidingWindow;

  public HybridRateLimiter(RateLimitConfig config) {
    this.tokenBucket = new TokenBucket(config.tokensPerMinute(), config.bucketCapacity());
    this.slidingWindow =
        new SlidingWindow(config.maxMessagesInWindow(), config.slidingWindow().toMillis());
  }

  /**
   * Tenta adquirir permissão para processar mensagem.
   *
   * @return true se ambos (token bucket E sliding window) permitirem
   */
  public boolean tryAcquire() {
    return tokenBucket.tryConsume() && slidingWindow.tryRecord();
  }

  /**
   * Token Bucket para rate limiting suave.
   *
   * <p>Tokens são reabastecidos a uma taxa constante (tokensPerMinute). Bucket tem capacidade
   * máxima que permite bursts.
   */
  private static class TokenBucket {
    private final int tokensPerMinute;
    private final int capacity;
    private final AtomicInteger tokens;
    private final AtomicLong lastRefillTime;

    TokenBucket(int tokensPerMinute, int capacity) {
      this.tokensPerMinute = tokensPerMinute;
      this.capacity = capacity;
      this.tokens = new AtomicInteger(capacity);
      this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }

    boolean tryConsume() {
      refill();
      return tokens.getAndUpdate(t -> t > 0 ? t - 1 : 0) > 0;
    }

    private synchronized void refill() {
      long now = System.currentTimeMillis();
      long lastRefill = lastRefillTime.get();
      long elapsed = now - lastRefill;

      if (elapsed > 0) {
        // Calculate tokens to add based on elapsed time
        int tokensToAdd = (int) (elapsed * tokensPerMinute / 60_000);

        if (tokensToAdd > 0) {
          // Update timestamp first to prevent other threads from adding the same tokens
          lastRefillTime.set(now);
          // Add tokens, respecting capacity
          tokens.updateAndGet(t -> Math.min(capacity, t + tokensToAdd));
        }
      }
    }
  }

  /**
   * Sliding Window para anti-flood rígido.
   *
   * <p>Mantém timestamps de mensagens recentes. Remove timestamps fora da janela antes de verificar
   * se pode aceitar nova mensagem.
   */
  private static class SlidingWindow {
    private final int maxMessages;
    private final long windowMillis;
    private final ConcurrentLinkedQueue<Long> timestamps;

    SlidingWindow(int maxMessages, long windowMillis) {
      this.maxMessages = maxMessages;
      this.windowMillis = windowMillis;
      this.timestamps = new ConcurrentLinkedQueue<>();
    }

    boolean tryRecord() {
      long now = System.currentTimeMillis();
      long cutoff = now - windowMillis;

      // Remove timestamps antigos (fora da janela)
      timestamps.removeIf(t -> t < cutoff);

      // Verifica se pode adicionar nova mensagem
      if (timestamps.size() < maxMessages) {
        timestamps.offer(now);
        return true;
      }

      return false;
    }
  }
}
