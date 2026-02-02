package com.paragon.messaging.whatsapp;

import com.paragon.messaging.whatsapp.payload.ContactMessage;

/**
 * Interface selada que representa todos os tipos possíveis de mensagens.
 *
 * <p>Usando sealed interface para garantir que apenas tipos conhecidos
 * de mensagens sejam criados, facilitando pattern matching e validação.</p>
 *
 * @author Your Name
 * @since 2.0
 */
public sealed interface Message permits
        TextMessage,
        MediaMessage,
        TemplateMessage,
        InteractiveMessage,
        LocationMessage,
        ContactMessage,
        ReactionMessage {

  /**
   * Retorna o tipo da mensagem.
   *
   * @return tipo da mensagem
   */
  MessageType getType();

  /**
   * Enum representando todos os tipos de mensagens suportados.
   */
  enum MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    STICKER,
    TEMPLATE,
    INTERACTIVE_BUTTON,
    INTERACTIVE_LIST,
    INTERACTIVE_CTA_URL,
    LOCATION,
    CONTACT,
    REACTION
  }
}
