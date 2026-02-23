# :material-database: OtelDataPoint

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelDataPoint` &nbsp;·&nbsp; **Record**

---

Represents an OTLP metric data point (gauge/counter value at a point in time).

**See Also**

- `<a href="https://opentelemetry.io/docs/specs/otel/metrics/data-model/">OTEL Metrics Data Model</a>`

## Methods

### `gaugeInt`

```java
public static @NonNull OtelDataPoint gaugeInt(
      long value, @NonNull List<OtelAttribute> attributes)
```

Creates a gauge data point with an integer value.

---

### `gaugeDouble`

```java
public static @NonNull OtelDataPoint gaugeDouble(
      double value, @NonNull List<OtelAttribute> attributes)
```

Creates a gauge data point with a double value.

---

### `counterInt`

```java
public static @NonNull OtelDataPoint counterInt(
      long startTime, long value, @NonNull List<OtelAttribute> attributes)
```

Creates a counter data point with an integer value.
