package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @param filename      For documents
 * @param voice         For audio
 * @param mediaAssetUrl Direct download URL (2025/2026 update)
 */
public record MediaContent(String id, String mimeType, String sha256, String caption, String filename, Boolean voice,
                           String mediaAssetUrl) {

  @JsonCreator
  public MediaContent(
          @JsonProperty("id") String id,
          @JsonProperty("mime_type") String mimeType,
          @JsonProperty("sha256") String sha256,
          @JsonProperty("caption") String caption,
          @JsonProperty("filename") String filename,
          @JsonProperty("voice") Boolean voice,
          @JsonProperty("media_asset_url") String mediaAssetUrl
  ) {
    this.id = id;
    this.mimeType = mimeType;
    this.sha256 = sha256;
    this.caption = caption;
    this.filename = filename;
    this.voice = voice;
    this.mediaAssetUrl = mediaAssetUrl;
  }
}
