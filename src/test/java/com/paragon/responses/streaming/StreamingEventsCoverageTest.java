package com.paragon.responses.streaming;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Coverage tests for streaming event types. Tests record construction and polymorphic dispatch for
 * events with simple constructors.
 *
 * <p>Note: Events requiring Response objects (ResponseCreatedEvent, ResponseQueuedEvent, etc.) are
 * tested via integration tests.
 */
@DisplayName("Streaming Events Coverage Tests")
class StreamingEventsCoverageTest {

  // =========================================================================
  // Text Streaming Events
  // =========================================================================
  @Nested
  @DisplayName("Text Streaming Events")
  class TextStreamingTests {

    @Test
    @DisplayName("OutputTextDeltaEvent should have correct fields")
    void outputTextDeltaEventShouldHaveCorrectFields() {
      OutputTextDeltaEvent event =
          new OutputTextDeltaEvent("response.output_text.delta", "item-1", 0, 0, "Hello", null, 5);
      assertEquals("response.output_text.delta", event.type());
      assertEquals("item-1", event.itemId());
      assertEquals(0, event.outputIndex());
      assertEquals(0, event.contentIndex());
      assertEquals("Hello", event.delta());
      assertEquals(5, event.sequenceNumber());
    }

    @Test
    @DisplayName("OutputTextDeltaEvent should support logprobs")
    void outputTextDeltaEventShouldSupportLogprobs() {
      List<Object> logprobs = List.of("prob1", "prob2");
      OutputTextDeltaEvent event =
          new OutputTextDeltaEvent(
              "response.output_text.delta", "item-1", 0, 0, "World", logprobs, 6);
      assertEquals(logprobs, event.logprobs());
    }

    @Test
    @DisplayName("OutputTextDoneEvent should have correct fields")
    void outputTextDoneEventShouldHaveCorrectFields() {
      OutputTextDoneEvent event =
          new OutputTextDoneEvent(
              "response.output_text.done", "item-1", 0, 0, "Complete text", null, 13);
      assertEquals("response.output_text.done", event.type());
      assertEquals("Complete text", event.text());
      assertEquals(13, event.sequenceNumber());
    }
  }

  // =========================================================================
  // Error Event
  // =========================================================================
  @Nested
  @DisplayName("Error Event")
  class ErrorEventTests {

    @Test
    @DisplayName("StreamingErrorEvent should have correct fields")
    void streamingErrorEventShouldHaveCorrectFields() {
      StreamingErrorEvent event =
          new StreamingErrorEvent("error", "rate_limit_exceeded", "Rate limit exceeded", null, 0);
      assertEquals("error", event.type());
      assertEquals("rate_limit_exceeded", event.code());
      assertEquals("Rate limit exceeded", event.message());
      assertNull(event.param());
    }

    @Test
    @DisplayName("StreamingErrorEvent should support null code and message")
    void streamingErrorEventShouldSupportNulls() {
      StreamingErrorEvent event = new StreamingErrorEvent("error", null, null, null, 0);
      assertNull(event.code());
      assertNull(event.message());
    }

    @Test
    @DisplayName("StreamingErrorEvent should support param field")
    void streamingErrorEventShouldSupportParam() {
      StreamingErrorEvent event =
          new StreamingErrorEvent("error", "invalid_param", "Invalid parameter", "model", 0);
      assertEquals("model", event.param());
    }
  }

  // =========================================================================
  // Polymorphic Interface Tests
  // =========================================================================
  @Nested
  @DisplayName("Polymorphic Interface Tests")
  class PolymorphicTests {

    @Test
    @DisplayName("Events should implement StreamingEvent")
    void eventsShouldImplementStreamingEvent() {
      StreamingEvent event1 =
          new OutputTextDeltaEvent("response.output_text.delta", "id", 0, 0, "text", null, 2);
      StreamingEvent event2 = new StreamingErrorEvent("error", "code", "msg", null, 3);

      assertInstanceOf(StreamingEvent.class, event1);
      assertInstanceOf(StreamingEvent.class, event2);
    }

    @Test
    @DisplayName("Events should support pattern matching")
    void eventsShouldSupportPatternMatching() {
      StreamingEvent event =
          new OutputTextDeltaEvent("response.output_text.delta", "id", 0, 0, "Hello", null, 1);

      String result =
          switch (event) {
            case OutputTextDeltaEvent e -> "Delta: " + e.delta();
            case OutputTextDoneEvent e -> "Done: " + e.text();
            case StreamingErrorEvent e -> "Error: " + e.message();
            default -> "Unknown";
          };

      assertEquals("Delta: Hello", result);
    }
  }

  // =========================================================================
  // Edge Cases
  // =========================================================================
  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle empty delta text")
    void shouldHandleEmptyDeltaText() {
      OutputTextDeltaEvent event =
          new OutputTextDeltaEvent("response.output_text.delta", "id", 0, 0, "", null, 1);
      assertEquals("", event.delta());
    }

    @Test
    @DisplayName("Should handle unicode in delta text")
    void shouldHandleUnicodeInDeltaText() {
      String unicode = "Hello 世界 🌍 سلام";
      OutputTextDeltaEvent event =
          new OutputTextDeltaEvent("response.output_text.delta", "id", 0, 0, unicode, null, 1);
      assertEquals(unicode, event.delta());
    }

    @Test
    @DisplayName("Should handle large sequence numbers")
    void shouldHandleLargeSequenceNumbers() {
      OutputTextDeltaEvent event =
          new OutputTextDeltaEvent(
              "response.output_text.delta", "id", 0, 0, "text", null, Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, event.sequenceNumber());
    }

    @Test
    @DisplayName("Should handle zero sequence numbers")
    void shouldHandleZeroSequenceNumbers() {
      StreamingErrorEvent event = new StreamingErrorEvent("error", null, null, null, 0);
      assertEquals(0, event.sequenceNumber());
    }
  }
}
