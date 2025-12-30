package com.paragon.broadcasting;

import java.util.Map;
import org.jspecify.annotations.NonNull;

public interface TraceMetrics {
  double getTotalCost();

  long getTotalLatencyMs();

  int getTotalInputTokens();

  int getTotalOutputTokens();

  int getTotalTokens();

  int getObservationCount();

  @NonNull Map<String, Object> getAggregatedAttributes();
}
