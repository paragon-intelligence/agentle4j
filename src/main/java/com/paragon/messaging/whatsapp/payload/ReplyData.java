package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReplyData {

  public final String id;
  public final String title;
  public final String description; // Optional, for list rows

  @JsonCreator
  public ReplyData(
      @JsonProperty("id") String id,
      @JsonProperty("title") String title,
      @JsonProperty("description") String description) {
    this.id = id;
    this.title = title;
    this.description = description;
  }
}
