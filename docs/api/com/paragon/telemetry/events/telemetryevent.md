# :material-approximately-equal: TelemetryEvent

`com.paragon.telemetry.events.TelemetryEvent` &nbsp;·&nbsp; **Interface**

---

Base sealed interface for all telemetry events emitted by the Responder.

Each event carries OpenTelemetry-compatible identifiers (traceId, spanId) and a sessionId for
correlating events within a single respond() call.

Events are designed to be immutable and thread-safe for async processing.

## Methods

### `sessionId`

```java
String sessionId()
```

Unique session identifier for correlating events within a single respond() call. Either
provided by the user or auto-generated as a UUID.

---

### `traceId`

```java
String traceId()
```

OpenTelemetry trace ID (16-byte hex string, 32 characters). Groups all spans that are part of
the same distributed trace.

---

### `spanId`

```java
String spanId()
```

OpenTelemetry span ID (8-byte hex string, 16 characters). Unique identifier for this specific
span/event.

---

### `parentSpanId`

```java
String parentSpanId()
```

Parent span ID for trace correlation, null if this is the root span.

---

### `timestampNanos`

```java
long timestampNanos()
```

Event timestamp in nanoseconds since Unix epoch. Used for precise timing in OpenTelemetry
exporters.

---

### `attributes`

```java
Map<String, Object> attributes()
```

Additional key-value attributes for the event. Follows OpenTelemetry semantic conventions.
