# :material-database: OtelLogsExportRequest

> This docs was updated at: 2026-03-17
















`com.paragon.telemetry.otel.OtelLogsExportRequest` &nbsp;·&nbsp; **Record**

---

Top-level structure for OTLP/HTTP logs export.

## Methods

### `forLogRecord`

```java
public static @NonNull OtelLogsExportRequest forLogRecord(@NonNull OtelLogRecord logRecord)
```

Creates an export request for a single log record.

---

### `forLogRecords`

```java
public static @NonNull OtelLogsExportRequest forLogRecords(
      @NonNull List<OtelLogRecord> logRecords)
```

Creates an export request for multiple log records.
