package com.paragon.agents;

import org.jspecify.annotations.NonNull;

/**
 * An {@link InputGuardrail} wrapper that carries a string ID for serialization support.
 *
 * <p>Created via {@link InputGuardrail#named(String, InputGuardrail)}.
 *
 * @see GuardrailRegistry
 * @since 1.0
 */
public final class NamedInputGuardrail implements InputGuardrail {

  private final @NonNull String id;
  private final @NonNull InputGuardrail delegate;

  NamedInputGuardrail(@NonNull String id, @NonNull InputGuardrail delegate) {
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
  public @NonNull GuardrailResult validate(@NonNull String input, @NonNull AgenticContext context) {
    return delegate.validate(input, context);
  }
}
