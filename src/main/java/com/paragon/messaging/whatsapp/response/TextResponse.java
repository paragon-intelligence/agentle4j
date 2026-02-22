package com.paragon.messaging.whatsapp.response;

import com.paragon.messaging.core.OutboundMessage;
import com.paragon.messaging.whatsapp.messages.TextMessage;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Simple text response for structured AI output.
 *
 * <p>Represents a plain text message with optional reaction and reply context.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Simple text response
 * TextResponse response = new TextResponse("Hello! How can I help?");
 *
 * // With reply context
 * TextResponse response = TextResponse.builder()
 *     .text("Thanks for your message!")
 *     .replyTo("wamid.xyz123")
 *     .build();
 *
 * // With reaction
 * TextResponse response = TextResponse.builder()
 *     .text("Great choice!")
 *     .reactTo("wamid.xyz123", "👍")
 *     .build();
 * }</pre>
 *
 * @param text the message text content
 * @param context optional response context
 * @author Agentle Team
 * @since 2.1
 */
public record TextResponse(@NonNull String text, @Nullable ResponseContext context)
    implements WhatsAppResponse {

  public TextResponse {
    Objects.requireNonNull(text, "text cannot be null");
  }

  /**
   * Creates a simple text response with no context.
   *
   * @param text the message text
   */
  public TextResponse(@NonNull String text) {
    this(text, null);
  }

  /**
   * Creates a builder for TextResponse.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public @NonNull List<OutboundMessage> toMessages() {
    TextMessage.Builder builder = TextMessage.builder().body(text);

    if (context != null && context.previewUrl()) {
      builder.enablePreviewUrl();
    }

    if (context != null && context.replyToMessageId() != null) {
      builder.replyTo(context.replyToMessageId());
    }

    return List.of(builder.build());
  }

  @Override
  public @NonNull String getTextContent() {
    return text;
  }

  @Override
  public @Nullable String getReactionEmoji() {
    return context != null ? context.reactionEmoji() : null;
  }

  @Override
  public @Nullable String getReactToMessageId() {
    return context != null ? context.reactToMessageId() : null;
  }

  @Override
  public @Nullable String getReplyToMessageId() {
    return context != null ? context.replyToMessageId() : null;
  }

  @Override
  public @Nullable ResponseContext getContext() {
    return context;
  }

  /** Builder for TextResponse. */
  public static final class Builder {
    private String text;
    private ResponseContext.Builder contextBuilder;

    private Builder() {}

    /**
     * Sets the message text.
     *
     * @param text the text content
     * @return this builder
     */
    public Builder text(@NonNull String text) {
      this.text = text;
      return this;
    }

    /**
     * Sets the message ID to reply to.
     *
     * @param messageId the message ID
     * @return this builder
     */
    public Builder replyTo(@NonNull String messageId) {
      ensureContextBuilder().replyTo(messageId);
      return this;
    }

    /**
     * Sets the reaction for this response.
     *
     * @param messageId the message ID to react to
     * @param emoji the reaction emoji
     * @return this builder
     */
    public Builder reactTo(@NonNull String messageId, @NonNull String emoji) {
      ensureContextBuilder().reactTo(messageId, emoji);
      return this;
    }

    /**
     * Enables or disables URL preview.
     *
     * @param preview true to enable URL preview
     * @return this builder
     */
    public Builder previewUrl(boolean preview) {
      ensureContextBuilder().previewUrl(preview);
      return this;
    }

    /**
     * Sets the full response context.
     *
     * @param context the context
     * @return this builder
     */
    public Builder context(@NonNull ResponseContext context) {
      this.contextBuilder = null; // Clear any partial builder
      return this;
    }

    private ResponseContext.Builder ensureContextBuilder() {
      if (contextBuilder == null) {
        contextBuilder = ResponseContext.builder();
      }
      return contextBuilder;
    }

    /**
     * Builds the TextResponse.
     *
     * @return the built response
     */
    public TextResponse build() {
      ResponseContext context = contextBuilder != null ? contextBuilder.build() : null;
      return new TextResponse(text, context);
    }
  }
}
