package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Interactive list selection reply from WhatsApp webhook.
 */
public final class ListReply implements InteractiveMessage.InteractiveContent {

    public final String type;
    public final ReplyData listReply;

    @JsonCreator
    public ListReply(
            @JsonProperty("type") String type,
            @JsonProperty("list_reply") ReplyData listReply
    ) {
        this.type = type;
        this.listReply = listReply;
    }
}
