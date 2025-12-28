package com.paragon.telemetry.events;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base sealed interface for all telemetry events emitted by the Responder.
 *
 * <p>Each event carries OpenTelemetry-compatible identifiers (traceId, spanId) and a sessionId for
 * correlating events within a single respond() call.
 *
 * <p>Events are designed to be immutable and thread-safe for async processing.
 */
public sealed interface TelemetryEvent
    permits ResponseStartedEvent, ResponseCompletedEvent, ResponseFailedEvent, AgentFailedEvent {

  /**
   * Unique session identifier for correlating events within a single respond() call. Either
   * provided by the user or auto-generated as a UUID.
   */
  @NonNull
  String sessionId();

  /**
   * OpenTelemetry trace ID (16-byte hex string, 32 characters). Groups all spans that are part of
   * the same distributed trace.
   */
  @NonNull
  String traceId();

  /**
   * OpenTelemetry span ID (8-byte hex string, 16 characters). Unique identifier for this specific
   * span/event.
   */
  @NonNull
  String spanId();

  /** Parent span ID for trace correlation, null if this is the root span. */
  @Nullable
  String parentSpanId();

  /**
   * Event timestamp in nanoseconds since Unix epoch. Used for precise timing in OpenTelemetry
   * exporters.
   */
  long timestampNanos();

  /** Additional key-value attributes for the event. Follows OpenTelemetry semantic conventions. */
  @NonNull
  Map<String, Object> attributes();
}
