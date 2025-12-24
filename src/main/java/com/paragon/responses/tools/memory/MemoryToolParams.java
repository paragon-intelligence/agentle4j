package com.paragon.responses.tools.memory;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for the MemoryTool.
 *
 * @param action The {@link MemoryAction} to perform
 * @param key The key to store/retrieve/delete
 * @param value The value to store (only required for STORE action)
 */
public record MemoryToolParams(
    @JsonProperty(required = true) @NonNull MemoryAction action,
    @JsonProperty(required = true) @NonNull String key,
    @JsonProperty() @Nullable String value) {
  public MemoryToolParams {
    // Compact constructor for validation
    if (key.isBlank()) {
      throw new IllegalArgumentException("key cannot be null or blank");
    }
    if (action == MemoryAction.STORE && (value == null || value.isBlank())) {
      throw new IllegalArgumentException("value is required for STORE action");
    }
  }
}
