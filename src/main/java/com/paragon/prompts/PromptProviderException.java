package com.paragon.prompts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a prompt cannot be retrieved from a {@link PromptProvider}.
 *
 * <p>This exception wraps underlying failures such as IO errors, network failures, or API errors,
 * providing a consistent exception type for prompt retrieval operations.
 *
 * @author Agentle Framework
 * @since 1.0
 */
public class PromptProviderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String promptId;
  private final boolean retryable;

  /**
   * Creates a new exception for a prompt retrieval failure.
   *
   * @param message the error message
   * @param promptId the ID of the prompt that failed to load
   */
  public PromptProviderException(@NonNull String message, @Nullable String promptId) {
    super(message);
    this.promptId = promptId;
    this.retryable = false;
  }

  /**
   * Creates a new exception with a cause.
   *
   * @param message the error message
   * @param promptId the ID of the prompt that failed to load
   * @param cause the underlying cause
   */
  public PromptProviderException(
      @NonNull String message, @Nullable String promptId, @Nullable Throwable cause) {
    super(message, cause);
    this.promptId = promptId;
    this.retryable = false;
  }

  /**
   * Creates a new exception with retryability information.
   *
   * @param message the error message
   * @param promptId the ID of the prompt that failed to load
   * @param cause the underlying cause
   * @param retryable whether the operation can be retried
   */
  public PromptProviderException(
      @NonNull String message,
      @Nullable String promptId,
      @Nullable Throwable cause,
      boolean retryable) {
    super(message, cause);
    this.promptId = promptId;
    this.retryable = retryable;
  }

  /**
   * Returns the ID of the prompt that failed to load.
   *
   * @return the prompt ID, may be null
   */
  @Nullable
  public String promptId() {
    return promptId;
  }

  /**
   * Returns whether the operation can be retried.
   *
   * <p>This is typically true for transient failures such as network timeouts or rate limiting, and
   * false for permanent failures like missing prompts.
   *
   * @return true if retryable, false otherwise
   */
  public boolean isRetryable() {
    return retryable;
  }
}
