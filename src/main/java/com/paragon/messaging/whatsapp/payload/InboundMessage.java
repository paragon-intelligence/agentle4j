package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Sealed interface for inbound WhatsApp webhook messages.
 *
 * <p>These are messages received FROM WhatsApp users TO the application
 * via the Meta WhatsApp Business API webhook. For outbound messages sent
 * to users, see {@link com.paragon.messaging.whatsapp.OutboundMessage}.</p>
 *
 * <h2>Supported Inbound Message Types</h2>
 * <ul>
 *   <li>{@link TextMessage} - Plain text messages</li>
 *   <li>{@link ImageMessage} - Image attachments</li>
 *   <li>{@link VideoMessage} - Video attachments</li>
 *   <li>{@link AudioMessage} - Voice messages and audio files</li>
 *   <li>{@link DocumentMessage} - Document/file attachments</li>
 *   <li>{@link StickerMessage} - Sticker messages</li>
 *   <li>{@link InteractiveMessage} - Button and list replies</li>
 *   <li>{@link LocationMessage} - Shared locations</li>
 *   <li>{@link ReactionMessage} - Emoji reactions</li>
 *   <li>{@link SystemMessage} - System notifications</li>
 *   <li>{@link OrderMessage} - Commerce order messages</li>
 * </ul>
 *
 * <h2>JSON Deserialization</h2>
 * <p>This interface uses Jackson polymorphic type handling to automatically
 * deserialize webhook payloads to the correct message type based on the
 * {@code type} field.</p>
 *
 * @author Agentle Team
 * @since 2.1
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextMessage.class, name = "text"),
        @JsonSubTypes.Type(value = ImageMessage.class, name = "image"),
        @JsonSubTypes.Type(value = VideoMessage.class, name = "video"),
        @JsonSubTypes.Type(value = AudioMessage.class, name = "audio"),
        @JsonSubTypes.Type(value = DocumentMessage.class, name = "document"),
        @JsonSubTypes.Type(value = StickerMessage.class, name = "sticker"),
        @JsonSubTypes.Type(value = InteractiveMessage.class, name = "interactive"),
        @JsonSubTypes.Type(value = LocationMessage.class, name = "location"),
        @JsonSubTypes.Type(value = ReactionMessage.class, name = "reaction"),
        @JsonSubTypes.Type(value = SystemMessage.class, name = "system"),
        @JsonSubTypes.Type(value = OrderMessage.class, name = "order")
})
public sealed interface InboundMessage permits
        AbstractInboundMessage, TextMessage, ImageMessage, VideoMessage, AudioMessage,
        DocumentMessage, StickerMessage, InteractiveMessage, LocationMessage,
        ReactionMessage, SystemMessage, OrderMessage {

    /**
     * Returns the sender's WhatsApp ID (phone number).
     *
     * @return the sender's WhatsApp ID
     */
    @NonNull String from();

    /**
     * Returns the unique message ID assigned by WhatsApp.
     *
     * <p>This ID can be used for:</p>
     * <ul>
     *   <li>Message deduplication</li>
     *   <li>Replying to specific messages</li>
     *   <li>Sending reactions</li>
     *   <li>Tracking delivery status</li>
     * </ul>
     *
     * @return the unique message ID
     */
    @NonNull String id();

    /**
     * Returns the message timestamp (Unix epoch seconds).
     *
     * @return the message timestamp
     */
    @NonNull String timestamp();

    /**
     * Returns the message type identifier.
     *
     * <p>Common values: "text", "image", "video", "audio", "document",
     * "sticker", "interactive", "location", "reaction", "system", "order"</p>
     *
     * @return the message type
     */
    @NonNull String type();

    /**
     * Returns the message context if this is a reply to another message.
     *
     * <p>The context contains information about the quoted message,
     * including its ID and whether it was forwarded.</p>
     *
     * @return the message context, or null if not a reply
     */
    @Nullable MessageContext context();

    /**
     * Extracts the primary text content from this inbound message.
     *
     * <p>For non-text messages, returns a descriptive placeholder:</p>
     * <ul>
     *   <li>Text: Returns the message body</li>
     *   <li>Image/Video: Returns caption or "[Image]"/"[Video]"</li>
     *   <li>Audio: Returns "[Audio message]"</li>
     *   <li>Location: Returns "[Location]"</li>
     *   <li>Interactive: Returns button/list selection</li>
     * </ul>
     *
     * @return extracted text content for AI processing
     */
    default @NonNull String extractTextContent() {
        return switch (this) {
            case TextMessage text -> text.text != null ? text.text.body : "";
            case ImageMessage img -> img.caption != null ? "[Image: " + img.caption + "]" : "[Image]";
            case VideoMessage vid -> vid.caption != null ? "[Video: " + vid.caption + "]" : "[Video]";
            case AudioMessage __ -> "[Audio message]";
            case DocumentMessage doc -> doc.filename != null ? "[Document: " + doc.filename + "]" : "[Document]";
            case StickerMessage __ -> "[Sticker]";
            case LocationMessage loc -> "[Location: " + loc.latitude + ", " + loc.longitude + "]";
            case InteractiveMessage inter -> extractInteractiveContent(inter);
            case ReactionMessage react -> react.emoji != null ? react.emoji : "";
            case SystemMessage __ -> "[System message]";
            case OrderMessage __ -> "[Order message]";
            case AbstractInboundMessage __ -> "[Unknown message type]";
        };
    }

    /**
     * Checks if this message is a reply to another message.
     *
     * @return true if this message has reply context
     */
    default boolean isReply() {
        return context() != null;
    }

    /**
     * Returns the ID of the message this is replying to, if any.
     *
     * @return the replied message ID, or null
     */
    default @Nullable String repliedToMessageId() {
        MessageContext ctx = context();
        return ctx != null ? ctx.id() : null;
    }

    private static String extractInteractiveContent(InteractiveMessage interactive) {
        if (interactive.buttonReply != null) {
            return "Button: " + interactive.buttonReply.title;
        }
        if (interactive.listReply != null) {
            return "List: " + interactive.listReply.title;
        }
        return "[Interactive response]";
    }
}
