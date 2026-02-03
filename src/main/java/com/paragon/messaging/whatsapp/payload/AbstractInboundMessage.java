package com.paragon.messaging.whatsapp.payload;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for inbound WhatsApp webhook messages.
 *
 * <p>Provides common fields and validation for all inbound message types:</p>
 * <ul>
 *   <li>{@code from} - Sender's WhatsApp ID</li>
 *   <li>{@code id} - Unique message identifier</li>
 *   <li>{@code timestamp} - Message timestamp (Unix epoch)</li>
 *   <li>{@code type} - Message type discriminator</li>
 *   <li>{@code context} - Reply context (optional)</li>
 * </ul>
 *
 * @author Agentle Team
 * @since 2.1
 */
public abstract sealed class AbstractInboundMessage implements InboundMessage
        permits TextMessage, ImageMessage, VideoMessage, AudioMessage,
        DocumentMessage, StickerMessage, InteractiveMessage, LocationMessage,
        ReactionMessage, SystemMessage, OrderMessage {

    @NotNull
    public final String from;

    @NotNull
    public final String id;

    @NotNull
    public final String timestamp;

    @NotNull
    public final String type;

    @Nullable
    public final MessageContext context;

    protected AbstractInboundMessage(
            String from,
            String id,
            String timestamp,
            String type,
            MessageContext context
    ) {
        this.from = from;
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.context = context;
    }

    @Override
    public @NonNull String from() {
        return from;
    }

    @Override
    public @NonNull String id() {
        return id;
    }

    @Override
    public @NonNull String timestamp() {
        return timestamp;
    }

    @Override
    public @NonNull String type() {
        return type;
    }

    @Override
    public @Nullable MessageContext context() {
        return context;
    }
}
