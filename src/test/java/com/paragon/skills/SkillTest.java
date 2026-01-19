package com.paragon.skills;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.prompts.Prompt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Skill class.
 */
@DisplayName("Skill")
class SkillTest {

  @Nested
  @DisplayName("Builder")
  class Builder {

    @Test
    @DisplayName("builder() creates new builder instance")
    void builder_createsNewInstance() {
      Skill.Builder builder = Skill.builder();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() creates skill with required fields")
    void build_createsSkillWithRequiredFields() {
      Skill skill = Skill.builder()
          .name("test-skill")
          .description("A test skill")
          .instructions("You are a test skill")
          .build();

      assertEquals("test-skill", skill.name());
      assertEquals("A test skill", skill.description());
      assertEquals("You are a test skill", skill.instructions().text());
    }

    @Test
    @DisplayName("build() throws when name is null")
    void build_throwsWhenNameNull() {
      assertThrows(NullPointerException.class, () -> {
        Skill.builder()
            .description("A test skill")
            .instructions("Instructions")
            .build();
      });
    }

    @Test
    @DisplayName("build() throws when description is null")
    void build_throwsWhenDescriptionNull() {
      assertThrows(NullPointerException.class, () -> {
        Skill.builder()
            .name("test-skill")
            .instructions("Instructions")
            .build();
      });
    }

    @Test
    @DisplayName("build() throws when instructions is null")
    void build_throwsWhenInstructionsNull() {
      assertThrows(NullPointerException.class, () -> {
        Skill.builder()
            .name("test-skill")
            .description("A test skill")
            .build();
      });
    }

    @Test
    @DisplayName("name must be lowercase with hyphens")
    void name_mustBeLowercaseWithHyphens() {
      assertThrows(IllegalArgumentException.class, () -> {
        Skill.builder()
            .name("Test_Skill")
            .description("A test skill")
            .instructions("Instructions")
            .build();
      });
    }

    @Test
    @DisplayName("name allows lowercase letters, numbers, and hyphens")
    void name_allowsValidCharacters() {
      Skill skill = Skill.builder()
          .name("test-skill-123")
          .description("A test skill")
          .instructions("Instructions")
          .build();

      assertEquals("test-skill-123", skill.name());
    }

    @Test
    @DisplayName("description cannot be empty")
    void description_cannotBeEmpty() {
      assertThrows(IllegalArgumentException.class, () -> {
        Skill.builder()
            .name("test-skill")
            .description("")
            .instructions("Instructions")
            .build();
      });
    }

    @Test
    @DisplayName("instructions accepts Prompt")
    void instructions_acceptsPrompt() {
      Prompt prompt = Prompt.of("You are {{role}}");
      Skill skill = Skill.builder()
          .name("test-skill")
          .description("A test skill")
          .instructions(prompt)
          .build();

      assertEquals(prompt, skill.instructions());
    }

    @Test
    @DisplayName("addResource adds resource to skill")
    void addResource_addsResourceToSkill() {
      Skill skill = Skill.builder()
          .name("test-skill")
          .description("A test skill")
          .instructions("Instructions")
          .addResource("FORMS.md", "Form filling guide")
          .build();

      assertEquals(1, skill.resources().size());
      assertTrue(skill.hasResources());
      assertEquals("Form filling guide", skill.resources().get("FORMS.md").text());
    }
  }

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("of(name, description, instructions) creates skill")
    void of_createsSkill() {
      Skill skill = Skill.of("test-skill", "A test skill", "Instructions");

      assertEquals("test-skill", skill.name());
      assertEquals("A test skill", skill.description());
      assertEquals("Instructions", skill.instructions().text());
    }

    @Test
    @DisplayName("of(name, description, Prompt) creates skill with Prompt")
    void of_createsSkillWithPrompt() {
      Prompt prompt = Prompt.of("Instructions");
      Skill skill = Skill.of("test-skill", "A test skill", prompt);

      assertEquals(prompt, skill.instructions());
    }
  }

  @Nested
  @DisplayName("Immutability")
  class Immutability {

    @Test
    @DisplayName("resources() returns unmodifiable map")
    void resources_returnsUnmodifiableMap() {
      Skill skill = Skill.of("test-skill", "A test skill", "Instructions");

      assertThrows(UnsupportedOperationException.class, () -> {
        skill.resources().put("test", Prompt.of("test"));
      });
    }
  }

  @Nested
  @DisplayName("Equality")
  class Equality {

    @Test
    @DisplayName("equals based on name only")
    void equals_basedOnNameOnly() {
      Skill skill1 = Skill.of("same-name", "Description 1", "Instructions 1");
      Skill skill2 = Skill.of("same-name", "Description 2", "Instructions 2");

      assertEquals(skill1, skill2);
      assertEquals(skill1.hashCode(), skill2.hashCode());
    }

    @Test
    @DisplayName("not equal if different names")
    void notEqual_ifDifferentNames() {
      Skill skill1 = Skill.of("skill-1", "Description", "Instructions");
      Skill skill2 = Skill.of("skill-2", "Description", "Instructions");

      assertNotEquals(skill1, skill2);
    }
  }

  @Nested
  @DisplayName("Prompt Generation")
  class PromptGeneration {

    @Test
    @DisplayName("toPromptSection includes name and description")
    void toPromptSection_includesNameAndDescription() {
      Skill skill = Skill.of("test-skill", "A test skill", "You help with tasks.");

      String section = skill.toPromptSection();
      
      assertTrue(section.contains("## Skill: test-skill"));
      assertTrue(section.contains("**When to use**: A test skill"));
      assertTrue(section.contains("You help with tasks."));
    }

    @Test
    @DisplayName("toPromptSection includes resources")
    void toPromptSection_includesResources() {
      Skill skill = Skill.builder()
          .name("test-skill")
          .description("A test skill")
          .instructions("Instructions")
          .addResource("FORMS.md", "Form filling guide")
          .build();

      String section = skill.toPromptSection();
      
      assertTrue(section.contains("### FORMS.md"));
      assertTrue(section.contains("Form filling guide"));
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("toString includes name and counts")
    void toString_includesNameAndCounts() {
      Skill skill = Skill.builder()
          .name("test-skill")
          .description("A test skill")
          .instructions("Instructions")
          .addResource("FORMS.md", "Guide")
          .build();

      String str = skill.toString();
      assertTrue(str.contains("test-skill"));
      assertTrue(str.contains("resources=1"));
    }
  }
}
