package com.paragon.messaging.whatsapp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Security configuration for WhatsApp webhook and message processing.
 *
 * <p>Provides protection against:</p>
 * <ul>
 *   <li>Webhook spoofing (signature validation)</li>
 *   <li>Message content attacks (XSS, injection)</li>
 *   <li>Flood attacks (rate limiting)</li>
 *   <li>Oversized messages (length limits)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Basic security (no signature validation)
 * SecurityConfig config = SecurityConfig.defaults("my-verify-token");
 *
 * // Strict security with signature validation
 * SecurityConfig config = SecurityConfig.strict("verify-token", "app-secret");
 *
 * // Custom configuration
 * SecurityConfig config = SecurityConfig.builder()
 *     .webhookVerifyToken("my-token")
 *     .appSecret("my-secret")
 *     .validateSignatures(true)
 *     .contentSanitization(true)
 *     .maxMessageLength(2048)
 *     .blockedPatterns(Set.of("(?i)<script"))
 *     .floodPreventionWindow(Duration.ofSeconds(5))
 *     .maxMessagesPerWindow(10)
 *     .build();
 * }</pre>
 *
 * @param webhookVerifyToken     Token for webhook verification (GET request)
 * @param appSecret              App secret for X-Hub-Signature-256 validation (nullable to disable)
 * @param validateSignatures     Whether to validate webhook signatures
 * @param contentSanitization    Whether to sanitize message content
 * @param maxMessageLength       Maximum allowed message length (characters)
 * @param blockedPatterns        Regex patterns to block (e.g., XSS, SQL injection)
 * @param floodPreventionWindow  Time window for flood detection
 * @param maxMessagesPerWindow   Maximum messages allowed per user in flood window
 * @author Agentle Team
 * @since 2.1
 */
