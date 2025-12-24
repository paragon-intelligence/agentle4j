package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Top-level structure for OTLP/HTTP metrics export. */
public record OtelMetricsExportRequest(@NonNull List<OtelResourceMetric> resourceMetrics) {

  /** Creates an export request for a list of metrics. */
  public static @NonNull OtelMetricsExportRequest forMetrics(@NonNull List<OtelMetric> metrics) {
    return new OtelMetricsExportRequest(List.of(OtelResourceMetric.forMetrics(metrics)));
  }

  /** Creates an export request for a single metric. */
  public static @NonNull OtelMetricsExportRequest forMetric(@NonNull OtelMetric metric) {
    return forMetrics(List.of(metric));
  }
}
