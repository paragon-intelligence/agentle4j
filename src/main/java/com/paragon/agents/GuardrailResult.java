package com.paragon.agents;

/**
 * Result of a guardrail validation check.
 *
 * <p>Guardrails validate input before processing or output before returning to the user. This
 * sealed interface represents the two possible outcomes: passed or failed.
 *
 * @see InputGuardrail
 * @see OutputGuardrail
 * @since 1.0
 */
public sealed interface GuardrailResult {

  /**
   * Indicates the validation passed successfully.
   */
  record Passed() implements GuardrailResult {
    private static final Passed INSTANCE = new Passed();
  }

  /**
   * Indicates the validation failed with a reason.
   *
   * @param reason human-readable explanation of why validation failed
   */
  record Failed(String reason) implements GuardrailResult {
    public Failed {
      if (reason == null || reason.isBlank()) {
        throw new IllegalArgumentException("Failed reason cannot be null or blank");
      }
    }
  }

  /**
   * Returns a passed result. Uses a cached singleton instance.
   *
   * @return a passed result
   */
  static GuardrailResult passed() {
    return Passed.INSTANCE;
  }

  /**
   * Returns a failed result with the given reason.
   *
   * @param reason the failure reason
   * @return a failed result
   */
  static GuardrailResult failed(String reason) {
    return new Failed(reason);
  }

  /**
   * Checks if this result represents a successful validation.
   *
   * @return true if passed, false if failed
   */
  default boolean isPassed() {
    return this instanceof Passed;
  }

  /**
   * Checks if this result represents a failed validation.
   *
   * @return true if failed, false if passed
   */
  default boolean isFailed() {
    return this instanceof Failed;
  }
}
