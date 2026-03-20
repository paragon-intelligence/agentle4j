# :material-database: SecurityConfig

`com.paragon.messaging.security.SecurityConfig` &nbsp;·&nbsp; **Record**

---

Security configuration for WhatsApp webhook and message processing.

Provides protection against:

  
- Webhook spoofing (signature validation)
- Message content attacks (XSS, injection)
- Flood attacks (rate limiting)
- Oversized messages (length limits)

### Usage Example

```java
// Basic security (no signature validation)
SecurityConfig config = SecurityConfig.defaults("my-verify-token");
// Strict security with signature validation
SecurityConfig config = SecurityConfig.strict("verify-token", "app-secret");
// Custom configuration
SecurityConfig config = SecurityConfig.builder()
    .webhookVerifyToken("my-token")
    .appSecret("my-secret")
    .validateSignatures(true)
    .contentSanitization(true)
    .maxMessageLength(2048)
    .blockedPatterns(Set.of("(?i)<script"))
    .floodPreventionWindow(Duration.ofSeconds(5))
    .maxMessagesPerWindow(10)
    .build();
```

*Since: 2.1*

## Fields

### `SecurityConfig`

```java
public SecurityConfig
```

Canonical constructor with validation.

## Methods

### `of`

```java
public static final Set<String> DEFAULT_BLOCKED_PATTERNS =
      Set.of(
          "(?i)(<script|javascript:|data:text/html|<iframe)", // XSS
          "(?i)(drop\\s+table|delete\\s+from|insert\\s+into|update\\s+.+\\s+set)", // SQL Injection
          "(?i)(\\$\\
```

Default blocked patterns for common attacks.

---

### `defaults`

```java
public static SecurityConfig defaults(@NonNull String verifyToken)
```

Creates a default security configuration.

Features:

  
- No signature validation
- Content sanitization enabled
- 4096 character message limit
- 10 second flood window with 20 message limit

**Parameters**

| Name | Description |
|------|-------------|
| `verifyToken` | the webhook verify token |

**Returns**

default security configuration

---

### `strict`

```java
public static SecurityConfig strict(@NonNull String verifyToken, @NonNull String appSecret)
```

Creates a strict security configuration.

Features:

  
- Signature validation enabled
- Content sanitization enabled
- Default attack pattern blocking
- 2048 character message limit
- 5 second flood window with 10 message limit

**Parameters**

| Name | Description |
|------|-------------|
| `verifyToken` | the webhook verify token |
| `appSecret` | the app secret for signature validation |

**Returns**

strict security configuration

---

### `builder`

```java
public static Builder builder()
```

Creates a new builder for SecurityConfig.

**Returns**

new builder

---

### `shouldValidateSignatures`

```java
public boolean shouldValidateSignatures()
```

Checks if signature validation is enabled and configured.

**Returns**

true if signatures should be validated

---

### `webhookVerifyToken`

```java
public Builder webhookVerifyToken(@NonNull String token)
```

Sets the webhook verification token.

**Parameters**

| Name | Description |
|------|-------------|
| `token` | the verify token |

**Returns**

this builder

---

### `appSecret`

```java
public Builder appSecret(@Nullable String secret)
```

Sets the app secret for signature validation.

**Parameters**

| Name | Description |
|------|-------------|
| `secret` | the app secret |

**Returns**

this builder

---

### `validateSignatures`

```java
public Builder validateSignatures(boolean validate)
```

Enables or disables signature validation.

**Parameters**

| Name | Description |
|------|-------------|
| `validate` | true to enable signature validation |

**Returns**

this builder

---

### `contentSanitization`

```java
public Builder contentSanitization(boolean sanitize)
```

Enables or disables content sanitization.

**Parameters**

| Name | Description |
|------|-------------|
| `sanitize` | true to enable sanitization |

**Returns**

this builder

---

### `maxMessageLength`

```java
public Builder maxMessageLength(int length)
```

Sets the maximum allowed message length.

**Parameters**

| Name | Description |
|------|-------------|
| `length` | maximum characters |

**Returns**

this builder

---

### `blockedPatterns`

```java
public Builder blockedPatterns(@NonNull Set<String> patterns)
```

Sets the regex patterns to block.

**Parameters**

| Name | Description |
|------|-------------|
| `patterns` | set of regex patterns |

**Returns**

this builder

---

### `floodPreventionWindow`

```java
public Builder floodPreventionWindow(@NonNull Duration window)
```

Sets the flood prevention time window.

**Parameters**

| Name | Description |
|------|-------------|
| `window` | the time window |

**Returns**

this builder

---

### `maxMessagesPerWindow`

```java
public Builder maxMessagesPerWindow(int max)
```

Sets the maximum messages allowed per user in the flood window.

**Parameters**

| Name | Description |
|------|-------------|
| `max` | maximum messages |

**Returns**

this builder

---

### `build`

```java
public SecurityConfig build()
```

Builds the SecurityConfig.

**Returns**

the built configuration

