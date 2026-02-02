package com.paragon.messaging.whatsapp.config;

import com.paragon.messaging.whatsapp.RateLimitConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for WhatsApp messaging integration.
 *
 * <p>This class encapsulates all necessary credentials and settings for connecting to the
 * WhatsApp Business Cloud API. It uses a builder pattern for cleaner construction and
 * validation.</p>
 *
 * @param phoneNumberId   The unique ID of the WhatsApp phone number
 * @param accessToken     The system user access token (permanent or temporary)
 * @param verifyToken     The verification token for webhook validation
 * @param rateLimitConfig Configuration for rate limiting and flood prevention
 * @param connectionTimeout HTTP connection timeout duration
 * @param readTimeout     HTTP read timeout duration
 * @since 1.0
 */
public record WhatsAppConfig(
        @NonNull String phoneNumberId,
        @NonNull String accessToken,
        @NonNull String verifyToken,
        @NonNull RateLimitConfig rateLimitConfig,
        @NonNull Duration connectionTimeout,
        @NonNull Duration readTimeout
) {

  public WhatsAppConfig {
    Objects.requireNonNull(phoneNumberId, "phoneNumberId cannot be null");
    Objects.requireNonNull(accessToken, "accessToken cannot be null");
    Objects.requireNonNull(verifyToken, "verifyToken cannot be null");
    Objects.requireNonNull(rateLimitConfig, "rateLimitConfig cannot be null");
    Objects.requireNonNull(connectionTimeout, "connectionTimeout cannot be null");
    Objects.requireNonNull(readTimeout, "readTimeout cannot be null");

    if (phoneNumberId.isBlank()) {
      throw new IllegalArgumentException("phoneNumberId cannot be blank");
    }
    if (accessToken.isBlank()) {
      throw new IllegalArgumentException("accessToken cannot be blank");
    }
    if (connectionTimeout.isNegative() || connectionTimeout.isZero()) {
      throw new IllegalArgumentException("connectionTimeout must be positive");
    }
    if (readTimeout.isNegative() || readTimeout.isZero()) {
      throw new IllegalArgumentException("readTimeout must be positive");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String phoneNumberId;
    private String accessToken;
    private String verifyToken;
    private RateLimitConfig rateLimitConfig;
    private Duration connectionTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);

    public Builder phoneNumberId(String phoneNumberId) {
      this.phoneNumberId = phoneNumberId;
      return this;
    }

    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    public Builder verifyToken(String verifyToken) {
      this.verifyToken = verifyToken;
      return this;
    }

    public Builder rateLimitConfig(RateLimitConfig rateLimitConfig) {
      this.rateLimitConfig = rateLimitConfig;
      return this;
    }

    public Builder connectionTimeout(Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public Builder readTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    public WhatsAppConfig build() {
      // Provide default lenient rate limit if none specified
      if (rateLimitConfig == null) {
        rateLimitConfig = RateLimitConfig.lenient();
      }

      return new WhatsAppConfig(
              phoneNumberId,
              accessToken,
              verifyToken,
              rateLimitConfig,
              connectionTimeout,
              readTimeout
      );
    }
  }
}
