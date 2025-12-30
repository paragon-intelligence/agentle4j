package com.paragon.responses.exception;

import java.time.Duration;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when rate limited by the API (HTTP 429).
 *
 * <p>This exception is always retryable. Use {@link #retryAfter()} to determine when to retry.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * if (error instanceof RateLimitException e) {
 *     Duration wait = e.retryAfter();
 *     if (wait != null) {
 *         Thread.sleep(wait.toMillis());
 *     }
 *     // Retry request
 * }
 * }</pre>
 */
public class RateLimitException extends ApiException {

  private final @Nullable Duration retryAfter;

  /**
   * Creates a new RateLimitException.
   *
   * @param message the error message
   * @param requestId optional request correlation ID
   * @param responseBody optional raw response body
   * @param retryAfter optional duration to wait before retrying
   */
  public RateLimitException(
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody,
      @Nullable Duration retryAfter) {
    super(
        ErrorCode.RATE_LIMITED,
        429,
        message,
        requestId,
        responseBody,
        "Wait "
            + (retryAfter != null ? retryAfter.toSeconds() + " seconds" : "a moment")
            + " before retrying",
        true);
    this.retryAfter = retryAfter;
  }

  /**
   * Returns the recommended duration to wait before retrying.
   *
   * <p>This is parsed from the API's Retry-After header if available.
   *
   * @return the retry-after duration, or null if not specified
   */
  public @Nullable Duration retryAfter() {
    return retryAfter;
  }
}
