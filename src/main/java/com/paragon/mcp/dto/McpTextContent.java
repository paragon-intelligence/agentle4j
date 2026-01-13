package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NonNull;

/**
 * MCP text content.
 *
 * @param text the text content
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpTextContent(@NonNull String text) implements McpContent {

  /**
   * Creates a new text content.
   *
   * @param text the text content
   * @return a new McpTextContent
   */
  public static McpTextContent of(@NonNull String text) {
    return new McpTextContent(text);
  }
}
