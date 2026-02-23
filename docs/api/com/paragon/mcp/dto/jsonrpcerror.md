# :material-database: JsonRpcError

> This docs was updated at: 2026-02-23

`com.paragon.mcp.dto.JsonRpcError` &nbsp;·&nbsp; **Record**

---

A JSON-RPC 2.0 error object.

## Methods

### `parseError`

```java
public static JsonRpcError parseError(@NonNull String message)
```

Creates a parse error.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |

**Returns**

a new JsonRpcError

---

### `invalidRequest`

```java
public static JsonRpcError invalidRequest(@NonNull String message)
```

Creates an invalid request error.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |

**Returns**

a new JsonRpcError

---

### `methodNotFound`

```java
public static JsonRpcError methodNotFound(@NonNull String method)
```

Creates a method not found error.

**Parameters**

| Name | Description |
|------|-------------|
| `method` | the method that was not found |

**Returns**

a new JsonRpcError

---

### `internalError`

```java
public static JsonRpcError internalError(@NonNull String message)
```

Creates an internal error.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |

**Returns**

a new JsonRpcError

