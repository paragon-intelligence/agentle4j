package com.paragon.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for StreamableHttpMcpClient using MockWebServer. */
class StreamableHttpMcpClientTest {

  private MockWebServer server;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void shouldConnectAndInitialize() throws Exception {
    // Enqueue initialize response
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "result": {
                    "protocolVersion": "2025-11-25",
                    "serverInfo": {
                      "name": "TestServer",
                      "version": "1.0.0"
                    }
                  }
                }
                """));

    // Enqueue initialized notification response (202 Accepted)
    server.enqueue(new MockResponse().setResponseCode(202));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    client.connect();

    // Verify initialize request was sent
    RecordedRequest initRequest = server.takeRequest();
    assertEquals("POST", initRequest.getMethod());
    assertEquals("/mcp", initRequest.getPath());
    assertTrue(initRequest.getBody().readUtf8().contains("initialize"));

    // Verify initialized notification was sent
    RecordedRequest notificationRequest = server.takeRequest();
    assertTrue(notificationRequest.getBody().readUtf8().contains("notifications/initialized"));

    client.close();
  }

  @Test
  void shouldListTools() throws Exception {
    // Initialize responses
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","serverInfo":{"name":"Test","version":"1.0"}}}
                """));
    server.enqueue(new MockResponse().setResponseCode(202));

    // List tools response
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "result": {
                    "tools": [
                      {
                        "name": "get_weather",
                        "description": "Get weather information",
                        "inputSchema": {"type": "object", "properties": {"location": {"type": "string"}}}
                      },
                      {
                        "name": "search",
                        "description": "Search the web",
                        "inputSchema": {"type": "object"}
                      }
                    ]
                  }
                }
                """));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    client.connect();
    var tools = client.listTools();

    assertEquals(2, tools.size());
    assertEquals("get_weather", tools.get(0).name());
    assertEquals("search", tools.get(1).name());

    client.close();
  }

  @Test
  void shouldCallTool() throws Exception {
    // Initialize
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","serverInfo":{"name":"Test","version":"1.0"}}}
                """));
    server.enqueue(new MockResponse().setResponseCode(202));

    // Tool call response
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "result": {
                    "content": [{"type": "text", "text": "Weather in NYC: Sunny, 72°F"}],
                    "isError": false
                  }
                }
                """));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    client.connect();
    var result = client.callTool("get_weather", Map.of("location", "NYC"));

    assertFalse(result.isError());
    assertTrue(result.getTextContent().contains("72°F"));

    client.close();
  }

  @Test
  void shouldIncludeCustomHeaders() throws Exception {
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","serverInfo":{"name":"Test","version":"1.0"}}}
                """));
    server.enqueue(new MockResponse().setResponseCode(202));

    var client =
        StreamableHttpMcpClient.builder()
            .serverUrl(server.url("/mcp").toString())
            .headerProvider(McpHeaderProvider.of(Map.of("X-Custom-Header", "test-value")))
            .build();

    client.connect();

    RecordedRequest request = server.takeRequest();
    assertEquals("test-value", request.getHeader("X-Custom-Header"));

    client.close();
  }

  @Test
  void shouldIncludeSessionId() throws Exception {
    // Initialize with session ID
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .addHeader("MCP-Session-Id", "session-12345")
            .setBody(
                """
                {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","serverInfo":{"name":"Test","version":"1.0"}}}
                """));
    server.enqueue(new MockResponse().setResponseCode(202));

    // Second request to verify session ID is sent
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":2,"result":{"tools":[]}}
                """));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    client.connect();
    assertEquals("session-12345", client.getSessionId());

    client.listTools();

    // Skip init requests, check tools/list request
    server.takeRequest(); // initialize
    server.takeRequest(); // initialized notification
    RecordedRequest listRequest = server.takeRequest();
    assertEquals("session-12345", listRequest.getHeader("MCP-Session-Id"));

    client.close();
  }

  @Test
  void shouldHandleServerError() throws Exception {
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":1,"error":{"code":-32600,"message":"Invalid Request"}}
                """));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    var exception = assertThrows(McpException.class, client::connect);
    assertTrue(exception.getMessage().contains("Invalid Request"));
    assertEquals(-32600, exception.getErrorCode());

    client.close();
  }

  @Test
  void shouldHandleHttpError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    var exception = assertThrows(McpException.class, client::connect);
    assertTrue(exception.getMessage().contains("500"));

    client.close();
  }

  @Test
  void shouldRejectOperationsBeforeConnect() {
    var client = StreamableHttpMcpClient.builder().serverUrl("http://localhost:9999/mcp").build();

    var exception = assertThrows(McpException.class, client::listTools);
    assertTrue(exception.getMessage().contains("not initialized"));

    client.close();
  }

  @Test
  void shouldGetToolsAsRemoteTools() throws Exception {
    // Initialize
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","serverInfo":{"name":"Test","version":"1.0"}}}
                """));
    server.enqueue(new MockResponse().setResponseCode(202));

    // Tools list
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"tool1","inputSchema":{"type":"object"}},{"name":"tool2","inputSchema":{"type":"object"}}]}}
                """));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    client.connect();
    var tools = client.asTools();

    assertEquals(2, tools.size());
    assertEquals("tool1", tools.get(0).getName());
    assertEquals("tool2", tools.get(1).getName());

    client.close();
  }

  @Test
  void shouldFilterToolsByName() throws Exception {
    // Initialize
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","serverInfo":{"name":"Test","version":"1.0"}}}
                """));
    server.enqueue(new MockResponse().setResponseCode(202));

    // Tools list
    server.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(
                """
                {"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"allowed","inputSchema":{"type":"object"}},{"name":"not_allowed","inputSchema":{"type":"object"}}]}}
                """));

    var client = StreamableHttpMcpClient.builder().serverUrl(server.url("/mcp").toString()).build();

    client.connect();
    var tools = client.asTools(java.util.Set.of("allowed"));

    assertEquals(1, tools.size());
    assertEquals("allowed", tools.get(0).getName());

    client.close();
  }
}
