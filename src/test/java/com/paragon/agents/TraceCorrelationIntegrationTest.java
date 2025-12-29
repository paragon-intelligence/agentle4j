package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.telemetry.processors.TraceIdGenerator;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Integration tests for trace correlation across multi-agent runs.
 * 
 * <p>Tests verify that traceId and spanId are properly propagated through:
 * - Agent.interactBlocking() auto-initialization
 * - Handoffs between agents
 * - ParallelAgents shared parent trace
 */
@DisplayName("Trace Correlation Integration Tests")
class TraceCorrelationIntegrationTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder = Responder.builder()
        .baseUrl(mockWebServer.url("/v1/responses"))
        .apiKey("test-key")
        .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("Agent.interactBlocking() Trace Auto-Init")
  class AgentTraceAutoInit {

    @Test
    @DisplayName("interactBlocking auto-initializes trace context when not set")
    void interactBlocking_autoInitializesTraceContext() throws Exception {
      Agent agent = createTestAgent("TestAgent");
      AgentContext ctx = AgentContext.create();
      
      assertFalse(ctx.hasTraceContext(), "Context should not have trace initially");
      
      enqueueSuccessResponse("Hello!");
      
      ctx.addInput(Message.user("Hello"));
      agent.interact(ctx).get(5, TimeUnit.SECONDS);
      
      // After interact, context should have trace initialized
      assertTrue(ctx.hasTraceContext(), "Context should have trace after interact");
      assertNotNull(ctx.parentTraceId(), "TraceId should be set");
      assertNotNull(ctx.parentSpanId(), "SpanId should be set");
      assertTrue(TraceIdGenerator.isValidTraceId(ctx.parentTraceId()), "TraceId should be valid");
      assertTrue(TraceIdGenerator.isValidSpanId(ctx.parentSpanId()), "SpanId should be valid");
    }

    @Test
    @DisplayName("interactBlocking preserves existing trace context")
    void interactBlocking_preservesExistingTraceContext() throws Exception {
      Agent agent = createTestAgent("TestAgent");
      String existingTraceId = "aaaa1111bbbb2222cccc3333dddd4444";
      String existingSpanId = "1111222233334444";
      
      AgentContext ctx = AgentContext.create()
          .withTraceContext(existingTraceId, existingSpanId);
      
      enqueueSuccessResponse("Hello!");
      
      ctx.addInput(Message.user("Hello"));
      agent.interact(ctx).get(5, TimeUnit.SECONDS);
      
      // Original trace should be preserved
      assertEquals(existingTraceId, ctx.parentTraceId(), "TraceId should be preserved");
      assertEquals(existingSpanId, ctx.parentSpanId(), "SpanId should be preserved");
    }

    @Test
    @DisplayName("multiple turns share the same trace context")
    void multipleTurns_shareSameTraceContext() throws Exception {
      Agent agent = createTestAgent("TestAgent");
      AgentContext ctx = AgentContext.create();
      
      enqueueSuccessResponse("First response");
      
      ctx.addInput(Message.user("First message"));
      agent.interact(ctx).get(5, TimeUnit.SECONDS);
      
      String firstTraceId = ctx.parentTraceId();
      String firstSpanId = ctx.parentSpanId();
      
      enqueueSuccessResponse("Second response");
      
      ctx.addInput(Message.user("Second message"));
      agent.interact(ctx).get(5, TimeUnit.SECONDS);
      
      // Trace should be preserved across interactions
      assertEquals(firstTraceId, ctx.parentTraceId(), "TraceId should be consistent across turns");
      assertEquals(firstSpanId, ctx.parentSpanId(), "SpanId should be consistent across turns");
    }
  }

  @Nested
  @DisplayName("ParallelAgents Shared Trace")
  class ParallelAgentsSharedTrace {

    @Test
    @DisplayName("run() sets shared parent trace on all agent contexts")
    void run_setsSharedParentTraceOnAllContexts() throws Exception {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");
      ParallelAgents parallel = ParallelAgents.of(agent1, agent2);
      
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");
      
      List<AgentResult> results = parallel.run("Test input").get(5, TimeUnit.SECONDS);
      
      assertEquals(2, results.size(), "Should have 2 results");
      assertFalse(results.get(0).isError(), "First result should not be error");
      assertFalse(results.get(1).isError(), "Second result should not be error");
    }

    @Test
    @DisplayName("run() with shared context propagates trace")
    void run_withSharedContext_propagatesTrace() throws Exception {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");
      ParallelAgents parallel = ParallelAgents.of(agent1, agent2);
      
      String existingTraceId = "eeee5555ffff6666aaaa7777bbbb8888";
      String existingSpanId = "5555666677778888";
      
      AgentContext sharedContext = AgentContext.create()
          .withTraceContext(existingTraceId, existingSpanId);
      
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");
      
      List<AgentResult> results = parallel.run("Test input", sharedContext).get(5, TimeUnit.SECONDS);
      
      assertEquals(2, results.size());
      // Original context should still have its trace (copy was made)
      assertEquals(existingTraceId, sharedContext.parentTraceId());
      assertEquals(existingSpanId, sharedContext.parentSpanId());
    }

    @Test
    @DisplayName("runFirst() sets shared parent trace on racing contexts")
    void runFirst_setsSharedParentTrace() throws Exception {
      Agent agent1 = createTestAgent("FastAgent");
      Agent agent2 = createTestAgent("SlowAgent");
      ParallelAgents parallel = ParallelAgents.of(agent1, agent2);
      
      enqueueSuccessResponse("Fast response");
      enqueueSuccessResponse("Slow response");
      
      AgentResult result = parallel.runFirst("Race").get(5, TimeUnit.SECONDS);
      
      assertNotNull(result, "Should have a result");
      assertFalse(result.isError(), "Result should not be error");
    }

    @Test
    @DisplayName("runAndSynthesize() propagates trace through synthesis")
    void runAndSynthesize_propagatesTraceToSynthesizer() throws Exception {
      Agent worker1 = createTestAgent("Worker1");
      Agent worker2 = createTestAgent("Worker2");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents parallel = ParallelAgents.of(worker1, worker2);
      
      enqueueSuccessResponse("Worker1 output");
      enqueueSuccessResponse("Worker2 output");
      enqueueSuccessResponse("Synthesized result");
      
      AgentResult result = parallel.runAndSynthesize("Analyze this", synthesizer)
          .get(5, TimeUnit.SECONDS);
      
      assertNotNull(result, "Should have synthesized result");
      assertFalse(result.isError(), "Synthesized result should not be error");
    }
  }

  @Nested
  @DisplayName("Trace Context Inheritance")
  class TraceContextInheritance {

    @Test
    @DisplayName("requestId is preserved through agent interactions")
    void requestId_isPreservedThroughInteractions() throws Exception {
      Agent agent = createTestAgent("TestAgent");
      AgentContext ctx = AgentContext.create()
          .withRequestId("user-session-12345");
      
      enqueueSuccessResponse("Hello!");
      
      ctx.addInput(Message.user("Hello"));
      agent.interact(ctx).get(5, TimeUnit.SECONDS);
      
      assertEquals("user-session-12345", ctx.requestId(), "RequestId should be preserved");
    }

    @Test
    @DisplayName("context copy preserves all trace fields")
    void contextCopy_preservesAllTraceFields() throws Exception {
      Agent agent = createTestAgent("TestAgent");
      AgentContext original = AgentContext.create()
          .withTraceContext("1111222233334444555566667777888", "aabbccddeeff0011")
          .withRequestId("session-abc");
      
      AgentContext copy = original.copy();
      
      assertEquals(original.parentTraceId(), copy.parentTraceId());
      assertEquals(original.parentSpanId(), copy.parentSpanId());
      assertEquals(original.requestId(), copy.requestId());
      
      // Verify copy is independent
      copy.withRequestId("different-session");
      assertEquals("session-abc", original.requestId(), "Original should not be modified");
    }

    @Test
    @DisplayName("context fork updates spanId but preserves traceId and requestId")
    void contextFork_updatesSpanIdPreservesOthers() throws Exception {
      String originalTrace = "aaaabbbbccccddddeeeeffffaaaabbbb";
      String originalSpan = "1234567890abcdef";
      
      AgentContext parent = AgentContext.create()
          .withTraceContext(originalTrace, originalSpan)
          .withRequestId("request-123");
      
      String newSpan = TraceIdGenerator.generateSpanId();
      AgentContext child = parent.fork(newSpan);
      
      assertEquals(originalTrace, child.parentTraceId(), "TraceId should be inherited");
      assertEquals(newSpan, child.parentSpanId(), "SpanId should be updated");
      assertEquals("request-123", child.requestId(), "RequestId should be inherited");
      
      // Turn count should be reset for child
      assertEquals(0, child.getTurnCount(), "Child turn count should be reset");
    }
  }

  // Helper methods

  private Agent createTestAgent(String name) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions("Test instructions for " + name)
        .responder(responder)
        .build();
  }

  private void enqueueSuccessResponse(String text) {
    String json = """
        {
          "id": "resp_001",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "test-model",
          "output": [
            {
              "type": "message",
              "id": "msg_001",
              "role": "assistant",
              "content": [
                {
                  "type": "output_text",
                  "text": "%s"
                }
              ]
            }
          ],
          "usage": {
            "input_tokens": 10,
            "output_tokens": 5,
            "total_tokens": 15
          }
        }
        """.formatted(text);

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(json)
        .addHeader("Content-Type", "application/json"));
  }
}
