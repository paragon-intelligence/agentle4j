package com.paragon.mcp;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;

/**
 * Provides HTTP headers for MCP HTTP transport.
 *
 * <p>This functional interface allows headers to be computed at runtime, which is useful for:
 *
 * <ul>
 *   <li>Authentication tokens that expire and need refresh
 *   <li>User context headers that change per request
 *   <li>Dynamic configuration based on runtime state
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Static headers
 * var provider = McpHeaderProvider.of(Map.of("X-API-Key", "secret"));
 *
 * // Bearer token with refresh
 * var provider = McpHeaderProvider.bearer(() -> authService.getAccessToken());
 *
 * // Custom runtime headers
 * McpHeaderProvider provider = () -> Map.of(
 *     "Authorization", "Bearer " + getToken(),
 *     "X-User-Id", getCurrentUserId()
 * );
 * }</pre>
 */
@FunctionalInterface
public interface McpHeaderProvider {

  /**
   * Returns the headers to include in MCP HTTP requests.
   *
   * @return a map of header names to values (never null, may be empty)
   */
  @NonNull Map<String, String> getHeaders();

  /**
   * Creates a header provider with static headers.
   *
   * @param headers the headers to provide
   * @return a new header provider
   */
  static McpHeaderProvider of(@NonNull Map<String, String> headers) {
    Objects.requireNonNull(headers, "headers cannot be null");
    var immutableHeaders = Map.copyOf(headers);
    return () -> immutableHeaders;
  }

  /**
   * Creates a header provider for Bearer token authentication.
   *
   * @param tokenSupplier supplies the bearer token (called on each request)
   * @return a new header provider
   */
  static McpHeaderProvider bearer(@NonNull Supplier<String> tokenSupplier) {
    Objects.requireNonNull(tokenSupplier, "tokenSupplier cannot be null");
    return () -> {
      String token = tokenSupplier.get();
      return token != null ? Map.of("Authorization", "Bearer " + token) : Map.of();
    };
  }

  /**
   * Creates an empty header provider (no headers).
   *
   * @return a header provider that returns an empty map
   */
  static McpHeaderProvider empty() {
    return Map::of;
  }

  /**
   * Combines this header provider with another, merging their headers.
   *
   * @param other the other header provider
   * @return a new header provider that combines both
   */
  default McpHeaderProvider and(@NonNull McpHeaderProvider other) {
    Objects.requireNonNull(other, "other cannot be null");
    return () -> {
      var combined = new java.util.HashMap<>(this.getHeaders());
      combined.putAll(other.getHeaders());
      return combined;
    };
  }
}
