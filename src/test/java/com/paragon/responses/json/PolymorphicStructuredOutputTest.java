package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Reasoning;
import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseOutput;
import com.paragon.responses.spec.TextConfigurationOptionsJsonSchemaFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class PolymorphicStructuredOutputTest {

  private JacksonJsonSchemaProducer producer;
  private ObjectMapper objectMapper;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Cat.class, name = "cat"),
    @JsonSubTypes.Type(value = Dog.class, name = "dog")
  })
  sealed interface Animal permits Cat, Dog {}

  record Cat(String name, int lives) implements Animal {}

  record Dog(String name, boolean good) implements Animal {}

  record Shelter(String id, Animal featured, List<Animal> animals) {}

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = SharedPayload.class, name = "alpha"),
    @JsonSubTypes.Type(value = SharedPayload.class, name = "beta")
  })
  sealed interface AliasUnion permits SharedPayload {}

  record SharedPayload(String name) implements AliasUnion {}

  record AliasWrapper(AliasUnion alias) {}

  @BeforeEach
  void setUp() {
    producer = new JacksonJsonSchemaProducer(new ObjectMapper());
    objectMapper = ResponsesApiObjectMapper.create();
  }

  @Test
  void rootPolymorphicSchemaUsesValueWrapper() {
    Map<String, Object> schema = producer.produce(Animal.class);

    assertEquals("object", schema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    assertTrue(properties.containsKey(StructuredOutputDefinition.ROOT_WRAPPER_PROPERTY));

    @SuppressWarnings("unchecked")
    Map<String, Object> wrapped =
        (Map<String, Object>) properties.get(StructuredOutputDefinition.ROOT_WRAPPER_PROPERTY);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> anyOf = (List<Map<String, Object>>) wrapped.get("anyOf");

    assertEquals(2, anyOf.size());
    assertEquals(List.of(StructuredOutputDefinition.ROOT_WRAPPER_PROPERTY), schema.get("required"));

    assertBranchIds(anyOf, "kind", "cat", "dog");
  }

  @Test
  void listRootSchemaUsesValueWrapper() {
    StructuredOutputDefinition<List<Animal>> definition =
        StructuredOutputDefinition.create(new TypeReference<List<Animal>>() {}, producer);
    Map<String, Object> schema = definition.schema();

    assertEquals("object", schema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> wrapped =
        (Map<String, Object>) properties.get(StructuredOutputDefinition.ROOT_WRAPPER_PROPERTY);

    assertEquals("array", wrapped.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> items = (Map<String, Object>) wrapped.get("items");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> anyOf = (List<Map<String, Object>>) items.get("anyOf");

    assertEquals(2, anyOf.size());
    assertBranchIds(anyOf, "kind", "cat", "dog");
  }

  @Test
  void nestedPolymorphicPropertiesUseAnyOf() {
    Map<String, Object> schema = producer.produce(Shelter.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> featured = (Map<String, Object>) properties.get("featured");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> featuredAnyOf = (List<Map<String, Object>>) featured.get("anyOf");

    assertEquals(2, featuredAnyOf.size());
    assertBranchIds(featuredAnyOf, "kind", "cat", "dog");

    @SuppressWarnings("unchecked")
    Map<String, Object> animals = (Map<String, Object>) properties.get("animals");
    @SuppressWarnings("unchecked")
    Map<String, Object> items = (Map<String, Object>) animals.get("items");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> itemAnyOf = (List<Map<String, Object>>) items.get("anyOf");

    assertEquals(2, itemAnyOf.size());
    assertBranchIds(itemAnyOf, "kind", "cat", "dog");
  }

  @Test
  void repeatedConcreteSubtypeAliasesRemainDistinct() {
    Map<String, Object> schema = producer.produce(AliasWrapper.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> alias = (Map<String, Object>) properties.get("alias");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> anyOf = (List<Map<String, Object>>) alias.get("anyOf");

    assertEquals(2, anyOf.size());
    assertBranchIds(anyOf, "type", "alpha", "beta");
  }

  @Test
  void structuredPayloadUsesWrappedSchemaForRootPolymorphicType() {
    CreateResponsePayload.Structured<ResponseOutput> payload =
        CreateResponsePayload.builder()
            .model("gpt-4o")
            .addUserMessage("Test")
            .withStructuredOutput(ResponseOutput.class)
            .build();

    assertTrue(payload.hasJsonSchemaTextFormat());

    TextConfigurationOptionsJsonSchemaFormat format =
        (TextConfigurationOptionsJsonSchemaFormat) payload.text().format();
    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) format.schema().get("properties");
    assertTrue(properties.containsKey(StructuredOutputDefinition.ROOT_WRAPPER_PROPERTY));
  }

  @Test
  void structuredOutputDefinitionParsesWrappedRoot() throws Exception {
    StructuredOutputDefinition<ResponseOutput> definition =
        StructuredOutputDefinition.create(ResponseOutput.class);

    ResponseOutput parsed =
        definition.parse(
            """
            {
              "value": {
                "type": "reasoning",
                "id": "reasoning_123",
                "summary": [],
                "status": "completed"
              }
            }
            """,
            objectMapper);

    assertInstanceOf(Reasoning.class, parsed);
  }

  @Test
  void structuredOutputDefinitionParsesWrappedListRoot() throws Exception {
    StructuredOutputDefinition<List<ResponseOutput>> definition =
        StructuredOutputDefinition.create(new TypeReference<List<ResponseOutput>>() {});

    List<ResponseOutput> parsed =
        definition.parse(
            """
            {
              "value": [
                {
                  "type": "reasoning",
                  "id": "reasoning_123",
                  "summary": [],
                  "status": "completed"
                }
              ]
            }
            """,
            objectMapper);

    assertEquals(1, parsed.size());
    assertInstanceOf(Reasoning.class, parsed.getFirst());
  }

  @Test
  void responseParseUnwrapsWrappedPolymorphicRoot() throws Exception {
    Response response =
        objectMapper.readValue(
            """
            {
              "id": "resp_1",
              "object": "response",
              "created_at": 1234567890,
              "status": "completed",
              "output": [
                {
                  "type": "message",
                  "id": "msg_1",
                  "status": "completed",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "{\\"value\\":{\\"type\\":\\"reasoning\\",\\"id\\":\\"reasoning_123\\",\\"summary\\":[],\\"status\\":\\"completed\\"}}"
                    }
                  ]
                }
              ],
              "model": "gpt-4o"
            }
            """,
            Response.class);

    var parsed = response.parse(ResponseOutput.class, objectMapper);

    assertInstanceOf(Reasoning.class, parsed.outputParsed());
  }

  private void assertBranchIds(
      List<Map<String, Object>> branches,
      String discriminatorField,
      String firstId,
      String secondId) {
    assertEquals(firstId, discriminatorValue(branches.get(0), discriminatorField));
    assertEquals(secondId, discriminatorValue(branches.get(1), discriminatorField));
  }

  @SuppressWarnings("unchecked")
  private String discriminatorValue(Map<String, Object> branch, String discriminatorField) {
    Map<String, Object> properties = (Map<String, Object>) branch.get("properties");
    Map<String, Object> discriminator = (Map<String, Object>) properties.get(discriminatorField);
    List<String> values = (List<String>) discriminator.get("enum");
    return values.getFirst();
  }
}
