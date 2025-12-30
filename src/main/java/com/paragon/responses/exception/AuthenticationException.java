package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when authentication fails (HTTP 401/403).
 *
 * <p>This exception is not retryable—the API key or credentials must be fixed.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * if (error instanceof AuthenticationException e) {
 *     log.error("Auth failed: {}", e.suggestion());
 *     // Prompt user to check API key
 * }
 * }</pre>
 */
public class AuthenticationException extends ApiException {

  /**
   * Creates a new AuthenticationException.
   *
   * @param statusCode the HTTP status code (401 or 403)
   * @param message the error message
   * @param requestId optional request correlation ID
   * @param responseBody optional raw response body
   */
  public AuthenticationException(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody) {
    super(
        statusCode == 401 ? ErrorCode.AUTHENTICATION_FAILED : ErrorCode.AUTHORIZATION_FAILED,
        statusCode,
        message,
        requestId,
        responseBody,
        statusCode == 401
            ? "Check that your API key is valid and not expired"
            : "Check that your API key has the required permissions",
        false);
  }
}
