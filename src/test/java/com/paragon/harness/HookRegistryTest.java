package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.AgenticContext;
import com.paragon.agents.AgentResult;
import com.paragon.agents.ToolExecution;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HookRegistry")
class HookRegistryTest {

  @Nested
  @DisplayName("fireBeforeRun")
  class FireBeforeRun {

    @Test
    @DisplayName("calls all hooks in registration order")
    void callsHooksInOrder() {
      List<String> callOrder = new ArrayList<>();
      HookRegistry registry = HookRegistry.create()
          .add(new AgentHook() {
            @Override public void beforeRun(AgenticContext ctx) { callOrder.add("hook1"); }
          })
          .add(new AgentHook() {
            @Override public void beforeRun(AgenticContext ctx) { callOrder.add("hook2"); }
          });

      registry.fireBeforeRun(AgenticContext.create());

      assertEquals(List.of("hook1", "hook2"), callOrder);
    }
  }

  @Nested
  @DisplayName("fireAfterRun")
  class FireAfterRun {

    @Test
    @DisplayName("calls hooks in reverse order (stack semantics)")
    void callsHooksInReverseOrder() {
      List<String> callOrder = new ArrayList<>();
      HookRegistry registry = HookRegistry.create()
          .add(new AgentHook() {
            @Override public void afterRun(AgentResult r, AgenticContext ctx) { callOrder.add("hook1"); }
          })
          .add(new AgentHook() {
            @Override public void afterRun(AgentResult r, AgenticContext ctx) { callOrder.add("hook2"); }
          });

      AgenticContext ctx = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException("test"), ctx, 0);
      registry.fireAfterRun(result, ctx);

      assertEquals(List.of("hook2", "hook1"), callOrder);
    }
  }

  @Nested
  @DisplayName("fireBeforeToolCall and fireAfterToolCall")
  class ToolCallHooks {

    @Test
    @DisplayName("fires beforeToolCall and afterToolCall")
    void firesToolCallHooks() {
      List<String> events = new ArrayList<>();
      HookRegistry registry = HookRegistry.create()
          .add(new AgentHook() {
            @Override
            public void beforeToolCall(FunctionToolCall call, AgenticContext ctx) {
              events.add("before:" + call.name());
            }
            @Override
            public void afterToolCall(FunctionToolCall call, ToolExecution exec, AgenticContext ctx) {
              events.add("after:" + call.name());
            }
          });

      FunctionToolCall call = new FunctionToolCall("{}", "call-1", "my_tool", null, null);
      FunctionToolCallOutput output = FunctionToolCallOutput.success("ok");
      ToolExecution exec = new ToolExecution("my_tool", "call-1", "{}", output, Duration.ofMillis(10));

      AgenticContext ctx = AgenticContext.create();
      registry.fireBeforeToolCall(call, ctx);
      registry.fireAfterToolCall(call, exec, ctx);

      assertEquals(List.of("before:my_tool", "after:my_tool"), events);
    }
  }

  @Test
  @DisplayName("hook failures are swallowed and do not propagate")
  void hookFailuresAreSwallowed() {
    HookRegistry registry = HookRegistry.create()
        .add(new AgentHook() {
          @Override
          public void beforeRun(AgenticContext ctx) {
            throw new RuntimeException("hook crashed!");
          }
        });

    // Must not throw
    assertDoesNotThrow(() -> registry.fireBeforeRun(AgenticContext.create()));
  }

  @Test
  @DisplayName("HookRegistry.of() creates registry with given hooks")
  void ofCreatesWithHooks() {
    AgentHook hook1 = new AgentHook() {};
    AgentHook hook2 = new AgentHook() {};
    HookRegistry registry = HookRegistry.of(hook1, hook2);
    assertEquals(2, registry.size());
  }
}
