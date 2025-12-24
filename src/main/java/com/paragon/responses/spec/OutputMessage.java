package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an assistant output message with optional structured output support.
 *
 * <p>OutputMessage extends {@link AssistantMessage} to represent API-returned assistant responses
 * with additional metadata. While similar to AssistantMessage in content, OutputMessage includes a
 * unique identifier and optional structured output that can be extracted when the model is
 * configured to generate responses following a specific schema.
 *
 * <p>The parsed field contains structured output when the API is configured to generate responses
 * in a specific format (JSON schema, function calls, etc.). When structured output is not used, the
 * parsed field will be {@code null}, and the generic type parameter is typically {@code Void}.
 *
 * @param <T> the type of the structured output, or {@code Void} when structured output is not used
 * @see AssistantMessage
 * @see MessageContent
 * @see InputMessageStatus
 */
public final class OutputMessage<T> extends AssistantMessage implements Item, ResponseOutput {
  private final String id;
  private final T parsed;

  /**
   * Constructs an OutputMessage with all fields.
   *
   * <p>This constructor is used by Jackson for deserialization.
   *
   * @param content the raw message content; must not be null
   * @param id the unique identifier for this message; must not be null
   * @param status the processing status of the message; must not be null
   * @param parsed the structured output extracted from the response, or {@code null} when
   *     structured output is not configured
   */
  @JsonCreator
  public OutputMessage(
      @JsonProperty("content") @NonNull List<MessageContent> content,
      @JsonProperty("id") @NonNull String id,
      @JsonProperty("status") @NonNull InputMessageStatus status,
      @JsonProperty("parsed") @Nullable T parsed) {
    super(content, status);
    this.id = id;
    this.parsed = parsed;
  }

  /**
   * Returns the unique identifier for this message.
   *
   * <p>The ID is assigned by the API and can be used to reference this specific message in
   * subsequent operations or for tracking purposes.
   *
   * @return the message identifier
   */
  @JsonProperty("id")
  public String id() {
    return id;
  }

  /**
   * Returns the structured output extracted from this message.
   *
   * <p>When the API is configured to generate responses following a specific schema (such as JSON
   * schema or function definitions), this field contains the parsed structured output. If
   * structured output is not configured or not applicable, this method returns {@code null}.
   *
   * <p>Common use cases include:
   *
   * <ul>
   *   <li>Extracting structured data from model responses
   *   <li>Getting function call results in a typed format
   *   <li>Parsing JSON schema-validated output
   * </ul>
   *
   * @return the structured output, or {@code null} if not available
   */
  @JsonProperty("parsed")
  public @Nullable T parsed() {
    return parsed;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    List<MessageContent> content = content();
    if (content == null || content.isEmpty()) {
      return "";
    }

    for (MessageContent messageContent : content) {
      sb.append(messageContent.toString());
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof OutputMessage)) return false;
    if (!super.equals(obj)) return false;
    OutputMessage<?> that = (OutputMessage<?>) obj;
    return java.util.Objects.equals(id, that.id)
        && java.util.Objects.equals(status(), that.status())
        && java.util.Objects.equals(parsed, that.parsed);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(super.hashCode(), id, status(), parsed);
  }
}
