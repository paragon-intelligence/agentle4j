package com.paragon.agents.context;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for context management in agents.
 *
 * <p>This class encapsulates all settings for managing conversation context length,
 * including the strategy to use, the maximum token limit, and the token counter.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Basic sliding window configuration
 * ContextManagementConfig config = ContextManagementConfig.builder()
 *     .strategy(new SlidingWindowStrategy())
 *     .maxTokens(4000)
 *     .build();
 * 
 * // With custom token counter
 * ContextManagementConfig config = ContextManagementConfig.builder()
 *     .strategy(new SlidingWindowStrategy())
 *     .maxTokens(4000)
 *     .tokenCounter(new MyCustomTokenCounter())
 *     .build();
 * 
 * // Use with agent
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .model("openai/gpt-4o")
 *     .responder(responder)
 *     .contextManagement(config)
 *     .build();
 * }</pre>
 *
 * @see ContextWindowStrategy
 * @see SlidingWindowStrategy
 * @see SummarizationStrategy
 * @since 1.0
 */
public final class ContextManagementConfig {

    private final @NonNull ContextWindowStrategy strategy;
    private final int maxTokens;
    private final @NonNull TokenCounter tokenCounter;

    private ContextManagementConfig(Builder builder) {
        this.strategy = Objects.requireNonNull(builder.strategy, "strategy is required");
        if (builder.maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        this.maxTokens = builder.maxTokens;
        this.tokenCounter = builder.tokenCounter != null
            ? builder.tokenCounter
            : new SimpleTokenCounter();
    }

    /**
     * Creates a new builder for ContextManagementConfig.
     *
     * @return a new builder instance
     */
    public static @NonNull Builder builder() {
        return new Builder();
    }

    /**
     * Returns the context window strategy.
     *
     * @return the strategy
     */
    public @NonNull ContextWindowStrategy strategy() {
        return strategy;
    }

    /**
     * Returns the maximum number of tokens allowed in context.
     *
     * @return the max tokens limit
     */
    public int maxTokens() {
        return maxTokens;
    }

    /**
     * Returns the token counter used for measuring content.
     *
     * @return the token counter
     */
    public @NonNull TokenCounter tokenCounter() {
        return tokenCounter;
    }

    /**
     * Builder for ContextManagementConfig.
     */
    public static final class Builder {
        private @Nullable ContextWindowStrategy strategy;
        private int maxTokens;
        private @Nullable TokenCounter tokenCounter;

        private Builder() {}

        /**
         * Sets the context window strategy (required).
         *
         * @param strategy the strategy to use for managing context
         * @return this builder
         * @see SlidingWindowStrategy
         * @see SummarizationStrategy
         */
        public @NonNull Builder strategy(@NonNull ContextWindowStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy);
            return this;
        }

        /**
         * Sets the maximum number of tokens allowed in context (required).
         *
         * <p>When context exceeds this limit, the strategy will be applied
         * to reduce it.
         *
         * @param maxTokens the maximum token limit
         * @return this builder
         */
        public @NonNull Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * Sets a custom token counter.
         *
         * <p>If not set, {@link SimpleTokenCounter} is used by default.
         *
         * @param tokenCounter the token counter to use
         * @return this builder
         */
        public @NonNull Builder tokenCounter(@NonNull TokenCounter tokenCounter) {
            this.tokenCounter = Objects.requireNonNull(tokenCounter);
            return this;
        }

        /**
         * Builds the ContextManagementConfig.
         *
         * @return a new ContextManagementConfig
         * @throws NullPointerException if strategy is null
         * @throws IllegalArgumentException if maxTokens is not positive
         */
        public @NonNull ContextManagementConfig build() {
            return new ContextManagementConfig(this);
        }
    }
}
