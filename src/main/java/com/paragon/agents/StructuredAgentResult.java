package com.paragon.agents;

import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseInputItem;
import java.util.List;
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
public record StructuredAgentResult<T>(
    @Nullable T output,
    @NonNull String rawOutput,
    @NonNull Response finalResponse,
    @NonNull List<ResponseInputItem> history,
    @NonNull List<ToolExecution> toolExecutions,
    int turnsUsed,
    @Nullable Agent handoffAgent,
    @Nullable Throwable error) {

  /**
   * Whether the interaction was successful.
   *
   * @return true if no error occurred and no handoff was triggered
   */
  public boolean isSuccess() {
    return error == null && handoffAgent == null;
  }

  /**
   * Whether a handoff to another agent occurred.
   *
   * @return true if handoff was triggered
   */
  public boolean isHandoff() {
    return handoffAgent != null;
  }

  /**
   * Whether the interaction failed.
   *
   * @return true if an error occurred
   */
  public boolean isError() {
    return error != null;
  }

  /**
   * Returns the typed output, or throws the underlying error if the interaction failed.
   *
   * <p>Use this instead of {@link #output()} when you want fail-fast behaviour: it guarantees a
   * non-null return on success and surfaces the root cause on failure, preventing callers from
   * silently receiving {@code null} and propagating it downstream (e.g. as a tool result that
   * confuses the LLM and causes an infinite retry loop).
   *
   * @return the typed output (never null on this path)
   * @throws RuntimeException wrapping {@link #error()} if {@link #isError()} is true
   */
  public @NonNull T outputOrThrow() {
    if (error != null) {
      if (error instanceof RuntimeException re) throw re;
      throw new RuntimeException("Agent interaction failed: " + error.getMessage(), error);
    }
    if (output == null) {
      throw new RuntimeException("Agent returned null output with no error");
    }
    return output;
  }

  /**
   * Returns the error message if one occurred.
   *
   * @return the error message or null
   */
  public @Nullable String errorMessage() {
    return error != null ? error.getMessage() : null;
  }

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
      @NonNull Response response,
      @NonNull List<ResponseInputItem> history,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed) {
    return new StructuredAgentResult<>(
        output, rawOutput, response, history, toolExecutions, turnsUsed, null, null);
  }

  /**
   * Convenience method for creating a simple successful result (for testing).
   *
   * @param output the typed output
   * @param rawOutput the raw JSON/text output
   * @param <T> the output type
   * @return a minimal success result
   */
  @SuppressWarnings("unchecked")
  public static <T> @NonNull StructuredAgentResult<T> success(
      @NonNull T output, @NonNull String rawOutput) {
    return new StructuredAgentResult<>(
        output, rawOutput, null, List.of(), List.of(), 0, null, null);
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
    return new StructuredAgentResult<>(
        (T) null,
        rawOutput != null ? rawOutput : "",
        response,
        history,
        toolExecutions,
        turnsUsed,
        null,
        error);
  }

  /**
   * Convenience method for creating a simple error result (for testing).
   *
   * @param error the error that occurred
   * @param <T> the output type
   * @return a minimal error result
   */
  @SuppressWarnings("unchecked")
  public static <T> @NonNull StructuredAgentResult<T> error(@NonNull Throwable error) {
    return new StructuredAgentResult<>((T) null, "", null, List.of(), List.of(), 0, null, error);
  }
}
