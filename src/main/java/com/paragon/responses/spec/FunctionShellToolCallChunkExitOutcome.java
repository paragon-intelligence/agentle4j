package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Indicates that the shell commands finished and returned an exit code.
 *
 * @param exitCode The exit code returned by the shell process.
 */
public record FunctionShellToolCallChunkExitOutcome(@NonNull Integer exitCode)
    implements FunctionShellToolCallChunkOutcome {}
