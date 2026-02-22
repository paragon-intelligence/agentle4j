package com.paragon.agents.toolplan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolStore;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.StructuredTaskScope;
import org.jspecify.annotations.NonNull;

/**
 * Executes a {@link ToolPlan} against a {@link FunctionToolStore}.
 *
 * <p>The executor:
 *
 * <ol>
 *   <li>Validates the plan (duplicate IDs, unknown tools, cycles, self-references)
 *   <li>Builds a dependency graph from {@code $ref} references in step arguments
 *   <li>Topologically sorts steps into execution "waves" using Kahn's algorithm
 *   <li>Executes each wave in parallel using {@link StructuredTaskScope} (virtual threads)
 *   <li>Resolves {@code $ref} references between waves
 *   <li>Collects and returns results
 * </ol>
 *
 * <p>Error strategy is <b>fail-forward</b>: if a step fails, dependent steps are skipped, but
 * independent steps continue executing.
 */
public final class ToolPlanExecutor {

  static final String PLAN_TOOL_NAME = "execute_tool_plan";

  private final FunctionToolStore toolStore;

  public ToolPlanExecutor(@NonNull FunctionToolStore toolStore) {
    this.toolStore = Objects.requireNonNull(toolStore, "toolStore cannot be null");
  }

  /**
   * Executes the given plan and returns a {@link ToolPlanResult}.
   *
   * @param plan the plan to execute
   * @return the execution result
   * @throws ToolPlanException if the plan fails validation (duplicate IDs, cycles, unknown tools)
   */
  public @NonNull ToolPlanResult execute(@NonNull ToolPlan plan) {
    Objects.requireNonNull(plan, "plan cannot be null");

    Instant start = Instant.now();
    List<ToolPlanStep> steps = plan.steps();
    if (steps == null || steps.isEmpty()) {
      throw new ToolPlanException("Plan must contain at least one step");
    }

    // 1. Validate
    validate(steps);

    // 2. Build dependency graph and topologically sort into waves
    Map<String, Set<String>> dependencies = buildDependencyGraph(steps);
    List<List<ToolPlanStep>> waves = topologicalSort(steps, dependencies);

    // 3. Execute waves
    Map<String, String> resolvedOutputs = new LinkedHashMap<>();
    Set<String> failedSteps = new HashSet<>();
    List<ToolPlanResult.StepResult> allResults = new ArrayList<>();
    Map<String, String> errors = new LinkedHashMap<>();

    for (List<ToolPlanStep> wave : waves) {
      List<ToolPlanResult.StepResult> waveResults =
          executeWave(wave, resolvedOutputs, failedSteps, errors);
      allResults.addAll(waveResults);
    }

    // 4. Build output results
    List<ToolPlanResult.StepResult> outputResults;
    if (plan.output_steps() != null && !plan.output_steps().isEmpty()) {
      outputResults =
          allResults.stream()
              .filter(r -> plan.output_steps().contains(r.stepId()))
              .toList();
    } else {
      outputResults = List.copyOf(allResults);
    }

    Duration totalDuration = Duration.between(start, Instant.now());
    return new ToolPlanResult(
        List.copyOf(allResults), List.copyOf(outputResults), totalDuration, Map.copyOf(errors));
  }

  private void validate(List<ToolPlanStep> steps) {
    Set<String> ids = new HashSet<>();
    for (ToolPlanStep step : steps) {
      if (step.id() == null || step.id().isBlank()) {
        throw new ToolPlanException("Step ID cannot be null or blank");
      }
      if (!ids.add(step.id())) {
        throw new ToolPlanException(step.id(), "Duplicate step ID: '" + step.id() + "'");
      }
      if (step.tool() == null || step.tool().isBlank()) {
        throw new ToolPlanException(step.id(), "Step tool name cannot be null or blank");
      }
      if (PLAN_TOOL_NAME.equals(step.tool())) {
        throw new ToolPlanException(
            step.id(),
            "Step '"
                + step.id()
                + "' cannot call '"
                + PLAN_TOOL_NAME
                + "' (recursive plans are not allowed)");
      }
      if (!toolStore.contains(step.tool())) {
        throw new ToolPlanException(
            step.id(), "Unknown tool '" + step.tool() + "' in step '" + step.id() + "'");
      }
    }
  }

  private Map<String, Set<String>> buildDependencyGraph(List<ToolPlanStep> steps) {
    Set<String> stepIds = new HashSet<>();
    for (ToolPlanStep step : steps) {
      stepIds.add(step.id());
    }

    Map<String, Set<String>> deps = new LinkedHashMap<>();
    for (ToolPlanStep step : steps) {
      Set<String> stepDeps = PlanReferenceResolver.extractDependencies(step.arguments());
      // Only keep dependencies that reference actual steps in this plan
      stepDeps.retainAll(stepIds);
      deps.put(step.id(), stepDeps);
    }
    return deps;
  }

  /**
   * Topologically sorts steps into execution waves using Kahn's algorithm. Each wave contains steps
   * that can execute in parallel.
   */
  List<List<ToolPlanStep>> topologicalSort(
      List<ToolPlanStep> steps, Map<String, Set<String>> dependencies) {
    Map<String, ToolPlanStep> stepMap = new LinkedHashMap<>();
    for (ToolPlanStep step : steps) {
      stepMap.put(step.id(), step);
    }

    // Calculate in-degrees
    Map<String, Integer> inDegree = new LinkedHashMap<>();
    Map<String, Set<String>> reverseDeps = new LinkedHashMap<>();
    for (ToolPlanStep step : steps) {
      inDegree.put(step.id(), 0);
      reverseDeps.put(step.id(), new LinkedHashSet<>());
    }
    for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
      String stepId = entry.getKey();
      for (String dep : entry.getValue()) {
        inDegree.merge(stepId, 1, Integer::sum);
        reverseDeps.computeIfAbsent(dep, k -> new LinkedHashSet<>()).add(stepId);
      }
    }

