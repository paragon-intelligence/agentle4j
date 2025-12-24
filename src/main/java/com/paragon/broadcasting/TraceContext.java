package com.paragon.broadcasting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record TraceContext(
    @Nullable String userId,
    @Nullable String sessionId,
    java.util.Map<String, Object> metadata,
    java.util.List<String> tags,
    @Nullable String version,
    @Nullable String release,
    @Nullable String environment,
    boolean isPublic) {
  public static @NonNull TraceContext minimal() {
    return new TraceContext(
        null, null, java.util.Map.of(), java.util.List.of(), null, null, null, false);
  }
}
