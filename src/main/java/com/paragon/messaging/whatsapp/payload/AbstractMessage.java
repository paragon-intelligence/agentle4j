package com.paragon.messaging.whatsapp.payload;

import jakarta.validation.constraints.NotNull;

public abstract sealed class AbstractMessage implements Message
        permits TextMessage, ImageMessage, VideoMessage, AudioMessage,
        DocumentMessage, StickerMessage, InteractiveMessage, LocationMessage,
        ReactionMessage, SystemMessage, OrderMessage {

  @NotNull
  public final String from;

  @NotNull
  public final String id;

  @NotNull
  public final String timestamp;

  @NotNull
  public final String type;

  public final MessageContext context;

  protected AbstractMessage(
          String from,
          String id,
          String timestamp,
          String type,
          MessageContext context
  ) {
    this.from = from;
    this.id = id;
    this.timestamp = timestamp;
    this.type = type;
    this.context = context;
  }
}
