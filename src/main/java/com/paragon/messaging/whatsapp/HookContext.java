package com.paragon.messaging.whatsapp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contexto passado para hooks durante processamento de lote de mensagens.
 *
 * <p>Contém informações sobre o lote sendo processado e um mapa mutável
 * para hooks compartilharem dados entre si.</p>
 *
 * <p><b>Exemplo de uso entre hooks:</b></p>
 * <pre>{@code
 * // Pre-hook armazena timestamp
 * ProcessingHook preHook = context -> {
 *     context.putMetadata("startTime", Instant.now());
 *     context.putMetadata("userTier", getUserTier(context.userId()));
 * };
 *
 * // Post-hook calcula duração
 * ProcessingHook postHook = context -> {
 *     Instant start = context.getMetadata("startTime", Instant.class).orElseThrow();
 *     Duration elapsed = Duration.between(start, Instant.now());
 *     metrics.record(context.userId(), elapsed);
 * };
 * }</pre>
 *
 * @param userId         ID do usuário cujas mensagens estão sendo processadas
 * @param messages       lista de mensagens no lote (imutável)
 * @param batchStartTime quando o processamento do lote começou
 * @param batchSize      número de mensagens
 * @param isRetry        se é tentativa de retry
 * @param retryCount     número de tentativas (0 = primeira tentativa)
 * @param metadata       mapa mutável (thread-safe) para compartilhar dados
 * @author Agentle Team
 * @since 1.0
 */
public record HookContext(
        String userId,
        List<Message> messages,
        Instant batchStartTime,
        int batchSize,
        boolean isRetry,
        int retryCount,
        Map<String, Object> metadata
) {

  public HookContext {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId cannot be null or blank");
    }
    if (messages == null) {
      throw new IllegalArgumentException("messages cannot be null");
    }
    if (batchStartTime == null) {
      throw new IllegalArgumentException("batchStartTime cannot be null");
    }
    if (batchSize < 0) {
      throw new IllegalArgumentException("batchSize cannot be negative");
    }
    if (retryCount < 0) {
      throw new IllegalArgumentException("retryCount cannot be negative");
    }

    // Mensagens imutáveis
    messages = List.copyOf(messages);

    // Metadata mutável (para hooks compartilharem)
    if (metadata == null) {
      metadata = new ConcurrentHashMap<>();
    }
  }

  /**
   * Cria contexto para primeira tentativa.
   *
   * @param userId   ID do usuário
   * @param messages mensagens a processar
   * @return novo HookContext
   */
  public static HookContext create(String userId, List<Message> messages) {
    return new HookContext(
            userId,
            messages,
            Instant.now(),
            messages.size(),
            false,
            0,
            new ConcurrentHashMap<>()
    );
  }

  /**
   * Cria contexto para retry.
   *
   * @param original   contexto original
   * @param retryCount número da tentativa
   * @return novo HookContext para retry
   */
  public static HookContext forRetry(HookContext original, int retryCount) {
    return new HookContext(
            original.userId,
            original.messages,
            Instant.now(),
            original.batchSize,
            true,
            retryCount,
            original.metadata  // Reutiliza metadata
    );
  }

  /**
   * Tempo decorrido desde início do processamento.
   *
   * @return duração desde batchStartTime
   */
  public Duration elapsedTime() {
    return Duration.between(batchStartTime, Instant.now());
  }

  /**
   * Primeira mensagem do lote.
   *
   * @return primeira mensagem
   * @throws IllegalStateException se lote vazio
   */
  public Message firstMessage() {
    if (messages.isEmpty()) {
      throw new IllegalStateException("Batch is empty");
    }
    return messages.get(0);
  }

  /**
   * Última mensagem do lote.
   *
   * @return última mensagem
   * @throws IllegalStateException se lote vazio
   */
  public Message lastMessage() {
    if (messages.isEmpty()) {
      throw new IllegalStateException("Batch is empty");
    }
    return messages.get(messages.size() - 1);
  }

  /**
   * Armazena valor em metadata (thread-safe).
   *
   * @param key   chave
   * @param value valor
   */
  public void putMetadata(String key, Object value) {
    metadata.put(key, value);
  }

  /**
   * Recupera valor de metadata.
   *
   * @param key chave
   * @return Optional com valor ou empty
   */
  public Optional<Object> getMetadata(String key) {
    return Optional.ofNullable(metadata.get(key));
  }

  /**
   * Recupera valor tipado de metadata.
   *
   * @param key  chave
   * @param type tipo esperado
   * @param <T>  tipo genérico
   * @return Optional com valor tipado ou empty
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getMetadata(String key, Class<T> type) {
    Object value = metadata.get(key);
    if (value != null && type.isInstance(value)) {
      return Optional.of((T) value);
    }
    return Optional.empty();
  }

  /**
   * Verifica se metadata contém chave.
   *
   * @param key chave
   * @return true se existe
   */
  public boolean hasMetadata(String key) {
    return metadata.containsKey(key);
  }

  /**
   * Verifica se é primeira tentativa (não é retry).
   *
   * @return true se retryCount == 0
   */
  public boolean isFirstAttempt() {
    return retryCount == 0;
  }
}