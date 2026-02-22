package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for AgentStream builder and callback configuration. */
@DisplayName("AgentStream")
class AgentStreamTest {

  // ==================== Failed Stream Tests ====================

  @Nested
  @DisplayName("Failed Stream Factory")
  class FailedStreamTests {

    @Test
    @DisplayName("failed creates pre-failed stream")
    void failedCreatesPreFailedStream() {
      AgenticContext context = AgenticContext.create();
      AgentResult failedResult = AgentResult.error(new RuntimeException("Test error"), context, 0);

      AgentStream stream = AgentStream.failed(failedResult);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("failed stream accepts error handler")
    void failedStreamAcceptsErrorHandler() {
      AgenticContext context = AgenticContext.create();
      AgentResult failedResult = AgentResult.error(new RuntimeException("Test error"), context, 0);
      AtomicBoolean errorCalled = new AtomicBoolean(false);

      AgentStream stream = AgentStream.failed(failedResult).onError(e -> errorCalled.set(true));

      assertNotNull(stream);
    }

    @Test
    @DisplayName("failed stream accepts complete handler")
    void failedStreamAcceptsCompleteHandler() {
      AgenticContext context = AgenticContext.create();
      AgentResult failedResult = AgentResult.error(new RuntimeException("Test error"), context, 0);
      AtomicReference<AgentResult> result = new AtomicReference<>();

      AgentStream stream = AgentStream.failed(failedResult).onComplete(result::set);

      assertNotNull(stream);
    }
  }

  // ==================== Handler Chaining Tests ====================

  @Nested
  @DisplayName("Handler Chaining")
  class HandlerChainingTests {

    @Test
    @DisplayName("all handlers can be chained")
    void allHandlersCanBeChained() {
      AgenticContext context = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AgentStream stream =
          AgentStream.failed(result)
              .onTurnStart(turn -> {})
              .onTextDelta(text -> {})
              .onTurnComplete(response -> {})
              .onToolExecuted(exec -> {})
              .onGuardrailFailed(failed -> {})
              .onHandoff(handoff -> {})
              .onComplete(r -> {})
              .onError(e -> {});

      assertNotNull(stream);
    }

    @Test
    @DisplayName("handlers return same stream instance for chaining")
    void handlersReturnSameInstance() {
      AgenticContext context = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AgentStream stream = AgentStream.failed(result);
      AgentStream chain1 = stream.onTextDelta(t -> {});
      AgentStream chain2 = chain1.onComplete(r -> {});

      assertSame(stream, chain1);
      assertSame(stream, chain2);
    }

    @Test
    @DisplayName("onTurnStart returns same instance")
    void onTurnStartReturnsSameInstance() {
      AgenticContext context = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AgentStream stream = AgentStream.failed(result);
      AgentStream chained = stream.onTurnStart(turn -> {});

      assertSame(stream, chained);
    }

    @Test
    @DisplayName("onToolExecuted returns same instance")
    void onToolExecutedReturnsSameInstance() {
      AgenticContext context = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AgentStream stream = AgentStream.failed(result);
      AgentStream chained = stream.onToolExecuted(exec -> {});

      assertSame(stream, chained);
    }

    @Test
    @DisplayName("onGuardrailFailed returns same instance")
    void onGuardrailFailedReturnsSameInstance() {
      AgenticContext context = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AgentStream stream = AgentStream.failed(result);
      AgentStream chained = stream.onGuardrailFailed(failed -> {});

      assertSame(stream, chained);
    }

    @Test
    @DisplayName("onHandoff returns same instance")
    void onHandoffReturnsSameInstance() {
      AgenticContext context = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AgentStream stream = AgentStream.failed(result);
      AgentStream chained = stream.onHandoff(handoff -> {});

      assertSame(stream, chained);
    }

    @Test
    @DisplayName("onError returns same instance")
    void onErrorReturnsSameInstance() {
      AgenticContext context = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AgentStream stream = AgentStream.failed(result);
      AgentStream chained = stream.onError(e -> {});

      assertSame(stream, chained);
    }
  }
}
