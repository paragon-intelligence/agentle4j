package com.paragon.agents;

import org.jspecify.annotations.NonNull;

/**
 * An {@link OutputGuardrail} wrapper that carries a string ID for serialization support.
 *
 * <p>Created via {@link OutputGuardrail#named(String, OutputGuardrail)}.
 *
 * @see GuardrailRegistry
 * @since 1.0
 */
public final class NamedOutputGuardrail implements OutputGuardrail {

  private final @NonNull String id;
  private final @NonNull OutputGuardrail delegate;

  NamedOutputGuardrail(@NonNull String id, @NonNull OutputGuardrail delegate) {
    this.id = id;
    this.delegate = delegate;
  }

  /**
   * Returns the unique ID of this guardrail.
   *
   * @return the guardrail ID
   */
  public @NonNull String id() {
    return id;
  }

  @Override
  public @NonNull GuardrailResult validate(
      @NonNull String output, @NonNull AgenticContext context) {
    return delegate.validate(output, context);
  }
}
