package com.paragon.agents.toolplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * Resolves {@code $ref:step_id} and {@code $ref:step_id.field.nested} references in tool plan step
 * argument strings.
 *
 * <p>This is a stateless utility class. All methods are static.
 *
 * <h2>Reference Syntax</h2>
 *
 * <ul>
 *   <li>{@code "$ref:step_id"} — replaced with the full output string of the referenced step. If
 *       the output is valid JSON, it is inserted unquoted. If plain text, it is inserted quoted.
 *   <li>{@code "$ref:step_id.field"} — the step output is parsed as JSON and the field is
 *       extracted. The extracted value is inserted (unquoted if JSON, quoted if string).
 *   <li>{@code "$ref:step_id.field.nested"} — dot-separated paths for deep JSON field access,
 *       translated to JSON Pointer (e.g., {@code /field/nested}).
 * </ul>
 */
public final class PlanReferenceResolver {

  /**
   * Matches: "$ref:step_id" or "$ref:step_id.field.nested" The entire match includes the
   * surrounding quotes. Group 1 = step_id, Group 2 = optional dot-separated field path.
   */
  static final Pattern REF_PATTERN =
      Pattern.compile("\"\\$ref:([a-zA-Z0-9_]+)(?:\\.([a-zA-Z0-9_.]+))?\"");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private PlanReferenceResolver() {}

  /**
   * Resolves all {@code $ref} references in the given arguments string.
   *
   * @param arguments the raw JSON arguments string from a {@link ToolPlanStep}
   * @param resolvedOutputs map of step_id to output string for each completed step
   * @return the arguments string with all references replaced
   * @throws ToolPlanException if a reference points to an unresolved step
   */
  public static @NonNull String resolve(
      @NonNull String arguments, @NonNull Map<String, String> resolvedOutputs) {
    Matcher matcher = REF_PATTERN.matcher(arguments);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String stepId = matcher.group(1);
      String fieldPath = matcher.group(2); // may be null

      String output = resolvedOutputs.get(stepId);
      if (output == null) {
        throw new ToolPlanException(
            stepId,
            "Reference to unresolved step '" + stepId + "'. "
                + "Available steps: " + resolvedOutputs.keySet());
      }

      String replacement;
      if (fieldPath != null) {
        replacement = extractField(stepId, output, fieldPath);
      } else {
        replacement = formatOutput(output);
      }

      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * Extracts dependency step IDs from the arguments string by scanning for {@code $ref:step_id}
   * patterns.
   *
   * @param arguments the raw JSON arguments string
   * @return set of step IDs that this arguments string depends on
   */
  public static @NonNull Set<String> extractDependencies(@NonNull String arguments) {
    Matcher matcher = REF_PATTERN.matcher(arguments);
    Set<String> deps = new java.util.LinkedHashSet<>();
    while (matcher.find()) {
      deps.add(matcher.group(1));
    }
    return deps;
  }

  /**
   * Extracts a field from a JSON output string using a dot-separated path.
   *
   * @return the field value formatted for insertion into JSON
   */
  private static String extractField(String stepId, String output, String fieldPath) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(output);
      // Convert dot-separated path to JSON Pointer: "field.nested" -> "/field/nested"
      String jsonPointer = "/" + fieldPath.replace('.', '/');
      JsonNode fieldNode = root.at(jsonPointer);

      if (fieldNode.isMissingNode()) {
        return "null";
      }

      if (fieldNode.isTextual()) {
        return "\"" + escapeJsonString(fieldNode.asText()) + "\"";
      }
      return fieldNode.toString();
    } catch (Exception e) {
      // Output is not valid JSON — can't extract field
      throw new ToolPlanException(
          stepId,
          "Cannot extract field '"
              + fieldPath
              + "' from step '"
              + stepId
              + "' output: not valid JSON",
          e);
    }
  }

  /**
   * Formats a step output for insertion into a JSON arguments string. If the output is valid JSON,
   * it is inserted as-is. If plain text, it is wrapped in quotes.
   */
  private static String formatOutput(String output) {
    if (output == null || output.isEmpty()) {
      return "\"\"";
    }

    // Check if output is valid JSON (object, array, number, boolean, null)
    String trimmed = output.trim();
    if (isJsonValue(trimmed)) {
      return trimmed;
    }

    // Plain text — wrap in quotes
    return "\"" + escapeJsonString(output) + "\"";
  }

  private static boolean isJsonValue(String s) {
    if (s.isEmpty()) return false;
    char first = s.charAt(0);
    // JSON object, array, or string
    if (first == '{' || first == '[' || first == '"') {
      try {
        OBJECT_MAPPER.readTree(s);
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    // JSON primitives: number, boolean, null
    if (s.equals("true") || s.equals("false") || s.equals("null")) return true;
    try {
      Double.parseDouble(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static String escapeJsonString(String s) {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
