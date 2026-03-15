package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.ResponseInputItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for StructuredAgentResult class.
 *
 * <p>Note: We use null for Response parameters since Response is a complex class that's difficult
 * to mock. The core logic of StructuredAgentResult doesn't depend on the actual Response content.
 */
class StructuredAgentResultTest {

  @Nested
  @DisplayName("Factory method: success")
  class SuccessFactoryTests {

    @Test
    @DisplayName("success factory creates a result with correct values")
    void successFactoryCreatesCorrectResult() {
      String output = "test output";
      String rawOutput = "{\"data\": \"test\"}";
      List<ResponseInputItem> history = List.of();
      List<ToolExecution> toolExecutions = List.of();
      int turnsUsed = 3;

      StructuredAgentResult<String> result =
          StructuredAgentResult.success(
              output, rawOutput, null, history, toolExecutions, turnsUsed);

      assertEquals(output, result.parsed());
      assertEquals(rawOutput, result.output());
      assertNull(result.finalResponse());
      assertEquals(history, result.history());
      assertEquals(toolExecutions, result.toolExecutions());
      assertEquals(turnsUsed, result.turnsUsed());
      assertNull(result.handoffAgent());
      assertNull(result.error());
    }

    @Test
    @DisplayName("success result returns true for isSuccess")
    void successResultIsSuccess() {
      StructuredAgentResult<Integer> result =
          StructuredAgentResult.success(42, "42", null, List.of(), List.of(), 1);

      assertTrue(result.isSuccess());
      assertFalse(result.isError());
      assertFalse(result.isHandoff());
    }

    @Test
    @DisplayName("success result has null errorMessage")
    void successResultHasNullErrorMessage() {
      StructuredAgentResult<Integer> result =
          StructuredAgentResult.success(42, "42", null, List.of(), List.of(), 1);

      assertNull(result.errorMessage());
    }

    @Test
    @DisplayName("success with different types works")
    void successWithDifferentTypes() {
      // Test with Integer
      StructuredAgentResult<Integer> intResult =
          StructuredAgentResult.success(100, "100", null, List.of(), List.of(), 1);
      assertEquals(100, intResult.parsed());

      // Test with custom object
      record TestData(String name, int value) {}
      TestData data = new TestData("test", 42);
      StructuredAgentResult<TestData> objResult =
          StructuredAgentResult.success(data, "{}", null, List.of(), List.of(), 1);
      assertEquals(data, objResult.parsed());
    }
  }

  @Nested
  @DisplayName("Factory method: error")
  class ErrorFactoryTests {

    @Test
    @DisplayName("error factory creates a result with correct values")
    void errorFactoryCreatesCorrectResult() {
      Throwable error = new RuntimeException("Test error");
      String rawOutput = "partial output";
      List<ResponseInputItem> history = List.of();
      List<ToolExecution> toolExecutions = List.of();
      int turnsUsed = 2;

      StructuredAgentResult<String> result =
          StructuredAgentResult.error(error, rawOutput, null, history, toolExecutions, turnsUsed);

      assertThrows(IllegalStateException.class, result::parsed);
      assertEquals(rawOutput, result.output());
      assertNull(result.finalResponse());
      assertEquals(history, result.history());
      assertEquals(toolExecutions, result.toolExecutions());
      assertEquals(turnsUsed, result.turnsUsed());
      assertNull(result.handoffAgent());
      assertEquals(error, result.error());
    }

    @Test
    @DisplayName("error result returns true for isError")
    void errorResultIsError() {
      StructuredAgentResult<Integer> result =
          StructuredAgentResult.error(
              new RuntimeException("err"), null, null, List.of(), List.of(), 1);

      assertTrue(result.isError());
      assertFalse(result.isSuccess());
      assertFalse(result.isHandoff());
    }

    @Test
    @DisplayName("error result returns error message")
    void errorResultHasErrorMessage() {
      String errorMessage = "Something went wrong";
      StructuredAgentResult<Integer> result =
          StructuredAgentResult.error(
              new RuntimeException(errorMessage), null, null, List.of(), List.of(), 1);

      assertEquals(errorMessage, result.errorMessage());
    }

    @Test
    @DisplayName("error factory with null rawOutput uses empty string")
    void errorWithNullRawOutputUsesEmptyString() {
      StructuredAgentResult<String> result =
          StructuredAgentResult.error(
              new RuntimeException("err"), null, null, List.of(), List.of(), 1);

      assertEquals("", result.output());
    }

    @Test
    @DisplayName("error factory with non-null rawOutput preserves it")
    void errorWithNonNullRawOutputPreservesIt() {
      String rawOutput = "some partial output";
      StructuredAgentResult<String> result =
          StructuredAgentResult.error(
              new RuntimeException("err"), rawOutput, null, List.of(), List.of(), 1);

      assertEquals(rawOutput, result.output());
    }
  }

  @Nested
  @DisplayName("Status predicate methods")
  class StatusPredicateTests {

    @Test
    @DisplayName("result with no error and no handoff is success")
    void noErrorNoHandoffIsSuccess() {
      StructuredAgentResult<String> result =
          StructuredAgentResult.success("output", "raw", null, List.of(), List.of(), 1);

      assertTrue(result.isSuccess());
      assertFalse(result.isError());
      assertFalse(result.isHandoff());
    }

    @Test
    @DisplayName("result with error is not success")
    void errorIsNotSuccess() {
      StructuredAgentResult<String> result =
          StructuredAgentResult.error(
              new RuntimeException("err"), "output", null, List.of(), List.of(), 1);

      assertFalse(result.isSuccess());
      assertTrue(result.isError());
      assertFalse(result.isHandoff());
    }
  }

  @Nested
  @DisplayName("Equality")
  class EqualityTests {

    @Test
    @DisplayName("equal results are equal")
    void equalResults() {
      List<ResponseInputItem> history = List.of();
      List<ToolExecution> toolExecutions = List.of();

      StructuredAgentResult<String> result1 =
          StructuredAgentResult.success("out", "raw", null, history, toolExecutions, 1);
      StructuredAgentResult<String> result2 =
          StructuredAgentResult.success("out", "raw", null, history, toolExecutions, 1);

      assertEquals(result1.parsed(), result2.parsed());
      assertEquals(result1.output(), result2.output());
      assertEquals(result1.turnsUsed(), result2.turnsUsed());
      assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("different outputs are not equal")
    void differentOutputsNotEqual() {
      StructuredAgentResult<String> result1 =
          StructuredAgentResult.success("out1", "raw", null, List.of(), List.of(), 1);
      StructuredAgentResult<String> result2 =
          StructuredAgentResult.success("out2", "raw", null, List.of(), List.of(), 1);

      assertNotEquals(result1.parsed(), result2.parsed());
      assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("different turnsUsed are not equal")
    void differentTurnsNotEqual() {
      StructuredAgentResult<String> result1 =
          StructuredAgentResult.success("out", "raw", null, List.of(), List.of(), 1);
      StructuredAgentResult<String> result2 =
          StructuredAgentResult.success("out", "raw", null, List.of(), List.of(), 5);

      assertNotEquals(result1, result2);
    }
  }
}
