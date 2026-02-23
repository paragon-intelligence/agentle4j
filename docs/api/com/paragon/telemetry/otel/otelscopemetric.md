# :material-database: OtelScopeMetric

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelScopeMetric` &nbsp;·&nbsp; **Record**

---

Groups metrics from a single instrumentation scope.

## Methods

### `forMetric`

```java
public static @NonNull OtelScopeMetric forMetric(@NonNull OtelMetric metric)
```

Creates a scope metric with a single metric using default Agentle scope.

---

### `forMetrics`

```java
public static @NonNull OtelScopeMetric forMetrics(@NonNull List<OtelMetric> metrics)
```

Creates a scope metric with multiple metrics using default Agentle scope.
