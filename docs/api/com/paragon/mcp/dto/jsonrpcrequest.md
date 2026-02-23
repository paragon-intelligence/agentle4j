# :material-database: JsonRpcRequest

> This docs was updated at: 2026-02-23

`com.paragon.mcp.dto.JsonRpcRequest` &nbsp;·&nbsp; **Record**

---

A JSON-RPC 2.0 request message.

## Methods

### `create`

```java
public static JsonRpcRequest create(
      @NonNull Object id, @NonNull String method, @Nullable Object params)
```

Creates a new JSON-RPC request.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the request identifier |
| `method` | the method name |
| `params` | the method parameters (can be null) |

**Returns**

a new JsonRpcRequest

---

### `create`

```java
public static JsonRpcRequest create(@NonNull Object id, @NonNull String method)
```

Creates a new JSON-RPC request without parameters.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the request identifier |
| `method` | the method name |

**Returns**

a new JsonRpcRequest

