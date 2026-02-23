# :material-code-braces: LangfuseProcessor

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.langfuse.LangfuseProcessor` &nbsp;·&nbsp; **Class**

Extends `TelemetryProcessor`

---

Telemetry processor that sends traces to Langfuse via OTLP/HTTP.

Langfuse only supports traces (not metrics or logs), so this processor converts
TelemetryEvents into OtelSpans and sends them to the `/api/public/otel/v1/traces` endpoint.

Authentication uses HTTP Basic Auth with public key as username and secret key as password.

**See Also**

- `<a href="https://langfuse.com/docs/integrations/opentelemetry">Langfuse OpenTelemetry Docs</a>`

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for LangfuseProcessor.

---

### `fromEnv`

```java
public static @NonNull LangfuseProcessor fromEnv()
```

Creates a processor from environment variables. Requires LANGFUSE_PUBLIC_KEY and
LANGFUSE_SECRET_KEY.

---

### `fromEnv`

```java
public static @NonNull LangfuseProcessor fromEnv(@NonNull OkHttpClient httpClient)
```

Creates a processor from environment variables with a custom HTTP client.

---

### `convertToSpan`

```java
private @NonNull OtelSpan convertToSpan(@NonNull TelemetryEvent event)
```

Converts a TelemetryEvent to an OtelSpan.

---

### `sendToLangfuse`

```java
private void sendToLangfuse(@NonNull String json)
```

Sends JSON payload to Langfuse OTEL endpoint.

---

### `httpClient`

```java
public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient)
```

Sets the HTTP client.

---

### `endpoint`

```java
public @NonNull Builder endpoint(@NonNull String endpoint)
```

Sets the Langfuse endpoint URL.

---

### `publicKey`

```java
public @NonNull Builder publicKey(@NonNull String publicKey)
```

Sets the Langfuse public key.

---

### `secretKey`

```java
public @NonNull Builder secretKey(@NonNull String secretKey)
```

Sets the Langfuse secret key.

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets the ObjectMapper for JSON serialization.

---

### `fromEnv`

```java
public @NonNull Builder fromEnv()
```

Loads configuration from environment variables. Reads LANGFUSE_PUBLIC_KEY,
LANGFUSE_SECRET_KEY, and optionally LANGFUSE_HOST.

---

### `build`

```java
public @NonNull LangfuseProcessor build()
```

Builds the LangfuseProcessor.
