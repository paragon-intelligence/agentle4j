package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a user message containing instructions or queries from end users.
 *
 * <p>User messages provide input from the application's end users, such as questions, requests, or
 * conversational input. These messages have lower priority than developer or system messages in the
 * instruction hierarchy, meaning developer-level instructions will override conflicting user
 * instructions.
 *
 * <p>This is a final implementation of {@link Message} and cannot be subclassed. User messages are
 * created without an explicit status, which is determined and populated by the API during
 * processing.
 *
 * @see Message
 * @see DeveloperMessage
 * @see AssistantMessage
 */
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(
    using = com.paragon.responses.json.MessageDeserializer.class)
public final class UserMessage extends Message {

  /**
   * Constructs a UserMessage with the specified content.
   *
   * <p>The status is implicitly set to {@code null} by the parent constructor, and will be
   * populated by the API after processing.
   *
   * @param content the message content from the user; must not be null
   */
  public UserMessage(@NonNull List<MessageContent> content) {
    super(content, InputMessageStatus.COMPLETED);
  }

  /**
   * Constructs a UserMessage with the specified content and status (for deserialization).
   *
   * @param content the message content from the user; must not be null
   * @param status the processing status; may be null
   */
  @JsonCreator
  public UserMessage(
      @JsonProperty("content") @NonNull List<MessageContent> content,
      @JsonProperty("status") @Nullable InputMessageStatus status) {
    super(content, status);
  }

  public static UserMessage of(
      @NonNull List<MessageContent> content, @Nullable InputMessageStatus status) {
    return new UserMessage(content, status);
  }

  /**
   * Returns the role identifier for this message type.
   *
   * <p>User messages have lower priority than developer messages but represent the primary
   * conversational input from application users.
   *
   * @return the string {@code "user"}
   */
  @Override
  public MessageRole role() {
    return MessageRole.USER;
  }
}
