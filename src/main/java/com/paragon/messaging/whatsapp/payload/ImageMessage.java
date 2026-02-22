package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/** Inbound image message from WhatsApp webhook. */
public final class ImageMessage extends AbstractInboundMessage {

  @Nullable public final String mediaId;

  @Nullable public final String mimeType;

  @Nullable public final String sha256;

  @Nullable public final String caption;

  @JsonCreator
  public ImageMessage(
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("type") String type,
      @JsonProperty("context") MessageContext context,
      @JsonProperty("image") MediaContent image) {
    super(from, id, timestamp, type, context);
    if (image != null) {
      this.mediaId = image.id();
      this.mimeType = image.mimeType();
      this.sha256 = image.sha256();
      this.caption = image.caption();
    } else {
      this.mediaId = null;
      this.mimeType = null;
      this.sha256 = null;
      this.caption = null;
    }
  }
}
