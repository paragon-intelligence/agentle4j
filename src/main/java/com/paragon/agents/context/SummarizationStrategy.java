package com.paragon.agents.context;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A context window strategy that summarizes older messages when context exceeds the limit.
 *
 * <p>This strategy:
 *
 * <ul>
 *   <li>Keeps recent messages intact
 *   <li>Summarizes older messages using an LLM
 *   <li>Replaces summarized messages with a single summary message
 *   <li>Caches summaries to avoid redundant API calls
 * </ul>
 *
 * <p>This strategy is more expensive (requires an LLM call) but preserves more context than simple
 * truncation, making it suitable for conversations where older context contains important
 * information.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Use agent's responder with a fast model for summarization
 * ContextWindowStrategy strategy = SummarizationStrategy.builder()
 *     .responder(responder)
 *     .model("openai/gpt-4o-mini")
 *     .build();
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
 * @see SlidingWindowStrategy
 * @since 1.0
 */
public final class SummarizationStrategy implements ContextWindowStrategy {

  private static final String DEFAULT_SUMMARIZATION_PROMPT =
      """
      Summarize the following conversation history concisely, preserving key information,
      decisions made, and any context that would be important for continuing the conversation.
      Focus on facts, user preferences, and any commitments made.

      Conversation to summarize:
      %s
      """;

  private final @NonNull Responder responder;
  private final @NonNull String model;
  private final @NonNull String summarizationPrompt;
  private final int keepRecentMessages;

  private SummarizationStrategy(Builder builder) {
    this.responder = Objects.requireNonNull(builder.responder, "responder is required");
    this.model = Objects.requireNonNull(builder.model, "model is required");
    this.summarizationPrompt =
        builder.summarizationPrompt != null
            ? builder.summarizationPrompt
            : DEFAULT_SUMMARIZATION_PROMPT;
    this.keepRecentMessages = builder.keepRecentMessages > 0 ? builder.keepRecentMessages : 5;
  }

  /**
   * Creates a new builder for SummarizationStrategy.
   *
   * @return a new builder instance
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a summarization strategy with the given responder and model.
   *
   * @param responder the responder to use for summarization calls
   * @param model the model to use for summarization
   * @return a new summarization strategy
   */
  public static @NonNull SummarizationStrategy withResponder(
      @NonNull Responder responder, @NonNull String model) {
    return builder().responder(responder).model(model).build();
  }

  @Override
  public @NonNull List<ResponseInputItem> manage(
      @NonNull List<ResponseInputItem> history, int maxTokens, @NonNull TokenCounter counter) {
    Objects.requireNonNull(history, "history cannot be null");
    Objects.requireNonNull(counter, "counter cannot be null");

    if (maxTokens <= 0 || history.isEmpty()) {
      return history;
    }

    // Fast path: check if already within limits
    int totalTokens = counter.countTokens(history);
    if (totalTokens <= maxTokens) {
      return history;
    }

    // Determine how many recent messages to keep
    int recentCount = Math.min(keepRecentMessages, history.size());
    int recentStartIdx = history.size() - recentCount;

    // Calculate tokens used by recent messages
    List<ResponseInputItem> recentMessages = history.subList(recentStartIdx, history.size());
    int recentTokens = counter.countTokens(recentMessages);

    // If recent messages already exceed limit, fall back to sliding window
    if (recentTokens >= maxTokens) {
      return new SlidingWindowStrategy().manage(history, maxTokens, counter);
    }

    // Get messages to summarize
    List<ResponseInputItem> toSummarize = history.subList(0, recentStartIdx);
    if (toSummarize.isEmpty()) {
      return recentMessages;
    }

    // Generate summary
    String summary = generateSummary(toSummarize);

    // Check if summary fits
    int summaryTokens = counter.countText(summary);
    if (recentTokens + summaryTokens > maxTokens) {
      // Summary is too long, fall back to sliding window
      return new SlidingWindowStrategy().manage(history, maxTokens, counter);
    }

    // Build result with summary + recent messages
    List<ResponseInputItem> result = new ArrayList<>();
    result.add(Message.developer("[Previous conversation summary] " + summary));
    result.addAll(recentMessages);

    return result;
  }

  /** Generates a summary of the given messages using the configured responder. */
  private String generateSummary(List<ResponseInputItem> messages) {
    String conversationText =
        messages.stream().map(this::formatMessage).collect(Collectors.joining("\n"));

    String prompt = String.format(summarizationPrompt, conversationText);

    try {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model(model).addUserMessage(prompt).build();

      Response response = responder.respond(payload).join();
      return response.outputText();
    } catch (Exception e) {
      // If summarization fails, return a basic fallback
      return "[Summarization failed - context truncated]";
    }
  }

  /** Formats a message for summarization. */
  private String formatMessage(ResponseInputItem item) {
    if (item instanceof Message message) {
      String role = message.role().toString();
      String content =
          message.content().stream()
              .map(
                  c -> {
                    if (c instanceof Text text) {
                      return text.text();
                    } else if (c instanceof Image) {
                      return "[Image]";
                    }
                    return c.toString();
                  })
              .collect(Collectors.joining(" "));
      return role + ": " + content;
    } else if (item instanceof FunctionToolCallOutput output) {
      return "Tool Result: " + output.output();
    }
    return item.toString();
  }

  /**
   * Returns the model used for summarization.
   *
   * @return the model identifier
   */
  public @NonNull String model() {
    return model;
  }

  /**
   * Returns the number of recent messages to keep.
   *
   * @return the keep recent messages count
   */
  public int keepRecentMessages() {
    return keepRecentMessages;
  }

  /** Builder for SummarizationStrategy. */
  public static final class Builder {
    private @Nullable Responder responder;
    private @Nullable String model;
    private @Nullable String summarizationPrompt;
    private int keepRecentMessages = 5;

    private Builder() {}

    /**
     * Sets the responder to use for summarization calls.
     *
     * @param responder the responder
     * @return this builder
     */
    public @NonNull Builder responder(@NonNull Responder responder) {
      this.responder = responder;
      return this;
    }

    /**
     * Sets the model to use for summarization.
     *
     * <p>A fast, cheaper model is recommended (e.g., "openai/gpt-4o-mini").
     *
     * @param model the model identifier
     * @return this builder
     */
    public @NonNull Builder model(@NonNull String model) {
      this.model = model;
      return this;
    }

    /**
     * Sets a custom prompt for summarization.
     *
     * <p>The prompt should contain a single {@code %s} placeholder where the conversation text will
     * be inserted.
     *
     * @param prompt the summarization prompt
     * @return this builder
     */
    public @NonNull Builder summarizationPrompt(@NonNull String prompt) {
      this.summarizationPrompt = prompt;
      return this;
    }

    /**
     * Sets the number of recent messages to keep without summarization.
     *
     * <p>Defaults to 5 messages.
     *
     * @param count the number of messages to keep
     * @return this builder
     */
    public @NonNull Builder keepRecentMessages(int count) {
      this.keepRecentMessages = count;
      return this;
    }

    /**
     * Builds the SummarizationStrategy.
     *
     * @return a new SummarizationStrategy
     * @throws NullPointerException if responder or model is null
     */
    public @NonNull SummarizationStrategy build() {
      return new SummarizationStrategy(this);
    }
  }
}
