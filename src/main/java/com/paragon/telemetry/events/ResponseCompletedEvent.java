package com.paragon.telemetry.events;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Event emitted when a respond() call completes successfully.
 *
 * <p>This event marks the end of a span and includes metrics about the completed response (tokens,
 * latency, cost).
 */
public record ResponseCompletedEvent(
    @NonNull String sessionId,
    @NonNull String traceId,
    @NonNull String spanId,
    @Nullable String parentSpanId,
    long timestampNanos,
    long startTimestampNanos,
    @Nullable String model,
    @Nullable Integer inputTokens,
    @Nullable Integer outputTokens,
    @Nullable Integer totalTokens,
    @Nullable Double costUsd,
    @NonNull Map<String, Object> attributes)
    implements TelemetryEvent {

  /** Duration of the operation in nanoseconds. */
  public long durationNanos() {
    return timestampNanos - startTimestampNanos;
  }

  /** Duration of the operation in milliseconds. */
  public long durationMs() {
    return durationNanos() / 1_000_000L;
  }

  /** Creates a completed event from a started event. */
  public static @NonNull ResponseCompletedEvent from(
      @NonNull ResponseStartedEvent startedEvent,
      @Nullable Integer inputTokens,
      @Nullable Integer outputTokens,
      @Nullable Integer totalTokens,
      @Nullable Double costUsd) {
    return new ResponseCompletedEvent(
        startedEvent.sessionId(),
        startedEvent.traceId(),
        startedEvent.spanId(),
        startedEvent.parentSpanId(),
        System.currentTimeMillis() * 1_000_000L,
        startedEvent.timestampNanos(),
        startedEvent.model(),
        inputTokens,
        outputTokens,
        totalTokens,
        costUsd,
        startedEvent.attributes());
  }

  /** Creates a minimal completed event. */
  public static @NonNull ResponseCompletedEvent create(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      long startTimestampNanos,
      @Nullable String model) {
    return new ResponseCompletedEvent(
        sessionId,
        traceId,
        spanId,
        null,
        System.currentTimeMillis() * 1_000_000L,
        startTimestampNanos,
        model,
        null,
        null,
        null,
        null,
        Map.of());
  }
}
