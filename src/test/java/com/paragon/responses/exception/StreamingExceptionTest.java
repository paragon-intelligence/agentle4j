package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StreamingException}. */
@DisplayName("StreamingException")
class StreamingExceptionTest {

  @Nested
  @DisplayName("Constructor")
  class Constructor {

    @Test
    @DisplayName("should create with partial output")
    void shouldCreateWithPartialOutput() {
      StreamingException e =
          new StreamingException(
              AgentleException.ErrorCode.CONNECTION_DROPPED,
              "Connection lost",
              "Partial content here",
              1024,
              true);

      assertEquals("Partial content here", e.partialOutput());
      assertEquals(1024, e.bytesReceived());
      assertTrue(e.isRetryable());
    }

    @Test
    @DisplayName("should create with null partial output")
    void shouldCreateWithNullPartialOutput() {
      StreamingException e =
          new StreamingException(
              AgentleException.ErrorCode.STREAM_TIMEOUT, "Timeout", null, 0, true);

      assertNull(e.partialOutput());
      assertEquals(0, e.bytesReceived());
    }

    @Test
    @DisplayName("should create with cause")
    void shouldCreateWithCause() {
      Exception cause = new java.io.IOException("Connection reset");
      StreamingException e =
          new StreamingException(
              AgentleException.ErrorCode.CONNECTION_DROPPED,
              "Connection dropped",
              cause,
              "Partial",
              512,
              true);

      assertEquals(cause, e.getCause());
      assertEquals("Partial", e.partialOutput());
      assertEquals(512, e.bytesReceived());
    }
  }

  @Nested
  @DisplayName("Factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("connectionDropped should create with correct code")
    void connectionDroppedShouldHaveCorrectCode() {
      Exception cause = new java.io.IOException("Reset by peer");
      StreamingException e = StreamingException.connectionDropped(cause, "partial", 256);

      assertEquals(AgentleException.ErrorCode.CONNECTION_DROPPED, e.code());
      assertEquals(cause, e.getCause());
      assertEquals("partial", e.partialOutput());
      assertEquals(256, e.bytesReceived());
      assertTrue(e.isRetryable());
      assertTrue(e.getMessage().contains("Connection dropped"));
    }

    @Test
    @DisplayName("timeout should create with correct code")
    void timeoutShouldHaveCorrectCode() {
      StreamingException e = StreamingException.timeout("timeout content", 100);

      assertEquals(AgentleException.ErrorCode.STREAM_TIMEOUT, e.code());
      assertEquals("timeout content", e.partialOutput());
      assertEquals(100, e.bytesReceived());
      assertTrue(e.isRetryable());
      assertTrue(e.getMessage().contains("timed out"));
    }
  }

  @Nested
  @DisplayName("Inheritance")
  class Inheritance {

    @Test
    @DisplayName("should extend AgentleException")
    void shouldExtendAgentleException() {
      StreamingException e =
          new StreamingException(
              AgentleException.ErrorCode.CONNECTION_DROPPED, "test", null, 0, false);

      assertInstanceOf(AgentleException.class, e);
    }
  }
}
