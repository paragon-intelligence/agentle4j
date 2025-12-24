package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Groups scope metrics by their originating resource. */
public record OtelResourceMetric(
    @NonNull OtelResource resource, @NonNull List<OtelScopeMetric> scopeMetrics) {

  /** Creates a resource metric with default Agentle resource. */
  public static @NonNull OtelResourceMetric forScopeMetric(@NonNull OtelScopeMetric scopeMetric) {
    return new OtelResourceMetric(OtelResource.agentle(), List.of(scopeMetric));
  }

  /** Creates a resource metric with a list of metrics. */
  public static @NonNull OtelResourceMetric forMetrics(@NonNull List<OtelMetric> metrics) {
    return forScopeMetric(OtelScopeMetric.forMetrics(metrics));
  }
}
