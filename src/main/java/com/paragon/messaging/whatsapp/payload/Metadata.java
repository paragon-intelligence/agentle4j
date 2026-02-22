package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Metadata(String displayPhoneNumber, String phoneNumberId) {

  @JsonCreator
  public Metadata(
      @JsonProperty("display_phone_number") String displayPhoneNumber,
      @JsonProperty("phone_number_id") String phoneNumberId) {
    this.displayPhoneNumber = displayPhoneNumber;
    this.phoneNumberId = phoneNumberId;
  }
}
