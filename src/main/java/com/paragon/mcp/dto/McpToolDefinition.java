package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An MCP tool definition as returned by the tools/list method.
 *
 * @param name the unique identifier for the tool
 * @param description human-readable description of the tool's functionality
 * @param inputSchema JSON Schema defining expected parameters
 * @param outputSchema optional JSON Schema defining expected output structure
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpToolDefinition(
    @NonNull String name,
    @Nullable String description,
    @NonNull Map<String, Object> inputSchema,
    @Nullable Map<String, Object> outputSchema) {

  /**
   * Creates a tool definition with only required fields.
   *
   * @param name the tool name
   * @param inputSchema the input schema
   * @return a new McpToolDefinition
   */
  public static McpToolDefinition of(
      @NonNull String name, @NonNull Map<String, Object> inputSchema) {
    return new McpToolDefinition(name, null, inputSchema, null);
  }

  /**
   * Creates a tool definition with description.
   *
   * @param name the tool name
   * @param description the tool description
   * @param inputSchema the input schema
   * @return a new McpToolDefinition
   */
  public static McpToolDefinition of(
      @NonNull String name,
      @NonNull String description,
      @NonNull Map<String, Object> inputSchema) {
    return new McpToolDefinition(name, description, inputSchema, null);
  }
}
