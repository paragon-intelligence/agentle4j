package com.paragon.messaging.core;

import com.paragon.messaging.whatsapp.payload.InboundMessage;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Processes batched messages from a user.
 *
 * <p>Implementations typically:
 *
 * <ol>
 *   <li>Combine messages into a single input
 *   <li>Call AI agent (via {@link com.paragon.agents.Interactable})
 *   <li>Send response via messaging platform
 *   <li>Optionally convert to audio (TTS)
 * </ol>
 *
 * <h2>Simple Example</h2>
 *
 * <pre>{@code
 * MessageProcessor processor = (userId, messages, context) -> {
 *     // 1. Combine messages
 *     String input = messages.stream()
 *         .map(InboundMessage::extractTextContent)
 *         .collect(Collectors.joining("\n"));
 *
 *     // 2. Process with AI agent
 *     AgentResult result = agent.interact(input);
 *     String response = result.output();
 *
 *     // 3. Send response
 *     whatsappProvider.sendText(
 *         Recipient.ofPhoneNumber(userId),
 *         new TextMessage(response)
 *     );
 * };
 * }</pre>
 *
 * <h2>Example with TTS</h2>
 *
 * <pre>{@code
 * MessageProcessor processor = (userId, messages, context) -> {
 *     String input = combineMessages(messages);
 *     String response = agent.interact(input).output();
 *
 *     // Decide text vs audio
 *     if (random.nextDouble() < speechPlayChance) {
 *         byte[] audio = ttsProvider.synthesize(response, ttsConfig);
 *         sendAudio(userId, audio);
 *     } else {
 *         sendText(userId, response);
 *     }
 * };
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>Processors are called from virtual threads and may be invoked concurrently for different
 * users. Implementations must be thread-safe if accessing shared mutable state.
 *
 * <h2>Error Handling</h2>
 *
 * <p>Exceptions thrown are captured by {@link com.paragon.messaging.batching.MessageBatchingService} and handled according to
 * {@link com.paragon.messaging.error.ErrorHandlingStrategy}.
 *
 * @author Agentle Team
 * @see com.paragon.messaging.batching.MessageBatchingService
 * @see com.paragon.messaging.processor.AIAgentProcessor
 * @since 1.0
 */
@FunctionalInterface
public interface MessageProcessor {

  /**
   * No-op processor that does nothing.
   *
   * @return empty processor
   */
  static MessageProcessor noOp() {
    return (userId, messages, context) -> {
    };
  }

  /**
   * Logging processor that only logs messages.
   *
   * @param logger consumer for log messages
   * @return logging processor
   */
  static MessageProcessor logging(Consumer<String> logger) {
    return (userId, messages, context) -> {
      String log =
              String.format(
                      "Processing %d messages for user %s: %s",
                      messages.size(),
                      userId,
                      messages.stream()
                              .map(InboundMessage::extractTextContent)
                              .collect(Collectors.joining(", ")));
      logger.accept(log);
    };
  }

  /**
   * Processes a batch of messages from a user.
   *
   * @param userId   the user's unique identifier (e.g., WhatsApp phone number)
   * @param messages batch of messages (guaranteed non-empty and ordered by timestamp)
   * @param context  processing context with metadata about the batch
   * @throws Exception if processing fails (will be handled by error strategy)
   */
  void process(
          @NonNull String userId,
          @NonNull List<? extends InboundMessage> messages,
          @NonNull ProcessingContext context)
          throws Exception;

  /**
   * Simplified processing without context.
   *
   * <p>Delegates to {@link #process(String, List, ProcessingContext)} with an empty context.
   *
   * @param userId   the user's unique identifier
   * @param messages batch of messages
   * @throws Exception if processing fails
   */
  default void process(@NonNull String userId, @NonNull List<? extends InboundMessage> messages)
          throws Exception {
    process(userId, messages, ProcessingContext.empty());
  }

  /**
   * Reason why a batch was triggered for processing.
   */
  enum ProcessingReason {
    /**
     * Maximum wait time (adaptive timeout) was reached.
     */
    TIMEOUT,

    /**
     * User stopped sending for the silence threshold duration.
     */
    SILENCE,

    /**
     * Buffer reached maximum size.
     */
    BUFFER_FULL,

    /**
     * Unknown or unspecified reason.
     */
    UNKNOWN
  }

  /**
   * Context information passed to the processor during batch processing.
   *
   * @param batchId          unique identifier for this batch
   * @param firstMessageId   ID of the first message in the batch (for reply context)
   * @param lastMessageId    ID of the last message in the batch
   * @param processingReason why the batch was triggered (timeout, silence, buffer full)
   * @param retryAttempt     current retry attempt (0 for first try)
   */
  record ProcessingContext(
          @NonNull String batchId,
          @NonNull String firstMessageId,
          @NonNull String lastMessageId,
          @NonNull ProcessingReason processingReason,
          int retryAttempt) {
    /**
     * Creates an empty context for simple processing.
     *
     * @return empty context
     */
    public static ProcessingContext empty() {
      return new ProcessingContext("", "", "", ProcessingReason.UNKNOWN, 0);
    }

    /**
     * Creates a context for a new batch.
     *
     * @param batchId        unique batch identifier
     * @param firstMessageId first message ID
     * @param lastMessageId  last message ID
     * @param reason         processing trigger reason
     * @return new context
     */
    public static ProcessingContext create(
            String batchId, String firstMessageId, String lastMessageId, ProcessingReason reason) {
      return new ProcessingContext(batchId, firstMessageId, lastMessageId, reason, 0);
    }

    /**
     * Creates a retry context from this context.
     *
     * @return context with incremented retry attempt
     */
    public ProcessingContext retry() {
      return new ProcessingContext(
              batchId, firstMessageId, lastMessageId, processingReason, retryAttempt + 1);
    }

    /**
     * Checks if this is a retry attempt.
     *
     * @return true if retryAttempt > 0
     */
    public boolean isRetry() {
      return retryAttempt > 0;
    }
  }
}
