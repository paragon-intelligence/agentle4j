package com.paragon.agents.context;

import java.util.List;
import org.jspecify.annotations.NonNull;

import com.paragon.responses.spec.Image;
import com.paragon.responses.spec.ResponseInputItem;

/**
 * Interface for counting tokens in conversation content.
 *
 * <p>Token counting is used by {@link ContextWindowStrategy} implementations to determine
 * when context exceeds the maximum token limit and needs to be managed.
 *
 * <p>Implementations should handle all content types that may appear in conversation history,
 * including text, images, and tool call results.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * TokenCounter counter = new SimpleTokenCounter();
 * 
 * int textTokens = counter.countText("Hello, how can I help you?");
 * int imageTokens = counter.countImage(Image.fromUrl("https://example.com/image.jpg"));
 * 
 * List<ResponseInputItem> history = context.getHistory();
 * int totalTokens = counter.countTokens(history);
 * }</pre>
 *
 * @see SimpleTokenCounter
 * @see ContextWindowStrategy
 * @since 1.0
 */
public interface TokenCounter {

    /**
     * Counts tokens for a single response input item.
     *
     * @param item the input item to count tokens for
     * @return the estimated token count
     */
    int countTokens(@NonNull ResponseInputItem item);

    /**
     * Counts tokens for a list of response input items.
     *
     * @param items the input items to count tokens for
     * @return the total estimated token count
     */
    default int countTokens(@NonNull List<ResponseInputItem> items) {
        return items.stream()
            .mapToInt(this::countTokens)
            .sum();
    }

    /**
     * Counts tokens for a text string.
     *
     * @param text the text to count tokens for
     * @return the estimated token count
     */
    int countText(@NonNull String text);

    /**
     * Counts tokens for an image.
     *
     * <p>Image token costs vary based on detail level and resolution.
     *
     * @param image the image to count tokens for
     * @return the estimated token count
     */
    int countImage(@NonNull Image image);
}
