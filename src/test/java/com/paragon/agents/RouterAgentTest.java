package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for RouterAgent.
 *
 * <p>Tests cover: - Builder pattern - Route configuration - Classification (classify) - Routing
 * with execution (route) - Fallback handling - Streaming - Async behavior
 */
@DisplayName("RouterAgent")
class RouterAgentTest {

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

  private void enqueueSuccessResponse(String text) {
    enqueueResponse(text);
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
      assertThrows(
              IllegalArgumentException.class,
              () -> {
                RouterAgent.builder().model("test-model").responder(responder).build();
              });
    }

    @Test
    @DisplayName("build() throws when model is null")
    void build_throwsWhenModelNull() {
      Agent target = createTestAgent("Target");

      assertThrows(
              NullPointerException.class,
              () -> {
                RouterAgent.builder().responder(responder).addRoute(target, "test").build();
              });
    }

    @Test
    @DisplayName("build() throws when responder is null")
    void build_throwsWhenResponderNull() {
      Agent target = createTestAgent("Target");

      assertThrows(
              NullPointerException.class,
              () -> {
                RouterAgent.builder().model("test-model").addRoute(target, "test").build();
              });
    }

    @Test
    @DisplayName("addRoute() adds route to router")
    void addRoute_addsRouteToRouter() {
      Agent target = createTestAgent("Target");

      RouterAgent router =
              RouterAgent.builder()
                      .model("test-model")
                      .responder(responder)
                      .addRoute(target, "test description")
                      .build();

      assertEquals(1, router.routes().size());
      assertEquals(target, router.routes().getFirst().target());
      assertEquals("test description", router.routes().getFirst().description());
    }

    @Test
    @DisplayName("multiple addRoute() calls add multiple routes")
    void addRoute_multipleCallsAddMultipleRoutes() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");
      Agent agent3 = createTestAgent("Agent3");

      RouterAgent router =
              RouterAgent.builder()
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

      RouterAgent router =
              RouterAgent.builder()
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

