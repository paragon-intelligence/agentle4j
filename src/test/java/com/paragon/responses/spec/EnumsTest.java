package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for spec package enums.
 */
@DisplayName("Spec Enums Tests")
class EnumsTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // RESPONSE GENERATION STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ResponseGenerationStatus")
  class ResponseGenerationStatusTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertNotNull(ResponseGenerationStatus.valueOf("COMPLETED"));
      assertNotNull(ResponseGenerationStatus.valueOf("FAILED"));
      assertNotNull(ResponseGenerationStatus.valueOf("IN_PROGRESS"));
      assertNotNull(ResponseGenerationStatus.valueOf("CANCELLED"));
      assertNotNull(ResponseGenerationStatus.valueOf("QUEUED"));
      assertNotNull(ResponseGenerationStatus.valueOf("INCOMPLETE"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INPUT MESSAGE STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("InputMessageStatus")
  class InputMessageStatusTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertNotNull(InputMessageStatus.valueOf("COMPLETED"));
      assertNotNull(InputMessageStatus.valueOf("IN_PROGRESS"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FUNCTION TOOL CALL OUTPUT STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionToolCallOutputStatus")
  class FunctionToolCallOutputStatusTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertNotNull(FunctionToolCallOutputStatus.valueOf("COMPLETED"));
      assertNotNull(FunctionToolCallOutputStatus.valueOf("IN_PROGRESS"));
      assertNotNull(FunctionToolCallOutputStatus.valueOf("INCOMPLETE"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FUNCTION TOOL CALL STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionToolCallStatus")
  class FunctionToolCallStatusTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertNotNull(FunctionToolCallStatus.valueOf("COMPLETED"));
      assertNotNull(FunctionToolCallStatus.valueOf("IN_PROGRESS"));
      assertNotNull(FunctionToolCallStatus.valueOf("INCOMPLETE"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MESSAGE ROLE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("MessageRole")
  class MessageRoleTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertNotNull(MessageRole.valueOf("USER"));
      assertNotNull(MessageRole.valueOf("ASSISTANT"));
      assertNotNull(MessageRole.valueOf("DEVELOPER"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RESPONSE OBJECT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ResponseObject")
  class ResponseObjectTests {

    @Test
    @DisplayName("has response value")
    void hasResponseValue() {
      assertNotNull(ResponseObject.valueOf("RESPONSE"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // IMAGE DETAIL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ImageDetail")
  class ImageDetailTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertNotNull(ImageDetail.valueOf("HIGH"));
      assertNotNull(ImageDetail.valueOf("LOW"));
      assertNotNull(ImageDetail.valueOf("AUTO"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRUNCATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Truncation")
  class TruncationTests {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertNotNull(Truncation.valueOf("AUTO"));
      assertNotNull(Truncation.valueOf("DISABLED"));
    }
  }
}
