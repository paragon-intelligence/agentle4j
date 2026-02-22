package com.paragon.mcp.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for JSON-RPC DTO serialization and deserialization. */
class JsonRpcDtoTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  // ===== JsonRpcRequest tests =====

  @Test
  void shouldSerializeRequest() throws Exception {
    var request = JsonRpcRequest.create(1, "tools/list", null);

    String json = objectMapper.writeValueAsString(request);

    assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
    assertTrue(json.contains("\"id\":1"));
    assertTrue(json.contains("\"method\":\"tools/list\""));
  }

  @Test
  void shouldSerializeRequestWithParams() throws Exception {
    var params = Map.of("name", "get_weather", "arguments", Map.of("location", "NYC"));
    var request = JsonRpcRequest.create("req-123", "tools/call", params);

    String json = objectMapper.writeValueAsString(request);

    assertTrue(json.contains("\"id\":\"req-123\""));
    assertTrue(json.contains("\"method\":\"tools/call\""));
    assertTrue(json.contains("\"name\":\"get_weather\""));
    assertTrue(json.contains("\"location\":\"NYC\""));
  }

  @Test
  void shouldDeserializeRequest() throws Exception {
    String json =
        """
        {
          "jsonrpc": "2.0",
          "id": 42,
          "method": "initialize",
          "params": {"protocolVersion": "2025-11-25"}
        }
        """;

    JsonRpcRequest request = objectMapper.readValue(json, JsonRpcRequest.class);

    assertEquals("2.0", request.jsonrpc());
    assertEquals(42, request.id());
    assertEquals("initialize", request.method());
    assertNotNull(request.params());
  }

  // ===== JsonRpcResponse tests =====

  @Test
  void shouldDeserializeSuccessResponse() throws Exception {
    String json =
        """
        {
          "jsonrpc": "2.0",
          "id": 1,
          "result": {
            "tools": [
              {"name": "test_tool", "description": "A test tool"}
            ]
          }
        }
        """;

    JsonRpcResponse response = objectMapper.readValue(json, JsonRpcResponse.class);

    assertTrue(response.isSuccess());
    assertFalse(response.isError());
    assertNotNull(response.result());
    assertNull(response.error());
  }

  @Test
  void shouldDeserializeErrorResponse() throws Exception {
    String json =
        """
        {
          "jsonrpc": "2.0",
          "id": 1,
          "error": {
            "code": -32601,
            "message": "Method not found"
          }
        }
        """;

    JsonRpcResponse response = objectMapper.readValue(json, JsonRpcResponse.class);

    assertFalse(response.isSuccess());
    assertTrue(response.isError());
    assertNull(response.result());
    assertNotNull(response.error());
    assertEquals(-32601, response.error().code());
    assertEquals("Method not found", response.error().message());
  }

  // ===== JsonRpcError tests =====

  @Test
  void shouldCreateStandardErrors() {
    assertEquals(-32700, JsonRpcError.PARSE_ERROR);
    assertEquals(-32600, JsonRpcError.INVALID_REQUEST);
    assertEquals(-32601, JsonRpcError.METHOD_NOT_FOUND);

    var parseError = JsonRpcError.parseError("Invalid JSON");
    assertEquals(-32700, parseError.code());
    assertTrue(parseError.message().contains("Invalid JSON"));

    var methodNotFound = JsonRpcError.methodNotFound("unknown_method");
    assertEquals(-32601, methodNotFound.code());
    assertTrue(methodNotFound.message().contains("unknown_method"));
  }

  // ===== McpToolDefinition tests =====

  @Test
  void shouldDeserializeToolDefinition() throws Exception {
    String json =
        """
        {
          "name": "get_weather",
          "description": "Get weather for a location",
          "inputSchema": {
            "type": "object",
            "properties": {
              "location": {"type": "string"}
            },
            "required": ["location"]
          }
        }
        """;

    McpToolDefinition tool = objectMapper.readValue(json, McpToolDefinition.class);

    assertEquals("get_weather", tool.name());
    assertEquals("Get weather for a location", tool.description());
    assertNotNull(tool.inputSchema());
    assertTrue(tool.inputSchema().containsKey("type"));
  }

  @Test
  void shouldCreateToolDefinitionWithFactoryMethods() {
    var schema = Map.<String, Object>of("type", "object");

    var toolWithSchema = McpToolDefinition.of("my_tool", schema);
    assertEquals("my_tool", toolWithSchema.name());
    assertNull(toolWithSchema.description());

    var toolWithDescription = McpToolDefinition.of("my_tool", "Does something", schema);
    assertEquals("my_tool", toolWithDescription.name());
    assertEquals("Does something", toolWithDescription.description());
  }

  // ===== McpToolResult tests =====

  @Test
  void shouldDeserializeToolResult() throws Exception {
    String json =
        """
        {
          "content": [
            {"type": "text", "text": "The weather is sunny"}
          ],
          "isError": false
        }
        """;

    McpToolResult result = objectMapper.readValue(json, McpToolResult.class);

    assertFalse(result.isError());
    assertEquals(1, result.content().size());
    assertInstanceOf(McpTextContent.class, result.content().getFirst());
    assertEquals("The weather is sunny", ((McpTextContent) result.content().getFirst()).text());
  }

  @Test
  void shouldGetTextContentFromResult() {
    var result = McpToolResult.text("Hello, world!");

    assertFalse(result.isError());
    assertEquals("Hello, world!", result.getTextContent());
  }

  @Test
  void shouldCreateErrorResult() {
    var result = McpToolResult.error("Something went wrong");

    assertTrue(result.isError());
    assertEquals("Something went wrong", result.getTextContent());
  }

  // ===== McpContent tests =====

  @Test
  void shouldDeserializeTextContent() throws Exception {
    String json =
        """
        {"type": "text", "text": "Hello"}
        """;

    McpContent content = objectMapper.readValue(json, McpContent.class);

    assertInstanceOf(McpTextContent.class, content);
    assertEquals("Hello", ((McpTextContent) content).text());
  }

  @Test
  void shouldDeserializeImageContent() throws Exception {
    String json =
        """
        {"type": "image", "data": "base64data", "mimeType": "image/png"}
        """;

    McpContent content = objectMapper.readValue(json, McpContent.class);

    assertInstanceOf(McpImageContent.class, content);
    McpImageContent imageContent = (McpImageContent) content;
    assertEquals("base64data", imageContent.data());
    assertEquals("image/png", imageContent.mimeType());
  }
}
