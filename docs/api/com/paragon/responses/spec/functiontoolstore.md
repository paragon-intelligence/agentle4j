# :material-code-braces: FunctionToolStore

`com.paragon.responses.spec.FunctionToolStore` &nbsp;·&nbsp; **Class**

---

A store that maps function names to their implementations.

This store enables calling function tools directly from API responses by binding the function
name and JSON arguments from the response to the actual function implementation.

Example usage:

```java
// Create and add tools
var getWeatherTool = new GetWeatherTool();
var store = FunctionToolStore.create()
    .add(getWeatherTool);
// Make API call
var response = responder.respond(payload).join();
// Get callable function tool calls
var functionToolCalls = response.functionToolCalls(store);
FunctionToolCallOutput result = functionToolCalls.getFirst().call();
```

## Methods

### `create`

```java
public static @NonNull FunctionToolStore create()
```

Creates a new FunctionToolStore with a default ObjectMapper.

**Returns**

a new empty store

---

### `create`

```java
public static @NonNull FunctionToolStore create(@NonNull ObjectMapper objectMapper)
```

Creates a new FunctionToolStore with the specified ObjectMapper.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the ObjectMapper to use for deserializing function arguments |

**Returns**

a new empty store

---

### `add`

```java
public @NonNull FunctionToolStore add(@NonNull FunctionTool<?> tool)
```

Adds a function tool to this store. The tool will be stored under its name as returned by
`FunctionTool.getName()`.

**Parameters**

| Name | Description |
|------|-------------|
| `tool` | the function tool to add |

**Returns**

this store for method chaining

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if a tool with the same name is already stored |

---

### `addAll`

```java
public @NonNull FunctionToolStore addAll(@NonNull FunctionTool<?>... tools)
```

Adds multiple function tools to this store.

**Parameters**

| Name | Description |
|------|-------------|
| `tools` | the function tools to add |

**Returns**

this store for method chaining

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if any tool has a duplicate name |

---

### `addAll`

```java
public @NonNull FunctionToolStore addAll(@NonNull Iterable<? extends FunctionTool<?>> tools)
```

Adds multiple function tools to this store.

**Parameters**

| Name | Description |
|------|-------------|
| `tools` | the function tools to add |

**Returns**

this store for method chaining

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if any tool has a duplicate name |

---

### `bind`

```java
public @NonNull BoundedFunctionCall bind(@NonNull FunctionToolCall toolCall)
```

Binds a function tool call from the API response to its implementation.

This creates a `BoundedFunctionCall` that can be invoked via `BoundedFunctionCall.call()`.

**Parameters**

| Name | Description |
|------|-------------|
| `toolCall` | the function tool call from the API response |

**Returns**

a bounded function call that can be invoked

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if no tool is stored for the function name |

---

### `bindAll`

```java
public @NonNull List<BoundedFunctionCall> bindAll(@NonNull List<FunctionToolCall> toolCalls)
```

Binds all function tool calls from the API response to their implementations.

**Parameters**

| Name | Description |
|------|-------------|
| `toolCalls` | the function tool calls from the API response |

**Returns**

a list of bounded function calls that can be invoked

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if any tool call references a function not in the store |

---

### `contains`

```java
public boolean contains(@NonNull String name)
```

Checks if a tool with the given name is stored.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the function name to check |

**Returns**

true if a tool with this name is stored

---

### `get`

```java
public FunctionTool<?> get(@NonNull String name)
```

Gets a tool by name.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the function name |

**Returns**

the tool, or null if not found

---

### `getObjectMapper`

```java
public @NonNull ObjectMapper getObjectMapper()
```

Returns the ObjectMapper used by this store.

**Returns**

the ObjectMapper

---

### `execute`

```java
public @NonNull FunctionToolCallOutput execute(@NonNull FunctionToolCall toolCall)
      throws JsonProcessingException
```

Executes a function tool call and returns the result.

This is a convenience method that binds and calls in one step.

**Parameters**

| Name | Description |
|------|-------------|
| `toolCall` | the function tool call to execute |

**Returns**

the function tool call output

**Throws**

| Type | Condition |
|------|-----------|
| `JsonProcessingException` | if the arguments cannot be deserialized |
| `IllegalArgumentException` | if no tool is stored for the function name |

---

### `executeAll`

```java
public @NonNull List<FunctionToolCallOutput> executeAll(@NonNull List<FunctionToolCall> toolCalls)
      throws JsonProcessingException
```

Executes all function tool calls and returns their results.

**Parameters**

| Name | Description |
|------|-------------|
| `toolCalls` | the function tool calls to execute |

**Returns**

a list of function tool call outputs

**Throws**

| Type | Condition |
|------|-----------|
| `JsonProcessingException` | if any arguments cannot be deserialized |
| `IllegalArgumentException` | if any tool call references a function not in the store |

