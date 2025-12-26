package com.paragon.responses;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Configuration for retry behavior with exponential backoff.
 *
 * <p>Use this to configure how the {@link Responder} handles transient failures such as rate
 * limiting (HTTP 429) and server errors (HTTP 5xx).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple configuration
 * Responder.builder()
 *     .openRouter()
 *     .apiKey(key)
 *     .maxRetries(3)
 *     .build();
 *
 * // Advanced configuration
 * Responder.builder()
 *     .openRouter()
 *     .apiKey(key)
 *     .retryPolicy(RetryPolicy.builder()
 *         .maxRetries(5)
 *         .initialDelay(Duration.ofMillis(500))
 *         .maxDelay(Duration.ofSeconds(30))
 *         .multiplier(2.0)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @param maxRetries Maximum number of retry attempts (0 = no retries)
 * @param initialDelay Initial delay before first retry
 * @param maxDelay Maximum delay between retries (caps exponential growth)
 * @param multiplier Multiplier for exponential backoff (e.g., 2.0 = double delay each retry)
 * @param retryableStatusCodes HTTP status codes that should trigger a retry
 */
public record RetryPolicy(
    int maxRetries,
    @NonNull Duration initialDelay,
    @NonNull Duration maxDelay,
    double multiplier,
    @NonNull Set<Integer> retryableStatusCodes) {

  /** Default retryable status codes: 429 (rate limit), 500, 502, 503, 504 (server errors). */
  public static final Set<Integer> DEFAULT_RETRYABLE_STATUS_CODES =
      Set.of(429, 500, 502, 503, 504);

  public RetryPolicy {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be >= 0");
    }
    if (multiplier < 1.0) {
      throw new IllegalArgumentException("multiplier must be >= 1.0");
    }
    Objects.requireNonNull(initialDelay, "initialDelay must not be null");
    Objects.requireNonNull(maxDelay, "maxDelay must not be null");
    Objects.requireNonNull(retryableStatusCodes, "retryableStatusCodes must not be null");
  }

  /**
   * Returns the default retry policy with sensible defaults:
   *
   * <ul>
   *   <li>maxRetries: 3
   *   <li>initialDelay: 1 second
   *   <li>maxDelay: 30 seconds
   *   <li>multiplier: 2.0
   *   <li>retryableStatusCodes: 429, 500, 502, 503, 504
   * </ul>
   */
  public static @NonNull RetryPolicy defaults() {
    return new RetryPolicy(
        3, Duration.ofSeconds(1), Duration.ofSeconds(30), 2.0, DEFAULT_RETRYABLE_STATUS_CODES);
  }

  /**
   * Returns a retry policy that disables retries (maxRetries = 0). Use this when you want to handle
   * retries manually or disable them entirely.
   */
  public static @NonNull RetryPolicy disabled() {
    return new RetryPolicy(
        0, Duration.ofSeconds(1), Duration.ofSeconds(30), 2.0, DEFAULT_RETRYABLE_STATUS_CODES);
  }

  /**
   * Creates a new builder for constructing a custom retry policy.
   *
   * @return a new builder instance
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Calculates the delay for a specific retry attempt using exponential backoff.
   *
   * @param attempt the retry attempt number (1-based)
   * @return the delay duration, capped at maxDelay
   */
  public @NonNull Duration getDelayForAttempt(int attempt) {
    if (attempt < 1) {
      return initialDelay;
    }
    double delayMs = initialDelay.toMillis() * Math.pow(multiplier, attempt - 1);
    long cappedDelayMs = Math.min((long) delayMs, maxDelay.toMillis());
    return Duration.ofMillis(cappedDelayMs);
  }

  /**
   * Checks if the given HTTP status code should trigger a retry.
   *
   * @param statusCode the HTTP status code
   * @return true if the status code is retryable
   */
  public boolean isRetryable(int statusCode) {
    return retryableStatusCodes.contains(statusCode);
  }

  /** Builder for constructing {@link RetryPolicy} instances. */
  public static final class Builder {
    private int maxRetries = 3;
    private Duration initialDelay = Duration.ofSeconds(1);
    private Duration maxDelay = Duration.ofSeconds(30);
    private double multiplier = 2.0;
    private Set<Integer> retryableStatusCodes = DEFAULT_RETRYABLE_STATUS_CODES;

    private Builder() {}

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxRetries max retries (0 = no retries)
     * @return this builder
     */
    public @NonNull Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Sets the initial delay before the first retry.
     *
     * @param initialDelay the initial delay
     * @return this builder
     */
    public @NonNull Builder initialDelay(@NonNull Duration initialDelay) {
      this.initialDelay = Objects.requireNonNull(initialDelay);
      return this;
    }

    /**
     * Sets the maximum delay between retries (caps exponential growth).
     *
     * @param maxDelay the maximum delay
     * @return this builder
     */
    public @NonNull Builder maxDelay(@NonNull Duration maxDelay) {
      this.maxDelay = Objects.requireNonNull(maxDelay);
      return this;
    }

    /**
     * Sets the multiplier for exponential backoff.
     *
     * @param multiplier the multiplier (must be >= 1.0)
     * @return this builder
     */
    public @NonNull Builder multiplier(double multiplier) {
      this.multiplier = multiplier;
      return this;
    }

    /**
     * Sets the HTTP status codes that should trigger a retry.
     *
     * @param statusCodes the retryable status codes
     * @return this builder
     */
    public @NonNull Builder retryableStatusCodes(@NonNull Set<Integer> statusCodes) {
      this.retryableStatusCodes = Objects.requireNonNull(statusCodes);
      return this;
    }

    /**
     * Builds the retry policy.
     *
     * @return the constructed RetryPolicy
     */
    public @NonNull RetryPolicy build() {
      return new RetryPolicy(maxRetries, initialDelay, maxDelay, multiplier, retryableStatusCodes);
    }
  }
}
