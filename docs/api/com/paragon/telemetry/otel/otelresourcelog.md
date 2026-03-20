# :material-database: OtelResourceLog

`com.paragon.telemetry.otel.OtelResourceLog` &nbsp;·&nbsp; **Record**

---

Groups scope logs by their originating resource.

## Methods

### `forScopeLog`

```java
public static @NonNull OtelResourceLog forScopeLog(@NonNull OtelScopeLog scopeLog)
```

Creates a resource log with default Agentle resource.

---

### `forLogRecord`

```java
public static @NonNull OtelResourceLog forLogRecord(@NonNull OtelLogRecord logRecord)
```

Creates a resource log with a single log record.
