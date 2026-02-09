package com.paragon.messaging.core;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Optional;

/**
 * Resposta do envio de uma mensagem.
 */
public record MessageResponse(

        String messageId,

        @NotNull(message = "Status cannot be null")
        MessageStatus status,

        @NotNull(message = "Timestamp cannot be null")
        Instant timestamp,

        String error,

        Optional<String> conversationId

) {

  /**
   * Constructor without error or conversationId (successful response).
   */
  public MessageResponse(String messageId, MessageStatus status, Instant timestamp) {
    this(messageId, status, timestamp, null, Optional.empty());
  }

  /**
   * Constructor without conversationId.
   */
  public MessageResponse(String messageId, MessageStatus status, Instant timestamp, String error) {
    this(messageId, status, timestamp, error, Optional.empty());
  }

  /**
   * Constructor without error (successful response with conversationId).
   */
  public MessageResponse(String messageId, MessageStatus status, Instant timestamp, Optional<String> conversationId) {
    this(messageId, status, timestamp, null, conversationId);
  }

  /**
   * Returns true if this response represents a successful send.
   *
   * @return true if error is null
   */
  public boolean success() {
    return error == null;
  }

  /**
   * Creates an accepted response with current timestamp.
   *
   * @param messageId the message ID
   * @return accepted response
   */
  public static MessageResponse accepted(String messageId) {
    return new MessageResponse(messageId, MessageStatus.ACCEPTED, Instant.now());
  }

  /**
   * Creates a failed response with current timestamp.
   *
   * @param error the error description
   * @return failed response
   */
  public static MessageResponse failed(String error) {
    return new MessageResponse(null, MessageStatus.FAILED, Instant.now(), error);
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