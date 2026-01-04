package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for HierarchicalAgents.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Builder validation
 *   <li>Department management
 *   <li>Executive and hierarchy structure
 *   <li>Execution methods
 *   <li>Streaming support
 *   <li>Direct department routing
 * </ul>
 */
@DisplayName("HierarchicalAgents")
class HierarchicalAgentsTest {

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

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builder() returns non-null builder")
    void builder_returnsBuilder() {
      HierarchicalAgents.Builder builder = HierarchicalAgents.builder();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() with executive and department succeeds")
    void build_withExecutiveAndDepartment_succeeds() {
      Agent executive = createTestAgent("CEO");
      Agent manager = createTestAgent("Manager");
      Agent worker = createTestAgent("Worker");

      HierarchicalAgents hierarchy =
          HierarchicalAgents.builder()
              .executive(executive)
              .addDepartment("Engineering", manager, worker)
              .build();

      assertNotNull(hierarchy);
      assertEquals(executive, hierarchy.executive());
      assertEquals(1, hierarchy.departments().size());
    }

    @Test
    @DisplayName("build() without executive throws exception")
    void build_withoutExecutive_throws() {
      Agent manager = createTestAgent("Manager");
      Agent worker = createTestAgent("Worker");

      assertThrows(
          NullPointerException.class,
          () ->
              HierarchicalAgents.builder().addDepartment("Engineering", manager, worker).build());
    }

    @Test
    @DisplayName("build() without departments throws exception")
    void build_withoutDepartments_throws() {
      Agent executive = createTestAgent("CEO");

      assertThrows(
          IllegalArgumentException.class,
          () -> HierarchicalAgents.builder().executive(executive).build());
    }

    @Test
    @DisplayName("addDepartment() with varargs succeeds")
    void addDepartment_varargs_succeeds() {
      Agent executive = createTestAgent("CEO");
      Agent manager = createTestAgent("Manager");
      Agent worker1 = createTestAgent("Worker1");
      Agent worker2 = createTestAgent("Worker2");

      HierarchicalAgents hierarchy =
          HierarchicalAgents.builder()
              .executive(executive)
              .addDepartment("Engineering", manager, worker1, worker2)
              .build();

      assertEquals(2, hierarchy.departments().get("Engineering").workers().size());
    }

    @Test
    @DisplayName("addDepartment() with list succeeds")
    void addDepartment_list_succeeds() {
      Agent executive = createTestAgent("CEO");
      Agent manager = createTestAgent("Manager");
      Agent worker1 = createTestAgent("Worker1");
      Agent worker2 = createTestAgent("Worker2");

      HierarchicalAgents hierarchy =
          HierarchicalAgents.builder()
              .executive(executive)
              .addDepartment("Engineering", manager, List.of(worker1, worker2))
              .build();

      assertEquals(2, hierarchy.departments().get("Engineering").workers().size());
    }

    @Test
    @DisplayName("addDepartment() validates null name")
    void addDepartment_nullName_throws() {
      Agent manager = createTestAgent("Manager");
      Agent worker = createTestAgent("Worker");

      assertThrows(
          NullPointerException.class,
          () -> HierarchicalAgents.builder().addDepartment(null, manager, worker));
    }

    @Test
    @DisplayName("addDepartment() validates null manager")
    void addDepartment_nullManager_throws() {
      Agent worker = createTestAgent("Worker");

      assertThrows(
          NullPointerException.class,
          () -> HierarchicalAgents.builder().addDepartment("Dept", null, worker));
    }

    @Test
    @DisplayName("addDepartment() validates empty workers")
    void addDepartment_emptyWorkers_throws() {
      Agent manager = createTestAgent("Manager");

      assertThrows(
          IllegalArgumentException.class,
          () -> HierarchicalAgents.builder().addDepartment("Dept", manager));
    }

    @Test
    @DisplayName("addDepartment() validates empty worker list")
    void addDepartment_emptyWorkerList_throws() {
      Agent manager = createTestAgent("Manager");

      assertThrows(
          IllegalArgumentException.class,
          () -> HierarchicalAgents.builder().addDepartment("Dept", manager, List.of()));
    }

    @Test
    @DisplayName("maxTurns() rejects invalid values")
    void maxTurns_invalidValue_throws() {
      assertThrows(
          IllegalArgumentException.class, () -> HierarchicalAgents.builder().maxTurns(0));
    }

    @Test
    @DisplayName("maxTurns() accepts valid values")
    void maxTurns_validValue_succeeds() {
      Agent executive = createTestAgent("CEO");
      Agent manager = createTestAgent("Manager");
      Agent worker = createTestAgent("Worker");

      HierarchicalAgents hierarchy =
          HierarchicalAgents.builder()
              .executive(executive)
              .addDepartment("Dept", manager, worker)
              .maxTurns(5)
              .build();

      assertNotNull(hierarchy);
    }
  }

  @Nested
  @DisplayName("Departments")
  class DepartmentTests {

    @Test
    @DisplayName("departments() returns unmodifiable map")
    void departments_returnsUnmodifiableMap() {
      HierarchicalAgents hierarchy = createTestHierarchy();

      assertThrows(
          UnsupportedOperationException.class,
          () -> hierarchy.departments().put("New", null));
    }

