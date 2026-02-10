package com.paragon.agents;

import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseInputItem;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The result of an agent interaction, containing the final output and execution metadata.
 *
 * <p>AgentResult captures everything that happened during an agent run:
 *
 * <ul>
 *   <li>The final text output
 *   <li>The last API response from the LLM
 *   <li>Complete conversation history
 *   <li>All tool executions that occurred
 *   <li>Number of LLM turns used
 *   <li>Any handoff that was triggered
 *   <li>Any error that occurred
 *   <li>Related results from parallel/composite execution
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * AgentResult result = agent.interact("What's the status of order #12345?");
 *
 * if (result.isSuccess()) {
 *     System.out.println(result.output());
 * } else if (result.isHandoff()) {
 *     // Handoff was auto-executed, result contains final output
 *     System.out.println("Handled by: " + result.handoffAgent().name());
 *     System.out.println(result.output());
 * } else {
 *     System.err.println("Error: " + result.error().getMessage());
 * }
 * }</pre>
 *
 * @see Agent
 * @since 1.0
 */
public final class AgentResult {

  private final @Nullable String output;
  private final @Nullable Response finalResponse;
  private final @NonNull List<ResponseInputItem> history;
  private final @NonNull List<ToolExecution> toolExecutions;
  private final int turnsUsed;
  private final @Nullable Agent handoffAgent;
  private final @Nullable Throwable error;
  private final @Nullable Object parsed;
  private final @Nullable AgentRunState pausedState;
  private final @NonNull List<AgentResult> relatedResults;

  private AgentResult(Builder builder) {
    this.output = builder.output;
    this.finalResponse = builder.finalResponse;
    this.history = builder.history != null ? List.copyOf(builder.history) : List.of();
    this.toolExecutions =
            builder.toolExecutions != null ? List.copyOf(builder.toolExecutions) : List.of();
    this.turnsUsed = builder.turnsUsed;
    this.handoffAgent = builder.handoffAgent;
    this.error = builder.error;
    this.parsed = builder.parsed;
    this.pausedState = builder.pausedState;
    this.relatedResults =
            builder.relatedResults != null ? List.copyOf(builder.relatedResults) : List.of();
  }

  // ===== Factory Methods =====

  /**
   * Creates a successful result.
   *
   * @param output         the final text output
   * @param response       the final API response
   * @param context        the agent context with history
   * @param toolExecutions all tool executions
   * @param turnsUsed      number of LLM turns
   * @return a success result
   */
  public static @NonNull AgentResult success(
          @NonNull String output,
          @NonNull Response response,
          @NonNull AgenticContext context,
          @NonNull List<ToolExecution> toolExecutions,
          int turnsUsed) {
    return new Builder()
            .output(output)
            .finalResponse(response)
            .history(context.getHistory())
            .toolExecutions(toolExecutions)
            .turnsUsed(turnsUsed)
            .build();
  }

  /**
   * Convenience method for creating a simple successful result (for testing).
   *
   * @param output the final text output
   * @return a minimal success result
   */
  public static @NonNull AgentResult success(@NonNull String output) {
    return new Builder()
            .output(output)
            .build();
  }

  /**
   * Creates a successful result with parsed structured output.
   *
   * @param output         the final text output (JSON)
   * @param parsed         the parsed structured object
   * @param response       the final API response
   * @param context        the agent context with history
   * @param toolExecutions all tool executions
   * @param turnsUsed      number of LLM turns
   * @param <T>            the parsed type
   * @return a success result with parsed output
   */
  public static <T> @NonNull AgentResult successWithParsed(
          @NonNull String output,
          @NonNull T parsed,
          @NonNull Response response,
          @NonNull AgenticContext context,
          @NonNull List<ToolExecution> toolExecutions,
          int turnsUsed) {
    return new Builder()
            .output(output)
            .parsed(parsed)
            .finalResponse(response)
            .history(context.getHistory())
            .toolExecutions(toolExecutions)
            .turnsUsed(turnsUsed)
            .build();
  }

