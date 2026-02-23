# :material-approximately-equal: ObservabilityBroadcaster

> This docs was updated at: 2026-02-23

`com.paragon.broadcasting.ObservabilityBroadcaster` &nbsp;·&nbsp; **Interface**

---

Core abstraction for LLM observability and distributed tracing. Broadcasts telemetry data
(traces, spans, metrics) to observability backends while maintaining semantic compliance with
OpenTelemetry and GenAI conventions.

Focuses exclusively on observability: traces, observations, cost tracking, context
propagation, and metadata management.

## Methods

### `startTrace`

```java
Trace startTrace(@NonNull String name, @NonNull TraceContext traceContext)
```

Starts a new trace (root observation representing a complete interaction). Maps to
OpenTelemetry root span.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | The name of the trace |
| `traceContext` | The initial context (user, session, metadata, etc.) |

**Returns**

A trace handle to pass to observations

---

### `getTrace`

```java
Trace getTrace(@NonNull String traceId)
```

Retrieves an existing trace by ID.

---

### `endTrace`

```java
void endTrace(@NonNull String traceId, @NonNull TraceEndOptions options)
```

Ends an active trace with the final output and status.

---

### `startObservation`

```java
Observation startObservation(
      @NonNull String traceId,
      @NonNull String name,
      @NonNull ObservationType type,
      @NonNull ObservationContext observationContext)
```

Starts a new observation (span) within a trace.

**Parameters**

| Name | Description |
|------|-------------|
| `traceId` | The parent trace ID |
| `name` | The name of the observation |
| `type` | The observation type (span, generation, event) |
| `observationContext` | The observation-specific context |

**Returns**

An observation handle

---

### `startObservation`

```java
Observation startObservation(
      @NonNull String traceId,
      @NonNull String parentObservationId,
      @NonNull String name,
      @NonNull ObservationType type,
      @NonNull ObservationContext observationContext)
```

Starts a nested observation under a parent observation. Allows hierarchical span structures for
agent graphs.

---

### `endObservation`

```java
void endObservation(@NonNull String observationId, @NonNull ObservationEndOptions options)
```

Ends an active observation.

---

### `getObservation`

```java
Observation getObservation(@NonNull String observationId)
```

Retrieves an existing observation by ID.

---

### `setAttribute`

```java
void setAttribute(@NonNull String observationId, @NonNull String key, @Nullable Object value)
```

Sets attributes (key-value pairs) on an observation. Used for OpenTelemetry semantic convention
attributes. Examples: gen_ai.system, gen_ai.request.model, gen_ai.usage.*, etc.

---

### `setAttributes`

```java
void setAttributes(@NonNull String observationId, java.util.Map<String, Object> attributes)
```

Sets multiple attributes at once.

---

### `setMetadata`

```java
void setMetadata(@NonNull String observationId, java.util.Map<String, Object> metadata)
```

Sets observation-level metadata (unstructured, searchable data). Nested keys are flattened for
filtering (use langfuse.observation.metadata.* prefix).

---

### `setMetadataKey`

```java
void setMetadataKey(@NonNull String observationId, @NonNull String key, @Nullable Object value)
```

Sets a single metadata key-value pair.

---

### `setTraceMetadata`

```java
void setTraceMetadata(@NonNull String traceId, java.util.Map<String, Object> metadata)
```

Sets trace-level metadata (propagated across all observations if using baggage).

---

### `setTraceMetadataKey`

```java
void setTraceMetadataKey(@NonNull String traceId, @NonNull String key, @Nullable Object value)
```

Sets a single trace metadata key-value pair.

---

### `setInput`

```java
void setInput(@NonNull String observationId, @Nullable Object input)
```

Sets the input for an observation. For generations: maps to gen_ai.prompt, input.value,
mlflow.spanInputs

---

### `setOutput`

```java
void setOutput(@NonNull String observationId, @Nullable Object output)
```

