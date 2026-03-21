# :material-database: OtelMetric

> This docs was updated at: 2026-03-21

`com.paragon.telemetry.otel.OtelMetric` &nbsp;·&nbsp; **Record**

---

Represents an OTLP metric with its data points.

**See Also**

- `OTEL metrics data model: https://opentelemetry.io/docs/specs/otel/metrics/data-model/`

## Methods

### `gauge`

```java
public static @NonNull OtelMetric gauge(
      @NonNull String name,
      @Nullable String description,
      @Nullable String unit,
      @NonNull List<OtelDataPoint> dataPoints)
```

Creates a gauge metric.

---

### `counter`

```java
public static @NonNull OtelMetric counter(
      @NonNull String name,
      @Nullable String description,
      @Nullable String unit,
      @NonNull List<OtelDataPoint> dataPoints)
```

Creates a cumulative sum (counter) metric.
