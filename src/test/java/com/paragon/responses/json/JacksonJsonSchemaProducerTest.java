package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JacksonJsonSchemaProducer}.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>Basic schema generation for simple classes
 *   <li>Schema generation for classes with primitive and object fields
 *   <li>Schema generation for classes with nested objects
 *   <li>Schema generation for classes with collections
 *   <li>Schema generation for records
 *   <li>Builder pattern functionality
 *   <li>Error handling
 * </ul>
 */
class JacksonJsonSchemaProducerTest {

  private JacksonJsonSchemaProducer producer;

  @BeforeEach
  void setUp() {
    producer = new JacksonJsonSchemaProducer(new ObjectMapper());
  }

  // ===== Test Data Classes =====

  /** Simple POJO with primitive fields */
  static class SimplePojo {
    private String name;
    private int age;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

  /** POJO with @JsonProperty annotations */
  static class AnnotatedPojo {
    @JsonProperty(required = true)
    private String requiredField;

    @JsonProperty("custom_name")
    private String customNameField;

    public String getRequiredField() {
      return requiredField;
    }

    public void setRequiredField(String requiredField) {
      this.requiredField = requiredField;
    }

    public String getCustomNameField() {
      return customNameField;
    }

    public void setCustomNameField(String customNameField) {
      this.customNameField = customNameField;
    }
  }

  /** POJO with nested object */
  static class NestedPojo {
    private String title;
    private SimplePojo nested;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public SimplePojo getNested() {
      return nested;
    }

    public void setNested(SimplePojo nested) {
      this.nested = nested;
    }
  }

  /** POJO with collection fields */
  static class CollectionPojo {
    private List<String> tags;
    private List<SimplePojo> items;

    public List<String> getTags() {
      return tags;
    }

    public void setTags(List<String> tags) {
      this.tags = tags;
    }

    public List<SimplePojo> getItems() {
      return items;
    }

    public void setItems(List<SimplePojo> items) {
      this.items = items;
    }
  }

  /** Simple record for testing */
  record SimpleRecord(String name, int value) {}

  /** Record with nested types */
  record ComplexRecord(String id, SimpleRecord child, List<String> items) {}

  // ===== Complex Nested Test Data Classes =====

  /** Enum for testing */
  enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  /** Enum for status */
  enum Status {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED
  }

  /** Level 3 nesting - deepest level */
  static class Address {
    private String street;
    private String city;
    private String country;
    private String postalCode;

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }

    public String getCountry() {
      return country;
    }

    public void setCountry(String country) {
      this.country = country;
    }

    public String getPostalCode() {
      return postalCode;
    }

    public void setPostalCode(String postalCode) {
      this.postalCode = postalCode;
    }
  }

  /** Level 2 nesting - contains Address */
  static class ContactInfo {
    private String email;
    private String phone;
    private Address primaryAddress;
    private Address secondaryAddress;

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public Address getPrimaryAddress() {
      return primaryAddress;
    }

    public void setPrimaryAddress(Address primaryAddress) {
      this.primaryAddress = primaryAddress;
    }

    public Address getSecondaryAddress() {
      return secondaryAddress;
    }

    public void setSecondaryAddress(Address secondaryAddress) {
      this.secondaryAddress = secondaryAddress;
    }
  }

  /** Level 1 nesting - contains ContactInfo */
  static class Person {
    private String id;
    private String firstName;
    private String lastName;
    private int age;
    private ContactInfo contactInfo;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public ContactInfo getContactInfo() {
      return contactInfo;
    }

    public void setContactInfo(ContactInfo contactInfo) {
      this.contactInfo = contactInfo;
    }
  }

  /** Root level - deepest nesting structure (4 levels deep) */
  static class Organization {
    private String name;
    private String description;
    private Person ceo;
    private List<Person> employees;
    private Map<String, Person> departments;
    private Address headquarters;
    private Priority priority;
    private Status status;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Person getCeo() {
      return ceo;
    }

    public void setCeo(Person ceo) {
      this.ceo = ceo;
    }

    public List<Person> getEmployees() {
      return employees;
    }

    public void setEmployees(List<Person> employees) {
      this.employees = employees;
    }

    public Map<String, Person> getDepartments() {
      return departments;
    }

    public void setDepartments(Map<String, Person> departments) {
      this.departments = departments;
    }

    public Address getHeadquarters() {
      return headquarters;
    }

    public void setHeadquarters(Address headquarters) {
      this.headquarters = headquarters;
    }

    public Priority getPriority() {
      return priority;
    }

