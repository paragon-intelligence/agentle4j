package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Represents error data in WhatsApp API responses.
 *
 * <p><b>Important:</b> Use the {@code code} field for programmatic error handling, not the {@code
 * title} field which is deprecated by Meta.
 *
 * @param code error code (use this for programmatic handling)
 * @param title error title (deprecated by Meta, but still sent in responses)
 * @param message error message
 * @param errorData nested error details object
 */
public record ErrorData(
    @JsonProperty("code") Integer code,
    @JsonProperty("title") @Nullable String title,
    @JsonProperty("message") @Nullable String message,
    @JsonProperty("error_data") @Nullable ErrorDetails errorData) {

  @JsonCreator
  public ErrorData(
      @JsonProperty("code") Integer code,
      @JsonProperty("title") @Nullable String title,
      @JsonProperty("message") @Nullable String message,
      @JsonProperty("error_data") @Nullable ErrorDetails errorData) {
    this.code = code;
    this.title = title;
    this.message = message;
    this.errorData = errorData;
  }

  /**
   * Nested error details.
   *
   * @param details specific error explanation
   */
  public record ErrorDetails(@JsonProperty("details") String details) {

    @JsonCreator
    public ErrorDetails(@JsonProperty("details") String details) {
      this.details = details;
    }
  }
}
