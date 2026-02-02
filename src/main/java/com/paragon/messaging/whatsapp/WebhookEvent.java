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
   * @param conversationId ID da conversa (pode ser null)
   * @param pricing        informações de cobrança (pode ser null)
   * @param errors         erros ocorridos (pode ser null)
   */
  record MessageStatusEvent(
          String messageId,
          String recipientId,
          MessageResponse.MessageStatus status,
          Instant timestamp,
          @Nullable String conversationId,
          @Nullable MessageResponse.PricingInfo pricing,
          @Nullable List<MessageResponse.ErrorInfo> errors
  ) implements WebhookEvent {

    public MessageStatusEvent {
      Objects.requireNonNull(messageId, "Message ID cannot be null");
      Objects.requireNonNull(recipientId, "Recipient ID cannot be null");
      Objects.requireNonNull(status, "Status cannot be null");
      Objects.requireNonNull(timestamp, "Timestamp cannot be null");
      // conversationId, pricing, errors can be null
    }

    @Override
    public WebhookEventType type() {
      return WebhookEventType.MESSAGE_STATUS;
    }

    public Optional<String> getConversationId() {
      return Optional.ofNullable(conversationId);
    }

    public Optional<MessageResponse.PricingInfo> getPricing() {
      return Optional.ofNullable(pricing);
    }

    public Optional<List<MessageResponse.ErrorInfo>> getErrors() {
      return Optional.ofNullable(errors).map(List::copyOf);
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
   * @param context     contexto da mensagem (se for resposta, etc., pode ser null)
   */
  record IncomingMessageEvent(
          String messageId,
          String senderId,
          @Nullable String senderName,
          IncomingMessageType messageType,
          IncomingMessageContent content,
          Instant timestamp,
          @Nullable MessageContext context
  ) implements WebhookEvent {

    public IncomingMessageEvent {
      Objects.requireNonNull(messageId, "Message ID cannot be null");
      Objects.requireNonNull(senderId, "Sender ID cannot be null");
      Objects.requireNonNull(messageType, "Message type cannot be null");
      Objects.requireNonNull(content, "Content cannot be null");
      Objects.requireNonNull(timestamp, "Timestamp cannot be null");
      // senderName, context can be null
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

    public Optional<String> getSenderName() {
      return Optional.ofNullable(senderName);
    }

    public Optional<MessageContext> getContext() {
      return Optional.ofNullable(context);
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
          @Nullable String caption,
          @Nullable String filename
  ) implements IncomingMessageContent {
    public MediaContent {
      Objects.requireNonNull(mediaId, "Media ID cannot be null");
      Objects.requireNonNull(mimeType, "MIME type cannot be null");
    }

    public Optional<String> getCaption() {
      return Optional.ofNullable(caption);
    }

    public Optional<String> getFilename() {
      return Optional.ofNullable(filename);
    }
  }

  /**
   * Conteúdo de localização.
   */
  record LocationContent(
          double latitude,
          double longitude,
          @Nullable String name,
          @Nullable String address
  ) implements IncomingMessageContent {

    public Optional<String> getName() {
      return Optional.ofNullable(name);
    }

    public Optional<String> getAddress() {
      return Optional.ofNullable(address);
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
          @Nullable String listDescription
  ) implements IncomingMessageContent {
    public ListReplyContent {
      Objects.requireNonNull(listId, "List ID cannot be null");
      Objects.requireNonNull(listTitle, "List title cannot be null");
    }

    public Optional<String> getListDescription() {
      return Optional.ofNullable(listDescription);
    }
  }

  /**
   * Contexto da mensagem (se for resposta, reenvio, etc.).
   */
  record MessageContext(
          @Nullable String referencedMessageId,
          @Nullable String forwardedFrom,
          boolean isForwarded
  ) {

    public Optional<String> getReferencedMessageId() {
      return Optional.ofNullable(referencedMessageId);
    }

    public Optional<String> getForwardedFrom() {
      return Optional.ofNullable(forwardedFrom);
    }
  }
}