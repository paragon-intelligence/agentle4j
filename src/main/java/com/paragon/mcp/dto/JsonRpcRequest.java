package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A JSON-RPC 2.0 request message.
 *
 * @param jsonrpc the JSON-RPC version (always "2.0")
 * @param id the request identifier
 * @param method the method name to invoke
 * @param params optional parameters for the method
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcRequest(
    @NonNull String jsonrpc,
    @NonNull Object id,
    @NonNull String method,
    @JsonProperty("params") @Nullable Object params) {

  private static final String JSONRPC_VERSION = "2.0";

  /**
   * Creates a new JSON-RPC request.
   *
   * @param id the request identifier
   * @param method the method name
   * @param params the method parameters (can be null)
   * @return a new JsonRpcRequest
   */
  public static JsonRpcRequest create(
      @NonNull Object id, @NonNull String method, @Nullable Object params) {
    return new JsonRpcRequest(JSONRPC_VERSION, id, method, params);
  }

  /**
   * Creates a new JSON-RPC request without parameters.
   *
   * @param id the request identifier
   * @param method the method name
   * @return a new JsonRpcRequest
   */
  public static JsonRpcRequest create(@NonNull Object id, @NonNull String method) {
    return new JsonRpcRequest(JSONRPC_VERSION, id, method, null);
  }
}
