package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A concrete, deserializable representation of a function tool definition.
 *
 * <p>This class is used for deserializing function tool definitions from API responses. Unlike
 * {@link FunctionTool} which is abstract and meant to be extended by users with their own
 * implementations, this class is a simple data carrier that can be instantiated by Jackson during
 * deserialization.
 *
 * <p>According to the OpenAPI spec for FunctionTool:
 *
 * <ul>
 *   <li>{@code type} - string (enum), required. Always "function"
 *   <li>{@code name} - string, required. The name of the function to call
 *   <li>{@code description} - any, optional
 *   <li>{@code parameters} - any, required. JSON Schema for the function parameters
 *   <li>{@code strict} - any, required
 * </ul>
 */
public record FunctionToolDefinition(
    @JsonProperty("name") @NonNull String name,
    @JsonProperty("description") @Nullable String description,
    @JsonProperty("parameters") @NonNull Map<String, Object> parameters,
    @JsonProperty("strict") @NonNull Boolean strict)
    implements Tool {

  @JsonCreator
  public FunctionToolDefinition(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("description") @Nullable String description,
      @JsonProperty("parameters") @NonNull Map<String, Object> parameters,
      @JsonProperty("strict") @Nullable Boolean strict) {
    this.name = name;
    this.description = description;
    this.parameters = parameters;
    this.strict = strict != null ? strict : true;
  }

  @JsonProperty("type")
  public @NonNull String getType() {
    return "function";
  }

  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("name", name, "type", "function"));
  }
}
