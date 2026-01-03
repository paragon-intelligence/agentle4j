package com.paragon.prompts;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.*;

/**
 * Comprehensive tests for the Prompt class.
 * 
 * Tests cover:
 * - Factory methods (of, fromText, empty)
 * - Variable interpolation
 * - Conditional blocks ({{#if}})
 * - Iteration blocks ({{#each}})
 * - Nested property access
 * - Fluent builder compilation
 * - String-like operations (append, substring, charAt, length)
 * - Edge cases and error handling
 */
class PromptTest {

  // ===== Factory Method Tests =====

  @Nested
  class FactoryMethods {

    @Test
    void of_createsPromptWithContent() {
      Prompt prompt = Prompt.of("Hello, World!");
      
      assertEquals("Hello, World!", prompt.content());
      assertEquals("Hello, World!", prompt.text());
      assertFalse(prompt.isCompiled());
    }

    @Test
    void fromText_createsPromptWithContent() {
      Prompt prompt = Prompt.fromText("Test content");
      
      assertEquals("Test content", prompt.content());
      assertFalse(prompt.isCompiled());
    }

    @Test
    void empty_createsEmptyPrompt() {
      Prompt prompt = Prompt.empty();
      
      assertEquals("", prompt.content());
      assertTrue(prompt.isEmpty());
      assertTrue(prompt.isBlank());
    }

    @Test
    void of_nullContent_throwsException() {
      assertThrows(NullPointerException.class, () -> Prompt.of(null));
    }
  }

  // ===== Simple Variable Interpolation Tests =====

  @Nested
  class VariableInterpolation {

    @Test
    void compile_simpleVariable_replacesPlaceholder() {
      Prompt prompt = Prompt.of("Hello, {{name}}!");
      Prompt compiled = prompt.compile(Map.of("name", "World"));
      
      assertEquals("Hello, World!", compiled.content());
      assertTrue(compiled.isCompiled());
    }

    @Test
    void compile_multipleVariables_replacesAll() {
      Prompt prompt = Prompt.of("{{greeting}}, {{name}}! You have {{count}} messages.");
      Prompt compiled = prompt.compile(Map.of(
          "greeting", "Hello",
          "name", "Alice",
          "count", 5
      ));
      
      assertEquals("Hello, Alice! You have 5 messages.", compiled.content());
    }

    @Test
    void compile_missingVariable_leavesPlaceholderInSimpleCompile() {
      Prompt prompt = Prompt.of("Hello, {{name}}!");
      Prompt compiled = prompt.compile(Map.of());
      
      // In simple compile (no control structures), missing vars remain
      assertEquals("Hello, {{name}}!", compiled.content());
    }

    @Test
    void compile_variableWithSpaces_trimsProperly() {
      Prompt prompt = Prompt.of("Hello, {{ name }}!");
      Prompt compiled = prompt.compile(Map.of("name", "World"));
      
      assertEquals("Hello, World!", compiled.content());
    }

    @Test
    void compile_nestedProperty_accessesDeepValue() {
      Prompt prompt = Prompt.of("Hello, {{user.name}}!");
      Prompt compiled = prompt.compile(Map.of(
          "user", Map.of("name", "Alice")
      ));
      
      assertEquals("Hello, Alice!", compiled.content());
    }

    @Test
    void compile_deeplyNestedProperty_accessesValue() {
      Prompt prompt = Prompt.of("City: {{user.address.city}}");
      Prompt compiled = prompt.compile(Map.of(
          "user", Map.of(
              "address", Map.of("city", "New York")
          )
      ));
      
      assertEquals("City: New York", compiled.content());
    }
  }

  // ===== Conditional Block Tests =====

  @Nested
  class ConditionalBlocks {

    @Test
    void compile_ifBlockWithTrueCondition_includesContent() {
      Prompt prompt = Prompt.of("{{#if showGreeting}}Hello{{/if}}, World!");
      Prompt compiled = prompt.compile(Map.of("showGreeting", true));
      
      assertEquals("Hello, World!", compiled.content());
    }