public record SecurityConfig(
        @NotBlank String webhookVerifyToken,
        @Nullable String appSecret,
        boolean validateSignatures,
        boolean contentSanitization,
        @Positive int maxMessageLength,
        @NonNull Set<String> blockedPatterns,
        @NonNull Duration floodPreventionWindow,
        @Positive int maxMessagesPerWindow
) {

    /**
     * Default blocked patterns for common attacks.
     */
    public static final Set<String> DEFAULT_BLOCKED_PATTERNS = Set.of(
            "(?i)(<script|javascript:|data:text/html|<iframe)",  // XSS
            "(?i)(drop\\s+table|delete\\s+from|insert\\s+into|update\\s+.+\\s+set)", // SQL Injection
            "(?i)(\\$\\{|\\{\\{|<%|<\\?)" // Template injection
    );

    /**
     * Canonical constructor with validation.
     */
    public SecurityConfig {
        if (webhookVerifyToken == null || webhookVerifyToken.isBlank()) {
            throw new IllegalArgumentException("webhookVerifyToken cannot be blank");
        }
        if (validateSignatures && (appSecret == null || appSecret.isBlank())) {
            throw new IllegalArgumentException("appSecret required when validateSignatures is true");
        }
        if (maxMessageLength <= 0) {
            maxMessageLength = 4096;
        }
        if (blockedPatterns == null) {
            blockedPatterns = Set.of();
        }
        if (floodPreventionWindow == null || floodPreventionWindow.isNegative()) {
            floodPreventionWindow = Duration.ofSeconds(10);
        }
        if (maxMessagesPerWindow <= 0) {
            maxMessagesPerWindow = 20;
        }

        // Validate regex patterns compile correctly
        for (String pattern : blockedPatterns) {
            try {
                Pattern.compile(pattern);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid blocked pattern: " + pattern, e);
            }
        }
    }

    /**
     * Creates a default security configuration.
     *
     * <p>Features:</p>
     * <ul>
     *   <li>No signature validation</li>
     *   <li>Content sanitization enabled</li>
     *   <li>4096 character message limit</li>
     *   <li>10 second flood window with 20 message limit</li>
     * </ul>
     *
     * @param verifyToken the webhook verify token
     * @return default security configuration
     */
    public static SecurityConfig defaults(@NonNull String verifyToken) {
        return builder()
                .webhookVerifyToken(verifyToken)
                .validateSignatures(false)
                .contentSanitization(true)
                .maxMessageLength(4096)
                .floodPreventionWindow(Duration.ofSeconds(10))
                .maxMessagesPerWindow(20)
                .build();
    }

    /**
     * Creates a strict security configuration.
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Signature validation enabled</li>
     *   <li>Content sanitization enabled</li>
     *   <li>Default attack pattern blocking</li>
     *   <li>2048 character message limit</li>
     *   <li>5 second flood window with 10 message limit</li>
     * </ul>
     *
     * @param verifyToken the webhook verify token
     * @param appSecret the app secret for signature validation
     * @return strict security configuration
     */
    public static SecurityConfig strict(@NonNull String verifyToken, @NonNull String appSecret) {
        return builder()
                .webhookVerifyToken(verifyToken)
                .appSecret(appSecret)
                .validateSignatures(true)
                .contentSanitization(true)
                .maxMessageLength(2048)
                .blockedPatterns(DEFAULT_BLOCKED_PATTERNS)
                .floodPreventionWindow(Duration.ofSeconds(5))
                .maxMessagesPerWindow(10)
                .build();
    }

    /**
     * Creates a new builder for SecurityConfig.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if signature validation is enabled and configured.
     *
     * @return true if signatures should be validated
     */
    public boolean shouldValidateSignatures() {
        return validateSignatures && appSecret != null && !appSecret.isBlank();
    }

    /**
     * Builder for SecurityConfig with fluent API.
     */
    public static final class Builder {
        private String webhookVerifyToken;
        private String appSecret;
        private boolean validateSignatures = false;
        private boolean contentSanitization = true;
        private int maxMessageLength = 4096;
        private Set<String> blockedPatterns = Set.of();
        private Duration floodPreventionWindow = Duration.ofSeconds(10);
        private int maxMessagesPerWindow = 20;

        private Builder() {}

        /**
         * Sets the webhook verification token.
         *
         * @param token the verify token
         * @return this builder
         */
        public Builder webhookVerifyToken(@NonNull String token) {
            this.webhookVerifyToken = token;
            return this;
        }

        /**
         * Sets the app secret for signature validation.
         *
         * @param secret the app secret
         * @return this builder
         */
        public Builder appSecret(@Nullable String secret) {
            this.appSecret = secret;
            return this;
        }

        /**
         * Enables or disables signature validation.
         *
         * @param validate true to enable signature validation
         * @return this builder
         */
        public Builder validateSignatures(boolean validate) {
            this.validateSignatures = validate;
            return this;
        }

        /**
         * Enables or disables content sanitization.
         *
         * @param sanitize true to enable sanitization
         * @return this builder
         */
        public Builder contentSanitization(boolean sanitize) {
            this.contentSanitization = sanitize;
            return this;
        }

        /**
         * Sets the maximum allowed message length.
         *
         * @param length maximum characters
         * @return this builder
         */
        public Builder maxMessageLength(int length) {
            this.maxMessageLength = length;
            return this;
        }

        /**
         * Sets the regex patterns to block.
         *
         * @param patterns set of regex patterns
         * @return this builder
         */
        public Builder blockedPatterns(@NonNull Set<String> patterns) {
            this.blockedPatterns = Set.copyOf(patterns);
            return this;
        }

        /**
         * Sets the flood prevention time window.
         *
         * @param window the time window
         * @return this builder
         */
        public Builder floodPreventionWindow(@NonNull Duration window) {
            this.floodPreventionWindow = window;
            return this;
        }

        /**
         * Sets the maximum messages allowed per user in the flood window.
         *
         * @param max maximum messages
         * @return this builder
         */
        public Builder maxMessagesPerWindow(int max) {
            this.maxMessagesPerWindow = max;
            return this;
        }

        /**
         * Builds the SecurityConfig.
         *
         * @return the built configuration
         */
        public SecurityConfig build() {
            return new SecurityConfig(
                    webhookVerifyToken, appSecret, validateSignatures,
                    contentSanitization, maxMessageLength, blockedPatterns,
                    floodPreventionWindow, maxMessagesPerWindow
            );
        }
    }
}
