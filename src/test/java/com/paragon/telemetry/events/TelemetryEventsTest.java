package com.paragon.telemetry.events;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.telemetry.TelemetryContext;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for telemetry event classes. */
@DisplayName("Telemetry Events")
class TelemetryEventsTest {

  private static final String SESSION_ID = "test-session";
  private static final String TRACE_ID = "trace-123";
  private static final String SPAN_ID = "span-456";
  private static final String MODEL = "gpt-4o";

  // ==================== ResponseStartedEvent Tests ====================

  @Nested
  @DisplayName("ResponseStartedEvent")
  class ResponseStartedEventTests {

    @Test
    @DisplayName("create creates minimal event")
    void createCreatesMinimalEvent() {
      ResponseStartedEvent event =
          ResponseStartedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, MODEL);

      assertEquals(SESSION_ID, event.sessionId());
      assertEquals(TRACE_ID, event.traceId());
      assertEquals(SPAN_ID, event.spanId());
      assertNull(event.parentSpanId());
      assertEquals(MODEL, event.model());
      assertTrue(event.timestampNanos() > 0);
      assertNotNull(event.attributes());
      assertTrue(event.attributes().isEmpty());
    }

    @Test
    @DisplayName("create with TelemetryContext includes attributes")
    void createWithContextIncludesAttributes() {
      TelemetryContext context =
          TelemetryContext.builder().addMetadata("user", "test-user").build();

      ResponseStartedEvent event =
          ResponseStartedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, MODEL, context);

