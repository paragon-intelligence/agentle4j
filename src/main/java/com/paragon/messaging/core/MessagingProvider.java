package com.paragon.messaging.core;

import com.paragon.messaging.whatsapp.payload.ContactMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * Interface for messaging providers (WhatsApp, Facebook Messenger, etc.).
 *
 * <p>This interface defines the contract for sending messages through different
 * messaging platforms. With Java virtual threads, the API is synchronous and simple,
 * but highly scalable when executed in virtual threads.</p>
 *
 * <h2>Usage with Virtual Threads</h2>
 * <pre>{@code
 * // Form 1: Virtual thread per task executor
 * try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
 *     executor.submit(() -> provider.sendMessage(recipient, message));
 * }
 *
 * // Form 2: Direct virtual thread
 * Thread.startVirtualThread(() -> {
 *     try {
 *         MessageResponse response = provider.sendMessage(recipient, message);
 *         System.out.println("Sent: " + response.messageId());
 *     } catch (MessagingException e) {
 *         e.printStackTrace();
 *     }
 * });
 *
 * // Form 3: Structured Concurrency (Java 21+)
 * try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
 *     var task1 = scope.fork(() -> provider.sendMessage(recipient1, message));
 *     var task2 = scope.fork(() -> provider.sendMessage(recipient2, message));
 *
 *     scope.join();
 *     scope.throwIfFailed();
 *
 *     MessageResponse r1 = task1.get();
 *     MessageResponse r2 = task2.get();
 * }
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.0
 */
public interface MessagingProvider {

  /**
   * Returns the provider type/name (e.g., "WHATSAPP_CLOUD", "FACEBOOK_MESSENGER").
   *
   * @return unique provider identifier
   */
  String getProviderType();

  /**
   * Checks if the provider is configured and ready to send messages.
   *
   * @return true if the provider is ready, false otherwise
   */
  boolean isConfigured();

  /**
   * Sends a message through the provider.
   *
   * <p>This method is blocking but can be executed efficiently
   * in virtual threads without consuming platform threads.</p>
   *
   * @param recipient message recipient (cannot be null)
   * @param message   message content (cannot be null, will be validated)
   * @return send response containing message ID and status
   * @throws MessagingException if there is a send error
   */
  MessageResponse sendMessage(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid OutboundMessage message
  ) throws MessagingException;

  /**
   * Sends a simple text message.
   *
   * @param recipient   message recipient
   * @param textMessage text message
   * @return send response
   * @throws MessagingException if there is a send error
   */
  default MessageResponse sendText(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid TextMessage textMessage
  ) throws MessagingException {
    return sendMessage(recipient, textMessage);
  }

  /**
   * Sends a media message (image, video, audio, document).
   *
   * @param recipient    message recipient
   * @param mediaMessage media message
   * @return send response
   * @throws MessagingException if there is a send error
   */
  default MessageResponse sendMedia(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid MediaMessage mediaMessage
  ) throws MessagingException {
    return sendMessage(recipient, mediaMessage);
  }

  /**
   * Sends a template message (for messages outside the 24h window).
   *
   * @param recipient       message recipient
   * @param templateMessage template-based message
   * @return send response
   * @throws MessagingException if there is a send error
   */
  default MessageResponse sendTemplate(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid TemplateMessage templateMessage
  ) throws MessagingException {
    return sendMessage(recipient, templateMessage);
  }

  /**
   * Sends an interactive message (buttons, lists, etc.).
   *
   * @param recipient          message recipient
   * @param interactiveMessage interactive message
   * @return send response
   * @throws MessagingException if there is a send error
   */
  default MessageResponse sendInteractive(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid InteractiveMessage interactiveMessage
  ) throws MessagingException {
    return sendMessage(recipient, interactiveMessage);
  }

  /**
   * Sends a location.
   *
   * @param recipient       message recipient
   * @param locationMessage location message
   * @return send response
   * @throws MessagingException if there is a send error
   */
  default MessageResponse sendLocation(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid LocationMessage locationMessage
  ) throws MessagingException {
    return sendMessage(recipient, locationMessage);
  }

  /**
   * Sends one or more contacts.
   *
   * @param recipient      message recipient
   * @param contactMessage contact message
   * @return send response
   * @throws MessagingException if there is a send error
   */
  default MessageResponse sendContact(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid ContactMessage contactMessage
  ) throws MessagingException {
    return sendMessage(recipient, contactMessage);
  }

  /**
   * Sends a reaction to an existing message.
   *
   * @param recipient       reaction recipient
   * @param reactionMessage reaction (emoji)
   * @return send response
   * @throws MessagingException if there is a send error
   */
  default MessageResponse sendReaction(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid ReactionMessage reactionMessage
  ) throws MessagingException {
    return sendMessage(recipient, reactionMessage);
  }

