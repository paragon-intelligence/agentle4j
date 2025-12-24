package com.paragon.telemetry.processors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.telemetry.events.ResponseCompletedEvent;
import com.paragon.telemetry.events.TelemetryEvent;

/**
 * Comprehensive tests for ProcessorRegistry.
 *
 * <p>Tests cover:
 * - Factory methods (empty, of)
 * - Broadcast to multiple processors
 * - Flush and shutdown lifecycle
 * - Accessors (hasProcessors, size)
 */
@DisplayName("ProcessorRegistry Tests")
class ProcessorRegistryTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("empty() creates registry with no processors")
    void empty_createsRegistryWithNoProcessors() {
      ProcessorRegistry registry = ProcessorRegistry.empty();

      assertFalse(registry.hasProcessors());
      assertEquals(0, registry.size());
    }

    @Test
    @DisplayName("of(List) creates registry with given processors")
    void ofList_createsRegistryWithProcessors() {
      TestProcessor p1 = new TestProcessor("p1");
      TestProcessor p2 = new TestProcessor("p2");

      ProcessorRegistry registry = ProcessorRegistry.of(List.of(p1, p2));

      assertTrue(registry.hasProcessors());
      assertEquals(2, registry.size());

      p1.shutdown();
      p2.shutdown();
    }

    @Test
    @DisplayName("of(single) creates registry with single processor")
    void ofSingle_createsRegistryWithSingleProcessor() {
      TestProcessor processor = new TestProcessor("test");

      ProcessorRegistry registry = ProcessorRegistry.of(processor);

      assertTrue(registry.hasProcessors());
      assertEquals(1, registry.size());

      processor.shutdown();
    }

    @Test
    @DisplayName("processors list is unmodifiable")
    void processorsListIsUnmodifiable() {
      TestProcessor processor = new TestProcessor("test");
      ProcessorRegistry registry = ProcessorRegistry.of(processor);

      assertThrows(UnsupportedOperationException.class, () ->
          registry.processors().add(new TestProcessor("another")));

      processor.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BROADCAST
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Broadcast")
  class Broadcast {

    @Test
    @DisplayName("broadcast sends event to all processors")
    void broadcast_sendsEventToAllProcessors() throws InterruptedException {
      TestProcessor p1 = new TestProcessor("p1");
      TestProcessor p2 = new TestProcessor("p2");
      ProcessorRegistry registry = ProcessorRegistry.of(List.of(p1, p2));

      TelemetryEvent event = createTestEvent();
      registry.broadcast(event);

      // Wait for async processing
      Thread.sleep(200);

      assertEquals(1, p1.getProcessedCount());
      assertEquals(1, p2.getProcessedCount());

      registry.shutdown();
    }

    @Test
    @DisplayName("broadcast handles empty registry gracefully")
    void broadcast_handlesEmptyRegistry() {
      ProcessorRegistry registry = ProcessorRegistry.empty();

      // Should not throw
      assertDoesNotThrow(() -> registry.broadcast(createTestEvent()));
    }

    @Test
    @DisplayName("broadcast continues even if one processor fails")
    void broadcast_continuesOnProcessorFailure() throws InterruptedException {
      TestProcessor goodProcessor = new TestProcessor("good");
      FailingProcessor failingProcessor = new FailingProcessor();
      ProcessorRegistry registry = ProcessorRegistry.of(List.of(failingProcessor, goodProcessor));

      registry.broadcast(createTestEvent());

      // Wait for async processing
      Thread.sleep(200);

      // Good processor should still receive the event
      assertEquals(1, goodProcessor.getProcessedCount());

      registry.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FLUSH AND SHUTDOWN
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Flush and Shutdown")
  class FlushAndShutdown {

    @Test
    @DisplayName("flushAll returns true when all processors flush successfully")
    void flushAll_returnsTrueWhenAllFlush() {
      TestProcessor p1 = new TestProcessor("p1");
      TestProcessor p2 = new TestProcessor("p2");
      ProcessorRegistry registry = ProcessorRegistry.of(List.of(p1, p2));

      boolean result = registry.flushAll(1, TimeUnit.SECONDS);

      assertTrue(result);

      registry.shutdown();
    }

    @Test
    @DisplayName("flushAll returns true for empty registry")
    void flushAll_returnsTrueForEmptyRegistry() {
      ProcessorRegistry registry = ProcessorRegistry.empty();

      boolean result = registry.flushAll(1, TimeUnit.SECONDS);

      assertTrue(result);
    }

    @Test
    @DisplayName("shutdown shuts down all processors")
    void shutdown_shutsDownAllProcessors() {
      TestProcessor p1 = new TestProcessor("p1");
      TestProcessor p2 = new TestProcessor("p2");
      ProcessorRegistry registry = ProcessorRegistry.of(List.of(p1, p2));

      registry.shutdown();

      assertFalse(p1.isRunning());
      assertFalse(p2.isRunning());
    }

    @Test
    @DisplayName("shutdown handles empty registry gracefully")
    void shutdown_handlesEmptyRegistry() {
      ProcessorRegistry registry = ProcessorRegistry.empty();

      assertDoesNotThrow(registry::shutdown);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("hasProcessors returns false for empty registry")
    void hasProcessors_returnsFalseForEmpty() {
      ProcessorRegistry registry = ProcessorRegistry.empty();

      assertFalse(registry.hasProcessors());
    }

    @Test
    @DisplayName("hasProcessors returns true when processors exist")
    void hasProcessors_returnsTrueWhenProcessorsExist() {
      TestProcessor processor = new TestProcessor("test");
      ProcessorRegistry registry = ProcessorRegistry.of(processor);

      assertTrue(registry.hasProcessors());

      processor.shutdown();
    }

    @Test
    @DisplayName("size returns correct count")
    void size_returnsCorrectCount() {
      TestProcessor p1 = new TestProcessor("p1");
      TestProcessor p2 = new TestProcessor("p2");
      TestProcessor p3 = new TestProcessor("p3");
      ProcessorRegistry registry = ProcessorRegistry.of(List.of(p1, p2, p3));

      assertEquals(3, registry.size());

      registry.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER CLASSES
  // ═══════════════════════════════════════════════════════════════════════════

  private TelemetryEvent createTestEvent() {
    return ResponseCompletedEvent.create(
        "session-123",
        "trace-456",
        "span-789",
        System.currentTimeMillis() * 1_000_000L,
        "test-model"
    );
  }

  /** Test processor that counts processed events. */
  private static class TestProcessor extends TelemetryProcessor {
    private final AtomicInteger processedCount = new AtomicInteger(0);

    TestProcessor(String name) {
      super(name);
    }

    @Override
    protected void doProcess(TelemetryEvent event) {
      processedCount.incrementAndGet();
    }

    int getProcessedCount() {
      return processedCount.get();
    }
  }

  /** Processor that always fails on process. */
  private static class FailingProcessor extends TelemetryProcessor {
    FailingProcessor() {
      super("failing");
    }

    @Override
    protected void doProcess(TelemetryEvent event) {
      throw new RuntimeException("Intentional failure");
    }
  }
}
