package com.paragon.agents.toolsearch;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RegexToolSearchStrategy")
class RegexToolSearchStrategyTest {

  // Test tools with distinct names/descriptions
  public record EmptyArgs() {}

  @FunctionMetadata(name = "get_weather", description = "Get the current weather forecast")
  static class WeatherTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("sunny");
    }
  }

  @FunctionMetadata(
      name = "search_database",
      description = "Search records in the company database")
  static class DatabaseTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("found");
    }
  }

  @FunctionMetadata(name = "send_email", description = "Send an email to a recipient")
  static class EmailTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("sent");
    }
  }

  @FunctionMetadata(name = "calculate_tax", description = "Calculate tax for a given amount")
  static class TaxTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("calculated");
    }
  }

  @FunctionMetadata(name = "create_ticket", description = "Create a support ticket")
  static class TicketTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("created");
    }
  }

  private final List<FunctionTool<?>> allTools =
      List.of(new WeatherTool(), new DatabaseTool(), new EmailTool(), new TaxTool(), new TicketTool());

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("default constructor creates strategy with maxResults=5")
    void defaultConstructor() {
      RegexToolSearchStrategy strategy = new RegexToolSearchStrategy();
      assertEquals(5, strategy.maxResults());
    }

    @Test
    @DisplayName("custom maxResults is respected")
    void customMaxResults() {
      RegexToolSearchStrategy strategy = new RegexToolSearchStrategy(3);
      assertEquals(3, strategy.maxResults());
    }

    @Test
    @DisplayName("throws on maxResults < 1")
    void throwsOnInvalidMaxResults() {
      assertThrows(IllegalArgumentException.class, () -> new RegexToolSearchStrategy(0));
      assertThrows(IllegalArgumentException.class, () -> new RegexToolSearchStrategy(-1));
    }
  }

  @Nested
  @DisplayName("Search")
  class SearchTests {

    @Test
    @DisplayName("matches tool by name")
    void matchesByName() {
      var strategy = new RegexToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("weather", allTools);

      assertEquals(1, results.size());
      assertEquals("get_weather", results.getFirst().getName());
    }

    @Test
    @DisplayName("matches tool by description")
    void matchesByDescription() {
      var strategy = new RegexToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("forecast", allTools);

      assertEquals(1, results.size());
      assertEquals("get_weather", results.getFirst().getName());
    }

    @Test
    @DisplayName("case insensitive matching")
    void caseInsensitive() {
      var strategy = new RegexToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("WEATHER", allTools);

      assertEquals(1, results.size());
      assertEquals("get_weather", results.getFirst().getName());
    }

    @Test
    @DisplayName("matches multiple tools with OR semantics")
    void matchesMultipleWithOr() {
      var strategy = new RegexToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("weather email", allTools);

      assertEquals(2, results.size());
    }

    @Test
    @DisplayName("respects maxResults limit")
    void respectsMaxResults() {
      var strategy = new RegexToolSearchStrategy(1);
      // "a" appears in many tool names/descriptions
      List<FunctionTool<?>> results = strategy.search("a", allTools);

      assertEquals(1, results.size());
    }

    @Test
    @DisplayName("returns empty on blank query")
    void emptyOnBlankQuery() {
      var strategy = new RegexToolSearchStrategy(5);

      assertTrue(strategy.search("", allTools).isEmpty());
      assertTrue(strategy.search("   ", allTools).isEmpty());
    }

    @Test
    @DisplayName("returns empty on empty tools list")
    void emptyOnEmptyTools() {
      var strategy = new RegexToolSearchStrategy(5);

      assertTrue(strategy.search("weather", List.of()).isEmpty());
    }

    @Test
    @DisplayName("returns empty when no match")
    void emptyWhenNoMatch() {
      var strategy = new RegexToolSearchStrategy(5);

      assertTrue(strategy.search("xyznonexistent", allTools).isEmpty());
    }

    @Test
    @DisplayName("returns unmodifiable list")
    void returnsUnmodifiableList() {
      var strategy = new RegexToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("weather", allTools);

      assertThrows(UnsupportedOperationException.class, () -> results.add(new WeatherTool()));
    }
  }
}
