package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Inbound location message from WhatsApp webhook.
 */
public final class LocationMessage extends AbstractInboundMessage {

    public final double latitude;

    public final double longitude;

    @Nullable
    public final String name;

    @Nullable
    public final String address;

    @Nullable
    public final String url;

    @JsonCreator
    public LocationMessage(
            @JsonProperty("from") String from,
            @JsonProperty("id") String id,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("type") String type,
            @JsonProperty("context") MessageContext context,
            @JsonProperty("location") LocationContent location
    ) {
        super(from, id, timestamp, type, context);
        if (location != null) {
            this.latitude = location.latitude();
            this.longitude = location.longitude();
            this.name = location.name();
            this.address = location.address();
            this.url = location.url();
        } else {
            this.latitude = 0.0;
            this.longitude = 0.0;
            this.name = null;
            this.address = null;
            this.url = null;
        }
    }

    public record LocationContent(
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude,
            @JsonProperty("name") @Nullable String name,
            @JsonProperty("address") @Nullable String address,
            @JsonProperty("url") @Nullable String url
    ) {}
}
