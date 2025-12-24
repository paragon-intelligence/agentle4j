package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.AllowedToolsMode;
import com.paragon.responses.spec.MessageRole;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for enum deserialization error handling.
 *
 * <p>Validates: Requirements 3.4
 */
class EnumDeserializationTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  @Test
  void invalidEnumValueThrowsDescriptiveError() {
    // Test with an invalid MessageRole value
    String invalidJson = "\"invalid_role\"";

    JsonMappingException exception =
        assertThrows(
            JsonMappingException.class,
            () -> {
              mapper.readValue(invalidJson, MessageRole.class);
            });

    // Verify the error message is descriptive
    String message = exception.getMessage();
    assertTrue(
        message.contains("Invalid enum value"), "Error message should indicate invalid enum value");
    assertTrue(message.contains("invalid_role"), "Error message should include the invalid value");
    assertTrue(message.contains("MessageRole"), "Error message should include the enum type name");
    assertTrue(
        message.contains("developer") || message.contains("user") || message.contains("assistant"),
        "Error message should list valid values");
  }

  @Test
  void invalidEnumValueWithUnderscoresThrowsDescriptiveError() {
    // Test with an invalid AllowedToolsMode value
    String invalidJson = "\"invalid_mode\"";

    JsonMappingException exception =
        assertThrows(
            JsonMappingException.class,
            () -> {
              mapper.readValue(invalidJson, AllowedToolsMode.class);
            });

    // Verify the error message is descriptive
    String message = exception.getMessage();
    assertTrue(
        message.contains("Invalid enum value"), "Error message should indicate invalid enum value");
    assertTrue(message.contains("invalid_mode"), "Error message should include the invalid value");
    assertTrue(
        message.contains("AllowedToolsMode"), "Error message should include the enum type name");
    assertTrue(
        message.contains("auto") || message.contains("required"),
        "Error message should list valid values");
  }

  @Test
  void emptyStringReturnsNull() throws Exception {
    // Empty string should deserialize to null
    String emptyJson = "\"\"";

    MessageRole result = mapper.readValue(emptyJson, MessageRole.class);

    assertNull(result, "Empty string should deserialize to null");
  }

  @Test
  void validLowercaseEnumDeserializesCorrectly() throws Exception {
    // Test valid lowercase values
    assertEquals(MessageRole.USER, mapper.readValue("\"user\"", MessageRole.class));
    assertEquals(MessageRole.ASSISTANT, mapper.readValue("\"assistant\"", MessageRole.class));
    assertEquals(MessageRole.DEVELOPER, mapper.readValue("\"developer\"", MessageRole.class));
  }

  @Test
  void validLowercaseEnumWithUnderscoresDeserializesCorrectly() throws Exception {
    // Test valid lowercase values with underscores
    assertEquals(AllowedToolsMode.AUTO, mapper.readValue("\"auto\"", AllowedToolsMode.class));
    assertEquals(
        AllowedToolsMode.REQUIRED, mapper.readValue("\"required\"", AllowedToolsMode.class));
  }
}
