package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Contact {

  @NotBlank(message = "WhatsApp ID cannot be blank")
  @Pattern(regexp = "\\+?[1-9]\\d{1,14}", message = "Must be a valid E.164 phone number")
  public final String waId;

  public final Profile profile;

  @JsonCreator
  public Contact(@JsonProperty("wa_id") String waId, @JsonProperty("profile") Profile profile) {
    this.waId = waId;
    this.profile = profile;
  }

  public static class Profile {
    @NotBlank(message = "Contact name cannot be blank")
    @Size(max = 256, message = "Contact name cannot exceed 256 characters")
    public final String name;

    @JsonCreator
    public Profile(@JsonProperty("name") String name) {
      this.name = name;
    }
  }
}