    public void setPriority(Priority priority) {
      this.priority = priority;
    }

    public Status getStatus() {
      return status;
    }

    public void setStatus(Status status) {
      this.status = status;
    }
  }

  /** POJO with Map fields */
  static class MapPojo {
    private Map<String, String> stringMap;
    private Map<String, Integer> integerMap;
    private Map<String, SimplePojo> objectMap;
    private Map<String, List<String>> nestedCollectionMap;

    public Map<String, String> getStringMap() {
      return stringMap;
    }

    public void setStringMap(Map<String, String> stringMap) {
      this.stringMap = stringMap;
    }

    public Map<String, Integer> getIntegerMap() {
      return integerMap;
    }

    public void setIntegerMap(Map<String, Integer> integerMap) {
      this.integerMap = integerMap;
    }

    public Map<String, SimplePojo> getObjectMap() {
      return objectMap;
    }

    public void setObjectMap(Map<String, SimplePojo> objectMap) {
      this.objectMap = objectMap;
    }

    public Map<String, List<String>> getNestedCollectionMap() {
      return nestedCollectionMap;
    }

    public void setNestedCollectionMap(Map<String, List<String>> nestedCollectionMap) {
      this.nestedCollectionMap = nestedCollectionMap;
    }
  }

  /** Record with complex nesting */
  record TaskRecord(
      String id,
      String title,
      Priority priority,
      Status status,
      Person assignee,
      List<Person> watchers,
      Map<String, String> metadata) {}

  // ===== Basic Schema Generation Tests =====

  @Nested
  class BasicSchemaGeneration {

    @Test
    void producesNonNullSchema() {
      Map<String, Object> schema = producer.produce(SimplePojo.class);

      assertNotNull(schema, "Schema should not be null");
      assertFalse(schema.isEmpty(), "Schema should not be empty");
    }

    @Test
    void schemaHasTypeProperty() {
      Map<String, Object> schema = producer.produce(SimplePojo.class);

      assertTrue(schema.containsKey("type"), "Schema should have 'type' property");
      assertEquals("object", schema.get("type"), "Type should be 'object' for a class");
    }

    @Test
    void schemaHasPropertiesForFields() {
      Map<String, Object> schema = producer.produce(SimplePojo.class);

      assertTrue(schema.containsKey("properties"), "Schema should have 'properties'");

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("name"), "Properties should include 'name' field");
      assertTrue(properties.containsKey("age"), "Properties should include 'age' field");
    }

    @Test
    void schemaDetectsCorrectTypes() {
      Map<String, Object> schema = producer.produce(SimplePojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      @SuppressWarnings("unchecked")
      Map<String, Object> nameProperty = (Map<String, Object>) properties.get("name");
      assertEquals("string", nameProperty.get("type"), "Name should be typed as 'string'");

      @SuppressWarnings("unchecked")
      Map<String, Object> ageProperty = (Map<String, Object>) properties.get("age");
      assertEquals("integer", ageProperty.get("type"), "Age should be typed as 'integer'");
    }
  }

  // ===== Annotated Classes Tests =====

  @Nested
  class AnnotatedClassSchemaGeneration {

    @Test
    void handlesCustomPropertyNames() {
      Map<String, Object> schema = producer.produce(AnnotatedPojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(
          properties.containsKey("custom_name"),
          "Properties should use @JsonProperty name 'custom_name'");
    }
  }

  // ===== Nested Objects Tests =====

  @Nested
  class NestedObjectSchemaGeneration {

    @Test
    void handlesNestedObjects() {
      Map<String, Object> schema = producer.produce(NestedPojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("nested"), "Properties should include 'nested' field");

      @SuppressWarnings("unchecked")
      Map<String, Object> nestedProperty = (Map<String, Object>) properties.get("nested");

      // Nested object should be represented as an object type or reference
      assertNotNull(nestedProperty, "Nested property should have a definition");
    }
  }

  // ===== Complex Nested Objects Tests =====

  @Nested
  class ComplexNestedObjectSchemaGeneration {

    @Test
    void handlesDeeplyNestedObjects() {
      // Organization -> Person -> ContactInfo -> Address (4 levels)
      Map<String, Object> schema = producer.produce(Organization.class);

      assertNotNull(schema);
      assertEquals("object", schema.get("type"));

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      // Verify top-level properties exist
      assertTrue(properties.containsKey("name"), "Should have 'name' property");
      assertTrue(properties.containsKey("ceo"), "Should have 'ceo' property (Person)");
      assertTrue(
          properties.containsKey("employees"), "Should have 'employees' property (List<Person>)");
      assertTrue(
          properties.containsKey("headquarters"), "Should have 'headquarters' property (Address)");
      assertTrue(properties.containsKey("priority"), "Should have 'priority' property (enum)");
      assertTrue(properties.containsKey("status"), "Should have 'status' property (enum)");
    }

