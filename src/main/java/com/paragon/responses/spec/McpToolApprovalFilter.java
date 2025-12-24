package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Specify which of the MCP server's tools require approval.
 *
 * @param always A filter object to specify which tools are allowed.
 * @param never A filter object to specify which tools are allowed.
 */
public record McpToolApprovalFilter(@Nullable McpToolFilter always, @Nullable McpToolFilter never) {
  public static McpToolApprovalFilter always(@NonNull McpToolFilter always) {
    return new McpToolApprovalFilter(always, null);
  }

  public static McpToolApprovalFilter never(@NonNull McpToolFilter never) {
    return new McpToolApprovalFilter(never, null);
  }
}
