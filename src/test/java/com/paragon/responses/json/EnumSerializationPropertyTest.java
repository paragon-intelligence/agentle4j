package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.AllowedToolsMode;
import com.paragon.responses.spec.ImageDetail;
import com.paragon.responses.spec.MessageRole;
import net.jqwik.api.*;

/**
 * Property-based tests for enum serialization.
 *
 * <p>Feature: responses-api-jackson-serialization Property 6: Enum lowercase serialization
 * Validates: Requirements 3.1, 3.3
 */
class EnumSerializationPropertyTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  /**
   * Property 6: Enum lowercase serialization
   *
   * <p>For any object containing enum fields, when serialized to JSON, all enum values should
   * appear as lowercase strings (with underscores preserved for multi-word enums).
   */
  @Property(tries = 100)
  void enumValuesSerializeToLowercase(@ForAll("messageRoles") MessageRole role) throws Exception {
    // Serialize the enum
    String json = mapper.writeValueAsString(role);

    // Remove quotes from JSON string
    String value = json.replace("\"", "");

    // Verify it's lowercase
    assertEquals(value, value.toLowerCase(), "Enum value should be lowercase");

    // Verify it matches the enum name in lowercase
    assertEquals(
        role.name().toLowerCase(), value, "Serialized value should match enum name in lowercase");
  }

  @Property(tries = 100)
  void enumValuesWithMultipleWordsPreserveUnderscores(
      @ForAll("allowedToolsModes") AllowedToolsMode mode) throws Exception {
    // Serialize the enum
    String json = mapper.writeValueAsString(mode);

    // Remove quotes
    String value = json.replace("\"", "");

    // Verify it's lowercase
    assertEquals(value, value.toLowerCase(), "Enum value should be lowercase");

    // Verify underscores are preserved
    assertEquals(mode.name().toLowerCase(), value, "Underscores should be preserved in lowercase");
  }

  @Property(tries = 100)
  void deserializedEnumsMatchOriginal(@ForAll("messageRoles") MessageRole original)
      throws Exception {
    // Serialize
    String json = mapper.writeValueAsString(original);

    // Deserialize
    MessageRole deserialized = mapper.readValue(json, MessageRole.class);

    // Verify round-trip
    assertEquals(original, deserialized, "Deserialized enum should match original");
  }

  @Property(tries = 100)
  void allEnumTypesSerializeToLowercase(@ForAll("imageDetails") ImageDetail detail)
      throws Exception {
    // Serialize
    String json = mapper.writeValueAsString(detail);

    // Remove quotes
    String value = json.replace("\"", "");

    // Verify lowercase
    assertEquals(
        detail.name().toLowerCase(), value, "All enum types should serialize to lowercase");
  }

  // Arbitraries (generators) for enum types

  @Provide
  Arbitrary<MessageRole> messageRoles() {
    return Arbitraries.of(MessageRole.values());
  }

  @Provide
  Arbitrary<AllowedToolsMode> allowedToolsModes() {
    return Arbitraries.of(AllowedToolsMode.values());
  }

  @Provide
  Arbitrary<ImageDetail> imageDetails() {
    return Arbitraries.of(ImageDetail.values());
  }
}
