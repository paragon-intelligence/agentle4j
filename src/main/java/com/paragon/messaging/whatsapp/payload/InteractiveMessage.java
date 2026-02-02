package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public final class InteractiveMessage extends AbstractMessage {

  public final InteractiveContent interactive;

  @JsonCreator
  public InteractiveMessage(
          @JsonProperty("from") String from,
          @JsonProperty("id") String id,
          @JsonProperty("timestamp") String timestamp,
          @JsonProperty("type") String type,
          @JsonProperty("context") MessageContext context,
          @JsonProperty("interactive") InteractiveContent interactive
  ) {
    super(from, id, timestamp, type, context);
    this.interactive = interactive;
  }

  @JsonTypeInfo(
          use = JsonTypeInfo.Id.NAME,
          include = JsonTypeInfo.As.EXISTING_PROPERTY,
          property = "type"
  )
  @JsonSubTypes({
          @JsonSubTypes.Type(value = ButtonReply.class, name = "button_reply"),
          @JsonSubTypes.Type(value = ListReply.class, name = "list_reply"),
          @JsonSubTypes.Type(value = NfmReply.class, name = "nfm_reply")
  })
  public sealed interface InteractiveContent permits ButtonReply, ListReply, NfmReply {
  }
}