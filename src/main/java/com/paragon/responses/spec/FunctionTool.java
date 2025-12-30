package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines a function in your own code the model can choose to call. Learn more about <a
 * href="https://platform.openai.com/docs/guides/function-calling">function calling</a>.
 *
 * @param <P> the parameters the function accept in a Java Record format.
 */
public abstract non-sealed class FunctionTool<P extends Record> implements Tool {

  private final @NonNull String name;
  private final @NonNull Map<String, Object> parameters;
  private final @NonNull Boolean strict;
  private final @Nullable String description;
  private final @NonNull Class<P> paramClass;
  private final boolean requiresConfirmation;

  public FunctionTool() {
    this(new JacksonJsonSchemaProducer(new ObjectMapper()));
  }

  /**
   * Creates a FunctionTool using metadata from the @FunctionMetadata annotation if present,
   * otherwise derives the name from the class name converted to snake_case.
   */
  @SuppressWarnings("unchecked")
  public FunctionTool(@NonNull JsonSchemaProducer jsonSchemaProducer) {
    FunctionMetadata metadata = this.getClass().getAnnotation(FunctionMetadata.class);

    if (metadata != null) {
      this.name = metadata.name();
      this.description = metadata.description().isEmpty() ? null : metadata.description();
      this.requiresConfirmation = metadata.requiresConfirmation();
    } else {
      // Derive name from class simple name converted to snake_case
      this.name = toSnakeCase(this.getClass().getSimpleName());
      this.description = null;
      this.requiresConfirmation = false;
    }
    this.strict = true;

    // Extract the actual type parameter at runtime
    Type genericSuperclass = getClass().getGenericSuperclass();
    if (!(genericSuperclass instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "FunctionTool subclass "
              + this.getClass().getName()
              + " must specify a concrete type parameter");
    }

    Type typeArgument = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
    if (!(typeArgument instanceof Class)) {
      throw new IllegalStateException(
          "FunctionTool type parameter must be a concrete class, got: " + typeArgument);
    }

    Class<P> paramClass = (Class<P>) typeArgument;
    this.paramClass = paramClass;
    this.parameters = jsonSchemaProducer.produce(paramClass);
  }

  /**
   * Creates a FunctionTool with manually specified parameters schema. Uses @FunctionMetadata for
   * name/description if present, otherwise derives from class name.
   */
  public FunctionTool(@NonNull Map<String, Object> parameters, boolean strict) {
    FunctionMetadata metadata = this.getClass().getAnnotation(FunctionMetadata.class);

    if (metadata != null) {
      this.name = metadata.name();
      this.description = metadata.description().isEmpty() ? null : metadata.description();
      this.requiresConfirmation = metadata.requiresConfirmation();
    } else {
      this.name = toSnakeCase(this.getClass().getSimpleName());
      this.description = null;
      this.requiresConfirmation = false;
    }
    this.parameters = Map.copyOf(parameters);
    this.strict = strict;
    // For manually specified parameters, we need to extract the param class from generics
    this.paramClass = extractParamClass();
  }

  @SuppressWarnings("unchecked")
  private Class<P> extractParamClass() {
    Type genericSuperclass = getClass().getGenericSuperclass();
    if (!(genericSuperclass instanceof ParameterizedType parameterizedType)) {
      throw new IllegalStateException(
          "FunctionTool subclass "
              + this.getClass().getName()
              + " must specify a concrete type parameter");
    }
    Type typeArgument = parameterizedType.getActualTypeArguments()[0];
    if (!(typeArgument instanceof Class)) {
      throw new IllegalStateException(
          "FunctionTool type parameter must be a concrete class, got: " + typeArgument);
    }
    return (Class<P>) typeArgument;
  }

  /**
   * Converts a PascalCase or camelCase string to snake_case. Example: "GetWeatherTool" ->
   * "get_weather_tool"
   */
  private static @NonNull String toSnakeCase(@NonNull String input) {
    if (input.isEmpty()) {
      return input;
    }
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          result.append('_');
        }
        result.append(Character.toLowerCase(c));
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  public abstract @Nullable FunctionToolCallOutput call(@Nullable P params);

  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("name", name, "type", "function"));
  }

  public @NonNull String getType() {
    return "function";
  }

  public @NonNull String getName() {
    return name;
  }

  public @Nullable String getDescription() {
    return description;
  }

  public @NonNull Map<String, Object> getParameters() {
    return Map.copyOf(parameters);
  }

  public @NonNull Boolean getStrict() {
    return strict;
  }

  /**
   * Returns the parameter class for this function tool. Used by the registry to deserialize JSON
   * arguments.
   *
   * @return the parameter class
   */
  public @NonNull Class<P> getParamClass() {
    return paramClass;
  }

  /**
   * Returns whether this tool requires human confirmation before execution.
   *
   * <p>When true, the tool will trigger the {@code onToolCallPending} or {@code onPause} callback
   * in AgentStream, allowing human-in-the-loop approval workflows.
   *
   * @return true if confirmation is required, false otherwise
   */
  public boolean requiresConfirmation() {
    return requiresConfirmation;
  }
}
