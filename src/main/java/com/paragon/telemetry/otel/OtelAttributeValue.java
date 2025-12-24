package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry attribute value wrapper supporting different value types. Only one of the value
 * fields should be set.
 *
 * <p>Follows the OTLP specification for attribute values.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtelAttributeValue(
    @JsonProperty("stringValue") @Nullable String stringValue,
    @JsonProperty("boolValue") @Nullable Boolean boolValue,
    @JsonProperty("intValue") @Nullable Long intValue,
    @JsonProperty("doubleValue") @Nullable Double doubleValue) {

  /** Creates a string attribute value. */
  public static @NonNull OtelAttributeValue ofString(@NonNull String value) {
    return new OtelAttributeValue(value, null, null, null);
  }

  /** Creates a boolean attribute value. */
  public static @NonNull OtelAttributeValue ofBool(boolean value) {
    return new OtelAttributeValue(null, value, null, null);
  }

  /** Creates an integer attribute value. */
  public static @NonNull OtelAttributeValue ofInt(long value) {
    return new OtelAttributeValue(null, null, value, null);
  }

  /** Creates a double attribute value. */
  public static @NonNull OtelAttributeValue ofDouble(double value) {
    return new OtelAttributeValue(null, null, null, value);
  }

  /** Creates an attribute value from any object, inferring the correct type. */
  public static @NonNull OtelAttributeValue of(@Nullable Object value) {
    if (value == null) {
      return ofString("");
    } else if (value instanceof String s) {
      return ofString(s);
    } else if (value instanceof Boolean b) {
      return ofBool(b);
    } else if (value instanceof Long l) {
      return ofInt(l);
    } else if (value instanceof Integer i) {
      return ofInt(i.longValue());
    } else if (value instanceof Double d) {
      return ofDouble(d);
    } else if (value instanceof Float f) {
      return ofDouble(f.doubleValue());
    } else if (value instanceof Number n) {
      return ofDouble(n.doubleValue());
    } else {
      return ofString(value.toString());
    }
  }
}
