# :material-code-braces: LangfusePromptProvider

`com.paragon.prompts.LangfusePromptProvider` &nbsp;·&nbsp; **Class**

Implements `PromptProvider`

---

A `PromptProvider` that retrieves prompts from the Langfuse API.

This provider fetches prompts from Langfuse's prompt management service, supporting versioned
prompts, labels, and both text and chat prompt types. It includes automatic retry with
exponential backoff for transient failures.

### Usage Example

```java
// Create provider with builder
PromptProvider provider = LangfusePromptProvider.builder()
    .httpClient(new OkHttpClient())
    .publicKey("pk-lf-xxx")
    .secretKey("sk-lf-xxx")
    .build();
// Retrieve a prompt
Prompt prompt = provider.providePrompt("my-prompt-name");
// Retrieve a specific version
Prompt prompt = provider.providePrompt("my-prompt", Map.of("version", "2"));
// Retrieve by label
Prompt prompt = provider.providePrompt("my-prompt", Map.of("label", "staging"));
```

### Supported Filters

  
- `version` - Retrieve a specific prompt version (integer)
- `label` - Retrieve prompt by label (e.g., "production", "staging")

*Since: 1.0*

## Fields

### `DEFAULT_BASE_URL`

```java
public static final String DEFAULT_BASE_URL = "https://cloud.langfuse.com"
```

Default Langfuse cloud API base URL.

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for LangfusePromptProvider.

**Returns**

a new builder instance

---

### `baseUrl`

```java
public @NonNull String baseUrl()
```

Returns the base URL for the Langfuse API.

**Returns**

the base URL

---

### `retryPolicy`

```java
public @NonNull RetryPolicy retryPolicy()
```

Returns the retry policy used by this provider.

**Returns**

the retry policy

---

### `httpClient`

```java
public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient)
```

Sets the HTTP client to use for API requests.

**Parameters**

| Name | Description |
|------|-------------|
| `httpClient` | the OkHttp client |

**Returns**

this builder

---

### `publicKey`

```java
public @NonNull Builder publicKey(@NonNull String publicKey)
```

Sets the Langfuse public key.

**Parameters**

| Name | Description |
|------|-------------|
| `publicKey` | the public API key |

**Returns**

this builder

---

### `secretKey`

```java
public @NonNull Builder secretKey(@NonNull String secretKey)
```

Sets the Langfuse secret key.

**Parameters**

| Name | Description |
|------|-------------|
| `secretKey` | the secret API key |

**Returns**

this builder

---

### `baseUrl`

```java
public @NonNull Builder baseUrl(@NonNull String baseUrl)
```

Sets the Langfuse API base URL.

Defaults to `LangfusePromptProvider#DEFAULT_BASE_URL`.

**Parameters**

| Name | Description |
|------|-------------|
| `baseUrl` | the base URL |

**Returns**

this builder

---

### `retryPolicy`

```java
public @NonNull Builder retryPolicy(@NonNull RetryPolicy retryPolicy)
```

Sets the retry policy for handling transient failures.

Defaults to `RetryPolicy.defaults()`.

**Parameters**

| Name | Description |
|------|-------------|
| `retryPolicy` | the retry policy |

**Returns**

this builder

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets a custom ObjectMapper for JSON deserialization.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the ObjectMapper |

**Returns**

this builder

---

### `fromEnv`

```java
public @NonNull Builder fromEnv()
```

Creates a provider using environment variables.

Reads `LANGFUSE_PUBLIC_KEY`, `LANGFUSE_SECRET_KEY`, and optionally `LANGFUSE_HOST`.

**Returns**

this builder with environment configuration

---

### `build`

```java
public @NonNull LangfusePromptProvider build()
```

Builds the LangfusePromptProvider.

**Returns**

a new `LangfusePromptProvider`

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if httpClient, publicKey, or secretKey is not set |

