package com.paragon.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link StreamConsumer}.
 *
 * <p>Tests cover: factory method, interface methods, and default method behavior.
 */
@DisplayName("StreamConsumer Tests")
class StreamConsumerTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FACTORY METHOD
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Method")
  class FactoryMethodTests {

    @Test
    @DisplayName("of() creates consumer with correct eventType")
    void ofCreatesConsumerWithCorrectEventType() {
      StreamConsumer<String> consumer =
          StreamConsumer.of(String.class, e -> {}, ex -> {}, () -> {});

      assertEquals(String.class, consumer.eventType());
    }

    @Test
    @DisplayName("of() creates consumer that calls onEvent")
    void ofCreatesConsumerThatCallsOnEvent() {
      AtomicReference<String> received = new AtomicReference<>();

      StreamConsumer<String> consumer =
          StreamConsumer.of(String.class, received::set, ex -> {}, () -> {});

      consumer.onEvent("test message");

      assertEquals("test message", received.get());
    }

    @Test
    @DisplayName("of() creates consumer that calls onError")
    void ofCreatesConsumerThatCallsOnError() {
      AtomicReference<Throwable> received = new AtomicReference<>();

      StreamConsumer<String> consumer =
          StreamConsumer.of(String.class, e -> {}, received::set, () -> {});

      RuntimeException error = new RuntimeException("test error");
      consumer.onError(error);

      assertEquals(error, received.get());
    }

    @Test
    @DisplayName("of() creates consumer that calls onComplete")
    void ofCreatesConsumerThatCallsOnComplete() {
      AtomicBoolean completed = new AtomicBoolean(false);

      StreamConsumer<String> consumer =
          StreamConsumer.of(String.class, e -> {}, ex -> {}, () -> completed.set(true));

      consumer.onComplete();

      assertTrue(completed.get());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INTERFACE IMPLEMENTATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Interface Implementation")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("custom implementation can provide eventType")
    void customImplementationCanProvideEventType() {
      StreamConsumer<Integer> consumer =
          new StreamConsumer<>() {
            @Override
            public Class<Integer> eventType() {
              return Integer.class;
            }

            @Override
            public void onEvent(Integer event) {}

            @Override
            public void onError(Throwable error) {}

            @Override
            public void onComplete() {}
          };

      assertEquals(Integer.class, consumer.eventType());
    }

    @Test
    @DisplayName("custom implementation receives events")
    void customImplementationReceivesEvents() {
      List<Integer> events = new ArrayList<>();

      StreamConsumer<Integer> consumer =
          new StreamConsumer<>() {
            @Override
            public Class<Integer> eventType() {
              return Integer.class;
            }

            @Override
            public void onEvent(Integer event) {
              events.add(event);
            }

            @Override
            public void onError(Throwable error) {}

            @Override
            public void onComplete() {}
          };

      consumer.onEvent(1);
      consumer.onEvent(2);
      consumer.onEvent(3);

      assertEquals(List.of(1, 2, 3), events);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DEFAULT METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Default Methods")
  class DefaultMethodTests {

    @Test
    @DisplayName("onRawLine default implementation does nothing")
    void onRawLineDefaultDoesNothing() {
      StreamConsumer<String> consumer =
          StreamConsumer.of(String.class, e -> {}, ex -> {}, () -> {});

      // Should not throw
      assertDoesNotThrow(() -> consumer.onRawLine("data: test"));
    }

    @Test
    @DisplayName("shouldCancel default returns false")
    void shouldCancelDefaultReturnsFalse() {
      StreamConsumer<String> consumer =
          StreamConsumer.of(String.class, e -> {}, ex -> {}, () -> {});

      assertFalse(consumer.shouldCancel());
    }

    @Test
    @DisplayName("custom implementation can override onRawLine")
    void customImplementationCanOverrideOnRawLine() {
      List<String> rawLines = new ArrayList<>();

      StreamConsumer<String> consumer =
          new StreamConsumer<>() {
            @Override
            public Class<String> eventType() {
              return String.class;
            }

            @Override
            public void onEvent(String event) {}

            @Override
            public void onError(Throwable error) {}

            @Override
            public void onComplete() {}

            @Override
            public void onRawLine(String line) {
              rawLines.add(line);
            }
          };

      consumer.onRawLine("data: test1");
      consumer.onRawLine("data: test2");

      assertEquals(List.of("data: test1", "data: test2"), rawLines);
    }

    @Test
    @DisplayName("custom implementation can override shouldCancel")
    void customImplementationCanOverrideShouldCancel() {
      AtomicInteger eventCount = new AtomicInteger(0);

      StreamConsumer<String> consumer =
          new StreamConsumer<>() {
            @Override
            public Class<String> eventType() {
              return String.class;
            }

            @Override
            public void onEvent(String event) {
              eventCount.incrementAndGet();
            }

            @Override
            public void onError(Throwable error) {}

            @Override
            public void onComplete() {}

            @Override
            public boolean shouldCancel() {
              return eventCount.get() >= 5;
            }
          };

      // Process events
      for (int i = 0; i < 10; i++) {
        if (!consumer.shouldCancel()) {
          consumer.onEvent("event-" + i);
        }
      }

      assertEquals(5, eventCount.get());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GENERIC TYPE HANDLING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Generic Type Handling")
  class GenericTypeHandlingTests {

    @Test
    @DisplayName("works with record types")
    void worksWithRecordTypes() {
      record TestEvent(String message, int code) {}

      AtomicReference<TestEvent> received = new AtomicReference<>();

      StreamConsumer<TestEvent> consumer =
          StreamConsumer.of(TestEvent.class, received::set, ex -> {}, () -> {});

      TestEvent event = new TestEvent("hello", 200);
      consumer.onEvent(event);

      assertEquals(event, received.get());
    }

    @Test
    @DisplayName("works with complex types")
    void worksWithComplexTypes() {
      List<List<String>> received = new ArrayList<>();

      @SuppressWarnings("unchecked")
      StreamConsumer<List<String>> consumer =
          StreamConsumer.of(
              (Class<List<String>>) (Class<?>) List.class, received::add, ex -> {}, () -> {});

      consumer.onEvent(List.of("a", "b", "c"));
      consumer.onEvent(List.of("x", "y"));

      assertEquals(2, received.size());
      assertEquals(List.of("a", "b", "c"), received.get(0));
      assertEquals(List.of("x", "y"), received.get(1));
    }
  }
}
