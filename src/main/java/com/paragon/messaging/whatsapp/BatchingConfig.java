package com.paragon.messaging.whatsapp;

import com.paragon.tts.TTSProvider;

import java.time.Duration;
import java.util.Optional;

/**
 * Configuração principal do {@link MessageBatchingService}.
 *
 * <p>Agrega todas configurações de batching, rate limiting, backpressure,
 * error handling, TTS e persistência.</p>
 *
 * <p><b>Exemplo Completo:</b></p>
 * <pre>{@code
 * BatchingConfig config = BatchingConfig.builder()
 *     .adaptiveTimeout(Duration.ofSeconds(5))
 *     .silenceThreshold(Duration.ofSeconds(2))
 *     .maxBufferSize(50)
 *     .rateLimitConfig(RateLimitConfig.lenient())
 *     .backpressureStrategy(BackpressureStrategy.DROP_OLDEST)
 *     .errorHandlingStrategy(ErrorHandlingStrategy.defaults())
 *     .messageStore(RedisMessageStore.create(redisClient))
 *     .ttsProvider(ElevenLabsTTSProvider.create(apiKey))
 *     .speechPlayChance(0.3)
 *     .build();
 * }</pre>
 *
 * @param adaptiveTimeout       timeout máximo de espera antes de processar
 * @param silenceThreshold      silêncio necessário para processar antes do timeout
 * @param maxBufferSize         tamanho máximo do buffer por usuário
 * @param rateLimitConfig       configuração de rate limiting
 * @param backpressureStrategy  estratégia quando buffer cheio
 * @param errorHandlingStrategy estratégia de retry e error handling
 * @param messageStore          store opcional para persistência e deduplicação
 * @param ttsProvider           provider opcional de text-to-speech
 * @param speechPlayChance      probabilidade de resposta em áudio (0.0-1.0)
 * @author Agentle Team
 * @since 1.0
 */
