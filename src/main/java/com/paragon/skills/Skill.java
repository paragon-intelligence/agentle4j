package com.paragon.skills;

import com.paragon.prompts.Prompt;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a modular expertise that augments an agent's capabilities.
 *
 * <p>A Skill packages instructions and resources that are injected into the agent's system prompt.
 * When a skill is added to an agent, its instructions become part of the agent's knowledge,
 * allowing the LLM to automatically apply the skill's expertise when relevant.
 *
 * <p>Unlike sub-agents, skills share the main agent's context window. They extend the agent's
 * capabilities without creating separate execution contexts.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Via Code</h3>
 *
 * <pre>{@code
 * Skill pdfSkill = Skill.builder()
 *     .name("pdf-processor")
 *     .description("Process PDF files, extract text, fill forms")
 *     .instructions("""
 *         You are a PDF processing expert. When working with PDFs:
 *         1. Analyze the document structure
 *         2. Extract or modify content as requested
 *         3. Return well-formatted results
 *         """)
 *     .build();
 * }</pre>
 *
 * <h3>Simple Factory</h3>
 *
 * <pre>{@code
 * Skill skill = Skill.of(
 *     "greeting",
 *     "Generate personalized greetings",
 *     "You create warm, personalized greetings..."
 * );
 * }</pre>
 *
 * <h2>Integration with Agents</h2>
 *
 * <pre>{@code
 * Agent agent = Agent.builder()
 *     .name("DocumentAssistant")
 *     .instructions("You help users with document tasks.")
 *     .addSkill(pdfSkill)  // Skill instructions are added to agent's prompt
 *     .responder(responder)
 *     .build();
 * }</pre>
 *
 * @see SkillProvider
 * @see SkillStore
 * @since 1.0
 */
public final class Skill {

  private final @NonNull String name;
  private final @NonNull String description;
  private final @NonNull Prompt instructions;
  private final @NonNull Map<String, Prompt> resources;

  private Skill(Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "name cannot be null");
    this.description = Objects.requireNonNull(builder.description, "description cannot be null");
    this.instructions =
        builder.instructions != null ? builder.instructions : Prompt.of(builder.instructionsText);
    this.resources = Map.copyOf(builder.resources);

