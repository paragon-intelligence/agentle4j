package com.paragon.skills;

import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing available skills.
 *
 * <p>SkillStore provides a centralized place to register and retrieve skills. It can generate a
 * formatted prompt section listing all available skills for inclusion in agent system prompts.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * SkillStore store = new SkillStore();
 * store.register(pdfSkill);
 * store.register(dataAnalyzerSkill);
 *
 * // Get a specific skill
 * Optional<Skill> skill = store.get("pdf-processor");
 *
 * // Generate prompt section
 * String skillsSection = store.generatePromptSection();
 * // Returns:
 * // ## Available Skills
 * // - pdf-processor: Process PDF files...
 * // - data-analyzer: Analyze data...
 * }</pre>
 *
 * <h2>Integration with Agents</h2>
 *
 * <pre>{@code
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .skillStore(store)  // Registers all skills as tools
 *     .build();
 * }</pre>
 *
 * @see Skill
 * @since 1.0
 */
public final class SkillStore {

  private final Map<String, Skill> skills = new ConcurrentHashMap<>();

  /**
   * Creates an empty SkillStore.
   */
  public SkillStore() {
  }

  /**
   * Creates a SkillStore with initial skills.
   *
   * @param skills the skills to register
   */
  public SkillStore(@NonNull Skill... skills) {
    for (Skill skill : skills) {
      register(skill);
    }
  }

  /**
   * Creates a SkillStore with initial skills.
   *
   * @param skills the skills to register
   */
  public SkillStore(@NonNull List<Skill> skills) {
    for (Skill skill : skills) {
      register(skill);
    }
  }

  /**
   * Registers a skill in the store.
   *
   * <p>If a skill with the same name already exists, it will be replaced.
   *
   * @param skill the skill to register
   * @return this store for chaining
   */
  public @NonNull SkillStore register(@NonNull Skill skill) {
    Objects.requireNonNull(skill, "skill cannot be null");
    skills.put(skill.name(), skill);
    return this;
  }

  /**
   * Registers multiple skills in the store.
   *
   * @param skills the skills to register
   * @return this store for chaining
   */
  public @NonNull SkillStore registerAll(@NonNull Skill... skills) {
    for (Skill skill : skills) {
      register(skill);
    }
    return this;
  }

  /**
   * Registers multiple skills in the store.
   *
   * @param skills the skills to register
   * @return this store for chaining
   */
  public @NonNull SkillStore registerAll(@NonNull List<Skill> skills) {
    for (Skill skill : skills) {
      register(skill);
    }
    return this;
  }

  /**
   * Retrieves a skill by name.
   *
   * @param name the skill name
   * @return Optional containing the skill, or empty if not found
   */
  public @NonNull Optional<Skill> get(@NonNull String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return Optional.ofNullable(skills.get(name));
  }

  /**
   * Returns all registered skills.
   *
   * @return unmodifiable list of skills
   */
  public @NonNull List<Skill> all() {
    return List.copyOf(skills.values());
  }

  /**
   * Returns the names of all registered skills.
   *
   * @return unmodifiable set of skill names
   */
  public @NonNull Set<String> names() {
    return Set.copyOf(skills.keySet());
  }

  /**
   * Checks if a skill is registered.
   *
   * @param name the skill name
   * @return true if the skill exists
   */
  public boolean contains(@NonNull String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return skills.containsKey(name);
  }

  /**
   * Returns the number of registered skills.
   *
   * @return skill count
   */
  public int size() {
    return skills.size();
  }

  /**
   * Returns whether the store is empty.
   *
   * @return true if no skills are registered
   */
  public boolean isEmpty() {
    return skills.isEmpty();
  }

  /**
   * Removes a skill from the store.
   *
   * @param name the skill name
   * @return Optional containing the removed skill, or empty if not found
   */
  public @NonNull Optional<Skill> remove(@NonNull String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return Optional.ofNullable(skills.remove(name));
  }

  /**
   * Clears all skills from the store.
   */
  public void clear() {
    skills.clear();
  }

  /**
   * Generates a formatted prompt section listing all available skills.
   *
   * <p>This can be appended to an agent's system prompt to inform the LLM about available skills.
   *
   * @return formatted skills section
   */
  public @NonNull String generatePromptSection() {
    if (skills.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("## Available Skills\n\n");
    sb.append("You have access to specialized skills. Call them as tools when appropriate:\n\n");

    List<Skill> sortedSkills = new ArrayList<>(skills.values());
    sortedSkills.sort((a, b) -> a.name().compareTo(b.name()));

    for (Skill skill : sortedSkills) {
      sb.append("- **").append(skill.name()).append("**: ");
      sb.append(skill.description()).append("\n");
    }

    return sb.toString();
  }

  /**
   * Generates a compact prompt section (one line per skill).
   *
   * @return compact skills section
   */
  public @NonNull String generateCompactPromptSection() {
    if (skills.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Available skills: ");

    List<String> skillDescriptions = new ArrayList<>();
    for (Skill skill : skills.values()) {
      skillDescriptions.add(skill.name() + " (" + truncate(skill.description(), 50) + ")");
    }
    Collections.sort(skillDescriptions);

    sb.append(String.join(", ", skillDescriptions));
    return sb.toString();
  }

  private String truncate(String text, int maxLength) {
    if (text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength - 3) + "...";
  }

  @Override
  public String toString() {
    return "SkillStore[" + skills.size() + " skills: " + skills.keySet() + "]";
  }
}
