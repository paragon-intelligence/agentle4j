package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/** Inbound sticker message from WhatsApp webhook. */
public final class StickerMessage extends AbstractInboundMessage {

  @Nullable public final String mediaId;

  @Nullable public final String mimeType;

  @Nullable public final String sha256;

  public final boolean animated;

  @JsonCreator
  public StickerMessage(
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("type") String type,
      @JsonProperty("context") MessageContext context,
      @JsonProperty("sticker") MediaContent sticker) {
    super(from, id, timestamp, type, context);
    if (sticker != null) {
      this.mediaId = sticker.id();
      this.mimeType = sticker.mimeType();
      this.sha256 = sticker.sha256();
      this.animated = sticker.animated() != null && sticker.animated();
    } else {
      this.mediaId = null;
      this.mimeType = null;
      this.sha256 = null;
      this.animated = false;
    }
  }
}
