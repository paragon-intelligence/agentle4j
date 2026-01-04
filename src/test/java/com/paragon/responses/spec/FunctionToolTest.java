package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.annotations.FunctionMetadata;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for FunctionTool.
 *
 * <p>Tests cover: - Construction with and without @FunctionMetadata - Name derivation from class
 * name (snake_case conversion) - Manual parameter specification - Getters for all properties -
 * toToolChoice() serialization - requiresConfirmation() behavior
 */
@DisplayName("FunctionTool Tests")
class FunctionToolTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // ═══════════════════════════════════════════════════════════════════════════
  // TEST TOOLS
  // ═══════════════════════════════════════════════════════════════════════════

  // Tool with annotation
  @FunctionMetadata(name = "get_weather", description = "Gets the weather")
  static class GetWeatherTool extends FunctionTool<WeatherParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable WeatherParams params) {
      return FunctionToolCallOutput.success("Sunny");
    }
  }

  // Tool with annotation including requiresConfirmation
  @FunctionMetadata(
      name = "risky_action",
      description = "A risky action",
      requiresConfirmation = true)
  static class RiskyActionTool extends FunctionTool<EmptyParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EmptyParams params) {
      return FunctionToolCallOutput.success("Done");
    }
  }

  // Tool without annotation - should derive name from class name
  static class TestFunctionTool extends FunctionTool<EmptyParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EmptyParams params) {
      return FunctionToolCallOutput.success("test");
    }
  }

  // Tool with no metadata and PascalCase name
  static class GetUserProfileTool extends FunctionTool<EmptyParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EmptyParams params) {
      return FunctionToolCallOutput.success("profile");
    }
  }

  // Parameter records
  public record WeatherParams(String location, String unit) {}

  public record EmptyParams() {}

  // ═══════════════════════════════════════════════════════════════════════════
  // CONSTRUCTOR TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("creates tool with @FunctionMetadata annotation")
    void createsToolWithAnnotation() {
      GetWeatherTool tool = new GetWeatherTool();

      assertEquals("get_weather", tool.getName());
      assertEquals("Gets the weather", tool.getDescription());
      assertFalse(tool.requiresConfirmation());
    }

    @Test
    @DisplayName("creates tool without annotation using snake_case name")
    void createsToolWithoutAnnotation() {
      TestFunctionTool tool = new TestFunctionTool();

      // TestFunctionTool -> test_function_tool
      assertEquals("test_function_tool", tool.getName());
      assertNull(tool.getDescription());
      assertFalse(tool.requiresConfirmation());
    }

    @Test
    @DisplayName("converts PascalCase to snake_case correctly")
    void convertsPascalCaseToSnakeCase() {
      GetUserProfileTool tool = new GetUserProfileTool();

      // GetUserProfileTool -> get_user_profile_tool
      assertEquals("get_user_profile_tool", tool.getName());
    }

    @Test
    @DisplayName("creates tool with requiresConfirmation=true")
    void createsToolWithRequiresConfirmation() {
      RiskyActionTool tool = new RiskyActionTool();

      assertTrue(tool.requiresConfirmation());
      assertEquals("risky_action", tool.getName());
      assertEquals("A risky action", tool.getDescription());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GETTERS TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Getters")
  class GetterTests {

    @Test
    @DisplayName("getType returns 'function'")
    void getTypeReturnsFunction() {
      GetWeatherTool tool = new GetWeatherTool();
      assertEquals("function", tool.getType());
    }

    @Test
    @DisplayName("getName returns configured name")
    void getNameReturnsConfiguredName() {
      GetWeatherTool tool = new GetWeatherTool();
      assertEquals("get_weather", tool.getName());
    }

    @Test
    @DisplayName("getDescription returns configured description")
    void getDescriptionReturnsConfiguredDescription() {
      GetWeatherTool tool = new GetWeatherTool();
      assertEquals("Gets the weather", tool.getDescription());
    }

    @Test
    @DisplayName("getDescription returns null when not configured")
    void getDescriptionReturnsNullWhenNotConfigured() {
      TestFunctionTool tool = new TestFunctionTool();
      assertNull(tool.getDescription());
    }

    @Test
    @DisplayName("getParameters returns JSON schema")
    void getParametersReturnsJsonSchema() {
      GetWeatherTool tool = new GetWeatherTool();
      Map<String, Object> params = tool.getParameters();

      assertNotNull(params);
      assertTrue(params.containsKey("type") || params.containsKey("properties"));
    }

    @Test
    @DisplayName("getStrict returns true by default")
    void getStrictReturnsTrue() {
      GetWeatherTool tool = new GetWeatherTool();
      assertTrue(tool.getStrict());
    }

    @Test
    @DisplayName("getParamClass returns parameter class")
    void getParamClassReturnsParameterClass() {
      GetWeatherTool tool = new GetWeatherTool();
      assertEquals(WeatherParams.class, tool.getParamClass());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CALL TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Call method")
  class CallTests {

    @Test
    @DisplayName("call executes and returns result")
    void callExecutesAndReturnsResult() {
      GetWeatherTool tool = new GetWeatherTool();
      WeatherParams params = new WeatherParams("New York", "celsius");

      FunctionToolCallOutput result = tool.call(params);

      assertNotNull(result);
      assertEquals("Sunny", result.output().toString());
    }

    @Test
    @DisplayName("call handles null parameters")
    void callHandlesNullParameters() {
      TestFunctionTool tool = new TestFunctionTool();

      FunctionToolCallOutput result = tool.call(null);

      assertNotNull(result);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SERIALIZATION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Serialization")
  class SerializationTests {

    @Test
    @DisplayName("toToolChoice generates correct JSON")
    void toToolChoiceGeneratesCorrectJson() throws JsonProcessingException {
      GetWeatherTool tool = new GetWeatherTool();
      String choice = tool.toToolChoice(objectMapper);

      assertNotNull(choice);
      assertTrue(choice.contains("get_weather"));
      assertTrue(choice.contains("function"));
    }

    @Test
    @DisplayName("toToolChoice contains name and type")
    void toToolChoiceContainsNameAndType() throws JsonProcessingException {
      TestFunctionTool tool = new TestFunctionTool();
      String choice = tool.toToolChoice(objectMapper);

      Map<?, ?> parsed = objectMapper.readValue(choice, Map.class);
      assertEquals("test_function_tool", parsed.get("name"));
      assertEquals("function", parsed.get("type"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // REQUIRES CONFIRMATION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Requires Confirmation")
  class RequiresConfirmationTests {

    @Test
    @DisplayName("requiresConfirmation defaults to false")
    void requiresConfirmationDefaultsToFalse() {
      GetWeatherTool tool = new GetWeatherTool();
      assertFalse(tool.requiresConfirmation());
    }

    @Test
    @DisplayName("requiresConfirmation returns true when set in annotation")
    void requiresConfirmationReturnsTrueWhenSet() {
      RiskyActionTool tool = new RiskyActionTool();
      assertTrue(tool.requiresConfirmation());
    }

    @Test
    @DisplayName("requiresConfirmation returns false without annotation")
    void requiresConfirmationReturnsFalseWithoutAnnotation() {
      TestFunctionTool tool = new TestFunctionTool();
      assertFalse(tool.requiresConfirmation());
    }
  }
}
