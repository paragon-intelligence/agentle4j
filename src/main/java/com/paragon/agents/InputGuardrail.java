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
  @NonNull GuardrailResult validate(@NonNull String input, @NonNull AgentContext context);
}
