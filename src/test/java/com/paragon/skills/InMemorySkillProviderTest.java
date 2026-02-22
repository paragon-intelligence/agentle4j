package com.paragon.skills;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the InMemorySkillProvider class. */
@DisplayName("InMemorySkillProvider")
class InMemorySkillProviderTest {

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("empty() creates empty provider")
    void empty_createsEmptyProvider() {
      InMemorySkillProvider provider = InMemorySkillProvider.empty();

      assertEquals(0, provider.size());
    }

    @Test
    @DisplayName("of() creates provider with skills")
    void of_createsProviderWithSkills() {
      Skill skill1 = createSkill("skill-1");
      Skill skill2 = createSkill("skill-2");

      InMemorySkillProvider provider = InMemorySkillProvider.of(skill1, skill2);

      assertEquals(2, provider.size());
      assertTrue(provider.exists("skill-1"));
      assertTrue(provider.exists("skill-2"));
    }

    @Test
    @DisplayName("builder pattern works")
    void builder_works() {
      InMemorySkillProvider provider =
          InMemorySkillProvider.builder()
              .add(createSkill("skill-1"))
              .add(createSkill("skill-2"))
              .build();

      assertEquals(2, provider.size());
    }
  }

  @Nested
  @DisplayName("Registration")
  class Registration {

    private InMemorySkillProvider provider;

    @BeforeEach
    void setUp() {
      provider = InMemorySkillProvider.empty();
    }

    @Test
    @DisplayName("register adds skill")
    void register_addsSkill() {
      Skill skill = createSkill("test-skill");

      provider.register(skill);

      assertTrue(provider.exists("test-skill"));
    }

    @Test
    @DisplayName("register returns provider for chaining")
    void register_returnsProviderForChaining() {
      Skill skill = createSkill("test-skill");

      InMemorySkillProvider result = provider.register(skill);

      assertSame(provider, result);
    }

    @Test
    @DisplayName("registerAll adds multiple skills")
    void registerAll_addsMultipleSkills() {
      provider.registerAll(createSkill("skill-1"), createSkill("skill-2"));

      assertEquals(2, provider.size());
    }

    @Test
    @DisplayName("remove removes skill")
    void remove_removesSkill() {
      provider.register(createSkill("test-skill"));

      assertTrue(provider.remove("test-skill"));
      assertFalse(provider.exists("test-skill"));
    }

    @Test
    @DisplayName("remove returns false when skill not found")
    void remove_returnsFalseWhenNotFound() {
      assertFalse(provider.remove("nonexistent"));
    }

    @Test
    @DisplayName("clear removes all skills")
    void clear_removesAllSkills() {
      provider.register(createSkill("skill-1"));
      provider.register(createSkill("skill-2"));

      provider.clear();

      assertEquals(0, provider.size());
    }
  }

  @Nested
  @DisplayName("SkillProvider Interface")
  class SkillProviderInterface {

    private InMemorySkillProvider provider;

    @BeforeEach
    void setUp() {
      provider = InMemorySkillProvider.of(createSkill("skill-1"), createSkill("skill-2"));
    }

    @Test
    @DisplayName("provide returns skill when found")
    void provide_returnsSkillWhenFound() {
      Skill skill = provider.provide("skill-1");

      assertNotNull(skill);
      assertEquals("skill-1", skill.name());
    }

    @Test
    @DisplayName("provide throws when skill not found")
    void provide_throwsWhenNotFound() {
      assertThrows(
          SkillProviderException.class,
          () -> {
            provider.provide("nonexistent");
          });
    }

    @Test
    @DisplayName("exists returns true when skill exists")
    void exists_returnsTrueWhenExists() {
      assertTrue(provider.exists("skill-1"));
    }

    @Test
    @DisplayName("exists returns false when skill not found")
    void exists_returnsFalseWhenNotFound() {
      assertFalse(provider.exists("nonexistent"));
    }

    @Test
    @DisplayName("listSkillIds returns all skill names")
    void listSkillIds_returnsAllSkillNames() {
      Set<String> ids = provider.listSkillIds();

      assertEquals(2, ids.size());
      assertTrue(ids.contains("skill-1"));
      assertTrue(ids.contains("skill-2"));
    }
  }

  // Helper methods

  private Skill createSkill(String name) {
    return Skill.of(name, "Description for " + name, "Instructions for " + name);
  }
}
