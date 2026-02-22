package com.paragon.agents.toolplan;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolStore;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A meta-tool that allows the LLM to batch multiple tool calls into a single declarative execution
 * plan.
 *
 * <p>When the LLM calls this tool, the framework locally executes the plan — topologically sorting
 * steps, running independent steps in parallel, resolving {@code $ref} references — and returns
 * only the designated output steps' results. Intermediate results never touch the LLM context,
 * saving tokens and reducing latency.
 *
 * <h2>How It Works</h2>
 *
 * <ol>
 *   <li>The LLM produces a {@link ToolPlan} as the argument to this tool
 *   <li>The framework validates the plan, builds a dependency graph, and sorts steps into waves
 *   <li>Each wave of independent steps executes in parallel using virtual threads
 *   <li>{@code $ref:step_id} references are resolved between waves with actual outputs
 *   <li>Only the designated {@code output_steps} results are returned to the LLM
 * </ol>
 *
 * <h2>Usage</h2>
 *
 * <p>This tool is registered automatically when {@code Agent.Builder.enableToolPlanning()} is
 * called. It should not be created manually.
 *
 * @see ToolPlan
 * @see ToolPlanExecutor
 */
@FunctionMetadata(
    name = "execute_tool_plan",
    description =
        "Execute a plan of multiple tool calls with data flow between them. "
            + "Use this when you need to call multiple tools where some depend on results of others, "
            + "or when you want to run independent tool calls in parallel for efficiency. "
            + "Each step has an id, a tool name, and arguments (a JSON string). "
            + "Use \"$ref:step_id\" in arguments to reference the full output of a previous step. "
            + "Use \"$ref:step_id.field\" to extract a specific JSON field from a previous step's output. "
            + "List which step IDs you need in output_steps (or omit for all results).")
public final class ToolPlanTool extends FunctionTool<ToolPlan> {

  private final @NonNull FunctionToolStore toolStore;

  /**
   * Creates a ToolPlanTool that executes plans against the given tool store.
   *
   * <p>The tool store reference is used lazily at execution time (in {@link #call}), not at
   * construction time. This allows the ToolPlanTool itself to be registered in the same store after
   * construction.
   *
   * @param toolStore the store containing all available tools
   */
  @SuppressWarnings("unchecked")
  public ToolPlanTool(@NonNull FunctionToolStore toolStore) {
    super(
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "steps",
                Map.of(
                    "type",
                    "array",
                    "description",
                    "The ordered list of tool call steps to execute",
                    "items",
                    Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of(
                            "id",
                            Map.of(
                                "type",
                                "string",
                                "description",
                                "Unique identifier for this step, used by $ref references"),
                            "tool",
                            Map.of(
                                "type",
                                "string",
                                "description",
                                "The name of the function tool to call"),
                            "arguments",
                            Map.of(
                                "type",
                                "string",
                                "description",
                                "JSON string of arguments for the tool. May contain \"$ref:step_id\" "
                                    + "to reference previous step output or \"$ref:step_id.field\" "
                                    + "to extract a specific JSON field")),
                        "required",
                        List.of("id", "tool", "arguments"),
                        "additionalProperties",
                        false)),
                "output_steps",
                Map.of(
                    "type",
                    "array",
                    "description",
                    "IDs of steps whose results should be returned. Omit or leave empty to return all results.",
                    "items",
                    Map.of("type", "string"))),
            "required",
            List.of("steps"),
            "additionalProperties",
            false),
        true);

    this.toolStore = Objects.requireNonNull(toolStore, "toolStore cannot be null");
  }

  @Override
  public @NonNull FunctionToolCallOutput call(@Nullable ToolPlan plan) {
    if (plan == null || plan.steps() == null || plan.steps().isEmpty()) {
      return FunctionToolCallOutput.error("Tool plan must contain at least one step");
    }

    try {
      ToolPlanExecutor executor = new ToolPlanExecutor(toolStore);
      ToolPlanResult result = executor.execute(plan);

      if (result.hasErrors()) {
        // Include both results and errors in the output
        String summary = result.toOutputSummary();
        String errorSummary =
            result.errors().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
        return FunctionToolCallOutput.success(
            "Plan completed with errors.\nResults: " + summary + "\nErrors: " + errorSummary);
      }

      return FunctionToolCallOutput.success(result.toOutputSummary());
    } catch (ToolPlanException e) {
      return FunctionToolCallOutput.error("Plan execution failed: " + e.getMessage());
    } catch (Exception e) {
      return FunctionToolCallOutput.error("Unexpected error during plan execution: " + e.getMessage());
    }
  }
}
