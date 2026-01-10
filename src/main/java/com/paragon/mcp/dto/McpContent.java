package com.paragon.mcp.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for MCP content types.
 *
 * <p>MCP tools can return different content types including text, images, and audio.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = McpTextContent.class, name = "text"),
  @JsonSubTypes.Type(value = McpImageContent.class, name = "image")
})
public sealed interface McpContent permits McpTextContent, McpImageContent {}
