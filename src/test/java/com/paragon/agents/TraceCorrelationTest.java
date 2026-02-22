package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.telemetry.TelemetryContext;
import com.paragon.telemetry.processors.TraceIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for trace correlation across multi-agent runs.
 *
 * <p>Verifies that traceId and spanId are properly propagated through AgentContext,
 * TelemetryContext, and across handoffs.
 */
class TraceCorrelationTest {

  @Test
  @DisplayName("AgentContext should auto-init trace context if not set")
  void agentContext_shouldInitTraceIfNotSet() {
    AgenticContext ctx = AgenticContext.create();
    assertFalse(ctx.hasTraceContext(), "New context should not have trace");

    // Calling withTraceContext should set it
    ctx.withTraceContext("abc123abc123abc123abc123abc123ab", "def456def456def4");

    assertTrue(ctx.hasTraceContext());
    assertEquals("abc123abc123abc123abc123abc123ab", ctx.parentTraceId().orElse(null));
    assertEquals("def456def456def4", ctx.parentSpanId().orElse(null));
  }

  @Test
  @DisplayName("AgentContext.fork should create child with same traceId but new parentSpanId")
  void agentContext_forkShouldPreserveTraceId() {
    AgenticContext parent =
        AgenticContext.create()
            .withTraceContext("aaaa1111aaaa1111aaaa1111aaaa1111", "bbbb2222bbbb2222")
            .withRequestId("req-001");
    parent.setState("key", "value");

    AgenticContext child = parent.fork("cccc3333cccc3333");

    // TraceId should be preserved
    assertEquals("aaaa1111aaaa1111aaaa1111aaaa1111", child.parentTraceId().orElse(null));
    // ParentSpanId should be the new one
    assertEquals("cccc3333cccc3333", child.parentSpanId().orElse(null));
    // RequestId should be preserved
    assertEquals("req-001", child.requestId().orElse(null));
    // State should be copied
    assertEquals("value", child.getState("key").orElse(null));
    // Turn count should be reset
    assertEquals(0, child.getTurnCount());
  }

  @Test
  @DisplayName("AgentContext.copy should preserve all trace context")
  void agentContext_copyShouldPreserveTraceContext() {
    AgenticContext original =
        AgenticContext.create()
            .withTraceContext("aaaa1111aaaa1111aaaa1111aaaa1111", "bbbb2222bbbb2222")
            .withRequestId("req-001");

    AgenticContext copy = original.copy();

    assertEquals(original.parentTraceId(), copy.parentTraceId());
    assertEquals(original.parentSpanId(), copy.parentSpanId());
    assertEquals(original.requestId(), copy.requestId());
  }

  @Test
  @DisplayName("TelemetryContext should support parent trace fields")
  void telemetryContext_shouldSupportParentTrace() {
    TelemetryContext ctx =
        TelemetryContext.builder()
            .userId("user-123")
            .traceName("test-operation")
            .parentTraceId("aaaa1111aaaa1111aaaa1111aaaa1111")
            .parentSpanId("bbbb2222bbbb2222")
            .requestId("req-001")
            .addTag("test")
            .build();

    assertEquals("user-123", ctx.userId());
    assertEquals("test-operation", ctx.traceName());
    assertEquals("aaaa1111aaaa1111aaaa1111aaaa1111", ctx.parentTraceId());
    assertEquals("bbbb2222bbbb2222", ctx.parentSpanId());
    assertEquals("req-001", ctx.requestId());
  }

  @Test
  @DisplayName("TelemetryContext.toAttributes should include requestId")
  void telemetryContext_toAttributesShouldIncludeRequestId() {
    TelemetryContext ctx = TelemetryContext.builder().requestId("req-abc-123").build();

    var attrs = ctx.toAttributes();

    assertEquals("req-abc-123", attrs.get("request.id"));
  }

  @Test
  @DisplayName("TraceIdGenerator should generate valid IDs")
  void traceIdGenerator_shouldGenerateValidIds() {
    String traceId = TraceIdGenerator.generateTraceId();
    String spanId = TraceIdGenerator.generateSpanId();

    assertTrue(TraceIdGenerator.isValidTraceId(traceId), "Generated traceId should be valid");
    assertTrue(TraceIdGenerator.isValidSpanId(spanId), "Generated spanId should be valid");

    assertEquals(32, traceId.length(), "TraceId should be 32 chars");
    assertEquals(16, spanId.length(), "SpanId should be 16 chars");
  }

  @Test
  @DisplayName("TelemetryContext.empty should have null trace fields")
  void telemetryContext_emptyShouldHaveNullFields() {
    TelemetryContext empty = TelemetryContext.empty();

    assertNull(empty.parentTraceId());
    assertNull(empty.parentSpanId());
    assertNull(empty.requestId());
    assertNull(empty.userId());
    assertNull(empty.traceName());
  }

  @Test
  @DisplayName("TelemetryContext.forUser should have null trace fields")
  void telemetryContext_forUserShouldHaveNullTraceFields() {
    TelemetryContext ctx = TelemetryContext.forUser("user-456");

    assertEquals("user-456", ctx.userId());
    assertNull(ctx.parentTraceId());
    assertNull(ctx.parentSpanId());
    assertNull(ctx.requestId());
  }

  @Test
  @DisplayName("AgentContext should track requestId separately from trace context")
  void agentContext_requestIdShouldBeIndependent() {
    AgenticContext ctx = AgenticContext.create().withRequestId("my-request-123");

    assertFalse(ctx.hasTraceContext(), "RequestId alone should not mean hasTraceContext");
    assertEquals("my-request-123", ctx.requestId().orElse(null));
  }
}
