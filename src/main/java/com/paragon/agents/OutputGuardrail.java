package com.paragon.agents;

import org.jspecify.annotations.NonNull;

/**
 * Validates agent output before returning to the user.
 *
 * <p>Output guardrails are executed after the agent has finished processing, just before the final
 * result is returned. They can be used to:
 *
 * <ul>
 *   <li>Filter or mask sensitive information in responses
 *   <li>Enforce output length limits
 *   <li>Check for inappropriate content
 *   <li>Validate structured output format
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * OutputGuardrail maxLength = (output, ctx) -> {
 *     if (output.length() > 1000) {
 *         return GuardrailResult.failed("Response exceeds 1000 characters");
 *     }
 *     return GuardrailResult.passed();
 * };
 *
 * Agent agent = Agent.builder()
 *     .addOutputGuardrail(maxLength)
 *     .build();
 * }</pre>
 *
 * @see InputGuardrail
 * @see GuardrailResult
 * @since 1.0
 */
@FunctionalInterface
public interface OutputGuardrail {

  /**
   * Validates the agent's output.
   *
   * @param output  the agent's output string
   * @param context the current agent context
   * @return the validation result
   */
  @NonNull
  GuardrailResult validate(@NonNull String output, @NonNull AgenticContext context);

  /**
   * Wraps a guardrail implementation with a named ID for blueprint serialization.
   *
   * <p>Use this when defining guardrails as lambdas that need to be serializable. The guardrail
   * is registered in the {@link GuardrailRegistry} and can be reconstructed during deserialization.
   *
   * <pre>{@code
   * OutputGuardrail guard = OutputGuardrail.named("max_length", (output, ctx) -> {
   *     if (output.length() > 1000) return GuardrailResult.failed("Too long");
   *     return GuardrailResult.passed();
   * });
   * }</pre>
   *
   * @param id the unique identifier for this guardrail
   * @param impl the guardrail implementation
   * @return a named guardrail wrapping the implementation
   */
  static @NonNull OutputGuardrail named(@NonNull String id, @NonNull OutputGuardrail impl) {
    GuardrailRegistry.registerOutput(id, impl);
    return new NamedOutputGuardrail(id, impl);
  }
}
