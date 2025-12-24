package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A response to an MCP approval request.
 *
 * @param approvalRequestId The ID of the approval request being answered.
 * @param approve Whether the request was approved.
 * @param id The unique ID of the approval response
 * @param reason Optional reason for the decision.
 */
public record McpApprovalResponse(
    @NonNull String approvalRequestId,
    @NonNull Boolean approve,
    @Nullable String id,
    @Nullable String reason)
    implements Item {}
