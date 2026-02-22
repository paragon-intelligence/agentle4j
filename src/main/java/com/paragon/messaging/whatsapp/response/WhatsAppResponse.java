package com.paragon.messaging.whatsapp.response;

import com.paragon.messaging.core.OutboundMessage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Interface for structured AI responses that can be converted to WhatsApp messages.
 *
 * <p>Implement this interface in your structured output types to enable automatic conversion to
 * WhatsApp messages with support for reactions, replies, and multiple message types.
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Convert structured output to one or more WhatsApp messages
 *   <li>Support for reactions (emoji response to user message)
 *   <li>Support for reply context (quote user message)
 *   <li>Support for multiple message types (text, buttons, lists, media)
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public record MenuResponse(
 *     String greeting,
 *     List<MenuItem> items,
 *     String footer,
 *     ResponseContext context
 * ) implements WhatsAppResponse {
 *
 *     public record MenuItem(String id, String title, String description) {}
 *
 *     @Override
 *     public List<OutboundMessage> toMessages() {
 *         return List.of(
 *             InteractiveMessage.ListMessage.builder()
 *                 .body(greeting)
 *                 .buttonText("View Options")
 *                 .addSection("Menu", items.stream()
 *                     .map(i -> new ListRow(i.id, i.title, i.description))
 *                     .toList())
 *                 .footer(footer)
 *                 .build()
 *         );
 *     }
 *
 *     @Override
 *     public String getTextContent() {
 *         return greeting + "\n" + items.stream()
 *             .map(MenuItem::title)
 *             .collect(Collectors.joining(", "));
 *     }
 *
 *     @Override
 *     public @Nullable String getReplyToMessageId() {
 *         return context != null ? context.replyToMessageId() : null;
 *     }
 *
 *     @Override
 *     public @Nullable String getReactionEmoji() {
 *         return context != null ? context.reactionEmoji() : null;
 *     }
 *
 *     @Override
 *     public @Nullable String getReactToMessageId() {
 *         return context != null ? context.reactToMessageId() : null;
 *     }
 * }
 * }</pre>
 *
 * <h2>With Structured Agent</h2>
 *
 * <pre>{@code
 * Interactable.Structured<MenuResponse> agent = Agent.builder()
 *     .name("MenuBot")
 *     .instructions("Create menu responses. Include a greeting and menu items.")
 *     .structured(MenuResponse.class)
 *     .responder(responder)
 *     .build();
 *
 * // The AIAgentProcessor will automatically:
 * // 1. Send reaction if getReactionEmoji() returns non-null
 * // 2. Convert toMessages() to WhatsApp API calls
 * // 3. Add reply context to messages if getReplyToMessageId() is set
 * }</pre>
 *
 * @author Agentle Team
 * @see ResponseContext
 * @see com.paragon.messaging.processor.AIAgentProcessor
 * @since 2.1
 */
public interface WhatsAppResponse {

  /**
   * Converts this response to one or more WhatsApp outbound messages.
   *
   * <p>The returned messages are sent in order. For most responses, a single message is sufficient.
   * Multiple messages can be used for:
   *
   * <ul>
   *   <li>Sending text followed by an image
   *   <li>Sending multiple media items
   *   <li>Sending a message then buttons
   * </ul>
   *
   * @return list of outbound messages to send
   */
  @NonNull
  List<OutboundMessage> toMessages();

  /**
   * Returns the primary text content of this response.
   *
   * <p>Used for:
   *
   * <ul>
   *   <li>Conversation history storage
   *   <li>TTS synthesis (if enabled)
   *   <li>Logging and debugging
   * </ul>
   *
   * @return the text content, or empty string if no text
   */
  @NonNull
  String getTextContent();

  /**
   * Returns the emoji to react with, if any.
   *
   * <p>When set, a reaction message is sent before the main response. The reaction is sent to the
   * message specified by {@link #getReactToMessageId()}.
   *
   * @return the reaction emoji, or null for no reaction
   */
  default @Nullable String getReactionEmoji() {
    return null;
  }

  /**
   * Returns the message ID to react to, if any.
   *
   * <p>Required when {@link #getReactionEmoji()} returns non-null.
   *
   * @return the message ID to react to, or null
   */
  default @Nullable String getReactToMessageId() {
    return null;
  }

  /**
   * Returns the message ID to reply to (quote), if any.
   *
   * <p>When set, the response messages will appear as replies/quotes to the specified message in
   * the WhatsApp client.
   *
   * @return the message ID to reply to, or null
   */
  default @Nullable String getReplyToMessageId() {
    return null;
  }

  /**
   * Returns the response context, if any.
   *
   * <p>The context provides additional metadata about how the response should be sent. If this
   * returns non-null, the individual getters (getReactionEmoji, getReplyToMessageId, etc.) should
   * delegate to it.
   *
   * @return the response context, or null
   */
  default @Nullable ResponseContext getContext() {
    return null;
  }

  /**
   * Checks if this response has any reaction.
   *
   * @return true if getReactionEmoji() and getReactToMessageId() are non-null
   */
  default boolean hasReaction() {
    String emoji = getReactionEmoji();
    String messageId = getReactToMessageId();
    return emoji != null && !emoji.isBlank() && messageId != null && !messageId.isBlank();
  }

  /**
   * Checks if this response should be sent as a reply.
   *
   * @return true if getReplyToMessageId() is non-null
   */
  default boolean hasReply() {
    String messageId = getReplyToMessageId();
    return messageId != null && !messageId.isBlank();
  }
}
