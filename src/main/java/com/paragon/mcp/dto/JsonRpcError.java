package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A JSON-RPC 2.0 error object.
 *
 * @param code the error code
 * @param message a short description of the error
 * @param data optional additional information about the error
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcError(int code, @NonNull String message, @Nullable Object data) {

  // Standard JSON-RPC error codes
  public static final int PARSE_ERROR = -32700;
  public static final int INVALID_REQUEST = -32600;
  public static final int METHOD_NOT_FOUND = -32601;
  public static final int INVALID_PARAMS = -32602;
  public static final int INTERNAL_ERROR = -32603;

  /**
   * Creates a parse error.
   *
   * @param message the error message
   * @return a new JsonRpcError
   */
  public static JsonRpcError parseError(@NonNull String message) {
    return new JsonRpcError(PARSE_ERROR, message, null);
  }

  /**
   * Creates an invalid request error.
   *
   * @param message the error message
   * @return a new JsonRpcError
   */
  public static JsonRpcError invalidRequest(@NonNull String message) {
    return new JsonRpcError(INVALID_REQUEST, message, null);
  }

  /**
   * Creates a method not found error.
   *
   * @param method the method that was not found
   * @return a new JsonRpcError
   */
  public static JsonRpcError methodNotFound(@NonNull String method) {
    return new JsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method, null);
  }

  /**
   * Creates an internal error.
   *
   * @param message the error message
   * @return a new JsonRpcError
   */
  public static JsonRpcError internalError(@NonNull String message) {
    return new JsonRpcError(INTERNAL_ERROR, message, null);
  }
}
