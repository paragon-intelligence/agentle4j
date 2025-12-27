package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base exception for all Agentle4j errors.
 *
 * <p>All exceptions in the Agentle hierarchy extend this class, providing:
 * <ul>
 *   <li>{@link #code()} - Machine-readable error code for programmatic handling</li>
 *   <li>{@link #suggestion()} - Optional hint for resolution</li>
 *   <li>{@link #isRetryable()} - Whether this error is safe to retry</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * responder.respond(payload)
 *     .exceptionally(error -> {
 *         if (error.getCause() instanceof AgentleException e && e.isRetryable()) {
 *             // Retry logic
 *         }
 *         return null;
 *     });
 * }</pre>
 */
public class AgentleException extends RuntimeException {

  private final @NonNull ErrorCode code;
  private final @Nullable String suggestion;
  private final boolean retryable;

  /**
   * Creates a new AgentleException.
   *
   * @param code the error code
   * @param message the error message
   * @param suggestion optional resolution hint
   * @param retryable whether the error is retryable
   */
  public AgentleException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @Nullable String suggestion,
      boolean retryable) {
    super(message);
    this.code = code;
    this.suggestion = suggestion;
    this.retryable = retryable;
  }

  /**
   * Creates a new AgentleException with a cause.
   *
   * @param code the error code
   * @param message the error message
   * @param cause the underlying cause
   * @param suggestion optional resolution hint
   * @param retryable whether the error is retryable
   */
  public AgentleException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @NonNull Throwable cause,
      @Nullable String suggestion,
      boolean retryable) {
    super(message, cause);
    this.code = code;
    this.suggestion = suggestion;
    this.retryable = retryable;
  }

  /**
   * Returns the machine-readable error code.
   *
   * @return the error code
   */
  public @NonNull ErrorCode code() {
    return code;
  }

  /**
   * Returns an optional hint for resolving this error.
   *
   * @return the suggestion, or null if none
   */
  public @Nullable String suggestion() {
    return suggestion;
  }

  /**
   * Returns whether this error is safe to retry.
   *
   * @return true if retryable
   */
  public boolean isRetryable() {
    return retryable;
  }

  /**
   * Error codes for Agentle exceptions.
   */
  public enum ErrorCode {
    // API errors
    RATE_LIMITED,
    AUTHENTICATION_FAILED,
    AUTHORIZATION_FAILED,
    INVALID_REQUEST,
    SERVER_ERROR,
    SERVICE_UNAVAILABLE,
    
    // Streaming errors
    CONNECTION_DROPPED,
    PARTIAL_OUTPUT,
    STREAM_TIMEOUT,
    
    // Agent errors
    GUARDRAIL_VIOLATED,
    TOOL_EXECUTION_FAILED,
    MAX_TURNS_EXCEEDED,
    
    // Configuration errors
    MISSING_CONFIGURATION,
    INVALID_CONFIGURATION,
    
    // General
    UNKNOWN
  }
}
