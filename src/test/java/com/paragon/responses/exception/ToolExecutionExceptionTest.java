package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ToolExecutionException}. */
@DisplayName("ToolExecutionException")
class ToolExecutionExceptionTest {

  @Nested
  @DisplayName("Constructor")
  class Constructor {

    @Test
    @DisplayName("should create with tool context")
    void shouldCreateWithToolContext() {
      ToolExecutionException e =
          new ToolExecutionException(
              "get_weather", "call-123", "{\"city\":\"NYC\"}", "Connection timeout");

      assertEquals("get_weather", e.toolName());
      assertEquals("call-123", e.callId());
      assertEquals("{\"city\":\"NYC\"}", e.arguments());
      assertEquals("Connection timeout", e.getMessage());
      assertEquals(AgentleException.ErrorCode.TOOL_EXECUTION_FAILED, e.code());
      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("should create with null optional fields")
    void shouldCreateWithNullOptionalFields() {
      ToolExecutionException e = new ToolExecutionException("my_tool", null, null, "Tool failed");

      assertEquals("my_tool", e.toolName());
      assertNull(e.callId());
      assertNull(e.arguments());
    }

    @Test
    @DisplayName("should create with cause")
    void shouldCreateWithCause() {
      Exception cause = new RuntimeException("Underlying error");
      ToolExecutionException e =
          new ToolExecutionException(
              "database_query", "call-456", "{\"query\":\"SELECT *\"}", "Query failed", cause);

      assertEquals(cause, e.getCause());
      assertEquals("database_query", e.toolName());
      assertEquals("call-456", e.callId());
      assertEquals("{\"query\":\"SELECT *\"}", e.arguments());
    }
  }

  @Nested
  @DisplayName("Inheritance")
  class Inheritance {

    @Test
    @DisplayName("should extend AgentleException")
    void shouldExtendAgentleException() {
      ToolExecutionException e = new ToolExecutionException("tool", null, null, "error");

      assertInstanceOf(AgentleException.class, e);
    }
  }
}
