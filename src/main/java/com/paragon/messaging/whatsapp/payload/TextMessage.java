package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TextMessage extends AbstractMessage {

  public final TextBody text;

  @JsonCreator
  public TextMessage(
          @JsonProperty("from") String from,
          @JsonProperty("id") String id,
          @JsonProperty("timestamp") String timestamp,
          @JsonProperty("type") String type,
          @JsonProperty("context") MessageContext context,
          @JsonProperty("text") TextBody text
  ) {
    super(from, id, timestamp, type, context);
    this.text = text;
  }

  public static class TextBody {
    public final String body;

    @JsonCreator
    public TextBody(@JsonProperty("body") String body) {
      this.body = body;
    }
  }
}
