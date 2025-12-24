package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Reasoning text content.
 *
 * @param text The reasoning text from the model.
 */
public record ReasoningTextContent(@NonNull String text) implements ReasoningContent {}
