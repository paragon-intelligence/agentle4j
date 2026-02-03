package com.paragon.messaging.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Optional;

/**
 * Resposta do envio de uma mensagem.
 */
public record MessageResponse(

        @NotBlank(message = "Message ID cannot be blank")
        String messageId,

        @NotNull(message = "Status cannot be null")
        MessageStatus status,

        @NotNull(message = "Timestamp cannot be null")
        Instant timestamp,

        Optional<String> conversationId

) {

  public MessageResponse(String messageId, MessageStatus status, Instant timestamp) {
    this(messageId, status, timestamp, Optional.empty());
  }

  /**
   * Status de envio de mensagem.
   */
  public enum MessageStatus {
    ACCEPTED,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    UNKNOWN
  }
}