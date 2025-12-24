package com.paragon.responses.spec;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The results of the file search tool call.
 *
 * @param attributes Set of 16 key-value pairs that can be attached to an object. This can be useful
 *     for storing additional information about the object in a structured format, and querying for
 *     objects via API or the dashboard. Keys are strings with a maximum length of 64 characters.
 *     Values are strings with a maximum length of 512 characters, booleans, or numbers.
 * @param file_id The unique ID of the file.
 * @param filename The name of the file.
 * @param score The relevance score of the file - a value between 0 and 1.
 * @param text The text that was retrieved from the file.
 */
public record FileSearchToolCallResult(
    @Nullable Map<String, String> attributes,
    @Nullable String file_id,
    @Nullable String filename,
    @Nullable Double score,
    @Nullable String text) {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <file_search_tool_call_result>
            <file_id>%s</file_id>
            <filename>%s</filename>
            <score>%s</score>
            <text>%s</text>
            <attributes>%s</attributes>
        </file_search_tool_call_result>
        """,
        file_id != null ? file_id : "null",
        filename != null ? filename : "null",
        score != null ? score : "null",
        text != null ? text : "null",
        attributes != null ? attributes : "null");
  }
}