    List<List<ToolPlanStep>> waves = new ArrayList<>();
    Set<String> processed = new HashSet<>();

    while (processed.size() < steps.size()) {
      // Find all steps with in-degree 0 that haven't been processed
      List<ToolPlanStep> wave = new ArrayList<>();
      for (ToolPlanStep step : steps) {
        if (!processed.contains(step.id()) && inDegree.getOrDefault(step.id(), 0) == 0) {
          wave.add(step);
        }
      }

      if (wave.isEmpty()) {
        throw new ToolPlanException("Cycle detected in tool plan dependencies");
      }

      // Process this wave: reduce in-degrees of dependent steps
      for (ToolPlanStep step : wave) {
        processed.add(step.id());
        for (String dependent : reverseDeps.getOrDefault(step.id(), Set.of())) {
          inDegree.merge(dependent, -1, Integer::sum);
        }
      }

      waves.add(wave);
    }

    return waves;
  }

  /**
   * Executes all steps in a wave in parallel using StructuredTaskScope.
   */
  private List<ToolPlanResult.StepResult> executeWave(
      List<ToolPlanStep> wave,
      Map<String, String> resolvedOutputs,
      Set<String> failedSteps,
      Map<String, String> errors) {

    if (wave.size() == 1) {
      // Single step — execute directly without StructuredTaskScope overhead
      ToolPlanStep step = wave.getFirst();
      ToolPlanResult.StepResult result =
          executeSingleStep(step, resolvedOutputs, failedSteps, errors);
      if (result.success()) {
        resolvedOutputs.put(step.id(), result.output());
      } else {
        failedSteps.add(step.id());
      }
      return List.of(result);
    }

    // Multiple steps — execute in parallel
    List<ToolPlanResult.StepResult> results = new ArrayList<>();
    try (var scope =
        StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {

      List<StructuredTaskScope.Subtask<ToolPlanResult.StepResult>> subtasks = new ArrayList<>();
      for (ToolPlanStep step : wave) {
        subtasks.add(
            scope.fork(() -> executeSingleStep(step, resolvedOutputs, failedSteps, errors)));
      }
      scope.join();

      for (StructuredTaskScope.Subtask<ToolPlanResult.StepResult> subtask : subtasks) {
        ToolPlanResult.StepResult result = subtask.get();
        results.add(result);
        if (result.success()) {
          resolvedOutputs.put(result.stepId(), result.output());
        } else {
          failedSteps.add(result.stepId());
        }
      }
    } catch (Exception e) {
      // If StructuredTaskScope fails, mark all steps in wave as failed
      for (ToolPlanStep step : wave) {
        if (!resolvedOutputs.containsKey(step.id()) && !failedSteps.contains(step.id())) {
          String error = "Parallel execution failed: " + e.getMessage();
          errors.put(step.id(), error);
          failedSteps.add(step.id());
          results.add(
              new ToolPlanResult.StepResult(
                  step.id(), step.tool(), error, Duration.ZERO, false));
        }
      }
    }

    return results;
  }

  private ToolPlanResult.StepResult executeSingleStep(
      ToolPlanStep step,
      Map<String, String> resolvedOutputs,
      Set<String> failedSteps,
      Map<String, String> errors) {

    Instant stepStart = Instant.now();

    // Check if any dependency failed
    Set<String> deps = PlanReferenceResolver.extractDependencies(step.arguments());
    for (String dep : deps) {
      if (failedSteps.contains(dep)) {
        String error =
            "Skipped because dependency '" + dep + "' failed";
        errors.put(step.id(), error);
        return new ToolPlanResult.StepResult(
            step.id(), step.tool(), error, Duration.between(stepStart, Instant.now()), false);
      }
    }

    try {
      // Resolve references
      String resolvedArgs = PlanReferenceResolver.resolve(step.arguments(), resolvedOutputs);

      // Create synthetic FunctionToolCall
      String syntheticCallId = "plan_" + step.id() + "_" + UUID.randomUUID();
      FunctionToolCall call =
          new FunctionToolCall(resolvedArgs, syntheticCallId, step.tool(), null, null);

      // Execute via tool store
      FunctionToolCallOutput output = toolStore.execute(call);
      String outputText = output.output().toString();

      Duration duration = Duration.between(stepStart, Instant.now());
      return new ToolPlanResult.StepResult(step.id(), step.tool(), outputText, duration, true);

    } catch (JsonProcessingException e) {
      String error = "Failed to deserialize arguments for tool '" + step.tool() + "': " + e.getMessage();
      errors.put(step.id(), error);
      return new ToolPlanResult.StepResult(
          step.id(), step.tool(), error, Duration.between(stepStart, Instant.now()), false);
    } catch (ToolPlanException e) {
      errors.put(step.id(), e.getMessage());
      return new ToolPlanResult.StepResult(
          step.id(), step.tool(), e.getMessage(), Duration.between(stepStart, Instant.now()), false);
    } catch (Exception e) {
      String error = "Tool execution failed: " + e.getMessage();
      errors.put(step.id(), error);
      return new ToolPlanResult.StepResult(
          step.id(), step.tool(), error, Duration.between(stepStart, Instant.now()), false);
    }
  }
}
