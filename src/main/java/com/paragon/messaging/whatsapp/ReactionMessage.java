package com.paragon.messaging.whatsapp;

import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

/**
 * Mensagem de reação (emoji) a uma mensagem existente.
 */
public record ReactionMessage(

        @NotBlank(message = "Message ID cannot be blank")
        String messageId,

        Optional<String> emoji

) implements Message {

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
  public MessageType getType() {
    return MessageType.REACTION;
  }
}
