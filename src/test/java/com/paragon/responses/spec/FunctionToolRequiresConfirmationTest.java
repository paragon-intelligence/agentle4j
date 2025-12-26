package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.annotations.FunctionMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for FunctionTool.requiresConfirmation() feature.
 */
@DisplayName("FunctionTool requiresConfirmation")
class FunctionToolRequiresConfirmationTest {

  // Test tools with different confirmation settings
  record SimpleParams(String value) {}

  @FunctionMetadata(name = "safe_tool", description = "A safe tool that auto-executes")
  static class SafeTool extends FunctionTool<SimpleParams> {
    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      return FunctionToolCallOutput.success("safe_tool_call", "Safe result");
    }
  }

  @FunctionMetadata(
      name = "dangerous_tool",
      description = "A dangerous tool that requires confirmation",
      requiresConfirmation = true)
  static class DangerousTool extends FunctionTool<SimpleParams> {
    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      return FunctionToolCallOutput.success("dangerous_tool_call", "Dangerous result");
    }
  }

  // Tool without annotation (should default to no confirmation)
  static class UnknownTool extends FunctionTool<SimpleParams> {
    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      return FunctionToolCallOutput.success("unknown_tool_call", "Unknown result");
    }
  }

  @Nested
  @DisplayName("requiresConfirmation()")
  class RequiresConfirmationTests {

    @Test
    @DisplayName("returns false for tool with default annotation")
    void defaultAnnotation_returnsFalse() {
      SafeTool tool = new SafeTool();
      assertFalse(tool.requiresConfirmation());
    }

    @Test
    @DisplayName("returns true for tool with requiresConfirmation=true")
    void explicitTrue_returnsTrue() {
      DangerousTool tool = new DangerousTool();
      assertTrue(tool.requiresConfirmation());
    }

    @Test
    @DisplayName("returns false for tool without annotation")
    void noAnnotation_returnsFalse() {
      UnknownTool tool = new UnknownTool();
      assertFalse(tool.requiresConfirmation());
    }
  }

  @Nested
  @DisplayName("FunctionToolStore.get()")
  class ToolStoreGetTests {

    @Test
    @DisplayName("returns tool by name")
    void get_returnsToolByName() {
      FunctionToolStore store = FunctionToolStore.create()
          .add(new SafeTool())
          .add(new DangerousTool());

      FunctionTool<?> safe = store.get("safe_tool");
      FunctionTool<?> dangerous = store.get("dangerous_tool");

      assertNotNull(safe);
      assertNotNull(dangerous);
      assertEquals("safe_tool", safe.getName());
      assertEquals("dangerous_tool", dangerous.getName());
    }

    @Test
    @DisplayName("returns null for unknown tool")
    void get_returnsNullForUnknownTool() {
      FunctionToolStore store = FunctionToolStore.create()
          .add(new SafeTool());

      assertNull(store.get("unknown"));
    }

    @Test
    @DisplayName("can check requiresConfirmation via store")
    void canCheckRequiresConfirmationViaStore() {
      FunctionToolStore store = FunctionToolStore.create()
          .add(new SafeTool())
          .add(new DangerousTool());

      FunctionTool<?> safe = store.get("safe_tool");
      FunctionTool<?> dangerous = store.get("dangerous_tool");

      assertFalse(safe.requiresConfirmation());
      assertTrue(dangerous.requiresConfirmation());
    }
  }

  @Nested
  @DisplayName("Alternative constructor")
  class AlternativeConstructorTests {

    // Tool using alternative constructor (manual params) but with annotation
    @FunctionMetadata(
        name = "manual_dangerous_tool",
        description = "A dangerous tool with manual params",
        requiresConfirmation = true)
    static class ManualDangerousTool extends FunctionTool<SimpleParams> {
      public ManualDangerousTool() {
        super(java.util.Map.of("type", "object"), true);
      }

      @Override
      public FunctionToolCallOutput call(SimpleParams params) {
        return FunctionToolCallOutput.success("manual_call", "Result");
      }
    }

    @Test
    @DisplayName("alternative constructor reads requiresConfirmation from annotation")
    void alternativeConstructor_readsAnnotation() {
      ManualDangerousTool tool = new ManualDangerousTool();
      assertTrue(tool.requiresConfirmation());
      assertEquals("manual_dangerous_tool", tool.getName());
    }
  }
}
