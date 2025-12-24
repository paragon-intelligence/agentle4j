package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for various spec records: ResponseError, PendingSafetyCheck, ReasoningSummaryText.
 */
class AdditionalRecordsTest {

  @Nested
  @DisplayName("ResponseError")
  class ResponseErrorTests {

    @Test
    @DisplayName("can be created with code and message")
    void creationWithBoth() {
      ResponseError error = new ResponseError(ErrorCode.SERVER_ERROR, "Something went wrong");

      assertEquals(ErrorCode.SERVER_ERROR, error.code());
      assertEquals("Something went wrong", error.message());
    }

    @Test
    @DisplayName("can be created with null code")
    void nullCode() {
      ResponseError error = new ResponseError(null, "Error message");

      assertNull(error.code());
      assertEquals("Error message", error.message());
    }

    @Test
    @DisplayName("can be created with null message")
    void nullMessage() {
      ResponseError error = new ResponseError(ErrorCode.RATE_LIMIT_EXCEEDED, null);

      assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, error.code());
      assertNull(error.message());
    }

    @Test
    @DisplayName("can be created with both null")
    void bothNull() {
      ResponseError error = new ResponseError(null, null);

      assertNull(error.code());
      assertNull(error.message());
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      ResponseError error1 = new ResponseError(ErrorCode.SERVER_ERROR, "error");
      ResponseError error2 = new ResponseError(ErrorCode.SERVER_ERROR, "error");
      ResponseError error3 = new ResponseError(ErrorCode.RATE_LIMIT_EXCEEDED, "error");

      assertEquals(error1, error2);
      assertEquals(error1.hashCode(), error2.hashCode());
      assertNotEquals(error1, error3);
    }
  }

  @Nested
  @DisplayName("PendingSafetyCheck")
  class PendingSafetyCheckTests {

    @Test
    @DisplayName("can be created with all fields")
    void creationWithAll() {
      PendingSafetyCheck check = new PendingSafetyCheck("check-123", "dangerous_action", "Review needed");

      assertEquals("check-123", check.id());
      assertEquals("dangerous_action", check.code());
      assertEquals("Review needed", check.message());
    }

    @Test
    @DisplayName("can be created with nullable fields")
    void creationWithNulls() {
      PendingSafetyCheck check = new PendingSafetyCheck("check-456", null, null);

      assertEquals("check-456", check.id());
      assertNull(check.code());
      assertNull(check.message());
    }

    @Test
    @DisplayName("toString returns formatted output with values")
    void toStringWithValues() {
      PendingSafetyCheck check = new PendingSafetyCheck("id-1", "code-1", "message-1");

      String result = check.toString();

      assertTrue(result.contains("<pending_safety_check>"));
      assertTrue(result.contains("<id>id-1</id>"));
      assertTrue(result.contains("<code>code-1</code>"));
      assertTrue(result.contains("<message>message-1</message>"));
      assertTrue(result.contains("</pending_safety_check>"));
    }

    @Test
    @DisplayName("toString handles null values")
    void toStringWithNulls() {
      PendingSafetyCheck check = new PendingSafetyCheck("id-2", null, null);

      String result = check.toString();

      assertTrue(result.contains("<id>id-2</id>"));
      assertTrue(result.contains("<code>null</code>"));
      assertTrue(result.contains("<message>null</message>"));
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      PendingSafetyCheck check1 = new PendingSafetyCheck("id", "code", "msg");
      PendingSafetyCheck check2 = new PendingSafetyCheck("id", "code", "msg");
      PendingSafetyCheck check3 = new PendingSafetyCheck("id2", "code", "msg");

      assertEquals(check1, check2);
      assertEquals(check1.hashCode(), check2.hashCode());
      assertNotEquals(check1, check3);
    }
  }

  @Nested
  @DisplayName("ReasoningSummaryText")
  class ReasoningSummaryTextTests {

    @Test
    @DisplayName("can be created with text")
    void creation() {
      ReasoningSummaryText summary = new ReasoningSummaryText("This is a summary of reasoning");

      assertEquals("This is a summary of reasoning", summary.text());
    }

    @Test
    @DisplayName("implements ReasoningSummary")
    void implementsInterface() {
      ReasoningSummaryText summary = new ReasoningSummaryText("test");
      assertTrue(summary instanceof ReasoningSummary);
    }

    @Test
    @DisplayName("empty text is valid")
    void emptyText() {
      ReasoningSummaryText summary = new ReasoningSummaryText("");
      assertEquals("", summary.text());
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      ReasoningSummaryText summary1 = new ReasoningSummaryText("text");
      ReasoningSummaryText summary2 = new ReasoningSummaryText("text");
      ReasoningSummaryText summary3 = new ReasoningSummaryText("other");

      assertEquals(summary1, summary2);
      assertEquals(summary1.hashCode(), summary2.hashCode());
      assertNotEquals(summary1, summary3);
    }
  }
}
