package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests verifying that {@link JacksonJsonSchemaProducer} generates schemas fully compliant with
 * OpenAI strict mode requirements:
 *
 * <ul>
 *   <li>Every object at every nesting level must have {@code "additionalProperties": false}
 *   <li>Every object at every nesting level must have a {@code "required"} array listing ALL its
 *       properties
 *   <li>No {@code $ref} references anywhere in the schema (must be fully inlined)
 *   <li>No unsupported keywords: {@code id}, {@code $schema}
 * </ul>
 *
 * <p>These tests are meant to expose real bugs, not just verify happy-path schema structure.
 */
class OpenAiStrictModeComplianceTest {

  private JacksonJsonSchemaProducer producer;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    producer = new JacksonJsonSchemaProducer(mapper);
  }

  // ===== Test Data =====

  record Address(String street, String city, String country) {}

  record ContactInfo(String email, String phone, Address primaryAddress, Address secondaryAddress) {}

  record Person(String id, String firstName, String lastName, ContactInfo contactInfo) {}

  record Team(String name, Person lead, List<Person> members) {}

  // ===== Helpers =====

  /** Recursively collects all schema nodes that have type "object". */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> collectAllObjectNodes(Map<String, Object> schema) {
    List<Map<String, Object>> result = new ArrayList<>();
    collectObjectNodesRecursive(schema, result);
    return result;
  }

  @SuppressWarnings("unchecked")
  private void collectObjectNodesRecursive(Object node, List<Map<String, Object>> accumulator) {
    if (!(node instanceof Map)) return;
    Map<String, Object> map = (Map<String, Object>) node;

    if ("object".equals(map.get("type")) && map.containsKey("properties")) {
      accumulator.add(map);
    }

    // Recurse into all map values
    for (Object value : map.values()) {
      if (value instanceof Map) {
        collectObjectNodesRecursive(value, accumulator);
      } else if (value instanceof List<?> list) {
        for (Object item : list) {
          collectObjectNodesRecursive(item, accumulator);
        }
      }
    }
  }

  /** Returns true if any node in the schema tree contains the given key. */
  @SuppressWarnings("unchecked")
  private boolean schemaTreeContainsKey(Object node, String key) {
    if (!(node instanceof Map)) return false;
    Map<String, Object> map = (Map<String, Object>) node;
    if (map.containsKey(key)) return true;
    for (Object value : map.values()) {
      if (value instanceof Map && schemaTreeContainsKey(value, key)) return true;
      if (value instanceof List<?> list) {
        for (Object item : list) {
          if (schemaTreeContainsKey(item, key)) return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if any schema node in the tree has an {@code "id"} field whose value is a
   * Jackson URN (starts with {@code "urn:"}). This distinguishes Jackson metadata from a
   * legitimate data field named {@code id}.
   */
  @SuppressWarnings("unchecked")
  private boolean schemaTreeContainsUrnId(Object node) {
    if (!(node instanceof Map)) return false;
    Map<String, Object> map = (Map<String, Object>) node;
    if (map.get("id") instanceof String v && v.startsWith("urn:")) return true;
    for (Object value : map.values()) {
      if (value instanceof Map && schemaTreeContainsUrnId(value)) return true;
      if (value instanceof List<?> list) {
        for (Object item : list) {
          if (schemaTreeContainsUrnId(item)) return true;
        }
      }
    }
    return false;
  }

  // ===== Tests: additionalProperties =====

  @Nested
  class AdditionalPropertiesFalse {

    @Test
    void rootObjectHasAdditionalPropertiesFalse() {
      Map<String, Object> schema = producer.produce(Address.class);
      assertEquals(
          false,
          schema.get("additionalProperties"),
          "Root object must have additionalProperties: false for OpenAI strict mode");
    }

    @Test
    void nestedObjectHasAdditionalPropertiesFalse() {
      // ContactInfo contains Address — the Address schema node must also have additionalProperties: false
      Map<String, Object> schema = producer.produce(ContactInfo.class);

      List<Map<String, Object>> allObjects = collectAllObjectNodes(schema);
      assertTrue(allObjects.size() >= 2, "Should find at least 2 object nodes (ContactInfo + Address)");

      for (Map<String, Object> objectNode : allObjects) {
        assertEquals(
            false,
            objectNode.get("additionalProperties"),
            "Every nested object must have additionalProperties: false. Failing node keys: "
                + objectNode.keySet());
      }
    }

    @Test
    void deeplyNestedObjectsAllHaveAdditionalPropertiesFalse() {
      // Person -> ContactInfo -> Address (3 levels)
      Map<String, Object> schema = producer.produce(Person.class);

      List<Map<String, Object>> allObjects = collectAllObjectNodes(schema);
      assertTrue(allObjects.size() >= 3, "Should find at least 3 object nodes (Person, ContactInfo, Address)");

      for (Map<String, Object> objectNode : allObjects) {
        assertEquals(
            false,
            objectNode.get("additionalProperties"),
            "Every object at every depth must have additionalProperties: false. Failing node keys: "
                + objectNode.keySet());
      }
    }

    @Test
    void arrayItemObjectHasAdditionalPropertiesFalse() {
      // Team has List<Person> — the items schema for Person must have additionalProperties: false
      Map<String, Object> schema = producer.produce(Team.class);

      List<Map<String, Object>> allObjects = collectAllObjectNodes(schema);
      // Team, Person (as lead), Person (as items in members), ContactInfo, Address
      assertTrue(allObjects.size() >= 2, "Should find object nodes including array item objects");

      for (Map<String, Object> objectNode : allObjects) {
        assertEquals(
            false,
            objectNode.get("additionalProperties"),
            "Array item objects must also have additionalProperties: false. Failing node keys: "
                + objectNode.keySet());
      }
    }
  }

  // ===== Tests: required array =====

  @Nested
  class RequiredArrayContainsAllProperties {

    @Test
    void rootObjectRequiredContainsAllProperties() {
      Map<String, Object> schema = producer.produce(Address.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");
      @SuppressWarnings("unchecked")
      List<String> required = (List<String>) schema.get("required");

      assertNotNull(required, "Root object must have a 'required' array");
      assertTrue(
          required.containsAll(props.keySet()),
          "Root 'required' must list all properties. Missing: "
              + props.keySet().stream().filter(k -> !required.contains(k)).toList());
    }

    @Test
    void nestedObjectRequiredContainsAllProperties() {
      Map<String, Object> schema = producer.produce(ContactInfo.class);

      List<Map<String, Object>> allObjects = collectAllObjectNodes(schema);
      assertTrue(allObjects.size() >= 2, "Should find at least 2 object nodes");

      for (Map<String, Object> objectNode : allObjects) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) objectNode.get("properties");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) objectNode.get("required");

        assertNotNull(
            required,
            "Every nested object must have a 'required' array. Missing from node with keys: "
                + objectNode.keySet());
        assertTrue(
            required.containsAll(props.keySet()),
            "Nested 'required' must list all properties. Node properties: "
                + props.keySet()
                + ", required: "
                + required);
      }
    }

    @Test
    void deeplyNestedRequiredContainsAllProperties() {
      Map<String, Object> schema = producer.produce(Person.class);

      List<Map<String, Object>> allObjects = collectAllObjectNodes(schema);
      assertTrue(allObjects.size() >= 3, "Should find Person, ContactInfo and Address objects");

      for (Map<String, Object> objectNode : allObjects) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) objectNode.get("properties");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) objectNode.get("required");

        assertNotNull(required, "Every deeply nested object must have 'required'. Node: " + objectNode.keySet());
        assertTrue(
            required.containsAll(props.keySet()),
            "Deeply nested 'required' must list ALL properties. Node: "
                + props.keySet()
                + " vs required: "
                + required);
      }
    }

    @Test
    void arrayItemObjectRequiredContainsAllProperties() {
      Map<String, Object> schema = producer.produce(Team.class);

      List<Map<String, Object>> allObjects = collectAllObjectNodes(schema);

      for (Map<String, Object> objectNode : allObjects) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) objectNode.get("properties");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) objectNode.get("required");

        assertNotNull(required, "Array item objects must also have 'required'. Node: " + objectNode.keySet());
        assertTrue(
            required.containsAll(props.keySet()),
            "Array item 'required' must list all properties. Props: "
                + props.keySet()
                + " vs required: "
                + required);
      }
    }
  }

  // ===== Tests: no $ref =====

  @Nested
  class NoRefReferences {

    @Test
    void simpleSchemaHasNoRef() {
      Map<String, Object> schema = producer.produce(Address.class);
      assertFalse(
          schemaTreeContainsKey(schema, "$ref"),
          "Schema must not contain $ref — OpenAI strict mode does not support $ref");
    }

    @Test
    void nestedSchemaHasNoRef() {
      Map<String, Object> schema = producer.produce(ContactInfo.class);
      assertFalse(
          schemaTreeContainsKey(schema, "$ref"),
          "Nested schema must not contain $ref — all types must be inlined");
    }

    @Test
    void deeplyNestedSchemaHasNoRef() {
      Map<String, Object> schema = producer.produce(Person.class);
      assertFalse(
          schemaTreeContainsKey(schema, "$ref"),
          "Deeply nested schema must not contain $ref");
    }

    @Test
    void schemaWithRepeatedTypesHasNoRef() {
      // ContactInfo uses Address twice (primaryAddress + secondaryAddress)
      // Jackson may use $ref to avoid repeating the Address schema
      Map<String, Object> schema = producer.produce(ContactInfo.class);
      assertFalse(
          schemaTreeContainsKey(schema, "$ref"),
          "Schema with repeated types must inline them instead of using $ref");
    }
  }

  // ===== Tests: no unsupported keywords =====

  @Nested
  class NoUnsupportedKeywords {

    @Test
    void schemaHasNoUrnIdField() {
      // jackson-module-jsonSchema adds "id": "urn:jsonschema:..." to each object — OpenAI rejects this
      Map<String, Object> schema = producer.produce(Address.class);
      assertFalse(
          schemaTreeContainsUrnId(schema),
          "Schema must not contain 'id': 'urn:...' metadata fields — not supported by OpenAI strict mode");
    }

    @Test
    void nestedSchemaHasNoUrnIdField() {
      // Person has a data field named "id" (String) — that must stay; URN metadata must be gone
      Map<String, Object> schema = producer.produce(Person.class);
      assertFalse(
          schemaTreeContainsUrnId(schema),
          "Nested schema objects must not contain 'id': 'urn:...' metadata fields");
    }

    @Test
    void schemaHasNoDollarSchemaField() {
      Map<String, Object> schema = producer.produce(Address.class);
      assertFalse(
          schemaTreeContainsKey(schema, "$schema"),
          "Schema must not contain '$schema' — not supported by OpenAI strict mode");
    }
  }

  // ===== Combined compliance check =====

  @Test
  void fullStrictModeComplianceForDeeplyNestedType() {
    // This is the all-in-one check: Person -> ContactInfo -> Address
    Map<String, Object> schema = producer.produce(Person.class);

    List<Map<String, Object>> allObjects = collectAllObjectNodes(schema);
    assertFalse(allObjects.isEmpty(), "Should find at least one object node");

    List<String> violations = new ArrayList<>();

    for (Map<String, Object> obj : allObjects) {
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) obj.get("properties");
      @SuppressWarnings("unchecked")
      List<String> required = (List<String>) obj.get("required");

      if (!Boolean.FALSE.equals(obj.get("additionalProperties"))) {
        violations.add("Missing additionalProperties:false on object with props: " + (props != null ? props.keySet() : "none"));
      }
      if (required == null) {
        violations.add("Missing required[] on object with props: " + (props != null ? props.keySet() : "none"));
      } else if (props != null && !required.containsAll(props.keySet())) {
        violations.add("Incomplete required[] on object with props: " + props.keySet() + ", required has: " + required);
      }
    }

    if (schemaTreeContainsKey(schema, "$ref")) {
      violations.add("Schema contains $ref references");
    }
    if (schemaTreeContainsUrnId(schema)) {
      violations.add("Schema contains 'id': 'urn:...' metadata fields (Jackson URN style)");
    }

    assertTrue(violations.isEmpty(), "OpenAI strict mode compliance violations:\n" + String.join("\n", violations));
  }
}
