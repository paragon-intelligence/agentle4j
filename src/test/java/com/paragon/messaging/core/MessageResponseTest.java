package com.paragon.messaging.core;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link MessageResponse} record and validation. */
@DisplayName("MessageResponse")
class MessageResponseTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("creates response with all fields")
    void createsWithAllFields() {
      Instant now = Instant.now();
      MessageResponse response =
          new MessageResponse(
              "wamid.123", MessageResponse.MessageStatus.SENT, now, Optional.of("conv-123"));

      assertEquals("wamid.123", response.messageId());
      assertEquals(MessageResponse.MessageStatus.SENT, response.status());
      assertEquals(now, response.timestamp());
      assertTrue(response.conversationId().isPresent());
      assertEquals("conv-123", response.conversationId().get());
    }

    @Test
    @DisplayName("creates response without conversation ID")
    void createsWithoutConversationId() {
      Instant now = Instant.now();
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.DELIVERED, now);

      assertEquals("wamid.123", response.messageId());
      assertTrue(response.conversationId().isEmpty());
    }

    @Test
    @DisplayName("creates response with explicit empty conversation ID")
    void createsWithEmptyConversationId() {
      Instant now = Instant.now();
      MessageResponse response =
          new MessageResponse(
              "wamid.123", MessageResponse.MessageStatus.DELIVERED, now, Optional.empty());

      assertTrue(response.conversationId().isEmpty());
    }
  }

  @Nested
  @DisplayName("Bean Validation")
  class ValidationTests {

    @Test
    @DisplayName("validates message ID not blank")
    void validatesMessageIdNotBlank() {
      MessageResponse response =
          new MessageResponse("", MessageResponse.MessageStatus.SENT, Instant.now());

      Set<ConstraintViolation<MessageResponse>> violations = validator.validate(response);
      assertFalse(violations.isEmpty());
      assertTrue(
          violations.stream().anyMatch(v -> v.getMessage().contains("Message ID cannot be blank")));
    }

    @Test
    @DisplayName("validates status not null")
    void validatesStatusNotNull() {
      MessageResponse response = new MessageResponse("wamid.123", null, Instant.now());

      Set<ConstraintViolation<MessageResponse>> violations = validator.validate(response);
      assertFalse(violations.isEmpty());
      assertTrue(
          violations.stream().anyMatch(v -> v.getMessage().contains("Status cannot be null")));
    }

    @Test
    @DisplayName("validates timestamp not null")
    void validatesTimestampNotNull() {
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, null);

      Set<ConstraintViolation<MessageResponse>> violations = validator.validate(response);
      assertFalse(violations.isEmpty());
      assertTrue(
          violations.stream().anyMatch(v -> v.getMessage().contains("Timestamp cannot be null")));
    }

    @Test
    @DisplayName("accepts valid response")
    void acceptsValidResponse() {
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, Instant.now());

      Set<ConstraintViolation<MessageResponse>> violations = validator.validate(response);
      assertTrue(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("MessageStatus Enum")
  class MessageStatusTests {

    @Test
    @DisplayName("has ACCEPTED status")
    void hasAcceptedStatus() {
      assertEquals(
          MessageResponse.MessageStatus.ACCEPTED,
          MessageResponse.MessageStatus.valueOf("ACCEPTED"));
    }

    @Test
    @DisplayName("has SENT status")
    void hasSentStatus() {
      assertEquals(
          MessageResponse.MessageStatus.SENT, MessageResponse.MessageStatus.valueOf("SENT"));
    }

    @Test
    @DisplayName("has DELIVERED status")
    void hasDeliveredStatus() {
      assertEquals(
          MessageResponse.MessageStatus.DELIVERED,
          MessageResponse.MessageStatus.valueOf("DELIVERED"));
    }

    @Test
    @DisplayName("has READ status")
    void hasReadStatus() {
      assertEquals(
          MessageResponse.MessageStatus.READ, MessageResponse.MessageStatus.valueOf("READ"));
    }

    @Test
    @DisplayName("has FAILED status")
    void hasFailedStatus() {
      assertEquals(
          MessageResponse.MessageStatus.FAILED, MessageResponse.MessageStatus.valueOf("FAILED"));
    }

    @Test
    @DisplayName("has UNKNOWN status")
    void hasUnknownStatus() {
      assertEquals(
          MessageResponse.MessageStatus.UNKNOWN, MessageResponse.MessageStatus.valueOf("UNKNOWN"));
    }
  }

  @Nested
  @DisplayName("Real-World Scenarios")
  class RealWorldScenariosTests {

    @Test
    @DisplayName("handles successful message send")
    void handlesSuccessfulSend() {
      Instant sendTime = Instant.now();
      MessageResponse response =
          new MessageResponse("wamid.HxA1234567890", MessageResponse.MessageStatus.SENT, sendTime);

      assertEquals("wamid.HxA1234567890", response.messageId());
      assertEquals(MessageResponse.MessageStatus.SENT, response.status());
      assertEquals(sendTime, response.timestamp());
    }

    @Test
    @DisplayName("handles delivery confirmation")
    void handlesDeliveryConfirmation() {
      MessageResponse response =
          new MessageResponse(
              "wamid.ABC123", MessageResponse.MessageStatus.DELIVERED, Instant.now());

      assertEquals(MessageResponse.MessageStatus.DELIVERED, response.status());
    }

    @Test
    @DisplayName("handles read receipt")
    void handlesReadReceipt() {
      MessageResponse response =
          new MessageResponse("wamid.ABC123", MessageResponse.MessageStatus.READ, Instant.now());

      assertEquals(MessageResponse.MessageStatus.READ, response.status());
    }

    @Test
    @DisplayName("handles failed message")
    void handlesFailedMessage() {
      MessageResponse response =
          new MessageResponse("wamid.FAIL123", MessageResponse.MessageStatus.FAILED, Instant.now());

      assertEquals(MessageResponse.MessageStatus.FAILED, response.status());
    }

    @Test
    @DisplayName("handles message with conversation tracking")
    void handlesConversationTracking() {
      MessageResponse response =
          new MessageResponse(
              "wamid.CONV123",
              MessageResponse.MessageStatus.SENT,
              Instant.now(),
              Optional.of("conversation_abc"));

      assertTrue(response.conversationId().isPresent());
      assertEquals("conversation_abc", response.conversationId().get());
    }
  }

  @Nested
  @DisplayName("Timestamp Handling")
  class TimestampTests {

    @Test
    @DisplayName("preserves exact timestamp")
    void preservesExactTimestamp() {
      Instant specificTime = Instant.parse("2024-02-04T12:00:00Z");
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, specificTime);

      assertEquals(specificTime, response.timestamp());
    }

    @Test
    @DisplayName("handles current timestamp")
    void handlesCurrentTimestamp() {
      Instant before = Instant.now();
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, Instant.now());
      Instant after = Instant.now();

      assertFalse(response.timestamp().isBefore(before));
      assertFalse(response.timestamp().isAfter(after));
    }
  }

  @Nested
  @DisplayName("Optional ConversationId")
  class ConversationIdTests {

    @Test
    @DisplayName("conversationId is empty by default")
    void conversationIdEmptyByDefault() {
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, Instant.now());

      assertTrue(response.conversationId().isEmpty());
    }

    @Test
    @DisplayName("conversationId can be set")
    void conversationIdCanBeSet() {
      MessageResponse response =
          new MessageResponse(
              "wamid.123",
              MessageResponse.MessageStatus.SENT,
              Instant.now(),
              Optional.of("conv-abc"));

      assertTrue(response.conversationId().isPresent());
      assertEquals("conv-abc", response.conversationId().get());
    }

    @Test
    @DisplayName("conversationId can be explicitly empty")
    void conversationIdCanBeExplicitlyEmpty() {
      MessageResponse response =
          new MessageResponse(
              "wamid.123", MessageResponse.MessageStatus.SENT, Instant.now(), Optional.empty());

      assertTrue(response.conversationId().isEmpty());
    }
  }

  @Nested
  @DisplayName("Equality and Hashing")
  class EqualityTests {

    @Test
    @DisplayName("responses with same data are equal")
    void responsesWithSameDataAreEqual() {
      Instant time = Instant.now();
      MessageResponse r1 =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, time);
      MessageResponse r2 =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, time);

      assertEquals(r1, r2);
      assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    @DisplayName("responses with different message IDs are not equal")
    void responsesWithDifferentIdsNotEqual() {
      Instant time = Instant.now();
      MessageResponse r1 =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, time);
      MessageResponse r2 =
          new MessageResponse("wamid.456", MessageResponse.MessageStatus.SENT, time);

      assertNotEquals(r1, r2);
    }

    @Test
    @DisplayName("responses with different statuses are not equal")
    void responsesWithDifferentStatusesNotEqual() {
      Instant time = Instant.now();
      MessageResponse r1 =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, time);
      MessageResponse r2 =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.DELIVERED, time);

      assertNotEquals(r1, r2);
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("handles very long message ID")
    void handlesLongMessageId() {
      String longId = "wamid." + "x".repeat(100);
      MessageResponse response =
          new MessageResponse(longId, MessageResponse.MessageStatus.SENT, Instant.now());

      assertEquals(longId, response.messageId());
    }

    @Test
    @DisplayName("handles past timestamp")
    void handlesPastTimestamp() {
      Instant pastTime = Instant.parse("2020-01-01T00:00:00Z");
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, pastTime);

      assertEquals(pastTime, response.timestamp());
    }

    @Test
    @DisplayName("handles future timestamp")
    void handlesFutureTimestamp() {
      Instant futureTime = Instant.parse("2030-01-01T00:00:00Z");
      MessageResponse response =
          new MessageResponse("wamid.123", MessageResponse.MessageStatus.SENT, futureTime);

      assertEquals(futureTime, response.timestamp());
    }
  }
}
