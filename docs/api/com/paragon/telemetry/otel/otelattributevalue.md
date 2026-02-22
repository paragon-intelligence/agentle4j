# :material-database: OtelAttributeValue

`com.paragon.telemetry.otel.OtelAttributeValue` &nbsp;·&nbsp; **Record**

---

OpenTelemetry attribute value wrapper supporting different value types. Only one of the value
fields should be set.

Follows the OTLP specification for attribute values.

## Methods

### `ofString`

```java
public static @NonNull OtelAttributeValue ofString(@NonNull String value)
```

Creates a string attribute value.

---

### `ofBool`

```java
public static @NonNull OtelAttributeValue ofBool(boolean value)
```

Creates a boolean attribute value.

---

### `ofInt`

```java
public static @NonNull OtelAttributeValue ofInt(long value)
```

Creates an integer attribute value.

---

### `ofDouble`

```java
public static @NonNull OtelAttributeValue ofDouble(double value)
```

Creates a double attribute value.

---

### `of`

```java
public static @NonNull OtelAttributeValue of(@Nullable Object value)
```

Creates an attribute value from any object, inferring the correct type.
