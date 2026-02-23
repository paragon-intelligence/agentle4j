# :material-database: OtelResourceMetric

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelResourceMetric` &nbsp;·&nbsp; **Record**

---

Groups scope metrics by their originating resource.

## Methods

### `forScopeMetric`

```java
public static @NonNull OtelResourceMetric forScopeMetric(@NonNull OtelScopeMetric scopeMetric)
```

Creates a resource metric with default Agentle resource.

---

### `forMetrics`

```java
public static @NonNull OtelResourceMetric forMetrics(@NonNull List<OtelMetric> metrics)
```

Creates a resource metric with a list of metrics.
