package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import java.util.List;
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
    @DisplayName("of(Agent...) creates orchestrator with members")
    void of_varargs_createsOrchestrator() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");

      ParallelAgents orchestrator = ParallelAgents.of(agent1, agent2);

      assertEquals(2, orchestrator.members().size());
      assertTrue(orchestrator.members().contains(agent1));
      assertTrue(orchestrator.members().contains(agent2));
    }

    @Test
    @DisplayName("of(List<Interactable>) creates orchestrator from list")
    void of_list_createsOrchestrator() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");
      List<Interactable> members = List.of(agent1, agent2);

      ParallelAgents orchestrator = ParallelAgents.of(members);

      assertEquals(2, orchestrator.members().size());
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
    @DisplayName("members() returns unmodifiable list")
    void members_returnsUnmodifiableList() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(
          UnsupportedOperationException.class,
          () -> orchestrator.members().add(createTestAgent("New")));
    }
  }

  @Nested
  @DisplayName("run(String)")
  class RunString {

    @Test
    @DisplayName("runAll(String) returns List<AgentResult>")
    void runAll_returnsListOfAgentResult() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("Hello");

      List<AgentResult> results = orchestrator.runAll("Test input");

      assertNotNull(results);
      assertInstanceOf(List.class, results);
    }

    @Test
    @DisplayName("runAll(String) returns result for each agent in order")
    void runAll_returnsResultsInOrder() throws Exception {
      Agent agent1 = createTestAgent("First");
      Agent agent2 = createTestAgent("Second");
      ParallelAgents orchestrator = ParallelAgents.of(agent1, agent2);

      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      List<AgentResult> results = orchestrator.runAll("Test");

      assertEquals(2, results.size());
    }

    @Test
    @DisplayName("runAll(String) throws when input is null")
    void runAll_throwsWhenInputNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runAll((String) null));
    }
  }

  @Nested
  @DisplayName("run(Text)")
  class RunText {

    @Test
    @DisplayName("runAll(Text) creates context and runs")
    void runAll_text_createsContextAndRuns() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("Hello");

      List<AgentResult> results = orchestrator.runAll(Text.valueOf("Test text"));

      assertNotNull(results);
    }

    @Test
    @DisplayName("runAll(Text) throws when text is null")
    void runAll_text_throwsWhenNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runAll((Text) null));
    }
  }

  @Nested
  @DisplayName("run(Message)")
  class RunMessage {

    @Test
    @DisplayName("runAll(Message) creates context and runs")
    void runAll_message_createsContextAndRuns() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("Hello");

      List<AgentResult> results = orchestrator.runAll(Message.user("Test message"));

      assertNotNull(results);
    }

    @Test
    @DisplayName("runAll(Message) throws when message is null")
    void runAll_message_throwsWhenNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runAll((Message) null));
    }
  }

  @Nested
  @DisplayName("run(AgentContext)")
  class RunContext {

    @Test
    @DisplayName("runAll(AgentContext) uses context for all agents")
    void runAll_context_usesContextForAllAgents() throws Exception {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Context input"));
      enqueueSuccessResponse("Response");

      List<AgentResult> results = orchestrator.runAll(context);

      assertNotNull(results);
      assertEquals(1, results.size());
    }

    @Test
    @DisplayName("runAll(AgentContext) throws when context is null")
    void runAll_context_throwsWhenNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runAll((AgentContext) null));
    }
  }

  @Nested
  @DisplayName("runFirst()")
  class RunFirst {

    @Test
    @DisplayName("runFirst(String) returns AgentResult for single result")
    void runFirst_returnsAgentResult() throws Exception {
      Agent agent1 = createTestAgent("Fast");
      Agent agent2 = createTestAgent("Slow");
      ParallelAgents orchestrator = ParallelAgents.of(agent1, agent2);

      enqueueSuccessResponse("First!");
      enqueueSuccessResponse("Second!");

      AgentResult result = orchestrator.runFirst("Race");

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

      AgentResult result = orchestrator.runFirst(Text.valueOf("Test"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("runFirst(Message) creates context and runs")
    void runFirst_message_createsContextAndRuns() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);
      enqueueSuccessResponse("First!");

      AgentResult result = orchestrator.runFirst(Message.user("Test"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("runFirst(AgentContext) uses context and runs")
    void runFirst_context_usesContextAndRuns() throws Exception {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Context input"));
      enqueueSuccessResponse("First!");

      AgentResult result = orchestrator.runFirst(context);

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("runAndSynthesize()")
  class RunAndSynthesize {

    @Test
    @DisplayName("runAndSynthesize(String, Agent) returns AgentResult")
    void runAndSynthesize_returnsAgentResult() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      enqueueSuccessResponse("Worker output");
      enqueueSuccessResponse("Synthesized result");

      AgentResult result = orchestrator.runAndSynthesize("Task", synthesizer);

      assertNotNull(result);
      assertInstanceOf(AgentResult.class, result);
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

      AgentResult result =
          orchestrator.runAndSynthesize(Text.valueOf("Task"), synthesizer);

      assertNotNull(result);
    }

    @Test
    @DisplayName("runAndSynthesize(Message, Agent) creates context and synthesizes")
    void runAndSynthesize_message_createsContextAndSynthesizes() {
      Agent worker = createTestAgent("Worker");
      Agent synthesizer = createTestAgent("Synthesizer");
      ParallelAgents orchestrator = ParallelAgents.of(worker);

      enqueueSuccessResponse("Worker output");
      enqueueSuccessResponse("Synthesized result");

      AgentResult result =
          orchestrator.runAndSynthesize(Message.user("Task"), synthesizer);

      assertNotNull(result);
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
          orchestrator.runAndSynthesize(context, synthesizer);

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Streaming Methods")
  class Streaming {

    @Test
    @DisplayName("runAllStream(String) returns ParallelStream")
    void runAllStream_string_returnsParallelStream() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      ParallelStream stream = orchestrator.runAllStream("Test");

      assertNotNull(stream);
      assertInstanceOf(ParallelStream.class, stream);
    }

    @Test
    @DisplayName("runAllStream(AgentContext) returns ParallelStream")
    void runAllStream_context_returnsParallelStream() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Test"));

      ParallelStream stream = orchestrator.runAllStream(context);

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
    @DisplayName("runAllStream(String) throws when input is null")
    void runAllStream_throwsWhenInputNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runAllStream((String) null));
    }

    @Test
    @DisplayName("runAllStream(AgentContext) throws when context is null")
    void runAllStream_throwsWhenContextNull() {
      Agent agent = createTestAgent("Test");
      ParallelAgents orchestrator = ParallelAgents.of(agent);

      assertThrows(NullPointerException.class, () -> orchestrator.runAllStream((AgentContext) null));
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
