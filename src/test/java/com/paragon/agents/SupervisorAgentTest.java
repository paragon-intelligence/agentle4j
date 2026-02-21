package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SupervisorAgent.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Builder validation
 *   <li>Worker management
 *   <li>Orchestration methods
 *   <li>Streaming support
 *   <li>Error handling
 * </ul>
 */
@DisplayName("SupervisorAgent")
class SupervisorAgentTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
            Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  private Agent createTestAgent(String name) {
    return Agent.builder()
            .name(name)
            .model("test-model")
            .instructions("Test instructions for " + name)
            .responder(responder)
            .build();
  }

  private SupervisorAgent createTestSupervisor(Agent worker) {
    return SupervisorAgent.builder()
            .name("TestSupervisor")
            .model("test-model")
            .instructions("Coordinate workers effectively")
            .responder(responder)
            .addWorker(worker, "general work")
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

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builder() returns non-null builder")
    void builder_returnsBuilder() {
      SupervisorAgent.Builder builder = SupervisorAgent.builder();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() with all required fields succeeds")
    void build_withRequiredFields_succeeds() {
      Agent worker = createTestAgent("Worker");

      SupervisorAgent supervisor =
              SupervisorAgent.builder()
                      .name("TestSupervisor")
                      .model("test-model")
                      .instructions("Coordinate workers")
                      .responder(responder)
                      .addWorker(worker, "does work")
                      .build();

      assertNotNull(supervisor);
      assertEquals("TestSupervisor", supervisor.name());
      assertEquals(1, supervisor.workers().size());
    }

    @Test
    @DisplayName("build() without workers throws exception")
    void build_withoutWorkers_throws() {
      assertThrows(
              IllegalArgumentException.class,
              () ->
                      SupervisorAgent.builder()
                              .name("TestSupervisor")
                              .model("test-model")
                              .instructions("Coordinate workers")
                              .responder(responder)
                              .build());
    }

    @Test
    @DisplayName("build() uses default name if not specified")
    void build_usesDefaultName() {
      Agent worker = createTestAgent("Worker");

      SupervisorAgent supervisor =
              SupervisorAgent.builder()
                      .model("test-model")
                      .instructions("Coordinate workers")
                      .responder(responder)
                      .addWorker(worker, "does work")
                      .build();

      assertEquals("Supervisor", supervisor.name());
    }

    @Test
    @DisplayName("addWorker() validates null worker")
    void addWorker_nullWorker_throws() {
      assertThrows(
              NullPointerException.class,
              () -> SupervisorAgent.builder().addWorker(null, "description"));
    }

    @Test
    @DisplayName("addWorker() validates null description")
    void addWorker_nullDescription_throws() {
      Agent worker = createTestAgent("Worker");
      assertThrows(
              NullPointerException.class, () -> SupervisorAgent.builder().addWorker(worker, null));
    }

    @Test
    @DisplayName("maxTurns() rejects invalid values")
    void maxTurns_invalidValue_throws() {
      assertThrows(IllegalArgumentException.class, () -> SupervisorAgent.builder().maxTurns(0));
      assertThrows(IllegalArgumentException.class, () -> SupervisorAgent.builder().maxTurns(-1));
    }

    @Test
    @DisplayName("maxTurns() accepts valid values")
    void maxTurns_validValue_succeeds() {
      Agent worker = createTestAgent("Worker");

      SupervisorAgent supervisor =
              SupervisorAgent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test")
                      .responder(responder)
                      .addWorker(worker, "work")
                      .maxTurns(5)
                      .build();

      assertNotNull(supervisor);
    }
  }

  // Helper methods

  @Nested
  @DisplayName("Workers")
  class WorkerTests {

    @Test
    @DisplayName("workers() returns unmodifiable list")
    void workers_returnsUnmodifiableList() {
      Agent worker = createTestAgent("Worker");

      SupervisorAgent supervisor =
              SupervisorAgent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test")
                      .responder(responder)
                      .addWorker(worker, "work")
                      .build();

      assertThrows(UnsupportedOperationException.class, () -> supervisor.workers().clear());
    }

    @Test
    @DisplayName("multiple workers can be added")
    void multipleWorkers_canBeAdded() {
      Agent worker1 = createTestAgent("Worker1");
      Agent worker2 = createTestAgent("Worker2");

      SupervisorAgent supervisor =
              SupervisorAgent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test")
                      .responder(responder)
                      .addWorker(worker1, "research")
                      .addWorker(worker2, "writing")
                      .build();

      assertEquals(2, supervisor.workers().size());
    }

    @Test
    @DisplayName("Worker record validates null agent")
    void workerRecord_nullAgent_throws() {
      assertThrows(
              NullPointerException.class, () -> new SupervisorAgent.Worker(null, "description"));
    }

    @Test
    @DisplayName("Worker record validates null description")
    void workerRecord_nullDescription_throws() {
      Agent agent = createTestAgent("Test");
      assertThrows(NullPointerException.class, () -> new SupervisorAgent.Worker(agent, null));
    }
  }

  @Nested
  @DisplayName("Orchestration")
  class OrchestrationTests {

    @Test
    @DisplayName("orchestrate(String) returns AgentResult")
    void orchestrate_string_returnsAgentResult() {
      Agent worker = createTestAgent("Worker");
      SupervisorAgent supervisor = createTestSupervisor(worker);
      enqueueSuccessResponse("Result");

      AgentResult result = supervisor.interact("Test task");

      assertNotNull(result);
      assertInstanceOf(AgentResult.class, result);
    }


    @Test
    @DisplayName("orchestrate(Text) returns AgentResult")
    void orchestrate_text_returnsAgentResult() {
      Agent worker = createTestAgent("Worker");
      SupervisorAgent supervisor = createTestSupervisor(worker);
      enqueueSuccessResponse("Result");

      AgentResult result = supervisor.interact(Text.valueOf("Test task"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("orchestrate(Message) returns AgentResult")
    void orchestrate_message_returnsAgentResult() {
      Agent worker = createTestAgent("Worker");
      SupervisorAgent supervisor = createTestSupervisor(worker);
      enqueueSuccessResponse("Result");

      AgentResult result = supervisor.interact(Message.user("Test task"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("orchestrate(AgentContext) returns AgentResult")
    void orchestrate_context_returnsAgentResult() {
      Agent worker = createTestAgent("Worker");
      SupervisorAgent supervisor = createTestSupervisor(worker);

      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Test task"));
      enqueueSuccessResponse("Result");

      AgentResult result = supervisor.interact(context);

      assertNotNull(result);
    }

    @Test
    @DisplayName("orchestrate() completes with result")
    void orchestrate_completesWithResult() throws Exception {
      Agent worker = createTestAgent("Worker");
      SupervisorAgent supervisor = createTestSupervisor(worker);
      enqueueSuccessResponse("Orchestrated result");

      AgentResult result = supervisor.interact("Do the task");

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Streaming")
  class StreamingTests {

    @Test
    @DisplayName("orchestrateStream(String) returns AgentStream")
    void orchestrateStream_string_returnsAgentStream() {
      Agent worker = createTestAgent("Worker");
      SupervisorAgent supervisor = createTestSupervisor(worker);

      AgentStream stream = supervisor.interactStream("Test task");

      assertNotNull(stream);
      assertInstanceOf(AgentStream.class, stream);
    }

    @Test
    @DisplayName("orchestrateStream(AgentContext) returns AgentStream")
    void orchestrateStream_context_returnsAgentStream() {
      Agent worker = createTestAgent("Worker");
      SupervisorAgent supervisor = createTestSupervisor(worker);

      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Test task"));

      AgentStream stream = supervisor.interactStream(context);

      assertNotNull(stream);
    }

  }
}
