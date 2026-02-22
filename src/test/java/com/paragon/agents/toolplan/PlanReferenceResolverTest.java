package com.paragon.agents.toolplan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PlanReferenceResolver")
class PlanReferenceResolverTest {

  @Nested
  @DisplayName("resolve()")
  class Resolve {

    @Test
    @DisplayName("replaces simple $ref with plain text output (quoted)")
    void simpleRefPlainText() {
      String arguments = "{\"data\": \"$ref:step_1\"}";
      Map<String, String> outputs = Map.of("step_1", "Hello World");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"data\": \"Hello World\"}", result);
    }

    @Test
    @DisplayName("replaces simple $ref with JSON object output (unquoted)")
    void simpleRefJsonObject() {
      String arguments = "{\"data\": \"$ref:step_1\"}";
      Map<String, String> outputs = Map.of("step_1", "{\"temp\": 25, \"city\": \"Tokyo\"}");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      // JSON value is inserted as-is (preserving original formatting)
      assertEquals("{\"data\": {\"temp\": 25, \"city\": \"Tokyo\"}}", result);
    }

    @Test
    @DisplayName("replaces simple $ref with JSON array output (unquoted)")
    void simpleRefJsonArray() {
      String arguments = "{\"items\": \"$ref:step_1\"}";
      Map<String, String> outputs = Map.of("step_1", "[1, 2, 3]");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      // JSON array is inserted as-is (preserving original formatting)
      assertEquals("{\"items\": [1, 2, 3]}", result);
    }

    @Test
    @DisplayName("replaces $ref with numeric output (unquoted)")
    void simpleRefNumeric() {
      String arguments = "{\"count\": \"$ref:step_1\"}";
      Map<String, String> outputs = Map.of("step_1", "42");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"count\": 42}", result);
    }

    @Test
    @DisplayName("replaces $ref with boolean output (unquoted)")
    void simpleRefBoolean() {
      String arguments = "{\"active\": \"$ref:step_1\"}";
      Map<String, String> outputs = Map.of("step_1", "true");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"active\": true}", result);
    }

    @Test
    @DisplayName("replaces field path $ref with extracted field")
    void fieldPathRef() {
      String arguments = "{\"city\": \"$ref:step_1.city\"}";
      Map<String, String> outputs = Map.of("step_1", "{\"city\": \"Tokyo\", \"temp\": 25}");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"city\": \"Tokyo\"}", result);
    }

    @Test
    @DisplayName("replaces nested field path $ref")
    void nestedFieldPathRef() {
      String arguments = "{\"name\": \"$ref:step_1.user.name\"}";
      Map<String, String> outputs =
          Map.of("step_1", "{\"user\": {\"name\": \"Alice\", \"age\": 30}}");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"name\": \"Alice\"}", result);
    }

    @Test
    @DisplayName("replaces field path $ref with numeric field value")
    void fieldPathNumericValue() {
      String arguments = "{\"temp\": \"$ref:step_1.temp\"}";
      Map<String, String> outputs = Map.of("step_1", "{\"temp\": 25}");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"temp\": 25}", result);
    }

    @Test
    @DisplayName("replaces multiple references in one string")
    void multipleRefs() {
      String arguments = "{\"a\": \"$ref:s1\", \"b\": \"$ref:s2\"}";
      Map<String, String> outputs = Map.of("s1", "hello", "s2", "world");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"a\": \"hello\", \"b\": \"world\"}", result);
    }

    @Test
    @DisplayName("passes through string with no references")
    void noReferences() {
      String arguments = "{\"location\": \"Tokyo\"}";
      Map<String, String> outputs = Map.of("step_1", "unused");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"location\": \"Tokyo\"}", result);
    }

    @Test
    @DisplayName("throws ToolPlanException for unresolved step reference")
    void unresolvedReference() {
      String arguments = "{\"data\": \"$ref:missing_step\"}";
      Map<String, String> outputs = Map.of("step_1", "value");

      ToolPlanException ex =
          assertThrows(
              ToolPlanException.class,
              () -> PlanReferenceResolver.resolve(arguments, outputs));
      assertEquals("missing_step", ex.stepId());
      assertTrue(ex.getMessage().contains("unresolved step"));
    }

    @Test
    @DisplayName("returns null for missing field in JSON output")
    void missingFieldReturnsNull() {
      String arguments = "{\"val\": \"$ref:step_1.nonexistent\"}";
      Map<String, String> outputs = Map.of("step_1", "{\"city\": \"Tokyo\"}");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"val\": null}", result);
    }

    @Test
    @DisplayName("throws for field path on non-JSON output")
    void fieldPathOnPlainTextThrows() {
      String arguments = "{\"val\": \"$ref:step_1.field\"}";
      Map<String, String> outputs = Map.of("step_1", "plain text");

      assertThrows(
          ToolPlanException.class,
          () -> PlanReferenceResolver.resolve(arguments, outputs));
    }

    @Test
    @DisplayName("handles empty output string")
    void emptyOutput() {
      String arguments = "{\"data\": \"$ref:step_1\"}";
      Map<String, String> outputs = Map.of("step_1", "");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"data\": \"\"}", result);
    }

    @Test
    @DisplayName("escapes special characters in plain text output")
    void escapesSpecialChars() {
      String arguments = "{\"data\": \"$ref:step_1\"}";
      Map<String, String> outputs = Map.of("step_1", "line1\nline2\ttab \"quoted\"");
      String result = PlanReferenceResolver.resolve(arguments, outputs);
      assertEquals("{\"data\": \"line1\\nline2\\ttab \\\"quoted\\\"\"}", result);
    }
  }

  @Nested
  @DisplayName("extractDependencies()")
  class ExtractDependencies {

    @Test
    @DisplayName("extracts single dependency")
    void singleDep() {
      Set<String> deps =
          PlanReferenceResolver.extractDependencies("{\"data\": \"$ref:step_1\"}");
      assertEquals(Set.of("step_1"), deps);
    }

    @Test
    @DisplayName("extracts multiple dependencies")
    void multipleDeps() {
      Set<String> deps =
          PlanReferenceResolver.extractDependencies(
              "{\"a\": \"$ref:s1\", \"b\": \"$ref:s2\"}");
      assertEquals(Set.of("s1", "s2"), deps);
    }

    @Test
    @DisplayName("extracts dependency from field path ref")
    void fieldPathDep() {
      Set<String> deps =
          PlanReferenceResolver.extractDependencies("{\"val\": \"$ref:step_1.field\"}");
      assertEquals(Set.of("step_1"), deps);
    }

    @Test
    @DisplayName("returns empty set when no references")
    void noDeps() {
      Set<String> deps =
          PlanReferenceResolver.extractDependencies("{\"location\": \"Tokyo\"}");
      assertTrue(deps.isEmpty());
    }

    @Test
    @DisplayName("deduplicates same step referenced multiple times")
    void dedup() {
      Set<String> deps =
          PlanReferenceResolver.extractDependencies(
              "{\"a\": \"$ref:s1\", \"b\": \"$ref:s1.field\"}");
      assertEquals(Set.of("s1"), deps);
    }
  }
}
