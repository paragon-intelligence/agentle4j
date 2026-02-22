package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public final class InteractiveMessage extends AbstractInboundMessage {

  public final InteractiveContent interactive;

  /** Convenience field for button reply content (null if not a button reply). */
  public final ReplyData buttonReply;

  /** Convenience field for list reply content (null if not a list reply). */
  public final ReplyData listReply;

  @JsonCreator
  public InteractiveMessage(
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("type") String type,
      @JsonProperty("context") MessageContext context,
      @JsonProperty("interactive") InteractiveContent interactive) {
    super(from, id, timestamp, type, context);
    this.interactive = interactive;

    // Extract convenience fields
    if (interactive instanceof ButtonReply br) {
      this.buttonReply = br.buttonReply;
      this.listReply = null;
    } else if (interactive instanceof ListReply lr) {
      this.buttonReply = null;
      this.listReply = lr.listReply;
    } else {
      this.buttonReply = null;
      this.listReply = null;
    }
  }

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ButtonReply.class, name = "button_reply"),
    @JsonSubTypes.Type(value = ListReply.class, name = "list_reply"),
    @JsonSubTypes.Type(value = NfmReply.class, name = "nfm_reply")
  })
  public sealed interface InteractiveContent permits ButtonReply, ListReply, NfmReply {}
}
