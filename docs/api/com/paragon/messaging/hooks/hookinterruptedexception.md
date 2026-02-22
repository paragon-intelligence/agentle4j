# :material-code-braces: HookInterruptedException

`com.paragon.messaging.hooks.HookInterruptedException` &nbsp;·&nbsp; **Class**

Extends `Exception`

---

Exception thrown by a `ProcessingHook` to interrupt message processing.

This exception is used by pre-hooks to intentionally stop the processing pipeline (e.g., for
content moderation, rate limiting, or validation failures). It is NOT considered a processing
error and will NOT trigger retries.

### Example Usage

```java
ProcessingHook moderationHook = context -> {
    if (containsInappropriateContent(context.messages())) {
        throw new HookInterruptedException(
            "Inappropriate content detected",
            "CONTENT_MODERATION"
        );
    }
};
```

**See Also**

- `ProcessingHook`

*Since: 2.1*

## Methods

### `HookInterruptedException`

```java
public HookInterruptedException(@NonNull String reason)
```

Creates a new HookInterruptedException with a reason message.

**Parameters**

| Name | Description |
|------|-------------|
| `reason` | human-readable reason for interruption |

---

### `HookInterruptedException`

```java
public HookInterruptedException(@NonNull String reason, @Nullable String reasonCode)
```

Creates a new HookInterruptedException with reason and code.

**Parameters**

| Name | Description |
|------|-------------|
| `reason` | human-readable reason for interruption |
| `reasonCode` | machine-readable code (e.g., "CONTENT_MODERATION", "RATE_LIMIT") |

---

### `getReasonCode`

```java
public @Nullable String getReasonCode()
```

Returns the machine-readable reason code.

**Returns**

reason code or null if not set

---

### `getReason`

```java
public @NonNull String getReason()
```

Returns the human-readable reason.

**Returns**

reason message

