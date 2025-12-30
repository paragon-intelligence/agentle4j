package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AgentleException} base class. */
@DisplayName("AgentleException")
class AgentleExceptionTest {

  @Nested
  @DisplayName("Constructor")
  class Constructor {

    @Test
    @DisplayName("should create exception with all fields")
    void shouldCreateWithAllFields() {
      AgentleException e =
          new AgentleException(
              AgentleException.ErrorCode.RATE_LIMITED,
              "Rate limit exceeded",
              "Wait 30 seconds",
              true);

      assertEquals(AgentleException.ErrorCode.RATE_LIMITED, e.code());
      assertEquals("Rate limit exceeded", e.getMessage());
      assertEquals("Wait 30 seconds", e.suggestion());
      assertTrue(e.isRetryable());
    }

    @Test
    @DisplayName("should create exception with null suggestion")
    void shouldCreateWithNullSuggestion() {
      AgentleException e =
          new AgentleException(AgentleException.ErrorCode.SERVER_ERROR, "Server error", null, true);

      assertEquals(AgentleException.ErrorCode.SERVER_ERROR, e.code());
      assertNull(e.suggestion());
      assertTrue(e.isRetryable());
    }

    @Test
    @DisplayName("should create non-retryable exception")
    void shouldCreateNonRetryable() {
      AgentleException e =
          new AgentleException(
              AgentleException.ErrorCode.AUTHENTICATION_FAILED,
              "Invalid API key",
              "Check your credentials",
              false);

      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("should create exception with cause")
    void shouldCreateWithCause() {
      RuntimeException cause = new RuntimeException("Original error");
      AgentleException e =
          new AgentleException(
              AgentleException.ErrorCode.CONNECTION_DROPPED,
              "Connection failed",
              cause,
              "Check network",
              true);

      assertEquals(cause, e.getCause());
      assertEquals("Connection failed", e.getMessage());
    }
  }

  @Nested
  @DisplayName("ErrorCode enum")
  class ErrorCodeEnum {

    @Test
    @DisplayName("should have all expected codes")
    void shouldHaveAllExpectedCodes() {
      AgentleException.ErrorCode[] codes = AgentleException.ErrorCode.values();

      assertTrue(codes.length >= 12, "Should have at least 12 error codes");

      // Verify key codes exist
      assertNotNull(AgentleException.ErrorCode.RATE_LIMITED);
      assertNotNull(AgentleException.ErrorCode.AUTHENTICATION_FAILED);
      assertNotNull(AgentleException.ErrorCode.SERVER_ERROR);
      assertNotNull(AgentleException.ErrorCode.INVALID_REQUEST);
      assertNotNull(AgentleException.ErrorCode.CONNECTION_DROPPED);
      assertNotNull(AgentleException.ErrorCode.GUARDRAIL_VIOLATED);
      assertNotNull(AgentleException.ErrorCode.TOOL_EXECUTION_FAILED);
      assertNotNull(AgentleException.ErrorCode.MISSING_CONFIGURATION);
    }
  }

  @Nested
  @DisplayName("Inheritance")
  class Inheritance {

    @Test
    @DisplayName("should extend RuntimeException")
    void shouldExtendRuntimeException() {
      AgentleException e =
          new AgentleException(AgentleException.ErrorCode.UNKNOWN, "test", null, false);

      assertInstanceOf(RuntimeException.class, e);
    }
  }
}
