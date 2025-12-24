package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Captured chunks of stdout and stderr output, along with their associated outcomes.
 *
 * @param outcome The {@link FunctionShellToolCallChunkExitOutcome} or {@link
 *     FunctionShellToolCallChunkTimeoutOutcome} outcome associated with this chunk.
 * @param stderr Captured stderr output for this chunk of the shell call.
 * @param stdout Captured stdout output for this chunk of the shell call.
 */
public record FunctionShellToolCallOutputChunk(
    @NonNull FunctionShellToolCallChunkOutcome outcome,
    @NonNull String stderr,
    @NonNull String stdout) {}
