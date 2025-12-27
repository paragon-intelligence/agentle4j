package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a tool execution fails.
 *
 * <p>This exception is not retryable—the tool implementation or arguments need to be fixed.
 *
 * <p>Example usage:
 * <pre>{@code
 * if (error instanceof ToolExecutionException e) {
 *     log.error("Tool {} failed: {}", e.toolName(), e.getMessage());
 * }
 * }</pre>
 */
public class ToolExecutionException extends AgentleException {

  private final @NonNull String toolName;
  private final @Nullable String callId;
  private final @Nullable String arguments;

  /**
   * Creates a new ToolExecutionException.
   *
   * @param toolName the name of the tool that failed
   * @param callId the tool call ID
   * @param arguments the tool arguments (as JSON)
   * @param message the error message
   */
  public ToolExecutionException(
      @NonNull String toolName,
      @Nullable String callId,
      @Nullable String arguments,
      @NonNull String message) {
    super(ErrorCode.TOOL_EXECUTION_FAILED, message, null, false);
    this.toolName = toolName;
    this.callId = callId;
    this.arguments = arguments;
  }

  /**
   * Creates a new ToolExecutionException with a cause.
   *
   * @param toolName the name of the tool that failed
   * @param callId the tool call ID
   * @param arguments the tool arguments (as JSON)
   * @param message the error message
   * @param cause the underlying cause
   */
  public ToolExecutionException(
      @NonNull String toolName,
      @Nullable String callId,
      @Nullable String arguments,
      @NonNull String message,
      @NonNull Throwable cause) {
    super(ErrorCode.TOOL_EXECUTION_FAILED, message, cause, null, false);
    this.toolName = toolName;
    this.callId = callId;
    this.arguments = arguments;
  }

  /**
   * Returns the name of the tool that failed.
   *
   * @return the tool name
   */
  public @NonNull String toolName() {
    return toolName;
  }

  /**
   * Returns the tool call ID.
   *
   * @return the call ID, or null if not available
   */
  public @Nullable String callId() {
    return callId;
  }

  /**
   * Returns the tool arguments as JSON.
   *
   * @return the arguments, or null if not available
   */
  public @Nullable String arguments() {
    return arguments;
  }
}
