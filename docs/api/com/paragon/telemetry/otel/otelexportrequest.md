# :material-database: OtelExportRequest

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelExportRequest` &nbsp;·&nbsp; **Record**

---

Full OTLP/HTTP trace export request body. This is the top-level structure sent to trace ingestion
endpoints.

Follows the OTLP/HTTP specification for ExportTraceServiceRequest.

## Methods

### `forSpan`

```java
public static @NonNull OtelExportRequest forSpan(@NonNull OtelSpan span)
```

Creates an export request for a single span.

---

### `forSpans`

```java
public static @NonNull OtelExportRequest forSpans(@NonNull List<OtelSpan> spans)
```

Creates an export request for multiple spans.
