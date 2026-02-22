package com.paragon.agents.toolplan;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolStore;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ToolPlanTool")
class ToolPlanToolTest {

  private FunctionToolStore store;
  private ToolPlanTool planTool;

  record EchoParams(@NonNull String message) {}

  static class EchoTool extends FunctionTool<EchoParams> {
    @Override
    public @NonNull String getName() {
      return "echo";
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EchoParams params) {
      if (params == null) return FunctionToolCallOutput.error("null");
      return FunctionToolCallOutput.success(params.message());
    }
  }

  @BeforeEach
  void setUp() {
    store = FunctionToolStore.create();
    store.add(new EchoTool());
    planTool = new ToolPlanTool(store);
    // Do NOT add planTool to store in tests (Agent does that in production)
  }

  @Nested
  @DisplayName("Metadata")
  class Metadata {

    @Test
    @DisplayName("has correct tool name")
    void name() {
      assertEquals("execute_tool_plan", planTool.getName());
    }

    @Test
    @DisplayName("has non-null description")
    void description() {
      assertNotNull(planTool.getDescription());
      assertTrue(planTool.getDescription().contains("plan"));
    }

    @Test
    @DisplayName("has non-empty parameter schema")
    void schema() {
      assertNotNull(planTool.getParameters());
      assertFalse(planTool.getParameters().isEmpty());
      assertEquals("object", planTool.getParameters().get("type"));
    }

    @Test
    @DisplayName("is strict mode")
    void strictMode() {
      assertTrue(planTool.getStrict());
    }
  }

  @Nested
  @DisplayName("call()")
  class Call {

    @Test
    @DisplayName("returns success for valid plan")
    void validPlan() {
      ToolPlan plan =
          new ToolPlan(
              List.of(new ToolPlanStep("s1", "echo", "{\"message\": \"hello\"}")),
              null);

      FunctionToolCallOutput result = planTool.call(plan);

      assertNotNull(result);
      assertTrue(result.output().toString().contains("hello"));
    }

    @Test
    @DisplayName("returns error for null plan")
    void nullPlan() {
      FunctionToolCallOutput result = planTool.call(null);

      assertNotNull(result);
      assertTrue(result.output().toString().contains("Error"));
    }

    @Test
    @DisplayName("returns error for empty steps")
    void emptySteps() {
      ToolPlan plan = new ToolPlan(List.of(), null);

      FunctionToolCallOutput result = planTool.call(plan);

      assertNotNull(result);
      assertTrue(result.output().toString().contains("Error"));
    }

    @Test
    @DisplayName("returns error for unknown tool in plan")
    void unknownTool() {
      ToolPlan plan =
          new ToolPlan(
              List.of(new ToolPlanStep("s1", "nonexistent", "{}")), null);

      FunctionToolCallOutput result = planTool.call(plan);

      assertNotNull(result);
      assertTrue(result.output().toString().contains("Error"));
    }

    @Test
    @DisplayName("executes multi-step plan with references")
    void multiStepWithRefs() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"data\"}"),
                  new ToolPlanStep(
                      "s2", "echo", "{\"message\": \"$ref:s1\"}")),
              List.of("s2"));

      FunctionToolCallOutput result = planTool.call(plan);

      assertNotNull(result);
      assertTrue(result.output().toString().contains("data"));
    }
  }
}
