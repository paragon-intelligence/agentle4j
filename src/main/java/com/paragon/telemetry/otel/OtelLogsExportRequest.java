package com.paragon.telemetry.otel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Top-level structure for OTLP/HTTP logs export. */
public record OtelLogsExportRequest(@NonNull List<OtelResourceLog> resourceLogs) {

  /** Creates an export request for a single log record. */
  public static @NonNull OtelLogsExportRequest forLogRecord(@NonNull OtelLogRecord logRecord) {
    return new OtelLogsExportRequest(List.of(OtelResourceLog.forLogRecord(logRecord)));
  }

  /** Creates an export request for multiple log records. */
  public static @NonNull OtelLogsExportRequest forLogRecords(
      @NonNull List<OtelLogRecord> logRecords) {
    return new OtelLogsExportRequest(
        List.of(OtelResourceLog.forScopeLog(OtelScopeLog.forRecords(logRecords))));
  }
}
