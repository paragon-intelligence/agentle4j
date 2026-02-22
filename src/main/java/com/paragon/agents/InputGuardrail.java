package com.paragon.agents;

import org.jspecify.annotations.NonNull;

/**
 * Validates user input before agent processing.
 *
 * <p>Input guardrails are executed at the start of each agent interaction, before the LLM is
 * called. They can be used to:
 *
 * <ul>
 *   <li>Filter out sensitive information (passwords, PII)
 *   <li>Validate input format or length
 *   <li>Enforce topic restrictions
 *   <li>Rate limit based on context state
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * InputGuardrail noPasswords = (input, ctx) -> {
 *     if (input.toLowerCase().contains("password")) {
 *         return GuardrailResult.failed("Cannot discuss passwords");
 *     }
 *     return GuardrailResult.passed();
 * };
 *
 * Agent agent = Agent.builder()
 *     .addInputGuardrail(noPasswords)
 *     .build();
 * }</pre>
 *
 * @see OutputGuardrail
 * @see GuardrailResult
 * @since 1.0
 */
@FunctionalInterface
public interface InputGuardrail {

  /**
   * Validates the user input.
   *
   * @param input the user's input string
   * @param context the current agent context (for state-based validation)
   * @return the validation result
   */
  @NonNull GuardrailResult validate(@NonNull String input, @NonNull AgenticContext context);

  /**
   * Wraps a guardrail implementation with a named ID for blueprint serialization.
   *
   * <p>Use this when defining guardrails as lambdas that need to be serializable. The guardrail is
   * registered in the {@link GuardrailRegistry} and can be reconstructed during deserialization.
   *
   * <pre>{@code
   * InputGuardrail guard = InputGuardrail.named("no_passwords", (input, ctx) -> {
   *     if (input.contains("password")) return GuardrailResult.failed("No passwords!");
   *     return GuardrailResult.passed();
   * });
   * }</pre>
   *
   * @param id the unique identifier for this guardrail
   * @param impl the guardrail implementation
   * @return a named guardrail wrapping the implementation
   */
  static @NonNull InputGuardrail named(@NonNull String id, @NonNull InputGuardrail impl) {
    GuardrailRegistry.registerInput(id, impl);
    return new NamedInputGuardrail(id, impl);
  }
}
