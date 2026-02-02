package com.paragon.messaging.whatsapp;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Interface selada para eventos de webhook.
 *
 * @author Your Name
 * @since 1.0
 */
public sealed interface WebhookEvent permits
        WebhookEvent.MessageStatusEvent,
        WebhookEvent.IncomingMessageEvent {

  /**
   * Retorna o tipo do evento.
   */
  WebhookEventType type();

  /**
   * Retorna o timestamp do evento.
   */
  Instant timestamp();

  /**
   * Enum de tipos de eventos webhook.
   */
  enum WebhookEventType {
    MESSAGE_STATUS,
    INCOMING_MESSAGE,
    INCOMING_REACTION,
    INCOMING_BUTTON_REPLY,
    INCOMING_LIST_REPLY
  }

  /**
   * Tipos de mensagens recebidas.
   */
  enum IncomingMessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    STICKER,
    LOCATION,
    CONTACT,
    REACTION,
    BUTTON_REPLY,
    LIST_REPLY
  }

  /**
   * Interface selada para conteúdo de mensagem recebida.
   */
  sealed interface IncomingMessageContent permits
          IncomingMessageContent.TextContent,
          IncomingMessageContent.MediaContent,
          IncomingMessageContent.LocationContent,
          IncomingMessageContent.ContactContent,
          IncomingMessageContent.ReactionContent,
          IncomingMessageContent.ButtonReplyContent,
          IncomingMessageContent.ListReplyContent {
  }

  /**
   * Evento de atualização de status de mensagem.
   *
   * @param messageId      ID da mensagem
   * @param recipientId    ID do destinatário
   * @param status         status da mensagem
   * @param timestamp      timestamp do evento
   * @param conversationId ID da conversa (opcional)
   * @param pricing        informações de cobrança (opcional)
   * @param errors         erros ocorridos (opcional)
   */
  record MessageStatusEvent(
          String messageId,
          String recipientId,
          MessageResponse.MessageStatus status,
          Instant timestamp,
          Optional<String> conversationId,
          Optional<MessageResponse.PricingInfo> pricing,
          Optional<List<MessageResponse.ErrorInfo>> errors
  ) implements WebhookEvent {

    public MessageStatusEvent {
      Objects.requireNonNull(messageId, "Message ID cannot be null");
      Objects.requireNonNull(recipientId, "Recipient ID cannot be null");
      Objects.requireNonNull(status, "Status cannot be null");
      Objects.requireNonNull(timestamp, "Timestamp cannot be null");
      Objects.requireNonNull(conversationId, "Conversation ID cannot be null");
      Objects.requireNonNull(pricing, "Pricing cannot be null");
      Objects.requireNonNull(errors, "Errors cannot be null");

      errors.ifPresent(List::copyOf);
    }

    @Override
    public WebhookEventType type() {
      return WebhookEventType.MESSAGE_STATUS;
    }

    /**
     * Verifica se a mensagem foi entregue com sucesso.
     */
    public boolean isDelivered() {
      return status == MessageResponse.MessageStatus.DELIVERED;
    }

    /**
     * Verifica se a mensagem foi lida.
     */
    public boolean isRead() {
      return status == MessageResponse.MessageStatus.READ;
    }

    /**
     * Verifica se houve falha.
     */
    public boolean isFailed() {
      return status == MessageResponse.MessageStatus.FAILED;
    }
  }

  /**
   * Evento de mensagem recebida.
   *
   * @param messageId   ID da mensagem recebida
   * @param senderId    ID do remetente
   * @param senderName  nome do remetente (se disponível)
   * @param messageType tipo da mensagem recebida
   * @param content     conteúdo da mensagem
   * @param timestamp   timestamp da mensagem
   * @param context     contexto da mensagem (se for resposta, etc.)
   */
  record IncomingMessageEvent(
          String messageId,
          String senderId,
          Optional<String> senderName,
          IncomingMessageType messageType,
          IncomingMessageContent content,
          Instant timestamp,
          Optional<MessageContext> context
  ) implements WebhookEvent {

    public IncomingMessageEvent {
      Objects.requireNonNull(messageId, "Message ID cannot be null");
      Objects.requireNonNull(senderId, "Sender ID cannot be null");
      Objects.requireNonNull(senderName, "Sender name cannot be null");
      Objects.requireNonNull(messageType, "Message type cannot be null");
      Objects.requireNonNull(content, "Content cannot be null");
      Objects.requireNonNull(timestamp, "Timestamp cannot be null");
      Objects.requireNonNull(context, "Context cannot be null");
    }

    @Override
    public WebhookEventType type() {
      return switch (messageType) {
        case TEXT -> WebhookEventType.INCOMING_MESSAGE;
        case REACTION -> WebhookEventType.INCOMING_REACTION;
        case BUTTON_REPLY -> WebhookEventType.INCOMING_BUTTON_REPLY;
        case LIST_REPLY -> WebhookEventType.INCOMING_LIST_REPLY;
        default -> WebhookEventType.INCOMING_MESSAGE;
      };
    }
  }

  /**
   * Conteúdo de mensagem de texto.
   */
  record TextContent(String body) implements IncomingMessageContent {
    public TextContent {
      Objects.requireNonNull(body, "Text body cannot be null");
    }
  }

  /**
   * Conteúdo de mídia.
   */
  record MediaContent(
          String mediaId,
          String mimeType,
          Optional<String> caption,
          Optional<String> filename
  ) implements IncomingMessageContent {
    public MediaContent {
      Objects.requireNonNull(mediaId, "Media ID cannot be null");
      Objects.requireNonNull(mimeType, "MIME type cannot be null");
      Objects.requireNonNull(caption, "Caption cannot be null");
      Objects.requireNonNull(filename, "Filename cannot be null");
    }
  }

  /**
   * Conteúdo de localização.
   */
  record LocationContent(
          double latitude,
          double longitude,
          Optional<String> name,
          Optional<String> address
  ) implements IncomingMessageContent {
    public LocationContent {
      Objects.requireNonNull(name, "Name cannot be null");
      Objects.requireNonNull(address, "Address cannot be null");
    }
  }

  /**
   * Conteúdo de contato.
   */
  record ContactContent(List<String> contactsData) implements IncomingMessageContent {
    public ContactContent {
      Objects.requireNonNull(contactsData, "Contacts data cannot be null");
      contactsData = List.copyOf(contactsData);
    }
  }

  /**
   * Conteúdo de reação.
   */
  record ReactionContent(String messageId, String emoji) implements IncomingMessageContent {
    public ReactionContent {
      Objects.requireNonNull(messageId, "Message ID cannot be null");
      Objects.requireNonNull(emoji, "Emoji cannot be null");
    }
  }

  /**
   * Conteúdo de resposta a botão.
   */
  record ButtonReplyContent(String buttonId, String buttonText) implements IncomingMessageContent {
    public ButtonReplyContent {
      Objects.requireNonNull(buttonId, "Button ID cannot be null");
      Objects.requireNonNull(buttonText, "Button text cannot be null");
    }
  }

  /**
   * Conteúdo de resposta a lista.
   */
  record ListReplyContent(
          String listId,
          String listTitle,
          Optional<String> listDescription
  ) implements IncomingMessageContent {
    public ListReplyContent {
      Objects.requireNonNull(listId, "List ID cannot be null");
      Objects.requireNonNull(listTitle, "List title cannot be null");
      Objects.requireNonNull(listDescription, "List description cannot be null");
    }
  }

  /**
   * Contexto da mensagem (se for resposta, reenvio, etc.).
   */
  record MessageContext(
          Optional<String> referencedMessageId,
          Optional<String> forwardedFrom,
          boolean isForwarded
  ) {
    public MessageContext {
      Objects.requireNonNull(referencedMessageId, "Referenced message ID cannot be null");
      Objects.requireNonNull(forwardedFrom, "Forwarded from cannot be null");
    }
  }
}