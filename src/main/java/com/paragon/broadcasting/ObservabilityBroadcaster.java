package com.paragon.broadcasting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Core abstraction for LLM observability and distributed tracing. Broadcasts telemetry data
 * (traces, spans, metrics) to observability backends while maintaining semantic compliance with
 * OpenTelemetry and GenAI conventions.
 *
 * <p>Focuses exclusively on observability: traces, observations, cost tracking, context
 * propagation, and metadata management.
 */
public interface ObservabilityBroadcaster {

  // ============ TRACE LIFECYCLE ============

  /**
   * Starts a new trace (root observation representing a complete interaction). Maps to
   * OpenTelemetry root span.
   *
   * @param name The name of the trace
   * @param traceContext The initial context (user, session, metadata, etc.)
   * @return A trace handle to pass to observations
   */
  @NonNull Trace startTrace(@NonNull String name, @NonNull TraceContext traceContext);

  /** Retrieves an existing trace by ID. */
  @Nullable Trace getTrace(@NonNull String traceId);

  /** Ends an active trace with the final output and status. */
  void endTrace(@NonNull String traceId, @NonNull TraceEndOptions options);

  // ============ OBSERVATION (SPAN) LIFECYCLE ============

  /**
   * Starts a new observation (span) within a trace.
   *
   * @param traceId The parent trace ID
   * @param name The name of the observation
   * @param type The observation type (span, generation, event)
   * @param observationContext The observation-specific context
   * @return An observation handle
   */
  @NonNull Observation startObservation(
      @NonNull String traceId,
      @NonNull String name,
      @NonNull ObservationType type,
      @NonNull ObservationContext observationContext);

  /**
   * Starts a nested observation under a parent observation. Allows hierarchical span structures for
   * agent graphs.
   */
  @NonNull Observation startObservation(
      @NonNull String traceId,
      @NonNull String parentObservationId,
      @NonNull String name,
      @NonNull ObservationType type,
      @NonNull ObservationContext observationContext);

  /** Ends an active observation. */
  void endObservation(@NonNull String observationId, @NonNull ObservationEndOptions options);

  /** Retrieves an existing observation by ID. */
  @Nullable Observation getObservation(@NonNull String observationId);

  // ============ ATTRIBUTES & METADATA ============

  /**
   * Sets attributes (key-value pairs) on an observation. Used for OpenTelemetry semantic convention
   * attributes. Examples: gen_ai.system, gen_ai.request.model, gen_ai.usage.*, etc.
   */
  void setAttribute(@NonNull String observationId, @NonNull String key, @Nullable Object value);

  /** Sets multiple attributes at once. */
  void setAttributes(@NonNull String observationId, java.util.Map<String, Object> attributes);

  /**
   * Sets observation-level metadata (unstructured, searchable data). Nested keys are flattened for
   * filtering (use langfuse.observation.metadata.* prefix).
   */
  void setMetadata(@NonNull String observationId, java.util.Map<String, Object> metadata);

  /** Sets a single metadata key-value pair. */
  void setMetadataKey(@NonNull String observationId, @NonNull String key, @Nullable Object value);

  /** Sets trace-level metadata (propagated across all observations if using baggage). */
  void setTraceMetadata(@NonNull String traceId, java.util.Map<String, Object> metadata);

  /** Sets a single trace metadata key-value pair. */
  void setTraceMetadataKey(@NonNull String traceId, @NonNull String key, @Nullable Object value);

  // ============ OBSERVATION INPUT/OUTPUT ============

  /**
   * Sets the input for an observation. For generations: maps to gen_ai.prompt, input.value,
   * mlflow.spanInputs
   */
  void setInput(@NonNull String observationId, @Nullable Object input);

  /**
   * Sets the output for an observation. For generations: maps to gen_ai.completion, output.value,
   * mlflow.spanOutputs
   */
  void setOutput(@NonNull String observationId, @Nullable Object output);