    // Validate name format (lowercase, numbers, hyphens only)
    if (!name.matches("^[a-z0-9-]+$")) {
      throw new IllegalArgumentException(
          "Skill name must contain only lowercase letters, numbers, and hyphens: " + name);
    }
    if (name.length() > 64) {
      throw new IllegalArgumentException("Skill name must be at most 64 characters: " + name);
    }
    if (description.isEmpty()) {
      throw new IllegalArgumentException("Skill description cannot be empty");
    }
    if (description.length() > 1024) {
      throw new IllegalArgumentException(
          "Skill description must be at most 1024 characters: " + description.length());
    }
  }

  /**
   * Creates a new Skill builder.
   *
   * @return a new builder instance
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a simple Skill with name, description, and instructions.
   *
   * @param name the skill name (lowercase, numbers, hyphens)
   * @param description when to use this skill
   * @param instructions the skill's instructions
   * @return a new Skill instance
   */
  public static @NonNull Skill of(
      @NonNull String name, @NonNull String description, @NonNull String instructions) {
    return builder().name(name).description(description).instructions(instructions).build();
  }

  /**
   * Creates a simple Skill with name, description, and instructions.
   *
   * @param name the skill name (lowercase, numbers, hyphens)
   * @param description when to use this skill
   * @param instructions the skill's instructions as a Prompt
   * @return a new Skill instance
   */
  public static @NonNull Skill of(
      @NonNull String name, @NonNull String description, @NonNull Prompt instructions) {
    return builder().name(name).description(description).instructions(instructions).build();
  }

  /**
   * Returns the skill's unique name.
   *
   * @return the skill name
   */
  public @NonNull String name() {
    return name;
  }

  /**
   * Returns the skill's description.
   *
   * <p>This description helps the LLM understand when to apply this skill's expertise.
   *
   * @return the skill description
   */
  public @NonNull String description() {
    return description;
  }

  /**
   * Returns the skill's instructions.
   *
   * <p>These instructions are injected into the agent's system prompt.
   *
   * @return the skill instructions
   */
  public @NonNull Prompt instructions() {
    return instructions;
  }

  /**
   * Returns additional resources (context files) for this skill.
   *
   * <p>Resources are named Prompts that provide supplementary context. For example, a PDF skill
   * might have a "FORMS.md" resource with form-filling guidance.
   *
   * @return unmodifiable map of resource name to content
   */
  public @NonNull Map<String, Prompt> resources() {
    return resources;
  }

  /**
   * Returns whether this skill has any additional resources.
   *
   * @return true if the skill has resources
   */
  public boolean hasResources() {
    return !resources.isEmpty();
  }

  /**
   * Generates the prompt section for this skill.
   *
   * <p>This produces a formatted text block that can be appended to an agent's system prompt. The
   * format includes the skill name, description, instructions, and any resources.
   *
   * @return the formatted skill prompt section
   */
  public @NonNull String toPromptSection() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n\n## Skill: ").append(name).append("\n");
    sb.append("**When to use**: ").append(description).append("\n\n");
    sb.append(instructions.text());

    if (!resources.isEmpty()) {
      for (Map.Entry<String, Prompt> entry : resources.entrySet()) {
        sb.append("\n\n### ").append(entry.getKey()).append("\n");
        sb.append(entry.getValue().text());
      }
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Skill skill)) return false;
    return Objects.equals(name, skill.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "Skill["
        + "name='"
        + name
        + '\''
        + ", description='"
        + (description.length() > 50 ? description.substring(0, 50) + "..." : description)
        + '\''
        + ", resources="
        + resources.size()
        + ']';
  }

  /** Builder for creating Skill instances. */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable String description;
    private @Nullable Prompt instructions;
    private @Nullable String instructionsText;
    private final Map<String, Prompt> resources = new HashMap<>();

    private Builder() {}

    /**
     * Sets the skill's unique name.
     *
     * <p>Must contain only lowercase letters, numbers, and hyphens. Maximum 64 characters.
     *
     * @param name the skill name
     * @return this builder
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    /**
     * Sets the skill's description.
     *
     * <p>This should explain what the skill does and when to use it. Maximum 1024 characters.
     *
     * @param description the skill description
     * @return this builder
     */
    public @NonNull Builder description(@NonNull String description) {
      this.description = Objects.requireNonNull(description);
      return this;
    }

    /**
     * Sets the skill's instructions.
     *
     * @param instructions the instructions as a Prompt
     * @return this builder
     */
    public @NonNull Builder instructions(@NonNull Prompt instructions) {
      this.instructions = Objects.requireNonNull(instructions);
      this.instructionsText = null;
      return this;
    }

    /**
     * Sets the skill's instructions.
     *
     * @param instructions the instructions as a string
     * @return this builder
     */
    public @NonNull Builder instructions(@NonNull String instructions) {
      this.instructionsText = Objects.requireNonNull(instructions);
      this.instructions = null;
      return this;
    }

    /**
     * Adds a resource (additional context file) to the skill.
     *
     * @param name the resource name (e.g., "FORMS.md")
     * @param content the resource content
     * @return this builder
     */
    public @NonNull Builder addResource(@NonNull String name, @NonNull Prompt content) {
      this.resources.put(Objects.requireNonNull(name), Objects.requireNonNull(content));
      return this;
    }

    /**
     * Adds a resource (additional context file) to the skill.
     *
     * @param name the resource name (e.g., "FORMS.md")
     * @param content the resource content as a string
     * @return this builder
     */
    public @NonNull Builder addResource(@NonNull String name, @NonNull String content) {
      return addResource(name, Prompt.of(content));
    }

    /**
     * Builds the Skill instance.
     *
     * @return the configured Skill
     * @throws NullPointerException if required fields are missing
     * @throws IllegalArgumentException if validation fails
     */
    public @NonNull Skill build() {
      if (instructions == null && instructionsText == null) {
        throw new NullPointerException("instructions cannot be null");
      }
      return new Skill(this);
    }
  }
}
