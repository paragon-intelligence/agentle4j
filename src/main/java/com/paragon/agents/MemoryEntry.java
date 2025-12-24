package com.paragon.agents;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single memory entry for long-term agent memory.
 *
 * <p>Memories are automatically injected into the first user message of each agent run, allowing
 * the agent to recall relevant context from previous sessions.
 *
 * @param id unique identifier for this memory
 * @param timestamp when this memory was created
 * @param content the memory content text
 * @param metadata optional key-value metadata
 * @since 1.0
 */
public record MemoryEntry(
    @NonNull String id,
    @NonNull Instant timestamp,
    @NonNull String content,
    @NonNull Map<String, Object> metadata) {

  /**
   * Creates a new memory entry with auto-generated ID and current timestamp.
   *
   * @param content the memory content
   * @return a new memory entry
   */
  public static @NonNull MemoryEntry of(@NonNull String content) {
    return new MemoryEntry(
        UUID.randomUUID().toString(), Instant.now(), content, Map.of());
  }

  /**
   * Creates a new memory entry with metadata.
   *
   * @param content the memory content
   * @param metadata key-value metadata
   * @return a new memory entry
   */
  public static @NonNull MemoryEntry of(
      @NonNull String content, @NonNull Map<String, Object> metadata) {
    return new MemoryEntry(
        UUID.randomUUID().toString(), Instant.now(), content, Map.copyOf(metadata));
  }

  /**
   * Creates a new memory entry with a specific ID.
   *
   * @param id the memory ID
   * @param content the memory content
   * @return a new memory entry
   */
  public static @NonNull MemoryEntry withId(@NonNull String id, @NonNull String content) {
    return new MemoryEntry(id, Instant.now(), content, Map.of());
  }

  /**
   * Returns a formatted string representation for injection into prompts.
   */
  public @NonNull String toPromptFormat() {
    return String.format("[%s] %s", timestamp.toString(), content);
  }
}
