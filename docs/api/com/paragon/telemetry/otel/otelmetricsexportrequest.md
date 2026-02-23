# :material-database: OtelMetricsExportRequest

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelMetricsExportRequest` &nbsp;·&nbsp; **Record**

---

Top-level structure for OTLP/HTTP metrics export.

## Methods

### `forMetrics`

```java
public static @NonNull OtelMetricsExportRequest forMetrics(@NonNull List<OtelMetric> metrics)
```

Creates an export request for a list of metrics.

---

### `forMetric`

```java
public static @NonNull OtelMetricsExportRequest forMetric(@NonNull OtelMetric metric)
```

Creates an export request for a single metric.
