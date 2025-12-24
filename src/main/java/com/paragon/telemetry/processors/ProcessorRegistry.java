package com.paragon.telemetry.processors;

import com.paragon.telemetry.events.TelemetryEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for managing multiple telemetry processors. Broadcasts events to all registered
 * processors.
 *
 * <p>This replaces the old {@code OpenTelemetryVendors} class with a cleaner API and proper
 * lifecycle management.
 */
public record ProcessorRegistry(@NonNull List<TelemetryProcessor> processors) {

  private static final Logger logger = LoggerFactory.getLogger(ProcessorRegistry.class);

  /** Creates an empty registry. */
  public static @NonNull ProcessorRegistry empty() {
    return new ProcessorRegistry(Collections.emptyList());
  }

  /** Creates a registry with the given processors. */
  public static @NonNull ProcessorRegistry of(@NonNull List<TelemetryProcessor> processors) {
    return new ProcessorRegistry(Collections.unmodifiableList(new ArrayList<>(processors)));
  }

  /** Creates a registry with a single processor. */
  public static @NonNull ProcessorRegistry of(@NonNull TelemetryProcessor processor) {
    return new ProcessorRegistry(List.of(processor));
  }

  /**
   * Broadcasts an event to all registered processors. This is fire-and-forget - it does not wait
   * for processing.
   *
   * @param event the event to broadcast
   */
  public void broadcast(@NonNull TelemetryEvent event) {
    for (TelemetryProcessor processor : processors) {
      try {
        processor.process(event);
      } catch (Exception e) {
        logger.error(
            "Error broadcasting event to {}: {}", processor.getProcessorName(), e.getMessage(), e);
      }
    }
  }

  /**
   * Flushes all processors.
   *
   * @param timeout max time to wait for each processor
   * @param unit time unit
   * @return true if all processors flushed successfully
   */
  public boolean flushAll(long timeout, @NonNull TimeUnit unit) {
    boolean allFlushed = true;
    for (TelemetryProcessor processor : processors) {
      if (!processor.flush(timeout, unit)) {
        allFlushed = false;
      }
    }
    return allFlushed;
  }

  /** Shuts down all registered processors gracefully. */
  public void shutdown() {
    logger.info("Shutting down {} processors...", processors.size());
    for (TelemetryProcessor processor : processors) {
      try {
        processor.shutdown();
      } catch (Exception e) {
        logger.error("Error shutting down {}: {}", processor.getProcessorName(), e.getMessage(), e);
      }
    }
    logger.info("All processors shut down");
  }

  /** Returns whether any processors are registered. */
  public boolean hasProcessors() {
    return !processors.isEmpty();
  }

  /** Returns the number of registered processors. */
  public int size() {
    return processors.size();
  }
}
