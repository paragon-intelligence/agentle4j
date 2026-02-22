package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/** Inbound document/file message from WhatsApp webhook. */
public final class DocumentMessage extends AbstractInboundMessage {

  @Nullable public final String mediaId;

  @Nullable public final String mimeType;

  @Nullable public final String sha256;

  @Nullable public final String filename;

  @Nullable public final String caption;

  @JsonCreator
  public DocumentMessage(
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("type") String type,
      @JsonProperty("context") MessageContext context,
      @JsonProperty("document") MediaContent document) {
    super(from, id, timestamp, type, context);
    if (document != null) {
      this.mediaId = document.id();
      this.mimeType = document.mimeType();
      this.sha256 = document.sha256();
      this.filename = document.filename();
      this.caption = document.caption();
    } else {
      this.mediaId = null;
      this.mimeType = null;
      this.sha256 = null;
      this.filename = null;
      this.caption = null;
    }
  }
}
