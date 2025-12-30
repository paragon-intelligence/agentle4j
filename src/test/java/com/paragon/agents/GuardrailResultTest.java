package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for GuardrailResult.
 *
 * <p>Tests cover: - Passed result creation - Failed result creation - Status checking methods -
 * Reason handling for failures
 */
@DisplayName("GuardrailResult")
class GuardrailResultTest {

  @Nested
  @DisplayName("Passed Result")
  class PassedResult {

    @Test
    @DisplayName("passed() creates a passing result")
    void passed_createsPassingResult() {
      GuardrailResult result = GuardrailResult.passed();

      assertNotNull(result);
      assertInstanceOf(GuardrailResult.Passed.class, result);
    }

    @Test
    @DisplayName("isPassed() returns true for passed result")
    void isPassed_returnsTrueForPassedResult() {
      GuardrailResult result = GuardrailResult.passed();

      assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("isFailed() returns false for passed result")
    void isFailed_returnsFalseForPassedResult() {
      GuardrailResult result = GuardrailResult.passed();

      assertFalse(result.isFailed());
    }

    @Test
    @DisplayName("passed() returns cached singleton")
    void passed_returnsCachedSingleton() {
      GuardrailResult result1 = GuardrailResult.passed();
      GuardrailResult result2 = GuardrailResult.passed();

      assertSame(result1, result2);
    }
  }

  @Nested
  @DisplayName("Failed Result")
  class FailedResult {

    @Test
    @DisplayName("failed() creates a failing result")
    void failed_createsFailingResult() {
      GuardrailResult result = GuardrailResult.failed("Invalid input");

      assertNotNull(result);
      assertInstanceOf(GuardrailResult.Failed.class, result);
    }

    @Test
    @DisplayName("isPassed() returns false for failed result")
    void isPassed_returnsFalseForFailedResult() {
      GuardrailResult result = GuardrailResult.failed("Error");

      assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("isFailed() returns true for failed result")
    void isFailed_returnsTrueForFailedResult() {
      GuardrailResult result = GuardrailResult.failed("Error");

      assertTrue(result.isFailed());
    }

    @Test
    @DisplayName("Failed.reason() returns the rejection reason")
    void failed_reason_returnsReason() {
      GuardrailResult.Failed result =
          (GuardrailResult.Failed) GuardrailResult.failed("Custom message");

      assertEquals("Custom message", result.reason());
    }

    @Test
    @DisplayName("failed() throws when reason is null")
    void failed_throwsWhenReasonNull() {
      assertThrows(IllegalArgumentException.class, () -> GuardrailResult.failed(null));
    }

    @Test
    @DisplayName("failed() throws when reason is blank")
    void failed_throwsWhenReasonBlank() {
      assertThrows(IllegalArgumentException.class, () -> GuardrailResult.failed(""));
    }
  }

  @Nested
  @DisplayName("Type Safety")
  class TypeSafety {

    @Test
    @DisplayName("Passed and Failed are distinct types")
    void passed_and_failed_areDistinctTypes() {
      GuardrailResult passed = GuardrailResult.passed();
      GuardrailResult failed = GuardrailResult.failed("Error");

      assertNotEquals(passed.getClass(), failed.getClass());
    }

    @Test
    @DisplayName("Can pattern match on result types")
    void canPatternMatchOnResultTypes() {
      GuardrailResult passed = GuardrailResult.passed();
      GuardrailResult failed = GuardrailResult.failed("Error");

      String passedMessage =
          switch (passed) {
            case GuardrailResult.Passed p -> "passed";
            case GuardrailResult.Failed f -> "failed: " + f.reason();
          };

      String failedMessage =
          switch (failed) {
            case GuardrailResult.Passed p -> "passed";
            case GuardrailResult.Failed f -> "failed: " + f.reason();
          };

      assertEquals("passed", passedMessage);
      assertEquals("failed: Error", failedMessage);
    }
  }
}
