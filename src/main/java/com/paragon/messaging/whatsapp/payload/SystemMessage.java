package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Inbound system message from WhatsApp webhook.
 *
 * <p>System messages include notifications like user identity changes,
 * number changes, or other system events.</p>
 */
public final class SystemMessage extends AbstractInboundMessage {

    @Nullable
    public final String body;

    @Nullable
    public final String identity;

    @Nullable
    public final String newWaId;

    @Nullable
    public final String waId;

    @Nullable
    public final String systemType;

    @Nullable
    public final String customer;

    @JsonCreator
    public SystemMessage(
            @JsonProperty("from") String from,
            @JsonProperty("id") String id,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("type") String type,
            @JsonProperty("context") MessageContext context,
            @JsonProperty("system") SystemContent system
    ) {
        super(from, id, timestamp, type, context);
        if (system != null) {
            this.body = system.body();
            this.identity = system.identity();
            this.newWaId = system.newWaId();
            this.waId = system.waId();
            this.systemType = system.type();
            this.customer = system.customer();
        } else {
            this.body = null;
            this.identity = null;
            this.newWaId = null;
            this.waId = null;
            this.systemType = null;
            this.customer = null;
        }
    }

    public record SystemContent(
            @JsonProperty("body") @Nullable String body,
            @JsonProperty("identity") @Nullable String identity,
            @JsonProperty("new_wa_id") @Nullable String newWaId,
            @JsonProperty("wa_id") @Nullable String waId,
            @JsonProperty("type") @Nullable String type,
            @JsonProperty("customer") @Nullable String customer
    ) {}
}
