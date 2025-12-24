package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a developer-level message with the highest instruction priority.
 *
 * <p>Developer messages are used to provide system-level instructions, constraints, or context that
 * should take precedence over user instructions. These messages typically configure the model's
 * behavior, define boundaries, or establish rules that the model should follow throughout the
 * conversation.
 *
 * <p>This is a final implementation of {@link Message} and cannot be subclassed. Developer messages
 * are created with a {@code null} status initially, which is populated by the API after processing.
 *
 * @see Message
 * @see UserMessage
 * @see AssistantMessage
 */
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(
    using = com.paragon.responses.json.MessageDeserializer.class)
public final class DeveloperMessage extends Message {

  /**
   * Constructs a DeveloperMessage with the specified content and status.
   *
   * <p>This constructor is used by Jackson for deserialization and by the factory method.
   *
   * @param content the message content for developer instructions; must not be null
   * @param status the processing status of the message; typically null for new messages
   */
  @JsonCreator
  public DeveloperMessage(
      @JsonProperty("content") @NonNull List<MessageContent> content,
      @JsonProperty("status") InputMessageStatus status) {
    super(content, status);
  }

  /**
   * Creates a new DeveloperMessage with the specified content.
   *
   * <p>The message is created with a {@code null} status, which will be populated by the API after
   * the message is processed.
   *
   * @param content the content for the developer message; must not be null
   * @param status
   * @return a new DeveloperMessage instance
   */
  public static DeveloperMessage of(
      @NonNull List<MessageContent> content, @Nullable InputMessageStatus status) {
    return new DeveloperMessage(content, status);
  }

  /**
   * Returns the role identifier for this message type.
   *
   * <p>Developer messages have the highest priority in the instruction hierarchy, overriding both
   * user and assistant message instructions.
   *
   * @return the string {@code "developer"}
   */
  @Override
  public MessageRole role() {
    return MessageRole.DEVELOPER;
  }
}
