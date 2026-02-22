package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NonNull;

/**
 * MCP image content.
 *
 * @param data base64-encoded image data
 * @param mimeType the MIME type of the image (e.g., "image/png")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpImageContent(@NonNull String data, @NonNull String mimeType)
    implements McpContent {

  /**
   * Creates a new image content.
   *
   * @param data base64-encoded image data
   * @param mimeType the MIME type
   * @return a new McpImageContent
   */
  public static McpImageContent of(@NonNull String data, @NonNull String mimeType) {
    return new McpImageContent(data, mimeType);
  }
}
