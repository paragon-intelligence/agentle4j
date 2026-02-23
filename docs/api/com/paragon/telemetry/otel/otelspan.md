# :material-database: OtelSpan

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelSpan` &nbsp;·&nbsp; **Record**

---

OpenTelemetry span representing a unit of work or operation.

Follows the OTLP specification for spans.

## Fields

### `SPAN_KIND_UNSPECIFIED`

```java
public static final int SPAN_KIND_UNSPECIFIED = 0
```

Span kinds as per OpenTelemetry spec.

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a builder for constructing spans.