  /**
   * Sends multiple messages in parallel using virtual threads.
   *
   * <p>This method uses Structured Concurrency (Java 25 JEP 505) with the new
   * {@code Joiner.allSuccessfulOrThrow()} that fails if ANY message fails.</p>
   *
   * <p><b>Behavior:</b></p>
   * <ul>
   *   <li>All messages are sent in parallel (one virtual thread per message)</li>
   *   <li>If ONE message fails, ALL others are automatically cancelled</li>
   *   <li>Returns results in the same order as input messages</li>
   * </ul>
   *
   * @param recipient message recipient
   * @param messages  list of messages to send
   * @return list of responses in the same order as messages
   * @throws MessagingException if any message fails
   */
  default java.util.List<MessageResponse> sendBatch(
          @NotNull @Valid Recipient recipient,
          @NotNull java.util.List<@Valid ? extends OutboundMessage> messages
  ) throws MessagingException {

    try (var scope = java.util.concurrent.StructuredTaskScope.open(
            java.util.concurrent.StructuredTaskScope.Joiner.allSuccessfulOrThrow())) {

      // Fork a subtask for each message
      var subtasks = messages.stream()
              .map(msg -> scope.fork(() -> sendMessage(recipient, msg)))
              .toList();

      // Wait for all (throws exception if any fail)
      scope.join();

      // Collect results in original order
      return subtasks.stream()
              .map(java.util.concurrent.StructuredTaskScope.Subtask::get)
              .toList();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MessagingException("Batch send interrupted", e);
    } catch (Exception e) {
      throw new MessagingException("Batch send failed", e);
    }
  }

  /**
   * Sends to multiple recipients in parallel, returns only successes.
   *
   * <p>Unlike {@link #sendBatch}, this method does NOT fail if some messages
   * fail. Use when you want "best effort" (e.g., mass notifications).</p>
   *
   * <p><b>Behavior:</b></p>
   * <ul>
   *   <li>All messages are sent in parallel</li>
   *   <li>Individual failures do NOT cancel others</li>
   *   <li>Returns only successes</li>
   * </ul>
   *
   * @param recipients list of recipients
   * @param message    message to send to all
   * @return list of successful responses (may be empty)
   * @throws MessagingException only if the send process itself fails
   */
  default java.util.List<MessageResponse> sendBroadcast(
          @NotNull java.util.List<@Valid Recipient> recipients,
          @NotNull @Valid OutboundMessage message
  ) throws MessagingException {

    try (var scope = java.util.concurrent.StructuredTaskScope.open(
            java.util.concurrent.StructuredTaskScope.Joiner.<MessageResponse>awaitAll())) {

      // Fork a subtask for each recipient
      var subtasks = recipients.stream()
              .map(recipient -> scope.fork(() -> sendMessage(recipient, message)))
              .toList();

      // Wait for all (does NOT throw exception if some fail)
      scope.join();

      // Collect only successes
      return subtasks.stream()
              .filter(java.util.concurrent.StructuredTaskScope.Subtask::state)
              .map(java.util.concurrent.StructuredTaskScope.Subtask::get)
              .toList();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MessagingException("Broadcast interrupted", e);
    }
  }

  /**
   * Response from sending a message.
   *
   * @param messageId the unique message ID assigned by the platform
   * @param status    the delivery status
   * @param timestamp the timestamp when the message was accepted
   */
  record MessageResponse(
          @NonNull String messageId,
          @NonNull String status,
          @NonNull java.time.Instant timestamp
  ) {
    /**
     * Creates a successful response with current timestamp.
     *
     * @param messageId the message ID
     * @return successful response
     */
    public static MessageResponse success(String messageId) {
      return new MessageResponse(messageId, "accepted", java.time.Instant.now());
    }
  }

  /**
   * Represents a message recipient.
   *
   * @param type  recipient type (phone_number, wa_id, etc.)
   * @param value the recipient identifier value
   */
  record Recipient(
          @NonNull String type,
          @NonNull String value
  ) {
    /**
     * Creates a recipient from a phone number.
     *
     * @param phoneNumber the E.164 formatted phone number
     * @return phone number recipient
     */
    public static Recipient ofPhoneNumber(String phoneNumber) {
      return new Recipient("phone_number", phoneNumber);
    }

    /**
     * Creates a recipient from a WhatsApp ID.
     *
     * @param waId the WhatsApp ID
     * @return WhatsApp ID recipient
     */
    public static Recipient ofWhatsAppId(String waId) {
      return new Recipient("wa_id", waId);
    }
  }

  /**
   * Exception thrown when messaging operations fail.
   */
  class MessagingException extends RuntimeException {
    public MessagingException(String message) {
      super(message);
    }

    public MessagingException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
