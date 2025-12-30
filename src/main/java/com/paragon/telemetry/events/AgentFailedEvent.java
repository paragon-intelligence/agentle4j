package com.paragon.telemetry.events;

import com.paragon.responses.exception.AgentExecutionException;
import com.paragon.responses.exception.AgentleException;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Telemetry event emitted when an agent execution fails.
 *
 * <p>Contains context about the failure including agent name, phase, error details, and trace
 * correlation.
 *
 * @param agentName the name of the agent that failed
 * @param sessionId the session ID for correlation
 * @param traceId the trace ID for correlation
 * @param spanId the span ID for correlation
 * @param parentSpanId the parent span ID for correlation
 * @param timestampNanos the event timestamp in nanoseconds
 * @param phase the phase where failure occurred
 * @param errorCode the machine-readable error code
 * @param errorMessage the human-readable error message
 * @param turnsCompleted number of turns completed before failure
 * @param suggestion optional resolution hint
 */
public record AgentFailedEvent(
    @NonNull String agentName,
    @NonNull String sessionId,
    @NonNull String traceId,
    @NonNull String spanId,
    @Nullable String parentSpanId,
    long timestampNanos,
    @NonNull String phase,
    @NonNull String errorCode,
    @NonNull String errorMessage,
    int turnsCompleted,
    @Nullable String suggestion)
    implements TelemetryEvent {

  @Override
  public @NonNull Map<String, Object> attributes() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("agent.name", agentName);
    attrs.put("agent.phase", phase);
    attrs.put("agent.turns_completed", turnsCompleted);
    attrs.put("error.code", errorCode);
    attrs.put("error.message", errorMessage);
    if (suggestion != null) {
      attrs.put("error.suggestion", suggestion);
    }
    return attrs;
  }

  /**
   * Creates an AgentFailedEvent from an AgentExecutionException.
   *
   * @param exception the exception
   * @param sessionId the session ID
   * @param traceId the trace ID
   * @param spanId the span ID
   * @param parentSpanId the parent span ID
   * @return a new AgentFailedEvent
   */
  public static AgentFailedEvent from(
      @NonNull AgentExecutionException exception,
      @NonNull String sessionId,
      @Nullable String traceId,
      @Nullable String spanId,
      @Nullable String parentSpanId) {
    return new AgentFailedEvent(
        exception.agentName(),
        sessionId,
        traceId != null ? traceId : "",
        spanId != null ? spanId : "",
        parentSpanId,
        System.nanoTime(),
        exception.phase().name(),
        exception.code().name(),
        exception.getMessage(),
        exception.turnsCompleted(),
        exception.suggestion());
  }

  /**
   * Creates an AgentFailedEvent from a generic exception.
   *
   * @param agentName the agent name
   * @param turnsCompleted turns completed before failure
   * @param exception the exception
   * @param sessionId the session ID
   * @param traceId the trace ID
   * @param spanId the span ID
   * @param parentSpanId the parent span ID
   * @return a new AgentFailedEvent
   */
  public static AgentFailedEvent from(
      @NonNull String agentName,
      int turnsCompleted,
      @NonNull Throwable exception,
      @NonNull String sessionId,
      @Nullable String traceId,
      @Nullable String spanId,
      @Nullable String parentSpanId) {

    String errorCode = "UNKNOWN";
    String suggestion = null;
    String phase = "UNKNOWN";

    if (exception instanceof AgentExecutionException agentEx) {
      errorCode = agentEx.code().name();
      suggestion = agentEx.suggestion();
      phase = agentEx.phase().name();
    } else if (exception instanceof AgentleException agentleEx) {
      errorCode = agentleEx.code().name();
      suggestion = agentleEx.suggestion();
    }

    return new AgentFailedEvent(
        agentName,
        sessionId,
        traceId != null ? traceId : "",
        spanId != null ? spanId : "",
        parentSpanId,
        System.nanoTime(),
        phase,
        errorCode,
        exception.getMessage() != null
            ? exception.getMessage()
            : exception.getClass().getSimpleName(),
        turnsCompleted,
        suggestion);
  }
}
