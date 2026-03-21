# :material-database: OtelLogRecord

> This docs was updated at: 2026-03-21

`com.paragon.telemetry.otel.OtelLogRecord` &nbsp;·&nbsp; **Record**

---

Represents an OTLP log record.

**See Also**

- `OTEL logs data model: https://opentelemetry.io/docs/specs/otel/logs/data-model/`

## Methods

### `info`

```java
public static @NonNull OtelLogRecord info(
      @NonNull String body,
      @NonNull List<OtelAttribute> attributes,
      @Nullable String traceId,
      @Nullable String spanId)
```

Creates an INFO log record.

---

### `error`

```java
public static @NonNull OtelLogRecord error(
      @NonNull String body,
      @NonNull List<OtelAttribute> attributes,
      @Nullable String traceId,
      @Nullable String spanId)
```

Creates an ERROR log record.

---

### `debug`

```java
public static @NonNull OtelLogRecord debug(
      @NonNull String body,
      @NonNull List<OtelAttribute> attributes,
      @Nullable String traceId,
      @Nullable String spanId)
```

Creates a DEBUG log record.
