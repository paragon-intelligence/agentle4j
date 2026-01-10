package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The result of an MCP tool call.
 *
 * @param content list of content items returned by the tool
 * @param isError whether the tool execution resulted in an error
 * @param structuredContent optional structured JSON content
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpToolResult(
    @NonNull List<McpContent> content, boolean isError, @Nullable Object structuredContent) {

  /**
   * Creates a successful text result.
   *
   * @param text the text content
   * @return a new McpToolResult
   */
  public static McpToolResult text(@NonNull String text) {
    return new McpToolResult(List.of(McpTextContent.of(text)), false, null);
  }

  /**
   * Creates an error result.
   *
   * @param errorMessage the error message
   * @return a new McpToolResult
   */
  public static McpToolResult error(@NonNull String errorMessage) {
    return new McpToolResult(List.of(McpTextContent.of(errorMessage)), true, null);
  }

  /**
   * Returns the text content concatenated, or empty string if no text content.
   *
   * @return the text content
   */
  public @NonNull String getTextContent() {
    StringBuilder sb = new StringBuilder();
    for (McpContent item : content) {
      if (item instanceof McpTextContent textContent) {
        if (!sb.isEmpty()) {
          sb.append("\n");
        }
        sb.append(textContent.text());
      }
    }
    return sb.toString();
  }
}
