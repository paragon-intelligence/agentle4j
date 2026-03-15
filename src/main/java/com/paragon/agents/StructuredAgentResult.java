package com.paragon.agents;

import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseInputItem;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The result of a structured agent interaction, containing the typed output.
 *
 * <p>This is the type-safe counterpart to {@link AgentResult} for agents with structured output.
 * The output is automatically deserialized to the specified type.
 *
 * @param <T> the type of the structured output
 * @since 1.0
 */
public class StructuredAgentResult<T> extends AgentResult {

  private final @Nullable T typedOutput;

  private StructuredAgentResult(AgentResult.Builder base, @Nullable T typedOutput) {
    super(base);
    this.typedOutput = typedOutput;
  }

  /**
   * Returns the typed output from the agent.
   *
   * @return the typed output, or null if the run failed before producing output
   */
  public @Nullable T typedOutput() {
    return typedOutput;
  }

  /**
   * Returns the raw string output (alias for {@link AgentResult#output()}).
   *
   * @return the raw text output, or null if not available
   */
  public @Nullable String rawOutput() {
    return super.output();
  }

  /**
   * Whether the interaction was successful.
   *
   * @return true if no error occurred and no handoff was triggered
   */
  @Override
  public boolean isSuccess() {
    return error() == null && handoffAgent() == null;
  }

  /**
   * Returns the typed output, or throws the underlying error if the interaction failed.
   *
   * <p>Use this instead of {@link #typedOutput()} when you want fail-fast behaviour: it guarantees
   * a non-null return on success and surfaces the root cause on failure.
   *
   * @return the typed output (never null on this path)
   * @throws RuntimeException wrapping {@link #error()} if {@link #isError()} is true
   */
  public @NonNull T outputOrThrow() {
    if (error() != null) {
      Throwable e = error();
      if (e instanceof RuntimeException re) throw re;
      throw new RuntimeException("Agent interaction failed: " + e.getMessage(), e);
    }
    if (typedOutput == null) {
      throw new RuntimeException("Agent returned null output with no error");
    }
    return typedOutput;
  }

  /**
   * Returns the error message if one occurred.
   *
   * @return the error message or null
   */
  public @Nullable String errorMessage() {
    return error() != null ? error().getMessage() : null;
  }

  // ===== Factory Methods =====

  /**
   * Creates a successful result.
   *
   * @param output the typed output
   * @param rawOutput the raw JSON/text output
   * @param response the final response
   * @param history the conversation history
   * @param toolExecutions the tool executions
   * @param turnsUsed number of turns used
   * @param <T> the output type
   * @return a success result
   */
  public static <T> @NonNull StructuredAgentResult<T> success(
      @NonNull T output,
      @NonNull String rawOutput,
      @Nullable Response response,
      @NonNull List<ResponseInputItem> history,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed) {
    AgentResult.Builder base =
        new AgentResult.Builder()
            .output(rawOutput)
            .finalResponse(response)
            .history(history)
            .toolExecutions(toolExecutions)
            .turnsUsed(turnsUsed)
            .parsed(output);
    return new StructuredAgentResult<>(base, output);
  }

  /**
   * Convenience method for creating a simple successful result (for testing).
   *
   * @param output the typed output
   * @param rawOutput the raw JSON/text output
   * @param <T> the output type
   * @return a minimal success result
   */
  public static <T> @NonNull StructuredAgentResult<T> success(
      @NonNull T output, @NonNull String rawOutput) {
    AgentResult.Builder base =
        new AgentResult.Builder()
            .output(rawOutput)
            .parsed(output);
    return new StructuredAgentResult<>(base, output);
  }

  /**
   * Creates an error result.
   *
   * @param error the error that occurred
   * @param rawOutput the raw output if any
   * @param response the last response if any
   * @param history the conversation history
   * @param toolExecutions the tool executions
   * @param turnsUsed number of turns used
   * @param <T> the output type
   * @return an error result
   */
  @SuppressWarnings("unchecked")
  public static <T> @NonNull StructuredAgentResult<T> error(
      @NonNull Throwable error,
      @Nullable String rawOutput,
      @Nullable Response response,
      @NonNull List<ResponseInputItem> history,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed) {
    AgentResult.Builder base =
        new AgentResult.Builder()
            .output(rawOutput != null ? rawOutput : "")
            .finalResponse(response)
            .history(history)
            .toolExecutions(toolExecutions)
            .turnsUsed(turnsUsed)
            .error(error);
    return new StructuredAgentResult<>(base, (T) null);
  }

  /**
   * Convenience method for creating a simple error result (for testing).
   *
   * @param error the error that occurred
   * @param <T> the output type
   * @return a minimal error result
   */
  @SuppressWarnings("unchecked")
  public static <T> @NonNull StructuredAgentResult<T> structuredError(@NonNull Throwable error) {
    AgentResult.Builder base = new AgentResult.Builder().error(error);
    return new StructuredAgentResult<>(base, (T) null);
  }

  // ===== equals / hashCode =====

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StructuredAgentResult<?> other)) return false;
    return turnsUsed() == other.turnsUsed()
        && Objects.equals(typedOutput, other.typedOutput)
        && Objects.equals(super.output(), other.output())
        && Objects.equals(finalResponse(), other.finalResponse())
        && Objects.equals(history(), other.history())
        && Objects.equals(toolExecutions(), other.toolExecutions())
        && Objects.equals(handoffAgent(), other.handoffAgent())
        && Objects.equals(error(), other.error());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        typedOutput,
        super.output(),
        finalResponse(),
        history(),
        toolExecutions(),
        turnsUsed(),
        handoffAgent(),
        error());
  }
}
