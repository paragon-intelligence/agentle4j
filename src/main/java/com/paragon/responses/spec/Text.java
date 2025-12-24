package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Represents a text input to the model.
 *
 * @param text The text input to the model.
 */
public record Text(@NonNull String text)
    implements MessageContent, FunctionToolCallOutputKind, CustomToolCallOutputKind {

  public static Text valueOf(String userMessage) {
    return new Text(userMessage);
  }

  @Override
  public @NonNull String toString() {
    return text;
  }
}
