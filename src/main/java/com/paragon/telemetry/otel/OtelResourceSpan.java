package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a collection of spans from a single resource. Groups scope spans by their originating
 * resource.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtelResourceSpan(
    @JsonProperty("resource") @Nullable OtelResource resource,
    @JsonProperty("scopeSpans") @Nullable List<OtelScopeSpan> scopeSpans) {

  /** Creates a resource span collection for Agentle with the given spans. */
  public static @NonNull OtelResourceSpan forAgentle(@NonNull List<OtelSpan> spans) {
    return new OtelResourceSpan(
        OtelResource.forAgentle(), List.of(OtelScopeSpan.forAgentle(spans)));
  }
}
