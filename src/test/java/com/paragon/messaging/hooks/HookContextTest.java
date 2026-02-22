package com.paragon.messaging.hooks;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.messaging.testutil.MockMessageFactory;
import com.paragon.messaging.whatsapp.payload.InboundMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link HookContext}. */
@DisplayName("HookContext")
class HookContextTest {

  private List<InboundMessage> messages;

  @BeforeEach
  void setUp() {
    messages =
        List.of(
            MockMessageFactory.createTextMessage("user123", "msg1", "First"),
            MockMessageFactory.createTextMessage("user123", "msg2", "Second"),
            MockMessageFactory.createTextMessage("user123", "msg3", "Third"));
  }

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("create() creates context for first attempt")
    void create_createsFirstAttempt() {
      HookContext context = HookContext.create("user123", messages);

      assertEquals("user123", context.userId());
      assertEquals(3, context.batchSize());
      assertFalse(context.isRetry());
      assertEquals(0, context.retryCount());
      assertTrue(context.isFirstAttempt());
      assertNotNull(context.metadata());
    }

    @Test
    @DisplayName("create() sets batch start time to now")
    void create_setsBatchStartTime() {
      Instant before = Instant.now();
      HookContext context = HookContext.create("user123", messages);
      Instant after = Instant.now();

      assertTrue(context.batchStartTime().isAfter(before.minusSeconds(1)));
      assertTrue(context.batchStartTime().isBefore(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("create() makes messages list immutable")
    void create_messagesImmutable() {
      HookContext context = HookContext.create("user123", messages);

      assertThrows(
          UnsupportedOperationException.class,
          () -> context.messages().add(MockMessageFactory.createTextMessage("user", "test")));
    }

    @Test
    @DisplayName("create() throws for null or blank userId")
    void create_nullUserId_throws() {
      assertThrows(IllegalArgumentException.class, () -> HookContext.create(null, messages));
      assertThrows(IllegalArgumentException.class, () -> HookContext.create("", messages));
      assertThrows(IllegalArgumentException.class, () -> HookContext.create("   ", messages));
    }

    @Test
    @DisplayName("create() throws for null messages")
    void create_nullMessages_throws() {
      assertThrows(IllegalArgumentException.class, () -> HookContext.create("user123", null));
    }

    @Test
    @DisplayName("forRetry() creates retry context")
    void forRetry_createsRetryContext() {
      HookContext original = HookContext.create("user123", messages);

      HookContext retry = HookContext.forRetry(original, 1);

      assertEquals("user123", retry.userId());
      assertEquals(3, retry.batchSize());
      assertTrue(retry.isRetry());
      assertEquals(1, retry.retryCount());
      assertFalse(retry.isFirstAttempt());
    }

    @Test
    @DisplayName("forRetry() reuses metadata from original")
    void forRetry_reusesMetadata() {
      HookContext original = HookContext.create("user123", messages);
      original.putMetadata("key", "value");

      HookContext retry = HookContext.forRetry(original, 1);

      assertEquals("value", retry.getMetadata("key").orElse(null));
    }

    @Test
    @DisplayName("forRetry() creates new batch start time")
    void forRetry_newBatchStartTime() throws InterruptedException {
      HookContext original = HookContext.create("user123", messages);
      Instant originalTime = original.batchStartTime();

      Thread.sleep(10);

      HookContext retry = HookContext.forRetry(original, 1);

      assertTrue(retry.batchStartTime().isAfter(originalTime));
    }
  }

  @Nested
  @DisplayName("Message Access")
  class MessageAccessTests {

    @Test
    @DisplayName("messages() returns all messages")
    void messages_returnsAll() {
      HookContext context = HookContext.create("user123", messages);

      assertEquals(3, context.messages().size());
    }

    @Test
    @DisplayName("firstMessage() returns first message")
    void firstMessage_returnsFirst() {
      HookContext context = HookContext.create("user123", messages);

      InboundMessage first = context.firstMessage();

      assertEquals("msg1", first.id());
    }

    @Test
    @DisplayName("firstMessage() throws for empty batch")
    void firstMessage_emptyBatch_throws() {
      HookContext context = HookContext.create("user123", List.of());

      assertThrows(IllegalStateException.class, () -> context.firstMessage());
    }

    @Test
    @DisplayName("lastMessage() returns last message")
    void lastMessage_returnsLast() {
      HookContext context = HookContext.create("user123", messages);

      InboundMessage last = context.lastMessage();

      assertEquals("msg3", last.id());
    }

    @Test
    @DisplayName("lastMessage() throws for empty batch")
    void lastMessage_emptyBatch_throws() {
      HookContext context = HookContext.create("user123", List.of());

      assertThrows(IllegalStateException.class, () -> context.lastMessage());
    }
  }

  @Nested
  @DisplayName("Metadata Operations")
  class MetadataOperationsTests {

    @Test
    @DisplayName("putMetadata() stores value")
    void putMetadata_storesValue() {
      HookContext context = HookContext.create("user123", messages);

      context.putMetadata("key", "value");

      assertEquals("value", context.getMetadata("key").orElse(null));
    }

    @Test
    @DisplayName("getMetadata() returns empty for missing key")
    void getMetadata_missingKey_returnsEmpty() {
      HookContext context = HookContext.create("user123", messages);

      assertTrue(context.getMetadata("nonexistent").isEmpty());
    }

    @Test
    @DisplayName("getMetadata(Class) returns typed value")
    void getMetadata_typed_returnsValue() {
      HookContext context = HookContext.create("user123", messages);
      context.putMetadata("count", 42);

      assertEquals(42, context.getMetadata("count", Integer.class).orElse(0));
    }

    @Test
    @DisplayName("getMetadata(Class) returns empty for wrong type")
    void getMetadata_wrongType_returnsEmpty() {
      HookContext context = HookContext.create("user123", messages);
      context.putMetadata("key", "string value");

      assertTrue(context.getMetadata("key", Integer.class).isEmpty());
    }

    @Test
    @DisplayName("hasMetadata() returns true for existing key")
    void hasMetadata_existingKey_returnsTrue() {
      HookContext context = HookContext.create("user123", messages);
      context.putMetadata("key", "value");

      assertTrue(context.hasMetadata("key"));
    }

    @Test
    @DisplayName("hasMetadata() returns false for missing key")
    void hasMetadata_missingKey_returnsFalse() {
      HookContext context = HookContext.create("user123", messages);

      assertFalse(context.hasMetadata("nonexistent"));
    }

    @Test
    @DisplayName("metadata is shared between operations")
    void metadata_shared() {
      HookContext context = HookContext.create("user123", messages);

      context.putMetadata("key1", "value1");
      context.putMetadata("key2", 123);
      context.putMetadata("key3", Instant.now());

      assertEquals(3, context.metadata().size());
      assertTrue(context.hasMetadata("key1"));
      assertTrue(context.hasMetadata("key2"));
      assertTrue(context.hasMetadata("key3"));
    }
  }

  @Nested
  @DisplayName("Time Operations")
  class TimeOperationsTests {

    @Test
    @DisplayName("elapsedTime() returns duration since start")
    void elapsedTime_returnsDuration() throws InterruptedException {
      HookContext context = HookContext.create("user123", messages);

      Thread.sleep(100);

      Duration elapsed = context.elapsedTime();

      assertTrue(elapsed.toMillis() >= 100, "Should have elapsed at least 100ms");
    }

    @Test
    @DisplayName("elapsedTime() increases over time")
    void elapsedTime_increases() throws InterruptedException {
      HookContext context = HookContext.create("user123", messages);

      Thread.sleep(50);
      Duration elapsed1 = context.elapsedTime();

      Thread.sleep(50);
      Duration elapsed2 = context.elapsedTime();

      assertTrue(elapsed2.compareTo(elapsed1) > 0);
    }
  }

  @Nested
  @DisplayName("Retry Operations")
  class RetryOperationsTests {

    @Test
    @DisplayName("isFirstAttempt() is true for retryCount 0")
    void isFirstAttempt_retryCount0_returnsTrue() {
      HookContext context = HookContext.create("user123", messages);

      assertTrue(context.isFirstAttempt());
    }

    @Test
    @DisplayName("isFirstAttempt() is false for retryCount > 0")
    void isFirstAttempt_retryCountPositive_returnsFalse() {
      HookContext original = HookContext.create("user123", messages);
      HookContext retry = HookContext.forRetry(original, 1);

      assertFalse(retry.isFirstAttempt());
    }

    @Test
    @DisplayName("retryCount increments correctly")
    void retryCount_increments() {
      HookContext original = HookContext.create("user123", messages);
      HookContext retry1 = HookContext.forRetry(original, 1);
      HookContext retry2 = HookContext.forRetry(original, 2);
      HookContext retry3 = HookContext.forRetry(original, 3);

      assertEquals(0, original.retryCount());
      assertEquals(1, retry1.retryCount());
      assertEquals(2, retry2.retryCount());
      assertEquals(3, retry3.retryCount());
    }
  }

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @Test
    @DisplayName("userId() returns correct user ID")
    void userId_returns() {
      HookContext context = HookContext.create("user456", messages);

      assertEquals("user456", context.userId());
    }

    @Test
    @DisplayName("batchSize() returns message count")
    void batchSize_returns() {
      HookContext context = HookContext.create("user123", messages);

      assertEquals(3, context.batchSize());
    }

    @Test
    @DisplayName("isRetry() reflects retry status")
    void isRetry_reflects() {
      HookContext original = HookContext.create("user123", messages);
      HookContext retry = HookContext.forRetry(original, 1);

      assertFalse(original.isRetry());
      assertTrue(retry.isRetry());
    }

    @Test
    @DisplayName("batchStartTime() is not null")
    void batchStartTime_notNull() {
      HookContext context = HookContext.create("user123", messages);

      assertNotNull(context.batchStartTime());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("empty message list is valid")
    void emptyMessageList_valid() {
      HookContext context = HookContext.create("user123", List.of());

      assertEquals(0, context.batchSize());
      assertTrue(context.messages().isEmpty());
    }

    @Test
    @DisplayName("single message is valid")
    void singleMessage_valid() {
      List<InboundMessage> single =
          List.of(MockMessageFactory.createTextMessage("user123", "Only one"));

      HookContext context = HookContext.create("user123", single);

      assertEquals(1, context.batchSize());
      assertEquals(context.firstMessage(), context.lastMessage());
    }

    @Test
    @DisplayName("metadata null handling in constructor")
    void metadata_nullInConstructor_createsNew() {
      HookContext context =
          new HookContext("user123", messages, Instant.now(), messages.size(), false, 0, null);

      assertNotNull(context.metadata());
      assertTrue(context.metadata().isEmpty());
    }
  }

  @Nested
  @DisplayName("Validation")
  class ValidationTests {

    @Test
    @DisplayName("constructor validates userId")
    void constructor_validatesUserId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new HookContext(null, messages, Instant.now(), 3, false, 0, null));

      assertThrows(
          IllegalArgumentException.class,
          () -> new HookContext("", messages, Instant.now(), 3, false, 0, null));
    }

    @Test
    @DisplayName("constructor validates messages not null")
    void constructor_validatesMessages() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new HookContext("user123", null, Instant.now(), 3, false, 0, null));
    }

    @Test
    @DisplayName("constructor validates batchStartTime not null")
    void constructor_validatesBatchStartTime() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new HookContext("user123", messages, null, 3, false, 0, null));
    }

    @Test
    @DisplayName("constructor validates batchSize non-negative")
    void constructor_validatesBatchSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new HookContext("user123", messages, Instant.now(), -1, false, 0, null));
    }

    @Test
    @DisplayName("constructor validates retryCount non-negative")
    void constructor_validatesRetryCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new HookContext("user123", messages, Instant.now(), 3, false, -1, null));
    }
  }
}
