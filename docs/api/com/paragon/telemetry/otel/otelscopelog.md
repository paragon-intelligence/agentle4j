# :material-database: OtelScopeLog

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelScopeLog` &nbsp;·&nbsp; **Record**

---

Groups log records from a single instrumentation scope.

## Methods

### `forRecord`

```java
public static @NonNull OtelScopeLog forRecord(@NonNull OtelLogRecord logRecord)
```

Creates a scope log with a single log record using default Agentle scope.

---

### `forRecords`

```java
public static @NonNull OtelScopeLog forRecords(@NonNull List<OtelLogRecord> logRecords)
```

Creates a scope log with multiple log records using default Agentle scope.
