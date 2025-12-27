package com.paragon.agents.context;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

import com.paragon.responses.spec.*;

/**
 * A simple token counter that uses character-based estimation.
 *
 * <p>This implementation provides approximate token counts using the following heuristics:
 *
 * <ul>
 *   <li><b>Text:</b> {@code text.length() / 4} (approximately 4 characters per token for English)
 *   <li><b>Images:</b> Fixed costs based on {@link ImageDetail} level:
 *     <ul>
 *       <li>{@code HIGH}: 765 tokens
 *       <li>{@code LOW}: 85 tokens
 *       <li>{@code AUTO}: 170 tokens (estimated average)
 *     </ul>
 * </ul>
 *
 * <p>This counter is thread-safe and stateless.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * TokenCounter counter = new SimpleTokenCounter();
 * 
 * // Count text tokens
 * int tokens = counter.countText("Hello, world!"); // ~3 tokens
 * 
 * // Count with custom chars-per-token ratio
 * TokenCounter customCounter = new SimpleTokenCounter(3); // 3 chars per token
 * }</pre>
 *
 * @see TokenCounter
 * @since 1.0
 */
public final class SimpleTokenCounter implements TokenCounter {

    /** Default characters per token ratio (approximately 4 for English). */
    public static final int DEFAULT_CHARS_PER_TOKEN = 4;

    /** Token cost for high-detail images. */
    public static final int HIGH_DETAIL_IMAGE_TOKENS = 765;

    /** Token cost for low-detail images. */
    public static final int LOW_DETAIL_IMAGE_TOKENS = 85;

    /** Token cost for auto-detail images (estimated average). */
    public static final int AUTO_DETAIL_IMAGE_TOKENS = 170;

    private final int charsPerToken;

    /**
     * Creates a SimpleTokenCounter with default settings.
     */
    public SimpleTokenCounter() {
        this(DEFAULT_CHARS_PER_TOKEN);
    }

    /**
     * Creates a SimpleTokenCounter with a custom characters-per-token ratio.
     *
     * @param charsPerToken the number of characters to consider as one token
     * @throws IllegalArgumentException if charsPerToken is less than 1
     */
    public SimpleTokenCounter(int charsPerToken) {
        if (charsPerToken < 1) {
            throw new IllegalArgumentException("charsPerToken must be at least 1");
        }
        this.charsPerToken = charsPerToken;
    }

    @Override
    public int countTokens(@NonNull ResponseInputItem item) {
        Objects.requireNonNull(item, "item cannot be null");

        if (item instanceof Message message) {
            return countMessage(message);
        } else if (item instanceof FunctionToolCallOutput output) {
            return countToolOutput(output);
        } else {
            // Unknown type - estimate based on toString
            return countText(item.toString());
        }
    }

    @Override
    public int countText(@NonNull String text) {
        Objects.requireNonNull(text, "text cannot be null");
        if (text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / charsPerToken);
    }

    @Override
    public int countImage(@NonNull Image image) {
        Objects.requireNonNull(image, "image cannot be null");
        return switch (image.detail()) {
            case HIGH -> HIGH_DETAIL_IMAGE_TOKENS;
            case LOW -> LOW_DETAIL_IMAGE_TOKENS;
            case AUTO -> AUTO_DETAIL_IMAGE_TOKENS;
        };
    }

    /**
     * Counts tokens in a message, including all content items.
     */
    private int countMessage(Message message) {
        int total = 0;
        List<MessageContent> content = message.content();
        if (content != null) {
            for (MessageContent contentItem : content) {
                total += countContent(contentItem);
            }
        }
        // Add small overhead for message structure
        return total + 4;
    }

    /**
     * Counts tokens for a single content item.
     */
    private int countContent(MessageContent content) {
        if (content instanceof Text text) {
            return countText(text.text());
        } else if (content instanceof Image image) {
            return countImage(image);
        } else {
            // Unknown content type - use toString estimate
            return countText(content.toString());
        }
    }

    /**
     * Counts tokens in a tool call output.
     */
    private int countToolOutput(FunctionToolCallOutput output) {
        int total = 0;
        FunctionToolCallOutputKind kind = output.output();
        if (kind != null) {
            if (kind instanceof Text text) {
                total += countText(text.text());
            } else if (kind instanceof Image image) {
                total += countImage(image);
            } else {
                total += countText(kind.toString());
            }
        }
        // Add overhead for tool call structure
        return total + 10;
    }

    /**
     * Returns the characters-per-token ratio used by this counter.
     *
     * @return the chars per token ratio
     */
    public int charsPerToken() {
        return charsPerToken;
    }
}
