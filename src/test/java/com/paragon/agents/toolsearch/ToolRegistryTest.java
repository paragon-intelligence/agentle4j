package com.paragon.agents.toolsearch;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ToolRegistry")
class ToolRegistryTest {

  public record EmptyArgs() {}

  @FunctionMetadata(name = "critical_tool", description = "A critical tool that's always needed")
  static class CriticalTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("critical");
    }
  }

  @FunctionMetadata(name = "rare_weather_tool", description = "Get weather forecast for a location")
  static class RareWeatherTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("weather");
    }
  }

  @FunctionMetadata(name = "rare_email_tool", description = "Send an email message")
  static class RareEmailTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("email");
    }
  }

  @FunctionMetadata(name = "rare_database_tool", description = "Search database records")
  static class RareDatabaseTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("database");
    }
  }

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builds with eager tools only")
    void buildsWithEagerOnly() {
      ToolRegistry registry =
          ToolRegistry.builder().eagerTool(new CriticalTool()).build();

      assertEquals(1, registry.eagerTools().size());
      assertTrue(registry.deferredTools().isEmpty());
      assertEquals(1, registry.allTools().size());
    }

    @Test
    @DisplayName("builds with strategy and deferred tools")
    void buildsWithDeferredTools() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new BM25ToolSearchStrategy(5))
              .deferredTool(new RareWeatherTool())
              .deferredTool(new RareEmailTool())
              .build();

      assertTrue(registry.eagerTools().isEmpty());
      assertEquals(2, registry.deferredTools().size());
      assertEquals(2, registry.allTools().size());
    }

    @Test
    @DisplayName("builds with both eager and deferred tools")
    void buildsWithBothTypes() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .eagerTool(new CriticalTool())
              .deferredTool(new RareWeatherTool())
              .deferredTool(new RareEmailTool())
              .build();

      assertEquals(1, registry.eagerTools().size());
      assertEquals(2, registry.deferredTools().size());
      assertEquals(3, registry.allTools().size());
    }

    @Test
    @DisplayName("throws when deferred tools but no strategy")
    void throwsWhenDeferredButNoStrategy() {
      assertThrows(
          IllegalStateException.class,
          () -> ToolRegistry.builder().deferredTool(new RareWeatherTool()).build());
    }

    @Test
    @DisplayName("bulk add via deferredTools(List)")
    void bulkAddDeferredTools() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new BM25ToolSearchStrategy(5))
              .deferredTools(List.of(new RareWeatherTool(), new RareEmailTool(), new RareDatabaseTool()))
              .build();

      assertEquals(3, registry.deferredTools().size());
    }

    @Test
    @DisplayName("bulk add via eagerTools(List)")
    void bulkAddEagerTools() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .eagerTools(List.of(new CriticalTool(), new RareWeatherTool()))
              .build();

      assertEquals(2, registry.eagerTools().size());
    }
  }

  @Nested
  @DisplayName("ResolveTools")
  class ResolveTests {

    @Test
    @DisplayName("always includes eager tools")
    void alwaysIncludesEagerTools() {
      CriticalTool criticalTool = new CriticalTool();
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .eagerTool(criticalTool)
              .deferredTool(new RareWeatherTool())
              .build();

      List<FunctionTool<?>> resolved = registry.resolveTools("database query");
      assertTrue(
          resolved.stream().anyMatch(t -> t.getName().equals("critical_tool")),
          "Eager tools should always be included");
    }

    @Test
    @DisplayName("includes matching deferred tools")
    void includesMatchingDeferredTools() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .deferredTool(new RareWeatherTool())
              .deferredTool(new RareEmailTool())
              .build();

      List<FunctionTool<?>> resolved = registry.resolveTools("weather forecast");
      assertTrue(
          resolved.stream().anyMatch(t -> t.getName().equals("rare_weather_tool")),
          "Matching deferred tool should be included");
    }

    @Test
    @DisplayName("excludes non-matching deferred tools")
    void excludesNonMatchingDeferredTools() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .deferredTool(new RareWeatherTool())
              .deferredTool(new RareEmailTool())
              .build();

      List<FunctionTool<?>> resolved = registry.resolveTools("weather forecast");
      assertFalse(
          resolved.stream().anyMatch(t -> t.getName().equals("rare_email_tool")),
          "Non-matching deferred tool should NOT be included");
    }

    @Test
    @DisplayName("returns only eager when no deferred match")
    void returnsOnlyEagerWhenNoMatch() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .eagerTool(new CriticalTool())
              .deferredTool(new RareWeatherTool())
              .build();

      List<FunctionTool<?>> resolved = registry.resolveTools("xyznonexistent");

      assertEquals(1, resolved.size());
      assertEquals("critical_tool", resolved.getFirst().getName());
    }

    @Test
    @DisplayName("returns eager tools when no deferred tools configured")
    void returnsEagerOnlyWhenNoDeferredConfigured() {
      ToolRegistry registry =
          ToolRegistry.builder().eagerTool(new CriticalTool()).build();

      List<FunctionTool<?>> resolved = registry.resolveTools("anything");

      assertEquals(1, resolved.size());
    }

    @Test
    @DisplayName("resolveTools returns unmodifiable list")
    void returnsUnmodifiableList() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .eagerTool(new CriticalTool())
              .deferredTool(new RareWeatherTool())
              .build();

      List<FunctionTool<?>> resolved = registry.resolveTools("weather");

      assertThrows(UnsupportedOperationException.class, () -> resolved.add(new CriticalTool()));
    }
  }

  @Nested
  @DisplayName("AllTools")
  class AllToolsTests {

    @Test
    @DisplayName("allTools returns union of eager and deferred")
    void returnsUnion() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new BM25ToolSearchStrategy(5))
              .eagerTool(new CriticalTool())
              .deferredTool(new RareWeatherTool())
              .deferredTool(new RareEmailTool())
              .build();

      assertEquals(3, registry.allTools().size());
    }

    @Test
    @DisplayName("allTools is unmodifiable")
    void allToolsIsUnmodifiable() {
      ToolRegistry registry = ToolRegistry.builder().eagerTool(new CriticalTool()).build();

      assertThrows(UnsupportedOperationException.class,
          () -> registry.allTools().add(new RareWeatherTool()));
    }
  }
}
