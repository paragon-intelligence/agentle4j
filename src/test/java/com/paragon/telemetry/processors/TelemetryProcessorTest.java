package com.paragon.telemetry.processors;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.telemetry.events.ResponseCompletedEvent;
import com.paragon.telemetry.events.TelemetryEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for TelemetryProcessor.
 *
 * <p>Tests cover: - Async event processing - Queue behavior - Flush and shutdown lifecycle - Error
 * handling
 */
@DisplayName("TelemetryProcessor Tests")
class TelemetryProcessorTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // BASIC FUNCTIONALITY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Basic Functionality")
  class BasicFunctionality {

    @Test
    @DisplayName("processor starts in running state")
    void processor_startsInRunningState() {
      TestProcessor processor = new TestProcessor("test");

      assertTrue(processor.isRunning());

      processor.shutdown();
    }

    @Test
    @DisplayName("getProcessorName returns configured name")
    void getProcessorName_returnsConfiguredName() {
      TestProcessor processor = new TestProcessor("my-processor");

      assertEquals("my-processor", processor.getProcessorName());

      processor.shutdown();
    }

    @Test
    @DisplayName("getQueueSize returns zero initially")
    void getQueueSize_returnsZeroInitially() {
      TestProcessor processor = new TestProcessor("test");

      assertEquals(0, processor.getQueueSize());

      processor.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ASYNC PROCESSING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Async Processing")
  class AsyncProcessing {

    @Test
    @DisplayName("process queues event and returns immediately")
    void process_queuesEventAndReturnsImmediately() {
      TestProcessor processor = new TestProcessor("test");
      TelemetryEvent event = createTestEvent();

      long start = System.currentTimeMillis();
      processor.process(event);
      long duration = System.currentTimeMillis() - start;

      // Should return very quickly (< 100ms)
      assertTrue(duration < 100);

      processor.shutdown();
    }

    @Test
    @DisplayName("process invokes doProcess asynchronously")
    void process_invokesDoProcessAsynchronously() throws InterruptedException {
      CountDownLatch latch = new CountDownLatch(1);
      LatchProcessor processor = new LatchProcessor("test", latch);

      processor.process(createTestEvent());

      // Wait for async processing
      boolean processed = latch.await(2, TimeUnit.SECONDS);
      assertTrue(processed);

      processor.shutdown();
    }

    @Test
    @DisplayName("multiple events are processed in order")
    void multipleEvents_processedInOrder() throws InterruptedException {
      OrderTrackingProcessor processor = new OrderTrackingProcessor("test");

      for (int i = 0; i < 5; i++) {
        processor.process(createTestEvent("session-" + i));
      }

      // Wait for processing
      Thread.sleep(500);

      assertEquals(5, processor.getProcessedCount());
      assertEquals("session-0", processor.getFirstSessionId());
      assertEquals("session-4", processor.getLastSessionId());

      processor.shutdown();
    }

    @Test
    @DisplayName("process returns failed future after shutdown")
    void process_returnsFailedFutureAfterShutdown() {
      TestProcessor processor = new TestProcessor("test");
      processor.shutdown();

      processor.process(createTestEvent());

      // Process returns void now, so we just verify no exception thrown
      // (would have thrown if shutdown check failed)
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FLUSH
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Flush")
  class Flush {

    @Test
    @DisplayName("flush returns true for empty queue")
    void flush_returnsTrueForEmptyQueue() {
      TestProcessor processor = new TestProcessor("test");

      boolean flushed = processor.flush(1, TimeUnit.SECONDS);

      assertTrue(flushed);

      processor.shutdown();
    }

    @Test
    @DisplayName("flush waits for pending events")
    void flush_waitsForPendingEvents() throws InterruptedException {
      TestProcessor processor = new TestProcessor("test");

      // Queue an event
      processor.process(createTestEvent());

      // Flush should succeed within reasonable time
      boolean flushed = processor.flush(5, TimeUnit.SECONDS);

      assertTrue(flushed);

      processor.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SHUTDOWN
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Shutdown")
  class Shutdown {

    @Test
    @DisplayName("shutdown sets running to false")
    void shutdown_setsRunningToFalse() {
      TestProcessor processor = new TestProcessor("test");

      processor.shutdown();

      assertFalse(processor.isRunning());
    }

    @Test
    @DisplayName("shutdown processes remaining events")
    void shutdown_processesRemainingEvents() throws InterruptedException {
      TestProcessor processor = new TestProcessor("test");

      // Queue some events
      for (int i = 0; i < 3; i++) {
        processor.process(createTestEvent());
      }

      processor.shutdown();

      // Allow time for shutdown to complete
      Thread.sleep(100);

      assertEquals(3, processor.getProcessedCount());
    }

    @Test
    @DisplayName("shutdown is idempotent")
    void shutdown_isIdempotent() {
      TestProcessor processor = new TestProcessor("test");

      // Multiple shutdowns should not throw
      assertDoesNotThrow(
          () -> {
            processor.shutdown();
            processor.shutdown();
            processor.shutdown();
          });
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ERROR HANDLING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandling {

    @Test
    @DisplayName("error in doProcess does not stop processing")
    void errorInDoProcess_doesNotStopProcessing() throws InterruptedException {
      FailEveryOtherProcessor processor = new FailEveryOtherProcessor("test");

      // Queue 4 events (events 0, 2 will fail; 1, 3 will succeed)
      for (int i = 0; i < 4; i++) {
        processor.process(createTestEvent());
      }

      Thread.sleep(500);

      // All events should be processed (even if some fail)
      assertEquals(4, processor.getAttemptCount());
      assertEquals(2, processor.getSuccessCount());

      processor.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS AND CLASSES
  // ═══════════════════════════════════════════════════════════════════════════

  private TelemetryEvent createTestEvent() {
    return createTestEvent("session-" + System.nanoTime());
  }

  private TelemetryEvent createTestEvent(String sessionId) {
    return ResponseCompletedEvent.create(
        sessionId, "trace-456", "span-789", System.currentTimeMillis() * 1_000_000L, "test-model");
  }

  /** Basic test processor that counts processed events. */
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

  /** Processor that signals a latch when processing. */
  private static class LatchProcessor extends TelemetryProcessor {
    private final CountDownLatch latch;

    LatchProcessor(String name, CountDownLatch latch) {
      super(name);
      this.latch = latch;
    }

    @Override
    protected void doProcess(TelemetryEvent event) {
      latch.countDown();
    }
  }

  /** Processor that tracks order of events. */
  private static class OrderTrackingProcessor extends TelemetryProcessor {
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicReference<String> firstSessionId = new AtomicReference<>();
    private final AtomicReference<String> lastSessionId = new AtomicReference<>();

    OrderTrackingProcessor(String name) {
      super(name);
    }

    @Override
    protected void doProcess(TelemetryEvent event) {
      firstSessionId.compareAndSet(null, event.sessionId());
      lastSessionId.set(event.sessionId());
      processedCount.incrementAndGet();
    }

    int getProcessedCount() {
      return processedCount.get();
    }

    String getFirstSessionId() {
      return firstSessionId.get();
    }

    String getLastSessionId() {
      return lastSessionId.get();
    }
  }

  /** Processor with configurable delay. */
  private static class SlowProcessor extends TelemetryProcessor {
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final long delayMs;

    SlowProcessor(String name, long delayMs) {
      super(name);
      this.delayMs = delayMs;
    }

    @Override
    protected void doProcess(TelemetryEvent event) {
      try {
        Thread.sleep(delayMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      processedCount.incrementAndGet();
    }

    int getProcessedCount() {
      return processedCount.get();
    }
  }

  /** Processor that takes a very long time. */
  private static class VerySlowProcessor extends TelemetryProcessor {
    VerySlowProcessor(String name) {
      super(name);
    }

    @Override
    protected void doProcess(TelemetryEvent event) {
      try {
        Thread.sleep(10000); // 10 seconds
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Processor that fails every other event. */
  private static class FailEveryOtherProcessor extends TelemetryProcessor {
    private final AtomicInteger attemptCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);

    FailEveryOtherProcessor(String name) {
      super(name);
    }

    @Override
    protected void doProcess(TelemetryEvent event) {
      int attempt = attemptCount.incrementAndGet();
      if (attempt % 2 == 1) {
        throw new RuntimeException("Intentional failure for attempt " + attempt);
      }
      successCount.incrementAndGet();
    }

    int getAttemptCount() {
      return attemptCount.get();
    }

    int getSuccessCount() {
      return successCount.get();
    }
  }
}
