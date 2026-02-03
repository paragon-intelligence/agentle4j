package com.paragon.messaging.whatsapp;

import com.paragon.messaging.whatsapp.payload.ContactMessage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Sealed interface representing all outbound message types for WhatsApp.
 *
 * <p>These are messages sent FROM the application TO WhatsApp users.
 * For inbound messages received from webhook payloads, see
 * {@link com.paragon.messaging.whatsapp.payload.InboundMessage}.</p>
 *
 * <h2>Supported Message Types</h2>
 * <ul>
 *   <li>{@link TextMessage} - Plain text messages</li>
 *   <li>{@link MediaMessage} - Images, videos, audio, documents, stickers</li>
 *   <li>{@link TemplateMessage} - Pre-approved template messages</li>
 *   <li>{@link InteractiveMessage} - Buttons, lists, CTA URLs</li>
 *   <li>{@link LocationMessage} - Geographic location sharing</li>
 *   <li>{@link ContactMessage} - Contact card (vCard)</li>
 *   <li>{@link ReactionMessage} - Emoji reactions to messages</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Text message
 * OutboundMessage text = new TextMessage("Hello, World!");
 *
 * // Interactive buttons
 * OutboundMessage buttons = InteractiveMessage.ButtonMessage.builder()
 *     .body("Choose an option:")
 *     .addButton("opt1", "Option 1")
 *     .addButton("opt2", "Option 2")
 *     .build();
 *
 * // Send via provider
 * messagingProvider.sendMessage(recipient, text);
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 */
public sealed interface OutboundMessage permits
        TextMessage,
        MediaMessage,
        TemplateMessage,
        InteractiveMessage,
        LocationMessage,
        ContactMessage,
        ReactionMessage {

    /**
     * Returns the message type for API serialization.
     *
     * @return the outbound message type
     */
    @NonNull OutboundMessageType type();

    /**
     * Extracts the primary text content from this message.
     *
     * <p>This method is used for:</p>
     * <ul>
     *   <li>Building conversation history for AI context</li>
     *   <li>Logging and debugging</li>
     *   <li>Message search and indexing</li>
     * </ul>
     *
     * <p>For non-text messages, returns a description or placeholder:</p>
     * <ul>
     *   <li>Text: Returns the body text</li>
     *   <li>Image/Video: Returns caption or "[Image]"/"[Video]"</li>
     *   <li>Audio: Returns "[Audio message]"</li>
     *   <li>Location: Returns name or "Location at lat,lng"</li>
     *   <li>Reaction: Returns the emoji</li>
     *   <li>Interactive: Returns the body text</li>
     * </ul>
     *
     * @return extracted text content, never null but may be empty
     */
    default @NonNull String extractTextContent() {
        return switch (this) {
            case TextMessage text -> text.body();
            case LocationMessage loc -> loc.name()
                    .orElseGet(() -> "Location at " + loc.toCoordinatesString());
            case ReactionMessage react -> react.emoji().orElse("");
            case InteractiveMessage interactive -> interactive.body();
            case MediaMessage media -> extractMediaContent(media);
            case TemplateMessage template -> "Template: " + template.name();
            case ContactMessage contact -> "Contact: " + contact.contacts().stream()
                    .map(ContactMessage.Contact::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        };
    }

    /**
     * Returns the message ID this message is replying to, if any.
     *
     * <p>When set, the WhatsApp client will display this message as a reply
     * to the referenced message.</p>
     *
     * @return the message ID to reply to, or null if not a reply
     */
    default @Nullable String replyToMessageId() {
        return null;
    }

    /**
     * Creates a copy of this message with a reply-to context.
     *
     * <p>Note: Not all message types support this operation. Implementations
     * that support replies should override this method.</p>
     *
     * @param messageId the message ID to reply to
     * @return a new message instance with reply context, or the same instance if unsupported
     */
    default @NonNull OutboundMessage withReplyTo(@NonNull String messageId) {
        return this;
    }

    /**
     * Checks if this message has any text content that can be extracted.
     *
     * @return true if extractTextContent() would return non-empty string
     */
    default boolean hasTextContent() {
        return !extractTextContent().isBlank();
    }

    private static String extractMediaContent(MediaMessage media) {
        return switch (media) {
            case MediaMessage.Image img -> img.caption()
                    .map(c -> "[Image: " + c + "]")
                    .orElse("[Image]");
            case MediaMessage.Video vid -> vid.caption()
                    .map(c -> "[Video: " + c + "]")
                    .orElse("[Video]");
            case MediaMessage.Audio __ -> "[Audio message]";
            case MediaMessage.Document doc -> doc.filename()
                    .map(f -> "[Document: " + f + "]")
                    .orElse("[Document]");
            case MediaMessage.Sticker __ -> "[Sticker]";
        };
    }

    /**
     * Enum representing all outbound message types supported by WhatsApp.
     */
    enum OutboundMessageType {
        TEXT,
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT,
        STICKER,
        TEMPLATE,
        INTERACTIVE_BUTTON,
        INTERACTIVE_LIST,
        INTERACTIVE_CTA_URL,
        LOCATION,
        CONTACT,
        REACTION
    }
}
