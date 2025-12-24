package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A list of tools available on an MCP server.
 *
 * @param id The unique ID of the list.
 * @param serverLabel The label of the MCP server.
 * @param tools The tools available on the server.
 * @param error Error message if the server could not list tools.
 */
public record McpListTools(
    @NonNull String id,
    @NonNull String serverLabel,
    @NonNull List<McpListToolResult> tools,
    @Nullable String error)
    implements Item, ResponseOutput {}
