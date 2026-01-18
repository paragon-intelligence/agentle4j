package com.paragon.skills;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the FilesystemSkillProvider class.
 */
@DisplayName("FilesystemSkillProvider")
class FilesystemSkillProviderTest {

  @TempDir
  Path tempDir;

  private Path skillsDir;

  @BeforeEach
  void setUp() throws IOException {
    skillsDir = tempDir.resolve("skills");
    Files.createDirectory(skillsDir);
  }

  @Nested
  @DisplayName("Creation")
  class Creation {

    @Test
    @DisplayName("create() succeeds with valid directory")
    void create_succeedsWithValidDirectory() {
      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);

      assertNotNull(provider);
      assertEquals(skillsDir, provider.skillsDirectory());
    }

    @Test
    @DisplayName("create() throws when directory doesn't exist")
    void create_throwsWhenDirectoryDoesNotExist() {
      Path nonexistent = tempDir.resolve("nonexistent");

      assertThrows(SkillProviderException.class, () -> {
        FilesystemSkillProvider.create(nonexistent);
      });
    }

    @Test
    @DisplayName("create() throws when path is a file")
    void create_throwsWhenPathIsFile() throws IOException {
      Path file = tempDir.resolve("file.txt");
      Files.createFile(file);

      assertThrows(SkillProviderException.class, () -> {
        FilesystemSkillProvider.create(file);
      });
    }
  }

  @Nested
  @DisplayName("Skill Loading")
  class SkillLoading {

    @Test
    @DisplayName("provide loads skill from directory")
    void provide_loadsSkillFromDirectory() throws IOException {
      createSkillDirectory("pdf-processor", """
          ---
          name: pdf-processor
          description: Process PDF files
          ---

          You are a PDF expert.
          """);

      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);
      Skill skill = provider.provide("pdf-processor");

      assertEquals("pdf-processor", skill.name());
      assertEquals("Process PDF files", skill.description());
      assertTrue(skill.instructions().text().contains("PDF expert"));
    }

    @Test
    @DisplayName("provide throws when skill not found")
    void provide_throwsWhenSkillNotFound() {
      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);

      assertThrows(SkillProviderException.class, () -> {
        provider.provide("nonexistent");
      });
    }

    @Test
    @DisplayName("provide throws when SKILL.md missing")
    void provide_throwsWhenSkillMdMissing() throws IOException {
      // Create directory without SKILL.md
      Files.createDirectory(skillsDir.resolve("empty-skill"));

      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);

      assertThrows(SkillProviderException.class, () -> {
        provider.provide("empty-skill");
      });
    }

    @Test
    @DisplayName("provide loads additional resources")
    void provide_loadsAdditionalResources() throws IOException {
      createSkillDirectory("pdf-processor", """
          ---
          name: pdf-processor
          description: Process PDF files
          ---

          Instructions.
          """);

      // Add additional resource file
      Path resourceFile = skillsDir.resolve("pdf-processor").resolve("FORMS.md");
      Files.writeString(resourceFile, "# Form Filling Guide", StandardCharsets.UTF_8);

      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);
      Skill skill = provider.provide("pdf-processor");

      assertTrue(skill.hasResources());
      assertTrue(skill.resources().containsKey("FORMS.md"));
    }
  }

  @Nested
  @DisplayName("Static Loading")
  class StaticLoading {

    @Test
    @DisplayName("loadFromFile loads skill from single file")
    void loadFromFile_loadsSkillFromSingleFile() throws IOException {
      Path skillFile = tempDir.resolve("single-skill.md");
      Files.writeString(skillFile, """
          ---
          name: single-skill
          description: A single skill file
          ---

          Instructions here.
          """, StandardCharsets.UTF_8);

      Skill skill = FilesystemSkillProvider.loadFromFile(skillFile);

      assertEquals("single-skill", skill.name());
    }

    @Test
    @DisplayName("loadFromFile throws when file not found")
    void loadFromFile_throwsWhenFileNotFound() {
      Path nonexistent = tempDir.resolve("nonexistent.md");

      assertThrows(SkillProviderException.class, () -> {
        FilesystemSkillProvider.loadFromFile(nonexistent);
      });
    }
  }

  @Nested
  @DisplayName("Existence Checking")
  class ExistenceChecking {

    @Test
    @DisplayName("exists returns true when skill exists")
    void exists_returnsTrueWhenSkillExists() throws IOException {
      createSkillDirectory("test-skill", """
          ---
          name: test-skill
          description: Test
          ---
          Instructions.
          """);

      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);

      assertTrue(provider.exists("test-skill"));
    }

    @Test
    @DisplayName("exists returns false when skill not found")
    void exists_returnsFalseWhenNotFound() {
      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);

      assertFalse(provider.exists("nonexistent"));
    }

    @Test
    @DisplayName("exists returns false when SKILL.md missing")
    void exists_returnsFalseWhenSkillMdMissing() throws IOException {
      Files.createDirectory(skillsDir.resolve("no-skill-md"));

      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);

      assertFalse(provider.exists("no-skill-md"));
    }
  }

  @Nested
  @DisplayName("Listing")
  class Listing {

    @Test
    @DisplayName("listSkillIds returns all skill directories")
    void listSkillIds_returnsAllSkillDirectories() throws IOException {
      createSkillDirectory("skill-1", """
          ---
          name: skill-1
          description: First skill
          ---
          Instructions.
          """);
      createSkillDirectory("skill-2", """
          ---
          name: skill-2
          description: Second skill
          ---
          Instructions.
          """);

      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);
      Set<String> skillIds = provider.listSkillIds();

      assertEquals(2, skillIds.size());
      assertTrue(skillIds.contains("skill-1"));
      assertTrue(skillIds.contains("skill-2"));
    }

    @Test
    @DisplayName("listSkillIds excludes directories without SKILL.md")
    void listSkillIds_excludesDirectoriesWithoutSkillMd() throws IOException {
      createSkillDirectory("valid-skill", """
          ---
          name: valid-skill
          description: Valid skill
          ---
          Instructions.
          """);
      Files.createDirectory(skillsDir.resolve("invalid-dir"));

      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);
      Set<String> skillIds = provider.listSkillIds();

      assertEquals(1, skillIds.size());
      assertTrue(skillIds.contains("valid-skill"));
    }

    @Test
    @DisplayName("listSkillIds returns empty set for empty directory")
    void listSkillIds_returnsEmptySetForEmptyDirectory() {
      FilesystemSkillProvider provider = FilesystemSkillProvider.create(skillsDir);
      Set<String> skillIds = provider.listSkillIds();

      assertTrue(skillIds.isEmpty());
    }
  }

  // Helper methods

  private void createSkillDirectory(String name, String skillMdContent) throws IOException {
    Path skillDir = skillsDir.resolve(name);
    Files.createDirectory(skillDir);
    Path skillFile = skillDir.resolve("SKILL.md");
    Files.writeString(skillFile, skillMdContent, StandardCharsets.UTF_8);
  }
}
