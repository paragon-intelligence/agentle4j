# :material-database: ResponseStartedEvent

`com.paragon.telemetry.events.ResponseStartedEvent` &nbsp;·&nbsp; **Record**

---

Event emitted when a respond() call begins.

This event marks the start of a span and includes information about the request being made
(model, input context).

## Methods

### `create`

```java
public static @NonNull ResponseStartedEvent create(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      @Nullable String model)
```

Creates a minimal started event with only required fields.

---

### `create`

```java
public static @NonNull ResponseStartedEvent create(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      @Nullable String model,
      @NonNull TelemetryContext context)
```

Creates a started event with TelemetryContext for custom user metadata.

---

### `createWithParent`

```java
public static @NonNull ResponseStartedEvent createWithParent(
      @NonNull String sessionId,
      @NonNull String traceId,
      @NonNull String spanId,
      @NonNull String parentSpanId,
      @Nullable String model)
```

Creates a started event with a parent span for nested operations.
