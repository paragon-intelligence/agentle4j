package com.paragon.telemetry.processors;

import com.paragon.telemetry.events.TelemetryEvent;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for async telemetry event processors.
 *
 * <p>Provides queue-based event processing with background worker threads. Implementations override
 * {@link #doProcess} to handle events.
 *
 * <p>Key design principles:
 *
 * <ul>
 *   <li>Fire-and-forget: {@link #process} returns immediately
 *   <li>Non-blocking: events are queued and processed asynchronously
 *   <li>Graceful shutdown: flush pending events before termination
 * </ul>
 */
public abstract class TelemetryProcessor {

  private static final Logger logger = LoggerFactory.getLogger(TelemetryProcessor.class);

  private final BlockingQueue<TelemetryEvent> eventQueue;
  private final ExecutorService executorService;
  private final AtomicBoolean running;
  private final int maxQueueSize;
  private final String processorName;

  /** Creates a processor with default queue size (1000) and single worker thread. */
  protected TelemetryProcessor(@NonNull String processorName) {
    this(processorName, 1000, 1);
  }

  /** Creates a processor with custom queue size and worker thread count. */
  protected TelemetryProcessor(@NonNull String processorName, int maxQueueSize, int workerThreads) {
    this.processorName = processorName;
    this.maxQueueSize = maxQueueSize;
    this.eventQueue = new LinkedBlockingQueue<>(maxQueueSize);
    this.running = new AtomicBoolean(true);
    this.executorService =
        Executors.newFixedThreadPool(
            workerThreads,
            r -> {
              Thread t = new Thread(r, processorName + "-worker");
              t.setDaemon(true);
              return t;
            });

    // Start worker threads
    for (int i = 0; i < workerThreads; i++) {
      executorService.submit(this::processLoop);
    }
  }

  /**
   * Queues an event for async processing. Returns immediately without blocking (fire-and-forget).
   *
   * @param event the telemetry event to process
   */
  public void process(@NonNull TelemetryEvent event) {
    if (!running.get()) {
      logger.warn("[{}] Ignoring event, processor has been shut down", processorName);
      return;
    }

    boolean offered = eventQueue.offer(event);
    if (!offered) {
      logger.warn("[{}] Event queue full, dropping event: {}", processorName, event.sessionId());
    }
  }

  /**
   * Processes an event. Implementations should handle the event according to their vendor-specific
   * logic (e.g., send to Langfuse, Grafana).
   *
   * <p>This method is called on a background thread and should not block for extended periods.
   *
   * @param event the event to process
   */
  protected abstract void doProcess(@NonNull TelemetryEvent event);

  /** Background worker loop that consumes events from the queue. */
  private void processLoop() {
    while (running.get() || !eventQueue.isEmpty()) {
      try {
        TelemetryEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
        if (event != null) {
          try {
            doProcess(event);
          } catch (Exception e) {
            logger.error("[{}] Error processing event: {}", processorName, e.getMessage(), e);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  /**
   * Flushes all pending events by waiting for the queue to drain. Blocks until all events are
   * processed or timeout is reached.
   *
   * @param timeout max time to wait
   * @param unit time unit
   * @return true if all events were flushed, false if timeout
   */
  public boolean flush(long timeout, @NonNull TimeUnit unit) {
    long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);

    while (!eventQueue.isEmpty()) {
      if (System.nanoTime() > deadlineNanos) {
        logger.warn(
            "[{}] Flush timeout with {} events remaining", processorName, eventQueue.size());
        return false;
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }

    return true;
  }

  /** Gracefully shuts down the processor. Attempts to flush pending events before termination. */
  public void shutdown() {
    logger.info("[{}] Shutting down...", processorName);
    running.set(false);

    // Give time to process remaining events
    flush(5, TimeUnit.SECONDS);

    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }

    logger.info("[{}] Shutdown complete", processorName);
  }

  /** Returns the processor name for logging and identification. */
  public @NonNull String getProcessorName() {
    return processorName;
  }

  /** Returns the current queue size. */
  public int getQueueSize() {
    return eventQueue.size();
  }

  /** Returns whether the processor is running. */
  public boolean isRunning() {
    return running.get();
  }
}
