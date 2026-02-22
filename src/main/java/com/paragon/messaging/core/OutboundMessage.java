package com.paragon.messaging.core;

import com.paragon.messaging.whatsapp.messages.ContactMessage;
import com.paragon.messaging.whatsapp.messages.InteractiveMessage;
import com.paragon.messaging.whatsapp.messages.LocationMessage;
import com.paragon.messaging.whatsapp.messages.MediaMessage;
import com.paragon.messaging.whatsapp.messages.ReactionMessage;
import com.paragon.messaging.whatsapp.messages.TemplateMessage;
import com.paragon.messaging.whatsapp.messages.TextMessage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Sealed interface representing all outbound message types for WhatsApp.
 *
 * <p>These are messages sent FROM the application TO WhatsApp users. For inbound messages received
 * from webhook payloads, see {@link com.paragon.messaging.whatsapp.payload.InboundMessage}.
 *
 * <h2>Supported Message Types</h2>
 *
 * <ul>
 *   <li>{@link TextMessage} - Plain text messages
 *   <li>{@link MediaMessage} - Images, videos, audio, documents, stickers
 *   <li>{@link TemplateMessage} - Pre-approved template messages
 *   <li>{@link InteractiveMessage} - Buttons, lists, CTA URLs
 *   <li>{@link LocationMessage} - Geographic location sharing
 *   <li>{@link ContactMessage} - Contact card (vCard)
 *   <li>{@link ReactionMessage} - Emoji reactions to messages
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
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
public sealed interface OutboundMessage
    permits TextMessageInterface,
        MediaMessageInterface,
        TemplateMessageInterface,
        InteractiveMessageInterface,
        LocationMessageInterface,
        ContactMessageInterface,
        ReactionMessageInterface {

  /**
   * Returns the message type for API serialization.
   *
   * @return the outbound message type
   */
  @NonNull OutboundMessageType type();

  /**
   * Extracts the primary text content from this message.
   *
   * <p>This method is used for:
   *
   * <ul>
   *   <li>Building conversation history for AI context
   *   <li>Logging and debugging
   *   <li>Message search and indexing
   * </ul>
   *
   * <p>For non-text messages, returns a description or placeholder:
   *
   * <ul>
   *   <li>Text: Returns the body text
   *   <li>Image/Video: Returns caption or "[Image]"/"[Video]"
   *   <li>Audio: Returns "[Audio message]"
   *   <li>Location: Returns name or "Location at lat,lng"
   *   <li>Reaction: Returns the emoji
   *   <li>Interactive: Returns the body text
   * </ul>
   *
   * @return extracted text content, never null but may be empty
   */
  default @NonNull String extractTextContent() {
    return switch (this) {
      case TextMessageInterface text -> text.body();
      case LocationMessageInterface loc ->
          loc.name().orElseGet(() -> "Location at " + loc.toCoordinatesString());
      case ReactionMessageInterface react -> react.emoji().orElse("");
      case InteractiveMessageInterface interactive -> interactive.body();
      case MediaMessageInterface media -> extractMediaContent(media);
      case TemplateMessageInterface template -> "Template: " + template.name();
      case ContactMessageInterface contact ->
          "Contact: [contact]"; // ContactMessage details handled by implementation
    };
  }

  /**
   * Returns the message ID this message is replying to, if any.
   *
   * <p>When set, the WhatsApp client will display this message as a reply to the referenced
   * message.
   *
   * @return the message ID to reply to, or null if not a reply
   */
  default @Nullable String replyToMessageId() {
    return null;
  }

  /**
   * Creates a copy of this message with a reply-to context.
   *
   * <p>Note: Not all message types support this operation. Implementations that support replies
   * should override this method.
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

  private static String extractMediaContent(MediaMessageInterface media) {
    if (media instanceof com.paragon.messaging.whatsapp.messages.MediaMessage concreteMedia) {
      return switch (concreteMedia) {
        case com.paragon.messaging.whatsapp.messages.MediaMessage.Image img ->
            img.caption().map(c -> "[Image: " + c + "]").orElse("[Image]");
        case com.paragon.messaging.whatsapp.messages.MediaMessage.Video vid ->
            vid.caption().map(c -> "[Video: " + c + "]").orElse("[Video]");
        case com.paragon.messaging.whatsapp.messages.MediaMessage.Audio __ -> "[Audio message]";
        case com.paragon.messaging.whatsapp.messages.MediaMessage.Document doc ->
            doc.filename().map(f -> "[Document: " + f + "]").orElse("[Document]");
        case com.paragon.messaging.whatsapp.messages.MediaMessage.Sticker __ -> "[Sticker]";
      };
    }
    return "[Media]";
  }

  /** Enum representing all outbound message types supported by WhatsApp. */
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
