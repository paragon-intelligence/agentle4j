package com.paragon.agents.toolplan;

import org.jspecify.annotations.NonNull;

/**
 * A single step in a tool execution plan.
 *
 * <p>Each step specifies a tool to call and its arguments. Arguments may contain {@code
 * $ref:step_id} references to inject outputs from previously executed steps.
 *
 * <h2>Reference Syntax</h2>
 *
 * <ul>
 *   <li>{@code "$ref:step_id"} — replaced with the full output of the referenced step
 *   <li>{@code "$ref:step_id.field"} — replaced with a specific JSON field from the referenced
 *       step's output
 *   <li>{@code "$ref:step_id.field.nested"} — dot-separated paths for nested JSON field access
 * </ul>
 *
 * @param id unique identifier for this step, used by {@code $ref} references from other steps
 * @param tool the name of the {@link com.paragon.responses.spec.FunctionTool} to call
 * @param arguments JSON string of arguments to pass to the tool. May contain {@code $ref:step_id}
 *     references that are resolved before execution.
 */
public record ToolPlanStep(@NonNull String id, @NonNull String tool, @NonNull String arguments) {}
