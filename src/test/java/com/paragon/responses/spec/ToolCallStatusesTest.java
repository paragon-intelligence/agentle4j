package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for tool call status enums.
 */
@DisplayName("Tool Call Statuses Tests")
class ToolCallStatusesTest {

  // =========================================================================
  // FunctionToolCallStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("FunctionToolCallStatus")
  class FunctionToolCallStatusTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(FunctionToolCallStatus.IN_PROGRESS);
      assertNotNull(FunctionToolCallStatus.COMPLETED);
      assertNotNull(FunctionToolCallStatus.INCOMPLETE);
    }

    @Test
    @DisplayName("should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(FunctionToolCallStatus.IN_PROGRESS, FunctionToolCallStatus.valueOf("IN_PROGRESS"));
      assertEquals(FunctionToolCallStatus.COMPLETED, FunctionToolCallStatus.valueOf("COMPLETED"));
      assertEquals(FunctionToolCallStatus.INCOMPLETE, FunctionToolCallStatus.valueOf("INCOMPLETE"));
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactlyThreeValues() {
      assertEquals(3, FunctionToolCallStatus.values().length);
    }
  }

  // =========================================================================
  // FunctionToolCallOutputStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("FunctionToolCallOutputStatus")
  class FunctionToolCallOutputStatusTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(FunctionToolCallOutputStatus.IN_PROGRESS);
      assertNotNull(FunctionToolCallOutputStatus.COMPLETED);
      assertNotNull(FunctionToolCallOutputStatus.INCOMPLETE);
    }
  }

  // =========================================================================
  // FileSearchToolCallStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("FileSearchToolCallStatus")
  class FileSearchToolCallStatusTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(FileSearchToolCallStatus.IN_PROGRESS);
      assertNotNull(FileSearchToolCallStatus.SEARCHING);
      assertNotNull(FileSearchToolCallStatus.INCOMPLETE);
      assertNotNull(FileSearchToolCallStatus.FAILED);
    }

    @Test
    @DisplayName("should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(FileSearchToolCallStatus.IN_PROGRESS, FileSearchToolCallStatus.valueOf("IN_PROGRESS"));
      assertEquals(FileSearchToolCallStatus.SEARCHING, FileSearchToolCallStatus.valueOf("SEARCHING"));
      assertEquals(FileSearchToolCallStatus.FAILED, FileSearchToolCallStatus.valueOf("FAILED"));
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      assertEquals(4, FileSearchToolCallStatus.values().length);
    }
  }

  // =========================================================================
  // CodeInterpreterToolCallStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("CodeInterpreterToolCallStatus")
  class CodeInterpreterToolCallStatusTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(CodeInterpreterToolCallStatus.IN_PROGRESS);
      assertNotNull(CodeInterpreterToolCallStatus.INTERPRETING);
      assertNotNull(CodeInterpreterToolCallStatus.COMPLETED);
      assertNotNull(CodeInterpreterToolCallStatus.INCOMPLETE);
    }
  }

  // =========================================================================
  // ComputerToolCallStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("ComputerToolCallStatus")
  class ComputerToolCallStatusTest {

    @Test
    @DisplayName("should have expected values")
    void shouldHaveExpectedValues() {
      assertNotNull(ComputerToolCallStatus.IN_PROGRESS);
      assertNotNull(ComputerToolCallStatus.COMPLETED);
      assertNotNull(ComputerToolCallStatus.INCOMPLETE);
    }
  }

  // =========================================================================
  // ComputerToolCallOutputStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("ComputerToolCallOutputStatus")
  class ComputerToolCallOutputStatusTest {

    @Test
    @DisplayName("should have expected values")
    void shouldHaveExpectedValues() {
      assertNotNull(ComputerToolCallOutputStatus.IN_PROGRESS);
      assertNotNull(ComputerToolCallOutputStatus.COMPLETED);
    }
  }

  // =========================================================================
  // McpToolCallStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("McpToolCallStatus")
  class McpToolCallStatusTest {

    @Test
    @DisplayName("should have expected values")
    void shouldHaveExpectedValues() {
      assertNotNull(McpToolCallStatus.IN_PROGRESS);
      assertNotNull(McpToolCallStatus.COMPLETED);
      assertNotNull(McpToolCallStatus.FAILED);
      assertNotNull(McpToolCallStatus.INCOMPLETE);
    }
  }

  // =========================================================================
  // LocalShellCallOutputStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("LocalShellCallOutputStatus")
  class LocalShellCallOutputStatusTest {

    @Test
    @DisplayName("should have expected values")
    void shouldHaveExpectedValues() {
      assertNotNull(LocalShellCallOutputStatus.IN_PROGRESS);
      assertNotNull(LocalShellCallOutputStatus.COMPLETED);
      assertNotNull(LocalShellCallOutputStatus.INCOMPLETE);
    }
  }

  // =========================================================================
  // FunctionShellToolCallStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("FunctionShellToolCallStatus")
  class FunctionShellToolCallStatusTest {

    @Test
    @DisplayName("should have expected values")
    void shouldHaveExpectedValues() {
      assertNotNull(FunctionShellToolCallStatus.IN_PROGRESS);
      assertNotNull(FunctionShellToolCallStatus.COMPLETED);
      assertNotNull(FunctionShellToolCallStatus.INCOMPLETE);
    }
  }

  // =========================================================================
  // ApplyPatchToolCallStatus Enum
  // =========================================================================
  @Nested
  @DisplayName("ApplyPatchToolCallStatus")
  class ApplyPatchToolCallStatusTest {

    @Test
    @DisplayName("should have expected values")
    void shouldHaveExpectedValues() {
      assertNotNull(ApplyPatchToolCallStatus.COMPLETED);
      assertNotNull(ApplyPatchToolCallStatus.FAILED);
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactlyTwoValues() {
      assertEquals(2, ApplyPatchToolCallStatus.values().length);
    }
  }
}
