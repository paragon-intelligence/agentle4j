# :material-code-braces: FunctionTool

`com.paragon.responses.spec.FunctionTool` &nbsp;·&nbsp; **Class**

Extends `Record>` &nbsp;·&nbsp; Implements `Tool`

---

Defines a function in your own code the model can choose to call. Learn more about function calling.

## Methods

### `FunctionTool`

```java
public FunctionTool(@NonNull JsonSchemaProducer jsonSchemaProducer)
```

Creates a FunctionTool using metadata from the @FunctionMetadata annotation if present,
otherwise derives the name from the class name converted to snake_case.

---

### `FunctionTool`

```java
public FunctionTool(@NonNull Map<String, Object> parameters, boolean strict)
```

Creates a FunctionTool with manually specified parameters schema. Uses @FunctionMetadata for
name/description if present, otherwise derives from class name.

---

### `toSnakeCase`

```java
private static @NonNull String toSnakeCase(@NonNull String input)
```

Converts a PascalCase or camelCase string to snake_case. Example: "GetWeatherTool" ->
"get_weather_tool"

---

### `getParamClass`

```java
public @NonNull Class<P> getParamClass()
```

Returns the parameter class for this function tool. Used by the registry to deserialize JSON
arguments.

**Returns**

the parameter class

---

### `requiresConfirmation`

```java
public boolean requiresConfirmation()
```

Returns whether this tool requires human confirmation before execution.

When true, the tool will trigger the `onToolCallPending` or `onPause` callback
in AgentStream, allowing human-in-the-loop approval workflows.

**Returns**

true if confirmation is required, false otherwise

