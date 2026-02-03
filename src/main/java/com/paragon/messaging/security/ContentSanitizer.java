package com.paragon.messaging.security;

import com.paragon.messaging.security.SecurityConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitizes message content to prevent injection attacks.
 *
 * <p>Provides protection against:</p>
 * <ul>
 *   <li>XSS (Cross-Site Scripting) patterns</li>
 *   <li>SQL injection patterns</li>
 *   <li>Template injection patterns</li>
 *   <li>Oversized messages</li>
 *   <li>Control characters and null bytes</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create from security config
 * ContentSanitizer sanitizer = ContentSanitizer.create(securityConfig);
 *
 * // Validate content
 * ContentSanitizer.ValidationResult result = sanitizer.validate(messageContent);
 * if (!result.isValid()) {
 *     log.warn("Content blocked: {}", result.blockedPatterns());
 *     return; // Reject message
 * }
 *
 * // Sanitize content (removes dangerous patterns)
 * String safe = sanitizer.sanitize(messageContent);
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 * @see SecurityConfig
 */
public final class ContentSanitizer {

    private final List<Pattern> blockedPatterns;
    private final int maxLength;
    private final boolean enabled;

    private ContentSanitizer(List<Pattern> blockedPatterns, int maxLength, boolean enabled) {
        this.blockedPatterns = blockedPatterns;
        this.maxLength = maxLength;
        this.enabled = enabled;
    }

    /**
     * Creates a sanitizer from a security configuration.
     *
     * @param config the security configuration
     * @return a new sanitizer
     */
    public static ContentSanitizer create(@NonNull SecurityConfig config) {
        Objects.requireNonNull(config, "config cannot be null");

        if (!config.contentSanitization()) {
            return new ContentSanitizer(List.of(), config.maxMessageLength(), false);
        }

        List<Pattern> patterns = config.blockedPatterns().stream()
                .map(Pattern::compile)
                .toList();

        return new ContentSanitizer(patterns, config.maxMessageLength(), true);
    }

    /**
     * Creates a sanitizer with default blocked patterns.
     *
     * @param maxLength maximum allowed message length
     * @return a new sanitizer with default patterns
     */
    public static ContentSanitizer withDefaults(int maxLength) {
        List<Pattern> patterns = SecurityConfig.DEFAULT_BLOCKED_PATTERNS.stream()
                .map(Pattern::compile)
                .toList();
        return new ContentSanitizer(patterns, maxLength, true);
    }

    /**
     * Creates a disabled sanitizer that accepts all content.
     *
     * @return a disabled sanitizer
     */
    public static ContentSanitizer disabled() {
        return new ContentSanitizer(List.of(), Integer.MAX_VALUE, false);
    }

    /**
     * Validates content against security rules.
     *
     * @param content the content to validate
     * @return validation result with details
     */
    public ValidationResult validate(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            return ValidationResult.valid();
        }

        if (!enabled) {
            return ValidationResult.valid();
        }

        List<String> issues = new ArrayList<>();

        // Check length
        if (content.length() > maxLength) {
            issues.add("Content exceeds maximum length of " + maxLength);
        }

        // Check for null bytes and control characters
        if (containsDangerousCharacters(content)) {
            issues.add("Content contains dangerous characters");
        }

        // Check blocked patterns
        List<String> matchedPatterns = new ArrayList<>();
        for (Pattern pattern : blockedPatterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                matchedPatterns.add(pattern.pattern());
            }
        }

        if (!matchedPatterns.isEmpty()) {
            issues.add("Content matches blocked patterns: " + String.join(", ", matchedPatterns));
        }

        return new ValidationResult(issues.isEmpty(), issues, matchedPatterns);
    }

    /**
     * Checks if content is valid without returning detailed results.
     *
     * @param content the content to check
     * @return true if content passes all checks
     */
    public boolean isValid(@Nullable String content) {
        return validate(content).isValid();
    }

    /**
     * Sanitizes content by removing or replacing dangerous patterns.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Removes null bytes and control characters</li>
     *   <li>Normalizes whitespace</li>
     *   <li>Truncates to max length</li>
     *   <li>Removes matched blocked patterns</li>
     * </ul>
     *
     * @param content the content to sanitize
     * @return sanitized content
     */
    public String sanitize(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        if (!enabled) {
            return content;
        }

        String result = content;

        // Remove null bytes and control characters (except newlines, tabs)
        result = result.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // Normalize whitespace (collapse multiple spaces, trim)
        result = result.replaceAll("\\s+", " ").trim();

        // Remove blocked patterns
        for (Pattern pattern : blockedPatterns) {
            result = pattern.matcher(result).replaceAll("[BLOCKED]");
        }

        // Truncate to max length
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }

    /**
     * Checks if content passes length check.
     *
     * @param content the content to check
     * @return true if content is within max length
     */
    public boolean isWithinLength(@Nullable String content) {
        return content == null || content.length() <= maxLength;
    }

    /**
     * Returns the maximum allowed content length.
     *
     * @return max length
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Checks if sanitization is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    private boolean containsDangerousCharacters(String content) {
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            // Allow printable ASCII, tabs, newlines, and common Unicode
            if (c == '\0') {
                return true; // Null byte
            }
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                return true; // Control character
            }
        }
        return false;
    }

    /**
     * Result of content validation.
     *
     * @param isValid         true if content passed all checks
     * @param issues          list of validation issues (empty if valid)
     * @param blockedPatterns list of matched blocked patterns
     */
    public record ValidationResult(
            boolean isValid,
            @NonNull List<String> issues,
            @NonNull List<String> blockedPatterns
    ) {
        public ValidationResult {
            issues = List.copyOf(issues);
            blockedPatterns = List.copyOf(blockedPatterns);
        }

        /**
         * Creates a valid result.
         *
         * @return valid result
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of(), List.of());
        }

        /**
         * Creates an invalid result with a single issue.
         *
         * @param issue the validation issue
         * @return invalid result
         */
        public static ValidationResult invalid(String issue) {
            return new ValidationResult(false, List.of(issue), List.of());
        }

        /**
         * Checks if any blocked patterns were matched.
         *
         * @return true if patterns were matched
         */
        public boolean hasBlockedPatterns() {
            return !blockedPatterns.isEmpty();
        }
    }
}
