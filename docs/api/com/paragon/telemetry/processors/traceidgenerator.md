# :material-code-braces: TraceIdGenerator

`com.paragon.telemetry.processors.TraceIdGenerator` &nbsp;·&nbsp; **Class**

---

Utility class for generating OpenTelemetry-compatible trace and span IDs.

## Methods

### `generateTraceId`

```java
public static @NonNull String generateTraceId()
```

Generates a random 16-byte (32 hex character) trace ID.

---

### `generateSpanId`

```java
public static @NonNull String generateSpanId()
```

Generates a random 8-byte (16 hex character) span ID.

---

### `isValidTraceId`

```java
public static boolean isValidTraceId(@NonNull String traceId)
```

Validates a trace ID format (32 hex characters).

---

### `isValidSpanId`

```java
public static boolean isValidSpanId(@NonNull String spanId)
```

Validates a span ID format (16 hex characters).
