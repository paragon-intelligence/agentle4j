package com.paragon.responses.spec;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an exec action in a local shell
 *
 * @param command The command to run.
 * @param env Environment variables to set for the command.
 * @param timeoutMs Optional timeout in milliseconds for the command.
 * @param user Optional user to run the command as.
 * @param workingDirectory Optional working directory to run the command in.
 */
public record LocalShellExecAction(
    @NonNull List<String> command,
    Map<String, String> env,
    @Nullable Integer timeoutMs,
    @Nullable String user,
    @Nullable String workingDirectory)
    implements LocalShellAction {}
