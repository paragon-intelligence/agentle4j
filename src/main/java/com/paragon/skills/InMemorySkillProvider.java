package com.paragon.skills;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Stores code-defined skills in memory.
 *
 * <p>This provider is useful for skills defined directly in code without external files. It can be
 * pre-populated with skills at construction time or skills can be registered dynamically.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Factory method with initial skills
 * SkillProvider provider = InMemorySkillProvider.of(skill1, skill2, skill3);
 *
 * // Builder pattern
 * SkillProvider provider = InMemorySkillProvider.builder()
 *     .add(pdfSkill)
 *     .add(dataSkill)
 *     .build();
 *
 * // Empty provider with dynamic registration
 * InMemorySkillProvider provider = InMemorySkillProvider.empty();
 * provider.register(newSkill);
 * }</pre>
 *
 * @see SkillProvider
 * @see Skill
 * @since 1.0
 */
public final class InMemorySkillProvider implements SkillProvider {

  private final Map<String, Skill> skills = new ConcurrentHashMap<>();

  private InMemorySkillProvider() {}

  /**
   * Creates an empty InMemorySkillProvider.
   *
   * @return a new empty provider
   */
  public static @NonNull InMemorySkillProvider empty() {
    return new InMemorySkillProvider();
  }

  /**
   * Creates an InMemorySkillProvider with initial skills.
   *
   * @param skills the skills to register
   * @return a new provider with the skills
   */
  public static @NonNull InMemorySkillProvider of(@NonNull Skill... skills) {
    InMemorySkillProvider provider = new InMemorySkillProvider();
    for (Skill skill : skills) {
      provider.register(skill);
    }
    return provider;
  }

  /**
   * Creates a new builder for InMemorySkillProvider.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Registers a skill in this provider.
   *
   * <p>If a skill with the same name already exists, it will be replaced.
   *
   * @param skill the skill to register
   * @return this provider for chaining
   */
  public @NonNull InMemorySkillProvider register(@NonNull Skill skill) {
    Objects.requireNonNull(skill, "skill cannot be null");
    skills.put(skill.name(), skill);
    return this;
  }

  /**
   * Registers multiple skills.
   *
   * @param skills the skills to register
   * @return this provider for chaining
   */
  public @NonNull InMemorySkillProvider registerAll(@NonNull Skill... skills) {
    for (Skill skill : skills) {
      register(skill);
    }
    return this;
  }

  /**
   * Removes a skill from this provider.
   *
   * @param skillId the skill name to remove
   * @return true if the skill was removed
   */
  public boolean remove(@NonNull String skillId) {
    Objects.requireNonNull(skillId, "skillId cannot be null");
    return skills.remove(skillId) != null;
  }

  /** Clears all skills from this provider. */
  public void clear() {
    skills.clear();
  }

  @Override
  public @NonNull Skill provide(@NonNull String skillId, @Nullable Map<String, String> filters) {
    Objects.requireNonNull(skillId, "skillId cannot be null");

    Skill skill = skills.get(skillId);
    if (skill == null) {
      throw new SkillProviderException(skillId, "Skill not found in memory");
    }
    return skill;
  }

  @Override
  public boolean exists(@NonNull String skillId) {
    Objects.requireNonNull(skillId, "skillId cannot be null");
    return skills.containsKey(skillId);
  }

  @Override
  public @NonNull Set<String> listSkillIds() {
    return Set.copyOf(skills.keySet());
  }

  /**
   * Returns the number of registered skills.
   *
   * @return skill count
   */
  public int size() {
    return skills.size();
  }

  @Override
  public String toString() {
    return "InMemorySkillProvider[" + skills.size() + " skills]";
  }

  /** Builder for InMemorySkillProvider. */
  public static final class Builder {
    private final InMemorySkillProvider provider = new InMemorySkillProvider();

    private Builder() {}

    /**
     * Adds a skill to the provider.
     *
     * @param skill the skill to add
     * @return this builder
     */
    public @NonNull Builder add(@NonNull Skill skill) {
      provider.register(skill);
      return this;
    }

    /**
     * Adds multiple skills to the provider.
     *
     * @param skills the skills to add
     * @return this builder
     */
    public @NonNull Builder addAll(@NonNull Skill... skills) {
      for (Skill skill : skills) {
        provider.register(skill);
      }
      return this;
    }

    /**
     * Builds the InMemorySkillProvider.
     *
     * @return the configured provider
     */
    public @NonNull InMemorySkillProvider build() {
      return provider;
    }
  }
}
