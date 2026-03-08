package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VerificationResult")
class VerificationResultTest {

  @Test
  @DisplayName("pass() creates a passing result with exit code 0")
  void passResult() {
    VerificationResult result = VerificationResult.pass("All tests passed");
    assertTrue(result.passed());
    assertEquals(0, result.exitCode());
    assertEquals("All tests passed", result.output());
  }

  @Test
  @DisplayName("fail() creates a failing result with given exit code")
  void failResult() {
    VerificationResult result = VerificationResult.fail("2 tests failed", 1);
    assertFalse(result.passed());
    assertEquals(1, result.exitCode());
    assertEquals("2 tests failed", result.output());
  }

  @Test
  @DisplayName("toSummary() includes PASSED for passing results")
  void toSummaryPassed() {
    VerificationResult result = VerificationResult.pass("ok");
    assertTrue(result.toSummary().contains("PASSED"));
  }

  @Test
  @DisplayName("toSummary() includes FAILED and exit code for failing results")
  void toSummaryFailed() {
    VerificationResult result = VerificationResult.fail("error output", 2);
    String summary = result.toSummary();
    assertTrue(summary.contains("FAILED"));
    assertTrue(summary.contains("2"));
    assertTrue(summary.contains("error output"));
  }
}
