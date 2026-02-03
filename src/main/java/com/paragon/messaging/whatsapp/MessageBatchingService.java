package com.paragon.messaging.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Serviço principal de batching e rate limiting de mensagens.
 *
 * <p>Funcionalidades:</p>
 * <ul>
 *   <li>Batching adaptativo com timeout e silence threshold</li>
 *   <li>Rate limiting híbrido (Token Bucket + Sliding Window)</li>
 *   <li>Deduplicação via MessageStore</li>
 *   <li>Backpressure handling configurável</li>
 *   <li>Error handling com retry exponencial</li>
 *   <li>Pre/post hooks extensíveis</li>
 *   <li>Processamento assíncrono com virtual threads</li>
 * </ul>
 *
 * <p><b>Exemplo de uso:</b></p>
 * <pre>{@code
 * MessageBatchingService service = MessageBatchingService.builder()
 *     .config(batchingConfig)
 *     .processor(messageProcessor)
 *     .addPreHook(loggingHook)
 *     .addPostHook(metricsHook)
 *     .build();
 *
 * // No webhook do WhatsApp
 * service.receiveMessage(userId, messageId, content, Instant.now());
 * }</pre>
 *
 * @author Agentle Team
 * @since 1.0
 */
public class MessageBatchingService {

  private static final Logger log = LoggerFactory.getLogger(MessageBatchingService.class);

  private final BatchingConfig config;
  private final MessageProcessor processor;
  private final List<ProcessingHook> preHooks;
  private final List<ProcessingHook> postHooks;

  // Estado por usuário
  private final ConcurrentHashMap<String, UserMessageBuffer> userBuffers;
  private final ConcurrentHashMap<String, HybridRateLimiter> rateLimiters;

  // Scheduler para batching adaptativo
  private final ScheduledExecutorService scheduler;
  private final Random random = new Random();

  private MessageBatchingService(Builder builder) {
    this.config = Objects.requireNonNull(builder.config, "config required");
    this.processor = Objects.requireNonNull(builder.processor, "processor required");
    this.preHooks = List.copyOf(builder.preHooks);
    this.postHooks = List.copyOf(builder.postHooks);

    this.userBuffers = new ConcurrentHashMap<>();
    this.rateLimiters = new ConcurrentHashMap<>();

    // Scheduler com virtual threads
    this.scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofVirtual().factory()
    );

