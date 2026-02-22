package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/** Inbound reaction message from WhatsApp webhook. */
public final class ReactionMessage extends AbstractInboundMessage {

  @Nullable public final String messageId;

  @Nullable public final String emoji;

  @JsonCreator
  public ReactionMessage(
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("type") String type,
      @JsonProperty("context") MessageContext context,
      @JsonProperty("reaction") ReactionContent reaction) {
    super(from, id, timestamp, type, context);
    if (reaction != null) {
      this.messageId = reaction.messageId();
      this.emoji = reaction.emoji();
    } else {
      this.messageId = null;
      this.emoji = null;
    }
  }

  /** Returns true if this is a reaction removal (empty emoji). */
  public boolean isRemoval() {
    return emoji == null || emoji.isEmpty();
  }

  public record ReactionContent(
      @JsonProperty("message_id") String messageId,
      @JsonProperty("emoji") @Nullable String emoji) {}
}
