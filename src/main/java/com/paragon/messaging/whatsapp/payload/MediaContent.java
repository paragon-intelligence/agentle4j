package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Media content payload for images, videos, audio, documents, and stickers.
 *
 * @param id            Media ID for downloading
 * @param mimeType      MIME type of the media
 * @param sha256        SHA256 hash of the media
 * @param caption       Optional caption (images, videos, documents)
 * @param filename      Filename for documents
 * @param voice         True if audio is a voice message
 * @param animated      True if sticker is animated
 * @param mediaAssetUrl Direct download URL (2025/2026 update)
 */
public record MediaContent(
        String id,
        String mimeType,
        String sha256,
        String caption,
        String filename,
        Boolean voice,
        Boolean animated,
        String mediaAssetUrl
) {

  @JsonCreator
  public MediaContent(
          @JsonProperty("id") String id,
          @JsonProperty("mime_type") String mimeType,
          @JsonProperty("sha256") String sha256,
          @JsonProperty("caption") String caption,
          @JsonProperty("filename") String filename,
          @JsonProperty("voice") Boolean voice,
          @JsonProperty("animated") Boolean animated,
          @JsonProperty("media_asset_url") String mediaAssetUrl
  ) {
    this.id = id;
    this.mimeType = mimeType;
    this.sha256 = sha256;
    this.caption = caption;
    this.filename = filename;
    this.voice = voice;
    this.animated = animated;
    this.mediaAssetUrl = mediaAssetUrl;
  }
}
