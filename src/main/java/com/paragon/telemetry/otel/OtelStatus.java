package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry span status. Indicates whether the span completed successfully, with errors, or
 * unset.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtelStatus(
    @JsonProperty("code") int code, @JsonProperty("message") @Nullable String message) {

  /** Status codes as per OpenTelemetry spec. */
  public static final int STATUS_CODE_UNSET = 0;

  public static final int STATUS_CODE_OK = 1;
  public static final int STATUS_CODE_ERROR = 2;

  /** Creates an unset status (default). */
  public static @NonNull OtelStatus unset() {
    return new OtelStatus(STATUS_CODE_UNSET, null);
  }

  /** Creates an OK status indicating successful completion. */
  public static @NonNull OtelStatus ok() {
    return new OtelStatus(STATUS_CODE_OK, null);
  }

  /** Creates an error status with a message. */
  public static @NonNull OtelStatus error(@NonNull String message) {
    return new OtelStatus(STATUS_CODE_ERROR, message);
  }
}
