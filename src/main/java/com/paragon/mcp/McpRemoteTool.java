package com.paragon.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.mcp.dto.McpToolDefinition;
import com.paragon.mcp.dto.McpToolResult;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A remote MCP tool that can be used with Agentle4j agents.
 *
 * <p>This class wraps an MCP tool definition and client, allowing the tool to be called by agents.
 * Unlike {@link com.paragon.responses.spec.FunctionTool}, this class works with dynamic JSON
 * schemas from MCP servers rather than compile-time Java Records.
 *
 * <p>When the agent calls this tool, it forwards the call to the MCP server and returns the result.
 */
public final class McpRemoteTool {

  private static final Logger log = LoggerFactory.getLogger(McpRemoteTool.class);

  private final McpClient client;
  private final McpToolDefinition definition;
  private final ObjectMapper objectMapper;

  /**
   * Creates a new remote MCP tool.
   *
   * @param client the MCP client to use for calling the tool
   * @param definition the tool definition from the MCP server
   * @param objectMapper the object mapper for JSON conversion
   */
  McpRemoteTool(
      @NonNull McpClient client,
      @NonNull McpToolDefinition definition,
      @NonNull ObjectMapper objectMapper) {
    this.client = Objects.requireNonNull(client, "client cannot be null");
    this.definition = Objects.requireNonNull(definition, "definition cannot be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
  }

  /**
   * Calls this tool with the given arguments.
   *
   * @param arguments the tool arguments as a map
   * @return the tool call output
   */
  public @NonNull FunctionToolCallOutput call(@Nullable Map<String, Object> arguments) {
    log.debug("Calling MCP tool: {} with arguments: {}", definition.name(), arguments);

    try {
      McpToolResult result = client.callTool(definition.name(), arguments);

      if (result.isError()) {
        String errorText = result.getTextContent();
        log.warn("MCP tool {} returned error: {}", definition.name(), errorText);
        return FunctionToolCallOutput.error(errorText);
      }

      String text = result.getTextContent();
      log.debug("MCP tool {} returned: {}", definition.name(), text);
      return FunctionToolCallOutput.success(text);

    } catch (McpException e) {
      log.error("MCP tool {} call failed: {}", definition.name(), e.getMessage());
      return FunctionToolCallOutput.error(e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error calling MCP tool {}", definition.name(), e);
      return FunctionToolCallOutput.error("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Calls this tool with JSON string arguments.
   *
   * @param jsonArguments the tool arguments as a JSON string
   * @return the tool call output
   */
  @SuppressWarnings("unchecked")
  public @NonNull FunctionToolCallOutput callWithJson(@Nullable String jsonArguments) {
    try {
      Map<String, Object> arguments = null;
      if (jsonArguments != null && !jsonArguments.isBlank()) {
        arguments = objectMapper.readValue(jsonArguments, Map.class);
      }
      return call(arguments);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse JSON arguments for MCP tool {}", definition.name(), e);
      return FunctionToolCallOutput.error("Invalid JSON arguments: " + e.getMessage());
    }
  }

  /**
   * Returns the tool name.
   *
   * @return the tool name
   */
  public @NonNull String getName() {
    return definition.name();
  }

  /**
   * Returns the tool description.
   *
   * @return the tool description, or null if not provided
   */
  public @Nullable String getDescription() {
    return definition.description();
  }

  /**
   * Returns the input schema for this tool.
   *
   * @return the input schema as a map
   */
  public @NonNull Map<String, Object> getInputSchema() {
    return definition.inputSchema();
  }

  /**
   * Returns the MCP tool definition.
   *
   * @return the tool definition
   */
  public @NonNull McpToolDefinition getDefinition() {
    return definition;
  }

  @Override
  public String toString() {
    return "McpRemoteTool[" + definition.name() + "]";
  }
}