      assertThrows(
              UnsupportedOperationException.class,
              () -> {
                router.routes().add(new RouterAgent.Route(createTestAgent("New"), "new"));
              });
    }

    @Test
    @DisplayName("Route record holds agent and description")
    void route_holdsAgentAndDescription() {
      Agent agent = createTestAgent("Test");
      RouterAgent.Route route = new RouterAgent.Route(agent, "test description");

      assertEquals(agent, route.target());
      assertEquals("test description", route.description());
    }

    @Test
    @DisplayName("Route throws when agent is null")
    void route_throwsWhenAgentNull() {
      assertThrows(
              NullPointerException.class,
              () -> {
                new RouterAgent.Route(null, "test");
              });
    }

    @Test
    @DisplayName("Route throws when description is null")
    void route_throwsWhenDescriptionNull() {
      Agent agent = createTestAgent("Test");

      assertThrows(
              NullPointerException.class,
              () -> {
                new RouterAgent.Route(agent, null);
              });
    }
  }

  @Nested
  @DisplayName("classify()")
  class Classify {

    @Test
    @DisplayName("classify() returns Optional<Agent>")
    void classify_returnsOptionalAgent() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      enqueueResponse("1"); // Mock LLM response

      Optional<Interactable> result = router.classify("test input");

      assertNotNull(result);
      assertInstanceOf(Optional.class, result);
    }

    @Test
    @DisplayName("classify() selects correct agent based on LLM response")
    void classify_selectsCorrectAgent() throws Exception {
      Agent billing = createTestAgent("Billing");
      Agent tech = createTestAgent("TechSupport");

      RouterAgent router =
              RouterAgent.builder()
                      .model("test-model")
                      .responder(responder)
                      .addRoute(billing, "billing, invoices")
                      .addRoute(tech, "technical issues")
                      .build();

      enqueueResponse("1"); // LLM says route to first agent

      Optional<Interactable> selected = router.classify("Invoice question");

      assertTrue(selected.isPresent());
      assertEquals(billing, selected.get());
    }

    @Test
    @DisplayName("classify() returns fallback when LLM response invalid")
    void classify_returnsFallbackWhenInvalid() throws Exception {
      Agent target = createTestAgent("Target");
      Agent fallback = createTestAgent("Fallback");

      RouterAgent router =
              RouterAgent.builder()
                      .model("test-model")
                      .responder(responder)
                      .addRoute(target, "test")
                      .fallback(fallback)
                      .build();

      enqueueResponse("invalid"); // LLM returns unparseable response

      Optional<Interactable> selected = router.classify("test");

      assertTrue(selected.isPresent());
      assertEquals(fallback, selected.get());
    }

    @Test
    @DisplayName("classify() returns null when no fallback and LLM invalid")
    void classify_returnsNullWhenNoFallback() throws Exception {
      Agent target = createTestAgent("Target");

      RouterAgent router =
              RouterAgent.builder()
                      .model("test-model")
                      .responder(responder)
                      .addRoute(target, "test")
                      .build();

      enqueueResponse("invalid");

      Optional<Interactable> selected = router.classify("test");

      assertTrue(selected.isEmpty());
    }

    @Test
    @DisplayName("classify() throws when input is null")
    void classify_throwsWhenInputNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.classify((String) null));
    }
  }

  @Nested
  @DisplayName("route(String)")
  class RouteString {

    @Test
    @DisplayName("route(String) returns AgentResult")
    void route_returnsAgentResult() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      // Classification response + target agent response
      enqueueResponse("1");
      enqueueSuccessResponse("Target response");

      AgentResult result = router.interact("test input");

      assertNotNull(result);
      assertInstanceOf(AgentResult.class, result);
    }

    @Test
    @DisplayName("route(String) returns error when no agent selected")
    void route_returnsErrorWhenNoAgentSelected() throws Exception {
      Agent target = createTestAgent("Target");

      RouterAgent router =
              RouterAgent.builder()
                      .model("test-model")
                      .responder(responder)
                      .addRoute(target, "test")
                      .build();

      enqueueResponse("invalid"); // No valid agent selected

      AgentResult result = router.interact("test");

      assertTrue(result.isError());
    }

    @Test
    @DisplayName("route(String) throws when input is null")
    void route_throwsWhenInputNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.interact((String) null));
    }
  }

  // Helper methods

  @Nested
  @DisplayName("route(Text)")
  class RouteText {

    @Test
    @DisplayName("route(Text) creates context and routes")
    void route_text_createsContextAndRoutes() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      enqueueResponse("1");
      enqueueSuccessResponse("Target response");

      AgentResult result = router.interact(Text.valueOf("test text input"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("route(Text) throws when text is null")
    void route_text_throwsWhenNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.interact((Text) null));
    }
  }

  @Nested
  @DisplayName("route(Message)")
  class RouteMessage {

    @Test
    @DisplayName("route(Message) creates context and routes")
    void route_message_createsContextAndRoutes() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      enqueueResponse("1");
      enqueueSuccessResponse("Target response");

      AgentResult result = router.interact(Message.user("test message input"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("route(Message) throws when message is null")
    void route_message_throwsWhenNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.interact((Message) null));
    }
  }

  @Nested
  @DisplayName("route(AgentContext)")
  class RouteContext {

    @Test
    @DisplayName("route(AgentContext) uses context for routing")
    void route_context_usesContextForRouting() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("test context input"));

      enqueueResponse("1");
      enqueueSuccessResponse("Target response");

      AgentResult result = router.interact(context);

      assertNotNull(result);
    }

    @Test
    @DisplayName("route(AgentContext) returns error when context has no user message")
    void route_context_returnsErrorWhenNoUserMessage() throws Exception {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      AgenticContext context = AgenticContext.create(); // Empty context

      AgentResult result = router.interact(context);

      assertTrue(result.isError());
    }

    @Test
    @DisplayName("route(AgentContext) throws when context is null")
    void route_context_throwsWhenNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.interact((AgenticContext) null));
    }
  }

  @Nested
  @DisplayName("routeStream()")
  class RouteStream {

    @Test
    @DisplayName("routeStream(AgentContext) returns RouterStream")
    void routeStream_context_returnsRouterStream() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("test input"));

      RouterStream stream = router.routeStream(context);

      assertNotNull(stream);
      assertInstanceOf(RouterStream.class, stream);
    }

    @Test
    @DisplayName("routeStream(AgentContext) throws when context is null")
    void routeStream_context_throwsWhenNull() {
      Agent target = createTestAgent("Target");
      RouterAgent router = createRouter(target);

      assertThrows(NullPointerException.class, () -> router.routeStream((AgenticContext) null));
    }
  }
}
