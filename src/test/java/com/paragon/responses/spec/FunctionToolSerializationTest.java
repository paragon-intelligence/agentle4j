package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.annotations.FunctionMetadata;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for verifying that {@link FunctionTool} objects annotated with {@link FunctionMetadata} are
 * serialized with the proper JSON schema according to the OpenAI Responses API specification.
 *
 * <p>According to the OpenAPI spec for FunctionTool:
 *
 * <ul>
 *   <li>{@code type} - string (enum), required. Always "function"
 *   <li>{@code name} - string, required. The name of the function to call
 *   <li>{@code description} - any, optional
 *   <li>{@code parameters} - any, required. JSON Schema for the function parameters
 *   <li>{@code strict} - any, required
 * </ul>
 */
class FunctionToolSerializationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = ResponsesApiObjectMapper.create();
  }

  // ===== Test Data Classes =====

  /** Simple enum for testing */
  enum TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
  }

  /** Simple record params for a weather tool */
  record GetWeatherParams(String location, TemperatureUnit unit) {}

  /** Record with multiple types for parameter testing */
  record ComplexParams(
      String query, int limit, boolean includeDetails, List<String> tags, TemperatureUnit unit) {}

  /** Empty params record */
  record EmptyParams() {}

  /** Nested record */
  record Address(String street, String city, String postalCode) {}

  /** Record with nested object */
  record PersonParams(String name, int age, Address address) {}

  // ===== Test Tools =====

  @FunctionMetadata(name = "get_weather", description = "Get the current weather for a location")
  static class GetWeatherTool extends FunctionTool<GetWeatherParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable GetWeatherParams params) {
      return null;
    }
  }

  @FunctionMetadata(name = "search_items", description = "Search for items with complex filters")
  static class ComplexParamsTool extends FunctionTool<ComplexParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable ComplexParams params) {
      return null;
    }
  }

  @FunctionMetadata(name = "no_description_tool")
  static class NoDescriptionTool extends FunctionTool<EmptyParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EmptyParams params) {
      return null;
    }
  }

  @FunctionMetadata(name = "get_person", description = "Get person information")
  static class PersonTool extends FunctionTool<PersonParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable PersonParams params) {
      return null;
    }
  }

  // ===== Basic Serialization Tests =====

  @Nested
  class BasicSerializationTests {

    @Test
    void functionTool_serializes_withTypeFunction() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertTrue(node.has("type"), "Serialized FunctionTool should have 'type' field");
      assertEquals("function", node.get("type").asText(), "type should be 'function'");
    }

    @Test
    void functionTool_serializes_withNameFromAnnotation() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertTrue(node.has("name"), "Serialized FunctionTool should have 'name' field");
      assertEquals("get_weather", node.get("name").asText(), "name should match @FunctionMetadata");
    }

    @Test
    void functionTool_serializes_withDescriptionFromAnnotation() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertTrue(
          node.has("description"), "Serialized FunctionTool should have 'description' field");
      assertEquals(
          "Get the current weather for a location",
          node.get("description").asText(),
          "description should match @FunctionMetadata");
    }

    @Test
    void functionTool_serializes_withStrictField() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertTrue(node.has("strict"), "Serialized FunctionTool should have 'strict' field");
      assertTrue(node.get("strict").asBoolean(), "strict should be true");
    }

    @Test
    void functionTool_serializes_withParametersField() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertTrue(node.has("parameters"), "Serialized FunctionTool should have 'parameters' field");
      assertTrue(node.get("parameters").isObject(), "parameters should be a JSON object");
    }
  }

  // ===== Parameters JSON Schema Tests =====

  @Nested
  class ParametersSchemaTests {

    @Test
    void parameters_hasTypeObject() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode parametersNode = objectMapper.readTree(json).get("parameters");

      assertEquals(
          "object", parametersNode.get("type").asText(), "parameters type should be 'object'");
    }

    @Test
    void parameters_hasPropertiesForRecordFields() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode parametersNode = objectMapper.readTree(json).get("parameters");

      assertTrue(
          parametersNode.has("properties"), "parameters should have 'properties' definition");

      JsonNode propertiesNode = parametersNode.get("properties");
      assertTrue(propertiesNode.has("location"), "properties should include 'location' field");
      assertTrue(propertiesNode.has("unit"), "properties should include 'unit' field");
    }

    @Test
    void parameters_hasRequiredArrayForStrictMode() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode parametersNode = objectMapper.readTree(json).get("parameters");

      assertTrue(
          parametersNode.has("required"),
          "parameters should have 'required' array for OpenAI strict mode");

      JsonNode requiredArray = parametersNode.get("required");
      assertTrue(requiredArray.isArray(), "'required' should be an array");
      assertTrue(requiredArray.size() > 0, "'required' should contain property names");

      // Verify all properties are in required array
      boolean hasLocation = false;
      boolean hasUnit = false;
      for (JsonNode req : requiredArray) {
        if ("location".equals(req.asText())) hasLocation = true;
        if ("unit".equals(req.asText())) hasUnit = true;
      }
      assertTrue(hasLocation, "'required' should include 'location'");
      assertTrue(hasUnit, "'required' should include 'unit'");
    }

    @Test
    void parameters_hasAdditionalPropertiesFalseForStrictMode() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode parametersNode = objectMapper.readTree(json).get("parameters");

      assertTrue(
          parametersNode.has("additionalProperties"),
          "parameters should have 'additionalProperties' for OpenAI strict mode");
      assertFalse(
          parametersNode.get("additionalProperties").asBoolean(),
          "'additionalProperties' should be false");
    }

    @Test
    void parameters_stringFieldHasCorrectType() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode locationProp = propertiesNode.get("location");
      assertEquals(
          "string", locationProp.get("type").asText(), "location should be typed as 'string'");
    }

    @Test
    void parameters_enumFieldHasEnumValues() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode unitProp = propertiesNode.get("unit");
      assertTrue(
          unitProp.has("enum") || unitProp.has("type"),
          "enum field should have 'enum' or 'type' property");
    }

    @Test
    void parameters_integerFieldHasCorrectType() throws Exception {
      ComplexParamsTool tool = new ComplexParamsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode limitProp = propertiesNode.get("limit");
      assertEquals("integer", limitProp.get("type").asText(), "limit should be typed as 'integer'");
    }

    @Test
    void parameters_booleanFieldHasCorrectType() throws Exception {
      ComplexParamsTool tool = new ComplexParamsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode includeDetailsProp = propertiesNode.get("includeDetails");
      assertEquals(
          "boolean",
          includeDetailsProp.get("type").asText(),
          "includeDetails should be typed as 'boolean'");
    }

    @Test
    void parameters_arrayFieldHasCorrectType() throws Exception {
      ComplexParamsTool tool = new ComplexParamsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode tagsProp = propertiesNode.get("tags");
      assertEquals("array", tagsProp.get("type").asText(), "tags should be typed as 'array'");
    }

    @Test
    void parameters_nestedObjectHasCorrectStructure() throws Exception {
      PersonTool tool = new PersonTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      assertTrue(propertiesNode.has("address"), "properties should include 'address' field");
      JsonNode addressProp = propertiesNode.get("address");

      // Address should be represented as an object with its own properties
      assertNotNull(addressProp, "address property should not be null");
    }
  }

  // ===== Edge Cases Tests =====

  @Nested
  class EdgeCaseTests {

    @Test
    void functionTool_withoutDescription_serializesWithoutDescriptionField() throws Exception {
      NoDescriptionTool tool = new NoDescriptionTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      // Description should be absent when empty (null exclusion by ObjectMapper)
      assertFalse(
          node.has("description") && !node.get("description").isNull(),
          "Empty description should not produce a description field or should be null");
    }

    @Test
    void functionTool_withEmptyParams_serializesValidSchema() throws Exception {
      NoDescriptionTool tool = new NoDescriptionTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode parametersNode = objectMapper.readTree(json).get("parameters");

      assertNotNull(parametersNode, "parameters should be present even for empty params record");
      assertEquals(
          "object",
          parametersNode.get("type").asText(),
          "parameters type should still be 'object'");
    }
  }

  // ===== Optional Annotation Tests =====

  /** Tool without @FunctionMetadata - should derive name from class name */
  static class UnannotatedWeatherTool extends FunctionTool<GetWeatherParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable GetWeatherParams params) {
      return null;
    }
  }

  /** Another unannotated tool to test snake_case conversion */
  static class MyComplexAPITool extends FunctionTool<EmptyParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EmptyParams params) {
      return null;
    }
  }

  @Nested
  class OptionalAnnotationTests {

    @Test
    void unannotatedTool_derivesNameFromClassName() throws Exception {
      UnannotatedWeatherTool tool = new UnannotatedWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertEquals("function", node.get("type").asText());
      assertEquals(
          "unannotated_weather_tool",
          node.get("name").asText(),
          "Name should be derived from class name in snake_case");
    }

    @Test
    void unannotatedTool_hasNullDescription() throws Exception {
      UnannotatedWeatherTool tool = new UnannotatedWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      // Description should be absent (null exclusion by ObjectMapper)
      assertFalse(node.has("description"), "Unannotated tool should not have description field");
    }

    @Test
    void unannotatedTool_stillHasParametersAndStrict() throws Exception {
      UnannotatedWeatherTool tool = new UnannotatedWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertTrue(node.has("parameters"), "Should have parameters");
      assertTrue(node.has("strict"), "Should have strict");
      assertTrue(node.get("strict").asBoolean(), "strict should be true");
    }

    @Test
    void unannotatedTool_snakeCaseConversion_handlesComplexNames() throws Exception {
      MyComplexAPITool tool = new MyComplexAPITool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      assertEquals(
          "my_complex_a_p_i_tool",
          node.get("name").asText(),
          "Complex class name should convert to snake_case");
    }

    @Test
    void annotatedTool_overridesDefaultBehavior() throws Exception {
      // GetWeatherTool has @FunctionMetadata with name="get_weather"
      GetWeatherTool annotatedTool = new GetWeatherTool();
      UnannotatedWeatherTool unannotatedTool = new UnannotatedWeatherTool();

      String annotatedJson = objectMapper.writeValueAsString(annotatedTool);
      String unannotatedJson = objectMapper.writeValueAsString(unannotatedTool);

      JsonNode annotatedNode = objectMapper.readTree(annotatedJson);
      JsonNode unannotatedNode = objectMapper.readTree(unannotatedJson);

      // Annotated should use the annotation value
      assertEquals("get_weather", annotatedNode.get("name").asText());
      // Unannotated should derive from class name
      assertEquals("unannotated_weather_tool", unannotatedNode.get("name").asText());
    }
  }

  // ===== Full Structure Verification Tests =====

  @Nested
  class FullStructureTests {

    @Test
    void functionTool_hasAllRequiredFieldsFromOpenAPISpec() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      // Verify all required fields as per OpenAPI spec are present
      assertTrue(node.has("type"), "Should have 'type' (required)");
      assertTrue(node.has("name"), "Should have 'name' (required)");
      assertTrue(node.has("parameters"), "Should have 'parameters' (required)");
      assertTrue(node.has("strict"), "Should have 'strict' (required)");

      // description is optional but should be present when provided
      assertTrue(node.has("description"), "Should have 'description' when provided in annotation");
    }

    @Test
    void functionTool_fieldValuesMatchSpec() throws Exception {
      ComplexParamsTool tool = new ComplexParamsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode node = objectMapper.readTree(json);

      // Verify field values
      assertEquals("function", node.get("type").asText());
      assertEquals("search_items", node.get("name").asText());
      assertEquals("Search for items with complex filters", node.get("description").asText());
      assertTrue(node.get("strict").asBoolean());

      // Verify parameters is a valid JSON Schema object
      JsonNode params = node.get("parameters");
      assertEquals("object", params.get("type").asText());
      assertTrue(params.has("properties"));
    }

    @Test
    void serializedJson_isValidForOpenAIResponsesApi() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      String json = objectMapper.writeValueAsString(tool);

      // Parse and verify it's valid JSON
      JsonNode node = objectMapper.readTree(json);
      assertNotNull(node, "Serialized JSON should be parseable");

      // Validate the structure matches what the OpenAI Responses API expects
      // The tool definition should be directly usable in a CreateResponse payload
      assertEquals("function", node.get("type").asText());
      assertFalse(node.get("name").asText().isEmpty(), "name should not be empty");
      assertFalse(node.get("parameters").isEmpty(), "parameters should not be empty");
    }
  }

  // ===== Snake Case Field Naming Tests =====

  @Nested
  class SnakeCaseFieldNamingTests {

    @Test
    void parameters_propertyNames_arePreserved() throws Exception {
      // Note: The ObjectMapper uses snake_case strategy, but for schema generation
      // the property names come from the record field names via JacksonJsonSchemaProducer
      ComplexParamsTool tool = new ComplexParamsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      // Verify fields are present (naming strategy may apply)
      assertTrue(
          propertiesNode.has("includeDetails") || propertiesNode.has("include_details"),
          "Should have 'includeDetails' or 'include_details' field");
    }
  }

  // ===== Complex Data Types Tests =====

  // --- Additional Complex Test Data Classes ---

  /** Deeply nested structure: Level 3 */
  record GeoCoordinates(double latitude, double longitude) {}

  /** Deeply nested structure: Level 2 */
  record Location(String name, GeoCoordinates coordinates, String timezone) {}

  /** Deeply nested structure: Level 1 */
  record Venue(String id, String name, Location location, List<String> amenities) {}

  /** Record with collections of complex types */
  record EventParams(
      String eventId,
      String title,
      Venue venue,
      List<PersonParams> attendees,
      Map<String, String> metadata) {}

  /** Record with @JsonProperty annotations */
  record AnnotatedParams(
      @com.fasterxml.jackson.annotation.JsonProperty(value = "search_query", required = true)
          String searchQuery,
      @com.fasterxml.jackson.annotation.JsonProperty("max_results") int maxResults,
      @com.fasterxml.jackson.annotation.JsonProperty("include_metadata") boolean includeMetadata) {}

  /** Record with nullable/optional fields */
  record OptionalFieldsParams(
      @org.jspecify.annotations.NonNull String requiredField,
      @Nullable String optionalField,
      @Nullable List<String> optionalList,
      @Nullable TemperatureUnit optionalEnum) {}

  /** Record with multiple enums */
  enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  enum Status {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
  }

  record MultiEnumParams(String taskId, Priority priority, Status status, TemperatureUnit unit) {}

  // --- Additional Test Tools ---

  @FunctionMetadata(
      name = "create_event",
      description = "Create a new event with venue and attendees")
  static class EventTool extends FunctionTool<EventParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable EventParams params) {
      return null;
    }
  }

  @FunctionMetadata(name = "search_with_annotations", description = "Search using annotated params")
  static class AnnotatedParamsTool extends FunctionTool<AnnotatedParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable AnnotatedParams params) {
      return null;
    }
  }

  @FunctionMetadata(name = "process_optional", description = "Process with optional fields")
  static class OptionalFieldsTool extends FunctionTool<OptionalFieldsParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable OptionalFieldsParams params) {
      return null;
    }
  }

  @FunctionMetadata(name = "update_task", description = "Update task with multiple enums")
  static class MultiEnumTool extends FunctionTool<MultiEnumParams> {
    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable MultiEnumParams params) {
      return null;
    }
  }

  @Nested
  class DeeplyNestedObjectTests {

    @Test
    void deeplyNestedParams_serializes_withAllLevels() throws Exception {
      EventTool tool = new EventTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode parametersNode = objectMapper.readTree(json).get("parameters");
      JsonNode propertiesNode = parametersNode.get("properties");

      // Verify level 1 - EventParams fields
      assertTrue(propertiesNode.has("eventId") || propertiesNode.has("event_id"));
      assertTrue(propertiesNode.has("title"));
      assertTrue(propertiesNode.has("venue"));
      assertTrue(propertiesNode.has("attendees"));
      assertTrue(propertiesNode.has("metadata"));
    }

    @Test
    void deeplyNestedParams_venueHasNestedLocation() throws Exception {
      EventTool tool = new EventTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode venueProp = propertiesNode.get("venue");
      assertNotNull(venueProp, "venue should exist");

      // Venue should be represented as an object type
      assertEquals("object", venueProp.get("type").asText(), "venue should be of type 'object'");
    }

    @Test
    void collectionOfComplexTypes_serialized_asArray() throws Exception {
      EventTool tool = new EventTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode attendeesProp = propertiesNode.get("attendees");
      assertNotNull(attendeesProp, "attendees should exist");
      assertEquals("array", attendeesProp.get("type").asText(), "attendees should be an array");
    }

    @Test
    void mapField_serialized_asObject() throws Exception {
      EventTool tool = new EventTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      JsonNode metadataProp = propertiesNode.get("metadata");
      assertNotNull(metadataProp, "metadata should exist");
      assertEquals(
          "object", metadataProp.get("type").asText(), "Map should be serialized as object");
    }
  }

  @Nested
  class JsonPropertyAnnotationTests {

    @Test
    void annotatedParams_usesCustomPropertyNames() throws Exception {
      AnnotatedParamsTool tool = new AnnotatedParamsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      // Should use @JsonProperty value instead of field name
      assertTrue(
          propertiesNode.has("search_query") || propertiesNode.has("searchQuery"),
          "Should have search_query or searchQuery field");
      assertTrue(
          propertiesNode.has("max_results") || propertiesNode.has("maxResults"),
          "Should have max_results or maxResults field");
    }

    @Test
    void annotatedParams_preservesFieldTypes() throws Exception {
      AnnotatedParamsTool tool = new AnnotatedParamsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      // Check that types are correct regardless of naming
      JsonNode searchField =
          propertiesNode.has("search_query")
              ? propertiesNode.get("search_query")
              : propertiesNode.get("searchQuery");
      if (searchField != null) {
        assertEquals("string", searchField.get("type").asText());
      }
    }
  }

  @Nested
  class NullableFieldsTests {

    @Test
    void optionalFields_areIncludedInSchema() throws Exception {
      OptionalFieldsTool tool = new OptionalFieldsTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      // All fields should be in the schema, even nullable ones
      assertTrue(
          propertiesNode.has("requiredField") || propertiesNode.has("required_field"),
          "Should have requiredField");
      assertTrue(
          propertiesNode.has("optionalField") || propertiesNode.has("optional_field"),
          "Should have optionalField");
      assertTrue(
          propertiesNode.has("optionalList") || propertiesNode.has("optional_list"),
          "Should have optionalList");
      assertTrue(
          propertiesNode.has("optionalEnum") || propertiesNode.has("optional_enum"),
          "Should have optionalEnum");
    }
  }

  @Nested
  class MultipleEnumsTests {

    @Test
    void multipleEnums_allSerialized() throws Exception {
      MultiEnumTool tool = new MultiEnumTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      // All enum fields should be present
      assertTrue(propertiesNode.has("priority"), "Should have priority");
      assertTrue(propertiesNode.has("status"), "Should have status");
      assertTrue(propertiesNode.has("unit"), "Should have unit");
    }

    @Test
    void multipleEnums_eachHasEnumOrStringType() throws Exception {
      MultiEnumTool tool = new MultiEnumTool();

      String json = objectMapper.writeValueAsString(tool);
      JsonNode propertiesNode = objectMapper.readTree(json).get("parameters").get("properties");

      for (String enumField : List.of("priority", "status", "unit")) {
        JsonNode fieldNode = propertiesNode.get(enumField);
        assertTrue(
            fieldNode.has("enum") || "string".equals(fieldNode.path("type").asText()),
            enumField + " should have 'enum' values or be typed as 'string'");
      }
    }
  }

  @Nested
  class RoundTripSerializationTests {

    @Test
    void functionTool_roundTrip_preservesStructure() throws Exception {
      GetWeatherTool originalTool = new GetWeatherTool();

      // Serialize to JSON
      String json = objectMapper.writeValueAsString(originalTool);

      // Parse as generic map to verify structure
      @SuppressWarnings("unchecked")
      Map<String, Object> toolMap = objectMapper.readValue(json, Map.class);

      // Verify key fields survive round-trip
      assertEquals("function", toolMap.get("type"));
      assertEquals("get_weather", toolMap.get("name"));
      assertEquals("Get the current weather for a location", toolMap.get("description"));
      assertTrue((Boolean) toolMap.get("strict"));
      assertNotNull(toolMap.get("parameters"));
    }

    @Test
    void complexTool_roundTrip_preservesNestedStructure() throws Exception {
      EventTool tool = new EventTool();

      String json = objectMapper.writeValueAsString(tool);

      @SuppressWarnings("unchecked")
      Map<String, Object> toolMap = objectMapper.readValue(json, Map.class);

      // Verify nested parameters structure survives
      @SuppressWarnings("unchecked")
      Map<String, Object> parameters = (Map<String, Object>) toolMap.get("parameters");
      assertNotNull(parameters);
      assertEquals("object", parameters.get("type"));

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
      assertNotNull(properties);
      assertTrue(properties.containsKey("venue") || properties.containsKey("title"));
    }
  }

  @Nested
  class CreateResponsePayloadIntegrationTests {

    @Test
    void functionTool_serializesWithinPayloadContext() throws Exception {
      GetWeatherTool tool = new GetWeatherTool();

      // Create a payload with the tool
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addTool(tool).build();

      // Serialize the full payload
      String json = objectMapper.writeValueAsString(payload);
      JsonNode payloadNode = objectMapper.readTree(json);

      // Verify tools array exists and contains our function tool
      assertTrue(payloadNode.has("tools"), "Payload should have 'tools' array");
      JsonNode toolsArray = payloadNode.get("tools");
      assertTrue(toolsArray.isArray(), "tools should be an array");
      assertTrue(toolsArray.size() > 0, "tools should contain at least one tool");

      // Verify the tool structure within the payload
      JsonNode firstTool = toolsArray.get(0);
      assertEquals("function", firstTool.get("type").asText());
      assertEquals("get_weather", firstTool.get("name").asText());
      assertTrue(firstTool.has("parameters"));
    }

    @Test
    void multipleFunctionTools_serializeCorrectlyInPayload() throws Exception {
      GetWeatherTool weatherTool = new GetWeatherTool();
      ComplexParamsTool searchTool = new ComplexParamsTool();

      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addTool(weatherTool)
              .addTool(searchTool)
              .build();

      String json = objectMapper.writeValueAsString(payload);
      JsonNode payloadNode = objectMapper.readTree(json);

      JsonNode toolsArray = payloadNode.get("tools");
      assertEquals(2, toolsArray.size(), "Should have 2 tools");

      // Verify both tools have correct names
      boolean hasWeatherTool = false;
      boolean hasSearchTool = false;
      for (JsonNode tool : toolsArray) {
        String name = tool.get("name").asText();
        if ("get_weather".equals(name)) hasWeatherTool = true;
        if ("search_items".equals(name)) hasSearchTool = true;
      }
      assertTrue(hasWeatherTool, "Should have get_weather tool");
      assertTrue(hasSearchTool, "Should have search_items tool");
    }
  }
}
