package com.paragon.parsing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the parsing package classes: MarkdownResult and AgenticFileParser. */
class ParsingTest {

  @Nested
  @DisplayName("MarkdownResult record")
  class MarkdownResultTests {

    @Test
    @DisplayName("MarkdownResult can be instantiated with a list of markdowns")
    void markdownResultInstantiation() {
      MarkdownResult result = new MarkdownResult(List.of("# Heading", "Some content"));
      assertNotNull(result);
      assertEquals(2, result.markdowns().size());
    }

    @Test
    @DisplayName("MarkdownResult implements record equality")
    void markdownResultEquality() {
      MarkdownResult result1 = new MarkdownResult(List.of("# Heading"));
      MarkdownResult result2 = new MarkdownResult(List.of("# Heading"));
      assertEquals(result1, result2);
      assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("MarkdownResult with different content are not equal")
    void markdownResultInequality() {
      MarkdownResult result1 = new MarkdownResult(List.of("# Heading 1"));
      MarkdownResult result2 = new MarkdownResult(List.of("# Heading 2"));
      assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("MarkdownResult toString concatenates all markdowns")
    void markdownResultToString() {
      MarkdownResult result = new MarkdownResult(List.of("# Page 1\n", "# Page 2\n"));
      String expected = "# Page 1\n# Page 2\n";
      assertEquals(expected, result.toString());
    }

    @Test
    @DisplayName("MarkdownResult with empty list returns empty string")
    void markdownResultEmptyList() {
      MarkdownResult result = new MarkdownResult(List.of());
      assertEquals("", result.toString());
      assertTrue(result.markdowns().isEmpty());
    }
  }

  @Nested
  @DisplayName("AgenticFileParser class")
  class AgenticFileParserTests {

    @Test
    @DisplayName("AgenticFileParser constructor accepts a Responder")
    void agenticFileParserConstructor() {
      // Verify AgenticFileParser constructor signature requires a Responder parameter.
      // The constructor takes @NonNull Responder, so we verify the class exists
      // and has the expected constructor signature via compilation.
      // A full integration test would require a real or mocked Responder.
      assertNotNull(AgenticFileParser.class);
      assertEquals(1, AgenticFileParser.class.getConstructors().length);
      assertEquals(
          com.paragon.responses.Responder.class,
          AgenticFileParser.class.getConstructors()[0].getParameterTypes()[0]);
    }
  }
}
