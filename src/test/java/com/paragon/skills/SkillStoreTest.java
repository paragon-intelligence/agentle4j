package com.paragon.skills;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the SkillStore class. */
@DisplayName("SkillStore")
class SkillStoreTest {

  private SkillStore store;

  @BeforeEach
  void setUp() {
    store = new SkillStore();
  }

  @Nested
  @DisplayName("Registration")
  class Registration {

    @Test
    @DisplayName("register adds skill to store")
    void register_addsSkillToStore() {
      Skill skill = createSkill("test-skill");
      store.register(skill);

      assertTrue(store.contains("test-skill"));
      assertEquals(1, store.size());
    }

    @Test
    @DisplayName("register replaces existing skill with same name")
    void register_replacesExistingSkill() {
      Skill skill1 = Skill.of("test-skill", "Description 1", "Instructions 1");
      Skill skill2 = Skill.of("test-skill", "Description 2", "Instructions 2");

      store.register(skill1);
      store.register(skill2);

      assertEquals(1, store.size());
      assertEquals("Description 2", store.get("test-skill").orElseThrow().description());
    }

    @Test
    @DisplayName("registerAll adds multiple skills")
    void registerAll_addsMultipleSkills() {
      Skill skill1 = createSkill("skill-1");
      Skill skill2 = createSkill("skill-2");

      store.registerAll(skill1, skill2);

      assertEquals(2, store.size());
      assertTrue(store.contains("skill-1"));
      assertTrue(store.contains("skill-2"));
    }

    @Test
    @DisplayName("register returns store for chaining")
    void register_returnsStoreForChaining() {
      Skill skill1 = createSkill("skill-1");
      Skill skill2 = createSkill("skill-2");

      SkillStore result = store.register(skill1).register(skill2);

      assertSame(store, result);
      assertEquals(2, store.size());
    }
  }

  @Nested
  @DisplayName("Retrieval")
  class Retrieval {

    @Test
    @DisplayName("get returns Optional with skill when found")
    void get_returnsOptionalWithSkill() {
      Skill skill = createSkill("test-skill");
      store.register(skill);

      Optional<Skill> result = store.get("test-skill");

      assertTrue(result.isPresent());
      assertEquals(skill, result.get());
    }

    @Test
    @DisplayName("get returns empty Optional when not found")
    void get_returnsEmptyOptionalWhenNotFound() {
      Optional<Skill> result = store.get("nonexistent");

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("all returns list of all skills")
    void all_returnsListOfAllSkills() {
      store.register(createSkill("skill-1"));
      store.register(createSkill("skill-2"));

      List<Skill> all = store.all();

      assertEquals(2, all.size());
    }

    @Test
    @DisplayName("names returns set of skill names")
    void names_returnsSetOfSkillNames() {
      store.register(createSkill("skill-1"));
      store.register(createSkill("skill-2"));

      var names = store.names();

      assertEquals(2, names.size());
      assertTrue(names.contains("skill-1"));
      assertTrue(names.contains("skill-2"));
    }
  }

  @Nested
  @DisplayName("Removal")
  class Removal {

    @Test
    @DisplayName("remove removes skill and returns it")
    void remove_removesSkillAndReturnsIt() {
      Skill skill = createSkill("test-skill");
      store.register(skill);

      Optional<Skill> removed = store.remove("test-skill");

      assertTrue(removed.isPresent());
      assertEquals(skill, removed.get());
      assertFalse(store.contains("test-skill"));
    }

    @Test
    @DisplayName("remove returns empty when skill not found")
    void remove_returnsEmptyWhenNotFound() {
      Optional<Skill> removed = store.remove("nonexistent");

      assertTrue(removed.isEmpty());
    }

    @Test
    @DisplayName("clear removes all skills")
    void clear_removesAllSkills() {
      store.register(createSkill("skill-1"));
      store.register(createSkill("skill-2"));

      store.clear();

      assertTrue(store.isEmpty());
      assertEquals(0, store.size());
    }
  }

  @Nested
  @DisplayName("Prompt Generation")
  class PromptGeneration {

    @Test
    @DisplayName("generatePromptSection returns empty for empty store")
    void generatePromptSection_returnsEmptyForEmptyStore() {
      String section = store.generatePromptSection();

      assertEquals("", section);
    }

    @Test
    @DisplayName("generatePromptSection lists all skills")
    void generatePromptSection_listsAllSkills() {
      store.register(Skill.of("pdf-processor", "Process PDF files", "Instructions"));
      store.register(Skill.of("data-analyzer", "Analyze data", "Instructions"));

      String section = store.generatePromptSection();

      assertTrue(section.contains("pdf-processor"));
      assertTrue(section.contains("Process PDF files"));
      assertTrue(section.contains("data-analyzer"));
      assertTrue(section.contains("Analyze data"));
    }

    @Test
    @DisplayName("generateCompactPromptSection creates one-liner")
    void generateCompactPromptSection_createsOneLiner() {
      store.register(Skill.of("pdf-processor", "Process PDF files", "Instructions"));

      String section = store.generateCompactPromptSection();

      assertTrue(section.contains("pdf-processor"));
      assertFalse(section.contains("\n"));
    }
  }

  @Nested
  @DisplayName("Constructors")
  class Constructors {

    @Test
    @DisplayName("constructor with varargs registers skills")
    void constructor_withVarargs_registersSkills() {
      Skill skill1 = createSkill("skill-1");
      Skill skill2 = createSkill("skill-2");

      SkillStore store = new SkillStore(skill1, skill2);

      assertEquals(2, store.size());
    }

    @Test
    @DisplayName("constructor with list registers skills")
    void constructor_withList_registersSkills() {
      Skill skill1 = createSkill("skill-1");
      Skill skill2 = createSkill("skill-2");

      SkillStore store = new SkillStore(List.of(skill1, skill2));

      assertEquals(2, store.size());
    }
  }

  // Helper methods

  private Skill createSkill(String name) {
    return Skill.of(name, "Description for " + name, "Instructions for " + name);
  }
}
