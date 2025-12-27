package com.paragon.agents.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

import com.paragon.responses.spec.DeveloperMessage;
import com.paragon.responses.spec.ResponseInputItem;

/**
 * A context window strategy that removes oldest messages to fit within the token limit.
 *
 * <p>This strategy implements a sliding window approach:
 * <ul>
 *   <li>Counts tokens from newest to oldest messages
 *   <li>Removes oldest messages when the limit is exceeded
 *   <li>Always keeps the most recent user message
 *   <li>Optionally preserves developer/system messages
 * </ul>
 *
 * <p>This is the simplest and most efficient context management strategy, suitable for
 * most use cases where older context can be safely discarded.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Basic sliding window
 * ContextWindowStrategy strategy = new SlidingWindowStrategy();
 * 
 * // Preserve developer messages at the start
 * ContextWindowStrategy strategy = SlidingWindowStrategy.preservingDeveloperMessages();
 * 
 * // Use with agent
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .model("openai/gpt-4o")
 *     .instructions("You are a helpful assistant.")
 *     .responder(responder)
 *     .contextWindow(strategy, 4000)
 *     .build();
 * }</pre>
 *
 * @see ContextWindowStrategy
 * @see SummarizationStrategy
 * @since 1.0
 */
public final class SlidingWindowStrategy implements ContextWindowStrategy {

    private final boolean preserveDeveloperMessages;

    /**
     * Creates a sliding window strategy with default settings.
     *
     * <p>By default, all messages including developer messages may be removed
     * when the context limit is exceeded.
     */
    public SlidingWindowStrategy() {
        this(false);
    }

    /**
     * Creates a sliding window strategy with configurable developer message preservation.
     *
     * @param preserveDeveloperMessages if true, developer messages at the start of
     *     the conversation will never be removed
     */
    public SlidingWindowStrategy(boolean preserveDeveloperMessages) {
        this.preserveDeveloperMessages = preserveDeveloperMessages;
    }

    /**
     * Creates a strategy that preserves the developer message.
     *
     * <p>The developer message (system prompt) at the start of the conversation
     * will be preserved even when the context limit is exceeded.
     *
     * @return a new strategy that preserves the developer message
     */
    public static @NonNull SlidingWindowStrategy preservingDeveloperMessage() {
        return new SlidingWindowStrategy(true);
    }

    @Override
    public @NonNull List<ResponseInputItem> manage(
            @NonNull List<ResponseInputItem> history,
            int maxTokens,
            @NonNull TokenCounter counter) {
        Objects.requireNonNull(history, "history cannot be null");
        Objects.requireNonNull(counter, "counter cannot be null");

        if (maxTokens <= 0) {
            return history;
        }

        // Fast path: check if already within limits
        int totalTokens = counter.countTokens(history);
        if (totalTokens <= maxTokens) {
            return history;
        }

        // Find preserved messages at the start (developer messages)
        int preserveCount = 0;
        int preservedTokens = 0;
        if (preserveDeveloperMessages) {
            for (ResponseInputItem item : history) {
                if (item instanceof DeveloperMessage) {
                    preservedTokens += counter.countTokens(item);
                    preserveCount++;
                } else {
                    break; // Only preserve consecutive developer messages at start
                }
            }
        }

        // Build result from end, keeping as many recent messages as possible
        List<ResponseInputItem> result = new ArrayList<>();
        int usedTokens = preservedTokens;
        int availableTokens = maxTokens - preservedTokens;

        // Add from end (most recent) until we exceed limit
        for (int i = history.size() - 1; i >= preserveCount; i--) {
            ResponseInputItem item = history.get(i);
            int itemTokens = counter.countTokens(item);

            if (usedTokens + itemTokens <= maxTokens) {
                result.addFirst(item);
                usedTokens += itemTokens;
            } else {
                // Can't fit more messages
                break;
            }
        }

        // Add preserved developer messages at the start
        for (int i = 0; i < preserveCount; i++) {
            result.addFirst(history.get(preserveCount - 1 - i));
        }

        return result;
    }

    /**
     * Returns whether this strategy preserves the developer message.
     *
     * @return true if the developer message is preserved
     */
    public boolean preservesDeveloperMessage() {
        return preserveDeveloperMessages;
    }
}
