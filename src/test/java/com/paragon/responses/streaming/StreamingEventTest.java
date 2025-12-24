package com.paragon.responses.streaming;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for StreamingEvent implementations.
 */
@DisplayName("StreamingEvent Tests")
class StreamingEventTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // OUTPUT TEXT DELTA EVENT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("OutputTextDeltaEvent")
  class OutputTextDeltaEventTests {

    @Test
    @DisplayName("creates output text delta event")
    void createsOutputTextDelta() {
      OutputTextDeltaEvent event = new OutputTextDeltaEvent(
          "response.output_text.delta",
          "item_123",
          0,
          0,
          "Hello",
          null,
          1
      );

      assertEquals("response.output_text.delta", event.type());
      assertEquals("item_123", event.itemId());
      assertEquals(0, event.outputIndex());
      assertEquals("Hello", event.delta());
      assertEquals(1, event.sequenceNumber());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STREAMING ERROR EVENT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("StreamingErrorEvent")
  class StreamingErrorEventTests {

    @Test
    @DisplayName("creates streaming error event")
    void createsStreamingError() {
      StreamingErrorEvent event = new StreamingErrorEvent(
          "error",
          "server_error",
          "Internal server error",
          null,  // param
          1
      );

      assertEquals("error", event.type());
      assertEquals("server_error", event.code());
      assertEquals("Internal server error", event.message());
      assertEquals(1, event.sequenceNumber());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FUNCTION CALL ARGUMENTS DELTA EVENT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionCallArgumentsDeltaEvent")
  class FunctionCallArgumentsDeltaEventTests {

    @Test
    @DisplayName("creates function call arguments delta")
    void createsFunctionCallDelta() {
      FunctionCallArgumentsDeltaEvent event = new FunctionCallArgumentsDeltaEvent(
          "response.function_call_arguments.delta",
          "item_456",
          0,
          "{\"city\":",
          1
      );

      assertEquals("response.function_call_arguments.delta", event.type());
      assertEquals("{\"city\":", event.delta());
      assertEquals("item_456", event.itemId());
    }
  }
}
