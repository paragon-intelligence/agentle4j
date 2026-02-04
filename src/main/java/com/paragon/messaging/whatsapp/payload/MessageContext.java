package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Context information about a message that is being replied to or quoted.
 *
 * @param from               sender of the original message
 * @param id                 ID of the original message
 * @param forwarded          whether the message has been forwarded
 * @param frequentlyForwarded whether the message has been frequently forwarded
 */
public record MessageContext(
        @JsonProperty("from") String from,
        @JsonProperty("id") String id,
        @JsonProperty("forwarded") @Nullable Boolean forwarded,
        @JsonProperty("frequently_forwarded") @Nullable Boolean frequentlyForwarded) {

    @JsonCreator
    public MessageContext(
            @JsonProperty("from") String from,
            @JsonProperty("id") String id,
            @JsonProperty("forwarded") @Nullable Boolean forwarded,
            @JsonProperty("frequently_forwarded") @Nullable Boolean frequentlyForwarded) {
        this.from = from;
        this.id = id;
        this.forwarded = forwarded;
        this.frequentlyForwarded = frequentlyForwarded;
    }
}