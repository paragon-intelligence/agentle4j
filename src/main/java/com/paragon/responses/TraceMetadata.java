package com.paragon.responses;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Metadata for OpenRouter trace/observability support.
 *
 * <p>Enables integration with observability platforms like Langfuse by sending trace information
 * directly in API requests. All fields are optional to support partial configuration.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * TraceMetadata trace = TraceMetadata.builder()
 *     .traceId("workflow_12345")
 *     .traceName("Document Processing Pipeline")
 *     .spanName("Summarization Step")
 *     .generationName("Generate Summary")
 *     .environment("production")
 *     .build();
 * }</pre>
 *
 * @see <a href="https://openrouter.ai/docs/guides/features/broadcast/langfuse">OpenRouter Langfuse
 *     Documentation</a>
 * @since 1.0
 */
public record TraceMetadata(
    @Nullable String traceId,
    @Nullable String traceName,
    @Nullable String spanName,
    @Nullable String generationName,
    @Nullable String parentSpanId,
    @Nullable String environment,
    @Nullable Map<String, Object> metadata) {

  /**
   * Checks if all fields in this trace metadata are null.
   *
   * @return true if all fields are null, false otherwise
   */
  public boolean isEmpty() {
    return traceId == null
        && traceName == null
        && spanName == null
        && generationName == null
        && parentSpanId == null
        && environment == null
        && (metadata == null || metadata.isEmpty());
  }

  /**
   * Returns this metadata or null if all fields are null.
   *
   * @return this metadata if not empty, null otherwise
   */
  public @Nullable TraceMetadata orNullIfEmpty() {
    return isEmpty() ? null : this;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    @Nullable String traceId = null;
    @Nullable String traceName = null;
    @Nullable String spanName = null;
    @Nullable String generationName = null;
    @Nullable String parentSpanId = null;
    @Nullable String environment = null;
    @Nullable Map<String, Object> metadata = null;

    /**
     * Sets the trace ID to group multiple requests into a single trace.
     *
     * @param traceId the trace ID
     * @return this builder
     */
    public Builder traceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    /**
     * Sets the custom name displayed in the Langfuse trace list.
     *
     * @param traceName the trace name
     * @return this builder
     */
    public Builder traceName(String traceName) {
      this.traceName = traceName;
      return this;
    }

    /**
     * Sets the name for intermediate spans in the hierarchy.
     *
     * @param spanName the span name
     * @return this builder
     */
    public Builder spanName(String spanName) {
      this.spanName = spanName;
      return this;
    }

    /**
     * Sets the name for the LLM generation observation.
     *
     * @param generationName the generation name
     * @return this builder
     */
    public Builder generationName(String generationName) {
      this.generationName = generationName;
      return this;
    }

    /**
     * Sets the parent observation ID to link to an existing span in the trace hierarchy.
     *
     * @param parentSpanId the parent span ID
     * @return this builder
     */
    public Builder parentSpanId(String parentSpanId) {
      this.parentSpanId = parentSpanId;
      return this;
    }

    /**
     * Sets the environment (e.g., "production", "staging").
     *
     * @param environment the environment
     * @return this builder
     */
    public Builder environment(String environment) {
      this.environment = environment;
      return this;
    }

    /**
     * Sets custom metadata as key-value pairs for filtering and analysis.
     *
     * @param metadata the metadata map
     * @return this builder
     */
    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public TraceMetadata build() {
      return new TraceMetadata(
          this.traceId,
          this.traceName,
          this.spanName,
          this.generationName,
          this.parentSpanId,
          this.environment,
          this.metadata);
    }
  }
}
