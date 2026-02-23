# :material-code-braces: ConfigurationException

> This docs was updated at: 2026-02-23

`com.paragon.responses.exception.ConfigurationException` &nbsp;·&nbsp; **Class**

Extends `AgentleException`

---

Exception thrown when required configuration is missing or invalid.

This exception is thrown during builder validation and is not retryable.

Example usage:

```java
try {
    Responder responder = Responder.builder()
        .openRouter()
        // Missing apiKey!
        .build();
} catch (ConfigurationException e) {
    log.error("Config error: {}", e.suggestion());
}
```

## Methods

### `ConfigurationException`

```java
public ConfigurationException(
      @NonNull String message, @Nullable String configKey, @Nullable String suggestion)
```

Creates a new ConfigurationException.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `configKey` | the configuration key that is missing or invalid |
| `suggestion` | optional resolution hint |

---

### `missing`

```java
public static ConfigurationException missing(@NonNull String configKey)
```

Creates a missing configuration exception.

**Parameters**

| Name | Description |
|------|-------------|
| `configKey` | the missing configuration key |

**Returns**

a new ConfigurationException

---

### `invalid`

```java
public static ConfigurationException invalid(@NonNull String configKey, @NonNull String reason)
```

Creates an invalid configuration exception.

**Parameters**

| Name | Description |
|------|-------------|
| `configKey` | the invalid configuration key |
| `reason` | why the value is invalid |

**Returns**

a new ConfigurationException

---

### `configKey`

```java
public @Nullable String configKey()
```

Returns the configuration key that caused the error.

**Returns**

the config key, or null if not specific to a key

