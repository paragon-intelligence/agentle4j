package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * A request for human approval of a tool invocation.
 *
 * @param arguments A JSON string of arguments for the tool.
 * @param id The unique ID of the approval request.
 * @param name The name of the tool to run.
 * @param serverLabel The label of the MCP server making the request.
 */
@com.fasterxml.jackson.annotation.JsonTypeName("mcp_approval_request")
public record McpApprovalRequest(
    @NonNull String arguments,
    @NonNull String id,
    @NonNull String name,
    @NonNull String serverLabel)
    implements Item, ResponseOutput {}
