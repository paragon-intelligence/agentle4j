package com.paragon.agents;

import com.paragon.responses.TraceMetadata;
import com.paragon.responses.json.StructuredOutputDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

/**
 * External structured-output wrapper that parses the final result without changing what the source
 * interactable sends to its own LLM calls.
 *
 * @param <T> the externally exposed structured output type
 */
public final class ReturnContract<T> implements Interactable.Structured<T> {
  private final @NonNull Interactable source;
  private final @NonNull StructuredOutputDefinition<T> structuredOutputDefinition;
  private final @NonNull ObjectMapper objectMapper;

  ReturnContract(
      @NonNull Interactable source,
      @NonNull StructuredOutputDefinition<T> structuredOutputDefinition,
      @NonNull ObjectMapper objectMapper) {
    this.source = source;
    this.structuredOutputDefinition = structuredOutputDefinition;
    this.objectMapper = objectMapper;
  }

  @Override
  public @NonNull String name() {
    return source.name();
  }

  @Override
  public @NonNull StructuredAgentResult<T> interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    AgentResult result = source.interact(context, trace);
    return result.toStructured(structuredOutputDefinition, objectMapper);
  }

  @Override
  public Interactable.@NonNull Streaming asStreaming() {
    return source.asStreaming();
  }

  public @NonNull Interactable source() {
    return source;
  }

  public @NonNull Class<T> outputType() {
    return structuredOutputDefinition.responseType();
  }
}
