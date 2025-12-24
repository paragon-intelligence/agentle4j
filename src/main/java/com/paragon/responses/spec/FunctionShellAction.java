package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The shell commands and limits that describe how to run the tool call.
 *
 * @param commands Ordered shell commands for the execution environment to run.
 * @param maxOutputLength Maximum number of UTF-8 characters to capture from combined stdout and
 *     stderr output.
 * @param timeoutMs Maximum wall-clock time in milliseconds to allow the shell commands to run.
 */
public record FunctionShellAction(
    @NonNull List<String> commands,
    @Nullable Integer maxOutputLength,
    @Nullable Integer timeoutMs) {}
