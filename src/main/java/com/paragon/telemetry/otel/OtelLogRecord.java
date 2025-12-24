package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OTLP log record.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/otel/logs/data-model/">OTEL Logs Data Model</a>
 */
public record OtelLogRecord(
    long timeUnixNano,
    long observedTimeUnixNano,
    int severityNumber,
    @Nullable String severityText,
    @Nullable String body,
    @NonNull List<OtelAttribute> attributes,
    @Nullable String traceId,
    @Nullable String spanId) {

  // Severity numbers as per OpenTelemetry specification
  public static final int SEVERITY_UNSPECIFIED = 0;
  public static final int SEVERITY_TRACE = 1;
  public static final int SEVERITY_DEBUG = 5;
  public static final int SEVERITY_INFO = 9;
  public static final int SEVERITY_WARN = 13;
  public static final int SEVERITY_ERROR = 17;
  public static final int SEVERITY_FATAL = 21;

  /** Creates an INFO log record. */
  public static @NonNull OtelLogRecord info(
      @NonNull String body,
      @NonNull List<OtelAttribute> attributes,
      @Nullable String traceId,
      @Nullable String spanId) {
    long now = System.nanoTime();
    return new OtelLogRecord(now, now, SEVERITY_INFO, "INFO", body, attributes, traceId, spanId);
  }

  /** Creates an ERROR log record. */
  public static @NonNull OtelLogRecord error(
      @NonNull String body,
      @NonNull List<OtelAttribute> attributes,
      @Nullable String traceId,
      @Nullable String spanId) {
    long now = System.nanoTime();
    return new OtelLogRecord(now, now, SEVERITY_ERROR, "ERROR", body, attributes, traceId, spanId);
  }

  /** Creates a DEBUG log record. */
  public static @NonNull OtelLogRecord debug(
      @NonNull String body,
      @NonNull List<OtelAttribute> attributes,
      @Nullable String traceId,
      @Nullable String spanId) {
    long now = System.nanoTime();
    return new OtelLogRecord(now, now, SEVERITY_DEBUG, "DEBUG", body, attributes, traceId, spanId);
  }
}