    log.info("MessageBatchingService initialized with config: {}", config);
  }

  /**
   * Builder para MessageBatchingService.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Recebe mensagem do webhook (retorna imediatamente).
   *
   * <p>Fluxo:</p>
   * <ol>
   *   <li>Verifica deduplicação (se MessageStore configurado)</li>
   *   <li>Aplica rate limiting</li>
   *   <li>Adiciona ao buffer do usuário</li>
   *   <li>Agenda processamento adaptativo</li>
   * </ol>
   *
   * @param userId    ID do usuário (ex: número WhatsApp)
   * @param messageId ID único da mensagem (deduplicação)
   * @param content   conteúdo textual da mensagem
   * @param timestamp quando mensagem foi recebida
   */
  public void receiveMessage(
          String userId,
          String messageId,
          String content,
          Instant timestamp) {

    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(messageId, "messageId cannot be null");
    Objects.requireNonNull(content, "content cannot be null");
    Objects.requireNonNull(timestamp, "timestamp cannot be null");

    try {
      // 1. Deduplicação
      if (isDuplicate(userId, messageId)) {
        log.debug("Duplicate message ignored: userId={}, messageId={}", userId, messageId);
        return;
      }

      // 2. Rate Limiting
      if (isRateLimited(userId)) {
        log.warn("Rate limit exceeded: userId={}", userId);
        handleRateLimitExceeded(userId);
        return;
      }

      // 3. Adicionar ao buffer
      UserMessageBuffer buffer = getOrCreateBuffer(userId);
      Message message = new Message(messageId, content, timestamp);

      boolean added = buffer.add(message);
      if (!added) {
        // Buffer cheio - aplicar backpressure strategy
        handleBackpressure(userId, buffer, message);
        return;
      }

      // 4. Agendar processamento adaptativo
      scheduleAdaptiveProcessing(userId, buffer);

    } catch (Exception e) {
      log.error("Error receiving message: userId={}, messageId={}", userId, messageId, e);
    }
  }

  /**
   * Verifica se mensagem é duplicata.
   */
  private boolean isDuplicate(String userId, String messageId) {
    return config.messageStore()
            .map(store -> store.hasProcessed(userId, messageId))
            .orElse(false);
  }

  /**
   * Verifica se usuário excedeu rate limit.
   */
  private boolean isRateLimited(String userId) {
    HybridRateLimiter limiter = getRateLimiter(userId);
    return !limiter.tryAcquire();
  }

  /**
   * Agenda processamento adaptativo do buffer.
   *
   * <p>Usa silence threshold: se usuário para de enviar por X segundos,
   * processa imediatamente (não espera timeout completo).</p>
   */
  private void scheduleAdaptiveProcessing(String userId, UserMessageBuffer buffer) {
    // Cancela agendamento anterior (se existir)
    buffer.cancelScheduledTask();

    // Agenda verificação após silence threshold
    ScheduledFuture<?> task = scheduler.schedule(
            () -> checkAndProcessIfSilent(userId, buffer),
            config.silenceThreshold().toMillis(),
            TimeUnit.MILLISECONDS
    );

    buffer.setScheduledTask(task);

    // Também agenda timeout máximo (em paralelo)
    scheduler.schedule(
            () -> processIfPending(userId, buffer),
            config.adaptiveTimeout().toMillis(),
            TimeUnit.MILLISECONDS
    );
  }

  /**
   * Verifica se houve silêncio e processa se sim.
   */
  private void checkAndProcessIfSilent(String userId, UserMessageBuffer buffer) {
    if (buffer.isEmpty()) {
      return;  // Buffer já foi processado
    }

    Duration sinceLastMessage = Duration.between(
            buffer.lastMessageTime(),
            Instant.now()
    );

    if (sinceLastMessage.compareTo(config.silenceThreshold()) >= 0) {
      // Silêncio detectado - processar
      log.debug("Silence threshold reached for user: {}", userId);
      processBatch(userId, buffer);
    } else {
      // Ainda recebendo - reagendar
      scheduleAdaptiveProcessing(userId, buffer);
    }
  }

  /**
   * Processa buffer se ainda houver mensagens pendentes (timeout atingido).
   */
  private void processIfPending(String userId, UserMessageBuffer buffer) {
    if (!buffer.isEmpty()) {
      log.debug("Adaptive timeout reached for user: {}", userId);
      processBatch(userId, buffer);
    }
  }

  /**
   * Processa batch de mensagens em virtual thread.
   */
  private void processBatch(String userId, UserMessageBuffer buffer) {
    List<Message> messages = buffer.drain();

    if (messages.isEmpty()) {
      return;
    }

    log.info("Processing batch for user {}: {} messages", userId, messages.size());

    // Processar em virtual thread (não bloqueia)
    Thread.startVirtualThread(() -> {
      processInVirtualThread(userId, messages, 0);
    });
  }

  /**
   * Processamento real em virtual thread (com retry).
   */
  private void processInVirtualThread(String userId, List<Message> messages, int retryCount) {
    HookContext context = createHookContext(userId, messages, retryCount);

    try {
      // Pre-hooks
      executeHooks(preHooks, context);

      // Processar com AI agent
      processor.process(userId, messages);

      // Marcar como processadas (deduplicação)
      markAsProcessed(userId, messages);

      // Post-hooks
      executeHooks(postHooks, context);

      log.info("Batch processed successfully: userId={}, count={}", userId, messages.size());

    } catch (HookInterruptedException e) {
      log.warn("Processing interrupted by hook: userId={}, reason={}, code={}",
              userId, e.getReason(), e.getReasonCode());

    } catch (Exception e) {
      log.error("Processing failed: userId={}, retryCount={}", userId, retryCount, e);
      handleProcessingError(userId, messages, retryCount, e, context);
    }
  }

  /**
   * Cria HookContext para hooks.
   */
  private HookContext createHookContext(String userId, List<Message> messages, int retryCount) {
    if (retryCount == 0) {
      return HookContext.create(userId, messages);
    } else {
      return HookContext.forRetry(
              HookContext.create(userId, messages),
              retryCount
      );
    }
  }

  /**
   * Executa lista de hooks.
   */
  private void executeHooks(List<ProcessingHook> hooks, HookContext context) throws Exception {
    for (ProcessingHook hook : hooks) {
      hook.execute(context);
    }
  }

  /**
   * Marca mensagens como processadas (deduplicação).
   */
  private void markAsProcessed(String userId, List<Message> messages) {
    config.messageStore().ifPresent(store -> {
      messages.forEach(msg -> store.markProcessed(userId, msg.messageId()));
    });
  }

  /**
   * Trata erro de processamento (retry com exponential backoff).
   */
  private void handleProcessingError(
          String userId,
          List<Message> messages,
          int retryCount,
          Exception error,
          HookContext context) {

    ErrorHandlingStrategy strategy = config.errorHandlingStrategy();

    if (retryCount < strategy.maxRetries()) {
      // Retry com exponential backoff
      Duration delay = strategy.calculateDelay(retryCount + 1);

      log.info("Retrying in {}: userId={}, attempt={}/{}",
              delay, userId, retryCount + 1, strategy.maxRetries());

      scheduler.schedule(
              () -> processInVirtualThread(userId, messages, retryCount + 1),
              delay.toMillis(),
              TimeUnit.MILLISECONDS
      );

    } else {
      // Falha permanente
      log.error("Processing failed permanently after {} retries: userId={}",
              retryCount, userId, error);

      // Dead Letter Queue
      if (strategy.hasDLQHandler()) {
        try {
          strategy.deadLetterHandler().get().accept(userId, messages);
          log.info("Messages sent to DLQ: userId={}, count={}", userId, messages.size());
        } catch (Exception dlqError) {
          log.error("DLQ handler failed: userId={}", userId, dlqError);
        }
      }

      // Notificar usuário
      if (strategy.notifyUserOnFailure()) {
        notifyUser(userId, strategy.getNotificationMessage());
      }
    }
  }

  /**
   * Trata backpressure quando buffer cheio.
   */
  private void handleBackpressure(String userId, UserMessageBuffer buffer, Message newMessage) {
    BackpressureStrategy strategy = config.backpressureStrategy();

    log.warn("Buffer full for user {}, applying strategy: {}", userId, strategy);

    switch (strategy) {
      case DROP_NEW -> {
        log.debug("Dropping new message: userId={}", userId);
        // Não faz nada - mensagem é descartada
      }

      case DROP_OLDEST -> {
        Message dropped = buffer.removeOldest();
        log.debug("Dropped oldest message: userId={}, droppedId={}",
                userId, dropped != null ? dropped.messageId() : "none");
        buffer.add(newMessage);
      }

      case FLUSH_AND_ACCEPT -> {
        log.debug("Flushing buffer and accepting new message: userId={}", userId);
        processBatch(userId, buffer);
        buffer.add(newMessage);
      }

      case REJECT_WITH_NOTIFICATION -> {
        log.debug("Rejecting message with notification: userId={}", userId);
        notifyUser(userId, "Você está enviando mensagens muito rápido. Por favor, aguarde um momento.");
      }

      case BLOCK_UNTIL_SPACE -> {
        log.warn("BLOCK_UNTIL_SPACE not recommended - may cause webhook timeout: userId={}", userId);
        // TODO: Implementar espera com timeout
      }
    }
  }

  /**
   * Trata rate limit excedido.
   */
  private void handleRateLimitExceeded(String userId) {
    // TODO: Implementar notificação ou ação customizada
    log.debug("Rate limit notification sent: userId={}", userId);
  }

  /**
   * Notifica usuário (deve ser implementado pelo consumidor da API).
   */
  private void notifyUser(String userId, String message) {
    // Esta é uma operação opcional - o consumidor da API
    // deve implementar notificação real se necessário
    log.info("User notification: userId={}, message={}", userId, message);
  }

  /**
   * Obtém ou cria buffer para usuário.
   */
  private UserMessageBuffer getOrCreateBuffer(String userId) {
    return userBuffers.computeIfAbsent(
            userId,
            k -> new UserMessageBuffer(userId, config.maxBufferSize())
    );
  }

  /**
   * Obtém ou cria rate limiter para usuário.
   */
  private HybridRateLimiter getRateLimiter(String userId) {
    return rateLimiters.computeIfAbsent(
            userId,
            k -> new HybridRateLimiter(config.rateLimitConfig())
    );
  }

  /**
   * Shutdown graceful do serviço.
   */
  public void shutdown() {
    log.info("Shutting down MessageBatchingService...");

    try {
      scheduler.shutdown();
      if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
      log.info("MessageBatchingService shut down successfully");
    } catch (InterruptedException e) {
      log.error("Shutdown interrupted", e);
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Retorna estatísticas do serviço (útil para monitoramento).
   */
  public ServiceStats getStats() {
    return new ServiceStats(
            userBuffers.size(),
            userBuffers.values().stream()
                    .mapToInt(UserMessageBuffer::size)
                    .sum()
    );
  }

  /**
   * Estatísticas do serviço.
   */
  public record ServiceStats(int activeUsers, int pendingMessages) {
  }

  public static class Builder {
    private final List<ProcessingHook> preHooks = new ArrayList<>();
    private final List<ProcessingHook> postHooks = new ArrayList<>();
    private BatchingConfig config;
    private MessageProcessor processor;

    public Builder config(BatchingConfig config) {
      this.config = config;
      return this;
    }

    public Builder processor(MessageProcessor processor) {
      this.processor = processor;
      return this;
    }

    public Builder addPreHook(ProcessingHook hook) {
      this.preHooks.add(hook);
      return this;
    }

    public Builder addPostHook(ProcessingHook hook) {
      this.postHooks.add(hook);
      return this;
    }

    public Builder addPreHooks(ProcessingHook... hooks) {
      this.preHooks.addAll(List.of(hooks));
      return this;
    }

    public Builder addPostHooks(ProcessingHook... hooks) {
      this.postHooks.addAll(List.of(hooks));
      return this;
    }

    public MessageBatchingService build() {
      return new MessageBatchingService(this);
    }
  }
}