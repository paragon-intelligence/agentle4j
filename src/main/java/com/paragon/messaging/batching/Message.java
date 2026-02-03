package com.paragon.messaging.batching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

/**
 * Immutable message data for batching.
 *
 * <p>Represents a simplified message in the batching queue. The full message
 * payload is available in {@link com.paragon.messaging.whatsapp.payload.InboundMessage}
 * after processing.</p>
 *
 * @param messageId unique message identifier (for deduplication)
 * @param content textual content of the message
 * @param timestamp when the message was received
 * @author Agentle Team
 * @since 2.1
 */
public record Message(
        @NonNull
        @NotBlank(message = "Message ID cannot be blank")
        String messageId,

        @NonNull
        @NotBlank(message = "Message content cannot be blank")
        String content,

        @NonNull
        @NotNull(message = "Timestamp cannot be null")
        @PastOrPresent(message = "Timestamp cannot be in the future")
        Instant timestamp
) {
    /**
     * Canonical constructor with validation.
     */
    public Message {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId cannot be null or blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }
    }
}
