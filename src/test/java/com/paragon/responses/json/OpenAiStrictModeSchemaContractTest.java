package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import tools.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies that every schema produced by {@link JacksonJsonSchemaProducer} satisfies the *complete*
 * contract OpenAI strict mode imposes before it will engage.
 *
 * <p>If strict mode does not engage, the model is free to format its answer however it likes
 * (including markdown code fences). These tests answer the question: "Is the problem on our side?"
 *
 * <p>Rules from <a href="https://platform.openai.com/docs/guides/structured-outputs">OpenAI
 * Structured Outputs docs</a>:
 * <ol>
 *   <li>Every object at every level has {@code "additionalProperties": false}
 *   <li>Every object at every level lists ALL its properties in {@code "required"}
 *   <li>No {@code $ref} anywhere (must be fully inlined)
 *   <li>No Jackson URN {@code "id"} metadata
 *   <li>Root schema must have {@code "type": "object"} (not string, enum, etc.)
 *   <li>No unsupported draft-3/4 keywords: {@code disallow}, {@code extends}, {@code format},
 *       {@code default}, {@code pattern}, {@code minimum}, {@code maximum}, {@code allOf},
 *       {@code not}, etc.
 *   <li>Enum fields must have {@code "type": "string"} alongside {@code "enum": [...]}
 *   <li>Array fields must have an {@code "items"} schema
 * </ol>
 */
class OpenAiStrictModeSchemaContractTest {

  /** Keywords OpenAI strict mode does not allow. */
  private static final List<String> FORBIDDEN_KEYWORDS =
      List.of(
          "disallow", "extends", "default", "pattern",
          "minLength", "maxLength", "minimum", "maximum",
          "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
          "minItems", "maxItems", "uniqueItems",
          "minProperties", "maxProperties",
          "allOf", "not", "$schema");

  private JacksonJsonSchemaProducer producer;

  @BeforeEach
  void setUp() {
    producer = new JacksonJsonSchemaProducer(new ObjectMapper());
  }

  // ===== Test data =====

  enum Status { ACTIVE, INACTIVE, PENDING }

  record Address(String street, String city, String country) {}

  record ContactInfo(String email, String phone, Address primary, Address secondary) {}

  record Person(String id, int age, boolean active, Status status,
                ContactInfo contact, List<Address> previousAddresses) {}

  record Wrapper(String label, List<Person> people, Map<String, String> metadata) {}

  // ===== Helpers =====

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> allObjectNodes(Object node) {
    List<Map<String, Object>> result = new ArrayList<>();
    collectObjects(node, result);
    return result;
  }

  @SuppressWarnings("unchecked")
  private void collectObjects(Object node, List<Map<String, Object>> acc) {
    if (!(node instanceof Map<?, ?> raw)) return;
    Map<String, Object> map = (Map<String, Object>) raw;
    if ("object".equals(map.get("type")) && map.containsKey("properties")) {
      acc.add(map);
    }
    for (Object v : map.values()) {
      if (v instanceof Map) collectObjects(v, acc);
      if (v instanceof List<?> l) l.forEach(i -> collectObjects(i, acc));
    }
  }

  @SuppressWarnings("unchecked")
  private boolean treeContains(Object node, String key) {
    if (!(node instanceof Map<?, ?> raw)) return false;
    Map<String, Object> map = (Map<String, Object>) raw;
    if (map.containsKey(key)) return true;
    for (Object v : map.values()) {
      if (treeContains(v, key)) return true;
      if (v instanceof List<?> l) { for (Object i : l) if (treeContains(i, key)) return true; }
    }
    return false;
  }

