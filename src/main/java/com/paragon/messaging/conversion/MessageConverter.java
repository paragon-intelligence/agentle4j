package com.paragon.messaging.conversion;

import com.paragon.messaging.core.OutboundMessage;
import com.paragon.messaging.whatsapp.payload.InboundMessage;
import com.paragon.responses.spec.AssistantMessage;
import com.paragon.responses.spec.UserMessage;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Converts between WhatsApp messages and framework message types.
 *
 * <p>This interface provides bidirectional conversion between:</p>
 * <ul>
 *   <li>{@link InboundMessage} (from WhatsApp) &rarr; {@link UserMessage} (for AI)</li>
 *   <li>{@link OutboundMessage} (to WhatsApp) &rarr; {@link AssistantMessage} (for history)</li>
 *   <li>AI text response &rarr; {@link OutboundMessage} (for sending)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MessageConverter converter = DefaultMessageConverter.create();
 *
 * // Convert incoming WhatsApp message to UserMessage for AI context
 * InboundMessage webhookMessage = ...;
 * UserMessage userMessage = converter.toUserMessage(webhookMessage);
 *
 * // Convert AI response to OutboundMessage for sending
 * String aiResponse = "Hello! How can I help you today?";
 * OutboundMessage outbound = converter.toOutboundMessage(aiResponse);
 *
 * // Convert batch of inbound messages to user messages
 * List<InboundMessage> batch = ...;
 * List<UserMessage> userMessages = converter.toUserMessages(batch);
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 * @see DefaultMessageConverter
 */
public interface MessageConverter {

    /**
     * Converts an inbound WhatsApp message to a framework UserMessage.
     *
     * <p>The conversion extracts text content from the inbound message:
     * <ul>
     *   <li>Text messages: body text</li>
     *   <li>Media messages: caption or type placeholder ([Image], [Video], etc.)</li>
     *   <li>Interactive messages: button/list selection text</li>
     *   <li>Location messages: coordinates description</li>
     * </ul>
     *
     * @param inbound the inbound WhatsApp message
     * @return a UserMessage containing the extracted content
     */
    @NonNull UserMessage toUserMessage(@NonNull InboundMessage inbound);

    /**
     * Converts multiple inbound WhatsApp messages to a single UserMessage.
     *
     * <p>When users send multiple messages in quick succession (message batching),
     * they should be combined into a single UserMessage for the AI context.
     * Messages are joined with newlines in chronological order.</p>
     *
     * @param inboundMessages the list of inbound messages (chronological order)
     * @return a single UserMessage containing combined content
     */
    @NonNull UserMessage toUserMessage(@NonNull List<? extends InboundMessage> inboundMessages);

    /**
     * Converts multiple inbound messages to individual UserMessages.
     *
     * <p>Use this when you need to preserve individual message boundaries
     * in conversation history.</p>
     *
     * @param inboundMessages the list of inbound messages
     * @return a list of UserMessages, one per inbound message
     */
    @NonNull List<UserMessage> toUserMessages(@NonNull List<? extends InboundMessage> inboundMessages);

    /**
     * Converts an outbound WhatsApp message to an AssistantMessage.
     *
     * <p>This is useful for building conversation history that includes
     * assistant responses. The outbound message's text content is extracted
     * and wrapped in an AssistantMessage.</p>
     *
     * @param outbound the outbound WhatsApp message
     * @return an AssistantMessage containing the extracted content
     */
    @NonNull AssistantMessage toAssistantMessage(@NonNull OutboundMessage outbound);

    /**
     * Converts multiple outbound messages to individual AssistantMessages.
     *
     * @param outboundMessages the list of outbound messages
     * @return a list of AssistantMessages
     */
    @NonNull List<AssistantMessage> toAssistantMessages(@NonNull List<? extends OutboundMessage> outboundMessages);

    /**
     * Converts a plain AI text response to an OutboundMessage.
     *
     * <p>This is the simplest conversion, wrapping the AI's text response
     * in a TextMessage ready for sending via WhatsApp.</p>
     *
     * @param aiResponse the AI's text response
     * @return an OutboundMessage (typically TextMessage)
     */
    @NonNull OutboundMessage toOutboundMessage(@NonNull String aiResponse);

    /**
     * Converts an AI text response to an OutboundMessage with reply context.
     *
     * <p>The resulting message will quote/reply to the specified message ID
     * when rendered in the WhatsApp client.</p>
     *
     * @param aiResponse the AI's text response
     * @param replyToMessageId the message ID to reply to
     * @return an OutboundMessage with reply context
     */
    @NonNull OutboundMessage toOutboundMessage(@NonNull String aiResponse, @NonNull String replyToMessageId);
}
