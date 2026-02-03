package com.paragon.messaging.store;

import java.util.List;

/**
 * Interface para persistência e deduplicação de mensagens.
 *
 * <p>Implementações podem usar:</p>
 * <ul>
 *   <li>In-memory (LRU cache)</li>
 *   <li>Redis</li>
 *   <li>Database SQL</li>
 *   <li>NoSQL</li>
 * </ul>
 *
 * @author Agentle Team
 * @since 1.0
 */
public interface MessageStore {

  /**
   * Store no-op.
   *
   * @return store vazio
   */
  static MessageStore noOp() {
    return new MessageStore() {
      @Override
      public void store(String userId, Message message) {
      }

      @Override
      public List<Message> retrieve(String userId) {
        return List.of();
      }

      @Override
      public void remove(String userId) {
      }

      @Override
      public boolean hasProcessed(String userId, String messageId) {
        return false;
      }

      @Override
      public void markProcessed(String userId, String messageId) {
      }
    };
  }

  /**
   * Armazena mensagem no buffer do usuário.
   *
   * @param userId  ID do usuário
   * @param message mensagem
   */
  void store(String userId, Message message);

  /**
   * Recupera todas mensagens do usuário.
   *
   * @param userId ID do usuário
   * @return lista de mensagens ordenadas
   */
  List<Message> retrieve(String userId);

  /**
   * Remove todas mensagens do usuário.
   *
   * @param userId ID do usuário
   */
  void remove(String userId);

  /**
   * Verifica se mensagem já foi processada.
   *
   * @param userId    ID do usuário
   * @param messageId ID da mensagem
   * @return true se já processada
   */
  boolean hasProcessed(String userId, String messageId);

  /**
   * Marca mensagem como processada.
   *
   * @param userId    ID do usuário
   * @param messageId ID da mensagem
   */
  void markProcessed(String userId, String messageId);
}