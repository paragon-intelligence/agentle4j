package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/** Inbound video message from WhatsApp webhook. */
public final class VideoMessage extends AbstractInboundMessage {

  @Nullable public final String mediaId;

  @Nullable public final String mimeType;

  @Nullable public final String sha256;

  @Nullable public final String caption;

  @JsonCreator
  public VideoMessage(
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("type") String type,
      @JsonProperty("context") MessageContext context,
      @JsonProperty("video") MediaContent video) {
    super(from, id, timestamp, type, context);
    if (video != null) {
      this.mediaId = video.id();
      this.mimeType = video.mimeType();
      this.sha256 = video.sha256();
      this.caption = video.caption();
    } else {
      this.mediaId = null;
      this.mimeType = null;
      this.sha256 = null;
      this.caption = null;
    }
  }
}
