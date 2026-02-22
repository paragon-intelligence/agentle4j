# :material-code-braces: FunctionToolFactory

`com.paragon.responses.spec.FunctionToolFactory` &nbsp;·&nbsp; **Class**

---

A factory for creating `FunctionTool` instances with a shared `JsonSchemaProducer`.

This factory simplifies dependency injection by providing a centralized way to create function
tools with a consistent JSON schema producer configuration.

Example usage:

```java
// Create factory with custom ObjectMapper
var factory = FunctionToolFactory.withObjectMapper(objectMapper);
// Create tools using the factory
var weatherTool = factory.create(GetWeatherTool.class);
var calculatorTool = factory.create(CalculatorTool.class);
// Add to store
var store = FunctionToolStore.create(objectMapper)
    .addAll(weatherTool, calculatorTool);
```

**See Also**

- `FunctionTool`
- `FunctionToolStore`

## Methods

### `create`

```java
public static @NonNull FunctionToolFactory create()
```

Creates a new factory with a default `JacksonJsonSchemaProducer`.

**Returns**

a new factory

---

### `withObjectMapper`

```java
public static @NonNull FunctionToolFactory withObjectMapper(@NonNull ObjectMapper objectMapper)
```

Creates a new factory with a `JacksonJsonSchemaProducer` using the provided ObjectMapper.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the ObjectMapper to use for JSON schema generation |

**Returns**

a new factory

---

### `withProducer`

```java
public static @NonNull FunctionToolFactory withProducer(
      @NonNull JsonSchemaProducer jsonSchemaProducer)
```

Creates a new factory with the provided `JsonSchemaProducer`.

**Parameters**

| Name | Description |
|------|-------------|
| `jsonSchemaProducer` | the JSON schema producer to use |

**Returns**

a new factory

---

### `create`

```java
public <T extends FunctionTool<?>> @NonNull T create(@NonNull Class<T> toolClass)
```

Creates a new instance of the specified `FunctionTool` class.

The tool class must have a constructor that accepts a `JsonSchemaProducer`.

**Parameters**

| Name | Description |
|------|-------------|
| `toolClass` | the class of the function tool to create |
| `<T>` | the type of the function tool |

**Returns**

a new instance of the function tool

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if the tool cannot be instantiated |

---

### `getJsonSchemaProducer`

```java
public @NonNull JsonSchemaProducer getJsonSchemaProducer()
```

Returns the `JsonSchemaProducer` used by this factory.

**Returns**

the JSON schema producer

