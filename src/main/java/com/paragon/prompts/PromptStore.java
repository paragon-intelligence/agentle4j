package com.paragon.prompts;

import org.jspecify.annotations.NonNull;

/**
 * Store interface for persisting prompts to various storage backends.
 *
 * <p>This interface provides write operations for prompt management, complementing the read-only
 * {@link PromptProvider} interface. Implementations may persist prompts to databases, file
 * systems, or other storage mechanisms.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Database-backed store
 * PromptStore store = new DatabasePromptStore(dataSource);
 * store.save("greeting", Prompt.of("Hello, {{name}}!"));
 *
 * // Later, delete the prompt
 * store.delete("greeting");
 * }</pre>
 *
 * <h2>Interface Segregation</h2>
 *
 * <p>The prompt management system follows the Interface Segregation Principle:
 * <ul>
 *   <li>{@link PromptProvider} - Read-only operations (retrieve, exists, list)
 *   <li>{@link PromptStore} - Write operations (save, delete)
 * </ul>
 *
 * <p>Implementations that need full CRUD capabilities can implement both interfaces.
 *
 * @author Agentle Framework
 * @since 1.0
 * @see PromptProvider
 */
public interface PromptStore {

  /**
   * Saves a prompt with the given identifier.
   *
   * <p>If a prompt with the same identifier already exists, it will be overwritten.
   *
   * @param promptId the unique identifier for the prompt
   * @param prompt the prompt to save
   * @throws NullPointerException if promptId or prompt is null
   * @throws PromptProviderException if the save operation fails
   */
  void save(@NonNull String promptId, @NonNull Prompt prompt);

  /**
   * Saves a prompt with the given identifier from raw content.
   *
   * <p>Convenience method that creates a {@link Prompt} from the content string.
   * If a prompt with the same identifier already exists, it will be overwritten.
   *
   * @param promptId the unique identifier for the prompt
   * @param content the raw prompt content (may contain template variables)
   * @throws NullPointerException if promptId or content is null
   * @throws PromptProviderException if the save operation fails
   */
  default void save(@NonNull String promptId, @NonNull String content) {
    save(promptId, Prompt.of(content));
  }

  /**
   * Deletes a prompt by its identifier.
   *
   * <p>If the prompt does not exist, this method does nothing (no-op).
   *
   * @param promptId the unique identifier for the prompt to delete
   * @throws NullPointerException if promptId is null
   * @throws PromptProviderException if the delete operation fails
   */
  void delete(@NonNull String promptId);
}
