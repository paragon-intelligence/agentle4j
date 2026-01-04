package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for ExtractionResult classes. */
class ExtractionResultTest {

  private static final URI TEST_URI = URI.create("https://example.com");
  private static final ExtractionPreferences DEFAULT_PREFS = ExtractionPreferences.defaults();

  // ===== Basic ExtractionResult Tests =====

  @Test
  void constructor_createsImmutableLists() {
    List<URI> urls = List.of(TEST_URI);
    List<String> html = List.of("<html></html>");
    List<String> markdown = List.of("# Title");
    List<ExtractionResult.ExtractionError> errors = List.of();

    ExtractionResult result =
        new ExtractionResult(urls, html, markdown, "# Title", DEFAULT_PREFS, errors);

    assertEquals(1, result.urls().size());
    assertEquals(1, result.html().size());
    assertEquals(1, result.markdown().size());
    assertEquals("# Title", result.combinedMarkdown());
    assertEquals(DEFAULT_PREFS, result.extractionPreferences());
    assertTrue(result.errors().isEmpty());
  }

  @Test
  void isSuccessful_withMarkdownAndNoErrors_returnsTrue() {
    ExtractionResult result =
        new ExtractionResult(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Title"),
            "# Title",
            DEFAULT_PREFS,
            List.of());

    assertTrue(result.isSuccessful());
    assertFalse(result.hasErrors());
  }

  @Test
  void isSuccessful_withErrors_returnsFalse() {
    ExtractionResult.ExtractionError error =
        ExtractionResult.ExtractionError.navigation(TEST_URI, "Navigation failed", null);

    ExtractionResult result =
        new ExtractionResult(
            List.of(TEST_URI), List.of(), List.of(), "", DEFAULT_PREFS, List.of(error));

    assertFalse(result.isSuccessful());
    assertTrue(result.hasErrors());
  }

  @Test
  void isSuccessful_withEmptyMarkdown_returnsFalse() {
    ExtractionResult result =
        new ExtractionResult(
            List.of(TEST_URI), List.of("<html></html>"), List.of(), "", DEFAULT_PREFS, List.of());

    assertFalse(result.isSuccessful());
  }

  // ===== ExtractionError Tests =====

  @Test
  void extractionError_navigation_createsCorrectType() {
    ExtractionResult.ExtractionError error =
        ExtractionResult.ExtractionError.navigation(TEST_URI, "Page not found", null);

    assertEquals(TEST_URI, error.url());
    assertEquals(ExtractionResult.ErrorType.NAVIGATION, error.type());
    assertEquals("Page not found", error.message());
    assertNull(error.cause());
  }

  @Test
  void extractionError_timeout_createsCorrectType() {
    ExtractionResult.ExtractionError error =
        ExtractionResult.ExtractionError.timeout(TEST_URI, "Request timed out", null);

    assertEquals(ExtractionResult.ErrorType.TIMEOUT, error.type());
  }

  @Test
  void extractionError_parsing_createsCorrectType() {
    Exception cause = new RuntimeException("Parse error");
    ExtractionResult.ExtractionError error =
        ExtractionResult.ExtractionError.parsing(TEST_URI, "Failed to parse HTML", cause);

    assertEquals(ExtractionResult.ErrorType.PARSING, error.type());
    assertEquals(cause, error.cause());
  }

  @Test
  void extractionError_llm_createsCorrectType() {
    ExtractionResult.ExtractionError error =
        ExtractionResult.ExtractionError.llm(TEST_URI, "LLM processing failed", null);

    assertEquals(ExtractionResult.ErrorType.LLM_PROCESSING, error.type());
  }

  @Test
  void extractionError_nullUrl_throwsException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new ExtractionResult.ExtractionError(
                null, ExtractionResult.ErrorType.UNKNOWN, "message", null));
  }

  @Test
  void extractionError_nullType_throwsException() {
    assertThrows(
        NullPointerException.class,
        () -> new ExtractionResult.ExtractionError(TEST_URI, null, "message", null));
  }

  @Test
  void extractionError_nullMessage_throwsException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new ExtractionResult.ExtractionError(
                TEST_URI, ExtractionResult.ErrorType.UNKNOWN, null, null));
  }

  // ===== ErrorType Enum Tests =====

  @Test
  void errorType_hasAllExpectedValues() {
    ExtractionResult.ErrorType[] types = ExtractionResult.ErrorType.values();

    assertEquals(5, types.length);
    assertNotNull(ExtractionResult.ErrorType.NAVIGATION);
    assertNotNull(ExtractionResult.ErrorType.TIMEOUT);
    assertNotNull(ExtractionResult.ErrorType.PARSING);
    assertNotNull(ExtractionResult.ErrorType.LLM_PROCESSING);
    assertNotNull(ExtractionResult.ErrorType.UNKNOWN);
  }

  // ===== Structured Result Tests =====

  @Test
  void structured_withOutput_isSuccessful() {
    String output = "Extracted data";

    ExtractionResult.Structured<String> result =
        new ExtractionResult.Structured<>(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Content"),
            "# Content",
            DEFAULT_PREFS,
            List.of(),
            output,
            String.class);

    assertTrue(result.isSuccessful());
    assertEquals(output, result.output());
    assertEquals(output, result.requireOutput());
    assertEquals(String.class, result.outputType());
  }

  @Test
  void structured_withNullOutput_isNotSuccessful() {
    ExtractionResult.Structured<String> result =
        new ExtractionResult.Structured<>(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Content"),
            "# Content",
            DEFAULT_PREFS,
            List.of(),
            null,
            String.class);

    assertFalse(result.isSuccessful());
    assertNull(result.output());
  }

  @Test
  void structured_requireOutput_withNullOutput_throwsException() {
    ExtractionResult.Structured<String> result =
        new ExtractionResult.Structured<>(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Content"),
            "# Content",
            DEFAULT_PREFS,
            List.of(),
            null,
            String.class);

    assertThrows(IllegalStateException.class, result::requireOutput);
  }

  // ===== Equals, HashCode, ToString Tests =====

  @Test
  void equals_sameValues_returnsTrue() {
    ExtractionResult result1 =
        new ExtractionResult(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Content"),
            "# Content",
            DEFAULT_PREFS,
            List.of());

    ExtractionResult result2 =
        new ExtractionResult(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Content"),
            "# Content",
            DEFAULT_PREFS,
            List.of());

    assertEquals(result1, result2);
    assertEquals(result1.hashCode(), result2.hashCode());
  }

  @Test
  void toString_containsRelevantInfo() {
    ExtractionResult result =
        new ExtractionResult(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Content"),
            "# Content",
            DEFAULT_PREFS,
            List.of());

    String str = result.toString();

    assertNotNull(str);
    assertTrue(str.contains("ExtractionResult"));
  }

  @Test
  void structured_toString_containsOutputType() {
    ExtractionResult.Structured<String> result =
        new ExtractionResult.Structured<>(
            List.of(TEST_URI),
            List.of("<html></html>"),
            List.of("# Content"),
            "# Content",
            DEFAULT_PREFS,
            List.of(),
            "output",
            String.class);

    String str = result.toString();

    assertNotNull(str);
    assertTrue(str.contains("Structured"));
    assertTrue(str.contains("String"));
  }
}
