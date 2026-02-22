package com.paragon.messaging.batching;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.messaging.testutil.MockMessageFactory;
import com.paragon.messaging.whatsapp.payload.InboundMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link UserMessageBuffer}. */
@DisplayName("UserMessageBuffer")
class UserMessageBufferTest {

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("constructor initializes buffer correctly")
    void constructor_initializesBuffer() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);

      assertEquals("user123", buffer.userId());
      assertEquals(0, buffer.size());
      assertTrue(buffer.isEmpty());
    }
  }

  @Nested
  @DisplayName("Message Operations")
  class MessageOperationsTests {

    @Test
    @DisplayName("add() adds message to buffer")
    void add_addsMessageToBuffer() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      InboundMessage message = MockMessageFactory.createTextMessage("user123", "Hello");

      boolean added = buffer.add(message);

      assertTrue(added);
      assertEquals(1, buffer.size());
      assertFalse(buffer.isEmpty());
    }

    @Test
    @DisplayName("add() updates last message time")
    void add_updatesLastMessageTime() throws InterruptedException {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      Instant before = Instant.now();

      Thread.sleep(10);
      InboundMessage message = MockMessageFactory.createTextMessage("user123", "Hello");
      buffer.add(message);

      Instant after = Instant.now();
      Instant lastTime = buffer.lastMessageTime();

      assertTrue(lastTime.isAfter(before));
      assertTrue(lastTime.isBefore(after) || lastTime.equals(after));
    }

    @Test
    @DisplayName("add() returns false when buffer is full")
    void add_returnsFalseWhenFull() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 2);

      buffer.add(MockMessageFactory.createTextMessage("user123", "Message 1"));
      buffer.add(MockMessageFactory.createTextMessage("user123", "Message 2"));

      boolean added = buffer.add(MockMessageFactory.createTextMessage("user123", "Message 3"));

      assertFalse(added, "Should not add message when buffer is full");
      assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("drain() returns all messages and clears buffer")
    void drain_returnsAllAndClears() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      InboundMessage msg1 = MockMessageFactory.createTextMessage("user123", "Message 1");
      InboundMessage msg2 = MockMessageFactory.createTextMessage("user123", "Message 2");
      InboundMessage msg3 = MockMessageFactory.createTextMessage("user123", "Message 3");

      buffer.add(msg1);
      buffer.add(msg2);
      buffer.add(msg3);

      List<InboundMessage> drained = buffer.drain();

      assertEquals(3, drained.size());
      assertTrue(buffer.isEmpty());
      assertEquals(0, buffer.size());
    }

    @Test
    @DisplayName("drain() on empty buffer returns empty list")
    void drain_onEmptyBuffer_returnsEmptyList() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);

      List<InboundMessage> drained = buffer.drain();

      assertNotNull(drained);
      assertTrue(drained.isEmpty());
    }

    @Test
    @DisplayName("removeOldest() removes and returns oldest message")
    void removeOldest_removesOldest() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      InboundMessage msg1 = MockMessageFactory.createTextMessage("user123", "id1", "First");
      InboundMessage msg2 = MockMessageFactory.createTextMessage("user123", "id2", "Second");

      buffer.add(msg1);
      buffer.add(msg2);

      InboundMessage removed = buffer.removeOldest();

      assertEquals(msg1.id(), removed.id());
      assertEquals(1, buffer.size());
    }

    @Test
    @DisplayName("removeOldest() on empty buffer returns null")
    void removeOldest_onEmptyBuffer_returnsNull() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);

      InboundMessage removed = buffer.removeOldest();

      assertNull(removed);
    }
  }

  @Nested
  @DisplayName("Size and State")
  class SizeStateTests {

    @Test
    @DisplayName("size() returns correct count")
    void size_returnsCorrectCount() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);

      assertEquals(0, buffer.size());

      buffer.add(MockMessageFactory.createTextMessage("user123", "Message 1"));
      assertEquals(1, buffer.size());

      buffer.add(MockMessageFactory.createTextMessage("user123", "Message 2"));
      assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("isEmpty() returns true for empty buffer")
    void isEmpty_returnsTrueForEmpty() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);

      assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("isEmpty() returns false for non-empty buffer")
    void isEmpty_returnsFalseForNonEmpty() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      buffer.add(MockMessageFactory.createTextMessage("user123", "Message"));

      assertFalse(buffer.isEmpty());
    }

    @Test
    @DisplayName("lastMessageTime() returns current time for new buffer")
    void lastMessageTime_returnsRecentTimeForNew() {
      Instant before = Instant.now();
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      Instant after = Instant.now();

      Instant lastTime = buffer.lastMessageTime();

      assertTrue(lastTime.isAfter(before.minus(Duration.ofSeconds(1))));
      assertTrue(lastTime.isBefore(after.plus(Duration.ofSeconds(1))));
    }
  }

  @Nested
  @DisplayName("Scheduled Task Management")
  class ScheduledTaskTests {

    @Test
    @DisplayName("setScheduledTask() stores task")
    void setScheduledTask_storesTask() {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);

      // Create a mock scheduled future
      ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
      var task =
          (java.util.concurrent.ScheduledFuture<?>)
              ((java.util.concurrent.ScheduledExecutorService) Executors.newScheduledThreadPool(1))
                  .schedule(() -> {}, 1, TimeUnit.SECONDS);

      assertDoesNotThrow(() -> buffer.setScheduledTask(task));

      task.cancel(false);
      executor.shutdown();
    }

    @Test
    @DisplayName("setScheduledTask() cancels previous task")
    void setScheduledTask_cancelsPreviousTask() throws InterruptedException {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      var executor = Executors.newScheduledThreadPool(1);

      var task1 = executor.schedule(() -> {}, 10, TimeUnit.SECONDS);
      buffer.setScheduledTask(task1);

      var task2 = executor.schedule(() -> {}, 10, TimeUnit.SECONDS);
      buffer.setScheduledTask(task2);

      // Short wait to allow cancellation to propagate
      Thread.sleep(50);
      assertTrue(task1.isCancelled(), "Previous task should be cancelled");

      task2.cancel(false);
      executor.shutdown();
    }

    @Test
    @DisplayName("cancelScheduledTask() cancels task")
    void cancelScheduledTask_cancelsTask() throws InterruptedException {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 10);
      var executor = Executors.newScheduledThreadPool(1);

      var task = executor.schedule(() -> {}, 10, TimeUnit.SECONDS);
      buffer.setScheduledTask(task);

      buffer.cancelScheduledTask();

      Thread.sleep(50);
      assertTrue(task.isCancelled(), "Task should be cancelled");

      executor.shutdown();
    }
  }

  @Nested
  @DisplayName("Thread Safety")
  class ThreadSafetyTests {

    @Test
    @DisplayName("concurrent adds are thread-safe")
    void concurrentAdds_areThreadSafe() throws InterruptedException {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 100);
      int threadCount = 10;
      int messagesPerThread = 5;
      CountDownLatch latch = new CountDownLatch(threadCount);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              for (int j = 0; j < messagesPerThread; j++) {
                buffer.add(
                    MockMessageFactory.createTextMessage(
                        "user123", "Thread " + threadId + " Message " + j));
              }
              latch.countDown();
            });
      }

      assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete");

      assertEquals(threadCount * messagesPerThread, buffer.size());

      executor.shutdown();
    }

    @Test
    @DisplayName("concurrent drain and add are thread-safe")
    void concurrentDrainAndAdd_areThreadSafe() throws InterruptedException {
      UserMessageBuffer buffer = new UserMessageBuffer("user123", 100);
      CountDownLatch latch = new CountDownLatch(2);
      ExecutorService executor = Executors.newFixedThreadPool(2);

      // Thread 1: Keep adding messages
      executor.submit(
          () -> {
            for (int i = 0; i < 50; i++) {
              buffer.add(MockMessageFactory.createTextMessage("user123", "Message " + i));
              try {
                Thread.sleep(1);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
            latch.countDown();
          });

      // Thread 2: Drain periodically
      executor.submit(
          () -> {
            for (int i = 0; i < 10; i++) {
              buffer.drain();
              try {
                Thread.sleep(5);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
            latch.countDown();
          });

      assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");

      // No assertion on size since it depends on timing,
      // but should not throw exceptions
      assertDoesNotThrow(() -> buffer.size());

      executor.shutdown();
    }
  }

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @Test
    @DisplayName("userId() returns correct user ID")
    void userId_returnsCorrectId() {
      UserMessageBuffer buffer = new UserMessageBuffer("user456", 10);

      assertEquals("user456", buffer.userId());
    }
  }
}
