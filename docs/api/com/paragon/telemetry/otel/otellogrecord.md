# :material-database: OtelLogRecord

`com.paragon.telemetry.otel.OtelLogRecord` &nbsp;·&nbsp; **Record**

---

Represents an OTLP log record.

**See Also**

- `<a href="https://opentelemetry.io/docs/specs/otel/logs/data-model/">OTEL Logs Data Model</a>`

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
