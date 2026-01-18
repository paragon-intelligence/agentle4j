package com.paragon.skills;

import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Provider interface for loading skills from various sources.
 *
 * <p>Implementations may load skills from the filesystem, remote URLs,
 * in-memory registries, or any other storage mechanism. This abstraction
 * allows applications to centralize skill management.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Filesystem provider
 * SkillProvider provider = FilesystemSkillProvider.create(Path.of("./skills"));
 * Skill skill = provider.provide("pdf-processor");
 *
 * // URL provider
 * SkillProvider urlProvider = UrlSkillProvider.builder()
 *     .httpClient(httpClient)
 *     .build();
 * Skill skill = urlProvider.provide("https://example.com/skills/pdf-processor/SKILL.md");
 *
 * // In-memory provider
 * SkillProvider memProvider = InMemorySkillProvider.of(skill1, skill2);
 * }</pre>
 *
 * @see FilesystemSkillProvider
 * @see UrlSkillProvider
 * @see InMemorySkillProvider
 * @since 1.0
 */
public interface SkillProvider {

  /**
   * Retrieves a skill by its identifier.
   *
   * @param skillId the unique identifier for the skill (e.g., skill name, path)
   * @param filters optional key-value pairs to filter the skill. Supported filters
   *     depend on the implementation (e.g., version, label).
   * @return the retrieved {@link Skill}
   * @throws NullPointerException if skillId is null
   * @throws SkillProviderException if the skill cannot be retrieved
   */
  @NonNull Skill provide(@NonNull String skillId, @Nullable Map<String, String> filters);

  /**
   * Retrieves a skill by its identifier without filters.
   *
   * @param skillId the unique identifier for the skill
   * @return the retrieved {@link Skill}
   * @throws NullPointerException if skillId is null
   * @throws SkillProviderException if the skill cannot be retrieved
   */
  @NonNull default Skill provide(@NonNull String skillId) {
    return provide(skillId, null);
  }

  /**
   * Checks if a skill with the given identifier exists.
   *
   * @param skillId the unique identifier for the skill
   * @return {@code true} if the skill exists, {@code false} otherwise
   * @throws NullPointerException if skillId is null
   */
  boolean exists(@NonNull String skillId);

  /**
   * Lists all available skill identifiers.
   *
   * @return an unmodifiable set of all available skill identifiers
   * @throws SkillProviderException if the listing fails
   */
  @NonNull Set<String> listSkillIds();
}
