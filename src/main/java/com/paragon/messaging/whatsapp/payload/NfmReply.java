package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Interactive NFM (Natural Flow Message) reply from WhatsApp webhook.
 *
 * <p>NFM replies are used for flows and forms in WhatsApp Business.</p>
 */
public final class NfmReply implements InteractiveMessage.InteractiveContent {

    public final String type;

    @Nullable
    public final String responseJson;

    @Nullable
    public final String body;

    @Nullable
    public final String name;

    @JsonCreator
    public NfmReply(
            @JsonProperty("type") String type,
            @JsonProperty("response_json") @Nullable String responseJson,
            @JsonProperty("body") @Nullable String body,
            @JsonProperty("name") @Nullable String name
    ) {
        this.type = type;
        this.responseJson = responseJson;
        this.body = body;
        this.name = name;
    }
}
