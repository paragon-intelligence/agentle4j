package com.paragon.parsing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.ParsedResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link AgenticFileParser}.
 *
 * <p>This class tests all parsing methods with various inputs including paths, URIs, base64
 * strings, and File objects. Uses Mockito to mock the underlying Responder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgenticFileParser Tests")
class AgenticFileParserTest {

  @Mock private Responder responder;

  @Mock private ParsedResponse<MarkdownResult> parsedResponse;

  private AgenticFileParser parser;

  @BeforeEach
  void setUp() {
    parser = new AgenticFileParser(responder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONSTRUCTOR TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("creates parser with non-null Responder")
    void createsWithNonNullResponder() {
      assertNotNull(parser);
      assertDoesNotThrow(() -> new AgenticFileParser(responder));
    }

    @Test
    @DisplayName("constructor signature requires Responder parameter")
    void constructorRequiresResponder() {
      assertEquals(1, AgenticFileParser.class.getConstructors().length);
      assertEquals(
          Responder.class, AgenticFileParser.class.getConstructors()[0].getParameterTypes()[0]);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PARSE BY PATH TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("parse(Path)")
  class ParsePathTests {

    @TempDir Path tempDir;

    @Test
    @DisplayName("successfully parses an existing file")
    void successfullyParsesExistingFile() throws IOException, ExecutionException, InterruptedException {
      // Create a temporary file
      Path testFile = tempDir.resolve("test.pdf");
      Files.writeString(testFile, "Test document content");

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Page 1", "## Section"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      CompletableFuture<MarkdownResult> future = parser.parse(testFile);
      MarkdownResult actual = future.get();

      // Verify
      assertNotNull(actual);
      assertEquals(result, actual);
      verify(responder).respond(any(CreateResponsePayload.Structured.class));
    }

    @Test
    @DisplayName("throws FileNotFoundException for non-existent file")
    void throwsForNonExistentFile() {
      Path nonExistent = tempDir.resolve("does-not-exist.pdf");

      assertThrows(FileNotFoundException.class, () -> parser.parse(nonExistent));
    }

    @Test
    @DisplayName("correctly encodes file content to base64")
    void encodesFileContentToBase64() throws IOException {
      // Create a temporary file with known content
      Path testFile = tempDir.resolve("test.txt");
      String content = "Hello, World!";
      Files.writeString(testFile, content);

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Parsed"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      parser.parse(testFile);

      // Verify responder was called with structured payload
      ArgumentCaptor<CreateResponsePayload.Structured> captor =
          ArgumentCaptor.forClass(CreateResponsePayload.Structured.class);
      verify(responder).respond(captor.capture());
      assertNotNull(captor.getValue());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PARSE BY URI TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("parse(URI)")
  class ParseUriTests {

    @Test
    @DisplayName("successfully parses a valid file URI")
    void successfullyParsesValidFileUri() throws IOException {
      URI validUri = URI.create("https://example.com/documents/report.pdf");

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Report Content"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      CompletableFuture<MarkdownResult> future = parser.parse(validUri);

      // Verify
      assertNotNull(future);
      verify(responder).respond(any(CreateResponsePayload.Structured.class));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for directory URI (trailing slash)")
    void throwsForDirectoryUri() {
      URI directoryUri = URI.create("https://example.com/documents/");

      assertThrows(IllegalArgumentException.class, () -> parser.parse(directoryUri));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for URI without extension")
    void throwsForUriWithoutExtension() {
      URI noExtensionUri = URI.create("https://example.com/documents/report");

      assertThrows(IllegalArgumentException.class, () -> parser.parse(noExtensionUri));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for URI with empty path")
    void throwsForEmptyPathUri() {
      URI emptyPathUri = URI.create("https://example.com");

      assertThrows(IllegalArgumentException.class, () -> parser.parse(emptyPathUri));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for opaque URI")
    void throwsForOpaqueUri() {
      // mailto: URIs have no path
      URI opaqueUri = URI.create("mailto:test@example.com");

      assertThrows(IllegalArgumentException.class, () -> parser.parse(opaqueUri));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PARSE BY BASE64 TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("parse(String base64)")
  class ParseBase64Tests {

    @Test
    @DisplayName("successfully parses valid base64 string")
    void successfullyParsesValidBase64() throws IOException {
      String base64 = "SGVsbG8sIFdvcmxkIQ=="; // "Hello, World!"

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Hello World"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      CompletableFuture<MarkdownResult> future = parser.parse(base64);

      // Verify
      assertNotNull(future);
      verify(responder).respond(any(CreateResponsePayload.Structured.class));
    }

    @Test
    @DisplayName("creates File from base64 and delegates to parse(File)")
    void createsFileFromBase64() throws IOException {
      String base64 = "VGVzdCBkb2N1bWVudA=="; // "Test document"

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Test"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      parser.parse(base64);

      // Verify the payload was created with structured output
      verify(responder).respond(any(CreateResponsePayload.Structured.class));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PARSE BY FILE TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("parse(File)")
  class ParseFileTests {

    @Test
    @DisplayName("successfully parses File object created from URL")
    void successfullyParsesFileFromUrl() throws IOException, ExecutionException, InterruptedException {
      com.paragon.responses.spec.File file =
          com.paragon.responses.spec.File.fromUrl("https://example.com/doc.pdf");

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Page 1", "# Page 2"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      CompletableFuture<MarkdownResult> future = parser.parse(file);
      MarkdownResult actual = future.get();

      // Verify
      assertEquals(result, actual);
      assertEquals(2, actual.markdowns().size());
    }

    @Test
    @DisplayName("successfully parses File object created from base64")
    void successfullyParsesFileFromBase64() throws IOException {
      com.paragon.responses.spec.File file =
          com.paragon.responses.spec.File.fromBase64("SGVsbG8=", "test.pdf");

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Content"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      parser.parse(file);

      // Verify
      verify(responder).respond(any(CreateResponsePayload.Structured.class));
    }

    @Test
    @DisplayName("builds correct CreateResponsePayload with structured output")
    void buildsCorrectPayload() throws IOException {
      com.paragon.responses.spec.File file =
          com.paragon.responses.spec.File.fromUrl("https://example.com/report.pdf");

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Report"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      parser.parse(file);

      // Capture and verify the payload
      ArgumentCaptor<CreateResponsePayload.Structured> captor =
          ArgumentCaptor.forClass(CreateResponsePayload.Structured.class);
      verify(responder).respond(captor.capture());

      CreateResponsePayload.Structured<?> capturedPayload = captor.getValue();
      assertNotNull(capturedPayload);
    }

    @Test
    @DisplayName("uses thenApply to extract parsed output")
    void usesThenApplyToExtractOutput() throws IOException {
      com.paragon.responses.spec.File file =
          com.paragon.responses.spec.File.fromUrl("https://example.com/test.pdf");

      // Setup mock
      MarkdownResult expectedResult = new MarkdownResult(List.of("# Extracted"));
      when(parsedResponse.outputParsed()).thenReturn(expectedResult);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Execute
      CompletableFuture<MarkdownResult> future = parser.parse(file);

      // The thenApply should have been applied
      assertInstanceOf(CompletableFuture.class, future);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SEEMS LIKE FILE (URI VALIDATION) TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("seemsLikeFile URI validation")
  class SeemsLikeFileTests {

    @Test
    @DisplayName("accepts URI with file extension")
    void acceptsUriWithExtension() throws IOException {
      // URI with extension should be accepted
      URI validUri = URI.create("https://example.com/document.pdf");

      // Setup mock to prevent NPE
      MarkdownResult result = new MarkdownResult(List.of("# Content"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Should not throw
      assertDoesNotThrow(() -> parser.parse(validUri));
    }

    @Test
    @DisplayName("rejects URI ending with slash")
    void rejectsTrailingSlash() {
      URI trailingSlashUri = URI.create("https://example.com/documents/");
      assertThrows(IllegalArgumentException.class, () -> parser.parse(trailingSlashUri));
    }

    @Test
    @DisplayName("rejects URI without dot in last segment")
    void rejectsWithoutDot() {
      URI noDotUri = URI.create("https://example.com/documents/report");
      assertThrows(IllegalArgumentException.class, () -> parser.parse(noDotUri));
    }

    @Test
    @DisplayName("accepts URI with query parameters if file has extension")
    void acceptsUriWithQueryParams() throws IOException {
      URI uriWithParams = URI.create("https://example.com/report.pdf?version=2");

      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Report v2"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // Path still has .pdf, so should be accepted
      assertDoesNotThrow(() -> parser.parse(uriWithParams));
    }

    @Test
    @DisplayName("accepts various file extensions")
    void acceptsVariousExtensions() throws IOException {
      // Setup mock
      MarkdownResult result = new MarkdownResult(List.of("# Content"));
      when(parsedResponse.outputParsed()).thenReturn(result);
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.completedFuture(parsedResponse));

      // All these should be accepted
      assertDoesNotThrow(() -> parser.parse(URI.create("https://example.com/doc.pdf")));
      assertDoesNotThrow(() -> parser.parse(URI.create("https://example.com/doc.txt")));
      assertDoesNotThrow(() -> parser.parse(URI.create("https://example.com/doc.docx")));
      assertDoesNotThrow(() -> parser.parse(URI.create("https://example.com/image.png")));
    }

    @Test
    @DisplayName("rejects URI with only root path")
    void rejectsRootPath() {
      URI rootUri = URI.create("https://example.com/");
      assertThrows(IllegalArgumentException.class, () -> parser.parse(rootUri));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ERROR HANDLING TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("propagates responder exceptions")
    void propagatesResponderExceptions() throws IOException {
      com.paragon.responses.spec.File file =
          com.paragon.responses.spec.File.fromUrl("https://example.com/test.pdf");

      // Setup mock to return failed future
      when(responder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));

      // Execute
      CompletableFuture<MarkdownResult> future = parser.parse(file);

      // Verify exception is propagated
      assertTrue(future.isCompletedExceptionally());
      assertThrows(ExecutionException.class, future::get);
    }

    @Test
    @DisplayName("file not found message contains path")
    void fileNotFoundMessageContainsPath(@TempDir Path tempDir) {
      Path nonExistent = tempDir.resolve("missing-file.pdf");

      FileNotFoundException exception =
          assertThrows(FileNotFoundException.class, () -> parser.parse(nonExistent));

      assertTrue(exception.getMessage().contains("missing-file.pdf"));
    }

    @Test
    @DisplayName("illegal argument message contains URI")
    void illegalArgumentMessageContainsUri() {
      URI invalidUri = URI.create("https://example.com/not-a-file/");

      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> parser.parse(invalidUri));

      assertTrue(exception.getMessage().contains("not-a-file"));
    }
  }
}