  /**
   * Creates a handoff result (after auto-executing the target agent).
   *
   * @param handoffAgent the agent that was handed off to
   * @param innerResult  the result from the handoff agent
   * @param context      the original context with combined history
   * @return a handoff result
   */
  public static @NonNull AgentResult handoff(
          @NonNull Agent handoffAgent,
          @NonNull AgentResult innerResult,
          @NonNull AgenticContext context) {
    return new Builder()
            .output(innerResult.output)
            .finalResponse(innerResult.finalResponse)
            .history(context.getHistory())
            .toolExecutions(innerResult.toolExecutions)
            .turnsUsed(innerResult.turnsUsed)
            .handoffAgent(handoffAgent)
            .parsed(innerResult.parsed)
            .build();
  }

  /**
   * Creates an error result.
   *
   * @param error     the error that occurred
   * @param context   the agent context at time of error
   * @param turnsUsed number of LLM turns before error
   * @return an error result
   */
  public static @NonNull AgentResult error(
          @NonNull Throwable error, @NonNull AgenticContext context, int turnsUsed) {
    return new Builder().error(error).history(context.getHistory()).turnsUsed(turnsUsed).build();
  }

  /**
   * Convenience method for creating a simple error result (for testing).
   *
   * @param error the error that occurred
   * @return a minimal error result
   */
  public static @NonNull AgentResult error(@NonNull Throwable error) {
    return new Builder().error(error).build();
  }

  /**
   * Creates an error result from a guardrail failure.
   *
   * @param reason  the guardrail failure reason
   * @param context the agent context
   * @return an error result
   */
  public static @NonNull AgentResult guardrailFailed(
          @NonNull String reason, @NonNull AgenticContext context) {
    return new Builder()
            .error(new GuardrailException(reason))
            .history(context.getHistory())
            .turnsUsed(0)
            .build();
  }

  /**
   * Creates a paused result for human-in-the-loop tool approval.
   *
   * <p>The run can be resumed later with {@code Agent.resume(state)}.
   *
   * @param state   the serializable run state
   * @param context the agent context
   * @return a paused result
   */
  public static @NonNull AgentResult paused(
          @NonNull AgentRunState state, @NonNull AgenticContext context) {
    return new Builder()
            .pausedState(state)
            .history(context.getHistory())
            .turnsUsed(context.getTurnCount())
            .build();
  }

  /**
   * Creates a composite result containing a primary result and related results.
   *
   * <p>This is useful for patterns like ParallelAgents where multiple agents run concurrently
   * and one result is selected as primary while the others are preserved as related results.
   *
   * @param primary the primary result (e.g., first to complete)
   * @param related the other related results
   * @return a result containing both primary and related results
   */
  public static @NonNull AgentResult composite(
          @NonNull AgentResult primary, @NonNull List<AgentResult> related) {
    return new Builder()
            .output(primary.output)
            .finalResponse(primary.finalResponse)
            .history(primary.history)
            .toolExecutions(primary.toolExecutions)
            .turnsUsed(primary.turnsUsed)
            .handoffAgent(primary.handoffAgent)
            .error(primary.error)
            .parsed(primary.parsed)
            .pausedState(primary.pausedState)
            .relatedResults(related)
            .build();
  }

  // ===== Accessors =====

  /**
   * Returns the final text output from the agent.
   *
   * @return the output text, or null if the run failed before producing output
   */
  public @Nullable String output() {
    return output;
  }

  /**
   * Returns the final API response.
   *
   * @return the last Response from the LLM, or null if not available
   */
  public @Nullable Response finalResponse() {
    return finalResponse;
  }

  /**
   * Returns the complete conversation history.
   *
   * @return an unmodifiable list of all messages
   */
  public @NonNull List<ResponseInputItem> history() {
    return history;
  }

  /**
   * Returns all tool executions that occurred during the run.
   *
   * @return an unmodifiable list of tool executions
   */
  public @NonNull List<ToolExecution> toolExecutions() {
    return toolExecutions;
  }

