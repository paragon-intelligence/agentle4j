# :material-code-braces: TelemetryProcessor

`com.paragon.telemetry.processors.TelemetryProcessor` &nbsp;·&nbsp; **Class**

---

Abstract base class for async telemetry event processors.

Provides queue-based event processing with background worker threads. Implementations override
`.doProcess` to handle events.

Key design principles:

  
- Fire-and-forget: `.process` returns immediately
- Non-blocking: events are queued and processed asynchronously
- Graceful shutdown: flush pending events before termination

## Methods

### `TelemetryProcessor`

```java
protected TelemetryProcessor(@NonNull String processorName)
```

Creates a processor with default queue size (1000) and single worker thread.

---

### `TelemetryProcessor`

```java
protected TelemetryProcessor(@NonNull String processorName, int maxQueueSize, int workerThreads)
```

Creates a processor with custom queue size and worker thread count.

---

### `process`

```java
public void process(@NonNull TelemetryEvent event)
```

Queues an event for async processing. Returns immediately without blocking (fire-and-forget).

**Parameters**

| Name | Description |
|------|-------------|
| `event` | the telemetry event to process |

---

### `doProcess`

```java
protected abstract void doProcess(@NonNull TelemetryEvent event)
```

Processes an event. Implementations should handle the event according to their vendor-specific
logic (e.g., send to Langfuse, Grafana).

This method is called on a background thread and should not block for extended periods.

**Parameters**

| Name | Description |
|------|-------------|
| `event` | the event to process |

---

### `processLoop`

```java
private void processLoop()
```

Background worker loop that consumes events from the queue.

---

### `flush`

```java
public boolean flush(long timeout, @NonNull TimeUnit unit)
```

Flushes all pending events by waiting for the queue to drain. Blocks until all events are
processed or timeout is reached.

**Parameters**

| Name | Description |
|------|-------------|
| `timeout` | max time to wait |
| `unit` | time unit |

**Returns**

true if all events were flushed, false if timeout

---

### `shutdown`

```java
public void shutdown()
```

Gracefully shuts down the processor. Attempts to flush pending events before termination.

---

### `getProcessorName`

```java
public @NonNull String getProcessorName()
```

Returns the processor name for logging and identification.

---

### `getQueueSize`

```java
public int getQueueSize()
```

Returns the current queue size.

---

### `isRunning`

```java
public boolean isRunning()
```

Returns whether the processor is running.
