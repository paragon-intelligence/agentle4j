package com.paragon.messaging.whatsapp.conversion;

import com.paragon.messaging.whatsapp.OutboundMessage;
import com.paragon.messaging.whatsapp.TextMessage;
import com.paragon.messaging.whatsapp.payload.InboundMessage;
import com.paragon.responses.spec.AssistantMessage;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.UserMessage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link MessageConverter}.
 *
 * <p>Provides straightforward conversion between WhatsApp message types
 * and the framework's UserMessage/AssistantMessage types for AI context.</p>
 *
 * <h2>Conversion Behavior</h2>
 * <ul>
 *   <li><b>Text messages:</b> Body text extracted directly</li>
 *   <li><b>Media messages:</b> Caption if present, otherwise type placeholder
 *       (e.g., "[Image]", "[Video: my_file.mp4]")</li>
 *   <li><b>Interactive messages:</b> Button or list selection text</li>
 *   <li><b>Location messages:</b> Coordinates description</li>
 *   <li><b>System/Order messages:</b> Type placeholders</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MessageConverter converter = DefaultMessageConverter.create();
 *
 * // Single message conversion
 * InboundMessage inbound = ...;
 * UserMessage user = converter.toUserMessage(inbound);
 *
 * // Batch conversion (combines into single message)
 * List<InboundMessage> batch = ...;
 * UserMessage combined = converter.toUserMessage(batch);
 *
 * // With custom message separator
 * MessageConverter custom = DefaultMessageConverter.builder()
 *     .batchSeparator(" | ")
 *     .build();
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 * @see MessageConverter
 */
public final class DefaultMessageConverter implements MessageConverter {

    private static final String DEFAULT_BATCH_SEPARATOR = "\n";
    private static final DefaultMessageConverter INSTANCE = new DefaultMessageConverter(DEFAULT_BATCH_SEPARATOR);

    private final String batchSeparator;

    private DefaultMessageConverter(@NonNull String batchSeparator) {
        this.batchSeparator = Objects.requireNonNull(batchSeparator, "batchSeparator cannot be null");
    }

    /**
     * Creates a new DefaultMessageConverter with default settings.
     *
     * <p>Uses newline as the batch separator when combining multiple messages.</p>
     *
     * @return a new converter instance
     */
    public static DefaultMessageConverter create() {
        return INSTANCE;
    }

    /**
     * Creates a builder for customizing the converter.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public @NonNull UserMessage toUserMessage(@NonNull InboundMessage inbound) {
        Objects.requireNonNull(inbound, "inbound message cannot be null");
        String content = inbound.extractTextContent();
        return Message.user(content);
    }

    @Override
    public @NonNull UserMessage toUserMessage(@NonNull List<? extends InboundMessage> inboundMessages) {
        Objects.requireNonNull(inboundMessages, "inboundMessages cannot be null");

        if (inboundMessages.isEmpty()) {
            return Message.user("");
        }

        if (inboundMessages.size() == 1) {
            return toUserMessage(inboundMessages.getFirst());
        }

        String combined = inboundMessages.stream()
                .map(InboundMessage::extractTextContent)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(batchSeparator));

        return Message.user(combined);
    }

    @Override
    public @NonNull List<UserMessage> toUserMessages(@NonNull List<? extends InboundMessage> inboundMessages) {
        Objects.requireNonNull(inboundMessages, "inboundMessages cannot be null");

        return inboundMessages.stream()
                .map(this::toUserMessage)
                .toList();
    }

    @Override
    public @NonNull AssistantMessage toAssistantMessage(@NonNull OutboundMessage outbound) {
        Objects.requireNonNull(outbound, "outbound message cannot be null");
        String content = outbound.extractTextContent();
        return Message.assistant(content);
    }

    @Override
    public @NonNull List<AssistantMessage> toAssistantMessages(@NonNull List<? extends OutboundMessage> outboundMessages) {
        Objects.requireNonNull(outboundMessages, "outboundMessages cannot be null");

        return outboundMessages.stream()
                .map(this::toAssistantMessage)
                .toList();
    }

    @Override
    public @NonNull OutboundMessage toOutboundMessage(@NonNull String aiResponse) {
        Objects.requireNonNull(aiResponse, "aiResponse cannot be null");
        return new TextMessage(aiResponse);
    }

    @Override
    public @NonNull OutboundMessage toOutboundMessage(@NonNull String aiResponse, @NonNull String replyToMessageId) {
        Objects.requireNonNull(aiResponse, "aiResponse cannot be null");
        Objects.requireNonNull(replyToMessageId, "replyToMessageId cannot be null");
        return TextMessage.builder()
                .body(aiResponse)
                .replyTo(replyToMessageId)
                .build();
    }

    /**
     * Builder for customizing DefaultMessageConverter.
     */
    public static final class Builder {
        private String batchSeparator = DEFAULT_BATCH_SEPARATOR;

        private Builder() {}

        /**
         * Sets the separator used when combining multiple messages into one.
         *
         * <p>Default is newline ({@code "\n"}).</p>
         *
         * @param separator the batch separator
         * @return this builder
         */
        public Builder batchSeparator(@NonNull String separator) {
            this.batchSeparator = Objects.requireNonNull(separator, "separator cannot be null");
            return this;
        }

        /**
         * Builds the configured DefaultMessageConverter.
         *
         * @return the converter instance
         */
        public DefaultMessageConverter build() {
            return new DefaultMessageConverter(batchSeparator);
        }
    }
}
