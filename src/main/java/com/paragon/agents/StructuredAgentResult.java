package com.paragon.agents;

import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseInputItem;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The result of a structured agent interaction, containing the typed output.
 *
 * <p>This is the type-safe counterpart to {@link AgentResult} for agents with structured output.
 * The output is automatically deserialized to the specified type. Use {@link #parsedOptional()} or
 * {@link #parsedOr(Object)} when you want typed convenience access without throwing.
 *
 * @param <T> the type of the structured output
 * @since 1.0
 */
public class StructuredAgentResult<T> extends AgentResult {

  private final @Nullable T parsed;

  private StructuredAgentResult(AgentResult.Builder base, @Nullable T parsed) {
    super(base);
    this.parsed = parsed;
  }

  /**
   * Returns the parsed typed output from the agent.
   *
   * @return the parsed output (never null on success)
   * @throws IllegalStateException if the result is an error or the parsed value is unexpectedly
   *     null
   */
  @Override
  public @NonNull T parsed() {
    if (isError()) {
      throw new IllegalStateException(
          "Cannot get parsed output from a failed result: " + error().getMessage(), error());
    }
    if (parsed == null) {
      throw new IllegalStateException("Parsed output is null despite a successful result");
    }
    return parsed;
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
   * Returns the parsed typed output, if available.
   *
   * @return an Optional containing the parsed output
   */
  @Override
  public @NonNull Optional<T> parsedOptional() {
    return Optional.ofNullable(parsed);
  }

  /**
   * Returns the parsed output when available, or the provided fallback value.
   *
   * @param fallback the fallback value to use when parsed output is unavailable
   * @return the parsed output or the fallback
   */
  public @Nullable T parsedOr(@Nullable T fallback) {
    return parsed != null ? parsed : fallback;
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
            .parsed(output)
            .outputOrigin(OutputOrigin.LOCAL);
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
            .parsed(output)
            .outputOrigin(OutputOrigin.LOCAL);
    return new StructuredAgentResult<>(base, output);
  }

  static <T> @NonNull StructuredAgentResult<T> success(
      @NonNull T output, @NonNull AgentResult baseResult) {
    AgentResult.Builder base = AgentResult.Builder.from(baseResult).parsed(output);
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
            .error(error)
            .outputOrigin(OutputOrigin.LOCAL);
    return new StructuredAgentResult<>(base, (T) null);
  }

  static <T> @NonNull StructuredAgentResult<T> error(
      @NonNull Throwable error, @NonNull AgentResult baseResult) {
    AgentResult.Builder base = AgentResult.Builder.from(baseResult).error(error);
    return new StructuredAgentResult<>(base, null);
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
    AgentResult.Builder base = new AgentResult.Builder().error(error).outputOrigin(OutputOrigin.LOCAL);
    return new StructuredAgentResult<>(base, (T) null);
  }

  // ===== equals / hashCode =====

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StructuredAgentResult<?> other)) return false;
    return turnsUsed() == other.turnsUsed()
        && Objects.equals(parsed, other.parsed)
        && Objects.equals(super.output(), other.output())
        && Objects.equals(finalResponse(), other.finalResponse())
        && Objects.equals(history(), other.history())
        && Objects.equals(toolExecutions(), other.toolExecutions())
        && Objects.equals(handoffAgent(), other.handoffAgent())
        && Objects.equals(error(), other.error())
        && outputOrigin() == other.outputOrigin()
        && Objects.equals(outputProducerName(), other.outputProducerName())
        && Objects.equals(delegationPath(), other.delegationPath());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        parsed,
        super.output(),
        finalResponse(),
        history(),
        toolExecutions(),
        turnsUsed(),
        handoffAgent(),
        error(),
        outputOrigin(),
        outputProducerName(),
        delegationPath());
  }
}
