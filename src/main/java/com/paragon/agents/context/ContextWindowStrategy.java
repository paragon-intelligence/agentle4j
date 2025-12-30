package com.paragon.agents.context;

import com.paragon.responses.spec.ResponseInputItem;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Strategy interface for managing conversation context length.
 *
 * <p>Context window strategies are used by {@link com.paragon.agents.Agent Agent} to ensure
 * conversation history stays within the model's token limits. When the context exceeds the maximum
 * token count, the strategy determines how to reduce it.
 *
 * <p>Common strategies include:
 *
 * <ul>
 *   <li>{@link SlidingWindowStrategy}: Removes oldest messages to fit within limit
 *   <li>{@link SummarizationStrategy}: Summarizes older messages using an LLM
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create agent with sliding window strategy
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .model("openai/gpt-4o")
 *     .instructions("You are a helpful assistant.")
 *     .responder(responder)
 *     .contextWindow(new SlidingWindowStrategy(), 4000)
 *     .build();
 *
 * // Or use summarization
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .model("openai/gpt-4o")
 *     .instructions("You are a helpful assistant.")
 *     .responder(responder)
 *     .contextWindow(SummarizationStrategy.withResponder(responder, "openai/gpt-4o-mini"), 4000)
 *     .build();
 * }</pre>
 *
 * @see SlidingWindowStrategy
 * @see SummarizationStrategy
 * @see TokenCounter
 * @since 1.0
 */
public interface ContextWindowStrategy {

  /**
   * Manages the conversation history to fit within the token limit.
   *
   * <p>This method is called before each LLM request when context management is enabled.
   * Implementations should return a modified list that fits within {@code maxTokens}.
   *
   * @param history the current conversation history (may be modified or replaced)
   * @param maxTokens the maximum number of tokens allowed
   * @param counter the token counter to use for measuring content
   * @return the managed history that fits within the token limit
   */
  @NonNull List<ResponseInputItem> manage(
      @NonNull List<ResponseInputItem> history, int maxTokens, @NonNull TokenCounter counter);
}
