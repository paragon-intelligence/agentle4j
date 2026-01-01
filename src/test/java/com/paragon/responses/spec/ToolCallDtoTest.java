package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for tool call DTO classes with 0 coverage.
 *
 * <p>Covers: ComputerToolCall, LocalShellCall, FunctionShellToolCall, CustomToolCall,
 * WebSearchToolCall, ImageGenerationCall
 */
@DisplayName("Tool Call DTOs")
class ToolCallDtoTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // COMPUTER TOOL CALL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ComputerToolCall")
  class ComputerToolCallTests {

    @Test
    @DisplayName("constructor sets all fields correctly")
    void constructor_setsAllFields() {
      Coordinate coord = new Coordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);
      List<PendingSafetyCheck> safetyChecks = List.of();

      ComputerToolCall call = new ComputerToolCall(
          action,
          "id-456",     // id comes before callId
          "call-123",   // callId comes after id
          safetyChecks,
          ComputerToolCallStatus.COMPLETED
      );

      assertEquals(action, call.action());
      assertEquals("call-123", call.callId());
      assertEquals("id-456", call.id());
      assertEquals(safetyChecks, call.pendingSafetyChecks());
      assertEquals(ComputerToolCallStatus.COMPLETED, call.status());
    }

    @Test
    @DisplayName("equals returns true for identical objects")
    void equals_returnsTrueForIdentical() {
      Coordinate coord = new Coordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);
      List<PendingSafetyCheck> safetyChecks = List.of();

      ComputerToolCall call1 = new ComputerToolCall(action, "id-456", "call-123", safetyChecks, ComputerToolCallStatus.COMPLETED);
      ComputerToolCall call2 = new ComputerToolCall(action, "id-456", "call-123", safetyChecks, ComputerToolCallStatus.COMPLETED);

      assertEquals(call1, call2);
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different objects")
    void equals_returnsFalseForDifferent() {
      Coordinate coord = new Coordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);
      List<PendingSafetyCheck> safetyChecks = List.of();

      ComputerToolCall call1 = new ComputerToolCall(action, "id-456", "call-123", safetyChecks, ComputerToolCallStatus.COMPLETED);
      ComputerToolCall call2 = new ComputerToolCall(action, "id-456", "call-999", safetyChecks, ComputerToolCallStatus.COMPLETED);

      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("equals handles null and different types")
    void equals_handlesNullAndDifferentTypes() {
      Coordinate coord = new Coordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);
      ComputerToolCall call = new ComputerToolCall(action, "id-456", "call-123", List.of(), ComputerToolCallStatus.COMPLETED);

      assertNotEquals(null, call);
      assertNotEquals("string", call);
      assertEquals(call, call);  // Same instance
    }

    @Test
    @DisplayName("toString returns formatted string")
    void toString_returnsFormattedString() {
      Coordinate coord = new Coordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);
      ComputerToolCall call = new ComputerToolCall(action, "id-456", "call-123", List.of(), ComputerToolCallStatus.COMPLETED);

      String result = call.toString();

      assertTrue(result.contains("call-123"));
      assertTrue(result.contains("id-456"));
      assertTrue(result.contains("COMPLETED"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LOCAL SHELL CALL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("LocalShellCall")
  class LocalShellCallTests {

    @Test
    @DisplayName("constructor sets all fields correctly")
    void constructor_setsAllFields() {
      LocalShellExecAction action = new LocalShellExecAction(List.of("ls", "-la"), null, null, null, null);
      LocalShellCall call = new LocalShellCall(action, "call-123", "id-456", "completed");

      assertEquals(action, call.action());
      assertEquals("call-123", call.callId());
      assertEquals("id-456", call.id());
      assertEquals("completed", call.status());
    }

    @Test
    @DisplayName("equals returns true for identical objects")
    void equals_returnsTrueForIdentical() {
      LocalShellExecAction action = new LocalShellExecAction(List.of("echo", "hello"), null, null, null, null);
      LocalShellCall call1 = new LocalShellCall(action, "call-1", "id-1", "completed");
      LocalShellCall call2 = new LocalShellCall(action, "call-1", "id-1", "completed");

      assertEquals(call1, call2);
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different objects")
    void equals_returnsFalseForDifferent() {
      LocalShellExecAction action = new LocalShellExecAction(List.of("pwd"), null, null, null, null);
      LocalShellCall call1 = new LocalShellCall(action, "call-1", "id-1", "completed");
      LocalShellCall call2 = new LocalShellCall(action, "call-2", "id-1", "completed");

      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("equals handles null and different types")
    void equals_handlesNullAndDifferentTypes() {
      LocalShellExecAction action = new LocalShellExecAction(List.of("ls"), null, null, null, null);
      LocalShellCall call = new LocalShellCall(action, "call-1", "id-1", "completed");

      assertNotEquals(null, call);
      assertNotEquals("string", call);
      assertEquals(call, call);
    }

    @Test
    @DisplayName("toString returns formatted string")
    void toString_returnsFormattedString() {
      LocalShellExecAction action = new LocalShellExecAction(List.of("ls"), null, null, null, null);
      LocalShellCall call = new LocalShellCall(action, "call-123", "id-456", "completed");

      String result = call.toString();

      assertTrue(result.contains("LocalShellCall"));
      assertTrue(result.contains("call-123"));
      assertTrue(result.contains("id-456"));
      assertTrue(result.contains("completed"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FUNCTION SHELL TOOL CALL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionShellToolCall")
  class FunctionShellToolCallTests {

    @Test
    @DisplayName("constructor sets all fields correctly")
    void constructor_setsAllFields() {
      FunctionShellAction action = new FunctionShellAction(List.of("echo hello"), 1000, 5000);
      FunctionShellToolCall call = new FunctionShellToolCall(action, "call-123", "id-456", FunctionShellToolCallStatus.COMPLETED);

      assertEquals(action, call.action());
      assertEquals("call-123", call.callId());
      assertEquals("id-456", call.id());
      assertEquals(FunctionShellToolCallStatus.COMPLETED, call.status());
    }

    @Test
    @DisplayName("constructor handles null optional fields")
    void constructor_handlesNullOptionalFields() {
      FunctionShellAction action = new FunctionShellAction(List.of("pwd"), null, null);
      FunctionShellToolCall call = new FunctionShellToolCall(action, "call-123", null, null);

      assertEquals(action, call.action());
      assertEquals("call-123", call.callId());
      assertNull(call.id());
      assertNull(call.status());
    }

    @Test
    @DisplayName("equals returns true for identical objects")
    void equals_returnsTrueForIdentical() {
      FunctionShellAction action = new FunctionShellAction(List.of("ls"), 500, 3000);
      FunctionShellToolCall call1 = new FunctionShellToolCall(action, "call-1", "id-1", FunctionShellToolCallStatus.IN_PROGRESS);
      FunctionShellToolCall call2 = new FunctionShellToolCall(action, "call-1", "id-1", FunctionShellToolCallStatus.IN_PROGRESS);

      assertEquals(call1, call2);
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different objects")
    void equals_returnsFalseForDifferent() {
      FunctionShellAction action = new FunctionShellAction(List.of("ls"), 500, 3000);
      FunctionShellToolCall call1 = new FunctionShellToolCall(action, "call-1", "id-1", FunctionShellToolCallStatus.COMPLETED);
      FunctionShellToolCall call2 = new FunctionShellToolCall(action, "call-1", "id-1", FunctionShellToolCallStatus.INCOMPLETE);

      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("equals handles null and different types")
    void equals_handlesNullAndDifferentTypes() {
      FunctionShellAction action = new FunctionShellAction(List.of("ls"), 500, 3000);
      FunctionShellToolCall call = new FunctionShellToolCall(action, "call-1", "id-1", FunctionShellToolCallStatus.COMPLETED);

      assertNotEquals(null, call);
      assertNotEquals("string", call);
      assertEquals(call, call);
    }

    @Test
    @DisplayName("toString returns formatted string")
    void toString_returnsFormattedString() {
      FunctionShellAction action = new FunctionShellAction(List.of("echo test"), 1000, 5000);
      FunctionShellToolCall call = new FunctionShellToolCall(action, "call-123", "id-456", FunctionShellToolCallStatus.COMPLETED);

      String result = call.toString();

      assertTrue(result.contains("FunctionShellToolCall"));
      assertTrue(result.contains("call-123"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CUSTOM TOOL CALL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("CustomToolCall")
  class CustomToolCallTests {

    @Test
    @DisplayName("constructor sets all fields correctly")
    void constructor_setsAllFields() {
      CustomToolCall call = new CustomToolCall("call-123", "{\"query\": \"test\"}", "my_tool", "id-456");

      assertEquals("call-123", call.callId());
      assertEquals("{\"query\": \"test\"}", call.input());
      assertEquals("my_tool", call.name());
      assertEquals("id-456", call.id());
    }

    @Test
    @DisplayName("constructor handles null id")
    void constructor_handlesNullId() {
      CustomToolCall call = new CustomToolCall("call-123", "input", "tool_name", null);

      assertEquals("call-123", call.callId());
      assertEquals("input", call.input());
      assertEquals("tool_name", call.name());
      assertNull(call.id());
    }

    @Test
    @DisplayName("equals returns true for identical objects")
    void equals_returnsTrueForIdentical() {
      CustomToolCall call1 = new CustomToolCall("call-1", "input", "tool", "id-1");
      CustomToolCall call2 = new CustomToolCall("call-1", "input", "tool", "id-1");

      assertEquals(call1, call2);
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different objects")
    void equals_returnsFalseForDifferent() {
      CustomToolCall call1 = new CustomToolCall("call-1", "input1", "tool", "id-1");
      CustomToolCall call2 = new CustomToolCall("call-1", "input2", "tool", "id-1");

      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("equals handles null and different types")
    void equals_handlesNullAndDifferentTypes() {
      CustomToolCall call = new CustomToolCall("call-1", "input", "tool", "id-1");

      assertNotEquals(null, call);
      assertNotEquals("string", call);
      assertEquals(call, call);
    }

    @Test
    @DisplayName("toString returns formatted string")
    void toString_returnsFormattedString() {
      CustomToolCall call = new CustomToolCall("call-123", "input-data", "my_tool", "id-456");

      String result = call.toString();

      assertTrue(result.contains("CustomToolCall"));
      assertTrue(result.contains("call-123"));
      assertTrue(result.contains("input-data"));
      assertTrue(result.contains("my_tool"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // WEB SEARCH TOOL CALL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("WebSearchToolCall")
  class WebSearchToolCallTests {

    @Test
    @DisplayName("constructor sets all fields correctly")
    void constructor_setsAllFields() {
      SearchAction action = new SearchAction("java programming", null);
      WebSearchToolCall call = new WebSearchToolCall(action, "id-123", "completed");

      assertEquals(action, call.action());
      assertEquals("id-123", call.id());
      assertEquals("completed", call.status());
    }

    @Test
    @DisplayName("equals returns true for identical objects")
    void equals_returnsTrueForIdentical() {
      SearchAction action = new SearchAction("test query", null);
      WebSearchToolCall call1 = new WebSearchToolCall(action, "id-1", "completed");
      WebSearchToolCall call2 = new WebSearchToolCall(action, "id-1", "completed");

      assertEquals(call1, call2);
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different objects")
    void equals_returnsFalseForDifferent() {
      SearchAction action = new SearchAction("test query", null);
      WebSearchToolCall call1 = new WebSearchToolCall(action, "id-1", "completed");
      WebSearchToolCall call2 = new WebSearchToolCall(action, "id-1", "in_progress");

      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("equals handles null and different types")
    void equals_handlesNullAndDifferentTypes() {
      SearchAction action = new SearchAction("test", null);
      WebSearchToolCall call = new WebSearchToolCall(action, "id-1", "completed");

      assertNotEquals(null, call);
      assertNotEquals("string", call);
      assertEquals(call, call);
    }

    @Test
    @DisplayName("toString returns XML formatted string")
    void toString_returnsXmlFormattedString() {
      SearchAction action = new SearchAction("java programming", null);
      WebSearchToolCall call = new WebSearchToolCall(action, "id-123", "completed");

      String result = call.toString();

      assertTrue(result.contains("<web_search_tool_call>"));
      assertTrue(result.contains("id-123"));
      assertTrue(result.contains("completed"));
    }

    @Test
    @DisplayName("works with OpenPageAction")
    void worksWithOpenPageAction() {
      OpenPageAction action = new OpenPageAction("https://example.com");
      WebSearchToolCall call = new WebSearchToolCall(action, "id-456", "in_progress");

      assertEquals(action, call.action());
      assertEquals("id-456", call.id());
    }

    @Test
    @DisplayName("works with FindAction")
    void worksWithFindAction() {
      FindAction action = new FindAction("search pattern", "https://example.com");
      WebSearchToolCall call = new WebSearchToolCall(action, "id-789", "completed");

      assertEquals(action, call.action());
      assertEquals("id-789", call.id());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // IMAGE GENERATION CALL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ImageGenerationCall")
  class ImageGenerationCallTests {

    @Test
    @DisplayName("constructor sets all fields correctly")
    void constructor_setsAllFields() {
      ImageGenerationCall call = new ImageGenerationCall("id-123", "base64encodedimage", "completed");

      assertEquals("id-123", call.id());
      assertEquals("base64encodedimage", call.result());
      assertEquals("completed", call.status());
    }

    @Test
    @DisplayName("equals returns true for identical objects")
    void equals_returnsTrueForIdentical() {
      ImageGenerationCall call1 = new ImageGenerationCall("id-1", "result1", "completed");
      ImageGenerationCall call2 = new ImageGenerationCall("id-1", "result1", "completed");

      assertEquals(call1, call2);
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different objects")
    void equals_returnsFalseForDifferent() {
      ImageGenerationCall call1 = new ImageGenerationCall("id-1", "result1", "completed");
      ImageGenerationCall call2 = new ImageGenerationCall("id-1", "result2", "completed");

      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("equals handles null and different types")
    void equals_handlesNullAndDifferentTypes() {
      ImageGenerationCall call = new ImageGenerationCall("id-1", "result", "completed");

      assertNotEquals(null, call);
      assertNotEquals("string", call);
      assertEquals(call, call);
    }

    @Test
    @DisplayName("toString returns formatted string")
    void toString_returnsFormattedString() {
      ImageGenerationCall call = new ImageGenerationCall("id-123", "base64data", "in_progress");

      String result = call.toString();

      assertTrue(result.contains("ImageGenerationCall"));
      assertTrue(result.contains("id-123"));
      assertTrue(result.contains("base64data"));
      assertTrue(result.contains("in_progress"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // COMPUTER USE ACTION IMPLEMENTATIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ComputerUseAction Implementations")
  class ComputerUseActionTests {

    @Test
    @DisplayName("ClickAction toString returns XML format")
    void clickAction_toStringReturnsXmlFormat() {
      Coordinate coord = new Coordinate(150, 250);
      ClickAction action = new ClickAction(ClickButton.RIGHT, coord);

      String result = action.toString();

      assertTrue(result.contains("<computer_use_action:click>"));
      assertTrue(result.contains("RIGHT"));
    }

    @Test
    @DisplayName("ClickAction accessors work correctly")
    void clickAction_accessorsWork() {
      Coordinate coord = new Coordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);

      assertEquals(ClickButton.LEFT, action.button());
      assertEquals(coord, action.coordinate());
    }

    @Test
    @DisplayName("ScreenshotAction can be instantiated")
    void screenshotAction_canBeInstantiated() {
      ScreenshotAction action = new ScreenshotAction();
      assertNotNull(action);
    }

    @Test
    @DisplayName("ScrollAction can be instantiated")
    void scrollAction_canBeInstantiated() {
      Coordinate coord = new Coordinate(100, 100);
      ScrollAction action = new ScrollAction(10, 20, coord);

      assertEquals(coord, action.coordinate());
      assertEquals(10, action.scrollX());
      assertEquals(20, action.scrollY());
    }

    @Test
    @DisplayName("TypeAction can be instantiated")
    void typeAction_canBeInstantiated() {
      TypeAction action = new TypeAction("Hello World");

      assertEquals("Hello World", action.text());
    }

    @Test
    @DisplayName("WaitAction can be instantiated")
    void waitAction_canBeInstantiated() {
      WaitAction action = new WaitAction();
      assertNotNull(action);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // WEB ACTION IMPLEMENTATIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("WebAction Implementations")
  class WebActionTests {

    @Test
    @DisplayName("SearchAction can be instantiated")
    void searchAction_canBeInstantiated() {
      SearchAction action = new SearchAction("test query", null);
      assertEquals("test query", action.query());
    }

    @Test
    @DisplayName("OpenPageAction can be instantiated")
    void openPageAction_canBeInstantiated() {
      OpenPageAction action = new OpenPageAction("https://example.com");
      assertEquals("https://example.com", action.url());
    }

    @Test
    @DisplayName("FindAction can be instantiated")
    void findAction_canBeInstantiated() {
      FindAction action = new FindAction("pattern to find", "https://example.com");
      assertEquals("pattern to find", action.pattern());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SHELL ACTION IMPLEMENTATIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Shell Action Implementations")
  class ShellActionTests {

    @Test
    @DisplayName("FunctionShellAction record accessors work")
    void functionShellAction_accessorsWork() {
      FunctionShellAction action = new FunctionShellAction(
          List.of("echo hello", "ls -la"),
          1000,
          5000
      );

      assertEquals(List.of("echo hello", "ls -la"), action.commands());
      assertEquals(1000, action.maxOutputLength());
      assertEquals(5000, action.timeoutMs());
    }

    @Test
    @DisplayName("FunctionShellAction handles null optional fields")
    void functionShellAction_handlesNullOptionalFields() {
      FunctionShellAction action = new FunctionShellAction(List.of("pwd"), null, null);

      assertEquals(List.of("pwd"), action.commands());
      assertNull(action.maxOutputLength());
      assertNull(action.timeoutMs());
    }

    @Test
    @DisplayName("LocalShellExecAction can be instantiated")
    void localShellExecAction_canBeInstantiated() {
      LocalShellExecAction action = new LocalShellExecAction(List.of("ls", "-la", "/tmp"), null, null, null, null);
      assertEquals(List.of("ls", "-la", "/tmp"), action.command());
    }
  }
}
