package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry resource describing the entity producing telemetry. Contains attributes like
 * service.name, service.version.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtelResource(@JsonProperty("attributes") @Nullable List<OtelAttribute> attributes) {

  /** Creates a resource with service identification. */
  public static @NonNull OtelResource forService(
      @NonNull String serviceName, @NonNull String serviceVersion) {
    return new OtelResource(
        List.of(
            OtelAttribute.ofString("service.name", serviceName),
            OtelAttribute.ofString("service.version", serviceVersion)));
  }

  /** Creates a resource for Agentle library. */
  public static @NonNull OtelResource forAgentle() {
    return forService("agentle-java", "1.0.0");
  }

  /** Alias for forAgentle(). */
  public static @NonNull OtelResource agentle() {
    return forAgentle();
  }
}
