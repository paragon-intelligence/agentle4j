package com.paragon.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.mcp.dto.McpToolDefinition;
import com.paragon.mcp.dto.McpToolResult;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A remote MCP tool that extends {@link FunctionTool} for seamless agent integration.
 *
 * <p>This class wraps an MCP tool definition and client, allowing MCP tools to be used directly
 * with agents via {@code agent.builder().addTool(mcpRemoteTool)}.
 *
 * <h2>Usage with Agents</h2>
 *
 * <pre>{@code
 * // Get tools from MCP server
 * var mcp = StdioMcpClient.builder()
 *     .command("npx", "-y", "@modelcontextprotocol/server-filesystem")
 *     .build();
 * mcp.connect();
 *
 * List<McpRemoteTool> tools = mcp.asTools();
 *
 * // Add to agent
 * Agent agent = Agent.builder()
 *     .name("FileAgent")
 *     .addTools(tools)  // Works because McpRemoteTool extends FunctionTool
 *     .build();
 * }</pre>
 *
 * @see McpClient#asTools()
 * @see FunctionTool
 */
public final class McpRemoteTool extends FunctionTool<McpRemoteTool.McpParams> {

  private static final Logger log = LoggerFactory.getLogger(McpRemoteTool.class);

  /**
   * Empty record used as the type parameter for FunctionTool. The actual parameters are handled
   * dynamically via the raw JSON arguments from the LLM.
   */
  public record McpParams() {}

  private final McpClient client;
  private final McpToolDefinition definition;
  private final ObjectMapper objectMapper;
  private final String toolName;
  private final String toolDescription;

  /**
   * Creates a new remote MCP tool.
   *
   * @param client the MCP client to use for calling the tool
   * @param definition the tool definition from the MCP server
   * @param objectMapper the object mapper for JSON conversion
   */
  public McpRemoteTool(
      @NonNull McpClient client,
      @NonNull McpToolDefinition definition,
      @NonNull ObjectMapper objectMapper) {
    // Use the MCP schema directly - pass it to FunctionTool's manual constructor
    super(definition.inputSchema(), true);
    this.client = Objects.requireNonNull(client, "client cannot be null");
    this.definition = Objects.requireNonNull(definition, "definition cannot be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    this.toolName = definition.name();
    this.toolDescription = definition.description();
  }

  /**
   * Calls the MCP tool.
   *
   * <p>Note: The params argument is not used because MCP tools have dynamic schemas. The actual
   * arguments are extracted from the raw JSON provided by the LLM during execution.
   */
  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable McpParams params) {
    // This method is called but we need the raw JSON arguments.
    // Return null to indicate we need special handling.
    // The actual execution happens via callWithRawArguments or through the agent's tool store.
    log.warn(
        "call(McpParams) invoked on MCP tool '{}' - use callWithRawArguments() instead", toolName);
    return FunctionToolCallOutput.error(
        "MCP tools require raw JSON arguments. Use callWithRawArguments().");
  }

  /**
   * Calls the MCP tool with raw JSON arguments from the LLM.
   *
   * @param jsonArguments the arguments as a JSON string
   * @return the tool call output
   */
  @SuppressWarnings("unchecked")
  public @NonNull FunctionToolCallOutput callWithRawArguments(@Nullable String jsonArguments) {
    log.debug("Calling MCP tool: {} with arguments: {}", toolName, jsonArguments);

    try {
      Map<String, Object> arguments = null;
      if (jsonArguments != null && !jsonArguments.isBlank()) {
        arguments = objectMapper.readValue(jsonArguments, Map.class);
      }
      return callWithMap(arguments);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse JSON arguments for MCP tool {}", toolName, e);
      return FunctionToolCallOutput.error("Invalid JSON arguments: " + e.getMessage());
    }
  }

  /**
   * Calls the MCP tool with a map of arguments.
   *
   * @param arguments the arguments as a map
   * @return the tool call output
   */
  public @NonNull FunctionToolCallOutput callWithMap(@Nullable Map<String, Object> arguments) {
    log.debug("Calling MCP tool: {} with arguments: {}", toolName, arguments);

    try {
      McpToolResult result = client.callTool(toolName, arguments);

      if (result.isError()) {
        String errorText = result.getTextContent();
        log.warn("MCP tool {} returned error: {}", toolName, errorText);
        return FunctionToolCallOutput.error(errorText);
      }

      String text = result.getTextContent();
      log.debug("MCP tool {} returned: {}", toolName, text);
      return FunctionToolCallOutput.success(text);

    } catch (McpException e) {
      log.error("MCP tool {} call failed: {}", toolName, e.getMessage());
      return FunctionToolCallOutput.error(e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error calling MCP tool {}", toolName, e);
      return FunctionToolCallOutput.error("Unexpected error: " + e.getMessage());
    }
  }

  // Override getters to return MCP tool metadata

  @Override
  public @NonNull String getName() {
    return toolName;
  }

  @Override
  public @Nullable String getDescription() {
    return toolDescription;
  }

  @Override
  public @NonNull Map<String, Object> getParameters() {
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

  /**
   * Returns the input schema for this tool.
   *
   * @return the input schema as a map
   */
  public @NonNull Map<String, Object> getInputSchema() {
    return definition.inputSchema();
  }

  @Override
  public String toString() {
    return "McpRemoteTool[" + toolName + "]";
  }
}
