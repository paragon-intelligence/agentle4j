package com.paragon.messaging.security;

import com.paragon.messaging.security.SecurityConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Validates WhatsApp webhook signatures using HMAC-SHA256.
 *
 * <p>WhatsApp webhook requests include an {@code X-Hub-Signature-256} header
 * containing an HMAC-SHA256 signature of the request payload. This class
 * validates that signature to ensure the webhook request is authentic.</p>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li>HMAC-SHA256 signature validation</li>
 *   <li>Constant-time comparison to prevent timing attacks</li>
 *   <li>Support for both hex-encoded and sha256= prefixed signatures</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create validator from security config
 * WebhookSignatureValidator validator = WebhookSignatureValidator.create(securityConfig);
 *
 * // Validate in Spring controller
 * @PostMapping("/webhook")
 * public ResponseEntity<?> handleWebhook(
 *         @RequestHeader("X-Hub-Signature-256") String signature,
 *         @RequestBody String payload) {
 *
 *     if (!validator.isValid(signature, payload)) {
 *         return ResponseEntity.status(401).body("Invalid signature");
 *     }
 *
 *     // Process webhook...
 *     return ResponseEntity.ok().build();
 * }
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 * @see SecurityConfig
 */
public final class WebhookSignatureValidator {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final byte[] secret;
    private final boolean enabled;

    private WebhookSignatureValidator(byte[] secret, boolean enabled) {
        this.secret = secret;
        this.enabled = enabled;
    }

    /**
     * Creates a validator from a security configuration.
     *
     * <p>If signature validation is disabled in the config, the validator
     * will always return true.</p>
     *
     * @param config the security configuration
     * @return a new validator
     */
    public static WebhookSignatureValidator create(@NonNull SecurityConfig config) {
        Objects.requireNonNull(config, "config cannot be null");

        if (!config.shouldValidateSignatures()) {
            return new WebhookSignatureValidator(new byte[0], false);
        }

        byte[] secretBytes = config.appSecret().getBytes(StandardCharsets.UTF_8);
        return new WebhookSignatureValidator(secretBytes, true);
    }

    /**
     * Creates a validator with the specified app secret.
     *
     * @param appSecret the WhatsApp app secret
     * @return a new validator
     */
    public static WebhookSignatureValidator create(@NonNull String appSecret) {
        Objects.requireNonNull(appSecret, "appSecret cannot be null");
        if (appSecret.isBlank()) {
            throw new IllegalArgumentException("appSecret cannot be blank");
        }
        byte[] secretBytes = appSecret.getBytes(StandardCharsets.UTF_8);
        return new WebhookSignatureValidator(secretBytes, true);
    }

    /**
     * Creates a disabled validator that always returns true.
     *
     * <p>Use for testing or when signature validation is not required.</p>
     *
     * @return a disabled validator
     */
    public static WebhookSignatureValidator disabled() {
        return new WebhookSignatureValidator(new byte[0], false);
    }

    /**
     * Validates the webhook signature.
     *
     * @param signature the X-Hub-Signature-256 header value
     * @param payload   the raw request payload
     * @return true if the signature is valid
     */
    public boolean isValid(@Nullable String signature, @NonNull String payload) {
        if (!enabled) {
            return true;
        }

        if (signature == null || signature.isBlank()) {
            return false;
        }

        Objects.requireNonNull(payload, "payload cannot be null");

        try {
            String expectedSignature = computeSignature(payload);
            String actualSignature = normalizeSignature(signature);

            return constantTimeEquals(expectedSignature, actualSignature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates the webhook signature.
     *
     * @param signature    the X-Hub-Signature-256 header value
     * @param payloadBytes the raw request payload bytes
     * @return true if the signature is valid
     */
    public boolean isValid(@Nullable String signature, @NonNull byte[] payloadBytes) {
        if (!enabled) {
            return true;
        }

        if (signature == null || signature.isBlank()) {
            return false;
        }

        Objects.requireNonNull(payloadBytes, "payloadBytes cannot be null");

        try {
            String expectedSignature = computeSignature(payloadBytes);
            String actualSignature = normalizeSignature(signature);

            return constantTimeEquals(expectedSignature, actualSignature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Computes the expected signature for a payload.
     *
     * @param payload the request payload
     * @return the hex-encoded signature
     */
    public String computeSignature(@NonNull String payload) {
        return computeSignature(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes the expected signature for payload bytes.
     *
     * @param payloadBytes the request payload bytes
     * @return the hex-encoded signature
     */
    public String computeSignature(@NonNull byte[] payloadBytes) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret, ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payloadBytes);
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SecurityException("Failed to compute HMAC signature", e);
        }
    }

    /**
     * Checks if signature validation is enabled.
     *
     * @return true if validation is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Normalizes the signature by removing the "sha256=" prefix if present.
     */
    private String normalizeSignature(String signature) {
        if (signature.toLowerCase().startsWith(SIGNATURE_PREFIX)) {
            return signature.substring(SIGNATURE_PREFIX.length()).toLowerCase();
        }
        return signature.toLowerCase();
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     *
     * <p>This method compares two strings character by character,
     * always comparing all characters regardless of where differences occur.
     * This prevents attackers from using timing information to guess the signature.</p>
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
