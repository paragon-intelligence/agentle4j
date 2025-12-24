package com.paragon.parsing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the parsing package classes: File, ParsedFile, and ParsingOptions.
 */
class ParsingTest {

  @Nested
  @DisplayName("File record")
  class FileRecordTests {

    @Test
    @DisplayName("File can be instantiated")
    void fileInstantiation() {
      File file = new File();
      assertNotNull(file);
    }

    @Test
    @DisplayName("File implements record equality")
    void fileEquality() {
      File file1 = new File();
      File file2 = new File();
      assertEquals(file1, file2);
      assertEquals(file1.hashCode(), file2.hashCode());
    }
  }

  @Nested
  @DisplayName("ParsedFile record")
  class ParsedFileRecordTests {

    @Test
    @DisplayName("ParsedFile can be instantiated")
    void parsedFileInstantiation() {
      ParsedFile parsedFile = new ParsedFile();
      assertNotNull(parsedFile);
    }

    @Test
    @DisplayName("ParsedFile implements record equality")
    void parsedFileEquality() {
      ParsedFile parsed1 = new ParsedFile();
      ParsedFile parsed2 = new ParsedFile();
      assertEquals(parsed1, parsed2);
      assertEquals(parsed1.hashCode(), parsed2.hashCode());
    }
  }

  @Nested
  @DisplayName("ParsingOptions record")
  class ParsingOptionsTests {

    @Test
    @DisplayName("ParsingOptions can be instantiated")
    void parsingOptionsInstantiation() {
      ParsingOptions options = new ParsingOptions();
      assertNotNull(options);
    }

    @Test
    @DisplayName("withDefaultOptions factory method returns instance")
    void withDefaultOptions() {
      ParsingOptions options = ParsingOptions.withDefaultOptions();
      assertNotNull(options);
    }

    @Test
    @DisplayName("withDefaultOptions returns equal instances")
    void withDefaultOptionsEquality() {
      ParsingOptions options1 = ParsingOptions.withDefaultOptions();
      ParsingOptions options2 = ParsingOptions.withDefaultOptions();
      assertEquals(options1, options2);
      assertEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    @DisplayName("direct and factory instances are equal")
    void directAndFactoryEqual() {
      ParsingOptions direct = new ParsingOptions();
      ParsingOptions factory = ParsingOptions.withDefaultOptions();
      assertEquals(direct, factory);
    }
  }

  @Nested
  @DisplayName("FileParser interface")
  class FileParserTests {

    @Test
    @DisplayName("FileParser default method calls parameterized parse")
    void defaultParseMethod() {
      // Create a mock FileParser that tracks calls
      final boolean[] parseWithOptionsCalled = {false};
      final ParsingOptions[] receivedOptions = {null};

      FileParser parser = new FileParser() {
        @Override
        public ParsedFile parse(File file, ParsingOptions options) {
          parseWithOptionsCalled[0] = true;
          receivedOptions[0] = options;
          return new ParsedFile();
        }
      };

      File file = new File();
      ParsedFile result = parser.parse(file);

      assertNotNull(result);
      assertTrue(parseWithOptionsCalled[0], "Parameterized parse should be called");
      assertNotNull(receivedOptions[0], "Options should not be null");
      assertEquals(ParsingOptions.withDefaultOptions(), receivedOptions[0]);
    }
  }
}
