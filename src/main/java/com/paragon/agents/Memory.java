package com.paragon.agents;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Interface for agent long-term memory storage with user isolation.
 *
 * <p>Memory provides persistent storage for agent knowledge that persists across sessions.
 * All operations are scoped by userId to ensure data isolation between users.
 *
 * <p>Memory is exposed to the agent as tools via {@link MemoryTool}. The userId is passed
 * securely by the developer in {@code Agent.interact(input, context, userId)}, NOT by the LLM,
 * to prevent prompt injection attacks.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Memory storage = InMemoryMemory.create();
 * 
 * Agent agent = Agent.builder()
 *     .addMemoryTools(storage)  // Adds memory as tools
 *     .build();
 *
 * // userId passed by developer - secure!
 * agent.interact("Remember my preference", context, "user-123");
 * }</pre>
 *
 * @see MemoryEntry
 * @see MemoryTool
 * @see InMemoryMemory
 * @since 1.0
 */
public interface Memory {

  /**
   * Adds a new memory entry for a user.
   *
   * @param userId the user ID (for isolation)
   * @param entry the memory to add
   */
  void add(@NonNull String userId, @NonNull MemoryEntry entry);

  /**
   * Adds a memory with just content for a user.
   *
   * @param userId the user ID
   * @param content the memory content
   */
  default void add(@NonNull String userId, @NonNull String content) {
    add(userId, MemoryEntry.of(content));
  }

  /**
   * Retrieves memories relevant to a query for a user.
   *
   * @param userId the user ID
   * @param query the search query
   * @param limit maximum number of memories to return
   * @return list of relevant memories, ordered by relevance
   */
  @NonNull
  List<MemoryEntry> retrieve(@NonNull String userId, @NonNull String query, int limit);

  /**
   * Updates an existing memory entry for a user.
   *
   * @param userId the user ID
   * @param id the memory ID to update
   * @param entry the new memory content
   * @throws IllegalArgumentException if memory with ID doesn't exist for this user
   */
  void update(@NonNull String userId, @NonNull String id, @NonNull MemoryEntry entry);

  /**
   * Deletes a memory by ID for a user.
   *
   * @param userId the user ID
   * @param id the memory ID to delete
   * @return true if memory was deleted, false if not found
   */
  boolean delete(@NonNull String userId, @NonNull String id);

  /**
   * Returns all stored memories for a user.
   *
   * @param userId the user ID
   * @return list of all memories for this user
   */
  @NonNull
  List<MemoryEntry> all(@NonNull String userId);

  /**
   * Returns the number of stored memories for a user.
   *
   * @param userId the user ID
   * @return memory count for this user
   */
  int size(@NonNull String userId);

  /**
   * Clears all memories for a user.
   *
   * @param userId the user ID
   */
  void clear(@NonNull String userId);

  /**
   * Clears all memories for all users.
   */
  void clearAll();
}
