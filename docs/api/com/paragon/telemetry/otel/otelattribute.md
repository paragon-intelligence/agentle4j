# :material-database: OtelAttribute

`com.paragon.telemetry.otel.OtelAttribute` &nbsp;·&nbsp; **Record**

---

OpenTelemetry key-value attribute pair. Used for resources, scopes, and spans.

Follows the OTLP specification for attributes.

## Methods

### `ofString`

```java
public static @NonNull OtelAttribute ofString(@NonNull String key, @NonNull String value)
```

Creates a string attribute.

---

### `ofBool`

```java
public static @NonNull OtelAttribute ofBool(@NonNull String key, boolean value)
```

Creates a boolean attribute.

---

### `ofInt`

```java
public static @NonNull OtelAttribute ofInt(@NonNull String key, long value)
```

Creates an integer attribute.

---

### `ofDouble`

```java
public static @NonNull OtelAttribute ofDouble(@NonNull String key, double value)
```

Creates a double attribute.

---

### `of`

```java
public static @NonNull OtelAttribute of(@NonNull String key, @NonNull Object value)
```

Creates an attribute from any value, inferring the type.
