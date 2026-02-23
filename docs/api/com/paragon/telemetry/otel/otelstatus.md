# :material-database: OtelStatus

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelStatus` &nbsp;·&nbsp; **Record**

---

OpenTelemetry span status. Indicates whether the span completed successfully, with errors, or
unset.

## Fields

### `STATUS_CODE_UNSET`

```java
public static final int STATUS_CODE_UNSET = 0
```

Status codes as per OpenTelemetry spec.

## Methods

### `unset`

```java
public static @NonNull OtelStatus unset()
```

Creates an unset status (default).

---

### `ok`

```java
public static @NonNull OtelStatus ok()
```

Creates an OK status indicating successful completion.

---

### `error`

```java
public static @NonNull OtelStatus error(@NonNull String message)
```

Creates an error status with a message.
