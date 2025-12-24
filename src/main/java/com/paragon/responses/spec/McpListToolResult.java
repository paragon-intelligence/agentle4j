package com.paragon.responses.spec;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The tools available on the server.
 *
 * @param inputSchema The JSON schema describing the tool's input.
 * @param name The name of the tool.
 * @param annotations Additional annotations about the tool.
 * @param description The description of the tool.
 */
public record McpListToolResult(
    @NonNull Map<String, Object> inputSchema,
    @NonNull String name,
    @Nullable Map<String, Object> annotations,
    @Nullable String description) {}
