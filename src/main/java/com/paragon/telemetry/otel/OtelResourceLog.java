package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Groups scope logs by their originating resource. */
public record OtelResourceLog(
    @NonNull OtelResource resource, @NonNull List<OtelScopeLog> scopeLogs) {

  /** Creates a resource log with default Agentle resource. */
  public static @NonNull OtelResourceLog forScopeLog(@NonNull OtelScopeLog scopeLog) {
    return new OtelResourceLog(OtelResource.agentle(), List.of(scopeLog));
  }

  /** Creates a resource log with a single log record. */
  public static @NonNull OtelResourceLog forLogRecord(@NonNull OtelLogRecord logRecord) {
    return forScopeLog(OtelScopeLog.forRecord(logRecord));
  }
}
