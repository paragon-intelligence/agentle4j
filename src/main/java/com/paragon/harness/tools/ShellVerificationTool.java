package com.paragon.harness.tools;

import com.paragon.harness.VerificationResult;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link FunctionTool} that runs a fixed shell command and returns the result.
 *
 * <p><b>Security model:</b> The command is fixed at construction time by the developer.
 * The agent can only trigger execution — it cannot modify the command. This prevents
 * prompt injection attacks.
 *
 * <p>Example: let the agent run the project's test suite:
 *
 * <pre>{@code
 * ShellVerificationTool testTool = ShellVerificationTool.builder()
 *     .name("run_tests")
 *     .description("Run the project test suite and return results")
 *     .command("mvn", "test", "-q")
 *     .workingDir(Path.of("/my/project"))
 *     .timeoutSeconds(120)
 *     .build();
 *
 * Agent agent = Agent.builder()
 *     .addTool(testTool)
 *     .build();
 * }</pre>
 *
 * @see VerificationResult
 * @since 1.0
 */
public final class ShellVerificationTool extends FunctionTool<ShellVerificationTool.TriggerRequest> {

  /**
   * Empty record — the agent triggers the tool with no parameters.
   * The command is fixed at construction time.
   */
  public record TriggerRequest() {}

  private final String toolName;
  private final String toolDescription;
  private final List<String> command;
  private final Path workingDir; // nullable, but Path is not a type-annotatable qualified type
  private final int timeoutSeconds;

  private ShellVerificationTool(Builder builder) {
    super(buildSchema(), true);
    this.toolName = Objects.requireNonNull(builder.name, "name cannot be null");
    this.toolDescription = builder.description != null ? builder.description : "Run verification command";
    this.command = List.copyOf(Objects.requireNonNull(builder.command, "command cannot be null"));
    if (this.command.isEmpty()) throw new IllegalArgumentException("command cannot be empty");
    this.workingDir = builder.workingDir;
    this.timeoutSeconds = builder.timeoutSeconds > 0 ? builder.timeoutSeconds : 60;
  }

  private static Map<String, Object> buildSchema() {
    // TriggerRequest is an empty record — minimal schema
    return Map.of(
        "type", "object",
        "properties", Map.of(),
        "required", List.of(),
        "additionalProperties", false);
  }

  @Override
  public @NonNull String getName() {
    return toolName;
  }

  @Override
  public @Nullable String getDescription() {
    return toolDescription;
  }

  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable TriggerRequest params) {
    VerificationResult result = runCommand();
    return FunctionToolCallOutput.success(result.toSummary());
  }

  /**
   * Runs the configured command and returns the result. Can also be called programmatically.
   *
   * @return the verification result
   */
  public @NonNull VerificationResult runCommand() {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true); // Merge stderr into stdout

    if (workingDir != null) {
      pb.directory(workingDir.toFile());
    }

    try {
      Process process = pb.start();
      boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

      if (completed) {
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.exitValue();
        return exitCode == 0
            ? VerificationResult.pass(output)
            : VerificationResult.fail(output, exitCode);
      } else {
        process.destroyForcibly();
        return VerificationResult.fail("Command timed out after " + timeoutSeconds + " seconds", -1);
      }
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return VerificationResult.fail("Failed to run command: " + e.getMessage(), -2);
    }
  }

  /** Returns a new builder. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /** Builder for {@link ShellVerificationTool}. */
  public static final class Builder {
    private String name;
    private String description;
    private List<String> command;
    private Path workingDir;
    private int timeoutSeconds = 60;

    private Builder() {}

    /** Sets the tool name (as seen by the LLM). */
    public @NonNull Builder name(@NonNull String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    /** Sets the tool description. */
    public @NonNull Builder description(@NonNull String description) {
      this.description = description;
      return this;
    }

    /** Sets the command as a vararg of tokens (no shell expansion). */
    public @NonNull Builder command(@NonNull String... command) {
      this.command = List.of(command);
      return this;
    }

    /** Sets the command as a list. */
    public @NonNull Builder command(@NonNull List<String> command) {
      this.command = List.copyOf(command);
      return this;
    }

    /** Sets the working directory for the command. */
    public @NonNull Builder workingDir(@NonNull Path workingDir) {
      this.workingDir = workingDir;
      return this;
    }

    /** Sets the command timeout in seconds (default: 60). */
    public @NonNull Builder timeoutSeconds(int timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
      return this;
    }

    /** Builds the tool. */
    public @NonNull ShellVerificationTool build() {
      return new ShellVerificationTool(this);
    }
  }
}
