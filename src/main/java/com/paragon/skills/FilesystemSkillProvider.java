package com.paragon.skills;

import com.paragon.prompts.Prompt;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Loads skills from the filesystem in SKILL.md format.
 *
 * <p>This provider expects skills to be organized as directories containing a SKILL.md file:
 *
 * <pre>
 * skills/
 * ├── pdf-processor/
 * │   ├── SKILL.md         (required)
 * │   └── FORMS.md         (optional resource)
 * └── data-analyzer/
 *     └── SKILL.md
 * </pre>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * SkillProvider provider = FilesystemSkillProvider.create(Path.of("./skills"));
 *
 * // Load a specific skill
 * Skill skill = provider.provide("pdf-processor");
 *
 * // List all available skills
 * Set<String> skillIds = provider.listSkillIds();
 *
 * // Check if a skill exists
 * boolean exists = provider.exists("pdf-processor");
 * }</pre>
 *
 * @see SkillProvider
 * @see SkillMarkdownParser
 * @since 1.0
 */
public final class FilesystemSkillProvider implements SkillProvider {

  private static final String SKILL_FILE = "SKILL.md";

  private final @NonNull Path skillsDirectory;
  private final @NonNull SkillMarkdownParser parser;

  private FilesystemSkillProvider(@NonNull Path skillsDirectory) {
    this.skillsDirectory = Objects.requireNonNull(skillsDirectory, "skillsDirectory cannot be null");
    this.parser = new SkillMarkdownParser();

    if (!Files.isDirectory(skillsDirectory)) {
      throw new SkillProviderException(
          "Skills directory does not exist or is not a directory: " + skillsDirectory);
    }
  }

  /**
   * Creates a FilesystemSkillProvider for the given directory.
   *
   * @param skillsDirectory the root directory containing skill subdirectories
   * @return a new provider
   * @throws SkillProviderException if the directory doesn't exist
   */
  public static @NonNull FilesystemSkillProvider create(@NonNull Path skillsDirectory) {
    return new FilesystemSkillProvider(skillsDirectory);
  }

  /**
   * Loads a skill from a single SKILL.md file (not a directory).
   *
   * @param skillFile path to the SKILL.md file
   * @return the parsed skill
   * @throws SkillProviderException if loading fails
   */
  public static @NonNull Skill loadFromFile(@NonNull Path skillFile) {
    Objects.requireNonNull(skillFile, "skillFile cannot be null");

    if (!Files.isRegularFile(skillFile)) {
      throw new SkillProviderException("Skill file does not exist: " + skillFile);
    }

    try {
      String content = Files.readString(skillFile, StandardCharsets.UTF_8);
      SkillMarkdownParser parser = new SkillMarkdownParser();
      return parser.parse(content);
    } catch (IOException e) {
      throw new SkillProviderException("Failed to read skill file: " + skillFile, e);
    }
  }

  @Override
  public @NonNull Skill provide(@NonNull String skillId, @Nullable Map<String, String> filters) {
    Objects.requireNonNull(skillId, "skillId cannot be null");

    Path skillDir = skillsDirectory.resolve(skillId);
    Path skillFile = skillDir.resolve(SKILL_FILE);

    if (!Files.isDirectory(skillDir)) {
      throw new SkillProviderException(skillId, "Skill directory not found: " + skillDir);
    }

    if (!Files.isRegularFile(skillFile)) {
      throw new SkillProviderException(skillId, "SKILL.md not found in: " + skillDir);
    }

    try {
      // Read and parse the main SKILL.md
      String content = Files.readString(skillFile, StandardCharsets.UTF_8);
      Skill.Builder builder = parseToBuilder(content, skillId);

      // Load additional resources (other .md files in the directory)
      loadResources(skillDir, builder);

      return builder.build();
    } catch (IOException e) {
      throw new SkillProviderException(skillId, "Failed to read skill files", e);
    }
  }

  /**
   * Parses SKILL.md content into a Skill.Builder for further modification.
   */
  private Skill.Builder parseToBuilder(String content, String skillId) {
    if (content.isBlank()) {
      throw new SkillProviderException(skillId, "SKILL.md is empty");
    }

    // Parse using the markdown parser
    Skill parsed = parser.parse(content, skillId);

    // Return a builder initialized with parsed values
    return Skill.builder()
        .name(parsed.name())
        .description(parsed.description())
        .instructions(parsed.instructions());
  }

  /**
   * Loads additional .md files as resources.
   */
  private void loadResources(Path skillDir, Skill.Builder builder) throws IOException {
    try (Stream<Path> files = Files.list(skillDir)) {
      files
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".md"))
          .filter(p -> !p.getFileName().toString().equals(SKILL_FILE))
          .forEach(resourceFile -> {
            try {
              String resourceName = resourceFile.getFileName().toString();
              String resourceContent = Files.readString(resourceFile, StandardCharsets.UTF_8);
              builder.addResource(resourceName, Prompt.of(resourceContent));
            } catch (IOException e) {
              // Log but don't fail - resources are optional
            }
          });
    }
  }

  @Override
  public boolean exists(@NonNull String skillId) {
    Objects.requireNonNull(skillId, "skillId cannot be null");

    Path skillDir = skillsDirectory.resolve(skillId);
    Path skillFile = skillDir.resolve(SKILL_FILE);

    return Files.isDirectory(skillDir) && Files.isRegularFile(skillFile);
  }

  @Override
  public @NonNull Set<String> listSkillIds() {
    try (Stream<Path> dirs = Files.list(skillsDirectory)) {
      return dirs
          .filter(Files::isDirectory)
          .filter(dir -> Files.isRegularFile(dir.resolve(SKILL_FILE)))
          .map(dir -> dir.getFileName().toString())
          .collect(Collectors.toUnmodifiableSet());
    } catch (IOException e) {
      throw new SkillProviderException("Failed to list skills in: " + skillsDirectory, e);
    }
  }

  /**
   * Returns the root skills directory.
   *
   * @return the skills directory path
   */
  public @NonNull Path skillsDirectory() {
    return skillsDirectory;
  }

  @Override
  public String toString() {
    return "FilesystemSkillProvider[" + skillsDirectory + "]";
  }
}
