package com.paragon.responses.streaming;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A lenient JSON parser that attempts to parse incomplete JSON strings. Used for real-time partial
 * parsing during structured output streaming.
 *
 * <p>The parser "completes" incomplete JSON by:
 *
 * <ul>
 *   <li>Closing unclosed strings with quotes
 *   <li>Adding missing closing braces and brackets
 *   <li>Handling trailing commas
 * </ul>
 *
 * <p><b>Note:</b> The target class must have all fields as {@code @Nullable} or use wrapper types
 * (e.g., {@code Integer} instead of {@code int}) to accept partially-filled objects.
 *
 * @param <T> the target type to parse into
 */
public class PartialJsonParser<T> {

  private final ObjectMapper objectMapper;
  private final Class<T> targetType;

  /**
   * Creates a new PartialJsonParser for the given type.
   *
   * @param objectMapper the ObjectMapper to use for parsing
   * @param targetType the class to parse into
   */
  public PartialJsonParser(@NonNull ObjectMapper objectMapper, @NonNull Class<T> targetType) {
    // Create a copy configured for lenient parsing
    this.objectMapper =
        objectMapper
            .copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    this.targetType = targetType;
  }

  /**
   * Attempts to parse partial/incomplete JSON, returning an instance with available fields.
   *
   * @param incompleteJson the incomplete JSON string (may be cut off mid-stream)
   * @return a partially-filled instance, or null if not enough data to parse
   */
  public @Nullable T parsePartial(@NonNull String incompleteJson) {
    if (incompleteJson == null || incompleteJson.isBlank()) {
      return null;
    }

    String completedJson = completeJson(incompleteJson.trim());
    if (completedJson == null) {
      return null;
    }

    try {
      return objectMapper.readValue(completedJson, targetType);
    } catch (Exception e) {
      // Not parseable yet - return null
      return null;
    }
  }

  /**
   * Static utility method to parse incomplete JSON directly to a Map.
   *
   * <p>This is the "zero-class" approach - no need to define a partial class with nullable fields.
   *
   * @param objectMapper the ObjectMapper to use
   * @param incompleteJson the incomplete JSON string
   * @return a Map containing available fields, or null if not parseable yet
   */
  public static @Nullable Map<String, Object> parseAsMap(
      @NonNull ObjectMapper objectMapper, @NonNull String incompleteJson) {
    if (incompleteJson == null || incompleteJson.isBlank()) {
      return null;
    }

    String completedJson = completeJsonStatic(incompleteJson.trim());
    if (completedJson == null) {
      return null;
    }

    try {
      ObjectMapper lenientMapper =
          objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return lenientMapper.readValue(completedJson, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Static version of completeJson for use in parseAsMap.
   *
   * @param partial the incomplete JSON
   * @return a potentially valid JSON string, or null if not completeable
   */
  private static @Nullable String completeJsonStatic(@NonNull String partial) {
    if (partial.isEmpty()) {
      return null;
    }

    StringBuilder result = new StringBuilder(partial);

    // Track state
    int braceCount = 0;
    int bracketCount = 0;
    boolean inString = false;
    boolean escaped = false;

    for (int i = 0; i < partial.length(); i++) {
      char c = partial.charAt(i);

      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\' && inString) {
        escaped = true;
        continue;
      }

      if (c == '"') {
        inString = !inString;
      } else if (!inString) {
        switch (c) {
          case '{' -> braceCount++;
          case '}' -> braceCount--;
          case '[' -> bracketCount++;
          case ']' -> bracketCount--;
        }
      }
    }

    // If we're still in a string, close it
    if (inString) {
      result.append('"');
    }

    // Remove trailing comma if present
    String trimmed = result.toString().trim();
    if (trimmed.endsWith(",")) {
      result = new StringBuilder(trimmed.substring(0, trimmed.length() - 1));
    } else if (trimmed.endsWith(":")) {
      result.append("null");
    }

    // Close any open brackets
    for (int i = 0; i < bracketCount; i++) {
      result.append(']');
    }

    // Close any open braces
    for (int i = 0; i < braceCount; i++) {
      result.append('}');
    }

    String completed = result.toString();

    // Basic validation - must start with { for an object
    if (!completed.trim().startsWith("{")) {
      return null;
    }

    return completed;
  }

  /**
   * Attempts to "complete" an incomplete JSON string by closing any open structures.
   *
   * @param partial the incomplete JSON
   * @return a potentially valid JSON string, or null if not completeable
   */
  private @Nullable String completeJson(@NonNull String partial) {
    if (partial.isEmpty()) {
      return null;
    }

    StringBuilder result = new StringBuilder(partial);

    // Track state
    int braceCount = 0;
    int bracketCount = 0;
    boolean inString = false;
    boolean escaped = false;
    char lastNonWhitespace = 0;

    for (int i = 0; i < partial.length(); i++) {
      char c = partial.charAt(i);

      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\' && inString) {
        escaped = true;
        continue;
      }

      if (c == '"') {
        inString = !inString;
      } else if (!inString) {
        switch (c) {
          case '{' -> braceCount++;
          case '}' -> braceCount--;
          case '[' -> bracketCount++;
          case ']' -> bracketCount--;
        }
      }

      if (!Character.isWhitespace(c)) {
        lastNonWhitespace = c;
      }
    }

    // If we're still in a string, close it
    if (inString) {
      result.append('"');
    }

    // Remove trailing comma if present (after closing string)
    String trimmed = result.toString().trim();
    if (trimmed.endsWith(",")) {
      result = new StringBuilder(trimmed.substring(0, trimmed.length() - 1));
    } else if (trimmed.endsWith(":")) {
      // Incomplete key-value pair - add null as value
      result.append("null");
    }

    // Close any open brackets
    for (int i = 0; i < bracketCount; i++) {
      result.append(']');
    }

    // Close any open braces
    for (int i = 0; i < braceCount; i++) {
      result.append('}');
    }

    String completed = result.toString();

    // Basic validation - must start with { for an object
    if (!completed.trim().startsWith("{")) {
      return null;
    }

    return completed;
  }
}
