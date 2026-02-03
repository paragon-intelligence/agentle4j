package com.paragon.messaging.batching;

import com.paragon.messaging.security.SecurityConfig;
import com.paragon.messaging.whatsapp.config.TTSConfig;
import org.jspecify.annotations.NonNull;
import com.paragon.messaging.ratelimit.RateLimitConfig;
import com.paragon.messaging.error.ErrorHandlingStrategy;
import com.paragon.messaging.store.MessageStore;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Main configuration for {@link MessageBatchingService}.
 *
 * <p>Aggregates all configurations for batching, rate limiting, backpressure,
 * error handling, TTS, security, and persistence.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * BatchingConfig config = BatchingConfig.builder()
 *     .adaptiveTimeout(Duration.ofSeconds(5))
 *     .silenceThreshold(Duration.ofSeconds(2))
 *     .maxBufferSize(50)
 *     .rateLimitConfig(RateLimitConfig.lenient())
 *     .backpressureStrategy(BackpressureStrategy.DROP_OLDEST)
 *     .errorHandlingStrategy(ErrorHandlingStrategy.defaults())
 *     .messageStore(RedisMessageStore.create(redisClient))
 *     .ttsConfig(TTSConfig.builder()
 *         .provider(elevenLabsProvider)
 *         .speechChance(0.3)
 *         .build())
 *     .securityConfig(SecurityConfig.strict("verify-token", "app-secret"))
 *     .build();
 * }</pre>
 *
 * @param adaptiveTimeout       Maximum wait time before processing
 * @param silenceThreshold      Silence needed to process before timeout
 * @param maxBufferSize         Maximum buffer size per user
 * @param rateLimitConfig       Rate limiting configuration
 * @param backpressureStrategy  Strategy when buffer is full
 * @param errorHandlingStrategy Retry and error handling strategy
 * @param messageStore          Optional store for persistence and deduplication
 * @param ttsConfig             Text-to-speech configuration
 * @param securityConfig        Optional security configuration
 * @author Agentle Team
 * @since 2.1
 */
