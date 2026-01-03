package com.paragon.prompts;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PromptProviderException}.
 */
class PromptProviderExceptionTest {

  @Nested
  class ConstructorTests {

    @Test
    void constructor_messageAndPromptId_setsFields() {
      PromptProviderException ex = new PromptProviderException("Error message", "my-prompt");
      
      assertEquals("Error message", ex.getMessage());
      assertEquals("my-prompt", ex.promptId());
      assertNull(ex.getCause());
      assertFalse(ex.isRetryable());
    }

    @Test
    void constructor_messagePromptIdAndCause_setsFields() {
      RuntimeException cause = new RuntimeException("Root cause");
      PromptProviderException ex = new PromptProviderException("Error message", "my-prompt", cause);
      
      assertEquals("Error message", ex.getMessage());
      assertEquals("my-prompt", ex.promptId());
      assertEquals(cause, ex.getCause());
      assertFalse(ex.isRetryable());
    }

    @Test
    void constructor_withRetryable_setsRetryable() {
      PromptProviderException ex = new PromptProviderException(
          "Rate limited", "my-prompt", null, true);
      
      assertTrue(ex.isRetryable());
    }

    @Test
    void constructor_withNonRetryable_setsNotRetryable() {
      PromptProviderException ex = new PromptProviderException(
          "Not found", "my-prompt", null, false);
      
      assertFalse(ex.isRetryable());
    }

    @Test
    void constructor_nullPromptId_allowsNull() {
      PromptProviderException ex = new PromptProviderException("Error", null);
      
      assertNull(ex.promptId());
    }

    @Test
    void constructor_allParameters_setsAllFields() {
      IOException cause = new java.io.IOException("Network error");
      PromptProviderException ex = new PromptProviderException(
          "Failed to connect", "prompt-123", cause, true);
      
      assertEquals("Failed to connect", ex.getMessage());
      assertEquals("prompt-123", ex.promptId());
      assertEquals(cause, ex.getCause());
      assertTrue(ex.isRetryable());
    }
  }

  @Nested
  class AccessorTests {

    @Test
    void promptId_returnsPromptId() {
      PromptProviderException ex = new PromptProviderException("Error", "test-id");
      assertEquals("test-id", ex.promptId());
    }

    @Test
    void isRetryable_defaultsFalse() {
      PromptProviderException ex = new PromptProviderException("Error", "id");
      assertFalse(ex.isRetryable());
    }

    @Test
    void isRetryable_whenTrue_returnsTrue() {
      PromptProviderException ex = new PromptProviderException("Error", "id", null, true);
      assertTrue(ex.isRetryable());
    }
  }

  @Nested
  class InheritanceTests {

    @Test
    void extendsRuntimeException() {
      PromptProviderException ex = new PromptProviderException("Error", "id");
      assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void canBeCaughtAsRuntimeException() {
      assertThrows(RuntimeException.class, () -> {
        throw new PromptProviderException("Error", "id");
      });
    }

    @Test
    void getMessage_returnsMessage() {
      PromptProviderException ex = new PromptProviderException("Custom error message", "id");
      assertEquals("Custom error message", ex.getMessage());
    }

    @Test
    void getCause_returnsCause() {
      IllegalStateException cause = new IllegalStateException("Bad state");
      PromptProviderException ex = new PromptProviderException("Wrapper", "id", cause);
      assertEquals(cause, ex.getCause());
    }
  }
}
