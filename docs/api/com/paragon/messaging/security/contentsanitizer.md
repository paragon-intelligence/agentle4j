# :material-code-braces: ContentSanitizer

> This docs was updated at: 2026-02-23

`com.paragon.messaging.security.ContentSanitizer` &nbsp;·&nbsp; **Class**

---

Sanitizes message content to prevent injection attacks.

Provides protection against:

  
- XSS (Cross-Site Scripting) patterns
- SQL injection patterns
- Template injection patterns
- Oversized messages
- Control characters and null bytes

### Usage Example

```java
// Create from security config
ContentSanitizer sanitizer = ContentSanitizer.create(securityConfig);
// Validate content
ContentSanitizer.ValidationResult result = sanitizer.validate(messageContent);
if (!result.isValid()) {
    log.warn("Content blocked: {}", result.blockedPatterns());
    return; // Reject message
}
// Sanitize content (removes dangerous patterns)
String safe = sanitizer.sanitize(messageContent);
```

**See Also**

- `SecurityConfig`

*Since: 2.1*

## Methods

### `create`

```java
public static ContentSanitizer create(@NonNull SecurityConfig config)
```

Creates a sanitizer from a security configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | the security configuration |

**Returns**

a new sanitizer

---

### `withDefaults`

```java
public static ContentSanitizer withDefaults(int maxLength)
```

Creates a sanitizer with default blocked patterns.

**Parameters**

| Name | Description |
|------|-------------|
| `maxLength` | maximum allowed message length |

**Returns**

a new sanitizer with default patterns

---

### `disabled`

```java
public static ContentSanitizer disabled()
```

Creates a disabled sanitizer that accepts all content.

**Returns**

a disabled sanitizer

---

### `validate`

```java
public ValidationResult validate(@Nullable String content)
```

Validates content against security rules.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the content to validate |

**Returns**

validation result with details

---

### `isValid`

```java
public boolean isValid(@Nullable String content)
```

Checks if content is valid without returning detailed results.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the content to check |

**Returns**

true if content passes all checks

---

### `sanitize`

```java
public String sanitize(@Nullable String content)
```

Sanitizes content by removing or replacing dangerous patterns.

This method:

  
- Removes null bytes and control characters
- Normalizes whitespace
- Truncates to max length
- Removes matched blocked patterns

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the content to sanitize |

**Returns**

sanitized content

---

### `isWithinLength`

```java
public boolean isWithinLength(@Nullable String content)
```

Checks if content passes length check.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the content to check |

**Returns**

true if content is within max length

---

### `getMaxLength`

```java
public int getMaxLength()
```

Returns the maximum allowed content length.

**Returns**

max length

---

### `isEnabled`

```java
public boolean isEnabled()
```

Checks if sanitization is enabled.

**Returns**

true if enabled

---

### `valid`

```java
public static ValidationResult valid()
```

Creates a valid result.

**Returns**

valid result

---

### `invalid`

```java
public static ValidationResult invalid(String issue)
```

Creates an invalid result with a single issue.

**Parameters**

| Name | Description |
|------|-------------|
| `issue` | the validation issue |

**Returns**

invalid result

---

### `hasBlockedPatterns`

```java
public boolean hasBlockedPatterns()
```

Checks if any blocked patterns were matched.

**Returns**

true if patterns were matched