  private boolean treeContainsUrnId(Object node) {
    if (!(node instanceof Map<?, ?> raw)) return false;
    @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) raw;
    if (map.get("id") instanceof String v && v.startsWith("urn:")) return true;
    for (Object val : map.values()) {
      if (treeContainsUrnId(val)) return true;
      if (val instanceof List<?> l) { for (Object i : l) if (treeContainsUrnId(i)) return true; }
    }
    return false;
  }

  /** Collects ALL violations across the whole schema tree. */
  private List<String> collectViolations(Map<String, Object> schema) {
    List<String> v = new ArrayList<>();

    // Rule 1 & 2: every object node
    for (Map<String, Object> obj : allObjectNodes(schema)) {
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) obj.get("properties");
      @SuppressWarnings("unchecked")
      List<String> required = (List<String>) obj.get("required");

      if (!Boolean.FALSE.equals(obj.get("additionalProperties")))
        v.add("Object missing additionalProperties:false — props: " + (props != null ? props.keySet() : "none"));

      if (required == null)
        v.add("Object missing required[] — props: " + (props != null ? props.keySet() : "none"));
      else if (props != null && !required.containsAll(props.keySet()))
        v.add("required[] incomplete — has " + required + " but props are " + props.keySet());
    }

    // Rule 3: no $ref
    if (treeContains(schema, "$ref"))
      v.add("Schema contains $ref references (must be fully inlined)");

    // Rule 4: no Jackson URN id metadata
    if (treeContainsUrnId(schema))
      v.add("Schema contains 'id': 'urn:...' Jackson metadata");

    // Rule 5: root must be type:object
    if (!"object".equals(schema.get("type")))
      v.add("Root type is '" + schema.get("type") + "' — must be 'object'");

    // Rule 6: no forbidden keywords anywhere
    for (String kw : FORBIDDEN_KEYWORDS) {
      if (treeContains(schema, kw))
        v.add("Forbidden keyword present: '" + kw + "'");
    }

    return v;
  }

  // ===== Rule 5: root must be type:object =====

  @Nested
  class RootMustBeObject {

    @Test
    void simpleRecordRootIsObject() {
      var schema = producer.produce(Address.class);
      assertEquals("object", schema.get("type"),
          "Root must be type:object for OpenAI strict mode to engage");
    }

    @Test
    void nestedRecordRootIsObject() {
      var schema = producer.produce(Person.class);
      assertEquals("object", schema.get("type"));
    }
  }

  // ===== Rule 6: no forbidden draft-3/4 keywords =====

  @Nested
  class NoForbiddenKeywords {

    @Test
    void simpleRecordHasNoForbiddenKeywords() {
      var schema = producer.produce(Address.class);
      for (String kw : FORBIDDEN_KEYWORDS) {
        assertFalse(treeContains(schema, kw),
            "Schema must not contain forbidden keyword: '" + kw + "'");
      }
    }

    @Test
    void complexRecordHasNoForbiddenKeywords() {
      var schema = producer.produce(Person.class);
      for (String kw : FORBIDDEN_KEYWORDS) {
        assertFalse(treeContains(schema, kw),
            "Complex schema must not contain forbidden keyword: '" + kw + "'");
      }
    }

    @Test
    void schemaWithEnumsHasNoForbiddenKeywords() {
      // Enums are common — make sure Jackson doesn't add draft-3 "disallow" or "extends"
      var schema = producer.produce(Person.class);
      assertFalse(treeContains(schema, "disallow"),
          "Jackson draft-3 'disallow' must not appear");
      assertFalse(treeContains(schema, "extends"),
          "Jackson draft-3 'extends' must not appear");
    }
  }

  // ===== Rule 7: enum fields must have type:string =====

  @Nested
  class EnumFieldsMustHaveTypeString {

    @Test
    void inlineEnumPropertyHasTypeString() {
      var schema = producer.produce(Person.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");
      @SuppressWarnings("unchecked")
      Map<String, Object> statusProp = (Map<String, Object>) props.get("status");

      assertNotNull(statusProp, "status property must exist");
      assertEquals("string", statusProp.get("type"),
          "Enum property must have type:string for OpenAI strict mode");
      assertNotNull(statusProp.get("enum"),
          "Enum property must have enum values");
    }

    @Test
    void enumValuesAreAllPresent() {
      var schema = producer.produce(Person.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");
      @SuppressWarnings("unchecked")
      Map<String, Object> statusProp = (Map<String, Object>) props.get("status");
      @SuppressWarnings("unchecked")
      List<String> values = (List<String>) statusProp.get("enum");

      assertTrue(values.containsAll(List.of("ACTIVE", "INACTIVE", "PENDING")),
          "All enum values must be present: " + values);
    }
  }

  // ===== Rule 8: array fields must have items schema =====

  @Nested
  class ArrayFieldsMustHaveItems {

    @Test
    void listOfObjectsHasItemsSchema() {
      var schema = producer.produce(Person.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");
      @SuppressWarnings("unchecked")
      Map<String, Object> addressesProp = (Map<String, Object>) props.get("previousAddresses");

      assertEquals("array", addressesProp.get("type"));
      assertNotNull(addressesProp.get("items"),
          "Array property must have an 'items' schema");
    }

    @Test
    void itemsSchemaForObjectArrayIsFullyInlined() {
      // List<Address> items must be inlined — not a $ref
      var schema = producer.produce(Person.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");
      @SuppressWarnings("unchecked")
      Map<String, Object> addressesProp = (Map<String, Object>) props.get("previousAddresses");
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) addressesProp.get("items");

      assertFalse(items.containsKey("$ref"),
          "Array items must be inlined, not a $ref");
      assertEquals("object", items.get("type"),
          "Array items for List<Address> must have type:object");
      assertNotNull(items.get("properties"),
          "Array items must have their properties inlined");
    }

    @Test
    void itemsObjectHasAdditionalPropertiesFalse() {
      var schema = producer.produce(Person.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");
      @SuppressWarnings("unchecked")
      Map<String, Object> addressesProp = (Map<String, Object>) props.get("previousAddresses");
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) addressesProp.get("items");

      assertEquals(false, items.get("additionalProperties"),
          "Array items object must also have additionalProperties:false");
    }

    @Test
    void itemsObjectHasRequiredArray() {
      var schema = producer.produce(Person.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");
      @SuppressWarnings("unchecked")
      Map<String, Object> addressesProp = (Map<String, Object>) props.get("previousAddresses");
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) addressesProp.get("items");
      @SuppressWarnings("unchecked")
      List<String> required = (List<String>) items.get("required");

      assertNotNull(required, "Array items object must have a required[] array");
      @SuppressWarnings("unchecked")
      Map<String, Object> itemProps = (Map<String, Object>) items.get("properties");
      assertTrue(required.containsAll(itemProps.keySet()),
          "Array items required[] must list all properties: " + itemProps.keySet());
    }
  }

  // ===== Repeated-type inlining (same type used twice must not produce $ref) =====

  @Nested
  class RepeatedTypesMustBeFullyInlined {

    @Test
    void sameTypeUsedTwiceIsInlinedBothTimes() {
      // ContactInfo has Address primary AND Address secondary — Jackson uses $ref for the second
      var schema = producer.produce(ContactInfo.class);

      assertFalse(treeContains(schema, "$ref"),
          "Both occurrences of Address must be inlined; no $ref allowed");
    }

    @Test
    void bothAddressOccurrencesHaveProperties() {
      var schema = producer.produce(ContactInfo.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");

      @SuppressWarnings("unchecked")
      Map<String, Object> primary = (Map<String, Object>) props.get("primary");
      @SuppressWarnings("unchecked")
      Map<String, Object> secondary = (Map<String, Object>) props.get("secondary");

      assertNotNull(primary.get("properties"),
          "primary (first Address occurrence) must have inlined properties");
      assertNotNull(secondary.get("properties"),
          "secondary (second Address occurrence, was $ref) must have inlined properties");
    }

    @Test
    void bothAddressOccurrencesAreFullyCompliant() {
      var schema = producer.produce(ContactInfo.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) schema.get("properties");

      for (String field : List.of("primary", "secondary")) {
        @SuppressWarnings("unchecked")
        Map<String, Object> addrSchema = (Map<String, Object>) props.get(field);
        assertEquals(false, addrSchema.get("additionalProperties"),
            field + " must have additionalProperties:false");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) addrSchema.get("required");
        assertNotNull(required, field + " must have required[]");
        @SuppressWarnings("unchecked")
        Map<String, Object> addrProps = (Map<String, Object>) addrSchema.get("properties");
        assertTrue(required.containsAll(addrProps.keySet()),
            field + " required[] must list all properties");
      }
    }
  }

  // ===== Full combined compliance check =====

  @Test
  void fullContractCompliance_simpleRecord() {
    var schema = producer.produce(Address.class);
    var violations = collectViolations(schema);
    assertTrue(violations.isEmpty(),
        "Address schema has strict mode violations:\n" + String.join("\n", violations));
  }

  @Test
  void fullContractCompliance_recordWithEnum() {
    var schema = producer.produce(Person.class);
    var violations = collectViolations(schema);
    assertTrue(violations.isEmpty(),
        "Person schema has strict mode violations:\n" + String.join("\n", violations));
  }

  @Test
  void fullContractCompliance_repeatedNestedType() {
    var schema = producer.produce(ContactInfo.class);
    var violations = collectViolations(schema);
    assertTrue(violations.isEmpty(),
        "ContactInfo schema (repeated Address) has strict mode violations:\n"
            + String.join("\n", violations));
  }

  @Test
  void fullContractCompliance_deeplyNested() {
    // Person -> ContactInfo -> Address (x2), List<Address>
    var schema = producer.produce(Person.class);
    var violations = collectViolations(schema);
    assertTrue(violations.isEmpty(),
        "Person (deeply nested) has strict mode violations:\n"
            + String.join("\n", violations));
  }

  @Test
  void fullContractCompliance_wrapperWithListOfComplexType() {
    var schema = producer.produce(Wrapper.class);
    var violations = collectViolations(schema);
    assertTrue(violations.isEmpty(),
        "Wrapper schema (List<Person>) has strict mode violations:\n"
            + String.join("\n", violations));
  }
}
