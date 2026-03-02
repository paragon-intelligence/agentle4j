package com.paragon.prompts;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Global registry for named {@link PromptProvider} instances.
 *
 * <p>Similar to {@link com.paragon.agents.GuardrailRegistry}, this registry allows prompt providers
 * to be registered at application startup and referenced by ID from agent blueprints. This enables
 * agent definitions (JSON/YAML) to reference prompts stored in external systems like Langfuse
 * without embedding the provider configuration in the blueprint itself.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // At application startup — register providers
 * PromptProviderRegistry.register("langfuse",
 *     LangfusePromptProvider.builder()
 *         .httpClient(httpClient)
 *         .publicKey("pk-xxx")
 *         .secretKey("sk-xxx")
 *         .build());
 *
 * PromptProviderRegistry.register("local",
 *     FilesystemPromptProvider.create(Path.of("./prompts")));
 *
 * // Later — blueprints reference providers by ID
 * // "instructions": { "source": "provider", "providerId": "langfuse", "promptId": "my-prompt" }
 * PromptProvider provider = PromptProviderRegistry.get("langfuse");
 * Prompt prompt = provider.providePrompt("my-prompt");
 * }</pre>
 *
 * @author Agentle Framework
 * @since 1.0
 * @see PromptProvider
 */
public final class PromptProviderRegistry {

  private static final Map<String, PromptProvider> PROVIDERS = new ConcurrentHashMap<>();

  private PromptProviderRegistry() {}

  /**
   * Registers a prompt provider with the given identifier.
   *
   * <p>If a provider is already registered with the same ID, it is silently replaced.
   *
   * @param id the unique identifier for this provider (e.g., "langfuse", "local", "db")
   * @param provider the prompt provider instance
   * @throws NullPointerException if id or provider is null
   * @throws IllegalArgumentException if id is empty
   */
  public static void register(@NonNull String id, @NonNull PromptProvider provider) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(provider, "provider must not be null");
    if (id.isEmpty()) {
      throw new IllegalArgumentException("id must not be empty");
    }
    PROVIDERS.put(id, provider);
  }

  /**
   * Retrieves a registered prompt provider by its identifier.
   *
   * @param id the provider identifier
   * @return the registered provider, or {@code null} if not found
   * @throws NullPointerException if id is null
   */
  public static @Nullable PromptProvider get(@NonNull String id) {
    Objects.requireNonNull(id, "id must not be null");
    return PROVIDERS.get(id);
  }

  /**
   * Checks whether a provider with the given ID is registered.
   *
   * @param id the provider identifier
   * @return {@code true} if a provider is registered with this ID
   * @throws NullPointerException if id is null
   */
  public static boolean contains(@NonNull String id) {
    Objects.requireNonNull(id, "id must not be null");
    return PROVIDERS.containsKey(id);
  }

  /**
   * Returns an unmodifiable view of all registered provider IDs.
   *
   * @return the set of registered provider IDs
   */
  public static @NonNull Set<String> registeredIds() {
    return Set.copyOf(PROVIDERS.keySet());
  }

  /**
   * Removes all registered providers.
   *
   * <p>Primarily useful for testing.
   */
  public static void clear() {
    PROVIDERS.clear();
  }

  /**
   * Removes a specific provider registration.
   *
   * @param id the provider identifier to remove
   * @throws NullPointerException if id is null
   */
  public static void unregister(@NonNull String id) {
    Objects.requireNonNull(id, "id must not be null");
    PROVIDERS.remove(id);
  }
}
