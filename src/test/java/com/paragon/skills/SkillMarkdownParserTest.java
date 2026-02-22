package com.paragon.skills;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the SkillMarkdownParser class. */
@DisplayName("SkillMarkdownParser")
class SkillMarkdownParserTest {

  private SkillMarkdownParser parser;

  @BeforeEach
  void setUp() {
    parser = new SkillMarkdownParser();
  }

  @Nested
  @DisplayName("Valid Parsing")
  class ValidParsing {

    @Test
    @DisplayName("parses valid SKILL.md with name and description")
    void parse_validSkillMd() {
      String content =
          """
          ---
          name: pdf-processor
          description: Process PDF files, extract text, fill forms.
          ---

          # PDF Processing

          You are a PDF processing expert.
          """;

      Skill skill = parser.parse(content);

      assertEquals("pdf-processor", skill.name());
      assertEquals("Process PDF files, extract text, fill forms.", skill.description());
      assertTrue(skill.instructions().text().contains("PDF processing expert"));
    }

    @Test
    @DisplayName("parses quoted name and description")
    void parse_quotedValues() {
      String content =
          """
          ---
          name: "my-skill"
          description: "A skill with quoted values"
          ---

          Instructions here.
          """;

      Skill skill = parser.parse(content);

      assertEquals("my-skill", skill.name());
      assertEquals("A skill with quoted values", skill.description());
    }

    @Test
    @DisplayName("parses single-quoted values")
    void parse_singleQuotedValues() {
      String content =
          """
          ---
          name: 'another-skill'
          description: 'Single quoted description'
          ---

          Instructions.
          """;

      Skill skill = parser.parse(content);

      assertEquals("another-skill", skill.name());
      assertEquals("Single quoted description", skill.description());
    }

    @Test
    @DisplayName("preserves markdown formatting in instructions")
    void parse_preservesMarkdownFormatting() {
      String content =
          """
          ---
          name: test-skill
          description: Test skill
          ---

          # Heading

          - List item 1
          - List item 2

          ```python
          print("code block")
          ```
          """;

      Skill skill = parser.parse(content);

      String instructions = skill.instructions().text();
      assertTrue(instructions.contains("# Heading"));
      assertTrue(instructions.contains("- List item 1"));
      assertTrue(instructions.contains("```python"));
    }
  }

  @Nested
  @DisplayName("Invalid Parsing")
  class InvalidParsing {

    @Test
    @DisplayName("throws when content is empty")
    void parse_throwsWhenEmpty() {
      assertThrows(
          SkillProviderException.class,
          () -> {
            parser.parse("");
          });
    }

    @Test
    @DisplayName("throws when content is blank")
    void parse_throwsWhenBlank() {
      assertThrows(
          SkillProviderException.class,
          () -> {
            parser.parse("   \n   ");
          });
    }

    @Test
    @DisplayName("throws when missing frontmatter")
    void parse_throwsWhenMissingFrontmatter() {
      String content =
          """
          # No Frontmatter

          Just markdown content.
          """;

      assertThrows(
          SkillProviderException.class,
          () -> {
            parser.parse(content);
          });
    }

    @Test
    @DisplayName("throws when missing name")
    void parse_throwsWhenMissingName() {
      String content =
          """
          ---
          description: A skill without name
          ---

          Instructions.
          """;

      assertThrows(
          SkillProviderException.class,
          () -> {
            parser.parse(content);
          });
    }

    @Test
    @DisplayName("throws when missing description")
    void parse_throwsWhenMissingDescription() {
      String content =
          """
          ---
          name: no-description
          ---

          Instructions.
          """;

      assertThrows(
          SkillProviderException.class,
          () -> {
            parser.parse(content);
          });
    }

    @Test
    @DisplayName("throws when no content after frontmatter")
    void parse_throwsWhenNoContent() {
      String content =
          """
          ---
          name: empty-skill
          description: A skill with no content
          ---
          """;

      assertThrows(
          SkillProviderException.class,
          () -> {
            parser.parse(content);
          });
    }
  }

  @Nested
  @DisplayName("Parse with Skill ID")
  class ParseWithSkillId {

    @Test
    @DisplayName("includes skillId in error message")
    void parse_includesSkillIdInError() {
      String content =
          """
          ---
          description: Missing name
          ---
          Content.
          """;

      SkillProviderException ex =
          assertThrows(
              SkillProviderException.class,
              () -> {
                parser.parse(content, "my-skill-id");
              });

      assertTrue(ex.getMessage().contains("my-skill-id"));
    }
  }

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("isValid returns true for valid content")
    void isValid_returnsTrueForValid() {
      String content =
          """
          ---
          name: valid-skill
          description: A valid skill
          ---

          Instructions.
          """;

      assertTrue(parser.isValid(content));
    }

    @Test
    @DisplayName("isValid returns false for invalid content")
    void isValid_returnsFalseForInvalid() {
      assertFalse(parser.isValid(""));
      assertFalse(parser.isValid("no frontmatter"));
      assertFalse(parser.isValid("---\n---"));
    }

    @Test
    @DisplayName("isValid returns false when missing required fields")
    void isValid_returnsFalseWhenMissingFields() {
      String noName =
          """
          ---
          description: Missing name
          ---
          Content.
          """;

      String noDescription =
          """
          ---
          name: missing-description
          ---
          Content.
          """;

      assertFalse(parser.isValid(noName));
      assertFalse(parser.isValid(noDescription));
    }
  }
}
