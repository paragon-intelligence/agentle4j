package com.paragon.agents;

import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Thread-safe global registry for named guardrails.
 *
 * <p>Enables serialization of lambda/anonymous guardrails by associating them with string IDs.
 * Guardrails registered here can be referenced by ID in {@link InteractableBlueprint} and
 * reconstructed during deserialization.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Register a lambda guardrail with an ID
 * InputGuardrail guard = InputGuardrail.named("no_passwords", (input, ctx) -> {
 *     if (input.contains("password")) return GuardrailResult.failed("No passwords!");
 *     return GuardrailResult.passed();
 * });
 *
 * // The guardrail is now serializable in blueprints
 * Agent agent = Agent.builder()
 *     .addInputGuardrail(guard)
 *     .build();
 *
 * // Serialize and deserialize
 * String json = objectMapper.writeValueAsString(agent.toBlueprint());
 * Interactable restored = objectMapper.readValue(json, InteractableBlueprint.class).toInteractable();
 * }</pre>
 *
 * @see InputGuardrail#named(String, InputGuardrail)
 * @see OutputGuardrail#named(String, OutputGuardrail)
 * @since 1.0
 */
public final class GuardrailRegistry {

  private static final ConcurrentHashMap<String, InputGuardrail> INPUT_GUARDRAILS =
      new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, OutputGuardrail> OUTPUT_GUARDRAILS =
      new ConcurrentHashMap<>();

  private GuardrailRegistry() {}

  /**
   * Registers an input guardrail with the given ID.
   *
   * @param id the unique identifier
   * @param guardrail the guardrail implementation
   */
  public static void registerInput(@NonNull String id, @NonNull InputGuardrail guardrail) {
    INPUT_GUARDRAILS.put(id, guardrail);
  }

  /**
   * Registers an output guardrail with the given ID.
   *
   * @param id the unique identifier
   * @param guardrail the guardrail implementation
   */
  public static void registerOutput(@NonNull String id, @NonNull OutputGuardrail guardrail) {
    OUTPUT_GUARDRAILS.put(id, guardrail);
  }

  /**
   * Retrieves a registered input guardrail by ID.
   *
   * @param id the guardrail ID
   * @return the guardrail, or null if not registered
   */
  public static @Nullable InputGuardrail getInput(@NonNull String id) {
    return INPUT_GUARDRAILS.get(id);
  }

  /**
   * Retrieves a registered output guardrail by ID.
   *
   * @param id the guardrail ID
   * @return the guardrail, or null if not registered
   */
  public static @Nullable OutputGuardrail getOutput(@NonNull String id) {
    return OUTPUT_GUARDRAILS.get(id);
  }

  /** Removes all registered guardrails. Useful for testing. */
  public static void clear() {
    INPUT_GUARDRAILS.clear();
    OUTPUT_GUARDRAILS.clear();
  }
}
