package com.paragon.messaging.whatsapp.messages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Optional;

/**
 * Mensagem de reação (emoji) a uma mensagem existente.
 */
public record ReactionMessage(

        @NotBlank(message = "Message ID cannot be blank")
        String messageId,

        Optional<@Size(max = 10, message = "Emoji cannot exceed 10 characters") String> emoji

) implements OutboundMessage {

  public ReactionMessage(String messageId, String emoji) {
    this(messageId, Optional.ofNullable(emoji));
  }

  public static ReactionMessage remove(String messageId) {
    return new ReactionMessage(messageId, Optional.empty());
  }

  public boolean isRemoval() {
    return emoji.isEmpty();
  }

  @Override
  public OutboundMessageType type() {
    return OutboundMessageType.REACTION;
  }
}
