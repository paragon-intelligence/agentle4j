package com.paragon.agents.toolplan;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * The result of executing a {@link ToolPlan}.
 *
 * @param stepResults all individual step results in execution order
 * @param outputResults only the results from output_steps (or all if output_steps was empty)
 * @param totalDuration wall-clock time for the entire plan execution
 * @param errors map of step ID to error message for any failed steps
 */
public record ToolPlanResult(
    @NonNull List<StepResult> stepResults,
    @NonNull List<StepResult> outputResults,
    @NonNull Duration totalDuration,
    @NonNull Map<String, String> errors) {

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Returns a formatted JSON summary of the output step results, suitable for returning to the LLM
   * context.
   *
   * @return JSON string with step IDs mapped to their outputs
   */
  public @NonNull String toOutputSummary() {
    if (outputResults.isEmpty()) {
      return "{}";
    }

    Map<String, Object> summary = new LinkedHashMap<>();
    for (StepResult result : outputResults) {
      if (result.success()) {
        summary.put(result.stepId(), result.output());
      } else {
        summary.put(result.stepId(), "ERROR: " + result.output());
      }
    }

    // Build JSON manually to avoid ObjectMapper dependency
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : summary.entrySet()) {
      if (!first) sb.append(", ");
      first = false;
      sb.append("\"").append(escapeJson(entry.getKey())).append("\": ");
      String value = String.valueOf(entry.getValue());
      // If value looks like JSON, insert as-is; otherwise quote it
      String trimmed = value.trim();
      if (isJsonStructure(trimmed)) {
        sb.append(trimmed);
      } else {
        sb.append("\"").append(escapeJson(value)).append("\"");
      }
    }
    sb.append("}");

    return sb.toString();
  }

  private static boolean isJsonStructure(String s) {
    if (s.isEmpty()) return false;
    char first = s.charAt(0);
    return first == '{' || first == '[';
  }

  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /**
   * Result of a single step execution.
   *
   * @param stepId the step identifier
   * @param toolName the tool that was called
   * @param output the tool output (text content)
   * @param duration execution time for this step
   * @param success true if the step completed successfully
   */
  public record StepResult(
      @NonNull String stepId,
      @NonNull String toolName,
      @NonNull String output,
      @NonNull Duration duration,
      boolean success) {}
}
