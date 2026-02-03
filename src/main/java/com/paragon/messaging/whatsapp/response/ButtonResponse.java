package com.paragon.messaging.whatsapp.response;

import com.paragon.messaging.whatsapp.messages.InteractiveMessage;
import com.paragon.messaging.whatsapp.messages.InteractiveMessage.ReplyButton;
import com.paragon.messaging.core.OutboundMessage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Button-based response for structured AI output.
 *
 * <p>Represents an interactive message with quick reply buttons (max 3).</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple button response
 * ButtonResponse response = ButtonResponse.builder()
 *     .body("How would you like to proceed?")
 *     .addButton("confirm", "Confirm")
 *     .addButton("cancel", "Cancel")
 *     .build();
 *
 * // With header, footer, and context
 * ButtonResponse response = ButtonResponse.builder()
 *     .header("Order Confirmation")
 *     .body("Your order is ready. Confirm to proceed with payment.")
 *     .footer("Tap a button to continue")
 *     .addButton("pay", "Pay Now")
 *     .addButton("later", "Pay Later")
 *     .addButton("cancel", "Cancel Order")
 *     .replyTo("wamid.xyz123")
 *     .build();
 * }</pre>
 *
 * @param body    the message body text
 * @param buttons the list of reply buttons (1-3)
 * @param header  optional header text
 * @param footer  optional footer text
 * @param context optional response context
 * @author Agentle Team
 * @since 2.1
 */
public record ButtonResponse(
        @NonNull String body,
        @NonNull List<Button> buttons,
        @Nullable String header,
        @Nullable String footer,
        @Nullable ResponseContext context
) implements WhatsAppResponse {

    public static final int MAX_BUTTONS = 3;

    public ButtonResponse {
        Objects.requireNonNull(body, "body cannot be null");
        Objects.requireNonNull(buttons, "buttons cannot be null");
        if (buttons.isEmpty()) {
            throw new IllegalArgumentException("At least one button is required");
        }
        if (buttons.size() > MAX_BUTTONS) {
            throw new IllegalArgumentException("Maximum " + MAX_BUTTONS + " buttons allowed");
        }
        buttons = List.copyOf(buttons);
    }

    /**
     * Creates a builder for ButtonResponse.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public @NonNull List<OutboundMessage> toMessages() {
        InteractiveMessage.ButtonMessage.Builder builder = InteractiveMessage.ButtonMessage.builder()
                .body(body);

        if (header != null) {
            builder.header(header);
        }
        if (footer != null) {
            builder.footer(footer);
        }

        for (Button button : buttons) {
            builder.addButton(button.id, button.title);
        }

        return List.of(builder.build());
    }

    @Override
    public @NonNull String getTextContent() {
        String buttonText = buttons.stream()
                .map(Button::title)
                .collect(Collectors.joining(", "));
        return body + " [" + buttonText + "]";
    }

    @Override
    public @Nullable String getReactionEmoji() {
        return context != null ? context.reactionEmoji() : null;
    }

    @Override
    public @Nullable String getReactToMessageId() {
        return context != null ? context.reactToMessageId() : null;
    }

    @Override
    public @Nullable String getReplyToMessageId() {
        return context != null ? context.replyToMessageId() : null;
    }

    @Override
    public @Nullable ResponseContext getContext() {
        return context;
    }

    /**
     * Represents a reply button.
     *
     * @param id    unique button identifier
     * @param title button display text (max 20 characters)
     */
    public record Button(@NonNull String id, @NonNull String title) {
        public Button {
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(title, "title cannot be null");
            if (title.length() > ReplyButton.MAX_TITLE_LENGTH) {
                throw new IllegalArgumentException(
                        "Button title cannot exceed " + ReplyButton.MAX_TITLE_LENGTH + " characters");
            }
        }
    }

    /**
     * Builder for ButtonResponse.
     */
    public static final class Builder {
        private String body;
        private final List<Button> buttons = new ArrayList<>();
        private String header;
        private String footer;
        private ResponseContext.Builder contextBuilder;

        private Builder() {}

        /**
         * Sets the message body text.
         *
         * @param body the body text
         * @return this builder
         */
        public Builder body(@NonNull String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the optional header text.
         *
         * @param header the header text
         * @return this builder
         */
        public Builder header(@Nullable String header) {
            this.header = header;
            return this;
        }

        /**
         * Sets the optional footer text.
         *
         * @param footer the footer text
         * @return this builder
         */
        public Builder footer(@Nullable String footer) {
            this.footer = footer;
            return this;
        }

        /**
         * Adds a button to the response.
         *
         * @param id    unique button identifier
         * @param title button display text
         * @return this builder
         */
        public Builder addButton(@NonNull String id, @NonNull String title) {
            this.buttons.add(new Button(id, title));
            return this;
        }

        /**
         * Adds a button to the response.
         *
         * @param button the button to add
         * @return this builder
         */
        public Builder addButton(@NonNull Button button) {
            this.buttons.add(button);
            return this;
        }

        /**
         * Sets the message ID to reply to.
         *
         * @param messageId the message ID
         * @return this builder
         */
        public Builder replyTo(@NonNull String messageId) {
            ensureContextBuilder().replyTo(messageId);
            return this;
        }

        /**
         * Sets the reaction for this response.
         *
         * @param messageId the message ID to react to
         * @param emoji     the reaction emoji
         * @return this builder
         */
        public Builder reactTo(@NonNull String messageId, @NonNull String emoji) {
            ensureContextBuilder().reactTo(messageId, emoji);
            return this;
        }

        private ResponseContext.Builder ensureContextBuilder() {
            if (contextBuilder == null) {
                contextBuilder = ResponseContext.builder();
            }
            return contextBuilder;
        }

        /**
         * Builds the ButtonResponse.
         *
         * @return the built response
         */
        public ButtonResponse build() {
            ResponseContext context = contextBuilder != null ? contextBuilder.build() : null;
            return new ButtonResponse(body, buttons, header, footer, context);
        }
    }
}
