package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents error data in WhatsApp API responses.
 *
 * @param code    error code
 * @param message error message
 * @param details error details
 */
public record ErrorData(Integer code, String message, String details) {

  @JsonCreator
  public ErrorData(
          @JsonProperty("code") Integer code,
          @JsonProperty("message") String message,
          @JsonProperty("error_data") String details
  ) {
    this.code = code;
    this.message = message;
    this.details = details;
  }
}
