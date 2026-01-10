package com.paragon.mcp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when MCP operations fail.
 *
 * <p>This exception wraps various failure modes including:
 * <ul>
 *   <li>Connection failures (server not reachable, process failed to start)
 *   <li>Protocol errors (invalid JSON-RPC, unexpected response format)
 *   <li>Tool execution failures (tool returned error, timeout)
 *   <li>Session errors (session expired, invalid session)
 * </ul>
 */
public class McpException extends RuntimeException {

  private final @Nullable Integer errorCode;

  /**
   * Creates an MCP exception with a message.
   *
   * @param message the error message
   */
  public McpException(@NonNull String message) {
    super(message);
    this.errorCode = null;
  }

  /**
   * Creates an MCP exception with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public McpException(@NonNull String message, @Nullable Throwable cause) {
    super(message, cause);
    this.errorCode = null;
  }

  /**
   * Creates an MCP exception with a JSON-RPC error code.
   *
   * @param message the error message
   * @param errorCode the JSON-RPC error code
   */
  public McpException(@NonNull String message, int errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Creates an MCP exception with a JSON-RPC error code and cause.
   *
   * @param message the error message
   * @param errorCode the JSON-RPC error code
   * @param cause the underlying cause
   */
  public McpException(@NonNull String message, int errorCode, @Nullable Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * Returns the JSON-RPC error code if available.
   *
   * @return the error code, or null if not a JSON-RPC error
   */
  public @Nullable Integer getErrorCode() {
    return errorCode;
  }

  /**
   * Creates an exception for connection failures.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return a new McpException
   */
  public static McpException connectionFailed(@NonNull String message, @Nullable Throwable cause) {
    return new McpException("Connection failed: " + message, cause);
  }

  /**
   * Creates an exception for protocol errors.
   *
   * @param message the error message
   * @return a new McpException
   */
  public static McpException protocolError(@NonNull String message) {
    return new McpException("Protocol error: " + message);
  }

  /**
   * Creates an exception from a JSON-RPC error response.
   *
   * @param code the JSON-RPC error code
   * @param message the error message
   * @return a new McpException
   */
  public static McpException fromJsonRpcError(int code, @NonNull String message) {
    return new McpException("JSON-RPC error [" + code + "]: " + message, code);
  }

  /**
   * Creates an exception for tool execution failures.
   *
   * @param toolName the name of the tool that failed
   * @param message the error message
   * @return a new McpException
   */
  public static McpException toolExecutionFailed(
      @NonNull String toolName, @NonNull String message) {
    return new McpException("Tool '" + toolName + "' execution failed: " + message);
  }
}
