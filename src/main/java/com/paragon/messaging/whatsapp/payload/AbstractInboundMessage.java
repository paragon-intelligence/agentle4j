package com.paragon.messaging.whatsapp.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for inbound WhatsApp webhook messages.
 *
 * <p>Provides common fields and validation for all inbound message types:
 *
 * <ul>
 *   <li>{@code from} - Sender's WhatsApp ID
 *   <li>{@code id} - Unique message identifier
 *   <li>{@code timestamp} - Message timestamp (Unix epoch)
 *   <li>{@code type} - Message type discriminator
 *   <li>{@code context} - Reply context (optional)
 * </ul>
 *
 * @author Agentle Team
 * @since 2.1
 */
public abstract sealed class AbstractInboundMessage implements InboundMessage
    permits TextMessage,
        ImageMessage,
        VideoMessage,
        AudioMessage,
        DocumentMessage,
        StickerMessage,
        InteractiveMessage,
        LocationMessage,
        ReactionMessage,
        SystemMessage,
        OrderMessage {

  @NotBlank(message = "Sender phone number cannot be blank")
  @Pattern(regexp = "[0-9]{10,15}", message = "Phone number must contain 10-15 digits")
  public final String from;

  @NotBlank(message = "Message ID cannot be blank")
  public final String id;

  @NotBlank(message = "Timestamp cannot be blank")
  public final String timestamp;

  @NotBlank(message = "Message type cannot be blank")
  public final String type;

  @Nullable public final MessageContext context;

  protected AbstractInboundMessage(
      String from, String id, String timestamp, String type, MessageContext context) {
    this.from = from;
    this.id = id;
    this.timestamp = timestamp;
    this.type = type;
    this.context = context;
  }

  @Override
  public @NonNull String from() {
    return from;
  }

  @Override
  public @NonNull String id() {
    return id;
  }

  @Override
  public @NonNull String timestamp() {
    return timestamp;
  }

  @Override
  public @NonNull String type() {
    return type;
  }

  @Override
  public @Nullable MessageContext context() {
    return context;
  }
}
