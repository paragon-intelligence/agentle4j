package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when the request is invalid (HTTP 4xx, excluding 401/403/429).
 *
 * <p>This exception is not retryable—the request must be fixed.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Invalid JSON payload</li>
 *   <li>Missing required parameters</li>
 *   <li>Invalid parameter values</li>
 *   <li>Unsupported model or feature</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (error instanceof InvalidRequestException e) {
 *     log.error("Invalid request: {}", e.getMessage());
 *     // Check payload construction
 * }
 * }</pre>
 */
public class InvalidRequestException extends ApiException {

  /**
   * Creates a new InvalidRequestException.
   *
   * @param statusCode the HTTP status code (4xx)
   * @param message the error message
   * @param requestId optional request correlation ID
   * @param responseBody optional raw response body
   */
  public InvalidRequestException(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody) {
    super(
        ErrorCode.INVALID_REQUEST,
        statusCode,
        message,
        requestId,
        responseBody,
        "Check the request payload for missing or invalid parameters",
        false);
  }

  /**
   * Creates a new InvalidRequestException with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public InvalidRequestException(@NonNull String message, @NonNull Throwable cause) {
    super(ErrorCode.INVALID_REQUEST, 400, message, cause, null, null,
        "Check the request payload for missing or invalid parameters", false);
  }
}