    @Test
    void compile_ifBlockWithFalseCondition_excludesContent() {
      Prompt prompt = Prompt.of("{{#if showGreeting}}Hello, {{/if}}World!");
      Prompt compiled = prompt.compile(Map.of("showGreeting", false));
      
      assertEquals("World!", compiled.content());
    }

    @Test
    void compile_ifBlockWithMissingCondition_excludesContent() {
      Prompt prompt = Prompt.of("{{#if showGreeting}}Hello, {{/if}}World!");
      Prompt compiled = prompt.compile(Map.of());
      
      assertEquals("World!", compiled.content());
    }

    @Test
    void compile_ifBlockWithNonEmptyString_includesContent() {
      Prompt prompt = Prompt.of("{{#if name}}Hello, {{name}}!{{/if}}");
      Prompt compiled = prompt.compile(Map.of("name", "Alice"));
      
      assertEquals("Hello, Alice!", compiled.content());
    }

    @Test
    void compile_ifBlockWithEmptyString_excludesContent() {
      Prompt prompt = Prompt.of("{{#if name}}Hello, {{name}}!{{/if}}");
      Prompt compiled = prompt.compile(Map.of("name", ""));
      
      assertEquals("", compiled.content());
    }

    @Test
    void compile_ifBlockWithNonZeroNumber_includesContent() {
      Prompt prompt = Prompt.of("{{#if count}}Count: {{count}}{{/if}}");
      Prompt compiled = prompt.compile(Map.of("count", 42));
      
      assertEquals("Count: 42", compiled.content());
    }

    @Test
    void compile_ifBlockWithZero_excludesContent() {
      Prompt prompt = Prompt.of("{{#if count}}Count: {{count}}{{/if}}");
      Prompt compiled = prompt.compile(Map.of("count", 0));
      
      assertEquals("", compiled.content());
    }

    @Test
    void compile_ifBlockWithNonEmptyCollection_includesContent() {
      Prompt prompt = Prompt.of("{{#if items}}Has items{{/if}}");
      Prompt compiled = prompt.compile(Map.of("items", List.of("a", "b")));
      
      assertEquals("Has items", compiled.content());
    }

    @Test
    void compile_ifBlockWithEmptyCollection_excludesContent() {
      Prompt prompt = Prompt.of("{{#if items}}Has items{{/if}}");
      Prompt compiled = prompt.compile(Map.of("items", List.of()));
      
      assertEquals("", compiled.content());
    }

    @Test
    void compile_nestedIfBlocks_processesCorrectly() {
      Prompt prompt = Prompt.of(
          "{{#if outer}}Outer{{#if inner}} Inner{{/if}}{{/if}}"
      );
      
      assertEquals("Outer Inner", 
          prompt.compile(Map.of("outer", true, "inner", true)).content());
      assertEquals("Outer", 
          prompt.compile(Map.of("outer", true, "inner", false)).content());
      assertEquals("", 
          prompt.compile(Map.of("outer", false, "inner", true)).content());
    }
  }

  // ===== Iteration Block Tests =====

  @Nested
  class IterationBlocks {

    @Test
    void compile_eachBlockWithList_iteratesItems() {
      Prompt prompt = Prompt.of("Items: {{#each items}}{{this}} {{/each}}");
      Prompt compiled = prompt.compile(Map.of("items", List.of("A", "B", "C")));
      
      assertEquals("Items: A B C ", compiled.content());
    }

    @Test
    void compile_eachBlockWithObjectProperties_accessesProperties() {
      List<Map<String, String>> items = List.of(
          Map.of("name", "Apple"),
          Map.of("name", "Banana")
      );
      
      Prompt prompt = Prompt.of("{{#each items}}- {{this.name}}\n{{/each}}");
      Prompt compiled = prompt.compile(Map.of("items", items));
      
      assertEquals("- Apple\n- Banana\n", compiled.content());
    }

    @Test
    void compile_eachBlockWithEmptyList_producesEmptyResult() {
      Prompt prompt = Prompt.of("Items: {{#each items}}{{this}}{{/each}}");
      Prompt compiled = prompt.compile(Map.of("items", List.of()));
      
      assertEquals("Items: ", compiled.content());
    }

