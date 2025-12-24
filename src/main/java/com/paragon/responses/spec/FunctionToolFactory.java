package com.paragon.responses.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A factory for creating {@link FunctionTool} instances with a shared {@link JsonSchemaProducer}.
 *
 * <p>This factory simplifies dependency injection by providing a centralized way to create function
 * tools with a consistent JSON schema producer configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create factory with custom ObjectMapper
 * var factory = FunctionToolFactory.withObjectMapper(objectMapper);
 *
 * // Create tools using the factory
 * var weatherTool = factory.create(GetWeatherTool.class);
 * var calculatorTool = factory.create(CalculatorTool.class);
 *
 * // Add to store
 * var store = FunctionToolStore.create(objectMapper)
 *     .addAll(weatherTool, calculatorTool);
 * }</pre>
 *
 * @see FunctionTool
 * @see FunctionToolStore
 */
public final class FunctionToolFactory {
  private final JsonSchemaProducer jsonSchemaProducer;

  private FunctionToolFactory(@NonNull JsonSchemaProducer jsonSchemaProducer) {
    this.jsonSchemaProducer =
        Objects.requireNonNull(jsonSchemaProducer, "jsonSchemaProducer cannot be null");
  }

  /**
   * Creates a new factory with a default {@link JacksonJsonSchemaProducer}.
   *
   * @return a new factory
   */
  public static @NonNull FunctionToolFactory create() {
    return new FunctionToolFactory(new JacksonJsonSchemaProducer(new ObjectMapper()));
  }

  /**
   * Creates a new factory with a {@link JacksonJsonSchemaProducer} using the provided ObjectMapper.
   *
   * @param objectMapper the ObjectMapper to use for JSON schema generation
   * @return a new factory
   */
  public static @NonNull FunctionToolFactory withObjectMapper(@NonNull ObjectMapper objectMapper) {
    return new FunctionToolFactory(new JacksonJsonSchemaProducer(objectMapper));
  }

  /**
   * Creates a new factory with the provided {@link JsonSchemaProducer}.
   *
   * @param jsonSchemaProducer the JSON schema producer to use
   * @return a new factory
   */
  public static @NonNull FunctionToolFactory withProducer(
      @NonNull JsonSchemaProducer jsonSchemaProducer) {
    return new FunctionToolFactory(jsonSchemaProducer);
  }

  /**
   * Creates a new instance of the specified {@link FunctionTool} class.
   *
   * <p>The tool class must have a constructor that accepts a {@link JsonSchemaProducer}.
   *
   * @param toolClass the class of the function tool to create
   * @param <T> the type of the function tool
   * @return a new instance of the function tool
   * @throws IllegalArgumentException if the tool cannot be instantiated
   */
  public <T extends FunctionTool<?>> @NonNull T create(@NonNull Class<T> toolClass) {
    Objects.requireNonNull(toolClass, "toolClass cannot be null");

    try {
      // Try constructor with JsonSchemaProducer first
      Constructor<T> constructor = toolClass.getDeclaredConstructor(JsonSchemaProducer.class);
      constructor.setAccessible(true);
      return constructor.newInstance(jsonSchemaProducer);
    } catch (NoSuchMethodException e) {
      // Fall back to default constructor if no JsonSchemaProducer constructor exists
      try {
        Constructor<T> defaultConstructor = toolClass.getDeclaredConstructor();
        defaultConstructor.setAccessible(true);
        return defaultConstructor.newInstance();
      } catch (NoSuchMethodException ex) {
        throw new IllegalArgumentException(
            "FunctionTool class '"
                + toolClass.getName()
                + "' must have either a constructor "
                + "accepting JsonSchemaProducer or a no-arg constructor.",
            ex);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
        throw new IllegalArgumentException(
            "Failed to instantiate FunctionTool: " + toolClass.getName(), ex);
      }
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalArgumentException(
          "Failed to instantiate FunctionTool: " + toolClass.getName(), e);
    }
  }

  /**
   * Returns the {@link JsonSchemaProducer} used by this factory.
   *
   * @return the JSON schema producer
   */
  public @NonNull JsonSchemaProducer getJsonSchemaProducer() {
    return jsonSchemaProducer;
  }
}
