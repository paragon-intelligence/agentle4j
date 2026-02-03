package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.MediaMessageInterface;
import com.paragon.messaging.core.OutboundMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Optional;

/**
 * Sealed interface for media messages with validation.
 *
 * <p>Supports image, video, audio, document, and sticker message types.</p>
 */
public sealed interface MediaMessage extends MediaMessageInterface permits
        MediaMessage.Image,
        MediaMessage.Video,
        MediaMessage.Audio,
        MediaMessage.Document,
        MediaMessage.Sticker {

  MediaSource source();

  Optional<String> caption();

  /**
   * Fonte de mídia (URL ou ID).
   */
  sealed interface MediaSource permits MediaSource.Url, MediaSource.MediaId {

    record Url(
            @NotBlank(message = "URL cannot be blank")
            @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
            String url
    ) implements MediaSource {
    }

    record MediaId(
            @NotBlank(message = "Media ID cannot be blank")
            String id
    ) implements MediaSource {
    }
  }

  /**
   * Mensagem de imagem.
   */
  record Image(
          @NotNull(message = "Image source cannot be null")
          MediaSource source,

          Optional<@Size(max = 1024, message = "Caption cannot exceed 1024 characters") String> caption
  ) implements MediaMessage {

    public Image(MediaSource source) {
      this(source, Optional.empty());
    }

    public Image(MediaSource source, String caption) {
      this(source, Optional.ofNullable(caption));
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.IMAGE;
    }
  }

  /**
   * Mensagem de vídeo.
   */
  record Video(
          @NotNull(message = "Video source cannot be null")
          MediaSource source,

          Optional<@Size(max = 1024) String> caption
  ) implements MediaMessage {

    public Video(MediaSource source) {
      this(source, Optional.empty());
    }

    public Video(MediaSource source, String caption) {
      this(source, Optional.ofNullable(caption));
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.VIDEO;
    }
  }

  /**
   * Mensagem de áudio.
   */
  record Audio(
          @NotNull(message = "Audio source cannot be null")
          MediaSource source
  ) implements MediaMessage {

    @Override
    public Optional<String> caption() {
      return Optional.empty();
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.AUDIO;
    }
  }

  /**
   * Mensagem de documento.
   */
  record Document(
          @NotNull(message = "Document source cannot be null")
          MediaSource source,

          Optional<@Size(max = 1000) String> filename,
          Optional<@Size(max = 1024) String> caption
  ) implements MediaMessage {

    public Document(MediaSource source, String filename) {
      this(source, Optional.ofNullable(filename), Optional.empty());
    }

    public Document(MediaSource source, String filename, String caption) {
      this(source, Optional.ofNullable(filename), Optional.ofNullable(caption));
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.DOCUMENT;
    }
  }

  /**
   * Mensagem de sticker.
   */
  record Sticker(
          @NotNull(message = "Sticker source cannot be null")
          MediaSource source
  ) implements MediaMessage {

    @Override
    public Optional<String> caption() {
      return Optional.empty();
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.STICKER;
    }
  }
}
