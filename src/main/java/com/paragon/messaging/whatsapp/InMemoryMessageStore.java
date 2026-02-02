package com.paragon.messaging.whatsapp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação in-memory de MessageStore com LRU cache para deduplicação.
 *
 * <p>Não persiste após restart. Use para desenvolvimento ou quando
 * persistência não é crítica.</p>
 *
 * <p><b>Exemplo:</b></p>
 * <pre>{@code
 * MessageStore store = InMemoryMessageStore.create();
 * // ou com capacidade customizada
 * MessageStore store = InMemoryMessageStore.create(10000);
 * }</pre>
 *
 * @author Agentle Team
 * @since 1.0
 */
public class InMemoryMessageStore implements MessageStore {

  private final Map<String, List<Message>> buffers = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> processedIds;
  private final int maxProcessedIds;

  private InMemoryMessageStore(int maxProcessedIds) {
    this.maxProcessedIds = maxProcessedIds;
    this.processedIds = new ConcurrentHashMap<>();
  }

  /**
   * Cria store com capacidade padrão (5000 IDs processados por usuário).
   *
   * @return novo store
   */
  public static InMemoryMessageStore create() {
    return new InMemoryMessageStore(5000);
  }

  /**
   * Cria store com capacidade customizada.
   *
   * @param maxProcessedIds máximo de IDs processados a manter por usuário
   * @return novo store
   */
  public static InMemoryMessageStore create(int maxProcessedIds) {
    return new InMemoryMessageStore(maxProcessedIds);
  }

  @Override
  public void store(String userId, Message message) {
    buffers.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(message);
  }

  @Override
  public List<Message> retrieve(String userId) {
    return buffers.getOrDefault(userId, List.of()).stream().toList();
  }

  @Override
  public void remove(String userId) {
    buffers.remove(userId);
  }

  @Override
  public boolean hasProcessed(String userId, String messageId) {
    Set<String> userProcessed = processedIds.get(userId);
    return userProcessed != null && userProcessed.contains(messageId);
  }

  @Override
  public void markProcessed(String userId, String messageId) {
    processedIds.computeIfAbsent(userId, k -> createLRUSet(maxProcessedIds))
            .add(messageId);
  }

  @SuppressWarnings("serial")
  private Set<String> createLRUSet(int maxSize) {
    return Collections.newSetFromMap(
            new LinkedHashMap<String, Boolean>(maxSize + 1, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > maxSize;
              }
            }
    );
  }
}
