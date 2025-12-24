package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link FunctionToolStore} functionality. */
class FunctionToolStoreTest {

  private FunctionToolStore store;
  private TestWeatherTool weatherTool;
  private TestCalculatorTool calculatorTool;

  @BeforeEach
  void setUp() {
    store = FunctionToolStore.create();
    weatherTool = new TestWeatherTool();
    calculatorTool = new TestCalculatorTool();
  }

  // ===== Test Record Types =====

  record WeatherParams(String location, String unit) {}

  record CalculatorParams(int a, int b, String operation) {}

  // ===== Test Function Tools =====

  static class TestWeatherTool extends FunctionTool<WeatherParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable WeatherParams params) {
      if (params == null) {
        return FunctionToolCallOutput.error("No parameters provided");
      }
      return FunctionToolCallOutput.success(
          String.format("Weather in %s: 25%s", params.location(), params.unit()));
    }

    @Override
    public @NonNull String getName() {
      return "get_weather";
    }
  }

  static class TestCalculatorTool extends FunctionTool<CalculatorParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable CalculatorParams params) {
      if (params == null) {
        return FunctionToolCallOutput.error("No parameters provided");
      }
      int result =
          switch (params.operation()) {
            case "add" -> params.a() + params.b();
            case "subtract" -> params.a() - params.b();
            case "multiply" -> params.a() * params.b();
            default -> 0;
          };
      return FunctionToolCallOutput.success(String.valueOf(result));
    }

    @Override
    public @NonNull String getName() {
      return "calculate";
    }
  }

  // ===== Tests =====

  @Nested
  @DisplayName("Store Creation")
  class StoreCreation {

    @Test
    @DisplayName("create() returns non-null store")
    void createReturnsNonNullStore() {
      assertNotNull(FunctionToolStore.create());
    }

    @Test
    @DisplayName("create(ObjectMapper) uses provided mapper")
    void createWithObjectMapperUsesProvidedMapper() {
      ObjectMapper customMapper = new ObjectMapper();
      FunctionToolStore customStore = FunctionToolStore.create(customMapper);
      assertSame(customMapper, customStore.getObjectMapper());
    }
  }

  @Nested
  @DisplayName("Adding Tools")
  class AddingTools {

    @Test
    @DisplayName("add() stores tool by name")
    void addStoresToolByName() {
      store.add(weatherTool);
      assertTrue(store.contains("get_weather"));
    }

    @Test
    @DisplayName("add() returns store for method chaining")
    void addReturnsStoreForChaining() {
      FunctionToolStore result = store.add(weatherTool);
      assertSame(store, result);
    }

    @Test
    @DisplayName("add() throws on duplicate name")
    void addThrowsOnDuplicateName() {
      store.add(weatherTool);
      TestWeatherTool duplicateTool = new TestWeatherTool();

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> store.add(duplicateTool));
      assertTrue(ex.getMessage().contains("get_weather"));
    }

    @Test
    @DisplayName("add() throws on null tool")
    void addThrowsOnNullTool() {
      assertThrows(NullPointerException.class, () -> store.add(null));
    }

    @Test
    @DisplayName("addAll(varargs) adds multiple tools")
    void addAllVarargsAddsMultipleTools() {
      store.addAll(weatherTool, calculatorTool);
      assertTrue(store.contains("get_weather"));
      assertTrue(store.contains("calculate"));
    }

    @Test
    @DisplayName("addAll(Iterable) adds multiple tools")
    void addAllIterableAddsMultipleTools() {
      store.addAll(List.of(weatherTool, calculatorTool));
      assertTrue(store.contains("get_weather"));
      assertTrue(store.contains("calculate"));
    }
  }

  @Nested
  @DisplayName("Binding Tool Calls")
  class BindingToolCalls {

    @Test
    @DisplayName("bind() creates callable BoundedFunctionCall")
    void bindCreatesCallableBoundedFunctionCall() {
      store.add(weatherTool);
      FunctionToolCall toolCall =
          new FunctionToolCall(
              "{\"location\":\"Tokyo\",\"unit\":\"C\"}",
              "call_123",
              "get_weather",
              "id_456",
              FunctionToolCallStatus.COMPLETED);

      BoundedFunctionCall bound = store.bind(toolCall);

      assertNotNull(bound);
      assertTrue(bound.isBound());
      assertEquals("get_weather", bound.name());
      assertEquals("call_123", bound.callId());
    }

    @Test
    @DisplayName("bind() throws for unregistered function")
    void bindThrowsForUnregisteredFunction() {
      FunctionToolCall toolCall =
          new FunctionToolCall("{}", "call_123", "unknown_function", null, null);

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> store.bind(toolCall));
      assertTrue(ex.getMessage().contains("unknown_function"));
    }

    @Test
    @DisplayName("bindAll() binds multiple tool calls")
    void bindAllBindsMultipleToolCalls() {
      store.addAll(weatherTool, calculatorTool);

      List<FunctionToolCall> toolCalls =
          List.of(
              new FunctionToolCall(
                  "{\"location\":\"Tokyo\",\"unit\":\"C\"}", "call_1", "get_weather", null, null),
              new FunctionToolCall(
                  "{\"a\":5,\"b\":3,\"operation\":\"add\"}", "call_2", "calculate", null, null));

      List<BoundedFunctionCall> bound = store.bindAll(toolCalls);

      assertEquals(2, bound.size());
      assertEquals("get_weather", bound.get(0).name());
      assertEquals("calculate", bound.get(1).name());
    }
  }

  @Nested
  @DisplayName("Executing Tool Calls")
  class ExecutingToolCalls {

    @Test
    @DisplayName("bound call() deserializes and invokes function")
    void boundCallDeserializesAndInvokesFunction() throws JsonProcessingException {
      store.add(weatherTool);
      FunctionToolCall toolCall =
          new FunctionToolCall(
              "{\"location\":\"Paris\",\"unit\":\"F\"}", "call_abc", "get_weather", null, null);

      BoundedFunctionCall bound = store.bind(toolCall);
      FunctionToolCallOutput result = bound.call();

      assertNotNull(result);
      assertTrue(result.output().toString().contains("Paris"));
    }

    @Test
    @DisplayName("execute() binds and calls in one step")
    void executeBindsAndCallsInOneStep() throws JsonProcessingException {
      store.add(calculatorTool);
      FunctionToolCall toolCall =
          new FunctionToolCall(
              "{\"a\":10,\"b\":5,\"operation\":\"subtract\"}",
              "call_math",
              "calculate",
              null,
              null);

      FunctionToolCallOutput result = store.execute(toolCall);

      assertNotNull(result);
      assertTrue(result.output().toString().contains("5"));
    }

    @Test
    @DisplayName("executeAll() executes multiple tool calls")
    void executeAllExecutesMultipleToolCalls() throws JsonProcessingException {
      store.addAll(weatherTool, calculatorTool);

      List<FunctionToolCall> toolCalls =
          List.of(
              new FunctionToolCall(
                  "{\"location\":\"London\",\"unit\":\"C\"}", "call_1", "get_weather", null, null),
              new FunctionToolCall(
                  "{\"a\":7,\"b\":3,\"operation\":\"multiply\"}",
                  "call_2",
                  "calculate",
                  null,
                  null));

      List<FunctionToolCallOutput> results = store.executeAll(toolCalls);

      assertEquals(2, results.size());
      assertTrue(results.get(0).output().toString().contains("London"));
      assertTrue(results.get(1).output().toString().contains("21"));
    }

    @Test
    @DisplayName("call() throws JsonProcessingException on invalid JSON")
    void callThrowsOnInvalidJson() {
      store.add(weatherTool);
      FunctionToolCall toolCall =
          new FunctionToolCall("not valid json", "call_bad", "get_weather", null, null);

      BoundedFunctionCall bound = store.bind(toolCall);
      assertThrows(JsonProcessingException.class, bound::call);
    }
  }

  @Nested
  @DisplayName("Unbound Function Calls")
  class UnboundFunctionCalls {

    @Test
    @DisplayName("unbound call throws IllegalStateException")
    void unboundCallThrowsIllegalStateException() {
      BoundedFunctionCall unbound =
          new BoundedFunctionCall(
              "{}",
              "call_id",
              "some_function",
              null,
              null,
              null, // no function
              null, // no paramClass
              null // no objectMapper
              );

      assertFalse(unbound.isBound());
      IllegalStateException ex = assertThrows(IllegalStateException.class, unbound::call);
      assertTrue(ex.getMessage().contains("FunctionToolStore.bind()"));
    }
  }

  @Nested
  @DisplayName("Contains Check")
  class ContainsCheck {

    @Test
    @DisplayName("contains() returns false for empty store")
    void containsReturnsFalseForEmptyStore() {
      assertFalse(store.contains("any_name"));
    }

    @Test
    @DisplayName("contains() returns true after adding tool")
    void containsReturnsTrueAfterAddingTool() {
      store.add(weatherTool);
      assertTrue(store.contains("get_weather"));
      assertFalse(store.contains("other_tool"));
    }
  }
}
