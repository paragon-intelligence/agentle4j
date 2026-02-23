# :material-code-braces: FloodDetector

> This docs was updated at: 2026-02-23

`com.paragon.messaging.security.FloodDetector` &nbsp;·&nbsp; **Class**

---

Detects message flooding from individual users.

Tracks message timestamps per user and blocks users who send too many messages within a
configured time window.

### Features

  
- Per-user message rate tracking
- Configurable window and threshold
- Thread-safe for concurrent access
- Automatic cleanup of expired data

### Usage Example

```java
// Create from security config
FloodDetector detector = FloodDetector.create(securityConfig);
// Check before processing
if (detector.isFlooding(userId)) {
    log.warn("Flood detected from user: {}", userId);
    return; // Reject message
}
// Record message (after processing or for blocking next time)
detector.recordMessage(userId);
// Periodic cleanup (e.g., scheduled task)
detector.cleanup();
```

**See Also**

- `SecurityConfig`

*Since: 2.1*

## Methods

### `create`

```java
public static FloodDetector create(@NonNull SecurityConfig config)
```

Creates a flood detector from a security configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | the security configuration |

**Returns**

a new flood detector

---

### `create`

```java
public static FloodDetector create(@NonNull Duration window, int maxMessages)
```

Creates a flood detector with custom settings.

**Parameters**

| Name | Description |
|------|-------------|
| `window` | the time window for counting messages |
| `maxMessages` | maximum messages allowed per window |

**Returns**

a new flood detector

---

### `disabled`

```java
public static FloodDetector disabled()
```

Creates a disabled flood detector that never detects flooding.

**Returns**

a disabled detector

---

### `isFlooding`

```java
public boolean isFlooding(@NonNull String userId)
```

Checks if a user is currently flooding (exceeding rate limit).

This method does NOT record a new message. Use `.recordMessage(String)` after
processing to track the message.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user identifier |

**Returns**

true if the user has exceeded the rate limit

---

### `recordMessage`

```java
public void recordMessage(@NonNull String userId)
```

Records a message from a user.

Call this after processing a message to track it for rate limiting.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user identifier |

---

### `checkAndRecord`

```java
public boolean checkAndRecord(@NonNull String userId)
```

Checks if flooding and records a message atomically.

Returns true if the user WAS flooding before this message. The message is recorded
regardless.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user identifier |

**Returns**

true if the user was already flooding

---

### `getMessageCount`

```java
public int getMessageCount(@NonNull String userId)
```

Returns the current message count for a user within the window.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user identifier |

**Returns**

message count in current window

---

### `getRemainingAllowance`

```java
public int getRemainingAllowance(@NonNull String userId)
```

Returns the remaining messages allowed for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user identifier |

**Returns**

remaining messages before rate limit

---

### `clearUser`

```java
public void clearUser(@NonNull String userId)
```

Clears all history for a specific user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user identifier |

---

### `clearAll`

```java
public void clearAll()
```

Clears all user histories.

---

### `cleanup`

```java
public int cleanup()
```

Removes expired entries from all user histories.

Call this periodically (e.g., every minute) to prevent memory growth.

**Returns**

number of users cleaned up (had all entries removed)

---

### `getTrackedUserCount`

```java
public int getTrackedUserCount()
```

Returns the number of users being tracked.

**Returns**

tracked user count

---

### `getWindow`

```java
public Duration getWindow()
```

Returns the configured window duration.

**Returns**

window duration

---

### `getMaxMessages`

```java
public int getMaxMessages()
```

Returns the maximum messages allowed per window.

**Returns**

max messages

---

### `isEnabled`

```java
public boolean isEnabled()
```

Checks if flood detection is enabled.

**Returns**

true if enabled