  /** Sets both input and output at once (useful when completing an observation). */
  void setInputAndOutput(
      @NonNull String observationId, @Nullable Object input, @Nullable Object output);

  // ============ LLM-SPECIFIC TRACKING (GENERATIONS) ============

  /**
   * Sets the model name for a generation observation. Only applies to observations of type
   * GENERATION.
   *
   * <p>Maps to: gen_ai.request.model, gen_ai.response.model, llm.model_name, model
   */
  void setModel(@NonNull String observationId, @NonNull String modelName);

  /**
   * Sets model parameters (e.g., temperature, max_tokens, top_p). Only applies to observations of
   * type GENERATION.
   *
   * <p>Maps to: gen_ai.request.*, llm.invocation_parameters.*
   */
  void setModelParameters(@NonNull String observationId, java.util.Map<String, Object> parameters);

  /**
   * Records token usage for a generation (input, output, total). Only applies to observations of
   * type GENERATION.
   *
   * <p>Maps to: gen_ai.usage.*, llm.token_count.*
   */
  void setTokenUsage(@NonNull String observationId, @NonNull TokenUsage usage);

  /**
   * Records the calculated cost of a generation in USD. Only applies to observations of type
   * GENERATION.
   *
   * <p>Maps to: gen_ai.usage.cost, langfuse.observation.cost_details
   */
  void setCost(@NonNull String observationId, @NonNull CostDetails cost);

  /**
   * Sets the completion start time (ISO 8601) for a generation. Useful for tracking token streaming
   * delays. Only applies to observations of type GENERATION.
   */
  void setCompletionStartTime(@NonNull String observationId, long completionStartTimeMs);

  // ============ TRACE CONTEXT PROPAGATION ============

  /**
   * Sets trace-level baggage attributes that should propagate to ALL observations in the trace.
   *
   * <p>Propagated attributes: - userId (langfuse.user.id, user.id) - sessionId
   * (langfuse.session.id, session.id) - release (langfuse.release) - version (langfuse.version) -
   * environment (langfuse.environment) - tags (langfuse.trace.tags) - metadata
   * (langfuse.trace.metadata.*)
   *
   * <p>Use OpenTelemetry Baggage + BaggageSpanProcessor for automatic propagation. This method is a
   * convenience for manual propagation.
   */
  void propagateContextAttributes(
      @NonNull String traceId, java.util.Map<String, Object> attributes);

  // ============ TRACE VERSIONING & DEPLOYMENT ============

  /**
   * Sets the version of the trace (e.g., "1.0.0", "v2"). Useful for tracking application logic
   * changes.
   */
  void setTraceVersion(@NonNull String traceId, @NonNull String version);

  /**
   * Sets the release identifier for the trace (e.g., "prod-2024-11-26"). Useful for deployment
   * tracking.
   */
  void setTraceRelease(@NonNull String traceId, @NonNull String release);

  /**
   * Sets the deployment environment for the trace (e.g., "production", "staging", "development").
   */
  void setTraceEnvironment(@NonNull String traceId, @NonNull String environment);

  /** Sets the version for an observation (can differ from trace version). */
  void setObservationVersion(@NonNull String observationId, @NonNull String version);

  /**
   * Sets the environment for an observation (can differ from trace environment). Maps to:
   * langfuse.environment, deployment.environment, deployment.environment.name
   */
  void setObservationEnvironment(@NonNull String observationId, @NonNull String environment);

  // ============ USER & SESSION TRACKING ============

  /**
   * Sets the user ID for a trace (for per-user analytics and support). Should be propagated to all
   * observations via baggage.
   *
   * <p>Maps to: langfuse.user.id, user.id
   */
  void setTraceUserId(@NonNull String traceId, @NonNull String userId);

