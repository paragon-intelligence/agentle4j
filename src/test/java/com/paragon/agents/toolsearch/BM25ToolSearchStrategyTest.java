package com.paragon.agents.toolsearch;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BM25ToolSearchStrategy")
class BM25ToolSearchStrategyTest {

  public record EmptyArgs() {}

  @FunctionMetadata(name = "get_weather", description = "Get the current weather forecast for a city")
  static class WeatherTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("sunny");
    }
  }

  @FunctionMetadata(
      name = "search_database",
      description = "Search and query records in the company database")
  static class DatabaseTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("found");
    }
  }

  @FunctionMetadata(name = "send_email", description = "Send an email message to a recipient")
  static class EmailTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("sent");
    }
  }

  @FunctionMetadata(name = "calculate_tax", description = "Calculate tax for a transaction amount")
  static class TaxTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("calculated");
    }
  }

  @FunctionMetadata(name = "create_ticket", description = "Create a support ticket for customer issues")
  static class TicketTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("created");
    }
  }

  @FunctionMetadata(name = "weather_forecast_detailed", description = "Get detailed weather forecast with temperature and humidity")
  static class DetailedWeatherTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("detailed");
    }
  }

  private final List<FunctionTool<?>> allTools =
      List.of(
          new WeatherTool(),
          new DatabaseTool(),
          new EmailTool(),
          new TaxTool(),
          new TicketTool(),
          new DetailedWeatherTool());

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("default constructor creates strategy with maxResults=5")
    void defaultConstructor() {
      BM25ToolSearchStrategy strategy = new BM25ToolSearchStrategy();
      assertEquals(5, strategy.maxResults());
    }

    @Test
    @DisplayName("throws on maxResults < 1")
    void throwsOnInvalidMaxResults() {
      assertThrows(IllegalArgumentException.class, () -> new BM25ToolSearchStrategy(0));
    }
  }

  @Nested
  @DisplayName("Relevance Ranking")
  class RankingTests {

    @Test
    @DisplayName("ranks weather tools higher for weather query")
    void ranksWeatherToolsHigher() {
      var strategy = new BM25ToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("weather forecast", allTools);

      assertFalse(results.isEmpty());
      // The weather tools should be ranked first
      assertTrue(
          results.getFirst().getName().contains("weather"),
          "Expected weather tool first, got: " + results.getFirst().getName());
    }

    @Test
    @DisplayName("email query ranks email tool first")
    void emailQueryRanksEmailFirst() {
      var strategy = new BM25ToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("send email message", allTools);

      assertFalse(results.isEmpty());
      assertEquals("send_email", results.getFirst().getName());
    }

    @Test
    @DisplayName("respects maxResults")
    void respectsMaxResults() {
      var strategy = new BM25ToolSearchStrategy(2);
      List<FunctionTool<?>> results = strategy.search("weather forecast", allTools);

      assertTrue(results.size() <= 2);
    }

    @Test
    @DisplayName("returns empty for non-matching query")
    void emptyForNonMatchingQuery() {
      var strategy = new BM25ToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("xyznonexistent", allTools);

      assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("returns empty for blank query")
    void emptyForBlankQuery() {
      var strategy = new BM25ToolSearchStrategy(5);
      assertTrue(strategy.search("", allTools).isEmpty());
      assertTrue(strategy.search("   ", allTools).isEmpty());
    }

    @Test
    @DisplayName("handles single-word query")
    void handlesSingleWordQuery() {
      var strategy = new BM25ToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("database", allTools);

      assertFalse(results.isEmpty());
      assertEquals("search_database", results.getFirst().getName());
    }

    @Test
    @DisplayName("returns unmodifiable list")
    void returnsUnmodifiableList() {
      var strategy = new BM25ToolSearchStrategy(5);
      List<FunctionTool<?>> results = strategy.search("weather", allTools);

      assertThrows(UnsupportedOperationException.class, () -> results.add(new WeatherTool()));
    }
  }
}