    @Test
    @DisplayName("multiple departments can be added")
    void multipleDepartments_canBeAdded() {
      Agent executive = createTestAgent("CEO");
      Agent manager1 = createTestAgent("TechManager");
      Agent manager2 = createTestAgent("SalesManager");
      Agent worker1 = createTestAgent("Dev");
      Agent worker2 = createTestAgent("SalesRep");

      HierarchicalAgents hierarchy =
          HierarchicalAgents.builder()
              .executive(executive)
              .addDepartment("Engineering", manager1, worker1)
              .addDepartment("Sales", manager2, worker2)
              .build();

      assertEquals(2, hierarchy.departments().size());
      assertTrue(hierarchy.departments().containsKey("Engineering"));
      assertTrue(hierarchy.departments().containsKey("Sales"));
    }

    @Test
    @DisplayName("Department record validates null manager")
    void department_nullManager_throws() {
      Agent worker = createTestAgent("Worker");
      assertThrows(
          NullPointerException.class,
          () -> new HierarchicalAgents.Department(null, List.of(worker)));
    }

    @Test
    @DisplayName("Department record validates null workers")
    void department_nullWorkers_throws() {
      Agent manager = createTestAgent("Manager");
      assertThrows(
          NullPointerException.class, () -> new HierarchicalAgents.Department(manager, null));
    }

    @Test
    @DisplayName("Department record validates empty workers")
    void department_emptyWorkers_throws() {
      Agent manager = createTestAgent("Manager");
      assertThrows(
          IllegalArgumentException.class,
          () -> new HierarchicalAgents.Department(manager, List.of()));
    }
  }

  @Nested
  @DisplayName("Execution")
  class ExecutionTests {

    @Test
    @DisplayName("execute(String) returns CompletableFuture")
    void execute_string_returnsCompletableFuture() {
      HierarchicalAgents hierarchy = createTestHierarchy();
      enqueueSuccessResponse("Result");

      CompletableFuture<AgentResult> future = hierarchy.execute("Task");

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("execute(String) validates null task")
    void execute_nullTask_throws() {
      HierarchicalAgents hierarchy = createTestHierarchy();

      assertThrows(NullPointerException.class, () -> hierarchy.execute((String) null));
    }

    @Test
    @DisplayName("execute(Text) returns CompletableFuture")
    void execute_text_returnsCompletableFuture() {
      HierarchicalAgents hierarchy = createTestHierarchy();
      enqueueSuccessResponse("Result");

      CompletableFuture<AgentResult> future = hierarchy.execute(Text.valueOf("Task"));

      assertNotNull(future);
    }

    @Test
    @DisplayName("execute(Message) returns CompletableFuture")
    void execute_message_returnsCompletableFuture() {
      HierarchicalAgents hierarchy = createTestHierarchy();
      enqueueSuccessResponse("Result");

      CompletableFuture<AgentResult> future = hierarchy.execute(Message.user("Task"));

      assertNotNull(future);
    }

    @Test
    @DisplayName("execute(AgentContext) returns CompletableFuture")
    void execute_context_returnsCompletableFuture() {
      HierarchicalAgents hierarchy = createTestHierarchy();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Task"));
      enqueueSuccessResponse("Result");

      CompletableFuture<AgentResult> future = hierarchy.execute(context);

      assertNotNull(future);
    }
  }

  @Nested
  @DisplayName("Streaming")
  class StreamingTests {

    @Test
    @DisplayName("executeStream(String) returns AgentStream")
    void executeStream_string_returnsAgentStream() {
      HierarchicalAgents hierarchy = createTestHierarchy();

      AgentStream stream = hierarchy.executeStream("Task");

      assertNotNull(stream);
      assertInstanceOf(AgentStream.class, stream);
    }

    @Test
    @DisplayName("executeStream(AgentContext) returns AgentStream")
    void executeStream_context_returnsAgentStream() {
      HierarchicalAgents hierarchy = createTestHierarchy();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Task"));

      AgentStream stream = hierarchy.executeStream(context);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("executeStream(String) validates null task")
    void executeStream_nullTask_throws() {
      HierarchicalAgents hierarchy = createTestHierarchy();

      assertThrows(NullPointerException.class, () -> hierarchy.executeStream((String) null));
    }
  }

  @Nested
  @DisplayName("Direct Department Routing")
  class DirectRoutingTests {

    @Test
    @DisplayName("sendToDepartment() routes to specific department")
    void sendToDepartment_routesToDepartment() {
      HierarchicalAgents hierarchy = createTestHierarchy();
      enqueueSuccessResponse("Result");

      CompletableFuture<AgentResult> future = hierarchy.sendToDepartment("Engineering", "Task");

      assertNotNull(future);
    }

    @Test
    @DisplayName("sendToDepartment() throws for unknown department")
    void sendToDepartment_unknownDepartment_throws() {
      HierarchicalAgents hierarchy = createTestHierarchy();

      assertThrows(
          IllegalArgumentException.class,
          () -> hierarchy.sendToDepartment("Unknown", "Task"));
    }

    @Test
    @DisplayName("sendToDepartment() validates null department name")
    void sendToDepartment_nullDepartmentName_throws() {
      HierarchicalAgents hierarchy = createTestHierarchy();

      assertThrows(
          NullPointerException.class, () -> hierarchy.sendToDepartment(null, "Task"));
    }

    @Test
    @DisplayName("sendToDepartment() validates null task")
    void sendToDepartment_nullTask_throws() {
      HierarchicalAgents hierarchy = createTestHierarchy();

      assertThrows(
          NullPointerException.class, () -> hierarchy.sendToDepartment("Engineering", null));
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

  private HierarchicalAgents createTestHierarchy() {
    Agent executive = createTestAgent("CEO");
    Agent manager = createTestAgent("Manager");
    Agent worker = createTestAgent("Worker");

    return HierarchicalAgents.builder()
        .executive(executive)
        .addDepartment("Engineering", manager, worker)
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