Sets the output for an observation. For generations: maps to gen_ai.completion, output.value,
mlflow.spanOutputs

---

### `setInputAndOutput`

```java
void setInputAndOutput(
      @NonNull String observationId, @Nullable Object input, @Nullable Object output)
```

Sets both input and output at once (useful when completing an observation).

---

### `setModel`

```java
void setModel(@NonNull String observationId, @NonNull String modelName)
```

Sets the model name for a generation observation. Only applies to observations of type
GENERATION.

Maps to: gen_ai.request.model, gen_ai.response.model, llm.model_name, model

---

### `setModelParameters`

```java
void setModelParameters(@NonNull String observationId, java.util.Map<String, Object> parameters)
```

Sets model parameters (e.g., temperature, max_tokens, top_p). Only applies to observations of
type GENERATION.

Maps to: gen_ai.request.*, llm.invocation_parameters.*

---

### `setTokenUsage`

```java
void setTokenUsage(@NonNull String observationId, @NonNull TokenUsage usage)
```

Records token usage for a generation (input, output, total). Only applies to observations of
type GENERATION.

Maps to: gen_ai.usage.*, llm.token_count.*

---

### `setCost`

```java
void setCost(@NonNull String observationId, @NonNull CostDetails cost)
```

Records the calculated cost of a generation in USD. Only applies to observations of type
GENERATION.

Maps to: gen_ai.usage.cost, langfuse.observation.cost_details

---

### `setCompletionStartTime`

```java
void setCompletionStartTime(@NonNull String observationId, long completionStartTimeMs)
```

Sets the completion start time (ISO 8601) for a generation. Useful for tracking token streaming
delays. Only applies to observations of type GENERATION.

---

### `propagateContextAttributes`

```java
void propagateContextAttributes(
      @NonNull String traceId, java.util.Map<String, Object> attributes)
```

Sets trace-level baggage attributes that should propagate to ALL observations in the trace.

Propagated attributes: - userId (langfuse.user.id, user.id) - sessionId
(langfuse.session.id, session.id) - release (langfuse.release) - version (langfuse.version) -
environment (langfuse.environment) - tags (langfuse.trace.tags) - metadata
(langfuse.trace.metadata.*)

Use OpenTelemetry Baggage + BaggageSpanProcessor for automatic propagation. This method is a
convenience for manual propagation.

---

### `setTraceVersion`

```java
void setTraceVersion(@NonNull String traceId, @NonNull String version)
```

Sets the version of the trace (e.g., "1.0.0", "v2"). Useful for tracking application logic
changes.

---

### `setTraceRelease`

```java
void setTraceRelease(@NonNull String traceId, @NonNull String release)
```

Sets the release identifier for the trace (e.g., "prod-2024-11-26"). Useful for deployment
tracking.

---

### `setTraceEnvironment`

```java
void setTraceEnvironment(@NonNull String traceId, @NonNull String environment)
```

Sets the deployment environment for the trace (e.g., "production", "staging", "development").

---

### `setObservationVersion`

```java
void setObservationVersion(@NonNull String observationId, @NonNull String version)
```

Sets the version for an observation (can differ from trace version).

---

### `setObservationEnvironment`

```java
void setObservationEnvironment(@NonNull String observationId, @NonNull String environment)
```

Sets the environment for an observation (can differ from trace environment). Maps to:
langfuse.environment, deployment.environment, deployment.environment.name

---

### `setTraceUserId`

```java
void setTraceUserId(@NonNull String traceId, @NonNull String userId)
```

Sets the user ID for a trace (for per-user analytics and support). Should be propagated to all
observations via baggage.

Maps to: langfuse.user.id, user.id

---

### `setTraceSessionId`

```java
void setTraceSessionId(@NonNull String traceId, @NonNull String sessionId)
```

Sets the session ID for a trace (for multi-turn conversations/workflows). Should be propagated
to all observations via baggage.

