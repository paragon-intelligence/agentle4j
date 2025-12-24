package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A tool call to a computer use tool. See the computer use guide for more information.
 *
 * @param callId The ID of the computer tool call that produced the output.
 * @param output A computer screenshot image used with the computer use tool.
 * @param acknowledgedSafetyChecks The safety checks reported by the API that have been acknowledged
 *     by the developer.
 * @param id The ID of the computer tool call output.
 * @param status The status of the message input. One of in_progress, completed, or incomplete.
 *     Populated when input items are returned via API.
 */
public record ComputerToolCallOutput(
    @NonNull String callId,
    @NonNull ComputerUseOutput output,
    @Nullable List<AcknowledgedSafetyCheck> acknowledgedSafetyChecks,
    @Nullable String id,
    @Nullable ComputerToolCallOutputStatus status)
    implements Item, ToolCallOutput {}
