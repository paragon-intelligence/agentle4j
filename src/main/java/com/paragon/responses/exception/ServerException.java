package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when the server encounters an error (HTTP 5xx).
 *
 * <p>Server errors are typically retryable. The built-in retry policy will automatically retry on
 * 500, 502, 503, and 504 status codes.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * if (error instanceof ServerException e && e.isRetryable()) {
 *     // Log and let built-in retry handle it
 *     log.warn("Server error {}, retrying: {}", e.statusCode(), e.getMessage());
 * }
 * }</pre>
 */
public class ServerException extends ApiException {

  /**
   * Creates a new ServerException.
   *
   * @param statusCode the HTTP status code (5xx)
   * @param message the error message
   * @param requestId optional request correlation ID
   * @param responseBody optional raw response body
   */
  public ServerException(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody) {
    super(
        statusCode == 503 ? ErrorCode.SERVICE_UNAVAILABLE : ErrorCode.SERVER_ERROR,
        statusCode,
        message,
        requestId,
        responseBody,
        "The API server encountered an error. This is usually temporary.",
        true);
  }
}
