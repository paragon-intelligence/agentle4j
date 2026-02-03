package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Inbound audio/voice message from WhatsApp webhook.
 */
public final class AudioMessage extends AbstractInboundMessage {

    @Nullable
    public final String mediaId;

    @Nullable
    public final String mimeType;

    @Nullable
    public final String sha256;

    public final boolean voice;

    @JsonCreator
    public AudioMessage(
            @JsonProperty("from") String from,
            @JsonProperty("id") String id,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("type") String type,
            @JsonProperty("context") MessageContext context,
            @JsonProperty("audio") MediaContent audio
    ) {
        super(from, id, timestamp, type, context);
        if (audio != null) {
            this.mediaId = audio.id();
            this.mimeType = audio.mimeType();
            this.sha256 = audio.sha256();
            this.voice = audio.voice() != null && audio.voice();
        } else {
            this.mediaId = null;
            this.mimeType = null;
            this.sha256 = null;
            this.voice = false;
        }
    }
}
