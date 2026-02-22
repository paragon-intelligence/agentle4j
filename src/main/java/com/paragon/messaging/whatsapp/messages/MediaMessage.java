package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.MediaMessageInterface;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Optional;

/**
 * Sealed interface for media messages with validation.
 *
 * <p>Supports image, video, audio, document, and sticker message types.
 */
public sealed interface MediaMessage extends MediaMessageInterface
    permits MediaMessage.Image,
        MediaMessage.Video,
        MediaMessage.Audio,
        MediaMessage.Document,
        MediaMessage.Sticker {

  MediaSource source();

  Optional<String> caption();

  /** Fonte de mídia (URL ou ID). */
  sealed interface MediaSource permits MediaSource.Url, MediaSource.MediaId {

    record Url(
        @NotBlank(message = "URL cannot be blank")
            @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
            String url)
        implements MediaSource {}

    record MediaId(@NotBlank(message = "Media ID cannot be blank") String id)
        implements MediaSource {}
  }

  /** Mensagem de imagem. */
  record Image(
      @NotNull(message = "Image source cannot be null") MediaSource source,
      Optional<@Size(max = 1024, message = "Caption cannot exceed 1024 characters") String> caption,
      String replyToMessageId)
      implements MediaMessage {

    public Image(MediaSource source) {
      this(source, Optional.empty(), null);
    }

    public Image(MediaSource source, String caption) {
      this(source, Optional.ofNullable(caption), null);
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.IMAGE;
    }

    @Override
    public Image withReplyTo(String messageId) {
      return new Image(source, caption, messageId);
    }
  }

  /** Mensagem de vídeo. */
  record Video(
      @NotNull(message = "Video source cannot be null") MediaSource source,
      Optional<@Size(max = 1024) String> caption,
      String replyToMessageId)
      implements MediaMessage {

    public Video(MediaSource source) {
      this(source, Optional.empty(), null);
    }

    public Video(MediaSource source, String caption) {
      this(source, Optional.ofNullable(caption), null);
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.VIDEO;
    }

    @Override
    public Video withReplyTo(String messageId) {
      return new Video(source, caption, messageId);
    }
  }

  /** Mensagem de áudio. */
  record Audio(
      @NotNull(message = "Audio source cannot be null") MediaSource source, String replyToMessageId)
      implements MediaMessage {

    public Audio(MediaSource source) {
      this(source, null);
    }

    @Override
    public Optional<String> caption() {
      return Optional.empty();
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.AUDIO;
    }

    @Override
    public Audio withReplyTo(String messageId) {
      return new Audio(source, messageId);
    }
  }

  /** Mensagem de documento. */
  record Document(
      @NotNull(message = "Document source cannot be null") MediaSource source,
      Optional<@Size(max = 1000) String> filename,
      Optional<@Size(max = 1024) String> caption,
      String replyToMessageId)
      implements MediaMessage {

    public Document(MediaSource source, String filename) {
      this(source, Optional.ofNullable(filename), Optional.empty(), null);
    }

    public Document(MediaSource source, String filename, String caption) {
      this(source, Optional.ofNullable(filename), Optional.ofNullable(caption), null);
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.DOCUMENT;
    }

    @Override
    public Document withReplyTo(String messageId) {
      return new Document(source, filename, caption, messageId);
    }
  }

  /** Mensagem de sticker. */
  record Sticker(
      @NotNull(message = "Sticker source cannot be null") MediaSource source,
      String replyToMessageId)
      implements MediaMessage {

    public Sticker(MediaSource source) {
      this(source, null);
    }

    @Override
    public Optional<String> caption() {
      return Optional.empty();
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.STICKER;
    }

    @Override
    public Sticker withReplyTo(String messageId) {
      return new Sticker(source, messageId);
    }
  }
}
