# :material-database: TelemetryContext

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.TelemetryContext` &nbsp;·&nbsp; **Record**

---

Context for telemetry events to add user metadata, tags, and custom attributes.

Used to enrich OpenTelemetry spans with additional context for vendors like Langfuse and
Grafana.

Usage:

```java
var context = TelemetryContext.builder()
    .userId("user-123")
    .traceName("chat.completion")
    .addTag("production")
    .addMetadata("version", "1.0")
    .build();
responder.respond(payload, sessionId, context);
```

## Methods

### `empty`

```java
public static @NonNull TelemetryContext empty()
```

Creates an empty telemetry context.

---

### `forUser`

```java
public static @NonNull TelemetryContext forUser(@NonNull String userId)
```

Creates a minimal context with just a user ID.

---

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder.

---

### `toAttributes`

```java
public @NonNull Map<String, Object> toAttributes()
```

Returns all context data as OpenTelemetry attributes.

---

### `getTraceNameOrDefault`

```java
public @NonNull String getTraceNameOrDefault(@NonNull String defaultName)
```

Returns the trace name or the default.

---

### `userId`

```java
public @NonNull Builder userId(@NonNull String userId)
```

Sets the user ID for telemetry correlation.

---

### `traceName`

```java
public @NonNull Builder traceName(@NonNull String traceName)
```

Sets the trace/span name.

---

### `parentTraceId`

```java
public @NonNull Builder parentTraceId(@NonNull String parentTraceId)
```

Sets the parent trace ID for distributed tracing.

---

### `parentSpanId`

```java
public @NonNull Builder parentSpanId(@NonNull String parentSpanId)
```

Sets the parent span ID for distributed tracing.

---

### `requestId`

```java
public @NonNull Builder requestId(@NonNull String requestId)
```

Sets the request ID for high-level correlation.

---

### `addMetadata`

```java
public @NonNull Builder addMetadata(@NonNull String key, @NonNull Object value)
```

Adds a metadata key-value pair.

---

### `metadata`

```java
public @NonNull Builder metadata(@NonNull Map<String, Object> metadata)
```

Adds all metadata from a map.

---

### `addTag`

```java
public @NonNull Builder addTag(@NonNull String tag)
```

Adds a tag.

---

### `tags`

```java
public @NonNull Builder tags(@NonNull List<String> tags)
```

Adds multiple tags.

---

### `build`

```java
public @NonNull TelemetryContext build()
```

Builds the TelemetryContext.
