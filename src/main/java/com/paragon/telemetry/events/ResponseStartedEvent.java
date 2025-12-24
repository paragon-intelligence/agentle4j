package com.paragon.telemetry.events;

import com.paragon.telemetry.TelemetryContext;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Event emitted when a respond() call begins.
 *
 * <p>This event marks the start of a span and includes information about the request being made
 * (model, input context).
 */
public record ResponseStartedEvent(
    @NonNull String sessionId,
    @NonNull String traceId,
    @NonNull String spanId,
    @Nullable String parentSpanId,
    long timestampNanos,
    @Nullable String model,
    @NonNull Map<String, Object> attributes)
    implements TelemetryEvent {

  /** Creates a minimal started event with only required fields. */
  public static @NonNull ResponseStartedEvent create(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      @Nullable String model) {
    return new ResponseStartedEvent(
        sessionId, traceId, spanId, null, System.currentTimeMillis() * 1_000_000L, model, Map.of());
  }

  /** Creates a started event with TelemetryContext for custom user metadata. */
  public static @NonNull ResponseStartedEvent create(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      @Nullable String model,
      @NonNull TelemetryContext context) {
    Map<String, Object> attrs = new HashMap<>(context.toAttributes());
    return new ResponseStartedEvent(
        sessionId,
        traceId,
        spanId,
        null,
        System.currentTimeMillis() * 1_000_000L,
        model,
        Map.copyOf(attrs));
  }

  /** Creates a started event with a parent span for nested operations. */
  public static @NonNull ResponseStartedEvent createWithParent(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      @NonNull String parentSpanId,
      @Nullable String model) {
    return new ResponseStartedEvent(
        sessionId,
        traceId,
        spanId,
        parentSpanId,
        System.currentTimeMillis() * 1_000_000L,
        model,
        Map.of());
  }
}
