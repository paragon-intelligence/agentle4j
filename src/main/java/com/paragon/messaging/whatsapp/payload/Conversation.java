package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Represents a WhatsApp conversation object in status updates.
 *
 * @param id                   conversation ID
 * @param origin               conversation origin with type information
 * @param expirationTimestamp  optional expiration timestamp (Unix epoch as string)
 */
public record Conversation(
        @JsonProperty("id") String id,
        @JsonProperty("origin") Origin origin,
        @JsonProperty("expiration_timestamp") @Nullable String expirationTimestamp) {

    @JsonCreator
    public Conversation(
            @JsonProperty("id") String id,
            @JsonProperty("origin") Origin origin,
            @JsonProperty("expiration_timestamp") @Nullable String expirationTimestamp) {
        this.id = id;
        this.origin = origin;
        this.expirationTimestamp = expirationTimestamp;
    }

    /**
     * Conversation origin information.
     *
     * @param type origin type (e.g., "user_initiated", "business_initiated")
     */
    public record Origin(@JsonProperty("type") String type) {

        @JsonCreator
        public Origin(@JsonProperty("type") String type) {
            this.type = type;
        }
    }
}
