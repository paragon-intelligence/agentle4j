package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an API request fails.
 *
 * <p>Provides HTTP-specific context:
 * <ul>
 *   <li>{@link #statusCode()} - The HTTP status code</li>
 *   <li>{@link #requestId()} - Request correlation ID for debugging</li>
 *   <li>{@link #responseBody()} - Raw error response from the API</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (error instanceof ApiException e) {
 *     log.error("API error {} (request {}): {}",
 *         e.statusCode(), e.requestId(), e.getMessage());
 * }
 * }</pre>
 *
 * @see RateLimitException
 * @see AuthenticationException
 * @see ServerException
 * @see InvalidRequestException
 */
public class ApiException extends AgentleException {

  private final int statusCode;
  private final @Nullable String requestId;
  private final @Nullable String responseBody;

  /**
   * Creates a new ApiException.
   *
   * @param code the error code
   * @param statusCode the HTTP status code
   * @param message the error message
   * @param requestId optional request correlation ID
   * @param responseBody optional raw response body
   * @param suggestion optional resolution hint
   * @param retryable whether the error is retryable
   */
  public ApiException(
      @NonNull ErrorCode code,
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody,
      @Nullable String suggestion,
      boolean retryable) {
    super(code, message, suggestion, retryable);
    this.statusCode = statusCode;
    this.requestId = requestId;
    this.responseBody = responseBody;
  }

  /**
   * Creates a new ApiException with a cause.
   *
   * @param code the error code
   * @param statusCode the HTTP status code
   * @param message the error message
   * @param cause the underlying cause
   * @param requestId optional request correlation ID
   * @param responseBody optional raw response body
   * @param suggestion optional resolution hint
   * @param retryable whether the error is retryable
   */
  public ApiException(
      @NonNull ErrorCode code,
      int statusCode,
      @NonNull String message,
      @NonNull Throwable cause,
      @Nullable String requestId,
      @Nullable String responseBody,
      @Nullable String suggestion,
      boolean retryable) {
    super(code, message, cause, suggestion, retryable);
    this.statusCode = statusCode;
    this.requestId = requestId;
    this.responseBody = responseBody;
  }

  /**
   * Creates an ApiException from an HTTP status code.
   *
   * <p>Automatically determines the appropriate subclass based on status code:
   * <ul>
   *   <li>401/403 → {@link AuthenticationException}</li>
   *   <li>429 → {@link RateLimitException}</li>
   *   <li>4xx → {@link InvalidRequestException}</li>
   *   <li>5xx → {@link ServerException}</li>
   * </ul>
   *
   * @param statusCode the HTTP status code
   * @param message the error message
   * @param requestId optional request correlation ID
   * @param responseBody optional raw response body
   * @return the appropriate ApiException subclass
   */
  public static ApiException fromStatusCode(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody) {
    return switch (statusCode) {
      case 401, 403 -> new AuthenticationException(statusCode, message, requestId, responseBody);
      case 429 -> new RateLimitException(message, requestId, responseBody, null);
      case 400, 404, 405, 406, 409, 410, 415, 422 ->
          new InvalidRequestException(statusCode, message, requestId, responseBody);
      default -> {
        if (statusCode >= 500 && statusCode < 600) {
          yield new ServerException(statusCode, message, requestId, responseBody);
        }
        // Fallback for unexpected status codes
        yield new ApiException(
            ErrorCode.UNKNOWN,
            statusCode,
            message,
            requestId,
            responseBody,
            null,
            false);
      }
    };
  }

  /**
   * Returns the HTTP status code.
   *
   * @return the status code
   */
  public int statusCode() {
    return statusCode;
  }

  /**
   * Returns the request correlation ID for debugging.
   *
   * @return the request ID, or null if not available
   */
  public @Nullable String requestId() {
    return requestId;
  }

  /**
   * Returns the raw error response body from the API.
   *
   * @return the response body, or null if not available
   */
  public @Nullable String responseBody() {
    return responseBody;
  }
}