    @Test
    void personHasNestedContactInfo() {
      Map<String, Object> schema = producer.produce(Person.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(
          properties.containsKey("contactInfo"), "Person should have 'contactInfo' property");

      @SuppressWarnings("unchecked")
      Map<String, Object> contactInfoProp = (Map<String, Object>) properties.get("contactInfo");
      assertNotNull(contactInfoProp, "ContactInfo property should have definition");
    }

    @Test
    void contactInfoHasNestedAddresses() {
      Map<String, Object> schema = producer.produce(ContactInfo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("primaryAddress"), "Should have 'primaryAddress'");
      assertTrue(properties.containsKey("secondaryAddress"), "Should have 'secondaryAddress'");
      assertTrue(properties.containsKey("email"), "Should have 'email'");
      assertTrue(properties.containsKey("phone"), "Should have 'phone'");
    }

    @Test
    void addressHasAllFields() {
      Map<String, Object> schema = producer.produce(Address.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("street"), "Should have 'street'");
      assertTrue(properties.containsKey("city"), "Should have 'city'");
      assertTrue(properties.containsKey("country"), "Should have 'country'");
      assertTrue(properties.containsKey("postalCode"), "Should have 'postalCode'");

      // Verify all are strings
      for (String field : List.of("street", "city", "country", "postalCode")) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldProp = (Map<String, Object>) properties.get(field);
        assertEquals("string", fieldProp.get("type"), field + " should be a string");
      }
    }

