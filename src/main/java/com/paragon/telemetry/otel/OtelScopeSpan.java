package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Collection of spans from a single instrumentation scope. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtelScopeSpan(
    @JsonProperty("scope") @Nullable OtelScope scope,
    @JsonProperty("spans") @Nullable List<OtelSpan> spans) {

  /** Creates a scope span collection for Agentle with the given spans. */
  public static @NonNull OtelScopeSpan forAgentle(@NonNull List<OtelSpan> spans) {
    return new OtelScopeSpan(OtelScope.forAgentle(), spans);
  }
}
