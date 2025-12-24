package com.paragon.responses.tools.memory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Interface for memory storage operations. */
public interface MemoryStore {

  /**
   * Stores a value associated with a key.
   *
   * @param key The key to store
   * @param value The value to store
   * @return A message indicating the result of the operation
   */
  @NonNull
  String store(@NonNull String key, @NonNull String value);

  /**
   * Retrieves a value associated with a key.
   *
   * @param key The key to retrieve
   * @return The value, or null if not found
   */
  @Nullable
  String retrieve(@NonNull String key);

  /**
   * Deletes a value associated with a key.
   *
   * @param key The key to delete
   * @return A message indicating the result of the operation
   */
  @NonNull
  String delete(@NonNull String key);
}
