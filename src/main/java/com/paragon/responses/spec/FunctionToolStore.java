package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A store that maps function names to their implementations.
 *
 * <p>This store enables calling function tools directly from API responses by binding the function
 * name and JSON arguments from the response to the actual function implementation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create and add tools
 * var getWeatherTool = new GetWeatherTool();
 * var store = FunctionToolStore.create()
 *     .add(getWeatherTool);
 *
 * // Make API call
 * var response = responder.respond(payload).join();
 *
 * // Get callable function tool calls
 * var functionToolCalls = response.functionToolCalls(store);
 * FunctionToolCallOutput result = functionToolCalls.getFirst().call();
 * }</pre>
 */
public final class FunctionToolStore {
  private final Map<String, FunctionTool<?>> tools;
  private final ObjectMapper objectMapper;

  private FunctionToolStore(@NonNull ObjectMapper objectMapper) {
    this.tools = new HashMap<>();
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
  }

  /**
   * Creates a new FunctionToolStore with a default ObjectMapper.
   *
   * @return a new empty store
   */
  public static @NonNull FunctionToolStore create() {
    return new FunctionToolStore(new ObjectMapper());
  }

  /**
   * Creates a new FunctionToolStore with the specified ObjectMapper.
   *
   * @param objectMapper the ObjectMapper to use for deserializing function arguments
   * @return a new empty store
   */
  public static @NonNull FunctionToolStore create(@NonNull ObjectMapper objectMapper) {
    return new FunctionToolStore(objectMapper);
  }

  /**
   * Adds a function tool to this store. The tool will be stored under its name as returned by
   * {@link FunctionTool#getName()}.
   *
   * @param tool the function tool to add
   * @return this store for method chaining
   * @throws IllegalArgumentException if a tool with the same name is already stored
   */
  public @NonNull FunctionToolStore add(@NonNull FunctionTool<?> tool) {
    Objects.requireNonNull(tool, "tool cannot be null");
    String name = tool.getName();
    if (tools.containsKey(name)) {
      throw new IllegalArgumentException(
          "A tool with name '"
              + name
              + "' is already stored. "
              + "Each function tool must have a unique name.");
    }
    tools.put(name, tool);
    return this;
  }

  /**
   * Adds multiple function tools to this store.
   *
   * @param tools the function tools to add
   * @return this store for method chaining
   * @throws IllegalArgumentException if any tool has a duplicate name
   */
  public @NonNull FunctionToolStore addAll(@NonNull FunctionTool<?>... tools) {
    for (FunctionTool<?> tool : tools) {
      add(tool);
    }
    return this;
  }

  /**
   * Adds multiple function tools to this store.
   *
   * @param tools the function tools to add
   * @return this store for method chaining
   * @throws IllegalArgumentException if any tool has a duplicate name
   */
  public @NonNull FunctionToolStore addAll(@NonNull Iterable<? extends FunctionTool<?>> tools) {
    for (FunctionTool<?> tool : tools) {
      add(tool);
    }
    return this;
  }

  /**
   * Binds a function tool call from the API response to its implementation.
   *
   * <p>This creates a {@link BoundedFunctionCall} that can be invoked via {@link
   * BoundedFunctionCall#call()}.
   *
   * @param toolCall the function tool call from the API response
   * @return a bounded function call that can be invoked
   * @throws IllegalArgumentException if no tool is stored for the function name
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public @NonNull BoundedFunctionCall bind(@NonNull FunctionToolCall toolCall) {
    Objects.requireNonNull(toolCall, "toolCall cannot be null");
    String name = toolCall.name();
    FunctionTool<?> tool = tools.get(name);
    if (tool == null) {
      throw new IllegalArgumentException(
          "No tool stored for function name: '" + name + "'. " + "Stored tools: " + tools.keySet());
    }
    return new BoundedFunctionCall(
        toolCall.arguments(),
        toolCall.callId(),
        toolCall.name(),
        toolCall.id(),
        toolCall.status(),
        (FunctionTool<Record>) tool,
        (Class<? extends Record>) tool.getParamClass(),
        objectMapper);
  }

  /**
   * Binds all function tool calls from the API response to their implementations.
   *
   * @param toolCalls the function tool calls from the API response
   * @return a list of bounded function calls that can be invoked
   * @throws IllegalArgumentException if any tool call references a function not in the store
   */
  public @NonNull List<BoundedFunctionCall> bindAll(@NonNull List<FunctionToolCall> toolCalls) {
    return toolCalls.stream().map(this::bind).toList();
  }

  /**
   * Checks if a tool with the given name is stored.
   *
   * @param name the function name to check
   * @return true if a tool with this name is stored
   */
  public boolean contains(@NonNull String name) {
    return tools.containsKey(name);
  }

  /**
   * Gets a tool by name.
   *
   * @param name the function name
   * @return the tool, or null if not found
   */
  public FunctionTool<?> get(@NonNull String name) {
    return tools.get(name);
  }

  /**
   * Returns the ObjectMapper used by this store.
   *
   * @return the ObjectMapper
   */
  public @NonNull ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * Executes a function tool call and returns the result.
   *
   * <p>This is a convenience method that binds and calls in one step.
   *
   * @param toolCall the function tool call to execute
   * @return the function tool call output
   * @throws JsonProcessingException if the arguments cannot be deserialized
   * @throws IllegalArgumentException if no tool is stored for the function name
   */
  public @NonNull FunctionToolCallOutput execute(@NonNull FunctionToolCall toolCall)
      throws JsonProcessingException {
    return bind(toolCall).call();
  }

  /**
   * Executes all function tool calls and returns their results.
   *
   * @param toolCalls the function tool calls to execute
   * @return a list of function tool call outputs
   * @throws JsonProcessingException if any arguments cannot be deserialized
   * @throws IllegalArgumentException if any tool call references a function not in the store
   */
  public @NonNull List<FunctionToolCallOutput> executeAll(@NonNull List<FunctionToolCall> toolCalls)
      throws JsonProcessingException {
    var results = new java.util.ArrayList<FunctionToolCallOutput>();
    for (FunctionToolCall toolCall : toolCalls) {
      results.add(execute(toolCall));
    }
    return results;
  }
}
