package com.paragon.messaging.whatsapp.response;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Context for WhatsApp responses including reaction and reply metadata.
 *
 * <p>Provides additional context for how responses should be sent, including:</p>
 * <ul>
 *   <li>Reply context (quote a specific message)</li>
 *   <li>Reactions (send an emoji reaction)</li>
 *   <li>URL preview settings</li>
 *   <li>Typing indicators</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple response (no special context)
 * ResponseContext context = ResponseContext.simple();
 *
 * // Reply to a specific message
 * ResponseContext context = ResponseContext.replyTo("wamid.xyz123");
 *
 * // React to user's message
 * ResponseContext context = ResponseContext.withReaction("wamid.xyz123", "👍");
 *
 * // Combine reply and reaction
 * ResponseContext context = ResponseContext.builder()
 *     .replyTo("wamid.xyz123")
 *     .reactTo("wamid.xyz123", "🎉")
 *     .typingIndicator(Duration.ofSeconds(2))
 *     .build();
 * }</pre>
 *
 * @param replyToMessageId    message ID to quote/reply to
 * @param reactionEmoji       emoji to react with
 * @param reactToMessageId    message ID to react to
 * @param previewUrl          whether to enable URL preview in text messages
 * @param typingIndicator     duration to show typing indicator before sending
 * @author Agentle Team
 * @since 2.1
 */
public record ResponseContext(
        @Nullable String replyToMessageId,
        @Nullable String reactionEmoji,
        @Nullable String reactToMessageId,
        boolean previewUrl,
        @Nullable Duration typingIndicator
) {

    private static final ResponseContext SIMPLE = new ResponseContext(null, null, null, true, null);

    /**
     * Creates a simple response context with no special settings.
     *
     * <p>URL preview is enabled by default.</p>
     *
     * @return simple context
     */
    public static ResponseContext simple() {
        return SIMPLE;
    }

    /**
     * Creates a context that replies to a specific message.
     *
     * <p>The response will appear as a quoted reply in WhatsApp.</p>
     *
     * @param messageId the message ID to reply to
     * @return reply context
     */
    public static ResponseContext replyTo(@NonNull String messageId) {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        return new ResponseContext(messageId, null, null, true, null);
    }

    /**
     * Creates a context that reacts to a message with an emoji.
     *
     * <p>The reaction is sent as a separate message before the main response.</p>
     *
     * @param messageId the message ID to react to
     * @param emoji     the emoji reaction
     * @return reaction context
     */
    public static ResponseContext withReaction(@NonNull String messageId, @NonNull String emoji) {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(emoji, "emoji cannot be null");
        return new ResponseContext(null, emoji, messageId, true, null);
    }

    /**
     * Creates a context that both replies to and reacts to a message.
     *
     * @param messageId the message ID to reply to and react to
     * @param emoji     the emoji reaction
     * @return combined reply and reaction context
     */
    public static ResponseContext replyAndReact(@NonNull String messageId, @NonNull String emoji) {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(emoji, "emoji cannot be null");
        return new ResponseContext(messageId, emoji, messageId, true, null);
    }

    /**
     * Creates a builder for custom response context.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if this context has a reply-to message.
     *
     * @return true if replyToMessageId is set
     */
    public boolean hasReply() {
        return replyToMessageId != null && !replyToMessageId.isBlank();
    }

    /**
     * Checks if this context has a reaction.
     *
     * @return true if both reactionEmoji and reactToMessageId are set
     */
    public boolean hasReaction() {
        return reactionEmoji != null && !reactionEmoji.isBlank()
                && reactToMessageId != null && !reactToMessageId.isBlank();
    }

    /**
     * Checks if a typing indicator should be shown.
     *
     * @return true if typingIndicator is set and positive
     */
    public boolean hasTypingIndicator() {
        return typingIndicator != null && !typingIndicator.isNegative() && !typingIndicator.isZero();
    }

    /**
     * Builder for ResponseContext.
     */
    public static final class Builder {
        private String replyToMessageId;
        private String reactionEmoji;
        private String reactToMessageId;
        private boolean previewUrl = true;
        private Duration typingIndicator;

        private Builder() {}

        /**
         * Sets the message ID to reply to (quote).
         *
         * @param messageId the message ID
         * @return this builder
         */
        public Builder replyTo(@Nullable String messageId) {
            this.replyToMessageId = messageId;
            return this;
        }

        /**
         * Sets the reaction emoji and target message.
         *
         * @param messageId the message ID to react to
         * @param emoji     the emoji reaction
         * @return this builder
         */
        public Builder reactTo(@NonNull String messageId, @NonNull String emoji) {
            this.reactToMessageId = messageId;
            this.reactionEmoji = emoji;
            return this;
        }

        /**
         * Sets just the reaction emoji (uses replyToMessageId as target if set).
         *
         * @param emoji the emoji reaction
         * @return this builder
         */
        public Builder reaction(@NonNull String emoji) {
            this.reactionEmoji = emoji;
            return this;
        }

        /**
         * Enables or disables URL preview in text messages.
         *
         * @param preview true to enable URL preview
         * @return this builder
         */
        public Builder previewUrl(boolean preview) {
            this.previewUrl = preview;
            return this;
        }

        /**
         * Sets the typing indicator duration.
         *
         * <p>When set, a typing indicator will be shown for this duration
         * before the response is sent.</p>
         *
         * @param duration the typing indicator duration
         * @return this builder
         */
        public Builder typingIndicator(@Nullable Duration duration) {
            this.typingIndicator = duration;
            return this;
        }

        /**
         * Builds the ResponseContext.
         *
         * @return the built context
         */
        public ResponseContext build() {
            // If reaction target not set but reply is, use reply target
            String effectiveReactTo = reactToMessageId;
            if (effectiveReactTo == null && reactionEmoji != null && replyToMessageId != null) {
                effectiveReactTo = replyToMessageId;
            }

            return new ResponseContext(
                    replyToMessageId,
                    reactionEmoji,
                    effectiveReactTo,
                    previewUrl,
                    typingIndicator
            );
        }
    }
}
