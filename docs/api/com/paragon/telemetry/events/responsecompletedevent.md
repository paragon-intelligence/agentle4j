# :material-database: ResponseCompletedEvent

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.events.ResponseCompletedEvent` &nbsp;·&nbsp; **Record**

---

Event emitted when a respond() call completes successfully.

This event marks the end of a span and includes metrics about the completed response (tokens,
latency, cost).

## Methods

### `durationNanos`

```java
public long durationNanos()
```

Duration of the operation in nanoseconds.

---

### `durationMs`

```java
public long durationMs()
```

Duration of the operation in milliseconds.

---

### `from`

```java
public static @NonNull ResponseCompletedEvent from(
      @NonNull ResponseStartedEvent startedEvent,
      @Nullable Integer inputTokens,
      @Nullable Integer outputTokens,
      @Nullable Integer totalTokens,
      @Nullable Double costUsd)
```

Creates a completed event from a started event.

---

### `create`

```java
public static @NonNull ResponseCompletedEvent create(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      long startTimestampNanos,
      @Nullable String model)
```

Creates a minimal completed event.
