# :material-database: FunctionToolCallOutput

`com.paragon.responses.spec.FunctionToolCallOutput` &nbsp;·&nbsp; **Record**

---

The output of a function tool call.

## Methods

### `success`

```java
public static FunctionToolCallOutput success(@NonNull String message)
```

Creates a successful text output with a generated call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | The success message |

**Returns**

A FunctionToolCallOutput with the text

---

### `success`

```java
public static FunctionToolCallOutput success(@NonNull String callId, @NonNull String message)
```

Creates a successful text output with a specific call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `callId` | The call ID |
| `message` | The success message |

**Returns**

A FunctionToolCallOutput with the text

---

### `error`

```java
public static FunctionToolCallOutput error(@NonNull String errorMessage)
```

Creates an error text output with a generated call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `errorMessage` | The error message |

**Returns**

A FunctionToolCallOutput with the error text

---

### `error`

```java
public static FunctionToolCallOutput error(@NonNull String callId, @NonNull String errorMessage)
```

Creates an error text output with a specific call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `callId` | The call ID |
| `errorMessage` | The error message |

**Returns**

A FunctionToolCallOutput with the error text

---

### `inProgress`

```java
public static FunctionToolCallOutput inProgress(@NonNull String message)
```

Creates an in-progress text output with a generated call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | The progress message |

**Returns**

A FunctionToolCallOutput with in-progress status

---

### `inProgress`

```java
public static FunctionToolCallOutput inProgress(@NonNull String callId, @NonNull String message)
```

Creates an in-progress text output with a specific call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `callId` | The call ID |
| `message` | The progress message |

**Returns**

A FunctionToolCallOutput with in-progress status

---

### `withImage`

```java
public static FunctionToolCallOutput withImage(@NonNull Image image)
```

Creates an output with an image result.

**Parameters**

| Name | Description |
|------|-------------|
| `image` | The image output |

**Returns**

A FunctionToolCallOutput with the image

---

### `withImage`

```java
public static FunctionToolCallOutput withImage(@NonNull String callId, @NonNull Image image)
```

Creates an output with an image result and specific call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `callId` | The call ID |
| `image` | The image output |

**Returns**

A FunctionToolCallOutput with the image

---

### `withFile`

```java
public static FunctionToolCallOutput withFile(@NonNull File file)
```

Creates an output with a file result.

**Parameters**

| Name | Description |
|------|-------------|
| `file` | The file output |

**Returns**

A FunctionToolCallOutput with the file

---

### `withFile`

```java
public static FunctionToolCallOutput withFile(@NonNull String callId, @NonNull File file)
```

Creates an output with a file result and specific call ID.

**Parameters**

| Name | Description |
|------|-------------|
| `callId` | The call ID |
| `file` | The file output |

**Returns**

A FunctionToolCallOutput with the file

---

### `generateCallId`

```java
private static String generateCallId()
```

Generates a unique call ID.

**Returns**

A unique call ID string

