package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a WhatsApp conversation object in status updates.
 *
 * @param id     conversation ID
 * @param origin conversation origin
 */
public record Conversation(String id, String origin) {

  @JsonCreator
  public Conversation(
          @JsonProperty("id") String id,
          @JsonProperty("origin") String origin
  ) {
    this.id = id;
    this.origin = origin;
  }
}