    @Test
    void handlesMultipleNestedObjectsOfSameType() {
      // ContactInfo has two Address fields
      Map<String, Object> schema = producer.produce(ContactInfo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      @SuppressWarnings("unchecked")
      Map<String, Object> primaryAddr = (Map<String, Object>) properties.get("primaryAddress");
      @SuppressWarnings("unchecked")
      Map<String, Object> secondaryAddr = (Map<String, Object>) properties.get("secondaryAddress");

      assertNotNull(primaryAddr, "Primary address should exist");
      assertNotNull(secondaryAddr, "Secondary address should exist");
    }

    @Test
    void handlesListOfNestedObjects() {
      Map<String, Object> schema = producer.produce(Organization.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      @SuppressWarnings("unchecked")
      Map<String, Object> employeesProp = (Map<String, Object>) properties.get("employees");

      assertEquals("array", employeesProp.get("type"), "Employees should be an array");
      assertTrue(employeesProp.containsKey("items"), "Array should have 'items' definition");
    }

    @Test
    void handlesMapOfNestedObjects() {
      Map<String, Object> schema = producer.produce(Organization.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("departments"), "Should have 'departments' map field");

      @SuppressWarnings("unchecked")
      Map<String, Object> deptsProp = (Map<String, Object>) properties.get("departments");
      assertNotNull(deptsProp, "Departments should have a definition");
    }
  }

  // ===== Enum Tests =====

  @Nested
  class EnumSchemaGeneration {

    @Test
    void handlesEnumFields() {
      Map<String, Object> schema = producer.produce(Organization.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("priority"), "Should have priority enum field");
      assertTrue(properties.containsKey("status"), "Should have status enum field");
    }

    @Test
    void enumSchemaHasCorrectType() {
      Map<String, Object> schema = producer.produce(Priority.class);

      assertNotNull(schema);
      // Enums are typically represented as strings in JSON Schema
      assertEquals("string", schema.get("type"), "Enum should be typed as 'string'");
    }

    @Test
    void enumSchemaHasEnumValues() {
      Map<String, Object> schema = producer.produce(Priority.class);

      assertTrue(schema.containsKey("enum"), "Enum schema should have 'enum' property with values");

      @SuppressWarnings("unchecked")
      List<String> enumValues = (List<String>) schema.get("enum");
      assertEquals(4, enumValues.size(), "Should have 4 enum values");
      assertTrue(enumValues.contains("LOW"), "Should contain LOW");
      assertTrue(enumValues.contains("MEDIUM"), "Should contain MEDIUM");
      assertTrue(enumValues.contains("HIGH"), "Should contain HIGH");
      assertTrue(enumValues.contains("CRITICAL"), "Should contain CRITICAL");
    }
  }

  // ===== Map Fields Tests =====

  @Nested
  class MapSchemaGeneration {

    @Test
    void handlesStringMaps() {
      Map<String, Object> schema = producer.produce(MapPojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("stringMap"), "Should have 'stringMap' property");

      @SuppressWarnings("unchecked")
      Map<String, Object> stringMapProp = (Map<String, Object>) properties.get("stringMap");
      assertEquals("object", stringMapProp.get("type"), "Map should be typed as 'object'");
    }

    @Test
    void handlesObjectMaps() {
      Map<String, Object> schema = producer.produce(MapPojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("objectMap"), "Should have 'objectMap' property");

      @SuppressWarnings("unchecked")
      Map<String, Object> objectMapProp = (Map<String, Object>) properties.get("objectMap");
      assertEquals("object", objectMapProp.get("type"), "Map should be typed as 'object'");
    }

    @Test
    void handlesNestedCollectionMaps() {
      Map<String, Object> schema = producer.produce(MapPojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(
          properties.containsKey("nestedCollectionMap"),
          "Should have 'nestedCollectionMap' property");
    }
  }

  // ===== Complex Record Tests =====

  @Nested
  class ComplexRecordSchemaGeneration {

    @Test
    void handlesRecordWithEnumAndNestedTypes() {
      Map<String, Object> schema = producer.produce(TaskRecord.class);

      assertNotNull(schema);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      // Verify all fields
      assertTrue(properties.containsKey("id"), "Should have 'id'");
      assertTrue(properties.containsKey("title"), "Should have 'title'");
      assertTrue(properties.containsKey("priority"), "Should have 'priority' (enum)");
      assertTrue(properties.containsKey("status"), "Should have 'status' (enum)");
      assertTrue(properties.containsKey("assignee"), "Should have 'assignee' (Person)");
      assertTrue(properties.containsKey("watchers"), "Should have 'watchers' (List<Person>)");
      assertTrue(properties.containsKey("metadata"), "Should have 'metadata' (Map)");
    }

    @Test
    void recordStringFieldsAreTypedCorrectly() {
      Map<String, Object> schema = producer.produce(TaskRecord.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      @SuppressWarnings("unchecked")
      Map<String, Object> idProp = (Map<String, Object>) properties.get("id");
      assertEquals("string", idProp.get("type"));

      @SuppressWarnings("unchecked")
      Map<String, Object> titleProp = (Map<String, Object>) properties.get("title");
      assertEquals("string", titleProp.get("type"));
    }

    @Test
    void recordListFieldIsArray() {
      Map<String, Object> schema = producer.produce(TaskRecord.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      @SuppressWarnings("unchecked")
      Map<String, Object> watchersProp = (Map<String, Object>) properties.get("watchers");
      assertEquals("array", watchersProp.get("type"));
    }
  }

  // ===== Collection Fields Tests =====

  @Nested
  class CollectionSchemaGeneration {

    @Test
    void handlesListOfPrimitives() {
      Map<String, Object> schema = producer.produce(CollectionPojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("tags"), "Properties should include 'tags' field");

      @SuppressWarnings("unchecked")
      Map<String, Object> tagsProperty = (Map<String, Object>) properties.get("tags");

      assertEquals("array", tagsProperty.get("type"), "Tags should be typed as 'array'");
    }

    @Test
    void handlesListOfObjects() {
      Map<String, Object> schema = producer.produce(CollectionPojo.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("items"), "Properties should include 'items' field");

      @SuppressWarnings("unchecked")
      Map<String, Object> itemsProperty = (Map<String, Object>) properties.get("items");

      assertEquals("array", itemsProperty.get("type"), "Items should be typed as 'array'");
    }
  }

  // ===== Record Tests =====

  @Nested
  class RecordSchemaGeneration {

    @Test
    void handlesSimpleRecord() {
      Map<String, Object> schema = producer.produce(SimpleRecord.class);

      assertNotNull(schema);
      assertEquals("object", schema.get("type"));

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("name"), "Should include 'name' from record");
      assertTrue(properties.containsKey("value"), "Should include 'value' from record");
    }

    @Test
    void handlesComplexRecord() {
      Map<String, Object> schema = producer.produce(ComplexRecord.class);

      assertNotNull(schema);

      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

      assertTrue(properties.containsKey("id"), "Should include 'id' from record");
      assertTrue(properties.containsKey("child"), "Should include 'child' from record");
      assertTrue(properties.containsKey("items"), "Should include 'items' from record");
    }
  }

  // ===== Builder Tests =====

  @Nested
  class BuilderTests {

    @Test
    void builderWithDefaultMapper() {
      JsonSchemaProducer producer = new JacksonJsonSchemaProducer.Builder().build();

      assertNotNull(producer);
      Map<String, Object> schema = producer.produce(SimplePojo.class);
      assertNotNull(schema);
    }

    @Test
    void builderWithCustomMapper() {
      ObjectMapper customMapper = new ObjectMapper();
      JsonSchemaProducer producer =
          new JacksonJsonSchemaProducer.Builder().mapper(customMapper).build();

      assertNotNull(producer);
      Map<String, Object> schema = producer.produce(SimplePojo.class);
      assertNotNull(schema);
    }

    @Test
    void builderWithNullMapperUsesDefault() {
      JsonSchemaProducer producer = new JacksonJsonSchemaProducer.Builder().mapper(null).build();

      assertNotNull(producer);
      // Should not throw - uses default mapper
      Map<String, Object> schema = producer.produce(SimplePojo.class);
      assertNotNull(schema);
    }
  }

  // ===== Different Input Classes Tests =====

  @Nested
  class DifferentInputClassesTests {

    @Test
    void producesUniqueSchemaPerClass() {
      Map<String, Object> simpleSchema = producer.produce(SimplePojo.class);
      Map<String, Object> nestedSchema = producer.produce(NestedPojo.class);

      assertNotEquals(
          simpleSchema.toString(),
          nestedSchema.toString(),
          "Different classes should produce different schemas");
    }

    @Test
    void sameClassProducesSameSchema() {
      Map<String, Object> schema1 = producer.produce(SimplePojo.class);
      Map<String, Object> schema2 = producer.produce(SimplePojo.class);

      assertEquals(schema1, schema2, "Same class should produce identical schemas");
    }

    @Test
    void complexNestedClassesDifferFromSimple() {
      Map<String, Object> simpleSchema = producer.produce(SimplePojo.class);
      Map<String, Object> orgSchema = producer.produce(Organization.class);

      assertNotEquals(simpleSchema, orgSchema);

      @SuppressWarnings("unchecked")
      Map<String, Object> simpleProps = (Map<String, Object>) simpleSchema.get("properties");
      @SuppressWarnings("unchecked")
      Map<String, Object> orgProps = (Map<String, Object>) orgSchema.get("properties");

      assertTrue(orgProps.size() > simpleProps.size(), "Organization should have more properties");
    }
  }

  // ===== Integration Test =====

  @Test
  void fullIntegrationTest() {
    // Create producer with builder
    JsonSchemaProducer schemaProducer =
        new JacksonJsonSchemaProducer.Builder().mapper(new ObjectMapper()).build();

    // Generate schema for complex record
    Map<String, Object> schema = schemaProducer.produce(ComplexRecord.class);

    // Verify structure
    assertNotNull(schema);
    assertEquals("object", schema.get("type"));
    assertTrue(schema.containsKey("properties"));

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

    // Verify all fields are present
    assertEquals(3, properties.size(), "Should have 3 properties: id, child, items");
    assertTrue(properties.containsKey("id"));
    assertTrue(properties.containsKey("child"));
    assertTrue(properties.containsKey("items"));

    // Verify types
    @SuppressWarnings("unchecked")
    Map<String, Object> idProp = (Map<String, Object>) properties.get("id");
    assertEquals("string", idProp.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> itemsProp = (Map<String, Object>) properties.get("items");
    assertEquals("array", itemsProp.get("type"));
  }

  @Test
  void fullIntegrationTestWithDeeplyNestedStructure() {
    // Generate schema for deeply nested Organization class
    Map<String, Object> schema = producer.produce(Organization.class);

    // Verify structure
    assertNotNull(schema);
    assertEquals("object", schema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

    // Organization has: name, description, ceo, employees, departments, headquarters, priority,
    // status
    assertEquals(8, properties.size(), "Organization should have 8 properties");

    // Verify nested structures exist
    assertTrue(properties.containsKey("ceo"), "Should have CEO (Person)");
    assertTrue(properties.containsKey("employees"), "Should have employees (List<Person>)");
    assertTrue(properties.containsKey("headquarters"), "Should have headquarters (Address)");

    // Verify enums
    assertTrue(properties.containsKey("priority"), "Should have priority enum");
    assertTrue(properties.containsKey("status"), "Should have status enum");
  }
}
