package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.OutboundMessage;
import com.paragon.messaging.core.TextMessageInterface;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a simple text message for outbound delivery.
 *
 * <p>Uses Bean Validation (Hibernate Validator) for declarative field validation,
 * ensuring the message meets API requirements before sending.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple text message
 * TextMessage message = new TextMessage("Hello, World!");
 *
 * // With URL preview enabled
 * TextMessage message = new TextMessage("Check out https://example.com", true);
 *
 * // As a reply to another message
 * TextMessage reply = TextMessage.builder()
 *     .body("Thanks for your message!")
 *     .replyTo("wamid.xyz123")
 *     .build();
 * }</pre>
 *
 * @param body              message content (1-4096 characters)
 * @param previewUrl        if true, generates URL previews in the text
 * @param replyToMessageId  optional message ID to reply to
 * @author Agentle Team
 * @since 2.0
 */
public record TextMessage(

        @NotBlank(message = "Message body cannot be blank")
        @Size(min = 1, max = 4096, message = "Message body must be between 1 and 4096 characters")
        String body,

        boolean previewUrl,

        @Nullable String replyToMessageId

) implements TextMessageInterface {

  /**
   * Maximum allowed length for message body.
   */
  public static final int MAX_BODY_LENGTH = 4096;

  /**
   * Convenience constructor without URL preview or reply context.
   *
   * @param body message content
   */
  public TextMessage(String body) {
    this(body, false, null);
  }

  /**
   * Convenience constructor with URL preview but no reply context.
   *
   * @param body       message content
   * @param previewUrl whether to generate URL previews
   */
  public TextMessage(String body, boolean previewUrl) {
    this(body, previewUrl, null);
  }

  /**
   * Creates a builder for TextMessage.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OutboundMessageType type() {
    return OutboundMessageType.TEXT;
  }

  @Override
  public @Nullable String replyToMessageId() {
    return replyToMessageId;
  }

  @Override
  public @NonNull OutboundMessage withReplyTo(@NonNull String messageId) {
    return new TextMessage(body, previewUrl, messageId);
  }

  /**
   * Builder for TextMessage with fluent API.
   */
  public static class Builder {
    private String body;
    private boolean previewUrl = false;
    private String replyToMessageId;

    private Builder() {
    }

    /**
     * Sets the message body text.
     *
     * @param body the message content
     * @return this builder
     */
    public Builder body(String body) {
      this.body = body;
      return this;
    }

    /**
     * Sets whether to generate URL previews.
     *
     * @param previewUrl true to enable URL previews
     * @return this builder
     */
    public Builder previewUrl(boolean previewUrl) {
      this.previewUrl = previewUrl;
      return this;
    }

    /**
     * Enables URL preview generation.
     *
     * @return this builder
     */
    public Builder enablePreviewUrl() {
      this.previewUrl = true;
      return this;
    }

    /**
     * Disables URL preview generation.
     *
     * @return this builder
     */
    public Builder disablePreviewUrl() {
      this.previewUrl = false;
      return this;
    }

    /**
     * Sets the message ID to reply to.
     *
     * <p>When set, the message will appear as a reply/quote in WhatsApp.</p>
     *
     * @param messageId the WhatsApp message ID to reply to
     * @return this builder
     */
    public Builder replyTo(@Nullable String messageId) {
      this.replyToMessageId = messageId;
      return this;
    }

    /**
     * Builds the TextMessage.
     *
     * <p>Note: Validation will be executed when the object is passed
     * to MessagingProvider via the @Valid annotation.</p>
     *
     * @return the built TextMessage
     */
    public TextMessage build() {
      return new TextMessage(body, previewUrl, replyToMessageId);
    }
  }
}