public record BatchingConfig(
        @NonNull Duration adaptiveTimeout,
        @NonNull Duration silenceThreshold,
        int maxBufferSize,
        @NonNull RateLimitConfig rateLimitConfig,
        @NonNull BackpressureStrategy backpressureStrategy,
        @NonNull ErrorHandlingStrategy errorHandlingStrategy,
        @Nullable MessageStore messageStore,
        @NonNull TTSConfig ttsConfig,
        @Nullable SecurityConfig securityConfig
) {

    /**
     * Canonical constructor with validation.
     */
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
        // rateLimitConfig is @NonNull, so no null check needed
        if (backpressureStrategy == null) {
            throw new IllegalArgumentException("backpressureStrategy cannot be null");
        }
        if (errorHandlingStrategy == null) {
            throw new IllegalArgumentException("errorHandlingStrategy cannot be null");
        }
        if (silenceThreshold.compareTo(adaptiveTimeout) > 0) {
            throw new IllegalArgumentException("silenceThreshold cannot be greater than adaptiveTimeout");
        }
        if (ttsConfig == null) {
            ttsConfig = TTSConfig.disabled();
        }
    }

    /**
     * Default configuration.
     *
     * <ul>
     *   <li>Adaptive timeout: 5 seconds</li>
     *   <li>Silence: 2 seconds</li>
     *   <li>Buffer: 50 messages</li>
     *   <li>Rate limit: Lenient</li>
     *   <li>Backpressure: DROP_OLDEST</li>
     *   <li>Error handling: 3 retries with exponential backoff</li>
     *   <li>No TTS, no persistence, no security</li>
     * </ul>
     *
     * @return default config
     */
    public static BatchingConfig defaults() {
        return builder().build();
    }

    /**
     * Creates a new builder for BatchingConfig.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if a message store is configured.
     *
     * @return true if message store is present
     */
    public boolean hasMessageStore() {
        return messageStore != null;
    }

    /**
     * Checks if security configuration is present.
     *
     * @return true if security config is present
     */
    public boolean hasSecurity() {
        return securityConfig != null;
    }

    /**
     * Checks if TTS is enabled.
     *
     * @return true if TTS is configured and enabled
     */
    public boolean hasTTS() {
        return ttsConfig.isEnabled();
    }

    /**
     * Builder for BatchingConfig with fluent API.
     */
    public static final class Builder {
        private Duration adaptiveTimeout = Duration.ofSeconds(5);
        private Duration silenceThreshold = Duration.ofSeconds(2);
        private int maxBufferSize = 50;
        private RateLimitConfig rateLimitConfig = RateLimitConfig.lenient();
        private BackpressureStrategy backpressureStrategy = BackpressureStrategy.DROP_OLDEST;
        private ErrorHandlingStrategy errorHandlingStrategy = ErrorHandlingStrategy.defaults();
        private MessageStore messageStore;
        private TTSConfig ttsConfig = TTSConfig.disabled();
        private SecurityConfig securityConfig;

        private Builder() {}

        /**
         * Sets the maximum wait time before processing.
         *
         * <p>Even if messages continue arriving, after this time
         * the buffer is processed.</p>
         *
         * @param timeout maximum timeout
         * @return this builder
         */
        public Builder adaptiveTimeout(@NonNull Duration timeout) {
            this.adaptiveTimeout = timeout;
            return this;
        }

        /**
         * Sets the silence threshold before processing.
         *
         * <p>If the user stops sending for this duration, the buffer
         * is processed immediately (doesn't wait for full timeout).</p>
         *
         * @param threshold silence duration
         * @return this builder
         */
        public Builder silenceThreshold(@NonNull Duration threshold) {
            this.silenceThreshold = threshold;
            return this;
        }

        /**
         * Sets the maximum buffer size per user.
         *
         * <p>When reached, backpressureStrategy is applied.</p>
         *
         * @param size maximum size
         * @return this builder
         */
        public Builder maxBufferSize(int size) {
            this.maxBufferSize = size;
            return this;
        }

        /**
         * Sets the rate limiting configuration.
         *
         * @param config rate limit config
         * @return this builder
         */
        public Builder rateLimitConfig(@NonNull RateLimitConfig config) {
            this.rateLimitConfig = config;
            return this;
        }

        /**
         * Sets the backpressure strategy.
         *
         * @param strategy the strategy
         * @return this builder
         */
        public Builder backpressureStrategy(@NonNull BackpressureStrategy strategy) {
            this.backpressureStrategy = strategy;
            return this;
        }

        /**
         * Sets the error handling strategy.
         *
         * @param strategy the strategy
         * @return this builder
         */
        public Builder errorHandlingStrategy(@NonNull ErrorHandlingStrategy strategy) {
            this.errorHandlingStrategy = strategy;
            return this;
        }

        /**
         * Sets the message store for persistence and deduplication.
         *
         * @param store the store (can be null)
         * @return this builder
         */
        public Builder messageStore(@Nullable MessageStore store) {
            this.messageStore = store;
            return this;
        }

        /**
         * Sets the TTS configuration.
         *
         * @param config TTS config
         * @return this builder
         */
        public Builder ttsConfig(@NonNull TTSConfig config) {
            this.ttsConfig = config;
            return this;
        }

        /**
         * Sets the security configuration.
         *
         * @param config security config (can be null)
         * @return this builder
         */
        public Builder securityConfig(@Nullable SecurityConfig config) {
            this.securityConfig = config;
            return this;
        }

        /**
         * Builds the BatchingConfig.
         *
         * @return the built configuration
         */
        public BatchingConfig build() {
            return new BatchingConfig(
                    adaptiveTimeout,
                    silenceThreshold,
                    maxBufferSize,
                    rateLimitConfig,
                    backpressureStrategy,
                    errorHandlingStrategy,
                    messageStore,
                    ttsConfig,
                    securityConfig
            );
        }
    }
}
