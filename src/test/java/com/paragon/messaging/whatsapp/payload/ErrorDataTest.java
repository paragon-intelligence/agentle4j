package com.paragon.messaging.whatsapp.payload;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ErrorData} and {@link ErrorData.ErrorDetails}. */
@DisplayName("ErrorData")
class ErrorDataTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Nested
  @DisplayName("ErrorData Deserialization")
  class DeserializationTests {

    @Test
    @DisplayName("deserializes error with all fields")
    void deserializesWithAllFields() throws JsonProcessingException {
      String json =
          """
          {
            "code": 131047,
            "title": "Re-engagement message",
            "message": "Re-engagement message not sent",
            "error_data": {
              "details": "Send a template message to re-engage the user"
            }
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertNotNull(error);
      assertEquals(131047, error.code());
      assertEquals("Re-engagement message", error.title());
      assertEquals("Re-engagement message not sent", error.message());
      assertNotNull(error.errorData());
      assertEquals("Send a template message to re-engage the user", error.errorData().details());
    }

    @Test
    @DisplayName("deserializes error with minimal fields")
    void deserializesMinimalError() throws JsonProcessingException {
      String json =
          """
          {
            "code": 100
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertNotNull(error);
      assertEquals(100, error.code());
      assertNull(error.title());
      assertNull(error.message());
      assertNull(error.errorData());
    }

    @Test
    @DisplayName("deserializes error without error_data")
    void deserializesWithoutErrorData() throws JsonProcessingException {
      String json =
          """
          {
            "code": 131031,
            "title": "Rate limit hit",
            "message": "Message failed to send because there were too many messages sent from this phone number"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(131031, error.code());
      assertNotNull(error.message());
      assertNull(error.errorData());
    }
  }

  @Nested
  @DisplayName("Common WhatsApp Error Codes")
  class CommonErrorCodesTests {

    @Test
    @DisplayName("handles authentication error (code 0)")
    void handlesAuthenticationError() throws JsonProcessingException {
      String json =
          """
          {
            "code": 0,
            "title": "Authentication failed",
            "message": "Invalid access token"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(0, error.code());
    }

    @Test
    @DisplayName("handles parameter error (code 100)")
    void handlesParameterError() throws JsonProcessingException {
      String json =
          """
          {
            "code": 100,
            "title": "Invalid parameter",
            "message": "Parameter value is not valid"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(100, error.code());
    }

    @Test
    @DisplayName("handles rate limit error (code 131031)")
    void handlesRateLimitError() throws JsonProcessingException {
      String json =
          """
          {
            "code": 131031,
            "title": "Rate limit hit",
            "message": "Message failed to send because there were too many messages sent from this phone number"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(131031, error.code());
      assertTrue(error.message().contains("too many messages"));
    }

    @Test
    @DisplayName("handles re-engagement error (code 131047)")
    void handlesReEngagementError() throws JsonProcessingException {
      String json =
          """
          {
            "code": 131047,
            "title": "Re-engagement message",
            "message": "Re-engagement message not sent",
            "error_data": {
              "details": "Customer needs to message you first"
            }
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(131047, error.code());
      assertNotNull(error.errorData());
    }

    @Test
    @DisplayName("handles missing template error (code 132000)")
    void handlesMissingTemplateError() throws JsonProcessingException {
      String json =
          """
          {
            "code": 132000,
            "title": "Template does not exist",
            "message": "Template name does not exist in the translation"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(132000, error.code());
    }
  }

  @Nested
  @DisplayName("ErrorDetails")
  class ErrorDetailsTests {

    @Test
    @DisplayName("deserializes error details")
    void deserializesErrorDetails() throws JsonProcessingException {
      String json =
          """
          {
            "details": "Additional error information"
          }
          """;

      ErrorData.ErrorDetails details = objectMapper.readValue(json, ErrorData.ErrorDetails.class);

      assertNotNull(details);
      assertEquals("Additional error information", details.details());
    }

    @Test
    @DisplayName("creates error details")
    void createsErrorDetails() {
      ErrorData.ErrorDetails details = new ErrorData.ErrorDetails("Test details");

      assertEquals("Test details", details.details());
    }
  }

  @Nested
  @DisplayName("Real-World Error Scenarios")
  class RealWorldScenariosTests {

    @Test
    @DisplayName("handles expired business account error")
    void handlesExpiredBusinessAccount() throws JsonProcessingException {
      String json =
          """
          {
            "code": 368,
            "title": "WhatsApp Business Account Expired",
            "message": "Unable to deliver message. Reason: WhatsApp Business Account has been restricted or disabled.",
            "error_data": {
              "details": "Contact WhatsApp support to resolve this issue"
            }
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(368, error.code());
      assertTrue(error.message().contains("restricted or disabled"));
      assertNotNull(error.errorData());
    }

    @Test
    @DisplayName("handles invalid recipient error")
    void handlesInvalidRecipient() throws JsonProcessingException {
      String json =
          """
          {
            "code": 1006,
            "title": "Resource not found",
            "message": "The phone number is not registered on WhatsApp"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(1006, error.code());
      assertTrue(error.message().contains("not registered"));
    }

    @Test
    @DisplayName("handles media upload error")
    void handlesMediaUploadError() throws JsonProcessingException {
      String json =
          """
          {
            "code": 131052,
            "title": "Media upload error",
            "message": "Media download failed. The media is either invalid or corrupted.",
            "error_data": {
              "details": "Check media URL and try again"
            }
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(131052, error.code());
      assertTrue(error.message().contains("Media download failed"));
    }

    @Test
    @DisplayName("handles template parameter mismatch")
    void handlesTemplateParameterMismatch() throws JsonProcessingException {
      String json =
          """
          {
            "code": 132012,
            "title": "Parameter count mismatch",
            "message": "The number of variable parameter values doesn't match format string",
            "error_data": {
              "details": "Template expects 3 parameters but received 2"
            }
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals(132012, error.code());
      assertNotNull(error.errorData());
    }
  }

  @Nested
  @DisplayName("JSON Field Mapping")
  class JsonFieldMappingTests {

    @Test
    @DisplayName("maps error_data to errorData")
    void mapsErrorData() throws JsonProcessingException {
      String json =
          """
          {
            "code": 100,
            "error_data": {
              "details": "Nested details"
            }
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertNotNull(error.errorData());
      assertEquals("Nested details", error.errorData().details());
    }

    @Test
    @DisplayName("round-trip serialization preserves snake_case")
    void roundTripPreservesSnakeCase() throws JsonProcessingException {
      ErrorData.ErrorDetails details = new ErrorData.ErrorDetails("Test");
      ErrorData original = new ErrorData(100, "Title", "Message", details);

      String json = objectMapper.writeValueAsString(original);
      ErrorData deserialized = objectMapper.readValue(json, ErrorData.class);

      assertEquals(original.code(), deserialized.code());
      assertEquals(original.errorData().details(), deserialized.errorData().details());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("handles null title")
    void handlesNullTitle() {
      ErrorData error = new ErrorData(100, null, "Message", null);

      assertNull(error.title());
      assertEquals(100, error.code());
    }

    @Test
    @DisplayName("handles null message")
    void handlesNullMessage() {
      ErrorData error = new ErrorData(100, "Title", null, null);

      assertNull(error.message());
    }

    @Test
    @DisplayName("handles all null optional fields")
    void handlesAllNullFields() {
      ErrorData error = new ErrorData(100, null, null, null);

      assertEquals(100, error.code());
      assertNull(error.title());
      assertNull(error.message());
      assertNull(error.errorData());
    }

    @Test
    @DisplayName("handles Unicode in error messages")
    void handlesUnicodeInMessages() throws JsonProcessingException {
      String json =
          """
          {
            "code": 100,
            "title": "错误",
            "message": "メッセージが失敗しました"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertEquals("错误", error.title());
      assertEquals("メッセージが失敗しました", error.message());
    }

    @Test
    @DisplayName("handles very long error messages")
    void handlesLongMessages() throws JsonProcessingException {
      String longMessage = "Error: " + "x".repeat(1000);
      String json =
          String.format(
              """
              {
                "code": 100,
                "message": "%s"
              }
              """,
              longMessage);

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      assertTrue(error.message().length() > 1000);
    }
  }

  @Nested
  @DisplayName("Programmatic Error Handling")
  class ProgrammaticHandlingTests {

    @Test
    @DisplayName("uses code for programmatic handling")
    void usesCodeForHandling() throws JsonProcessingException {
      String json =
          """
          {
            "code": 131047,
            "title": "Deprecated title",
            "message": "Use code, not title"
          }
          """;

      ErrorData error = objectMapper.readValue(json, ErrorData.class);

      // Demonstrate code-based handling (not title-based)
      switch (error.code()) {
        case 131047 -> assertEquals("Re-engagement required", "Re-engagement required");
        case 131031 -> assertEquals("Rate limited", "Rate limited");
        default -> fail("Unknown error code");
      }
    }
  }
}
