package com.paragon.skills;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.FunctionToolCallOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the SkillReaderTool. */
@DisplayName("SkillReaderTool")
class SkillReaderToolTest {

  private SkillStore store;
  private SkillReaderTool tool;

  @BeforeEach
  void setUp() {
    store = new SkillStore();
    store.register(
        Skill.builder()
            .name("pdf-processor")
            .description("Process PDF files, extract text, fill forms")
            .instructions(
                "You are a PDF processing expert.\n"
                    + "1. Analyze the document structure\n"
                    + "2. Extract or modify content")
            .addResource("FORMS.md", "Form filling guide content here")
            .build());
    store.register(
        Skill.of("data-analyzer", "Analyze data and produce insights", "You are a data expert."));
    tool = new SkillReaderTool(store);
  }

  @Nested
  @DisplayName("Metadata")
  class Metadata {

    @Test
    @DisplayName("tool name is read_skill")
    void toolName_isReadSkill() {
      assertEquals("read_skill", tool.getName());
    }

    @Test
    @DisplayName("tool has a description")
    void toolHasDescription() {
      assertNotNull(tool.getDescription());
      assertTrue(tool.getDescription().contains("skill"));
    }
  }

  @Nested
  @DisplayName("Successful Calls")
  class SuccessfulCalls {

    @Test
    @DisplayName("returns full skill content when skill exists")
    void call_returnsFullContent_whenSkillExists() {
      FunctionToolCallOutput output = tool.call(new SkillReaderTool.Params("pdf-processor"));

      String text = output.output().toString();
      assertTrue(text.contains("## Skill: pdf-processor"));
      assertTrue(text.contains("**When to use**: Process PDF files"));
      assertTrue(text.contains("PDF processing expert"));
      assertTrue(text.contains("### FORMS.md"));
      assertTrue(text.contains("Form filling guide content here"));
    }

    @Test
    @DisplayName("returns skill content without resources for simple skills")
    void call_returnsContent_forSimpleSkill() {
      FunctionToolCallOutput output = tool.call(new SkillReaderTool.Params("data-analyzer"));

      String text = output.output().toString();
      assertTrue(text.contains("## Skill: data-analyzer"));
      assertTrue(text.contains("data expert"));
      assertFalse(text.contains("###")); // no resources section
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandling {

    @Test
    @DisplayName("returns error with available skills when skill not found")
    void call_returnsError_whenSkillNotFound() {
      FunctionToolCallOutput output = tool.call(new SkillReaderTool.Params("nonexistent"));

      String text = output.output().toString();
      assertTrue(text.contains("Error"));
      assertTrue(text.contains("nonexistent"));
      assertTrue(text.contains("data-analyzer"));
      assertTrue(text.contains("pdf-processor"));
    }

    @Test
    @DisplayName("returns error when params are null")
    void call_returnsError_whenParamsNull() {
      FunctionToolCallOutput output = tool.call(null);

      String text = output.output().toString();
      assertTrue(text.contains("Error"));
      assertTrue(text.contains("skill_name is required"));
    }

    @Test
    @DisplayName("returns error when skill name is blank")
    void call_returnsError_whenSkillNameBlank() {
      FunctionToolCallOutput output = tool.call(new SkillReaderTool.Params("   "));

      String text = output.output().toString();
      assertTrue(text.contains("Error"));
      assertTrue(text.contains("skill_name is required"));
    }
  }

  @Nested
  @DisplayName("Constructor")
  class Constructor {

    @Test
    @DisplayName("throws when skillStore is null")
    void constructor_throwsWhenSkillStoreNull() {
      assertThrows(NullPointerException.class, () -> new SkillReaderTool(null));
    }
  }
}
