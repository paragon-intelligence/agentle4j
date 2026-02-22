package com.paragon.agents.toolplan;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A declarative execution plan for multiple tool calls with data flow between them.
 *
 * <p>The LLM produces this as the argument to the {@link ToolPlanTool} meta-tool. The framework
 * then executes the plan locally — topologically sorting steps, running independent steps in
 * parallel, resolving {@code $ref} references, and returning only the designated output steps'
 * results back to the LLM context.
 *
 * <h2>Example Plan</h2>
 *
 * <pre>{@code
 * {
 *   "steps": [
 *     { "id": "s1", "tool": "get_weather", "arguments": "{\"location\": \"Tokyo\"}" },
 *     { "id": "s2", "tool": "get_weather", "arguments": "{\"location\": \"London\"}" },
 *     { "id": "s3", "tool": "compare_data", "arguments": "{\"a\": \"$ref:s1\", \"b\": \"$ref:s2\"}" }
 *   ],
 *   "output_steps": ["s3"]
 * }
 * }</pre>
 *
 * @param steps the list of steps to execute. Steps can reference each other via {@code $ref:step_id}
 *     syntax in their arguments.
 * @param output_steps IDs of steps whose results should be returned to the LLM. If null or empty,
 *     all step results are returned.
 */
public record ToolPlan(
    @NonNull List<ToolPlanStep> steps, @Nullable List<String> output_steps) {}
