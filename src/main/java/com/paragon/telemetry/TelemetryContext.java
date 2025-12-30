package com.paragon.telemetry;

import java.util.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Context for telemetry events to add user metadata, tags, and custom attributes.
 *
 * <p>Used to enrich OpenTelemetry spans with additional context for vendors like Langfuse and
 * Grafana.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var context = TelemetryContext.builder()
 *     .userId("user-123")
 *     .traceName("chat.completion")
 *     .addTag("production")
 *     .addMetadata("version", "1.0")
 *     .build();
 *
 * responder.respond(payload, sessionId, context);
 * }</pre>
 */
public record TelemetryContext(
    @Nullable String userId,
    @Nullable String traceName,
    @Nullable String parentTraceId,
    @Nullable String parentSpanId,
    @Nullable String requestId,
    @NonNull Map<String, Object> metadata,
    @NonNull List<String> tags) {

  /** Creates an empty telemetry context. */
  public static @NonNull TelemetryContext empty() {
    return new TelemetryContext(null, null, null, null, null, Map.of(), List.of());
  }

  /** Creates a minimal context with just a user ID. */
  public static @NonNull TelemetryContext forUser(@NonNull String userId) {
    return new TelemetryContext(userId, null, null, null, null, Map.of(), List.of());
  }

  /** Creates a new builder. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /** Returns all context data as OpenTelemetry attributes. */
  public @NonNull Map<String, Object> toAttributes() {
    Map<String, Object> attrs = new HashMap<>();

    if (userId != null) {
      attrs.put("user.id", userId);
      attrs.put("langfuse.user.id", userId); // Langfuse-specific
    }

    if (traceName != null) {
      attrs.put("span.name", traceName);
    }

    if (requestId != null) {
      attrs.put("request.id", requestId);
    }

    if (!tags.isEmpty()) {
      attrs.put("langfuse.tags", String.join(",", tags));
    }

    // Add all metadata
    metadata.forEach(
        (key, value) -> {
          attrs.put("langfuse.metadata." + key, value);
          attrs.put(key, value); // Also add without prefix
        });

    return Collections.unmodifiableMap(attrs);
  }

  /** Returns the trace name or the default. */
  public @NonNull String getTraceNameOrDefault(@NonNull String defaultName) {
    return traceName != null ? traceName : defaultName;
  }

  /** Builder for TelemetryContext. */
  public static class Builder {
    private String userId;
    private String traceName;
    private String parentTraceId;
    private String parentSpanId;
    private String requestId;
    private final Map<String, Object> metadata = new HashMap<>();
    private final List<String> tags = new ArrayList<>();

    /** Sets the user ID for telemetry correlation. */
    public @NonNull Builder userId(@NonNull String userId) {
      this.userId = Objects.requireNonNull(userId);
      return this;
    }

    /** Sets the trace/span name. */
    public @NonNull Builder traceName(@NonNull String traceName) {
      this.traceName = Objects.requireNonNull(traceName);
      return this;
    }

    /** Sets the parent trace ID for distributed tracing. */
    public @NonNull Builder parentTraceId(@NonNull String parentTraceId) {
      this.parentTraceId = Objects.requireNonNull(parentTraceId);
      return this;
    }

    /** Sets the parent span ID for distributed tracing. */
    public @NonNull Builder parentSpanId(@NonNull String parentSpanId) {
      this.parentSpanId = Objects.requireNonNull(parentSpanId);
      return this;
    }

    /** Sets the request ID for high-level correlation. */
    public @NonNull Builder requestId(@NonNull String requestId) {
      this.requestId = Objects.requireNonNull(requestId);
      return this;
    }

    /** Adds a metadata key-value pair. */
    public @NonNull Builder addMetadata(@NonNull String key, @NonNull Object value) {
      this.metadata.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
      return this;
    }

    /** Adds all metadata from a map. */
    public @NonNull Builder metadata(@NonNull Map<String, Object> metadata) {
      this.metadata.putAll(Objects.requireNonNull(metadata));
      return this;
    }

    /** Adds a tag. */
    public @NonNull Builder addTag(@NonNull String tag) {
      this.tags.add(Objects.requireNonNull(tag));
      return this;
    }

    /** Adds multiple tags. */
    public @NonNull Builder tags(@NonNull List<String> tags) {
      this.tags.addAll(Objects.requireNonNull(tags));
      return this;
    }

    /** Builds the TelemetryContext. */
    public @NonNull TelemetryContext build() {
      return new TelemetryContext(
          userId,
          traceName,
          parentTraceId,
          parentSpanId,
          requestId,
          Map.copyOf(metadata),
          List.copyOf(tags));
    }
  }
}
