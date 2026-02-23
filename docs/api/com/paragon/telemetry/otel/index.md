# Package `com.paragon.telemetry.otel`

> This docs was updated at: 2026-02-23

---

## :material-database: Records

| Name | Description |
|------|-------------|
| [`OtelAttribute`](otelattribute.md) | OpenTelemetry key-value attribute pair |
| [`OtelAttributeValue`](otelattributevalue.md) | OpenTelemetry attribute value wrapper supporting different value types |
| [`OtelDataPoint`](oteldatapoint.md) | Represents an OTLP metric data point (gauge/counter value at a point in time) |
| [`OtelExportRequest`](otelexportrequest.md) | Full OTLP/HTTP trace export request body |
| [`OtelLogRecord`](otellogrecord.md) | Represents an OTLP log record |
| [`OtelLogsExportRequest`](otellogsexportrequest.md) | Top-level structure for OTLP/HTTP logs export |
| [`OtelMetric`](otelmetric.md) | Represents an OTLP metric with its data points |
| [`OtelMetricsExportRequest`](otelmetricsexportrequest.md) | Top-level structure for OTLP/HTTP metrics export |
| [`OtelResource`](otelresource.md) | OpenTelemetry resource describing the entity producing telemetry |
| [`OtelResourceLog`](otelresourcelog.md) | Groups scope logs by their originating resource |
| [`OtelResourceMetric`](otelresourcemetric.md) | Groups scope metrics by their originating resource |
| [`OtelResourceSpan`](otelresourcespan.md) | Represents a collection of spans from a single resource |
| [`OtelScope`](otelscope.md) | OpenTelemetry instrumentation scope information |
| [`OtelScopeLog`](otelscopelog.md) | Groups log records from a single instrumentation scope |
| [`OtelScopeMetric`](otelscopemetric.md) | Groups metrics from a single instrumentation scope |
| [`OtelScopeSpan`](otelscopespan.md) | Collection of spans from a single instrumentation scope |
| [`OtelSpan`](otelspan.md) | OpenTelemetry span representing a unit of work or operation |
| [`OtelStatus`](otelstatus.md) | OpenTelemetry span status |