    @Test
    void compile_eachBlockWithNull_producesEmptyResult() {
      Prompt prompt = Prompt.of("Items: {{#each items}}{{this}}{{/each}}");
      Map<String, Object> context = new HashMap<>();
      context.put("items", null);
      Prompt compiled = prompt.compile(context);
      
      assertEquals("Items: ", compiled.content());
    }

    @Test
    void compile_eachBlockWithArray_iteratesElements() {
      Prompt prompt = Prompt.of("{{#each numbers}}{{this}} {{/each}}");
      Prompt compiled = prompt.compile(Map.of("numbers", new Integer[]{1, 2, 3}));
      
      assertEquals("1 2 3 ", compiled.content());
    }

    @Test
    void compile_nestedEachBlocks_processesCorrectly() {
      List<Map<String, Object>> categories = List.of(
          Map.of("name", "Fruits", "items", List.of("Apple", "Banana")),
          Map.of("name", "Vegetables", "items", List.of("Carrot"))
      );
      
      Prompt prompt = Prompt.of(
          "{{#each categories}}{{this.name}}: {{#each this.items}}{{this}} {{/each}}\n{{/each}}"
      );
      Prompt compiled = prompt.compile(Map.of("categories", categories));
      
      assertEquals("Fruits: Apple Banana \nVegetables: Carrot \n", compiled.content());
    }
  }

  // ===== Varargs Compile Tests =====

  @Nested
  class VarargsCompile {

    @Test
    void compile_varargs_simpleKeyValue() {
      Prompt prompt = Prompt.of("Hello, {{name}}!");
      Prompt compiled = prompt.compile("name", "World");
      
      assertEquals("Hello, World!", compiled.content());
    }

    @Test
    void compile_varargs_multipleKeyValues() {
      Prompt prompt = Prompt.of("{{greeting}}, {{name}}! You are {{age}} years old.");
      Prompt compiled = prompt.compile("greeting", "Hi", "name", "Alice", "age", 30);
      
      assertEquals("Hi, Alice! You are 30 years old.", compiled.content());
    }

    @Test
    void compile_varargs_oddNumberOfRest_throwsException() {
      Prompt prompt = Prompt.of("{{a}} {{b}}");
      
      assertThrows(IllegalArgumentException.class, () ->
          prompt.compile("key1", "value1", "key2"));
    }

    @Test
    void compile_varargs_nonStringRestKey_throwsException() {
      Prompt prompt = Prompt.of("{{a}} {{b}}");
      
      assertThrows(IllegalArgumentException.class, () ->
          prompt.compile("key1", "value1", 123, "value2"));
    }

    @Test
    void compile_varargs_nullFirstKey_throwsException() {
      Prompt prompt = Prompt.of("{{name}}");
      
      assertThrows(NullPointerException.class, () ->
          prompt.compile(null, "value"));
    }

    @Test
    void compile_varargs_nullValue_allowed() {
      Prompt prompt = Prompt.of("Value: {{value}}");
      // Note: null values become empty string after compilation
      Prompt compiled = prompt.compile("value", null);
      
      assertTrue(compiled.isCompiled());
    }

    @Test
    void compile_varargs_mixedTypes() {
      Prompt prompt = Prompt.of("{{string}} {{number}} {{bool}}");
      Prompt compiled = prompt.compile(
          "string", "text",
          "number", 42,
          "bool", true
      );
      
      assertEquals("text 42 true", compiled.content());
    }
  }

  // ===== Fluent Builder Tests =====

  @Nested
  class FluentBuilder {

    @Test
    void compile_withBuilder_addsVariables() {
      Prompt compiled = Prompt.of("Hello, {{name}}!")
          .compile()
          .with("name", "World")
          .build();
      
      assertEquals("Hello, World!", compiled.content());
    }

    @Test
    void compile_withBuilder_multipleValues() {
      Prompt compiled = Prompt.of("{{greeting}}, {{name}}!")
          .compile()
          .with("greeting", "Hi")
          .with("name", "Alice")
          .build();
      
      assertEquals("Hi, Alice!", compiled.content());
    }

