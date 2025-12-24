package com.paragon.agents;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseInputItem;

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
    @NonNull T output,
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
}
