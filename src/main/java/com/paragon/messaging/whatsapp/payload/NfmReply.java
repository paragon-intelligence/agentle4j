package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Interactive NFM (Natural Flow Message) reply from WhatsApp webhook.
 *
 * <p>NFM replies are used for flows and forms in WhatsApp Business.</p>
 * 
 * <p><b>Note:</b> The {@code responseJson} field contains the actual parsed JSON object
 * returned from a WhatsApp Flow, not a serialized JSON string.</p>
 */
public final class NfmReply implements InteractiveMessage.InteractiveContent {

    public final String type;

    @Nullable
    public final Map<String, Object> responseJson;

    @Nullable
    public final String body;

    @Nullable
    public final String name;

    @JsonCreator
    public NfmReply(
            @JsonProperty("type") String type,
            @JsonProperty("response_json") @Nullable Map<String, Object> responseJson,
            @JsonProperty("body") @Nullable String body,
            @JsonProperty("name") @Nullable String name
    ) {
        this.type = type;
        this.responseJson = responseJson;
        this.body = body;
        this.name = name;
    }
}
