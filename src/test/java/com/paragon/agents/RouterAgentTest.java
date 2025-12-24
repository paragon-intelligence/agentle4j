package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.Responder;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Comprehensive tests for RouterAgent.
 *
 * <p>Tests cover:
 * - Builder pattern
 * - Route configuration
 * - Classification (classify)
 * - Routing with execution (route)
 * - Fallback handling
 * - Async behavior
 */
@DisplayName("RouterAgent")
class RouterAgentTest {

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
  @DisplayName("Builder")
  class Builder {

    @Test
    @DisplayName("builder() creates new builder instance")
    void builder_createsNewInstance() {
      RouterAgent.Builder builder = RouterAgent.builder();

      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() throws when no routes provided")
    void build_throwsWhenNoRoutes() {
      assertThrows(IllegalArgumentException.class, () -> {
        RouterAgent.builder()
            .model("test-model")
            .responder(responder)
            .build();
      });
    }

    @Test
    @DisplayName("build() throws when model is null")
    void build_throwsWhenModelNull() {
      Agent target = createTestAgent("Target");

      assertThrows(NullPointerException.class, () -> {
        RouterAgent.builder()
            .responder(responder)
            .addRoute(target, "test")
            .build();
      });
    }

    @Test
    @DisplayName("build() throws when responder is null")
    void build_throwsWhenResponderNull() {
      Agent target = createTestAgent("Target");

      assertThrows(NullPointerException.class, () -> {
        RouterAgent.builder()
            .model("test-model")
            .addRoute(target, "test")
            .build();
      });
    }

    @Test
    @DisplayName("addRoute() adds route to router")
    void addRoute_addsRouteToRouter() {
      Agent target = createTestAgent("Target");

      RouterAgent router = RouterAgent.builder()
          .model("test-model")
          .responder(responder)
          .addRoute(target, "test description")
          .build();

      assertEquals(1, router.routes().size());
      assertEquals(target, router.routes().getFirst().agent());
      assertEquals("test description", router.routes().getFirst().description());
    }

    @Test
    @DisplayName("multiple addRoute() calls add multiple routes")
    void addRoute_multipleCallsAddMultipleRoutes() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");
      Agent agent3 = createTestAgent("Agent3");

      RouterAgent router = RouterAgent.builder()
          .model("test-model")
          .responder(responder)
          .addRoute(agent1, "billing")
          .addRoute(agent2, "technical")
          .addRoute(agent3, "sales")
          .build();

      assertEquals(3, router.routes().size());
    }

    @Test
    @DisplayName("fallback() sets fallback agent")
    void fallback_setsFallbackAgent() {
      Agent target = createTestAgent("Target");
      Agent fallback = createTestAgent("Fallback");

      RouterAgent router = RouterAgent.builder()
          .model("test-model")
          .responder(responder)
          .addRoute(target, "test")
          .fallback(fallback)
          .build();

      assertNotNull(router);
    }
  }

  @Nested
  @DisplayName("routes()")
  class Routes {

    @Test
    @DisplayName("routes() returns unmodifiable list")
    void routes_returnsUnmodifiableList() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(UnsupportedOperationException.class, () -> {
        router.routes().add(new RouterAgent.Route(createTestAgent("New"), "new"));
      });
    }

    @Test
    @DisplayName("Route record holds agent and description")
    void route_holdsAgentAndDescription() {
      Agent agent = createTestAgent("Test");
      RouterAgent.Route route = new RouterAgent.Route(agent, "test description");

      assertEquals(agent, route.agent());
      assertEquals("test description", route.description());
    }

    @Test
    @DisplayName("Route throws when agent is null")
    void route_throwsWhenAgentNull() {
      assertThrows(NullPointerException.class, () -> {
        new RouterAgent.Route(null, "test");
      });
    }

    @Test
    @DisplayName("Route throws when description is null")
    void route_throwsWhenDescriptionNull() {
      Agent agent = createTestAgent("Test");

      assertThrows(NullPointerException.class, () -> {
        new RouterAgent.Route(agent, null);
      });
    }
  }

  @Nested
  @DisplayName("classify()")
  class Classify {

    @Test
    @DisplayName("classify() returns CompletableFuture")
    void classify_returnsCompletableFuture() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      CompletableFuture<Agent> future = router.classify("test input");

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("classify() selects correct agent based on LLM response")
    void classify_selectsCorrectAgent() throws Exception {
      Agent billing = createTestAgent("Billing");
      Agent tech = createTestAgent("TechSupport");
      
      RouterAgent router = RouterAgent.builder()
          .model("test-model")
          .responder(responder)
          .addRoute(billing, "billing, invoices")
          .addRoute(tech, "technical issues")
          .build();

      enqueueResponse("1"); // LLM says route to first agent

      Agent selected = router.classify("Invoice question").get(5, TimeUnit.SECONDS);

      assertEquals(billing, selected);
    }

    @Test
    @DisplayName("classify() returns fallback when LLM response invalid")
    void classify_returnsFallbackWhenInvalid() throws Exception {
      Agent target = createTestAgent("Target");
      Agent fallback = createTestAgent("Fallback");
      
      RouterAgent router = RouterAgent.builder()
          .model("test-model")
          .responder(responder)
          .addRoute(target, "test")
          .fallback(fallback)
          .build();

      enqueueResponse("invalid"); // LLM returns unparseable response

      Agent selected = router.classify("test").get(5, TimeUnit.SECONDS);

      assertEquals(fallback, selected);
    }

    @Test
    @DisplayName("classify() returns null when no fallback and LLM invalid")
    void classify_returnsNullWhenNoFallback() throws Exception {
      Agent target = createTestAgent("Target");
      
      RouterAgent router = RouterAgent.builder()
          .model("test-model")
          .responder(responder)
          .addRoute(target, "test")
          .build();

      enqueueResponse("invalid");

      Agent selected = router.classify("test").get(5, TimeUnit.SECONDS);

      assertNull(selected);
    }

    @Test
    @DisplayName("classify() throws when input is null")
    void classify_throwsWhenInputNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.classify(null));
    }
  }

  @Nested
  @DisplayName("route()")
  class Route {

    @Test
    @DisplayName("route() returns CompletableFuture")
    void route_returnsCompletableFuture() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      // Classification response + target agent response
      enqueueResponse("1");
      enqueueSuccessResponse("Target response");

      CompletableFuture<AgentResult> future = router.route("test input");

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("route() returns error when no agent selected")
    void route_returnsErrorWhenNoAgentSelected() throws Exception {
      Agent target = createTestAgent("Target");
      
      RouterAgent router = RouterAgent.builder()
          .model("test-model")
          .responder(responder)
          .addRoute(target, "test")
          .build();

      enqueueResponse("invalid"); // No valid agent selected

      AgentResult result = router.route("test").get(5, TimeUnit.SECONDS);

      assertTrue(result.isError());
    }

    @Test
    @DisplayName("route() throws when input is null")
    void route_throwsWhenInputNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.route(null));
    }

    @Test
    @DisplayName("route() throws when context is null")
    void route_throwsWhenContextNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.route("test", null));
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

  private RouterAgent createRouter(Agent target) {
    return RouterAgent.builder()
        .model("test-model")
        .responder(responder)
        .addRoute(target, "test")
        .build();
  }

  private void enqueueResponse(String text) {
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

  private void enqueueSuccessResponse(String text) {
    enqueueResponse(text);
  }
}
