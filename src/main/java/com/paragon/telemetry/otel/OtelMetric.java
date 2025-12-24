package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OTLP metric with its data points.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/otel/metrics/data-model/">OTEL Metrics Data
 *     Model</a>
 */
public record OtelMetric(
    @NonNull String name,
    @Nullable String description,
    @Nullable String unit,
    @Nullable OtelGauge gauge,
    @Nullable OtelSum sum) {

  /** Creates a gauge metric. */
  public static @NonNull OtelMetric gauge(
      @NonNull String name,
      @Nullable String description,
      @Nullable String unit,
      @NonNull List<OtelDataPoint> dataPoints) {
    return new OtelMetric(name, description, unit, new OtelGauge(dataPoints), null);
  }

  /** Creates a cumulative sum (counter) metric. */
  public static @NonNull OtelMetric counter(
      @NonNull String name,
      @Nullable String description,
      @Nullable String unit,
      @NonNull List<OtelDataPoint> dataPoints) {
    return new OtelMetric(
        name,
        description,
        unit,
        null,
        new OtelSum(dataPoints, "AGGREGATION_TEMPORALITY_CUMULATIVE", true));
  }

  /** Inner record for gauge metric data. */
  public record OtelGauge(@NonNull List<OtelDataPoint> dataPoints) {}

  /** Inner record for sum (counter) metric data. */
  public record OtelSum(
      @NonNull List<OtelDataPoint> dataPoints,
      @NonNull String aggregationTemporality,
      boolean isMonotonic) {}
}
