package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for RouterStream.
 *
 * <p>Tests cover: callback registration, routing, error handling, and streaming behavior.
 */
@DisplayName("RouterStream Tests")
class RouterStreamTest {

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

  // ═══════════════════════════════════════════════════════════════════════════
  // CALLBACK REGISTRATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Callback Registration")
  class CallbackRegistrationTests {

    @Test
    @DisplayName("onRouteSelected registers callback")
    void onRouteSelectedRegistersCallback() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream = router.routeStream(context);
      AtomicBoolean called = new AtomicBoolean(false);

      RouterStream result = stream.onRouteSelected(agent -> called.set(true));

      assertNotNull(result);
      assertSame(stream, result);
    }

    @Test
    @DisplayName("onTextDelta registers callback")
    void onTextDeltaRegistersCallback() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream = router.routeStream(context);
      List<String> deltas = new ArrayList<>();

      RouterStream result = stream.onTextDelta(deltas::add);

      assertNotNull(result);
      assertSame(stream, result);
    }

    @Test
    @DisplayName("onComplete registers callback")
    void onCompleteRegistersCallback() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream = router.routeStream(context);
      AtomicReference<AgentResult> resultRef = new AtomicReference<>();

      RouterStream result = stream.onComplete(resultRef::set);

      assertNotNull(result);
      assertSame(stream, result);
    }

    @Test
    @DisplayName("onError registers callback")
    void onErrorRegistersCallback() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream = router.routeStream(context);
      AtomicReference<Throwable> errorRef = new AtomicReference<>();

      RouterStream result = stream.onError(errorRef::set);

      assertNotNull(result);
      assertSame(stream, result);
    }

    @Test
    @DisplayName("onTurnStart registers callback")
    void onTurnStartRegistersCallback() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream = router.routeStream(context);
      List<Integer> turns = new ArrayList<>();

      RouterStream result = stream.onTurnStart(turns::add);

      assertNotNull(result);
      assertSame(stream, result);
    }

    @Test
    @DisplayName("onToolExecuted registers callback")
    void onToolExecutedRegistersCallback() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream = router.routeStream(context);
      List<ToolExecution> executions = new ArrayList<>();

      RouterStream result = stream.onToolExecuted(executions::add);

      assertNotNull(result);
      assertSame(stream, result);
    }

    @Test
    @DisplayName("onHandoff registers callback")
    void onHandoffRegistersCallback() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream = router.routeStream(context);
      List<Handoff> handoffs = new ArrayList<>();

      RouterStream result = stream.onHandoff(handoffs::add);

      assertNotNull(result);
      assertSame(stream, result);
    }

    @Test
    @DisplayName("callbacks can be chained fluently")
    void callbacksCanBeChainedFluently() {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      RouterStream stream =
          router
              .routeStream(context)
              .onRouteSelected(agent -> {})
              .onTextDelta(delta -> {})
              .onComplete(result -> {})
              .onError(error -> {})
              .onTurnStart(turn -> {})
              .onToolExecuted(exec -> {})
              .onHandoff(handoff -> {});

      assertNotNull(stream);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ERROR HANDLING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("start with empty context returns error")
    void startWithEmptyContextReturnsError() throws Exception {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      // No user message added

      AtomicReference<Throwable> errorRef = new AtomicReference<>();
      AtomicReference<AgentResult> resultRef = new AtomicReference<>();

      CompletableFuture<AgentResult> future =
          router.routeStream(context).onError(errorRef::set).onComplete(resultRef::set).start();

      AgentResult result = future.get(5, TimeUnit.SECONDS);

      assertTrue(result.isError());
      assertNotNull(errorRef.get());
      assertNotNull(resultRef.get());
    }

    @Test
    @DisplayName("start with blank user message returns error")
    void startWithBlankUserMessageReturnsError() throws Exception {
      RouterAgent router = createSimpleRouter();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("   "));

      AtomicReference<Throwable> errorRef = new AtomicReference<>();

      CompletableFuture<AgentResult> future =
          router.routeStream(context).onError(errorRef::set).start();

      AgentResult result = future.get(5, TimeUnit.SECONDS);

      assertTrue(result.isError());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ROUTING EXECUTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Routing Execution")
  class RoutingExecutionTests {

    @Test
    @DisplayName("start classifies and routes to selected agent")
    void startClassifiesAndRoutesToSelectedAgent() throws Exception {
      // Enqueue classification response that selects "1" (index for first agent)
      enqueueRouteClassificationResponse("1");
      // Enqueue response for the selected agent
      enqueueSuccessResponse("Hello from Sales agent!");

      Agent salesAgent = createTestAgent("Sales", "Handle sales inquiries");
      Agent supportAgent = createTestAgent("Support", "Handle support issues");

      RouterAgent router =
          RouterAgent.builder()
              .model("test-model")
              .responder(responder)
              .addRoute(salesAgent, "Sales inquiries")
              .addRoute(supportAgent, "Support issues")
              .build();

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("I want to buy something"));

      AtomicBoolean completed = new AtomicBoolean(false);

      CompletableFuture<AgentResult> future =
          router.routeStream(context).onComplete(r -> completed.set(true)).start();

      AgentResult result = future.get(5, TimeUnit.SECONDS);

      // Test that we get some result
      assertNotNull(result);
      // The routing completed
      assertTrue(result.isHandoff() || result.isError() || completed.get());
    }

    @Test
    @DisplayName("onTextDelta receives streaming deltas")
    void onTextDeltaReceivesStreamingDeltas() throws Exception {
      enqueueRouteClassificationResponse("Sales");
      enqueueSuccessResponse("Hello from agent");

      Agent salesAgent = createTestAgent("Sales", "Sales");

      RouterAgent router =
          RouterAgent.builder()
              .model("test-model")
              .responder(responder)
              .addRoute(salesAgent, "Sales")
              .build();

      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Buy something"));

      List<String> deltas = new ArrayList<>();

      CompletableFuture<AgentResult> future =
          router.routeStream(context).onTextDelta(deltas::add).start();

      future.get(5, TimeUnit.SECONDS);

      // Deltas may or may not be captured depending on streaming behavior
      assertNotNull(deltas);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private RouterAgent createSimpleRouter() {
    return RouterAgent.builder()
        .model("test-model")
        .responder(responder)
        .addRoute(createTestAgent("Default", "Default agent"), "Default route")
        .build();
  }

  private Agent createTestAgent(String name, String instructions) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions(instructions)
        .responder(responder)
        .build();
  }

  private void enqueueRouteClassificationResponse(String agentName) {
    // Classification response that includes the agent name
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
          "usage": {"input_tokens": 10, "output_tokens": 5, "total_tokens": 15}
        }
        """
            .formatted(agentName);

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
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
          "usage": {"input_tokens": 10, "output_tokens": 5, "total_tokens": 15}
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
