# :material-code-braces: WebhookSignatureValidator

> This docs was updated at: 2026-02-23

`com.paragon.messaging.security.WebhookSignatureValidator` &nbsp;·&nbsp; **Class**

---

Validates WhatsApp webhook signatures using HMAC-SHA256.

WhatsApp webhook requests include an `X-Hub-Signature-256` header containing an
HMAC-SHA256 signature of the request payload. This class validates that signature to ensure the
webhook request is authentic.

### Security Features

  
- HMAC-SHA256 signature validation
- Constant-time comparison to prevent timing attacks
- Support for both hex-encoded and sha256= prefixed signatures

### Usage Example

{@code
// Create validator from security config
WebhookSignatureValidator validator = WebhookSignatureValidator.create(securityConfig);
// Validate in Spring controller

**See Also**

- `SecurityConfig`

*Since: 2.1*

## Methods

### `create`

```java
public static WebhookSignatureValidator create(@NonNull SecurityConfig config)
```

Creates a validator from a security configuration.

If signature validation is disabled in the config, the validator will always return true.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | the security configuration |

**Returns**

a new validator

---

### `create`

```java
public static WebhookSignatureValidator create(@NonNull String appSecret)
```

Creates a validator with the specified app secret.

**Parameters**

| Name | Description |
|------|-------------|
| `appSecret` | the WhatsApp app secret |

**Returns**

a new validator

---

### `disabled`

```java
public static WebhookSignatureValidator disabled()
```

Creates a disabled validator that always returns true.

Use for testing or when signature validation is not required.

**Returns**

a disabled validator

---

### `isValid`

```java
public boolean isValid(@Nullable String signature, @NonNull String payload)
```

Validates the webhook signature.

**Parameters**

| Name | Description |
|------|-------------|
| `signature` | the X-Hub-Signature-256 header value |
| `payload` | the raw request payload |

**Returns**

true if the signature is valid

---

### `isValid`

```java
public boolean isValid(@Nullable String signature, @NonNull byte[] payloadBytes)
```

Validates the webhook signature.

**Parameters**

| Name | Description |
|------|-------------|
| `signature` | the X-Hub-Signature-256 header value |
| `payloadBytes` | the raw request payload bytes |

**Returns**

true if the signature is valid

---

### `computeSignature`

```java
public String computeSignature(@NonNull String payload)
```

Computes the expected signature for a payload.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |

**Returns**

the hex-encoded signature

---

### `computeSignature`

```java
public String computeSignature(@NonNull byte[] payloadBytes)
```

Computes the expected signature for payload bytes.

**Parameters**

| Name | Description |
|------|-------------|
| `payloadBytes` | the request payload bytes |

**Returns**

the hex-encoded signature

---

### `isEnabled`

```java
public boolean isEnabled()
```

Checks if signature validation is enabled.

**Returns**

true if validation is enabled

---

### `normalizeSignature`

```java
private String normalizeSignature(String signature)
```

Normalizes the signature by removing the "sha256=" prefix if present.

---

### `constantTimeEquals`

```java
private boolean constantTimeEquals(String a, String b)
```

Constant-time comparison to prevent timing attacks.

This method compares two strings character by character, always comparing all characters
regardless of where differences occur. This prevents attackers from using timing information to
guess the signature.
