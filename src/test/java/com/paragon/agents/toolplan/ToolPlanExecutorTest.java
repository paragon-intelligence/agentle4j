package com.paragon.agents.toolplan;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolStore;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ToolPlanExecutor")
class ToolPlanExecutorTest {

  private FunctionToolStore store;
  private ToolPlanExecutor executor;

  // ===== Test Tools =====

  record EchoParams(@NonNull String message) {}

  static class EchoTool extends FunctionTool<EchoParams> {
    @Override
    public @NonNull String getName() {
      return "echo";
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EchoParams params) {
      if (params == null) return FunctionToolCallOutput.error("null params");
      return FunctionToolCallOutput.success(params.message());
    }
  }

  record WeatherParams(@NonNull String location) {}

  static class WeatherTool extends FunctionTool<WeatherParams> {
    @Override
    public @NonNull String getName() {
      return "get_weather";
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable WeatherParams params) {
      if (params == null) return FunctionToolCallOutput.error("null params");
      return FunctionToolCallOutput.success(
          "{\"temp\": 25, \"city\": \"" + params.location() + "\"}");
    }
  }

  record ConcatParams(@NonNull String a, @NonNull String b) {}

  static class ConcatTool extends FunctionTool<ConcatParams> {
    @Override
    public @NonNull String getName() {
      return "concat";
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable ConcatParams params) {
      if (params == null) return FunctionToolCallOutput.error("null params");
      return FunctionToolCallOutput.success(params.a() + " + " + params.b());
    }
  }

  record FailParams() {}

  static class FailingTool extends FunctionTool<FailParams> {
    @Override
    public @NonNull String getName() {
      return "fail_tool";
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable FailParams params) {
      throw new RuntimeException("Intentional failure");
    }
  }

  @BeforeEach
  void setUp() {
    store = FunctionToolStore.create();
    store.add(new EchoTool());
    store.add(new WeatherTool());
    store.add(new ConcatTool());
    store.add(new FailingTool());
    executor = new ToolPlanExecutor(store);
  }

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("throws for empty steps list")
    void emptySteps() {
      ToolPlan plan = new ToolPlan(List.of(), null);
      assertThrows(ToolPlanException.class, () -> executor.execute(plan));
    }

    @Test
    @DisplayName("throws for null steps list")
    void nullSteps() {
      ToolPlan plan = new ToolPlan(null, null);
      assertThrows(Exception.class, () -> executor.execute(plan));
    }

