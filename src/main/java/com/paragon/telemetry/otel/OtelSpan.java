package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry span representing a unit of work or operation.
 *
 * <p>Follows the OTLP specification for spans.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtelSpan(
    @JsonProperty("traceId") @NonNull String traceId,
    @JsonProperty("spanId") @NonNull String spanId,
    @JsonProperty("parentSpanId") @Nullable String parentSpanId,
    @JsonProperty("name") @NonNull String name,
    @JsonProperty("kind") int kind,
    @JsonProperty("startTimeUnixNano") @NonNull String startTimeUnixNano,
    @JsonProperty("endTimeUnixNano") @Nullable String endTimeUnixNano,
    @JsonProperty("attributes") @Nullable List<OtelAttribute> attributes,
    @JsonProperty("status") @Nullable OtelStatus status) {

  /** Span kinds as per OpenTelemetry spec. */
  public static final int SPAN_KIND_UNSPECIFIED = 0;

  public static final int SPAN_KIND_INTERNAL = 1;
  public static final int SPAN_KIND_SERVER = 2;
  public static final int SPAN_KIND_CLIENT = 3;
  public static final int SPAN_KIND_PRODUCER = 4;
  public static final int SPAN_KIND_CONSUMER = 5;

  /** Creates a builder for constructing spans. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /** Builder for OtelSpan. */
  public static class Builder {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String name;
    private int kind = SPAN_KIND_INTERNAL;
    private String startTimeUnixNano;
    private String endTimeUnixNano;
    private List<OtelAttribute> attributes;
    private OtelStatus status;

    public Builder traceId(@NonNull String traceId) {
      this.traceId = traceId;
      return this;
    }

    public Builder spanId(@NonNull String spanId) {
      this.spanId = spanId;
      return this;
    }

    public Builder parentSpanId(@Nullable String parentSpanId) {
      this.parentSpanId = parentSpanId;
      return this;
    }

    public Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public Builder kind(int kind) {
      this.kind = kind;
      return this;
    }

    public Builder clientKind() {
      this.kind = SPAN_KIND_CLIENT;
      return this;
    }

    public Builder startTimeNanos(long nanos) {
      this.startTimeUnixNano = String.valueOf(nanos);
      return this;
    }

    public Builder endTimeNanos(long nanos) {
      this.endTimeUnixNano = String.valueOf(nanos);
      return this;
    }

    public Builder attributes(@Nullable List<OtelAttribute> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder status(@Nullable OtelStatus status) {
      this.status = status;
      return this;
    }

    public @NonNull OtelSpan build() {
      return new OtelSpan(
          traceId,
          spanId,
          parentSpanId,
          name,
          kind,
          startTimeUnixNano,
          endTimeUnixNano,
          attributes,
          status);
    }
  }
}
