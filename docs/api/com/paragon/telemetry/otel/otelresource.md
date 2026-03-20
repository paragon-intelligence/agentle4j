# :material-database: OtelResource

`com.paragon.telemetry.otel.OtelResource` &nbsp;·&nbsp; **Record**

---

OpenTelemetry resource describing the entity producing telemetry. Contains attributes like
service.name, service.version.

## Methods

### `forService`

```java
public static @NonNull OtelResource forService(
      @NonNull String serviceName, @NonNull String serviceVersion)
```

Creates a resource with service identification.

---

### `forAgentle`

```java
public static @NonNull OtelResource forAgentle()
```

Creates a resource for Agentle library.

---

### `agentle`

```java
public static @NonNull OtelResource agentle()
```

Alias for forAgentle().
