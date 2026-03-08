package com.paragon.agents;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Thread-safe global registry for named {@link InteractableBlueprint} instances.
 *
 * <p>Allows blueprints to be registered by a string ID and resolved at deserialization time using
 * the {@code source: registry} discriminator in YAML/JSON.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // At startup, register blueprints
 * BlueprintRegistry.register("clinico-geral", clinicoGeralBlueprint);
 *
 * // In YAML, reference by ID:
 * target:
 *   source: registry
 *   id: clinico-geral
 * }</pre>
 *
 * <p>Follows the same pattern as {@link GuardrailRegistry} and {@code PromptProviderRegistry}.
 *
 * @since 1.0
 */
public final class BlueprintRegistry {

  private static final ConcurrentHashMap<String, InteractableBlueprint> REGISTRY =
      new ConcurrentHashMap<>();

  private BlueprintRegistry() {}

  /**
   * Registers a blueprint with the given ID.
   *
   * @param id the unique identifier
   * @param blueprint the blueprint to register
   */
  public static void register(@NonNull String id, @NonNull InteractableBlueprint blueprint) {
    REGISTRY.put(id, blueprint);
  }

  /**
   * Retrieves a registered blueprint by ID.
   *
   * @param id the blueprint ID
   * @return the blueprint, or null if not registered
   */
  public static @Nullable InteractableBlueprint get(@NonNull String id) {
    return REGISTRY.get(id);
  }

  /**
   * Returns whether a blueprint with the given ID is registered.
   *
   * @param id the blueprint ID
   * @return true if registered, false otherwise
   */
  public static boolean contains(@NonNull String id) {
    return REGISTRY.containsKey(id);
  }

  /**
   * Removes a registered blueprint by ID.
   *
   * @param id the blueprint ID to remove
   */
  public static void unregister(@NonNull String id) {
    REGISTRY.remove(id);
  }

  /** Removes all registered blueprints. Useful for testing. */
  public static void clear() {
    REGISTRY.clear();
  }

  /**
   * Returns a snapshot of all registered IDs.
   *
   * @return an unmodifiable set of registered IDs
   */
  public static Set<String> registeredIds() {
    return Set.copyOf(REGISTRY.keySet());
  }
}
