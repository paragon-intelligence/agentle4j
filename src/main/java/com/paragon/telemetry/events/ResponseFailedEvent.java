package com.paragon.telemetry.events;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Event emitted when a respond() call fails with an error.
 *
 * <p>This event marks the end of a span with error status and includes exception details for
 * debugging.
 */
public record ResponseFailedEvent(
    @NonNull String sessionId,
    @NonNull String traceId,
    @NonNull String spanId,
    @Nullable String parentSpanId,
    long timestampNanos,
    long startTimestampNanos,
    @Nullable String model,
    @NonNull String errorType,
    @NonNull String errorMessage,
    @Nullable String stackTrace,
    @Nullable Integer httpStatusCode,
    @NonNull Map<String, Object> attributes)
    implements TelemetryEvent {

  /** Duration of the operation in nanoseconds before failure. */
  public long durationNanos() {
    return timestampNanos - startTimestampNanos;
  }

  /** Duration of the operation in milliseconds before failure. */
  public long durationMs() {
    return durationNanos() / 1_000_000L;
  }

  /** Creates a failed event from a started event and an exception. */
  public static @NonNull ResponseFailedEvent from(
      @NonNull ResponseStartedEvent startedEvent, @NonNull Throwable exception) {
    return new ResponseFailedEvent(
        startedEvent.sessionId(),
        startedEvent.traceId(),
        startedEvent.spanId(),
        startedEvent.parentSpanId(),
        System.currentTimeMillis() * 1_000_000L,
        startedEvent.timestampNanos(),
        startedEvent.model(),
        exception.getClass().getName(),
        exception.getMessage() != null ? exception.getMessage() : "Unknown error",
        getStackTraceString(exception),
        null,
        startedEvent.attributes());
  }

  /** Creates a failed event from a started event with HTTP error details. */
  public static @NonNull ResponseFailedEvent fromHttpError(
      @NonNull ResponseStartedEvent startedEvent,
      int httpStatusCode,
      @NonNull String errorMessage) {
    return new ResponseFailedEvent(
        startedEvent.sessionId(),
        startedEvent.traceId(),
        startedEvent.spanId(),
        startedEvent.parentSpanId(),
        System.currentTimeMillis() * 1_000_000L,
        startedEvent.timestampNanos(),
        startedEvent.model(),
        "HttpError",
        errorMessage,
        null,
        httpStatusCode,
        startedEvent.attributes());
  }

  private static @Nullable String getStackTraceString(@NonNull Throwable exception) {
    StringBuilder sb = new StringBuilder();
    for (StackTraceElement element : exception.getStackTrace()) {
      sb.append(element.toString()).append("\n");
      if (sb.length() > 2000) {
        sb.append("... truncated");
        break;
      }
    }
    return sb.toString();
  }
}
