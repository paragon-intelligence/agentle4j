# :material-database: ProcessorRegistry

`com.paragon.telemetry.processors.ProcessorRegistry` &nbsp;·&nbsp; **Record**

---

Registry for managing multiple telemetry processors. Broadcasts events to all registered
processors.

This replaces the old `OpenTelemetryVendors` class with a cleaner API and proper
lifecycle management.

## Methods

### `empty`

```java
public static @NonNull ProcessorRegistry empty()
```

Creates an empty registry.

---

### `of`

```java
public static @NonNull ProcessorRegistry of(@NonNull List<TelemetryProcessor> processors)
```

Creates a registry with the given processors.

---

### `of`

```java
public static @NonNull ProcessorRegistry of(@NonNull TelemetryProcessor processor)
```

Creates a registry with a single processor.

---

### `broadcast`

```java
public void broadcast(@NonNull TelemetryEvent event)
```

Broadcasts an event to all registered processors. This is fire-and-forget - it does not wait
for processing.

**Parameters**

| Name | Description |
|------|-------------|
| `event` | the event to broadcast |

---

### `flushAll`

```java
public boolean flushAll(long timeout, @NonNull TimeUnit unit)
```

Flushes all processors.

**Parameters**

| Name | Description |
|------|-------------|
| `timeout` | max time to wait for each processor |
| `unit` | time unit |

**Returns**

true if all processors flushed successfully

---

### `shutdown`

```java
public void shutdown()
```

Shuts down all registered processors gracefully.

---

### `hasProcessors`

```java
public boolean hasProcessors()
```

Returns whether any processors are registered.

---

### `size`

```java
public int size()
```

Returns the number of registered processors.
