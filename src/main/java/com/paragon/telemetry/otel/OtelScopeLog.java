package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Groups log records from a single instrumentation scope. */
public record OtelScopeLog(@NonNull OtelScope scope, @NonNull List<OtelLogRecord> logRecords) {

  /** Creates a scope log with a single log record using default Agentle scope. */
  public static @NonNull OtelScopeLog forRecord(@NonNull OtelLogRecord logRecord) {
    return new OtelScopeLog(OtelScope.agentle(), List.of(logRecord));
  }

  /** Creates a scope log with multiple log records using default Agentle scope. */
  public static @NonNull OtelScopeLog forRecords(@NonNull List<OtelLogRecord> logRecords) {
    return new OtelScopeLog(OtelScope.agentle(), logRecords);
  }
}
