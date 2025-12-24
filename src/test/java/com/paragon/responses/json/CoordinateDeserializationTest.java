package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.MoveAction;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Coordinate deserialization error cases.
 *
 * <p>Feature: responses-api-jackson-serialization Validates: Requirements 4.4
 */
class CoordinateDeserializationTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  /** Test that JSON with only x field throws descriptive error. Validates: Requirements 4.4 */
  @Test
  void deserializationFailsWithOnlyXField() {
    String json = "{\"type\": \"move\", \"x\": 100}";

    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              mapper.readValue(json, MoveAction.class);
            });

    // Verify error message is descriptive
    String message = exception.getMessage().toLowerCase();
    assertTrue(
        message.contains("y") || message.contains("missing") || message.contains("required"),
        "Error message should indicate missing y field: " + exception.getMessage());
  }

  /** Test that JSON with only y field throws descriptive error. Validates: Requirements 4.4 */
  @Test
  void deserializationFailsWithOnlyYField() {
    String json = "{\"type\": \"move\", \"y\": 200}";

    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              mapper.readValue(json, MoveAction.class);
            });

    // Verify error message is descriptive
    String message = exception.getMessage().toLowerCase();
    assertTrue(
        message.contains("x") || message.contains("missing") || message.contains("required"),
        "Error message should indicate missing x field: " + exception.getMessage());
  }

  /**
   * Test that JSON with neither x nor y field throws descriptive error. Validates: Requirements 4.4
   */
  @Test
  void deserializationFailsWithNoCoordinateFields() {
    String json = "{\"type\": \"move\"}";

    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              mapper.readValue(json, MoveAction.class);
            });

    // Verify error message is descriptive
    String message = exception.getMessage().toLowerCase();
    assertTrue(
        message.contains("x")
            || message.contains("y")
            || message.contains("missing")
            || message.contains("required"),
        "Error message should indicate missing coordinate fields: " + exception.getMessage());
  }

  /**
   * Test that JSON with both x and y fields deserializes successfully. This is a positive test to
   * ensure the error cases are specific.
   */
  @Test
  void deserializationSucceedsWithBothFields() throws Exception {
    String json = "{\"type\": \"move\", \"x\": 100, \"y\": 200}";

    MoveAction action = mapper.readValue(json, MoveAction.class);

    assertNotNull(action);
    assertNotNull(action.coordinate());
    assertEquals(100, action.coordinate().x());
    assertEquals(200, action.coordinate().y());
  }
}
