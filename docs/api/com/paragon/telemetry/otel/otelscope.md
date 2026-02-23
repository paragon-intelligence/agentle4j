# :material-database: OtelScope

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.otel.OtelScope` &nbsp;·&nbsp; **Record**

---

OpenTelemetry instrumentation scope information. Identifies the library that produces the
telemetry.

## Methods

### `forAgentle`

```java
public static @NonNull OtelScope forAgentle()
```

Creates a scope for Agentle library.

---

### `of`

```java
public static @NonNull OtelScope of(@NonNull String name, @NonNull String version)
```

Creates a scope with just name and version.

---

### `agentle`

```java
public static @NonNull OtelScope agentle()
```

Alias for forAgentle().