public record BatchingConfig(
        Duration adaptiveTimeout,
        Duration silenceThreshold,
        int maxBufferSize,
        RateLimitConfig rateLimitConfig,
        BackpressureStrategy backpressureStrategy,
        ErrorHandlingStrategy errorHandlingStrategy,
        Optional<MessageStore> messageStore,
        Optional<TTSProvider> ttsProvider,
        double speechPlayChance
) {

  public BatchingConfig {
    if (adaptiveTimeout == null || adaptiveTimeout.isNegative() || adaptiveTimeout.isZero()) {
      throw new IllegalArgumentException("adaptiveTimeout must be positive");
    }
    if (silenceThreshold == null || silenceThreshold.isNegative()) {
      throw new IllegalArgumentException("silenceThreshold must be non-negative");
    }
    if (maxBufferSize <= 0) {
      throw new IllegalArgumentException("maxBufferSize must be positive");
    }
    if (rateLimitConfig == null) {
      throw new IllegalArgumentException("rateLimitConfig cannot be null");
    }
    if (backpressureStrategy == null) {
      throw new IllegalArgumentException("backpressureStrategy cannot be null");
    }
    if (errorHandlingStrategy == null) {
      throw new IllegalArgumentException("errorHandlingStrategy cannot be null");
    }
    if (speechPlayChance < 0.0 || speechPlayChance > 1.0) {
      throw new IllegalArgumentException("speechPlayChance must be between 0.0 and 1.0");
    }
    if (silenceThreshold.compareTo(adaptiveTimeout) > 0) {
      throw new IllegalArgumentException("silenceThreshold cannot be greater than adaptiveTimeout");
    }

    // Garantir Optional
    if (messageStore == null) {
      messageStore = Optional.empty();
    }
    if (ttsProvider == null) {
      ttsProvider = Optional.empty();
    }
  }

  /**
   * Configuração padrão.
   *
   * <ul>
   *   <li>Timeout adaptativo: 5 segundos</li>
   *   <li>Silêncio: 2 segundos</li>
   *   <li>Buffer: 50 mensagens</li>
   *   <li>Rate limit: Leniente</li>
   *   <li>Backpressure: DROP_OLDEST</li>
   *   <li>Error handling: 3 retries com exponential backoff</li>
   *   <li>Sem TTS, sem persistência</li>
   * </ul>
   *
   * @return config padrão
   */
  public static BatchingConfig defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Duration adaptiveTimeout = Duration.ofSeconds(5);
    private Duration silenceThreshold = Duration.ofSeconds(2);
    private int maxBufferSize = 50;
    private RateLimitConfig rateLimitConfig = RateLimitConfig.lenient();
    private BackpressureStrategy backpressureStrategy = BackpressureStrategy.DROP_OLDEST;
    private ErrorHandlingStrategy errorHandlingStrategy = ErrorHandlingStrategy.defaults();
    private MessageStore messageStore;
    private TTSProvider ttsProvider;
    private double speechPlayChance = 0.0;

    /**
     * Define timeout máximo de espera antes de processar.
     *
     * <p>Mesmo que mensagens continuem chegando, após este tempo
     * o buffer é processado.</p>
     *
     * @param adaptiveTimeout timeout máximo
     * @return builder
     */
    public Builder adaptiveTimeout(Duration adaptiveTimeout) {
      this.adaptiveTimeout = adaptiveTimeout;
      return this;
    }

    /**
     * Define silêncio necessário para processar antes do timeout.
     *
     * <p>Se usuário para de enviar por este tempo, buffer é processado
     * imediatamente (não aguarda timeout completo).</p>
     *
     * @param silenceThreshold duração do silêncio
     * @return builder
     */
    public Builder silenceThreshold(Duration silenceThreshold) {
      this.silenceThreshold = silenceThreshold;
      return this;
    }

    /**
     * Define tamanho máximo do buffer por usuário.
     *
     * <p>Quando atingido, backpressureStrategy entra em ação.</p>
     *
     * @param maxBufferSize tamanho máximo
     * @return builder
     */
    public Builder maxBufferSize(int maxBufferSize) {
      this.maxBufferSize = maxBufferSize;
      return this;
    }

    /**
     * Define configuração de rate limiting.
     *
     * @param rateLimitConfig config de rate limit
     * @return builder
     */
    public Builder rateLimitConfig(RateLimitConfig rateLimitConfig) {
      this.rateLimitConfig = rateLimitConfig;
      return this;
    }

    /**
     * Define estratégia de backpressure.
     *
     * @param backpressureStrategy estratégia
     * @return builder
     */
    public Builder backpressureStrategy(BackpressureStrategy backpressureStrategy) {
      this.backpressureStrategy = backpressureStrategy;
      return this;
    }

    /**
     * Define estratégia de error handling.
     *
     * @param errorHandlingStrategy estratégia
     * @return builder
     */
    public Builder errorHandlingStrategy(ErrorHandlingStrategy errorHandlingStrategy) {
      this.errorHandlingStrategy = errorHandlingStrategy;
      return this;
    }

    /**
     * Define message store para persistência e deduplicação.
     *
     * @param messageStore store (pode ser null)
     * @return builder
     */
    public Builder messageStore(MessageStore messageStore) {
      this.messageStore = messageStore;
      return this;
    }

    /**
     * Define provider de TTS.
     *
     * @param ttsProvider provider (pode ser null)
     * @return builder
     */
    public Builder ttsProvider(TTSProvider ttsProvider) {
      this.ttsProvider = ttsProvider;
      return this;
    }

    /**
     * Define probabilidade de resposta em áudio (0.0-1.0).
     *
     * <p>Requer ttsProvider configurado.</p>
     *
     * @param speechPlayChance probabilidade (0.0 = nunca, 1.0 = sempre)
     * @return builder
     */
    public Builder speechPlayChance(double speechPlayChance) {
      this.speechPlayChance = speechPlayChance;
      return this;
    }

    public BatchingConfig build() {
      return new BatchingConfig(
              adaptiveTimeout,
              silenceThreshold,
              maxBufferSize,
              rateLimitConfig,
              backpressureStrategy,
              errorHandlingStrategy,
              Optional.ofNullable(messageStore),
              Optional.ofNullable(ttsProvider),
              speechPlayChance
      );
    }
  }
}