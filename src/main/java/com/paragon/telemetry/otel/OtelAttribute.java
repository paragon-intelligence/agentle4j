package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/**
 * OpenTelemetry key-value attribute pair. Used for resources, scopes, and spans.
 *
 * <p>Follows the OTLP specification for attributes.
 */
public record OtelAttribute(
    @JsonProperty("key") @NonNull String key,
    @JsonProperty("value") @NonNull OtelAttributeValue value) {

  /** Creates a string attribute. */
  public static @NonNull OtelAttribute ofString(@NonNull String key, @NonNull String value) {
    return new OtelAttribute(key, OtelAttributeValue.ofString(value));
  }

  /** Creates a boolean attribute. */
  public static @NonNull OtelAttribute ofBool(@NonNull String key, boolean value) {
    return new OtelAttribute(key, OtelAttributeValue.ofBool(value));
  }

  /** Creates an integer attribute. */
  public static @NonNull OtelAttribute ofInt(@NonNull String key, long value) {
    return new OtelAttribute(key, OtelAttributeValue.ofInt(value));
  }

  /** Creates a double attribute. */
  public static @NonNull OtelAttribute ofDouble(@NonNull String key, double value) {
    return new OtelAttribute(key, OtelAttributeValue.ofDouble(value));
  }

  /** Creates an attribute from any value, inferring the type. */
  public static @NonNull OtelAttribute of(@NonNull String key, @NonNull Object value) {
    return new OtelAttribute(key, OtelAttributeValue.of(value));
  }
}
