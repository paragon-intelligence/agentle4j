package com.paragon.messaging.whatsapp.config;

import com.paragon.tts.TTSProvider;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for Text-to-Speech in WhatsApp messaging.
 *
 * <p>Controls whether and how often responses are sent as audio messages
 * instead of text messages.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Disabled TTS (default)
 * TTSConfig config = TTSConfig.disabled();
 *
 * // TTS with 30% chance of audio response
 * TTSConfig config = TTSConfig.builder()
 *     .provider(elevenLabsProvider)
 *     .speechChance(0.3)
 *     .languageCode("pt-BR")
 *     .defaultVoiceId("voice123")
 *     .build();
 * }</pre>
 *
 * @param provider        The TTS provider to use (nullable if TTS disabled)
 * @param speechChance    Probability of responding with audio (0.0-1.0)
 * @param defaultVoiceId  Default voice ID for synthesis (provider-specific)
 * @param languageCode    Language code for synthesis (e.g., "pt-BR", "en-US")
 * @author Agentle Team
 * @since 2.1
 */
public record TTSConfig(
        @Nullable TTSProvider provider,

        @DecimalMin(value = "0.0", message = "Speech chance must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Speech chance must be between 0.0 and 1.0")
        double speechChance,

        @Nullable String defaultVoiceId,

        @NonNull
        @Pattern(regexp = "[a-z]{2}-[A-Z]{2}", message = "Language code must be in format 'xx-XX' (e.g., 'pt-BR', 'en-US')")
        String languageCode
) {

    /**
     * Canonical constructor with validation.
     */
    public TTSConfig {
        if (languageCode == null || languageCode.isBlank()) {
            languageCode = "pt-BR";
        }
    }

    /**
     * Creates a disabled TTS configuration.
     *
     * <p>No audio responses will be generated.</p>
     *
     * @return disabled TTS configuration
     */
    public static TTSConfig disabled() {
        return new TTSConfig(null, 0.0, null, "pt-BR");
    }

    /**
     * Creates a TTS configuration that always responds with audio.
     *
     * @param provider the TTS provider
     * @return always-audio TTS configuration
     */
    public static TTSConfig alwaysAudio(@NonNull TTSProvider provider) {
        return new TTSConfig(provider, 1.0, null, "pt-BR");
    }

    /**
     * Creates a new builder for TTSConfig.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if TTS is enabled.
     *
     * @return true if a provider is configured and speechChance is greater than 0
     */
    public boolean isEnabled() {
        return provider != null && speechChance > 0.0;
    }

    /**
     * Checks if TTS should be used for a given random value.
     *
     * <p>Call this with {@code random.nextDouble()} to determine
     * if audio should be sent.</p>
     *
     * @param randomValue random value between 0.0 and 1.0
     * @return true if audio should be used
     */
    public boolean shouldUseAudio(double randomValue) {
        return isEnabled() && randomValue < speechChance;
    }

    /**
     * Builder for TTSConfig with fluent API.
     */
    public static final class Builder {
        private TTSProvider provider;
        private double speechChance = 0.0;
        private String defaultVoiceId;
        private String languageCode = "pt-BR";

        private Builder() {}

        /**
         * Sets the TTS provider.
         *
         * @param provider the TTS provider
         * @return this builder
         */
        public Builder provider(@Nullable TTSProvider provider) {
            this.provider = provider;
            return this;
        }

        /**
         * Sets the probability of responding with audio.
         *
         * @param chance probability between 0.0 (never) and 1.0 (always)
         * @return this builder
         */
        public Builder speechChance(double chance) {
            this.speechChance = chance;
            return this;
        }

        /**
         * Sets the default voice ID for synthesis.
         *
         * @param voiceId provider-specific voice identifier
         * @return this builder
         */
        public Builder defaultVoiceId(@Nullable String voiceId) {
            this.defaultVoiceId = voiceId;
            return this;
        }

        /**
         * Sets the language code for synthesis.
         *
         * @param code language code (e.g., "pt-BR", "en-US")
         * @return this builder
         */
        public Builder languageCode(@NonNull String code) {
            this.languageCode = code;
            return this;
        }

        /**
         * Builds the TTSConfig.
         *
         * @return the built configuration
         */
        public TTSConfig build() {
            return new TTSConfig(provider, speechChance, defaultVoiceId, languageCode);
        }
    }
}