    @Test
    void compile_withBuilder_withAll() {
      Prompt compiled = Prompt.of("{{greeting}}, {{name}}!")
          .compile()
          .withAll(Map.of("greeting", "Hello", "name", "Bob"))
          .build();
      
      assertEquals("Hello, Bob!", compiled.content());
    }

    @Test
    void compile_withBuilder_buildIf_onlyMatchingVars() {
      Prompt prompt = Prompt.of("Hello, {{name}}!");
      Prompt compiled = prompt.compile()
          .with("name", "World")
          .with("unused", "value")
          .buildIf();
      
      assertEquals("Hello, World!", compiled.content());
    }
  }

  // ===== compileIf Tests =====

  @Nested
  class CompileIfTests {

    @Test
    void compileIf_onlyAppliesMatchingVariables() {
      Prompt prompt = Prompt.of("Hello, {{name}}!");
      Prompt compiled = prompt.compileIf(Map.of(
          "name", "World",
          "unused", "ignored"
      ));
      
      assertEquals("Hello, World!", compiled.content());
    }

    @Test
    void compileIf_withNoMatchingVars_leavesUnchanged() {
      Prompt prompt = Prompt.of("Hello, {{name}}!");
      Prompt compiled = prompt.compileIf(Map.of("other", "value"));
      
      assertEquals("Hello, {{name}}!", compiled.content());
    }
  }

  // ===== extractVariableNames Tests =====

  @Nested
  class ExtractVariableNames {

    @Test
    void extractVariableNames_simpleVariables() {
      Prompt prompt = Prompt.of("{{greeting}}, {{name}}!");
      Set<String> variables = prompt.extractVariableNames();
      
      assertEquals(Set.of("greeting", "name"), variables);
    }

    @Test
    void extractVariableNames_nestedProperties_returnsRootKey() {
      Prompt prompt = Prompt.of("{{user.name}} lives in {{user.address.city}}");
      Set<String> variables = prompt.extractVariableNames();
      
      // Should only contain root keys
      assertEquals(Set.of("user"), variables);
    }

    @Test
    void extractVariableNames_fromIfBlock() {
      Prompt prompt = Prompt.of("{{#if showGreeting}}Hello{{/if}}");
      Set<String> variables = prompt.extractVariableNames();
      
      assertTrue(variables.contains("showGreeting"));
    }

    @Test
    void extractVariableNames_fromEachBlock() {
      Prompt prompt = Prompt.of("{{#each items}}{{this}}{{/each}}");
      Set<String> variables = prompt.extractVariableNames();
      
      assertTrue(variables.contains("items"));
    }

    @Test
    void extractVariableNames_emptyPrompt_returnsEmptySet() {
      Prompt prompt = Prompt.of("No variables here");
      Set<String> variables = prompt.extractVariableNames();
      
      assertTrue(variables.isEmpty());
    }
  }

  // ===== String Operations Tests =====

  @Nested
  class StringOperations {

    @Test
    void append_prompt_concatenatesContent() {
      Prompt p1 = Prompt.of("Hello, ");
      Prompt p2 = Prompt.of("World!");
      Prompt result = p1.append(p2);
      
      assertEquals("Hello, World!", result.content());
      assertFalse(result.isCompiled());
    }

    @Test
    void append_string_concatenatesContent() {
      Prompt prompt = Prompt.of("Hello, ");
      Prompt result = prompt.append("World!");
      
      assertEquals("Hello, World!", result.content());
    }

    @Test
    void substring_withRange_extractsSubstring() {
      Prompt prompt = Prompt.of("Hello, World!");
      Prompt result = prompt.substring(0, 5);
      
      assertEquals("Hello", result.content());
    }

    @Test
    void substring_fromIndex_extractsRemainder() {
      Prompt prompt = Prompt.of("Hello, World!");
      Prompt result = prompt.substring(7);
      
      assertEquals("World!", result.content());
    }

    @Test
    void charAt_returnsCorrectCharacter() {
      Prompt prompt = Prompt.of("Hello");
      
      assertEquals('H', prompt.charAt(0));
      assertEquals('e', prompt.charAt(1));
      assertEquals('o', prompt.charAt(4));
    }

