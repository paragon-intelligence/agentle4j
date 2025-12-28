package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an agent execution fails.
 *
 * <p>Provides agent-specific context:
 * <ul>
 *   <li>{@link #agentName()} - Name of the agent that failed</li>
 *   <li>{@link #phase()} - Phase where failure occurred</li>
 *   <li>{@link #turnsCompleted()} - Number of turns completed before failure</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * AgentResult result = agent.interact("Hello").join();
 * if (result.isError() && result.error() instanceof AgentExecutionException e) {
 *     System.err.println("Agent " + e.agentName() + " failed in " + e.phase());
 *     System.err.println("Completed " + e.turnsCompleted() + " turns before failure");
 *     if (e.isRetryable()) {
 *         // Retry logic
 *     }
 * }
 * }</pre>
 */
public class AgentExecutionException extends AgentleException {

  /**
   * Execution phases where an agent can fail.
   */
  public enum Phase {
    /** Input guardrail validation failed. */
    INPUT_GUARDRAIL,
    /** LLM API call failed. */
    LLM_CALL,
    /** Tool execution failed. */
    TOOL_EXECUTION,
    /** Output guardrail validation failed. */
    OUTPUT_GUARDRAIL,
    /** Agent handoff failed. */
    HANDOFF,
    /** Response parsing failed. */
    PARSING,
    /** Max turns limit exceeded. */
    MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName;
  private final @NonNull Phase phase;
  private final int turnsCompleted;
  private final @Nullable String lastResponseId;

  /**
   * Creates a new AgentExecutionException.
   *
   * @param agentName the name of the agent that failed
   * @param phase the phase where failure occurred
   * @param turnsCompleted number of turns completed before failure
   * @param message the error message
   */
  public AgentExecutionException(
      @NonNull String agentName,
      @NonNull Phase phase,
      int turnsCompleted,
      @NonNull String message) {
    super(mapPhaseToErrorCode(phase), message, getSuggestionForPhase(phase), isPhaseRetryable(phase));
    this.agentName = agentName;
    this.phase = phase;
    this.turnsCompleted = turnsCompleted;
    this.lastResponseId = null;
  }

  /**
   * Creates a new AgentExecutionException with a cause.
   *
   * @param agentName the name of the agent that failed
   * @param phase the phase where failure occurred
   * @param turnsCompleted number of turns completed before failure
   * @param message the error message
   * @param cause the underlying cause
   */
  public AgentExecutionException(
      @NonNull String agentName,
      @NonNull Phase phase,
      int turnsCompleted,
      @NonNull String message,
      @NonNull Throwable cause) {
    super(mapPhaseToErrorCode(phase), message, cause, getSuggestionForPhase(phase), isPhaseRetryable(phase));
    this.agentName = agentName;
    this.phase = phase;
    this.turnsCompleted = turnsCompleted;
    this.lastResponseId = null;
  }

  /**
   * Creates a new AgentExecutionException with full context.
   *
   * @param agentName the name of the agent that failed
   * @param phase the phase where failure occurred
   * @param turnsCompleted number of turns completed before failure
   * @param lastResponseId the last response ID before failure
   * @param message the error message
   * @param cause the underlying cause
   */
  public AgentExecutionException(
      @NonNull String agentName,
      @NonNull Phase phase,
      int turnsCompleted,
      @Nullable String lastResponseId,
      @NonNull String message,
      @Nullable Throwable cause) {
    super(mapPhaseToErrorCode(phase), message, 
        cause != null ? cause : null, 
        getSuggestionForPhase(phase), isPhaseRetryable(phase));
    this.agentName = agentName;
    this.phase = phase;
    this.turnsCompleted = turnsCompleted;
    this.lastResponseId = lastResponseId;
  }

  // Factory methods for common cases

  /**
   * Creates an exception for max turns exceeded.
   *
   * @param agentName the agent name
   * @param maxTurns the max turns limit
   * @param turnsCompleted the turns completed
   * @return a new AgentExecutionException
   */
  public static AgentExecutionException maxTurnsExceeded(
      @NonNull String agentName, int maxTurns, int turnsCompleted) {
    return new AgentExecutionException(
        agentName,
        Phase.MAX_TURNS_EXCEEDED,
        turnsCompleted,
        String.format("Agent '%s' exceeded maximum turns (%d). Consider increasing maxTurns or simplifying the request.", 
            agentName, maxTurns));
  }

  /**
   * Creates an exception for LLM call failure.
   *
   * @param agentName the agent name
   * @param turnsCompleted turns completed before failure
   * @param cause the underlying cause
   * @return a new AgentExecutionException
   */
  public static AgentExecutionException llmCallFailed(
      @NonNull String agentName, int turnsCompleted, @NonNull Throwable cause) {
    return new AgentExecutionException(
        agentName,
        Phase.LLM_CALL,
        turnsCompleted,
        String.format("Agent '%s' LLM call failed: %s", agentName, cause.getMessage()),
        cause);
  }

  /**
   * Creates an exception for parsing failure.
   *
   * @param agentName the agent name
   * @param turnsCompleted turns completed before failure
   * @param cause the underlying cause
   * @return a new AgentExecutionException
   */
  public static AgentExecutionException parsingFailed(
      @NonNull String agentName, int turnsCompleted, @NonNull Throwable cause) {
    return new AgentExecutionException(
        agentName,
        Phase.PARSING,
        turnsCompleted,
        String.format("Agent '%s' failed to parse response: %s", agentName, cause.getMessage()),
        cause);
  }

  /**
   * Creates an exception for handoff failure.
   *
   * @param agentName the agent name
   * @param targetAgentName the target agent name
   * @param turnsCompleted turns completed before failure
   * @param cause the underlying cause
   * @return a new AgentExecutionException
   */
  public static AgentExecutionException handoffFailed(
      @NonNull String agentName, 
      @NonNull String targetAgentName, 
      int turnsCompleted, 
      @NonNull Throwable cause) {
    return new AgentExecutionException(
        agentName,
        Phase.HANDOFF,
        turnsCompleted,
        String.format("Agent '%s' handoff to '%s' failed: %s", agentName, targetAgentName, cause.getMessage()),
        cause);
  }

  /**
   * Returns the name of the agent that failed.
   *
   * @return the agent name
   */
  public @NonNull String agentName() {
    return agentName;
  }

  /**
   * Returns the phase where failure occurred.
   *
   * @return the failure phase
   */
  public @NonNull Phase phase() {
    return phase;
  }

  /**
   * Returns the number of turns completed before failure.
   *
   * @return the turns completed
   */
  public int turnsCompleted() {
    return turnsCompleted;
  }

  /**
   * Returns the last response ID before failure.
   *
   * @return the last response ID, or null if not available
   */
  public @Nullable String lastResponseId() {
    return lastResponseId;
  }

  private static ErrorCode mapPhaseToErrorCode(Phase phase) {
    return switch (phase) {
      case INPUT_GUARDRAIL, OUTPUT_GUARDRAIL -> ErrorCode.GUARDRAIL_VIOLATED;
      case TOOL_EXECUTION -> ErrorCode.TOOL_EXECUTION_FAILED;
      case MAX_TURNS_EXCEEDED -> ErrorCode.MAX_TURNS_EXCEEDED;
      case LLM_CALL -> ErrorCode.SERVER_ERROR;
      case HANDOFF, PARSING -> ErrorCode.UNKNOWN;
    };
  }

  private static @Nullable String getSuggestionForPhase(Phase phase) {
    return switch (phase) {
      case INPUT_GUARDRAIL -> "Rephrase your input to avoid triggering the guardrail";
      case OUTPUT_GUARDRAIL -> "The AI's response was blocked by a safety filter";
      case TOOL_EXECUTION -> "Check the tool implementation or arguments";
      case MAX_TURNS_EXCEEDED -> "Increase maxTurns or simplify the request";
      case LLM_CALL -> "Check API connectivity and credentials";
      case HANDOFF -> "Verify the target agent is properly configured";
      case PARSING -> "Check the response format matches the expected schema";
    };
  }

  private static boolean isPhaseRetryable(Phase phase) {
    return switch (phase) {
      case LLM_CALL -> true;  // Network errors may be transient
      case INPUT_GUARDRAIL, OUTPUT_GUARDRAIL, TOOL_EXECUTION, 
           MAX_TURNS_EXCEEDED, HANDOFF, PARSING -> false;
    };
  }
}
