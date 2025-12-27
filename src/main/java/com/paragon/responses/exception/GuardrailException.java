package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a guardrail validation fails.
 *
 * <p>Provides guardrail-specific context:
 * <ul>
 *   <li>{@link #guardrailName()} - Which guardrail failed</li>
 *   <li>{@link #violationType()} - INPUT or OUTPUT</li>
 *   <li>{@link #reason()} - Human-readable reason for failure</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (result.isError() && result.error() instanceof GuardrailException e) {
 *     log.warn("Blocked by {}: {}", e.guardrailName(), e.reason());
 *     if (e.violationType() == ViolationType.INPUT) {
 *         // Prompt user to rephrase
 *     }
 * }
 * }</pre>
 */
public class GuardrailException extends AgentleException {

  private final @Nullable String guardrailName;
  private final @NonNull ViolationType violationType;
  private final @NonNull String reason;

  /**
   * Type of guardrail violation.
   */
  public enum ViolationType {
    /** Input guardrail blocked the request before LLM call. */
    INPUT,
    /** Output guardrail blocked the response after LLM call. */
    OUTPUT
  }

  /**
   * Creates a new GuardrailException.
   *
   * @param guardrailName the name of the guardrail that failed
   * @param violationType whether this was an input or output violation
   * @param reason the human-readable reason for failure
   */
  public GuardrailException(
      @Nullable String guardrailName,
      @NonNull ViolationType violationType,
      @NonNull String reason) {
    super(
        ErrorCode.GUARDRAIL_VIOLATED,
        String.format(
            "Guardrail%s blocked %s: %s",
            guardrailName != null ? " '" + guardrailName + "'" : "",
            violationType == ViolationType.INPUT ? "input" : "output",
            reason),
        violationType == ViolationType.INPUT
            ? "Rephrase your request to avoid triggering this guardrail"
            : "The AI's response was blocked by a safety filter",
        false);
    this.guardrailName = guardrailName;
    this.violationType = violationType;
    this.reason = reason;
  }

  /**
   * Creates an input guardrail exception.
   *
   * @param reason the reason for failure
   * @return a new GuardrailException
   */
  public static GuardrailException inputViolation(@NonNull String reason) {
    return new GuardrailException(null, ViolationType.INPUT, reason);
  }

  /**
   * Creates an input guardrail exception with a name.
   *
   * @param guardrailName the guardrail name
   * @param reason the reason for failure
   * @return a new GuardrailException
   */
  public static GuardrailException inputViolation(
      @NonNull String guardrailName, @NonNull String reason) {
    return new GuardrailException(guardrailName, ViolationType.INPUT, reason);
  }

  /**
   * Creates an output guardrail exception.
   *
   * @param reason the reason for failure
   * @return a new GuardrailException
   */
  public static GuardrailException outputViolation(@NonNull String reason) {
    return new GuardrailException(null, ViolationType.OUTPUT, reason);
  }

  /**
   * Creates an output guardrail exception with a name.
   *
   * @param guardrailName the guardrail name
   * @param reason the reason for failure
   * @return a new GuardrailException
   */
  public static GuardrailException outputViolation(
      @NonNull String guardrailName, @NonNull String reason) {
    return new GuardrailException(guardrailName, ViolationType.OUTPUT, reason);
  }

  /**
   * Returns the name of the guardrail that failed.
   *
   * @return the guardrail name, or null if anonymous
   */
  public @Nullable String guardrailName() {
    return guardrailName;
  }

  /**
   * Returns whether this was an input or output violation.
   *
   * @return the violation type
   */
  public @NonNull ViolationType violationType() {
    return violationType;
  }

  /**
   * Returns the human-readable reason for failure.
   *
   * @return the reason
   */
  public @NonNull String reason() {
    return reason;
  }
}