    @Test
    @DisplayName("throws for duplicate step IDs")
    void duplicateIds() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"a\"}"),
                  new ToolPlanStep("s1", "echo", "{\"message\": \"b\"}")),
              null);

      ToolPlanException ex =
          assertThrows(ToolPlanException.class, () -> executor.execute(plan));
      assertEquals("s1", ex.stepId());
      assertTrue(ex.getMessage().contains("Duplicate"));
    }

    @Test
    @DisplayName("throws for unknown tool name")
    void unknownTool() {
      ToolPlan plan =
          new ToolPlan(
              List.of(new ToolPlanStep("s1", "nonexistent_tool", "{}")), null);

      ToolPlanException ex =
          assertThrows(ToolPlanException.class, () -> executor.execute(plan));
      assertEquals("s1", ex.stepId());
      assertTrue(ex.getMessage().contains("Unknown tool"));
    }

    @Test
    @DisplayName("throws for recursive plan tool reference")
    void recursivePlan() {
      ToolPlan plan =
          new ToolPlan(
              List.of(new ToolPlanStep("s1", "execute_tool_plan", "{}")), null);

      ToolPlanException ex =
          assertThrows(ToolPlanException.class, () -> executor.execute(plan));
      assertTrue(ex.getMessage().contains("recursive"));
    }
  }

  @Nested
  @DisplayName("Single Step Execution")
  class SingleStep {

    @Test
    @DisplayName("executes a single step successfully")
    void singleStep() {
      ToolPlan plan =
          new ToolPlan(
              List.of(new ToolPlanStep("s1", "echo", "{\"message\": \"hello\"}")), null);

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.hasErrors());
      assertEquals(1, result.stepResults().size());
      assertEquals(1, result.outputResults().size());
      assertEquals("hello", result.stepResults().getFirst().output());
      assertTrue(result.stepResults().getFirst().success());
    }

    @Test
    @DisplayName("handles tool that returns JSON")
    void jsonOutput() {
      ToolPlan plan =
          new ToolPlan(
              List.of(new ToolPlanStep("s1", "get_weather", "{\"location\": \"Tokyo\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.hasErrors());
      assertTrue(result.stepResults().getFirst().output().contains("Tokyo"));
    }
  }

  @Nested
  @DisplayName("Sequential Dependencies")
  class SequentialDeps {

    @Test
    @DisplayName("resolves $ref between sequential steps")
    void refResolution() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"hello\"}"),
                  new ToolPlanStep(
                      "s2", "concat", "{\"a\": \"$ref:s1\", \"b\": \"world\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.hasErrors());
      assertEquals(2, result.stepResults().size());
      assertEquals("hello + world", result.stepResults().get(1).output());
    }

    @Test
    @DisplayName("resolves field path $ref between steps")
    void fieldPathRef() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "get_weather", "{\"location\": \"Tokyo\"}"),
                  new ToolPlanStep(
                      "s2", "echo", "{\"message\": \"$ref:s1.city\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.hasErrors());
      assertEquals("Tokyo", result.stepResults().get(1).output());
    }

    @Test
    @DisplayName("chains three sequential steps")
    void threeStepChain() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"A\"}"),
                  new ToolPlanStep(
                      "s2", "concat", "{\"a\": \"$ref:s1\", \"b\": \"B\"}"),
                  new ToolPlanStep(
                      "s3", "concat", "{\"a\": \"$ref:s2\", \"b\": \"C\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.hasErrors());
      assertEquals(3, result.stepResults().size());
      assertEquals("A + B + C", result.stepResults().get(2).output());
    }
  }

  @Nested
  @DisplayName("Parallel Execution")
  class ParallelExec {

    @Test
    @DisplayName("runs independent steps in parallel (same wave)")
    void independentStepsParallel() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"first\"}"),
                  new ToolPlanStep("s2", "echo", "{\"message\": \"second\"}"),
                  new ToolPlanStep("s3", "echo", "{\"message\": \"third\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.hasErrors());
      assertEquals(3, result.stepResults().size());
      // All three should complete (order may vary in results list)
      Set<String> outputs = new java.util.HashSet<>();
      for (ToolPlanResult.StepResult r : result.stepResults()) {
        outputs.add(r.output());
      }
      assertTrue(outputs.contains("first"));
      assertTrue(outputs.contains("second"));
      assertTrue(outputs.contains("third"));
    }

    @Test
    @DisplayName("diamond dependency: A and B parallel, C depends on both")
    void diamondDependency() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("a", "echo", "{\"message\": \"alpha\"}"),
                  new ToolPlanStep("b", "echo", "{\"message\": \"beta\"}"),
                  new ToolPlanStep(
                      "c", "concat", "{\"a\": \"$ref:a\", \"b\": \"$ref:b\"}")),
              List.of("c"));

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.hasErrors());
      assertEquals(3, result.stepResults().size());
      assertEquals(1, result.outputResults().size());
      assertEquals("alpha + beta", result.outputResults().getFirst().output());
    }
  }

  @Nested
  @DisplayName("Cycle Detection")
  class CycleDetection {

    @Test
    @DisplayName("detects direct cycle between two steps")
    void directCycle() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "concat", "{\"a\": \"$ref:s2\", \"b\": \"x\"}"),
                  new ToolPlanStep("s2", "concat", "{\"a\": \"$ref:s1\", \"b\": \"y\"}")),
              null);

      ToolPlanException ex =
          assertThrows(ToolPlanException.class, () -> executor.execute(plan));
      assertTrue(ex.getMessage().contains("Cycle"));
    }

    @Test
    @DisplayName("detects self-referencing step")
    void selfReference() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"$ref:s1\"}")),
              null);

      ToolPlanException ex =
          assertThrows(ToolPlanException.class, () -> executor.execute(plan));
      assertTrue(ex.getMessage().contains("Cycle"));
    }
  }

  @Nested
  @DisplayName("Output Steps Filtering")
  class OutputSteps {

    @Test
    @DisplayName("returns only designated output_steps")
    void filterByOutputSteps() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"intermediate\"}"),
                  new ToolPlanStep("s2", "echo", "{\"message\": \"final\"}")),
              List.of("s2"));

      ToolPlanResult result = executor.execute(plan);

      assertEquals(2, result.stepResults().size());
      assertEquals(1, result.outputResults().size());
      assertEquals("s2", result.outputResults().getFirst().stepId());
      assertEquals("final", result.outputResults().getFirst().output());
    }

    @Test
    @DisplayName("returns all results when output_steps is null")
    void nullOutputSteps() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"a\"}"),
                  new ToolPlanStep("s2", "echo", "{\"message\": \"b\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertEquals(2, result.outputResults().size());
    }

    @Test
    @DisplayName("returns all results when output_steps is empty")
    void emptyOutputSteps() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"a\"}"),
                  new ToolPlanStep("s2", "echo", "{\"message\": \"b\"}")),
              List.of());

      ToolPlanResult result = executor.execute(plan);

      assertEquals(2, result.outputResults().size());
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandling {

    @Test
    @DisplayName("captures step failure and continues independent steps")
    void failForwardIndependent() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "fail_tool", "{}"),
                  new ToolPlanStep("s2", "echo", "{\"message\": \"ok\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertTrue(result.hasErrors());
      assertTrue(result.errors().containsKey("s1"));

      // s2 should still succeed
      ToolPlanResult.StepResult s2 =
          result.stepResults().stream()
              .filter(r -> r.stepId().equals("s2"))
              .findFirst()
              .orElseThrow();
      assertTrue(s2.success());
      assertEquals("ok", s2.output());
    }

    @Test
    @DisplayName("skips dependent step when dependency fails")
    void skipDependentOnFailure() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "fail_tool", "{}"),
                  new ToolPlanStep(
                      "s2", "echo", "{\"message\": \"$ref:s1\"}")),
              null);

      ToolPlanResult result = executor.execute(plan);

      assertTrue(result.hasErrors());
      assertTrue(result.errors().containsKey("s1"));
      assertTrue(result.errors().containsKey("s2"));

      ToolPlanResult.StepResult s2 =
          result.stepResults().stream()
              .filter(r -> r.stepId().equals("s2"))
              .findFirst()
              .orElseThrow();
      assertFalse(s2.success());
      assertTrue(s2.output().contains("dependency"));
    }
  }

  @Nested
  @DisplayName("Topological Sort")
  class TopologicalSort {

    @Test
    @DisplayName("produces correct waves for linear chain")
    void linearChain() {
      List<ToolPlanStep> steps =
          List.of(
              new ToolPlanStep("s1", "echo", "{}"),
              new ToolPlanStep("s2", "echo", "{\"x\": \"$ref:s1\"}"),
              new ToolPlanStep("s3", "echo", "{\"x\": \"$ref:s2\"}"));

      Map<String, Set<String>> deps =
          Map.of(
              "s1", Set.of(),
              "s2", Set.of("s1"),
              "s3", Set.of("s2"));

      List<List<ToolPlanStep>> waves = executor.topologicalSort(steps, deps);

      assertEquals(3, waves.size());
      assertEquals(1, waves.get(0).size());
      assertEquals("s1", waves.get(0).getFirst().id());
      assertEquals("s2", waves.get(1).getFirst().id());
      assertEquals("s3", waves.get(2).getFirst().id());
    }

    @Test
    @DisplayName("groups independent steps into same wave")
    void parallelWave() {
      List<ToolPlanStep> steps =
          List.of(
              new ToolPlanStep("s1", "echo", "{}"),
              new ToolPlanStep("s2", "echo", "{}"),
              new ToolPlanStep("s3", "echo", "{\"x\": \"$ref:s1\"}"));

      Map<String, Set<String>> deps =
          Map.of(
              "s1", Set.of(),
              "s2", Set.of(),
              "s3", Set.of("s1"));

      List<List<ToolPlanStep>> waves = executor.topologicalSort(steps, deps);

      assertEquals(2, waves.size());
      assertEquals(2, waves.get(0).size()); // s1 and s2 in same wave
      assertEquals(1, waves.get(1).size()); // s3 in second wave
    }
  }

  @Nested
  @DisplayName("ToolPlanResult")
  class ResultTests {

    @Test
    @DisplayName("toOutputSummary produces valid JSON-like format")
    void outputSummary() {
      ToolPlan plan =
          new ToolPlan(
              List.of(
                  new ToolPlanStep("s1", "echo", "{\"message\": \"hello\"}"),
                  new ToolPlanStep("s2", "echo", "{\"message\": \"world\"}")),
              List.of("s2"));

      ToolPlanResult result = executor.execute(plan);
      String summary = result.toOutputSummary();

      assertNotNull(summary);
      assertTrue(summary.contains("s2"));
      assertTrue(summary.contains("world"));
    }

    @Test
    @DisplayName("totalDuration is non-negative")
    void durationNonNegative() {
      ToolPlan plan =
          new ToolPlan(
              List.of(new ToolPlanStep("s1", "echo", "{\"message\": \"hi\"}")), null);

      ToolPlanResult result = executor.execute(plan);

      assertFalse(result.totalDuration().isNegative());
    }
  }
}
