package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * List of allowed tool names or a filter object.
 *
 * @param readOnly Indicates whether a tool modifies data or is read-only. If an MCP server is
 *     annotated with
 * @param toolNames List of allowed tool names.
 */
public record McpToolFilter(@Nullable Boolean readOnly, List<String> toolNames) {}
