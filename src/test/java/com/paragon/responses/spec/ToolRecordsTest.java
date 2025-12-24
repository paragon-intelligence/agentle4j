package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Tool-related records: AllowedTools, WebSearchTool.
 */
class ToolRecordsTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Nested
  @DisplayName("AllowedTools")
  class AllowedToolsTests {

    @Test
    @DisplayName("can be created with mode and tools list")
    void creation() {
      // Create a simple tool for testing
      WebSearchTool searchTool = new WebSearchTool(null, null, null);
      AllowedTools allowed = new AllowedTools(AllowedToolsMode.AUTO, List.of(searchTool));

      assertEquals(AllowedToolsMode.AUTO, allowed.mode());
      assertEquals(1, allowed.tools().size());
    }

    @Test
    @DisplayName("toToolChoice returns valid JSON")
    void toToolChoiceFormat() throws JsonProcessingException {
      WebSearchTool searchTool = new WebSearchTool(null, null, null);
      AllowedTools allowed = new AllowedTools(AllowedToolsMode.AUTO, List.of(searchTool));

      String json = allowed.toToolChoice(mapper);

      assertNotNull(json);
      assertTrue(json.contains("mode"));
      assertTrue(json.contains("tools"));
    }

    @Test
    @DisplayName("implements ToolChoice")
    void implementsInterface() {
      AllowedTools allowed = new AllowedTools(AllowedToolsMode.AUTO, List.of());
      assertTrue(allowed instanceof ToolChoice);
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      List<Tool> tools = List.of();
      AllowedTools tools1 = new AllowedTools(AllowedToolsMode.AUTO, tools);
      AllowedTools tools2 = new AllowedTools(AllowedToolsMode.AUTO, tools);
      AllowedTools tools3 = new AllowedTools(AllowedToolsMode.REQUIRED, tools);

      assertEquals(tools1, tools2);
      assertEquals(tools1.hashCode(), tools2.hashCode());
      assertNotEquals(tools1, tools3);
    }
  }

  @Nested
  @DisplayName("WebSearchTool")
  class WebSearchToolTests {

    @Test
    @DisplayName("can be created with all null parameters")
    void creationWithNulls() {
      WebSearchTool tool = new WebSearchTool(null, null, null);

      assertNull(tool.filters());
      assertNull(tool.searchContextSize());
      assertNull(tool.userLocation());
    }

    @Test
    @DisplayName("can be created with context size")
    void creationWithContextSize() {
      WebSearchTool tool = new WebSearchTool(null, WebSearchSearchContextSize.HIGH, null);

      assertEquals(WebSearchSearchContextSize.HIGH, tool.searchContextSize());
    }

    @Test
    @DisplayName("toToolChoice returns web_search type")
    void toToolChoiceFormat() throws JsonProcessingException {
      WebSearchTool tool = new WebSearchTool(null, null, null);

      String json = tool.toToolChoice(mapper);

      assertNotNull(json);
      assertTrue(json.contains("web_search"));
    }

    @Test
    @DisplayName("implements Tool")
    void implementsInterface() {
      WebSearchTool tool = new WebSearchTool(null, null, null);
      assertTrue(tool instanceof Tool);
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      WebSearchTool tool1 = new WebSearchTool(null, WebSearchSearchContextSize.MEDIUM, null);
      WebSearchTool tool2 = new WebSearchTool(null, WebSearchSearchContextSize.MEDIUM, null);
      WebSearchTool tool3 = new WebSearchTool(null, WebSearchSearchContextSize.HIGH, null);

      assertEquals(tool1, tool2);
      assertEquals(tool1.hashCode(), tool2.hashCode());
      assertNotEquals(tool1, tool3);
    }
  }

  @Nested
  @DisplayName("AllowedToolsMode enum")
  class AllowedToolsModeTests {

    @Test
    @DisplayName("has expected values")
    void hasValues() {
      assertTrue(AllowedToolsMode.values().length > 0);
      for (AllowedToolsMode mode : AllowedToolsMode.values()) {
        assertNotNull(mode.name());
      }
    }

    @Test
    @DisplayName("can be used in AllowedTools")
    void usableInAllowedTools() {
      for (AllowedToolsMode mode : AllowedToolsMode.values()) {
        AllowedTools allowed = new AllowedTools(mode, List.of());
        assertEquals(mode, allowed.mode());
      }
    }
  }

  @Nested
  @DisplayName("WebSearchSearchContextSize enum")
  class WebSearchContextSizeTests {

    @Test
    @DisplayName("has expected values")
    void hasValues() {
      assertTrue(WebSearchSearchContextSize.values().length > 0);
      for (WebSearchSearchContextSize size : WebSearchSearchContextSize.values()) {
        assertNotNull(size.name());
      }
    }

    @Test
    @DisplayName("can be used in WebSearchTool")
    void usableInWebSearchTool() {
      for (WebSearchSearchContextSize size : WebSearchSearchContextSize.values()) {
        WebSearchTool tool = new WebSearchTool(null, size, null);
        assertEquals(size, tool.searchContextSize());
      }
    }
  }
}
