package com.paragon.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for McpHeaderProvider and McpException. */
class McpCoreTest {

  // ===== McpHeaderProvider tests =====

  @Test
  void shouldProvideStaticHeaders() {
    var provider = McpHeaderProvider.of(Map.of("X-API-Key", "secret123"));

    Map<String, String> headers = provider.getHeaders();

    assertEquals("secret123", headers.get("X-API-Key"));
  }

  @Test
  void shouldProvideBearerToken() {
    var provider = McpHeaderProvider.bearer(() -> "mytoken");

    Map<String, String> headers = provider.getHeaders();

    assertEquals("Bearer mytoken", headers.get("Authorization"));
  }

  @Test
  void shouldHandleNullTokenInBearerProvider() {
    var provider = McpHeaderProvider.bearer(() -> null);

    Map<String, String> headers = provider.getHeaders();

    assertTrue(headers.isEmpty());
  }

  @Test
  void shouldProvideEmptyHeaders() {
    var provider = McpHeaderProvider.empty();

    Map<String, String> headers = provider.getHeaders();

    assertTrue(headers.isEmpty());
  }

  @Test
  void shouldCombineHeaderProviders() {
    var provider1 = McpHeaderProvider.of(Map.of("X-Header-1", "value1"));
    var provider2 = McpHeaderProvider.of(Map.of("X-Header-2", "value2"));

    var combined = provider1.and(provider2);
    Map<String, String> headers = combined.getHeaders();

    assertEquals("value1", headers.get("X-Header-1"));
    assertEquals("value2", headers.get("X-Header-2"));
  }

  @Test
  void shouldProvideImmutableStaticHeaders() {
    var originalMap = Map.of("Key", "Value");
    var provider = McpHeaderProvider.of(originalMap);

    // Should return same value
    assertEquals("Value", provider.getHeaders().get("Key"));
  }

  // ===== McpException tests =====

  @Test
  void shouldCreateBasicException() {
    var ex = new McpException("Something failed");

    assertEquals("Something failed", ex.getMessage());
    assertNull(ex.getErrorCode());
    assertNull(ex.getCause());
  }

  @Test
  void shouldCreateExceptionWithCause() {
    var cause = new RuntimeException("Root cause");
    var ex = new McpException("Wrapper", cause);

    assertEquals("Wrapper", ex.getMessage());
    assertEquals(cause, ex.getCause());
    assertNull(ex.getErrorCode());
  }

  @Test
  void shouldCreateExceptionWithErrorCode() {
    var ex = new McpException("JSON-RPC error", -32601);

    assertEquals("JSON-RPC error", ex.getMessage());
    assertEquals(-32601, ex.getErrorCode());
  }

  @Test
  void shouldCreateConnectionFailedException() {
    var cause = new RuntimeException("Network error");
    var ex = McpException.connectionFailed("Could not connect", cause);

    assertTrue(ex.getMessage().contains("Connection failed"));
    assertTrue(ex.getMessage().contains("Could not connect"));
    assertEquals(cause, ex.getCause());
  }

  @Test
  void shouldCreateProtocolErrorException() {
    var ex = McpException.protocolError("Invalid JSON");

    assertTrue(ex.getMessage().contains("Protocol error"));
    assertTrue(ex.getMessage().contains("Invalid JSON"));
  }

  @Test
  void shouldCreateJsonRpcErrorException() {
    var ex = McpException.fromJsonRpcError(-32601, "Method not found");

    assertTrue(ex.getMessage().contains("-32601"));
    assertTrue(ex.getMessage().contains("Method not found"));
    assertEquals(-32601, ex.getErrorCode());
  }

  @Test
  void shouldCreateToolExecutionFailedException() {
    var ex = McpException.toolExecutionFailed("get_weather", "API rate limit exceeded");

    assertTrue(ex.getMessage().contains("get_weather"));
    assertTrue(ex.getMessage().contains("API rate limit exceeded"));
    assertTrue(ex.getMessage().contains("execution failed"));
  }
}