  /**
   * Sets the session ID for a trace (for multi-turn conversations/workflows). Should be propagated
   * to all observations via baggage.
   *
   * <p>Maps to: langfuse.session.id, session.id
   */
  void setTraceSessionId(@NonNull String traceId, @NonNull String sessionId);

  // ============ TAGS & CATEGORIZATION ============

  /**
   * Adds tags to a trace for categorization and filtering. Example: ["production", "priority-high",
   * "retry"]
   */
  void addTraceTags(@NonNull String traceId, java.util.List<String> tags);

  /** Removes a tag from a trace. */
  void removeTraceTag(@NonNull String traceId, @NonNull String tag);

  /** Sets all tags for a trace (replaces existing tags). */
  void setTraceTags(@NonNull String traceId, java.util.List<String> tags);

  // ============ OBSERVATION STATUS & SEVERITY ============

  /**
   * Sets the severity level of an observation.
   *
   * <p>Levels: DEBUG, DEFAULT, WARNING, ERROR Inferred from OpenTelemetry span.status.code if not
   * explicitly set.
   */
  void setObservationLevel(@NonNull String observationId, @NonNull ObservationLevel level);

  /**
   * Sets a status message for an observation (e.g., error message, reason for failure). Inferred
   * from OpenTelemetry span.status.message if not explicitly set.
   */
  void setObservationStatusMessage(@NonNull String observationId, @Nullable String statusMessage);

  /** Marks an observation as successful. */
  void setObservationSuccess(@NonNull String observationId);

  /** Marks an observation as failed with an optional error message. */
  void setObservationError(@NonNull String observationId, @Nullable String errorMessage);

  // ============ MULTI-MODALITY ============

  /**
   * Logs multi-modal input (text, images, audio, etc.) for an observation.
   *
   * <p>Example: text prompt, screenshot, audio file, etc.
   */
  void logMultiModalInput(@NonNull String observationId, java.util.List<MultiModalContent> content);

  /**
   * Logs multi-modal output for an observation.
   *
   * <p>Example: generated text, image, audio, etc.
   */
  void logMultiModalOutput(
      @NonNull String observationId, java.util.List<MultiModalContent> content);

  /** Logs a single multi-modal content item as input. */
  void logMultiModalInputItem(@NonNull String observationId, @NonNull MultiModalContent content);

  /** Logs a single multi-modal content item as output. */
  void logMultiModalOutputItem(@NonNull String observationId, @NonNull MultiModalContent content);

  // ============ COMMENTS & ANNOTATIONS ============

  /** Adds a comment/annotation to a trace. Useful for manual notes during debugging or analysis. */
  void addTraceComment(@NonNull String traceId, @NonNull String comment, @Nullable String author);

  /** Adds a comment/annotation to an observation. */
  void addObservationComment(
      @NonNull String observationId, @NonNull String comment, @Nullable String author);

  // ============ MASKING & PRIVACY ============

  /**
   * Masks (redacts) sensitive data in an observation's input. Useful for PII, API keys, passwords,
   * etc.
   */
  void maskObservationInput(@NonNull String observationId);

  /** Masks (redacts) sensitive data in an observation's output. */
  void maskObservationOutput(@NonNull String observationId);

  /** Marks a trace as public (allows sharing via URL). */
  void setTracePublic(@NonNull String traceId, boolean isPublic);

  // ============ TRACE ANALYTICS ============

  /** Retrieves aggregated metrics for a trace (cost, latency, token counts, etc.). */
  @NonNull TraceMetrics getTraceMetrics(@NonNull String traceId);

  /** Retrieves all observations within a trace (for building agent graphs). */
  java.util.List<Observation> getTraceObservations(@NonNull String traceId);

  // ============ LIFECYCLE MANAGEMENT ============

  /**
   * Flushes any pending/buffered traces to the backend. Called automatically at shutdown but can be
   * invoked manually for batch operations.
   */
  void flush();

  /** Gracefully shuts down the provider, flushing all pending traces. */
  void shutdown();
}