      assertEquals(SESSION_ID, event.sessionId());
      assertNotNull(event.attributes());
    }

    @Test
    @DisplayName("createWithParent sets parent span ID")
    void createWithParentSetsParentSpanId() {
      String parentSpanId = "parent-span-789";
      ResponseStartedEvent event =
          ResponseStartedEvent.createWithParent(
              SESSION_ID, TRACE_ID, SPAN_ID, parentSpanId, MODEL);

      assertEquals(SESSION_ID, event.sessionId());
      assertEquals(parentSpanId, event.parentSpanId());
      assertEquals(MODEL, event.model());
    }

    @Test
    @DisplayName("implements TelemetryEvent")
    void implementsTelemetryEvent() {
      ResponseStartedEvent event =
          ResponseStartedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, MODEL);

      assertInstanceOf(TelemetryEvent.class, event);
    }
  }

  // ==================== ResponseCompletedEvent Tests ====================

  @Nested
  @DisplayName("ResponseCompletedEvent")
  class ResponseCompletedEventTests {

    @Test
    @DisplayName("create creates minimal event")
    void createCreatesMinimalEvent() {
      long startTime = System.nanoTime();
      ResponseCompletedEvent event =
          ResponseCompletedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, startTime, MODEL);

      assertEquals(SESSION_ID, event.sessionId());
      assertEquals(TRACE_ID, event.traceId());
      assertEquals(SPAN_ID, event.spanId());
      assertNull(event.parentSpanId());
      assertEquals(MODEL, event.model());
      assertEquals(startTime, event.startTimestampNanos());
      assertNull(event.inputTokens());
      assertNull(event.outputTokens());
      assertNull(event.totalTokens());
      assertNull(event.costUsd());
    }

    @Test
    @DisplayName("from creates event from started event")
    void fromCreatesEventFromStartedEvent() {
      ResponseStartedEvent startedEvent =
          ResponseStartedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, MODEL);

      ResponseCompletedEvent completedEvent =
          ResponseCompletedEvent.from(startedEvent, 100, 200, 300, 0.01);

      assertEquals(startedEvent.sessionId(), completedEvent.sessionId());
      assertEquals(startedEvent.traceId(), completedEvent.traceId());
      assertEquals(startedEvent.spanId(), completedEvent.spanId());
      assertEquals(startedEvent.model(), completedEvent.model());
      assertEquals(100, completedEvent.inputTokens());
      assertEquals(200, completedEvent.outputTokens());
      assertEquals(300, completedEvent.totalTokens());
      assertEquals(0.01, completedEvent.costUsd());
    }

    @Test
    @DisplayName("durationNanos calculates correctly")
    void durationNanosCalculatesCorrectly() {
      long startNanos = 1_000_000_000L;
      long endNanos = 2_500_000_000L;
      ResponseCompletedEvent event =
          new ResponseCompletedEvent(
              SESSION_ID,
              TRACE_ID,
              SPAN_ID,
              null,
              endNanos,
              startNanos,
              MODEL,
              null,
              null,
              null,
              null,
              Map.of());

      assertEquals(1_500_000_000L, event.durationNanos());
    }

    @Test
    @DisplayName("durationMs converts correctly")
    void durationMsConvertsCorrectly() {
      long startNanos = 1_000_000_000L;
      long endNanos = 2_500_000_000L;
      ResponseCompletedEvent event =
          new ResponseCompletedEvent(
              SESSION_ID,
              TRACE_ID,
              SPAN_ID,
              null,
              endNanos,
              startNanos,
              MODEL,
              null,
              null,
              null,
              null,
              Map.of());

      assertEquals(1500L, event.durationMs());
    }

    @Test
    @DisplayName("implements TelemetryEvent")
    void implementsTelemetryEvent() {
      ResponseCompletedEvent event =
          ResponseCompletedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, 0, MODEL);

      assertInstanceOf(TelemetryEvent.class, event);
    }
  }

  // ==================== ResponseFailedEvent Tests ====================

  @Nested
  @DisplayName("ResponseFailedEvent")
  class ResponseFailedEventTests {

    @Test
    @DisplayName("from creates event from started event and exception")
    void fromCreatesEventFromStartedEventAndException() {
      ResponseStartedEvent startedEvent =
          ResponseStartedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, MODEL);
      RuntimeException error = new RuntimeException("Something went wrong");

      ResponseFailedEvent failedEvent = ResponseFailedEvent.from(startedEvent, error);

      assertEquals(startedEvent.sessionId(), failedEvent.sessionId());
      assertEquals(startedEvent.traceId(), failedEvent.traceId());
      assertTrue(failedEvent.errorType().contains("RuntimeException"));
      assertEquals("Something went wrong", failedEvent.errorMessage());
    }

    @Test
    @DisplayName("fromHttpError creates event with HTTP status code")
    void fromHttpErrorCreatesEventWithStatusCode() {
      ResponseStartedEvent startedEvent =
          ResponseStartedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, MODEL);

      ResponseFailedEvent failedEvent =
          ResponseFailedEvent.fromHttpError(startedEvent, 500, "Internal Server Error");

      assertEquals(500, failedEvent.httpStatusCode());
      assertEquals("HttpError", failedEvent.errorType());
      assertEquals("Internal Server Error", failedEvent.errorMessage());
    }

    @Test
    @DisplayName("durationNanos calculates correctly")
    void durationNanosCalculatesCorrectly() {
      long startNanos = 1_000_000_000L;
      long endNanos = 2_000_000_000L;
      ResponseFailedEvent event =
          new ResponseFailedEvent(
              SESSION_ID,
              TRACE_ID,
              SPAN_ID,
              null,
              endNanos,
              startNanos,
              MODEL,
              "Error",
              "message",
              null,
              null,
              Map.of());

      assertEquals(1_000_000_000L, event.durationNanos());
    }

    @Test
    @DisplayName("durationMs converts correctly")
    void durationMsConvertsCorrectly() {
      long startNanos = 1_000_000_000L;
      long endNanos = 2_000_000_000L;
      ResponseFailedEvent event =
          new ResponseFailedEvent(
              SESSION_ID,
              TRACE_ID,
              SPAN_ID,
              null,
              endNanos,
              startNanos,
              MODEL,
              "Error",
              "message",
              null,
              null,
              Map.of());

      assertEquals(1000L, event.durationMs());
    }

    @Test
    @DisplayName("implements TelemetryEvent")
    void implementsTelemetryEvent() {
      ResponseStartedEvent startedEvent =
          ResponseStartedEvent.create(SESSION_ID, TRACE_ID, SPAN_ID, MODEL);
      ResponseFailedEvent failedEvent =
          ResponseFailedEvent.from(startedEvent, new RuntimeException("test"));

      assertInstanceOf(TelemetryEvent.class, failedEvent);
    }
  }

  // ==================== AgentFailedEvent Tests ====================

  @Nested
  @DisplayName("AgentFailedEvent")
  class AgentFailedEventTests {

    @Test
    @DisplayName("from creates event from agent name and exception")
    void fromCreatesEventFromAgentNameAndException() {
      RuntimeException error = new RuntimeException("Agent failed");

      AgentFailedEvent event =
          AgentFailedEvent.from(
              "TestAgent", 
              3, 
              error, 
              SESSION_ID, 
              TRACE_ID, 
              SPAN_ID, 
              null);

      assertEquals("TestAgent", event.agentName());
      assertEquals(SESSION_ID, event.sessionId());
      assertEquals(TRACE_ID, event.traceId());
      assertEquals(3, event.turnsCompleted());
      assertEquals("Agent failed", event.errorMessage());
    }

    @Test
    @DisplayName("attributes returns expected map")
    void attributesReturnsExpectedMap() {
      AgentFailedEvent event =
          new AgentFailedEvent(
              "MyAgent",
              SESSION_ID,
              TRACE_ID,
              SPAN_ID,
              null,
              System.nanoTime(),
              "LLM_CALL",
              "API_ERROR",
              "API call failed",
              5,
              "Retry the request");

      Map<String, Object> attrs = event.attributes();

      assertEquals("MyAgent", attrs.get("agent.name"));
      assertEquals("LLM_CALL", attrs.get("agent.phase"));
      assertEquals(5, attrs.get("agent.turns_completed"));
      assertEquals("API_ERROR", attrs.get("error.code"));
      assertEquals("API call failed", attrs.get("error.message"));
      assertEquals("Retry the request", attrs.get("error.suggestion"));
    }

    @Test
    @DisplayName("attributes excludes null suggestion")
    void attributesExcludesNullSuggestion() {
      AgentFailedEvent event =
          new AgentFailedEvent(
              "MyAgent",
              SESSION_ID,
              TRACE_ID,
              SPAN_ID,
              null,
              System.nanoTime(),
              "PHASE",
              "CODE",
              "message",
              0,
              null);

      Map<String, Object> attrs = event.attributes();
      assertFalse(attrs.containsKey("error.suggestion"));
    }

    @Test
    @DisplayName("implements TelemetryEvent")
    void implementsTelemetryEvent() {
      AgentFailedEvent event =
          AgentFailedEvent.from("Agent", 0, new RuntimeException(), SESSION_ID, TRACE_ID, null, null);

      assertInstanceOf(TelemetryEvent.class, event);
    }
  }
}
