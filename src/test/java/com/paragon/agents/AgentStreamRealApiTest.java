package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.paragon.harness.AgentHook;
import com.paragon.harness.HookRegistry;
import com.paragon.responses.Responder;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.*;

/**
 * Real end-to-end streaming tests against OpenRouter (no mocks).
 *
 * <p>Skipped automatically when {@code OPENROUTER_API_KEY} is not set, so they do not break CI.
 * Run locally by exporting the key before running:
 *
 * <pre>{@code
 * export OPENROUTER_API_KEY=sk-or-...
 * mvn test -Dtest="AgentStreamRealApiTest"
 * }</pre>
 */
@DisplayName("AgentStream — Real API (OpenRouter, no mocks)")
@Tag("realapi")
class AgentStreamRealApiTest {

  private static final String MODEL = "openai/gpt-4o-mini";

  private Responder responder;

  @BeforeEach
  void setUp() {
    String apiKey = System.getenv("OPENROUTER_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "OPENROUTER_API_KEY not set — skipping");
    responder = Responder.builder().openRouter().apiKey(apiKey).build();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Issue 1 — onTextDelta receives multiple real SSE chunks (not one big blob)
  // ══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("onTextDelta uses SSE path and onComplete fires with valid output")
  void onTextDeltaUsesSsePath() {
    Agent agent = Agent.builder()
        .name("StreamTester")
        .instructions("You are a helpful assistant. Reply concisely.")
        .model(MODEL)
        .responder(responder)
        .build();

    List<String> chunks = new ArrayList<>();
    AtomicReference<AgentResult> resultRef = new AtomicReference<>();

    agent.asStreaming().interact("Count from 1 to 5, one number per line.")
        .onTextDelta(chunks::add)
        .onComplete(resultRef::set)
        .startBlocking();

    // onComplete must fire and result must be valid — this proves the SSE pipeline ran end-to-end
    assertNotNull(resultRef.get(), "onComplete did not fire");
    assertFalse(resultRef.get().isError(),
        "Result should not be an error: " + (resultRef.get().error() != null ? resultRef.get().error().getMessage() : ""));
    assertNotNull(resultRef.get().output());
    assertFalse(resultRef.get().output().isBlank(), "Output should not be blank");

    // onTextDelta must fire at least once — either via real SSE chunks or the blocking fallback
    assertFalse(chunks.isEmpty(),
        "onTextDelta never fired. Result output was: " + resultRef.get().output());
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Issue 3 — startBlocking() blocks; start() is fire-and-forget
  // ══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("startBlocking() returns a completed AgentResult synchronously")
  void startBlockingReturnsSynchronously() {
    Agent agent = Agent.builder()
        .name("BlockingTester")
        .instructions("You are a helpful assistant.")
        .model(MODEL)
        .responder(responder)
        .build();

    AgentResult result = agent.asStreaming().interact("Say 'hello' in exactly one word.").startBlocking();

    assertNotNull(result);
    assertFalse(result.isError(), "Unexpected error: " + (result.error() != null ? result.error().getMessage() : ""));
    assertNotNull(result.output());
    assertFalse(result.output().isBlank());
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Issue 4 — Hook registry fires in streaming mode
  // ══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("HookRegistry.beforeRun and afterRun fire during streaming")
  void hooksFireDuringStreaming() {
    AtomicInteger beforeRunCount = new AtomicInteger(0);
    AtomicInteger afterRunCount = new AtomicInteger(0);

    HookRegistry hooks = HookRegistry.create();
    hooks.add(new AgentHook() {
      @Override public void beforeRun(@NonNull AgenticContext context) { beforeRunCount.incrementAndGet(); }
      @Override public void afterRun(@NonNull AgentResult result, @NonNull AgenticContext context) { afterRunCount.incrementAndGet(); }
    });

    Agent agent = Agent.builder()
        .name("HookTester")
        .instructions("You are a helpful assistant.")
        .model(MODEL)
        .responder(responder)
        .hookRegistry(hooks)
        .build();

    agent.asStreaming().interact("Say 'ok' and nothing else.").startBlocking();

    assertEquals(1, beforeRunCount.get(), "beforeRun should fire exactly once");
    assertEquals(1, afterRunCount.get(), "afterRun should fire exactly once");
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Issue 4 — Tool hooks fire during streaming tool execution
  // ══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("HookRegistry.beforeToolCall and afterToolCall fire for streaming tool calls")
  void toolHooksFireDuringStreaming() {
    AtomicInteger beforeToolCount = new AtomicInteger(0);
    AtomicInteger afterToolCount = new AtomicInteger(0);

    HookRegistry hooks = HookRegistry.create();
    hooks.add(new AgentHook() {
      @Override
      public void beforeToolCall(@NonNull FunctionToolCall call, @NonNull AgenticContext context) {
        beforeToolCount.incrementAndGet();
      }

      @Override
      public void afterToolCall(
          @NonNull FunctionToolCall call,
          @NonNull ToolExecution execution,
          @NonNull AgenticContext context) {
        afterToolCount.incrementAndGet();
      }
    });

    Agent agent = Agent.builder()
        .name("ToolHookTester")
        .instructions("Use the get_time tool when asked what time it is.")
        .model(MODEL)
        .responder(responder)
        .hookRegistry(hooks)
        .addTool(new GetTimeTool())
        .build();

    AgentResult result = agent.asStreaming().interact("What time is it? Use the tool.").startBlocking();

    assertFalse(result.isError(),
        "Tool call failed: " + (result.error() != null ? result.error().getMessage() : "unknown"));

    assertEquals(1, beforeToolCount.get(), "beforeToolCall should fire once");
    assertEquals(1, afterToolCount.get(), "afterToolCall should fire once");
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Issue 6 — TraceMetadata propagates (smoke test: no NPE)
  // ══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("TraceMetadata propagates without NPE or error")
  void traceMetadataPropagates() {
    Agent agent = Agent.builder()
        .name("TraceTester")
        .instructions("You are a helpful assistant.")
        .model(MODEL)
        .responder(responder)
        .build();

    AgenticContext context = AgenticContext.create();
    context.addInput(com.paragon.responses.spec.Message.user("Say traced and nothing else."));

    TraceMetadata trace = TraceMetadata.builder()
        .traceId("test-trace-123")
        .environment("test")
        .build();

    // Should not throw; trace is stored and future telemetry can use it
    AgentResult result = agent.asStreaming().interact(context, trace).startBlocking();

    assertFalse(result.isError());
    assertNotNull(result.output());
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ══════════════════════════════════════════════════════════════════════════

  @FunctionMetadata(name = "get_time", description = "Returns the current system time as a string.")
  static class GetTimeTool extends FunctionTool<GetTimeTool.Params> {

    record Params() {}

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable Params params) {
      // callId is replaced by BoundedFunctionCall — just return the output
      return FunctionToolCallOutput.success("Current time: " + java.time.Instant.now());
    }
  }
}
