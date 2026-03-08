package com.paragon.harness;

import org.jspecify.annotations.NonNull;

/**
 * The result of running a verification command (test suite, linter, etc.).
 *
 * @param passed whether the command exited with code 0
 * @param output combined stdout and stderr from the command
 * @param exitCode the process exit code
 * @see com.paragon.harness.tools.ShellVerificationTool
 * @since 1.0
 */
public record VerificationResult(boolean passed, @NonNull String output, int exitCode) {

  /** Creates a passing result. */
  public static @NonNull VerificationResult pass(@NonNull String output) {
    return new VerificationResult(true, output, 0);
  }

  /** Creates a failing result with the given exit code. */
  public static @NonNull VerificationResult fail(@NonNull String output, int exitCode) {
    return new VerificationResult(false, output, exitCode);
  }

  /** Returns a concise summary suitable for injection into agent context. */
  public @NonNull String toSummary() {
    String status = passed ? "PASSED" : "FAILED (exit code " + exitCode + ")";
    return "Verification " + status + "\n\n" + output;
  }
}