    @Test
    void length_returnsContentLength() {
      Prompt prompt = Prompt.of("Hello, World!");
      
      assertEquals(13, prompt.length());
    }

    @Test
    void isEmpty_withEmptyContent_returnsTrue() {
      assertTrue(Prompt.of("").isEmpty());
      assertFalse(Prompt.of("x").isEmpty());
    }

    @Test
    void isBlank_withWhitespaceOnly_returnsTrue() {
      assertTrue(Prompt.of("   ").isBlank());
      assertTrue(Prompt.of("\n\t").isBlank());
      assertFalse(Prompt.of("x").isBlank());
    }
  }

  // ===== Equals, HashCode, ToString Tests =====

  @Nested
  class ObjectMethods {

    @Test
    void equals_sameContent_returnsTrue() {
      Prompt p1 = Prompt.of("Hello");
      Prompt p2 = Prompt.of("Hello");
      
      assertEquals(p1, p2);
      assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void equals_differentContent_returnsFalse() {
      Prompt p1 = Prompt.of("Hello");
      Prompt p2 = Prompt.of("World");
      
      assertNotEquals(p1, p2);
    }

    @Test
    void equals_sameContentDifferentCompiledState_returnsFalse() {
      Prompt p1 = Prompt.of("Hello, {{name}}!");
      Prompt p2 = p1.compile(Map.of("name", "World"));
      Prompt p3 = Prompt.of("Hello, World!");
      
      // p2 and p3 have same content but p2 is compiled, p3 is not
      assertNotEquals(p2, p3);
    }

    @Test
    void equals_sameInstance_returnsTrue() {
      Prompt prompt = Prompt.of("Hello");
      assertEquals(prompt, prompt);
    }

    @Test
    void equals_null_returnsFalse() {
      Prompt prompt = Prompt.of("Hello");
      assertNotEquals(null, prompt);
    }

    @Test
    void toString_returnsContent() {
      Prompt prompt = Prompt.of("Hello, World!");
      assertEquals("Hello, World!", prompt.toString());
    }
  }

  // ===== Edge Cases and Error Handling =====

  @Nested
  class EdgeCases {

    @Test
    void compile_nullContext_throwsException() {
      Prompt prompt = Prompt.of("Hello");
      assertThrows(NullPointerException.class, () -> prompt.compile(null));
    }

    @Test
    void compile_specialCharactersInReplacement_handlesCorrectly() {
      Prompt prompt = Prompt.of("Result: {{value}}");
      Prompt compiled = prompt.compile(Map.of("value", "$100 for item #1"));
      
      assertEquals("Result: $100 for item #1", compiled.content());
    }

    @Test
    void compile_regexMetaCharacters_handlesCorrectly() {
      Prompt prompt = Prompt.of("Pattern: {{pattern}}");
      Prompt compiled = prompt.compile(Map.of("pattern", "a*b+c?"));
      
      assertEquals("Pattern: a*b+c?", compiled.content());
    }

    @Test
    void compile_backslashesInValue_handlesCorrectly() {
      Prompt prompt = Prompt.of("Path: {{path}}");
      Prompt compiled = prompt.compile(Map.of("path", "C:\\Users\\Test"));
      
      assertEquals("Path: C:\\Users\\Test", compiled.content());
    }

    @Test
    void compile_multilineContent_preservesLineBreaks() {
      Prompt prompt = Prompt.of("Line 1: {{a}}\nLine 2: {{b}}\nLine 3: {{c}}");
      Prompt compiled = prompt.compile(Map.of("a", "A", "b", "B", "c", "C"));
      
      assertEquals("Line 1: A\nLine 2: B\nLine 3: C", compiled.content());
    }

    @Test
    void compile_ifBlockWithMultilineContent_preservesLineBreaks() {
      Prompt prompt = Prompt.of("{{#if show}}Line 1\nLine 2\nLine 3{{/if}}");
      Prompt compiled = prompt.compile(Map.of("show", true));
      
      assertEquals("Line 1\nLine 2\nLine 3", compiled.content());
    }

