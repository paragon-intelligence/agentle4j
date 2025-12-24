package com.paragon.broadcasting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ObservationContext(
    @NonNull ObservationLevel level,
    @Nullable Object input,
    java.util.Map<String, Object> metadata,
    java.util.Map<String, Object> attributes) {
  public static @NonNull ObservationContext minimal() {
    return new ObservationContext(
        ObservationLevel.DEFAULT, null, java.util.Map.of(), java.util.Map.of());
  }
}
