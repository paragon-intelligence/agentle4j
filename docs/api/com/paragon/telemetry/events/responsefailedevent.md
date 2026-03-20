# :material-database: ResponseFailedEvent

`com.paragon.telemetry.events.ResponseFailedEvent` &nbsp;·&nbsp; **Record**

---

Event emitted when a respond() call fails with an error.

This event marks the end of a span with error status and includes exception details for
debugging.

## Methods

### `durationNanos`

```java
public long durationNanos()
```

Duration of the operation in nanoseconds before failure.

---

### `durationMs`

```java
public long durationMs()
```

Duration of the operation in milliseconds before failure.

---

### `from`

```java
public static @NonNull ResponseFailedEvent from(
      @NonNull ResponseStartedEvent startedEvent, @NonNull Throwable exception)
```

Creates a failed event from a started event and an exception.

---

### `fromHttpError`

```java
public static @NonNull ResponseFailedEvent fromHttpError(
      @NonNull ResponseStartedEvent startedEvent,
      int httpStatusCode,
      @NonNull String errorMessage)
```

Creates a failed event from a started event with HTTP error details.
