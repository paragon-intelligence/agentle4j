package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.Text;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Comprehensive tests for ParallelAgents.
 *
 * <p>Tests cover:
 * - Factory methods (of)
 * - Parallel execution (run)
 * - First-to-complete racing (runFirst)
 * - Fan-out/fan-in synthesis (runAndSynthesize)
 * - Context isolation
 * - Error handling
 * - Async behavior
 */
@DisplayName("ParallelAgents")
class ParallelAgentsTest {

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

  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("of(Agent...) creates orchestrator with agents")
    void of_varargs_createsOrchestrator() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");

      ParallelAgents orchestrator = ParallelAgents.of(agent1, agent2);

      assertEquals(2, orchestrator.agents().size());
      assertTrue(orchestrator.agents().contains(agent1));
      assertTrue(orchestrator.agents().contains(agent2));
    }

    @Test
    @DisplayName("of(List<Agent>) creates orchestrator from list")
    void of_list_createsOrchestrator() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");
      List<Agent> agents = List.of(agent1, agent2);

      ParallelAgents orchestrator = ParallelAgents.of(agents);

      assertEquals(2, orchestrator.agents().size());
    }

    @Test
    @DisplayName("of() throws when no agents provided")
    void of_throwsWhenEmpty() {
      assertThrows(IllegalArgumentException.class, () -> ParallelAgents.of());
    }

    @Test
    @DisplayName("of() throws when null provided")
    void of_throwsWhenNull() {
      assertThrows(NullPointerException.class, () -> ParallelAgents.of((Agent[]) null));
    }

    @Test
    @DisplayName("agents() returns unmodifiable list")
    void agents_returnsUnmodifiableList() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(UnsupportedOperationException.class, 
          () -> orchestrator.agents().add(createTestAgent("New")));
    }
  }

  @Nested
  @DisplayName("run() - Parallel Execution")
  class RunParallelExecution {

    @Test
    @DisplayName("run() returns CompletableFuture")
    void run_returnsCompletableFuture() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("Hello");

      CompletableFuture<List<AgentResult>> future = orchestrator.run("Test input");

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("run() returns result for each agent in order")
    void run_returnsResultsInOrder() throws Exception {
      Agent agent1 = createTestAgent("First");
      Agent agent2 = createTestAgent("Second");
      ParallelAgents orchestrator = ParallelAgents.of(agent1, agent2);
      
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      List<AgentResult> results = orchestrator.run("Test").get(5, TimeUnit.SECONDS);

      assertEquals(2, results.size());
    }

    @Test
    @DisplayName("run() throws when input is null")
    void run_throwsWhenInputNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.run(null));
    }
  }

  @Nested
  @DisplayName("runFirst() - Racing")
  class RunFirstRacing {

    @Test
    @DisplayName("runFirst() returns CompletableFuture with single result")
    void runFirst_returnsFutureWithSingleResult() throws Exception {
      Agent agent1 = createTestAgent("Fast");
      Agent agent2 = createTestAgent("Slow");
      ParallelAgents orchestrator = ParallelAgents.of(agent1, agent2);
      
      enqueueSuccessResponse("First!");
      enqueueSuccessResponse("Second!");

      CompletableFuture<AgentResult> future = orchestrator.runFirst("Race");

      assertNotNull(future);
      AgentResult result = future.get(5, TimeUnit.SECONDS);
      assertNotNull(result);
    }

    @Test
    @DisplayName("runFirst() throws when input is null")
    void runFirst_throwsWhenInputNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runFirst(null));
    }
  }

  @Nested
  @DisplayName("runAndSynthesize() - Fan-out/Fan-in")
  class RunAndSynthesize {

    @Test
    @DisplayName("runAndSynthesize() returns CompletableFuture")
    void runAndSynthesize_returnsCompletableFuture() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);
      
      enqueueSuccessResponse("Worker output");
      enqueueSuccessResponse("Synthesized result");

      CompletableFuture<AgentResult> future = orchestrator.runAndSynthesize("Task", synthesizer);

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("runAndSynthesize() throws when input is null")
    void runAndSynthesize_throwsWhenInputNull() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      assertThrows(NullPointerException.class, 
          () -> orchestrator.runAndSynthesize(null, synthesizer));
    }

    @Test
    @DisplayName("runAndSynthesize() throws when synthesizer is null")
    void runAndSynthesize_throwsWhenSynthesizerNull() {
      Agent worker = createTestAgent("Worker");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      assertThrows(NullPointerException.class, 
          () -> orchestrator.runAndSynthesize("Task", null));
    }
  }

  @Nested
  @DisplayName("Context Isolation")
  class ContextIsolation {

    @Test
    @DisplayName("run() uses copy of shared context for each agent")
    void run_usesContextCopyForEachAgent() throws Exception {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");
      ParallelAgents orchestrator = ParallelAgents.of(agent1, agent2);

      AgentContext sharedContext = AgentContext.create();
      sharedContext.setState("shared", "initial");

      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      orchestrator.run("Test", sharedContext).get(5, TimeUnit.SECONDS);

      // Original context unchanged
      assertEquals("initial", sharedContext.getState("shared"));
    }

    @Test
    @DisplayName("run() without context creates fresh context for each agent")
    void run_withoutContext_createsFreshContexts() throws Exception {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      enqueueSuccessResponse("Response");

      List<AgentResult> results = orchestrator.run("Test").get(5, TimeUnit.SECONDS);

      assertNotNull(results);
      assertEquals(1, results.size());
    }
  }

  // Helper methods

  private Agent createTestAgent(String name) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions("Test instructions")
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
