package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.ComputerUseAction;
import com.paragon.responses.spec.MessageContent;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for discriminated union error cases.
 *
 * <p>Tests that missing or unknown discriminator fields throw descriptive errors. Validates:
 * Requirements 5.3, 5.4
 */
class DiscriminatedUnionErrorTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  /**
   * Test that missing discriminator field throws descriptive error for MessageContent. Validates:
   * Requirement 5.3
   */
  @Test
  void messageContentMissingDiscriminatorThrowsError() {
    // JSON without type field
    String jsonWithoutType = "{\"text\":\"Hello world\"}";

    // Attempt to deserialize should throw exception
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              mapper.readValue(jsonWithoutType, MessageContent.class);
            });

    // Verify error message is descriptive
    String message = exception.getMessage();
    assertNotNull(message, "Exception should have a message");
    assertTrue(
        message.contains("type")
            || message.contains("discriminator")
            || message.contains("missing"),
        "Error message should mention missing type/discriminator field");
  }

  /**
   * Test that unknown discriminator value throws descriptive error for MessageContent. Validates:
   * Requirement 5.4
   */
  @Test
  void messageContentUnknownDiscriminatorThrowsError() {
    // JSON with invalid type value
    String jsonWithInvalidType = "{\"type\":\"unknown_type\",\"text\":\"Hello\"}";

    // Attempt to deserialize should throw InvalidTypeIdException
    InvalidTypeIdException exception =
        assertThrows(
            InvalidTypeIdException.class,
            () -> {
              mapper.readValue(jsonWithInvalidType, MessageContent.class);
            });

    // Verify error message is descriptive
    String message = exception.getMessage();
    assertNotNull(message, "Exception should have a message");
    assertTrue(
        message.contains("unknown_type") || message.contains("not recognized"),
        "Error message should mention the unknown type value");
  }

  /**
   * Test that missing discriminator field throws descriptive error for ComputerUseAction.
   * Validates: Requirement 5.3
   */
  @Test
  void computerUseActionMissingDiscriminatorThrowsError() {
    // JSON without type field
    String jsonWithoutType = "{\"text\":\"some text\"}";

    // Attempt to deserialize should throw exception
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              mapper.readValue(jsonWithoutType, ComputerUseAction.class);
            });

    // Verify error message is descriptive
    String message = exception.getMessage();
    assertNotNull(message, "Exception should have a message");
    assertTrue(
        message.contains("type")
            || message.contains("discriminator")
            || message.contains("missing"),
        "Error message should mention missing type/discriminator field");
  }

  /**
   * Test that unknown discriminator value throws descriptive error for ComputerUseAction.
   * Validates: Requirement 5.4
   */
  @Test
  void computerUseActionUnknownDiscriminatorThrowsError() {
    // JSON with invalid type value
    String jsonWithInvalidType = "{\"type\":\"invalid_action\",\"x\":100,\"y\":200}";

    // Attempt to deserialize should throw InvalidTypeIdException
    InvalidTypeIdException exception =
        assertThrows(
            InvalidTypeIdException.class,
            () -> {
              mapper.readValue(jsonWithInvalidType, ComputerUseAction.class);
            });

    // Verify error message is descriptive
    String message = exception.getMessage();
    assertNotNull(message, "Exception should have a message");
    assertTrue(
        message.contains("invalid_action") || message.contains("not recognized"),
        "Error message should mention the unknown type value");
  }

  /**
   * Test that valid MessageContent with correct discriminator deserializes successfully. This is a
   * positive test to ensure our error tests aren't too strict.
   */
  @Test
  void messageContentWithValidDiscriminatorDeserializesSuccessfully() throws Exception {
    // Valid JSON with correct type
    String validJson = "{\"type\":\"input_text\",\"text\":\"Hello world\"}";

    // Should deserialize without error
    MessageContent content = mapper.readValue(validJson, MessageContent.class);

    assertNotNull(content, "Should successfully deserialize valid JSON");
    assertTrue(
        content instanceof com.paragon.responses.spec.Text, "Should deserialize to Text type");
  }

  /**
   * Test that valid ComputerUseAction with correct discriminator deserializes successfully. This is
   * a positive test to ensure our error tests aren't too strict.
   */
  @Test
  void computerUseActionWithValidDiscriminatorDeserializesSuccessfully() throws Exception {
    // Valid JSON with correct type
    String validJson = "{\"type\":\"type\",\"text\":\"Hello\"}";

    // Should deserialize without error
    ComputerUseAction action = mapper.readValue(validJson, ComputerUseAction.class);

    assertNotNull(action, "Should successfully deserialize valid JSON");
    assertTrue(
        action instanceof com.paragon.responses.spec.TypeAction,
        "Should deserialize to TypeAction type");
  }
}
