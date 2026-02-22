package com.paragon.skills;

import com.paragon.prompts.Prompt;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * Parser for SKILL.md files using YAML frontmatter and Markdown content.
 *
 * <p>The parser handles files in Claude's SKILL.md format:
 *
 * <pre>{@code
 * ---
 * name: pdf-processor
 * description: Process PDF files, extract text, fill forms.
 * ---
 *
 * # PDF Processing
 *
 * ## Instructions
 * You are a PDF processing expert...
 * }</pre>
 *
 * <h2>YAML Frontmatter</h2>
 *
 * <p>The frontmatter section (between --- markers) must contain:
 *
 * <ul>
 *   <li><b>name</b> (required): Lowercase letters, numbers, and hyphens only. Max 64 chars.
 *   <li><b>description</b> (required): When to use this skill. Max 1024 chars.
 * </ul>
 *
 * <h2>Markdown Body</h2>
 *
 * <p>Everything after the frontmatter is treated as the skill's instructions.
 *
 * @see Skill
 * @see FilesystemSkillProvider
 * @since 1.0
 */
public final class SkillMarkdownParser {

  // Pattern to match YAML frontmatter (between --- markers)
  private static final Pattern FRONTMATTER_PATTERN =
      Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n?", Pattern.DOTALL);

  // Pattern to extract name from YAML
  private static final Pattern NAME_PATTERN =
      Pattern.compile("^name:\\s*[\"']?([^\"'\\n]+)[\"']?\\s*$", Pattern.MULTILINE);

  // Pattern to extract description from YAML
  private static final Pattern DESCRIPTION_PATTERN =
      Pattern.compile("^description:\\s*[\"']?([^\"'\\n]+)[\"']?\\s*$", Pattern.MULTILINE);

  /** Creates a new SkillMarkdownParser. */
  public SkillMarkdownParser() {}

  /**
   * Parses a SKILL.md file content into a Skill.
   *
   * @param content the file content
   * @return the parsed Skill
   * @throws SkillProviderException if parsing fails
   */
  public @NonNull Skill parse(@NonNull String content) {
    Objects.requireNonNull(content, "content cannot be null");

    if (content.isBlank()) {
      throw new SkillProviderException("SKILL.md content is empty");
    }

    // Extract frontmatter
    Matcher frontmatterMatcher = FRONTMATTER_PATTERN.matcher(content);
    if (!frontmatterMatcher.find()) {
      throw new SkillProviderException(
          "Invalid SKILL.md format: missing YAML frontmatter (--- markers)");
    }

    String frontmatter = frontmatterMatcher.group(1);
    String markdown = content.substring(frontmatterMatcher.end()).trim();

    // Extract name from frontmatter
    String name = extractValue(frontmatter, NAME_PATTERN, "name");
    if (name == null || name.isBlank()) {
      throw new SkillProviderException("SKILL.md frontmatter missing required field: name");
    }

    // Extract description from frontmatter
    String description = extractValue(frontmatter, DESCRIPTION_PATTERN, "description");
    if (description == null || description.isBlank()) {
      throw new SkillProviderException("SKILL.md frontmatter missing required field: description");
    }

    // Markdown body becomes instructions
    if (markdown.isBlank()) {
      throw new SkillProviderException("SKILL.md has no content after frontmatter");
    }

    return Skill.builder()
        .name(name.trim())
        .description(description.trim())
        .instructions(Prompt.of(markdown))
        .build();
  }

  /**
   * Parses a SKILL.md file content with a fallback skill ID.
   *
   * <p>If the name is not found in frontmatter, uses the provided skillId.
   *
   * @param content the file content
   * @param skillId fallback skill ID (used if name not in frontmatter)
   * @return the parsed Skill
   * @throws SkillProviderException if parsing fails
   */
  public @NonNull Skill parse(@NonNull String content, @NonNull String skillId) {
    Objects.requireNonNull(content, "content cannot be null");
    Objects.requireNonNull(skillId, "skillId cannot be null");

    try {
      return parse(content);
    } catch (SkillProviderException e) {
      // Re-throw with skillId context
      throw new SkillProviderException(skillId, e.getMessage());
    }
  }

  /**
   * Validates that SKILL.md content is parseable without fully parsing.
   *
   * @param content the content to validate
   * @return true if the content appears to be valid SKILL.md format
   */
  public boolean isValid(@NonNull String content) {
    if (content == null || content.isBlank()) {
      return false;
    }

    Matcher frontmatterMatcher = FRONTMATTER_PATTERN.matcher(content);
    if (!frontmatterMatcher.find()) {
      return false;
    }

    String frontmatter = frontmatterMatcher.group(1);
    String name = extractValue(frontmatter, NAME_PATTERN, "name");
    String description = extractValue(frontmatter, DESCRIPTION_PATTERN, "description");

    return name != null && !name.isBlank() && description != null && !description.isBlank();
  }

  private String extractValue(String yaml, Pattern pattern, String fieldName) {
    Matcher matcher = pattern.matcher(yaml);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }
}
