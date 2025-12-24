package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OTLP metric data point (gauge/counter value at a point in time).
 *
 * @see <a href="https://opentelemetry.io/docs/specs/otel/metrics/data-model/">OTEL Metrics Data
 *     Model</a>
 */
public record OtelDataPoint(
    long startTimeUnixNano,
    long timeUnixNano,
    @Nullable Long asInt,
    @Nullable Double asDouble,
    @NonNull List<OtelAttribute> attributes) {

  /** Creates a gauge data point with an integer value. */
  public static @NonNull OtelDataPoint gaugeInt(
      long value, @NonNull List<OtelAttribute> attributes) {
    long now = System.nanoTime();
    return new OtelDataPoint(now, now, value, null, attributes);
  }

  /** Creates a gauge data point with a double value. */
  public static @NonNull OtelDataPoint gaugeDouble(
      double value, @NonNull List<OtelAttribute> attributes) {
    long now = System.nanoTime();
    return new OtelDataPoint(now, now, null, value, attributes);
  }

  /** Creates a counter data point with an integer value. */
  public static @NonNull OtelDataPoint counterInt(
      long startTime, long value, @NonNull List<OtelAttribute> attributes) {
    return new OtelDataPoint(startTime, System.nanoTime(), value, null, attributes);
  }
}