Maps to: langfuse.session.id, session.id

---

### `addTraceTags`

```java
void addTraceTags(@NonNull String traceId, java.util.List<String> tags)
```

Adds tags to a trace for categorization and filtering. Example: ["production", "priority-high",
"retry"]

---

### `removeTraceTag`

```java
void removeTraceTag(@NonNull String traceId, @NonNull String tag)
```

Removes a tag from a trace.

---

### `setTraceTags`

```java
void setTraceTags(@NonNull String traceId, java.util.List<String> tags)
```

Sets all tags for a trace (replaces existing tags).

---

### `setObservationLevel`

```java
void setObservationLevel(@NonNull String observationId, @NonNull ObservationLevel level)
```

Sets the severity level of an observation.

Levels: DEBUG, DEFAULT, WARNING, ERROR Inferred from OpenTelemetry span.status.code if not
explicitly set.

---

### `setObservationStatusMessage`

```java
void setObservationStatusMessage(@NonNull String observationId, @Nullable String statusMessage)
```

Sets a status message for an observation (e.g., error message, reason for failure). Inferred
from OpenTelemetry span.status.message if not explicitly set.

---

### `setObservationSuccess`

```java
void setObservationSuccess(@NonNull String observationId)
```

Marks an observation as successful.

---

### `setObservationError`

```java
void setObservationError(@NonNull String observationId, @Nullable String errorMessage)
```

Marks an observation as failed with an optional error message.

---

### `logMultiModalInput`

```java
void logMultiModalInput(@NonNull String observationId, java.util.List<MultiModalContent> content)
```

Logs multi-modal input (text, images, audio, etc.) for an observation.

Example: text prompt, screenshot, audio file, etc.

---

### `logMultiModalOutput`

```java
void logMultiModalOutput(
      @NonNull String observationId, java.util.List<MultiModalContent> content)
```

Logs multi-modal output for an observation.

Example: generated text, image, audio, etc.

---

### `logMultiModalInputItem`

```java
void logMultiModalInputItem(@NonNull String observationId, @NonNull MultiModalContent content)
```

Logs a single multi-modal content item as input.

---

### `logMultiModalOutputItem`

```java
void logMultiModalOutputItem(@NonNull String observationId, @NonNull MultiModalContent content)
```

Logs a single multi-modal content item as output.

---

### `addTraceComment`

```java
void addTraceComment(@NonNull String traceId, @NonNull String comment, @Nullable String author)
```

Adds a comment/annotation to a trace. Useful for manual notes during debugging or analysis.

---

### `addObservationComment`

```java
void addObservationComment(
      @NonNull String observationId, @NonNull String comment, @Nullable String author)
```

Adds a comment/annotation to an observation.

---

### `maskObservationInput`

```java
void maskObservationInput(@NonNull String observationId)
```

Masks (redacts) sensitive data in an observation's input. Useful for PII, API keys, passwords,
etc.

---

### `maskObservationOutput`

```java
void maskObservationOutput(@NonNull String observationId)
```

Masks (redacts) sensitive data in an observation's output.

---

### `setTracePublic`

```java
void setTracePublic(@NonNull String traceId, boolean isPublic)
```

Marks a trace as public (allows sharing via URL).

---

### `getTraceMetrics`

```java
TraceMetrics getTraceMetrics(@NonNull String traceId)
```

Retrieves aggregated metrics for a trace (cost, latency, token counts, etc.).

---

### `getTraceObservations`

```java
java.util.List<Observation> getTraceObservations(@NonNull String traceId)
```

Retrieves all observations within a trace (for building agent graphs).

---

### `flush`

```java
void flush()
```

Flushes any pending/buffered traces to the backend. Called automatically at shutdown but can be
invoked manually for batch operations.

---

### `shutdown`

```java
void shutdown()
```

Gracefully shuts down the provider, flushing all pending traces.
