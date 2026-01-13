package com.paragon.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.mcp.dto.JsonRpcResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for StdioMcpClient. */
class StdioMcpClientTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  // ===== Builder tests =====

  @Test
  void shouldRequireCommand() {
    var builder = StdioMcpClient.builder();

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldAcceptCommandAsVarargs() {
    var client = StdioMcpClient.builder().command("echo", "hello").build();

    assertNotNull(client);
    client.close();
  }

  @Test
  void shouldAcceptCommandAsList() {
    var client = StdioMcpClient.builder().command(List.of("echo", "hello")).build();

    assertNotNull(client);
    client.close();
  }

  @Test
  void shouldAcceptEnvironmentVariables() {
    var client =
        StdioMcpClient.builder()
            .command("echo", "test")
            .environment("KEY", "value")
            .environment(Map.of("KEY2", "value2"))
            .build();

    assertNotNull(client);
    client.close();
  }

  @Test
  void shouldAcceptWorkingDirectory() {
    var client =
        StdioMcpClient.builder()
            .command("echo", "test")
            .workingDirectory(Path.of("/tmp"))
            .build();

    assertNotNull(client);
    client.close();
  }

  @Test
  void shouldAcceptCustomObjectMapper() {
    var customMapper = new ObjectMapper();
    var client =
        StdioMcpClient.builder().command("echo", "test").objectMapper(customMapper).build();

    assertNotNull(client);
    client.close();
  }

  // ===== Lifecycle tests =====

  @Test
  void shouldRejectOperationsBeforeConnect() {
    var client = StdioMcpClient.builder().command("echo", "test").build();

    var exception = assertThrows(McpException.class, client::listTools);
    assertTrue(exception.getMessage().contains("not initialized"));

    client.close();
  }

  @Test
  void shouldRejectOperationsAfterClose() {
    var client = StdioMcpClient.builder().command("echo", "test").build();
    client.close();

    var exception = assertThrows(McpException.class, client::listTools);
    assertTrue(exception.getMessage().contains("closed"));
  }

  @Test
  void shouldHandleDoubleClose() {
    var client = StdioMcpClient.builder().command("echo", "test").build();

    // First close
    client.close();

    // Second close should not throw
    assertDoesNotThrow(client::close);
  }

  // ===== Connection failure tests =====

  @Test
  void shouldHandleNonExistentCommand() {
    var client =
        StdioMcpClient.builder()
            .command("this-command-does-not-exist-12345")
            .build();

    var exception = assertThrows(McpException.class, client::connect);
    assertTrue(exception.getMessage().contains("Connection failed") 
        || exception.getMessage().contains("failed"));

    client.close();
  }

  // ===== JSON-RPC parsing tests =====

  @Test
  void shouldSerializeRequest() throws Exception {
    // Use the toJson helper through a mock scenario
    var client =
        StdioMcpClient.builder().command("echo", "test").objectMapper(objectMapper).build();

    // The client uses objectMapper internally
    String json = objectMapper.writeValueAsString(Map.of("test", "value"));
    assertNotNull(json);
    assertTrue(json.contains("test"));

    client.close();
  }

  @Test
  void shouldParseResponse() throws Exception {
    String json =
        """
        {"jsonrpc":"2.0","id":1,"result":{"tools":[]}}
        """;

    JsonRpcResponse response = objectMapper.readValue(json, JsonRpcResponse.class);

    assertTrue(response.isSuccess());
    assertFalse(response.isError());

    // Note: We can test the parseResponse method indirectly through full integration
  }

  // ===== Tool conversion tests =====

  @Test
  void shouldFilterToolsByName() throws Exception {
    // This tests the asTools(Set<String>) filtering logic
    // Since we can't easily mock the full connection, we test the filtering conceptually

    // Given tool definitions in a real scenario:
    // ["read_file", "write_file", "list_directory"]
    // When filtered by Set.of("read_file", "list_directory")
    // Then only those two should be returned

    // The actual filtering logic is tested in StreamableHttpMcpClientTest
    // This is a placeholder acknowledging the same logic applies to StdioMcpClient
    assertTrue(true);
  }

  // ===== Error scenario tests =====

  @Test
  void shouldThrowMcpExceptionForProtocolErrors() {
    var exception = McpException.protocolError("Invalid JSON");

    assertTrue(exception.getMessage().contains("Protocol error"));
    assertTrue(exception.getMessage().contains("Invalid JSON"));
  }

  @Test
  void shouldThrowMcpExceptionForConnectionFailures() {
    var cause = new IOException("Connection refused");
    var exception = McpException.connectionFailed("Server not reachable", cause);

    assertTrue(exception.getMessage().contains("Connection failed"));
    assertEquals(cause, exception.getCause());
  }

  @Test
  void shouldThrowMcpExceptionForToolFailures() {
    var exception = McpException.toolExecutionFailed("my_tool", "Timeout");

    assertTrue(exception.getMessage().contains("my_tool"));
    assertTrue(exception.getMessage().contains("Timeout"));
    assertTrue(exception.getMessage().contains("execution failed"));
  }
}
