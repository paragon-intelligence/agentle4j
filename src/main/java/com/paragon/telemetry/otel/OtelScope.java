package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry instrumentation scope information. Identifies the library that produces the
 * telemetry.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtelScope(
    @JsonProperty("name") @Nullable String name,
    @JsonProperty("version") @Nullable String version,
    @JsonProperty("attributes") @Nullable List<OtelAttribute> attributes) {

  /** Creates a scope for Agentle library. */
  public static @NonNull OtelScope forAgentle() {
    return new OtelScope("agentle-java", "1.0.0", null);
  }

  /** Creates a scope with just name and version. */
  public static @NonNull OtelScope of(@NonNull String name, @NonNull String version) {
    return new OtelScope(name, version, null);
  }

  /** Alias for forAgentle(). */
  public static @NonNull OtelScope agentle() {
    return forAgentle();
  }
}
