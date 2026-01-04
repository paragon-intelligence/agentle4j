package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for ParallelAgents.
 *
 * <p>Tests cover: - Factory methods (of) - Parallel execution (run) - First-to-complete racing
 * (runFirst) - Fan-out/fan-in synthesis (runAndSynthesize) - Streaming - Error handling - Async
 * behavior
 */
@DisplayName("ParallelAgents")
class ParallelAgentsTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
        Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
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

      assertThrows(
          UnsupportedOperationException.class,
          () -> orchestrator.agents().add(createTestAgent("New")));
    }
  }

  @Nested
  @DisplayName("run(String)")
  class RunString {

    @Test
    @DisplayName("run(String) returns CompletableFuture")
    void run_returnsCompletableFuture() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("Hello");

      CompletableFuture<List<AgentResult>> future = orchestrator.run("Test input");

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("run(String) returns result for each agent in order")
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
    @DisplayName("run(String) throws when input is null")
    void run_throwsWhenInputNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.run((String) null));
    }
  }

  @Nested
  @DisplayName("run(Text)")
  class RunText {

    @Test
    @DisplayName("run(Text) creates context and runs")
    void run_text_createsContextAndRuns() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("Hello");

      CompletableFuture<List<AgentResult>> future = orchestrator.run(Text.valueOf("Test text"));

      assertNotNull(future);
    }

    @Test
    @DisplayName("run(Text) throws when text is null")
    void run_text_throwsWhenNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.run((Text) null));
    }
  }

  @Nested
  @DisplayName("run(Message)")
  class RunMessage {

    @Test
    @DisplayName("run(Message) creates context and runs")
    void run_message_createsContextAndRuns() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("Hello");

      CompletableFuture<List<AgentResult>> future = orchestrator.run(Message.user("Test message"));

      assertNotNull(future);
    }

    @Test
    @DisplayName("run(Message) throws when message is null")
    void run_message_throwsWhenNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.run((Message) null));
    }
  }

  @Nested
  @DisplayName("run(AgentContext)")
  class RunContext {

    @Test
    @DisplayName("run(AgentContext) uses context for all agents")
    void run_context_usesContextForAllAgents() throws Exception {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Context input"));
      enqueueSuccessResponse("Response");

      List<AgentResult> results = orchestrator.run(context).get(5, TimeUnit.SECONDS);

      assertNotNull(results);
      assertEquals(1, results.size());
    }

    @Test
    @DisplayName("run(AgentContext) throws when context is null")
    void run_context_throwsWhenNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.run((AgentContext) null));
    }
  }

  @Nested
  @DisplayName("runFirst()")
  class RunFirst {

    @Test
    @DisplayName("runFirst(String) returns CompletableFuture with single result")
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
    @DisplayName("runFirst(String) throws when input is null")
    void runFirst_throwsWhenInputNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runFirst((String) null));
    }

    @Test
    @DisplayName("runFirst(Text) creates context and runs")
    void runFirst_text_createsContextAndRuns() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("First!");

      CompletableFuture<AgentResult> future = orchestrator.runFirst(Text.valueOf("Test"));

      assertNotNull(future);
    }

    @Test
    @DisplayName("runFirst(Message) creates context and runs")
    void runFirst_message_createsContextAndRuns() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("First!");

      CompletableFuture<AgentResult> future = orchestrator.runFirst(Message.user("Test"));

      assertNotNull(future);
    }

    @Test
    @DisplayName("runFirst(AgentContext) uses context and runs")
    void runFirst_context_usesContextAndRuns() throws Exception {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Context input"));
      enqueueSuccessResponse("First!");

      AgentResult result = orchestrator.runFirst(context).get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("runAndSynthesize()")
  class RunAndSynthesize {

    @Test
    @DisplayName("runAndSynthesize(String, Agent) returns CompletableFuture")
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
    @DisplayName("runAndSynthesize(String, Agent) throws when input is null")
    void runAndSynthesize_throwsWhenInputNull() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      assertThrows(
          NullPointerException.class,
          () -> orchestrator.runAndSynthesize((String) null, synthesizer));
    }

    @Test
    @DisplayName("runAndSynthesize(String, Agent) throws when synthesizer is null")
    void runAndSynthesize_throwsWhenSynthesizerNull() {
      Agent worker = createTestAgent("Worker");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      assertThrows(NullPointerException.class, () -> orchestrator.runAndSynthesize("Task", null));
    }

    @Test
    @DisplayName("runAndSynthesize(Text, Agent) creates context and synthesizes")
    void runAndSynthesize_text_createsContextAndSynthesizes() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      enqueueSuccessResponse("Worker output");
      enqueueSuccessResponse("Synthesized result");

      CompletableFuture<AgentResult> future =
          orchestrator.runAndSynthesize(Text.valueOf("Task"), synthesizer);

      assertNotNull(future);
    }

    @Test
    @DisplayName("runAndSynthesize(Message, Agent) creates context and synthesizes")
    void runAndSynthesize_message_createsContextAndSynthesizes() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      enqueueSuccessResponse("Worker output");
      enqueueSuccessResponse("Synthesized result");

      CompletableFuture<AgentResult> future =
          orchestrator.runAndSynthesize(Message.user("Task"), synthesizer);

      assertNotNull(future);
    }

    @Test
    @DisplayName("runAndSynthesize(AgentContext, Agent) uses context and synthesizes")
    void runAndSynthesize_context_usesContextAndSynthesizes() throws Exception {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Task from context"));

      enqueueSuccessResponse("Worker output");
      enqueueSuccessResponse("Synthesized result");

      AgentResult result =
          orchestrator.runAndSynthesize(context, synthesizer).get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Streaming Methods")
  class Streaming {

    @Test
    @DisplayName("runStream(String) returns ParallelStream")
    void runStream_string_returnsParallelStream() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      ParallelStream stream = orchestrator.runStream("Test");

      assertNotNull(stream);
      assertInstanceOf(ParallelStream.class, stream);
    }

    @Test
    @DisplayName("runStream(AgentContext) returns ParallelStream")
    void runStream_context_returnsParallelStream() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Test"));

      ParallelStream stream = orchestrator.runStream(context);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("runFirstStream(String) returns ParallelStream")
    void runFirstStream_string_returnsParallelStream() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      ParallelStream stream = orchestrator.runFirstStream("Test");

      assertNotNull(stream);
    }

    @Test
    @DisplayName("runFirstStream(AgentContext) returns ParallelStream")
    void runFirstStream_context_returnsParallelStream() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Test"));

      ParallelStream stream = orchestrator.runFirstStream(context);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("runAndSynthesizeStream(String, Agent) returns ParallelStream")
    void runAndSynthesizeStream_string_returnsParallelStream() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      ParallelStream stream = orchestrator.runAndSynthesizeStream("Task", synthesizer);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("runAndSynthesizeStream(AgentContext, Agent) returns ParallelStream")
    void runAndSynthesizeStream_context_returnsParallelStream() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Task"));

      ParallelStream stream = orchestrator.runAndSynthesizeStream(context, synthesizer);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("runStream(String) throws when input is null")
    void runStream_throwsWhenInputNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runStream((String) null));
    }

    @Test
    @DisplayName("runStream(AgentContext) throws when context is null")
    void runStream_throwsWhenContextNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runStream((AgentContext) null));
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
    String json =
        """
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
        """
            .formatted(text);

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }
}
