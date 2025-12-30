package com.paragon.responses.streaming;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for PartialJsonParser.
 *
 * <p>Tests cover: - Complete JSON parsing - Incomplete JSON completion and parsing - Edge cases
 * (empty, null fields, arrays) - Map-based parsing
 */
@DisplayName("PartialJsonParser Tests")
class PartialJsonParserTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // COMPLETE JSON
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Complete JSON")
  class CompleteJson {

    @Test
    @DisplayName("parses complete JSON object")
    void parsesCompleteJson() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      PartialPerson result = parser.parsePartial("{\"name\": \"John\", \"age\": 30}");

      assertNotNull(result);
      assertEquals("John", result.name());
      assertEquals(30, result.age());
    }

    @Test
    @DisplayName("parses nested objects")
    void parsesNestedObjects() {
      PartialJsonParser<PersonWithAddress> parser =
          new PartialJsonParser<>(objectMapper, PersonWithAddress.class);

      String json = "{\"name\": \"Alice\", \"address\": {\"city\": \"NYC\", \"zip\": \"10001\"}}";
      PersonWithAddress result = parser.parsePartial(json);

      assertNotNull(result);
      assertEquals("Alice", result.name());
      assertNotNull(result.address());
      assertEquals("NYC", result.address().city());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INCOMPLETE JSON
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Incomplete JSON")
  class IncompleteJson {

    @Test
    @DisplayName("completes unclosed brace")
    void completesUnclosedBrace() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      PartialPerson result = parser.parsePartial("{\"name\": \"John\"");

      assertNotNull(result);
      assertEquals("John", result.name());
    }

    @Test
    @DisplayName("completes unclosed string")
    void completesUnclosedString() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      // String value is incomplete
      PartialPerson result = parser.parsePartial("{\"name\": \"Jo");

      assertNotNull(result);
      // Value should be whatever was captured
      assertNotNull(result.name());
    }

    @Test
    @DisplayName("handles trailing comma")
    void handlesTrailingComma() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      PartialPerson result = parser.parsePartial("{\"name\": \"John\",");

      assertNotNull(result);
      assertEquals("John", result.name());
    }

    @Test
    @DisplayName("handles incomplete key-value with colon")
    void handlesIncompleteKeyValue() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      PartialPerson result = parser.parsePartial("{\"name\":");

      // Should complete with null value
      assertNotNull(result);
    }

    @Test
    @DisplayName("completes multiple unclosed braces")
    void completesMultipleUnclosedBraces() {
      PartialJsonParser<PersonWithAddress> parser =
          new PartialJsonParser<>(objectMapper, PersonWithAddress.class);

      String partial = "{\"name\": \"Bob\", \"address\": {\"city\": \"LA\"";
      PersonWithAddress result = parser.parsePartial(partial);

      assertNotNull(result);
      assertEquals("Bob", result.name());
      assertNotNull(result.address());
      assertEquals("LA", result.address().city());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EDGE CASES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("returns null for empty string")
    void returnsNullForEmpty() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      assertNull(parser.parsePartial(""));
    }

    @Test
    @DisplayName("returns null for whitespace only")
    void returnsNullForWhitespace() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      assertNull(parser.parsePartial("   "));
    }

    @Test
    @DisplayName("returns null for non-object JSON")
    void returnsNullForNonObject() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      // Arrays are not supported
      assertNull(parser.parsePartial("[1, 2, 3]"));
    }

    @Test
    @DisplayName("handles escaped quotes in strings")
    void handlesEscapedQuotes() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      // String with escaped quote
      PartialPerson result = parser.parsePartial("{\"name\": \"John \\\"Doe\\\"\"}");

      assertNotNull(result);
      assertEquals("John \"Doe\"", result.name());
    }

    @Test
    @DisplayName("handles null values in JSON")
    void handlesNullValues() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      PartialPerson result = parser.parsePartial("{\"name\": null, \"age\": 25}");

      assertNotNull(result);
      assertNull(result.name());
      assertEquals(25, result.age());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MAP PARSING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Map Parsing")
  class MapParsing {

    @Test
    @DisplayName("parseAsMap returns map for complete JSON")
    void parseAsMapComplete() {
      Map<String, Object> result =
          PartialJsonParser.parseAsMap(objectMapper, "{\"name\": \"John\", \"age\": 30}");

      assertNotNull(result);
      assertEquals("John", result.get("name"));
      assertEquals(30, result.get("age"));
    }

    @Test
    @DisplayName("parseAsMap handles incomplete JSON")
    void parseAsMapIncomplete() {
      Map<String, Object> result =
          PartialJsonParser.parseAsMap(objectMapper, "{\"name\": \"John\"");

      assertNotNull(result);
      assertEquals("John", result.get("name"));
    }

    @Test
    @DisplayName("parseAsMap returns null for empty")
    void parseAsMapEmpty() {
      Map<String, Object> result = PartialJsonParser.parseAsMap(objectMapper, "");

      assertNull(result);
    }

    @Test
    @DisplayName("parseAsMap returns null for whitespace")
    void parseAsMapWhitespace() {
      Map<String, Object> result = PartialJsonParser.parseAsMap(objectMapper, "   ");

      assertNull(result);
    }

    @Test
    @DisplayName("parseAsMap handles nested objects")
    void parseAsMapNested() {
      Map<String, Object> result =
          PartialJsonParser.parseAsMap(objectMapper, "{\"person\": {\"name\": \"Alice\"}}");

      assertNotNull(result);
      assertNotNull(result.get("person"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STREAMING SIMULATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Streaming Simulation")
  class StreamingSimulation {

    @Test
    @DisplayName("progressive parsing shows fields appearing")
    void progressiveParsing() {
      PartialJsonParser<PartialPerson> parser =
          new PartialJsonParser<>(objectMapper, PartialPerson.class);

      // Simulate streaming JSON chunk by chunk
      String[] chunks = {
        "{",
        "{\"na",
        "{\"name\"",
        "{\"name\": ",
        "{\"name\": \"Al",
        "{\"name\": \"Alice",
        "{\"name\": \"Alice\"",
        "{\"name\": \"Alice\", ",
        "{\"name\": \"Alice\", \"age",
        "{\"name\": \"Alice\", \"age\":",
        "{\"name\": \"Alice\", \"age\": 25",
        "{\"name\": \"Alice\", \"age\": 25}"
      };

      PartialPerson lastResult = null;
      for (String chunk : chunks) {
        PartialPerson result = parser.parsePartial(chunk);
        if (result != null && result.name() != null) {
          lastResult = result;
        }
      }

      assertNotNull(lastResult);
      assertEquals("Alice", lastResult.name());
      assertEquals(25, lastResult.age());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TEST TYPES
  // ═══════════════════════════════════════════════════════════════════════════

  public record PartialPerson(String name, Integer age) {
    @JsonCreator
    public PartialPerson(@JsonProperty("name") String name, @JsonProperty("age") Integer age) {
      this.name = name;
      this.age = age;
    }
  }

  public record Address(String city, String zip) {
    @JsonCreator
    public Address(@JsonProperty("city") String city, @JsonProperty("zip") String zip) {
      this.city = city;
      this.zip = zip;
    }
  }

  public record PersonWithAddress(String name, Address address) {
    @JsonCreator
    public PersonWithAddress(
        @JsonProperty("name") String name, @JsonProperty("address") Address address) {
      this.name = name;
      this.address = address;
    }
  }
}