    @Test
    void append_null_throwsException() {
      Prompt prompt = Prompt.of("Hello");
      assertThrows(NullPointerException.class, () -> prompt.append((Prompt) null));
      assertThrows(NullPointerException.class, () -> prompt.append((String) null));
    }
  }

  // ===== Bean Property Access Tests =====

  @Nested
  class BeanPropertyAccess {

    @Test
    void compile_recordProperty_accessesViaMethod() {
      record Person(String name, int age) {}
      
      Prompt prompt = Prompt.of("Name: {{person.name}}, Age: {{person.age}}");
      Prompt compiled = prompt.compile(Map.of("person", new Person("Alice", 30)));
      
      assertEquals("Name: Alice, Age: 30", compiled.content());
    }

    @Test
    void compile_javaBean_accessesViaGetter() {
      // Using a simple inner class with getters
      class User {
        private final String name;
        User(String name) { this.name = name; }
        public String getName() { return name; }
      }
      
      Prompt prompt = Prompt.of("User: {{user.name}}");
      Prompt compiled = prompt.compile(Map.of("user", new User("Bob")));
      
      assertEquals("User: Bob", compiled.content());
    }
  }

  // ===== TemplateException Tests =====

  @Nested
  class TemplateExceptionTests {

    @Test
    void templateException_hasMessage() {
      Prompt.TemplateException ex = new Prompt.TemplateException("Test error");
      assertEquals("Test error", ex.getMessage());
    }

    @Test
    void templateException_hasCause() {
      Exception cause = new RuntimeException("Original");
      Prompt.TemplateException ex = new Prompt.TemplateException("Wrapped", cause);
      
      assertEquals("Wrapped", ex.getMessage());
      assertEquals(cause, ex.getCause());
    }
  }

  // ===== String-Like Operations Extended Tests =====

  @Nested
  class StringOperationsExtended {

    @Test
    void substring_extractsPortion() {
      Prompt prompt = Prompt.of("Hello, World!");
      Prompt sub = prompt.substring(0, 5);
      assertEquals("Hello", sub.content());
    }

    @Test
    void charAt_returnsCorrectCharacter() {
      Prompt prompt = Prompt.of("Hello");
      assertEquals('e', prompt.charAt(1));
    }

    @Test
    void length_returnsCorrectLength() {
      Prompt prompt = Prompt.of("Test");
      assertEquals(4, prompt.length());
    }

    @Test
    void isEmpty_returnsTrueForEmpty() {
      Prompt prompt = Prompt.empty();
      assertTrue(prompt.isEmpty());
    }

    @Test
    void isEmpty_returnsFalseForNonEmpty() {
      Prompt prompt = Prompt.of("Content");
      assertFalse(prompt.isEmpty());
    }

    @Test
    void append_combinesPrompts() {
      Prompt p1 = Prompt.of("Hello ");
      Prompt p2 = Prompt.of("World");
      Prompt combined = p1.append(p2);
      assertEquals("Hello World", combined.content());
    }

    @Test
    void append_combinesWithString() {
      Prompt prompt = Prompt.of("Hello ");
      Prompt combined = prompt.append("World");
      assertEquals("Hello World", combined.content());
    }
  }

  // ===== Extract Variable Names Extended Tests =====

  @Nested
  class ExtractVariableNamesExtendedTests {

    @Test
    void extractVariableNames_findsAllVariables() {
      Prompt prompt = Prompt.of("{{greeting}}, {{name}}! You have {{count}} items.");
      var names = prompt.extractVariableNames();
      
      assertTrue(names.contains("greeting"));
      assertTrue(names.contains("name"));
      assertTrue(names.contains("count"));
      assertEquals(3, names.size());
    }

    @Test
    void extractVariableNames_handlesNestedProperties() {
      Prompt prompt = Prompt.of("{{user.name}} - {{user.address.city}}");
      var names = prompt.extractVariableNames();
      
      // Should extract root keys
      assertTrue(names.contains("user"));
    }

    @Test
    void extractVariableNames_returnsEmptyForNoVariables() {
      Prompt prompt = Prompt.of("No variables here");
      var names = prompt.extractVariableNames();
      
      assertTrue(names.isEmpty());
    }
  }
}
