package com.paragon.skills;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A tool that lets the agent read the full instructions and resources for a specific skill.
 *
 * <p>Instead of injecting every skill's full content into the system prompt (wasting context
 * tokens), this tool implements <b>progressive disclosure</b>: the agent sees a concise catalog of
 * available skills (name + description) in its prompt, and calls this tool only when it decides a
 * skill is relevant to the current task.
 *
 * <h2>How It Works</h2>
 *
 * <ol>
 *   <li>The agent's system prompt includes a catalog listing each skill's name and description
 *   <li>When the agent determines a skill is relevant, it calls {@code read_skill(skillName)}
 *   <li>This tool looks up the skill in the {@link SkillStore} and returns its full content
 *   <li>The agent can then apply the skill's expertise with full instructions available
 * </ol>
 *
 * <p>This tool is registered automatically when skills are added to an agent via {@code
 * Agent.Builder.addSkill()}. It should not be created manually.
 *
 * @see Skill
 * @see SkillStore
 * @since 1.0
 */
@FunctionMetadata(
    name = "read_skill",
    description =
        "Read the full instructions and resources for a specific skill. "
            + "Call this when you need to apply a skill's expertise to the current task. "
            + "Pass the exact skill name from the skills catalog.")
public final class SkillReaderTool extends FunctionTool<SkillReaderTool.Params> {

  private final @NonNull SkillStore skillStore;

  /**
   * Parameters for the read_skill tool.
   *
   * @param skillName the exact name of the skill to read (from the skills catalog)
   */
  public record Params(
      @JsonProperty("skill_name")
          @JsonPropertyDescription(
              "The exact name of the skill to read, as listed in the skills catalog")
          @NonNull String skillName) {}

  /**
   * Creates a SkillReaderTool backed by the given skill store.
   *
   * @param skillStore the store containing available skills
   */
  public SkillReaderTool(@NonNull SkillStore skillStore) {
    super();
    this.skillStore = Objects.requireNonNull(skillStore, "skillStore cannot be null");
  }

  @Override
  public @NonNull FunctionToolCallOutput call(@Nullable Params params) {
    if (params == null || params.skillName() == null || params.skillName().isBlank()) {
      return FunctionToolCallOutput.error(
          "skill_name is required. Available skills: " + availableSkillNames());
    }

    return skillStore
        .get(params.skillName())
        .map(
            skill ->
                FunctionToolCallOutput.success(
                    skill.toPromptSection())) // Reuses the existing full-content formatter
        .orElseGet(
            () ->
                FunctionToolCallOutput.error(
                    "Skill '"
                        + params.skillName()
                        + "' not found. Available skills: "
                        + availableSkillNames()));
  }

  private String availableSkillNames() {
    return skillStore.all().stream().map(Skill::name).sorted().collect(Collectors.joining(", "));
  }
}
