package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ButtonReply implements InteractiveMessage.InteractiveContent {

  public final String type;
  public final ReplyData buttonReply;

  @JsonCreator
  public ButtonReply(
          @JsonProperty("type") String type,
          @JsonProperty("button_reply") ReplyData buttonReply
  ) {
    this.type = type;
    this.buttonReply = buttonReply;
  }
}
