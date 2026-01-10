package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A JSON-RPC 2.0 response message.
 *
 * @param jsonrpc the JSON-RPC version (always "2.0")
 * @param id the request identifier this response corresponds to
 * @param result the result if the request succeeded (mutually exclusive with error)
 * @param error error information if the request failed (mutually exclusive with result)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcResponse(
    @NonNull String jsonrpc,
    @Nullable Object id,
    @Nullable Object result,
    @Nullable JsonRpcError error) {

  /**
   * Checks if this response indicates an error.
   *
   * @return true if this is an error response
   */
  public boolean isError() {
    return error != null;
  }

  /**
   * Checks if this response indicates success.
   *
   * @return true if this is a successful response
   */
  public boolean isSuccess() {
    return error == null && result != null;
  }
}
