# :material-code-braces: GrafanaProcessor

`com.paragon.telemetry.grafana.GrafanaProcessor` &nbsp;·&nbsp; **Class**

Extends `TelemetryProcessor`

---

Telemetry processor that sends telemetry to Grafana Cloud via OTLP/HTTP.

Grafana supports traces, metrics, and logs. Each signal type can be individually
enabled/disabled via the builder.

Default configuration enables traces only. Enable metrics and logs as needed for your
observability requirements.

**See Also**

- `<a href="https://grafana.com/docs/grafana-cloud/send-data/otlp/">Grafana OTLP Docs</a>`

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for GrafanaProcessor.

---

### `fromEnv`

```java
public static @NonNull GrafanaProcessor fromEnv()
```

Creates a processor from environment variables.

---

### `sendTrace`

```java
private void sendTrace(@NonNull TelemetryEvent event)
```

Sends trace data to Grafana.

---

### `sendMetrics`

```java
private void sendMetrics(@NonNull ResponseCompletedEvent event)
```

Sends metrics data to Grafana. Creates gauge/counter metrics from the completed event.

---

### `sendLog`

```java
private void sendLog(@NonNull TelemetryEvent event)
```

Sends log data to Grafana.

---

### `convertToSpan`

```java
private @NonNull OtelSpan convertToSpan(@NonNull TelemetryEvent event)
```

Converts a TelemetryEvent to an OtelSpan.

---

### `sendToEndpoint`

```java
private void sendToEndpoint(
      @NonNull String endpoint, @NonNull String json, @NonNull String type)
```

Sends JSON payload to the specified endpoint.

---

### `isTracesEnabled`

```java
public boolean isTracesEnabled()
```

Returns whether traces are enabled.

---

### `isMetricsEnabled`

```java
public boolean isMetricsEnabled()
```

Returns whether metrics are enabled.

---

### `isLogsEnabled`

```java
public boolean isLogsEnabled()
```

Returns whether logs are enabled.

---

### `httpClient`

```java
public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient)
```

Sets the HTTP client.

---

### `baseUrl`

```java
public @NonNull Builder baseUrl(@NonNull String baseUrl)
```

Sets the Grafana OTLP base URL (e.g., https://otlp-gateway-prod-us-east-0.grafana.net/otlp).

---

### `instanceId`

```java
public @NonNull Builder instanceId(@NonNull String instanceId)
```

Sets the Grafana Cloud instance ID.

---

### `apiToken`

```java
public @NonNull Builder apiToken(@NonNull String apiToken)
```

Sets the Grafana Cloud API token.

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets the ObjectMapper for JSON serialization.

---

### `tracesEnabled`

```java
public @NonNull Builder tracesEnabled(boolean enabled)
```

Enables or disables trace sending.

---

### `metricsEnabled`

```java
public @NonNull Builder metricsEnabled(boolean enabled)
```

Enables or disables metrics sending.

---

### `logsEnabled`

```java
public @NonNull Builder logsEnabled(boolean enabled)
```

Enables or disables log sending.

---

### `fromEnv`

```java
public @NonNull Builder fromEnv()
```

Loads configuration from environment variables.

---

### `build`

```java
public @NonNull GrafanaProcessor build()
```

Builds the GrafanaProcessor.
