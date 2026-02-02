package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Contact {

  public final String waId;
  public final Profile profile;

  @JsonCreator
  public Contact(
          @JsonProperty("wa_id") String waId,
          @JsonProperty("profile") Profile profile
  ) {
    this.waId = waId;
    this.profile = profile;
  }

  public static class Profile {
    public final String name;

    @JsonCreator
    public Profile(@JsonProperty("name") String name) {
      this.name = name;
    }
  }
}