  /**
   * Returns the number of LLM turns used.
   *
   * @return the turn count
   */
  public int turnsUsed() {
    return turnsUsed;
  }

  /**
   * Returns the agent that was handed off to, if a handoff occurred.
   *
   * @return the handoff target agent, or null if no handoff
   */
  public @Nullable Agent handoffAgent() {
    return handoffAgent;
  }

  /**
   * Returns the error if the run failed.
   *
   * @return the error, or null if successful
   */
  public @Nullable Throwable error() {
    return error;
  }

  /**
   * Returns the parsed structured output if applicable.
   *
   * @param <T> the expected type
   * @return the parsed object, or null if not a structured output run
   */
  @SuppressWarnings("unchecked")
  public <T> @Nullable T parsed() {
    return (T) parsed;
  }

  // ===== Status Checks =====

  /**
   * Checks if this is a successful result.
   *
   * @return true if no error and no handoff occurred
   */
  public boolean isSuccess() {
    return error == null;
  }

  /**
   * Checks if a handoff occurred (to another agent).
   *
   * @return true if control was transferred to another agent
   */
  public boolean isHandoff() {
    return handoffAgent != null;
  }

  /**
   * Checks if an error occurred.
   *
   * @return true if an error occurred
   */
  public boolean isError() {
    return error != null;
  }

  /**
   * Checks if this result contains parsed structured output.
   *
   * @return true if parsed output is available
   */
  public boolean hasParsed() {
    return parsed != null;
  }

  /**
   * Checks if this run is paused waiting for approval.
   *
   * @return true if paused
   */
  public boolean isPaused() {
    return pausedState != null;
  }

  /**
   * Returns the paused state if this run is paused.
   *
   * @return the paused state, or null if not paused
   */
  public @Nullable AgentRunState pausedState() {
    return pausedState;
  }

  /**
   * Returns related results from parallel or composite execution.
   *
   * <p>When using patterns like parallel execution, this contains the results
   * from other agents that ran alongside the primary result.
   *
   * @return an unmodifiable list of related results (empty if none)
   */
  public @NonNull List<AgentResult> relatedResults() {
    return relatedResults;
  }

  /**
   * Checks if this result has related results from parallel execution.
   *
   * @return true if related results are present
   */
  public boolean hasRelatedResults() {
    return !relatedResults.isEmpty();
  }

  // ===== Builder =====

  private static final class Builder {
    private @Nullable String output;
    private @Nullable Response finalResponse;
    private @Nullable List<ResponseInputItem> history;
    private @Nullable List<ToolExecution> toolExecutions;
    private int turnsUsed;
    private @Nullable Agent handoffAgent;
    private @Nullable Throwable error;
    private @Nullable Object parsed;
    private @Nullable AgentRunState pausedState;
    private @Nullable List<AgentResult> relatedResults;

    Builder output(String output) {
      this.output = output;
      return this;
    }

    Builder finalResponse(Response response) {
      this.finalResponse = response;
      return this;
    }

    Builder history(List<ResponseInputItem> history) {
      this.history = history;
      return this;
    }

    Builder toolExecutions(List<ToolExecution> toolExecutions) {
      this.toolExecutions = toolExecutions;
      return this;
    }

    Builder turnsUsed(int turnsUsed) {
      this.turnsUsed = turnsUsed;
      return this;
    }

    Builder handoffAgent(Agent agent) {
      this.handoffAgent = agent;
      return this;
    }

    Builder error(Throwable error) {
      this.error = error;
      return this;
    }

    Builder parsed(Object parsed) {
      this.parsed = parsed;
      return this;
    }

    Builder pausedState(AgentRunState state) {
      this.pausedState = state;
      return this;
    }

    Builder relatedResults(List<AgentResult> relatedResults) {
      this.relatedResults = relatedResults;
      return this;
    }

    AgentResult build() {
      return new AgentResult(this);
    }
  }

  /**
   * Exception thrown when a guardrail validation fails.
   */
  public static final class GuardrailException extends RuntimeException {
    public GuardrailException(String message) {
      super(message);
    }
  }
}
