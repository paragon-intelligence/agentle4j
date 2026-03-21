package com.paragon.agents;

import com.paragon.responses.json.StructuredOutputDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

final class StructuredOutputSupport {

  private StructuredOutputSupport() {}

  static boolean isCompatible(
      @NonNull StructuredOutputDefinition<?> definition, @Nullable Object parsed) {
    return parsed != null && definition.responseType().isInstance(parsed);
  }

  static <T> @NonNull T parse(
      @NonNull StructuredOutputDefinition<T> definition,
      @Nullable String text,
      @NonNull ObjectMapper objectMapper)
      throws JacksonException {
    return definition.parse(stripMarkdownFences(text), objectMapper);
  }

  static @NonNull String stripMarkdownFences(@Nullable String text) {
    if (text == null) {
      return "";
    }

    String trimmed = text.strip();
    if (!trimmed.startsWith("```")) {
      return text;
    }

    int firstNewline = trimmed.indexOf('\n');
    if (firstNewline == -1) {
      return text;
    }

    int lastFence = trimmed.lastIndexOf("```");
    if (lastFence <= firstNewline) {
      return text;
    }

    return trimmed.substring(firstNewline + 1, lastFence).strip();
  }
}
