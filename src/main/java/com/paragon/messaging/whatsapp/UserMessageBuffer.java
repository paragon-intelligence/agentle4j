package com.paragon.messaging.whatsapp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Buffer de mensagens para um único usuário (thread-safe).
 *
 * <p>Usa {@link ConcurrentLinkedQueue} para garantir thread safety sem locks.
 * Cada usuário tem seu próprio buffer isolado.</p>
 *
 * <p><b>Thread Safety:</b> Todas operações são thread-safe.</p>
 *
 * @author Agentle Team
 * @since 1.0
 */
public class UserMessageBuffer {

  private final String userId;
  private final ConcurrentLinkedQueue<Message> messages;
  private final AtomicReference<Instant> lastMessageTime;
  private final AtomicReference<ScheduledFuture<?>> scheduledTask;
  private final int maxSize;

  public UserMessageBuffer(String userId, int maxSize) {
    this.userId = userId;
    this.messages = new ConcurrentLinkedQueue<>();
    this.lastMessageTime = new AtomicReference<>(Instant.now());
    this.scheduledTask = new AtomicReference<>();
    this.maxSize = maxSize;
  }

  /**
   * Adiciona mensagem ao buffer.
   *
   * @param message mensagem a adicionar
   * @return true se adicionada, false se buffer cheio
   */
  public boolean add(Message message) {
    if (size() >= maxSize) {
      return false;
    }
    messages.offer(message);
    lastMessageTime.set(Instant.now());
    return true;
  }

  /**
   * Remove mensagem mais antiga (para backpressure DROP_OLDEST).
   *
   * @return mensagem removida ou null se vazio
   */
  public Message removeOldest() {
    return messages.poll();
  }

  /**
   * Drena todas mensagens do buffer (limpa buffer).
   *
   * @return lista de mensagens (ordenadas)
   */
  public List<Message> drain() {
    List<Message> batch = new ArrayList<>();
    Message msg;
    while ((msg = messages.poll()) != null) {
      batch.add(msg);
    }
    return batch;
  }

  /**
   * Retorna tamanho atual do buffer.
   *
   * @return número de mensagens
   */
  public int size() {
    return messages.size();
  }

  /**
   * Verifica se buffer está vazio.
   *
   * @return true se vazio
   */
  public boolean isEmpty() {
    return messages.isEmpty();
  }

  /**
   * Retorna timestamp da última mensagem recebida.
   *
   * @return timestamp
   */
  public Instant lastMessageTime() {
    return lastMessageTime.get();
  }

  /**
   * Define tarefa agendada (cancela anterior se existir).
   *
   * @param task nova tarefa agendada
   */
  public void setScheduledTask(ScheduledFuture<?> task) {
    ScheduledFuture<?> old = scheduledTask.getAndSet(task);
    if (old != null && !old.isDone()) {
      old.cancel(false);
    }
  }

  /**
   * Cancela tarefa agendada atual.
   */
  public void cancelScheduledTask() {
    ScheduledFuture<?> task = scheduledTask.getAndSet(null);
    if (task != null && !task.isDone()) {
      task.cancel(false);
    }
  }

  /**
   * Retorna ID do usuário deste buffer.
   *
   * @return user ID
   */
  public String userId() {
    return userId;
  }
}
