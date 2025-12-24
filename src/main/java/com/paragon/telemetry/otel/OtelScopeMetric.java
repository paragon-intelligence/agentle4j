package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Groups metrics from a single instrumentation scope. */
public record OtelScopeMetric(@NonNull OtelScope scope, @NonNull List<OtelMetric> metrics) {

  /** Creates a scope metric with a single metric using default Agentle scope. */
  public static @NonNull OtelScopeMetric forMetric(@NonNull OtelMetric metric) {
    return new OtelScopeMetric(OtelScope.agentle(), List.of(metric));
  }

  /** Creates a scope metric with multiple metrics using default Agentle scope. */
  public static @NonNull OtelScopeMetric forMetrics(@NonNull List<OtelMetric> metrics) {
    return new OtelScopeMetric(OtelScope.agentle(), metrics);
  }
}
