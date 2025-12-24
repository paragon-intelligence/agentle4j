package com.paragon.telemetry.otel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Full OTLP/HTTP trace export request body. This is the top-level structure sent to trace ingestion
 * endpoints.
 *
 * <p>Follows the OTLP/HTTP specification for ExportTraceServiceRequest.
 */
public record OtelExportRequest(
    @JsonProperty("resourceSpans") @NonNull List<OtelResourceSpan> resourceSpans) {

  /** Creates an export request for a single span. */
  public static @NonNull OtelExportRequest forSpan(@NonNull OtelSpan span) {
    return new OtelExportRequest(List.of(OtelResourceSpan.forAgentle(List.of(span))));
  }

  /** Creates an export request for multiple spans. */
  public static @NonNull OtelExportRequest forSpans(@NonNull List<OtelSpan> spans) {
    return new OtelExportRequest(List.of(OtelResourceSpan.forAgentle(spans)));
  }
}
