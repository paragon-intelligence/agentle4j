package com.paragon.prompts;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link FilesystemPromptProvider}. */
class FilesystemPromptProviderTest {

  @TempDir Path tempDir;

  private FilesystemPromptProvider provider;

  @BeforeEach
  void setUp() {
    provider = FilesystemPromptProvider.create(tempDir);
  }

  // ===== Factory Method Tests =====

  @Nested
  class FactoryMethods {

    @Test
    void create_withPath_returnsProvider() {
      FilesystemPromptProvider p = FilesystemPromptProvider.create(tempDir);
      assertNotNull(p);
      assertEquals(tempDir, p.baseDirectory());
    }

    @Test
    void create_withString_returnsProvider() {
      FilesystemPromptProvider p = FilesystemPromptProvider.create(tempDir.toString());
      assertNotNull(p);
      assertEquals(tempDir, p.baseDirectory());
    }

    @Test
    void create_nullPath_throwsNullPointerException() {
      assertThrows(NullPointerException.class, () -> FilesystemPromptProvider.create((Path) null));
    }

    @Test
    void create_nullString_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> FilesystemPromptProvider.create((String) null));
    }
  }

  // ===== Basic Prompt Loading =====

  @Nested
  class PromptLoading {

    @Test
    void providePrompt_existingFile_returnsPrompt() throws IOException {
      String content = "Hello, {{name}}!";
      Files.writeString(tempDir.resolve("greeting.txt"), content, StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("greeting.txt", null);

      assertEquals(content, prompt.content());
      assertFalse(prompt.isCompiled());
    }

    @Test
    void providePrompt_multilineContent_preservesNewlines() throws IOException {
      String content = "Line 1\nLine 2\nLine 3";
      Files.writeString(tempDir.resolve("multiline.txt"), content, StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("multiline.txt");

      assertEquals(content, prompt.content());
    }

    @Test
    void providePrompt_emptyFile_returnsEmptyPrompt() throws IOException {
      Files.writeString(tempDir.resolve("empty.txt"), "", StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("empty.txt", null);

      assertTrue(prompt.isEmpty());
    }

    @Test
    void providePrompt_unicodeContent_handlesCorrectly() throws IOException {
      String content = "こんにちは 🌍 مرحبا";
      Files.writeString(tempDir.resolve("unicode.txt"), content, StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("unicode.txt");

      assertEquals(content, prompt.content());
    }

    @Test
    void providePrompt_templateContent_returnsUncompiled() throws IOException {
      String content = "Hello, {{#if greeting}}{{name}}{{/if}}!";
      Files.writeString(tempDir.resolve("template.txt"), content, StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("template.txt");

      assertFalse(prompt.isCompiled());
      assertEquals(content, prompt.content());
    }
  }

  // ===== Error Handling =====

  @Nested
  class ErrorHandling {

    @Test
    void providePrompt_nonExistentFile_throwsException() {
      PromptProviderException ex =
          assertThrows(
              PromptProviderException.class, () -> provider.providePrompt("nonexistent.txt", null));

      assertEquals("nonexistent.txt", ex.promptId());
      assertTrue(ex.getMessage().contains("not found"));
      assertFalse(ex.isRetryable());
    }

    @Test
    void providePrompt_nullPromptId_throwsNullPointerException() {
      assertThrows(NullPointerException.class, () -> provider.providePrompt(null, null));
    }

    @Test
    void providePrompt_emptyPromptId_throwsException() {
      PromptProviderException ex =
          assertThrows(PromptProviderException.class, () -> provider.providePrompt("", null));

      assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void providePrompt_pathTraversal_throwsException() {
      PromptProviderException ex =
          assertThrows(
              PromptProviderException.class,
              () -> provider.providePrompt("../../../etc/passwd", null));

      assertTrue(ex.getMessage().contains("traversal"));
    }

    @Test
    void providePrompt_absolutePathOutsideBase_throwsException() {
      PromptProviderException ex =
          assertThrows(
              PromptProviderException.class, () -> provider.providePrompt("/../outside.txt", null));

      assertTrue(ex.getMessage().contains("traversal"));
    }
  }

  // ===== Subdirectory Handling =====

  @Nested
  class SubdirectoryHandling {

    @Test
    void providePrompt_subdirectoryFile_returnsPrompt() throws IOException {
      Path subdir = tempDir.resolve("templates");
      Files.createDirectories(subdir);
      Files.writeString(subdir.resolve("email.txt"), "Email template", StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("templates/email.txt", null);

      assertEquals("Email template", prompt.content());
    }

    @Test
    void providePrompt_deeplyNestedFile_returnsPrompt() throws IOException {
      Path nested = tempDir.resolve("a/b/c/d");
      Files.createDirectories(nested);
      Files.writeString(nested.resolve("deep.txt"), "Deep content", StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("a/b/c/d/deep.txt");

      assertEquals("Deep content", prompt.content());
    }

    @Test
    void providePrompt_nonExistentSubdirectory_throwsException() {
      PromptProviderException ex =
          assertThrows(
              PromptProviderException.class,
              () -> provider.providePrompt("nonexistent/file.txt", null));

      assertTrue(ex.getMessage().contains("not found"));
    }
  }

  // ===== Filters (Ignored) =====

  @Nested
  class FiltersHandling {

    @Test
    void providePrompt_withFilters_ignoresFilters() throws IOException {
      Files.writeString(tempDir.resolve("test.txt"), "Content", StandardCharsets.UTF_8);

      // Filters should be ignored by filesystem provider
      Prompt prompt =
          provider.providePrompt("test.txt", Map.of("version", "2", "label", "production"));

      assertEquals("Content", prompt.content());
    }

    @Test
    void providePrompt_nullFilters_works() throws IOException {
      Files.writeString(tempDir.resolve("test.txt"), "Content", StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("test.txt", null);

      assertEquals("Content", prompt.content());
    }

    @Test
    void providePrompt_emptyFilters_works() throws IOException {
      Files.writeString(tempDir.resolve("test.txt"), "Content", StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("test.txt", Map.of());

      assertEquals("Content", prompt.content());
    }
  }

  // ===== Default Method =====

  @Nested
  class DefaultMethodTests {

    @Test
    void providePrompt_noFilters_callsWithNullFilters() throws IOException {
      Files.writeString(tempDir.resolve("test.txt"), "Content", StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("test.txt");

      assertEquals("Content", prompt.content());
    }
  }

  // ===== Integration with Prompt =====

  @Nested
  class PromptIntegration {

    @Test
    void providePrompt_thenCompile_works() throws IOException {
      String template = "Hello, {{name}}! Today is {{day}}.";
      Files.writeString(tempDir.resolve("greeting.txt"), template, StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("greeting.txt");
      Prompt compiled = prompt.compile(Map.of("name", "Alice", "day", "Monday"));

      assertEquals("Hello, Alice! Today is Monday.", compiled.content());
      assertTrue(compiled.isCompiled());
    }

    @Test
    void providePrompt_thenExtractVariables_works() throws IOException {
      String template = "{{greeting}}, {{name}}!";
      Files.writeString(tempDir.resolve("vars.txt"), template, StandardCharsets.UTF_8);

      Prompt prompt = provider.providePrompt("vars.txt");
      var variables = prompt.extractVariableNames();

      assertTrue(variables.contains("greeting"));
      assertTrue(variables.contains("name"));
    }
  }

  // ===== baseDirectory Accessor =====

  @Nested
  class AccessorTests {

    @Test
    void baseDirectory_returnsConfiguredPath() {
      assertEquals(tempDir, provider.baseDirectory());
    }
  }
}
