package com.paragon.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.mcp.dto.McpContent;
import com.paragon.mcp.dto.McpTextContent;
import com.paragon.mcp.dto.McpToolDefinition;
import com.paragon.mcp.dto.McpToolResult;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for McpRemoteTool. */
class McpRemoteToolTest {

  @Mock private McpClient mockClient;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    objectMapper = new ObjectMapper();
  }

  @Test
  void shouldCallToolSuccessfully() throws Exception {
    var definition = McpToolDefinition.of("test_tool", "A test tool", Map.of("type", "object"));
    var tool = new McpRemoteTool(mockClient, definition, objectMapper);

    var result = new McpToolResult(List.of(McpTextContent.of("Success!")), false, null);
    when(mockClient.callTool(eq("test_tool"), any())).thenReturn(result);

    var arguments = Map.<String, Object>of("key", "value");
    FunctionToolCallOutput output = tool.call(arguments);

    assertEquals("Success!", output.output().toString());
    verify(mockClient).callTool("test_tool", arguments);
  }

  @Test
  void shouldHandleToolError() throws Exception {
    var definition = McpToolDefinition.of("failing_tool", Map.of("type", "object"));
    var tool = new McpRemoteTool(mockClient, definition, objectMapper);

    var result = new McpToolResult(List.of(McpTextContent.of("Error occurred")), true, null);
    when(mockClient.callTool(eq("failing_tool"), any())).thenReturn(result);

    FunctionToolCallOutput output = tool.call(null);

    assertTrue(output.output().toString().contains("Error"));
    verify(mockClient).callTool("failing_tool", null);
  }

  @Test
  void shouldHandleMcpException() throws Exception {
    var definition = McpToolDefinition.of("exception_tool", Map.of("type", "object"));
    var tool = new McpRemoteTool(mockClient, definition, objectMapper);

    when(mockClient.callTool(eq("exception_tool"), any()))
        .thenThrow(new McpException("Connection failed"));

    FunctionToolCallOutput output = tool.call(null);

    assertTrue(output.output().toString().contains("Connection failed"));
  }

  @Test
  void shouldCallWithJsonArguments() throws Exception {
    var definition = McpToolDefinition.of("json_tool", Map.of("type", "object"));
    var tool = new McpRemoteTool(mockClient, definition, objectMapper);

    var result = new McpToolResult(List.of(McpTextContent.of("OK")), false, null);
    when(mockClient.callTool(eq("json_tool"), any())).thenReturn(result);

    FunctionToolCallOutput output = tool.callWithJson("{\"foo\": \"bar\"}");

    assertEquals("OK", output.output().toString());
    verify(mockClient).callTool(eq("json_tool"), argThat(map -> "bar".equals(map.get("foo"))));
  }

  @Test
  void shouldHandleInvalidJsonArguments() {
    var definition = McpToolDefinition.of("json_tool", Map.of("type", "object"));
    var tool = new McpRemoteTool(mockClient, definition, objectMapper);

    FunctionToolCallOutput output = tool.callWithJson("not valid json");

    assertTrue(output.output().toString().contains("Invalid JSON"));
    verifyNoInteractions(mockClient);
  }

  @Test
  void shouldReturnToolProperties() {
    var schema = Map.<String, Object>of("type", "object", "properties", Map.of());
    var definition = McpToolDefinition.of("my_tool", "My tool description", schema);
    var tool = new McpRemoteTool(mockClient, definition, objectMapper);

    assertEquals("my_tool", tool.getName());
    assertEquals("My tool description", tool.getDescription());
    assertEquals(schema, tool.getInputSchema());
    assertEquals(definition, tool.getDefinition());
  }

  @Test
  void shouldHaveReadableToString() {
    var definition = McpToolDefinition.of("readable_tool", Map.of("type", "object"));
    var tool = new McpRemoteTool(mockClient, definition, objectMapper);

    String str = tool.toString();

    assertTrue(str.contains("readable_tool"));
    assertTrue(str.contains("McpRemoteTool"));
  }
}
