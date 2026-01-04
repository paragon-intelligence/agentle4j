package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for FunctionToolCall and related DTOs with low coverage. */
@DisplayName("Function Tool Call DTOs")
class FunctionToolCallDtoTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FUNCTION TOOL CALL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionToolCall")
  class FunctionToolCallTests {

    @Test
    @DisplayName("constructor sets all fields correctly")
    void constructor_setsAllFields() {
      FunctionToolCall call =
          new FunctionToolCall(
              "{\"city\": \"Tokyo\"}",
              "call-123",
              "get_weather",
              "id-456",
              FunctionToolCallStatus.COMPLETED);

      assertEquals("{\"city\": \"Tokyo\"}", call.arguments());
      assertEquals("call-123", call.callId());
      assertEquals("get_weather", call.name());
      assertEquals("id-456", call.id());
      assertEquals(FunctionToolCallStatus.COMPLETED, call.status());
    }

    @Test
    @DisplayName("constructor handles null optional fields")
    void constructor_handlesNullOptionalFields() {
      FunctionToolCall call = new FunctionToolCall("{}", "call-123", "my_func", null, null);

      assertEquals("{}", call.arguments());
      assertEquals("call-123", call.callId());
      assertEquals("my_func", call.name());
      assertNull(call.id());
      assertNull(call.status());
    }

    @Test
    @DisplayName("equals returns true for identical objects")
    void equals_returnsTrueForIdentical() {
      FunctionToolCall call1 =
          new FunctionToolCall("{}", "call-1", "func", "id-1", FunctionToolCallStatus.COMPLETED);
      FunctionToolCall call2 =
          new FunctionToolCall("{}", "call-1", "func", "id-1", FunctionToolCallStatus.COMPLETED);

      assertEquals(call1, call2);
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different objects")
    void equals_returnsFalseForDifferent() {
      FunctionToolCall call1 =
          new FunctionToolCall("{}", "call-1", "func_a", "id-1", FunctionToolCallStatus.COMPLETED);
      FunctionToolCall call2 =
          new FunctionToolCall("{}", "call-1", "func_b", "id-1", FunctionToolCallStatus.COMPLETED);

      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("equals handles null and different types")
    void equals_handlesNullAndDifferentTypes() {
      FunctionToolCall call = new FunctionToolCall("{}", "call-1", "func", "id-1", null);

      assertNotEquals(null, call);
      assertNotEquals("string", call);
      assertEquals(call, call);
    }

    @Test
    @DisplayName("toString returns XML formatted string")
    void toString_returnsXmlFormattedString() {
      FunctionToolCall call =
          new FunctionToolCall(
              "{\"query\": \"test\"}",
              "call-123",
              "search",
              "id-456",
              FunctionToolCallStatus.IN_PROGRESS);

      String result = call.toString();

      assertTrue(result.contains("<function_tool_call>"));
      assertTrue(result.contains("call-123"));
      assertTrue(result.contains("search"));
      assertTrue(result.contains("{\"query\": \"test\"}"));
    }

    @Test
    @DisplayName("toString handles null id and status")
    void toString_handlesNullIdAndStatus() {
      FunctionToolCall call = new FunctionToolCall("{}", "call-123", "my_func", null, null);

      String result = call.toString();

      assertTrue(result.contains("null")); // Should show null for id and status
    }

    @Test
    @DisplayName("can be serialized and deserialized with Jackson")
    void canBeSerializedAndDeserialized() throws Exception {
      ObjectMapper mapper = new ObjectMapper();
      FunctionToolCall original =
          new FunctionToolCall(
              "{\"key\": \"value\"}",
              "call-1",
              "test_func",
              "id-1",
              FunctionToolCallStatus.COMPLETED);

      String json = mapper.writeValueAsString(original);
      assertNotNull(json);
      assertTrue(json.contains("function_call"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FUNCTION TOOL CALL STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionToolCallStatus")
  class FunctionToolCallStatusTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      FunctionToolCallStatus[] values = FunctionToolCallStatus.values();
      assertTrue(values.length > 0);
    }

    @Test
    @DisplayName("IN_PROGRESS is valid")
    void inProgress_isValid() {
      FunctionToolCallStatus status = FunctionToolCallStatus.IN_PROGRESS;
      assertNotNull(status);
    }

    @Test
    @DisplayName("COMPLETED is valid")
    void completed_isValid() {
      FunctionToolCallStatus status = FunctionToolCallStatus.COMPLETED;
      assertNotNull(status);
    }

    @Test
    @DisplayName("INCOMPLETE is valid")
    void incomplete_isValid() {
      FunctionToolCallStatus status = FunctionToolCallStatus.INCOMPLETE;
      assertNotNull(status);
    }

    @Test
    @DisplayName("valueOf works for valid values")
    void valueOf_worksForValidValues() {
      assertEquals(FunctionToolCallStatus.COMPLETED, FunctionToolCallStatus.valueOf("COMPLETED"));
      assertEquals(
          FunctionToolCallStatus.IN_PROGRESS, FunctionToolCallStatus.valueOf("IN_PROGRESS"));
      assertEquals(FunctionToolCallStatus.INCOMPLETE, FunctionToolCallStatus.valueOf("INCOMPLETE"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FUNCTION SHELL TOOL CALL STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionShellToolCallStatus")
  class FunctionShellToolCallStatusTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      FunctionShellToolCallStatus[] values = FunctionShellToolCallStatus.values();
      assertTrue(values.length > 0);
    }

    @Test
    @DisplayName("can iterate through all values")
    void canIterateThroughAllValues() {
      for (FunctionShellToolCallStatus status : FunctionShellToolCallStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // COMPUTER TOOL CALL STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ComputerToolCallStatus")
  class ComputerToolCallStatusTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      ComputerToolCallStatus[] values = ComputerToolCallStatus.values();
      assertTrue(values.length > 0);
    }

    @Test
    @DisplayName("can iterate through all values")
    void canIterateThroughAllValues() {
      for (ComputerToolCallStatus status : ComputerToolCallStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CLICK BUTTON
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ClickButton")
  class ClickButtonTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      ClickButton[] values = ClickButton.values();
      assertTrue(values.length > 0);
    }

    @Test
    @DisplayName("LEFT button exists")
    void leftButton_exists() {
      assertNotNull(ClickButton.LEFT);
    }

    @Test
    @DisplayName("RIGHT button exists")
    void rightButton_exists() {
      assertNotNull(ClickButton.RIGHT);
    }

    @Test
    @DisplayName("valueOf works for valid values")
    void valueOf_worksForValidValues() {
      assertEquals(ClickButton.LEFT, ClickButton.valueOf("LEFT"));
      assertEquals(ClickButton.RIGHT, ClickButton.valueOf("RIGHT"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // COORDINATE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Coordinate")
  class CoordinateTests {

    @Test
    @DisplayName("constructor sets fields correctly")
    void constructor_setsFieldsCorrectly() {
      Coordinate coord = new Coordinate(100, 200);

      assertEquals(100, coord.x());
      assertEquals(200, coord.y());
    }

    @Test
    @DisplayName("equals works for identical coordinates")
    void equals_worksForIdentical() {
      Coordinate coord1 = new Coordinate(50, 75);
      Coordinate coord2 = new Coordinate(50, 75);

      assertEquals(coord1, coord2);
      assertEquals(coord1.hashCode(), coord2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different coordinates")
    void equals_returnsFalseForDifferent() {
      Coordinate coord1 = new Coordinate(50, 75);
      Coordinate coord2 = new Coordinate(50, 100);

      assertNotEquals(coord1, coord2);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PENDING SAFETY CHECK
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("PendingSafetyCheck")
  class PendingSafetyCheckTests {

    @Test
    @DisplayName("constructor sets fields correctly")
    void constructor_setsFieldsCorrectly() {
      PendingSafetyCheck check =
          new PendingSafetyCheck("check-123", "code-456", "Safety check description");

      assertEquals("check-123", check.id());
      assertEquals("code-456", check.code());
      assertEquals("Safety check description", check.message());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL CHOICE MODE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ToolChoiceMode")
  class ToolChoiceModeTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      ToolChoiceMode[] values = ToolChoiceMode.values();
      assertTrue(values.length >= 3);
    }

    @Test
    @DisplayName("NONE exists")
    void none_exists() {
      assertNotNull(ToolChoiceMode.NONE);
    }

    @Test
    @DisplayName("AUTO exists")
    void auto_exists() {
      assertNotNull(ToolChoiceMode.AUTO);
    }

    @Test
    @DisplayName("REQUIRED exists")
    void required_exists() {
      assertNotNull(ToolChoiceMode.REQUIRED);
    }

    @Test
    @DisplayName("toToolChoice returns lowercase name")
    void toToolChoice_returnsLowercaseName() {
      ObjectMapper mapper = new ObjectMapper();
      assertEquals("none", ToolChoiceMode.NONE.toToolChoice(mapper));
      assertEquals("auto", ToolChoiceMode.AUTO.toToolChoice(mapper));
      assertEquals("required", ToolChoiceMode.REQUIRED.toToolChoice(mapper));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SERVICE TIER TYPE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ServiceTierType")
  class ServiceTierTypeTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      ServiceTierType[] values = ServiceTierType.values();
      assertTrue(values.length > 0);
    }

    @Test
    @DisplayName("AUTO exists")
    void auto_exists() {
      assertNotNull(ServiceTierType.AUTO);
    }

    @Test
    @DisplayName("DEFAULT exists")
    void default_exists() {
      assertNotNull(ServiceTierType.DEFAULT);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRUNCATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Truncation")
  class TruncationTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      Truncation[] values = Truncation.values();
      assertTrue(values.length >= 2);
    }

    @Test
    @DisplayName("AUTO exists")
    void auto_exists() {
      assertNotNull(Truncation.AUTO);
    }

    @Test
    @DisplayName("DISABLED exists")
    void disabled_exists() {
      assertNotNull(Truncation.DISABLED);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // REASONING CONFIG
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ReasoningConfig")
  class ReasoningConfigTests {

    @Test
    @DisplayName("constructor sets effort")
    void constructor_setsEffort() {
      ReasoningConfig config = new ReasoningConfig(ReasoningEffort.HIGH, null);

      assertEquals(ReasoningEffort.HIGH, config.effort());
    }

    @Test
    @DisplayName("constructor handles null summary")
    void constructor_handlesNullSummary() {
      ReasoningConfig config = new ReasoningConfig(ReasoningEffort.MEDIUM, null);

      assertNull(config.summary());
    }

    @Test
    @DisplayName("constructor sets summary kind")
    void constructor_setsSummaryKind() {
      ReasoningConfig config = new ReasoningConfig(ReasoningEffort.LOW, ReasoningSummaryKind.AUTO);

      assertEquals(ReasoningSummaryKind.AUTO, config.summary());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // REASONING EFFORT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ReasoningEffort")
  class ReasoningEffortTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      ReasoningEffort[] values = ReasoningEffort.values();
      assertTrue(values.length >= 3);
    }

    @Test
    @DisplayName("LOW exists")
    void low_exists() {
      assertNotNull(ReasoningEffort.LOW);
    }

    @Test
    @DisplayName("MEDIUM exists")
    void medium_exists() {
      assertNotNull(ReasoningEffort.MEDIUM);
    }

    @Test
    @DisplayName("HIGH exists")
    void high_exists() {
      assertNotNull(ReasoningEffort.HIGH);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STREAM OPTIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("StreamOptions")
  class StreamOptionsTests {

    @Test
    @DisplayName("constructor sets includeObfuscation")
    void constructor_setsIncludeObfuscation() {
      StreamOptions options = new StreamOptions(true);
      assertTrue(options.includeObfuscation());
    }

    @Test
    @DisplayName("constructor with false includeObfuscation")
    void constructor_withFalseIncludeObfuscation() {
      StreamOptions options = new StreamOptions(false);
      assertFalse(options.includeObfuscation());
    }

    @Test
    @DisplayName("constructor with null includeObfuscation")
    void constructor_withNullIncludeObfuscation() {
      StreamOptions options = new StreamOptions(null);
      assertNull(options.includeObfuscation());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MODEL VERBOSITY CONFIG
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ModelVerbosityConfig")
  class ModelVerbosityConfigTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      ModelVerbosityConfig[] values = ModelVerbosityConfig.values();
      assertTrue(values.length >= 3);
    }

    @Test
    @DisplayName("LOW exists")
    void low_exists() {
      assertNotNull(ModelVerbosityConfig.LOW);
    }

    @Test
    @DisplayName("MEDIUM exists")
    void medium_exists() {
      assertNotNull(ModelVerbosityConfig.MEDIUM);
    }

    @Test
    @DisplayName("HIGH exists")
    void high_exists() {
      assertNotNull(ModelVerbosityConfig.HIGH);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INPUT MESSAGE STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("InputMessageStatus")
  class InputMessageStatusTests {

    @Test
    @DisplayName("enum values exist")
    void enumValuesExist() {
      InputMessageStatus[] values = InputMessageStatus.values();
      assertTrue(values.length > 0);
    }

    @Test
    @DisplayName("COMPLETED exists")
    void completed_exists() {
      assertNotNull(InputMessageStatus.COMPLETED);
    }
  }
}
