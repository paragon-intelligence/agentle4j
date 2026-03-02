import com.paragon.agents.Agent;
import com.paragon.agents.AgenticContext;
import com.paragon.agents.AgentResult;
import com.paragon.agents.FunctionTool;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import io.github.cdimascio.dotenv.Dotenv;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Cookbook Example 17: Agent with Tool Planning
 *
 * <p>Demonstrates how enabling tool planning allows the LLM to batch multiple tool calls
 * into a single declarative execution plan (using {@code execute_tool_plan} meta-tool).
 * The framework resolves {@code $ref} dependencies and executes independent steps in parallel,
 * reducing API round-trips and saving tokens.
 *
 * <p>Flow for "Compare weather in Tokyo and London":
 * <ol>
 *   <li>get_weather(Tokyo) — independent
 *   <li>get_weather(London) — independent (runs in parallel with step 1)
 *   <li>format_report($ref:step1, $ref:step2) — depends on both, runs after
 * </ol>
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class AgentWithToolPlanning {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  /** Temperature unit enumeration. */
  public enum TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
  }

  /** Parameters for the get_weather function tool. */
  public record GetWeatherParams(@NonNull String location, @NonNull TemperatureUnit unit) {}

  /** Parameters for the format_report function tool. */
  public record FormatReportParams(@NonNull String data_a, @NonNull String data_b) {}

  /**
   * Example function tool that retrieves weather information. In a real application, this would
   * call an actual weather API.
   */
  @FunctionMetadata(
      name = "get_weather",
      description = "Gets the current weather in a given location.")
  public static class GetWeatherTool extends FunctionTool<GetWeatherParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable GetWeatherParams params) {
      if (params == null) return FunctionToolCallOutput.error("No parameters provided");
      String unitSymbol = params.unit() == TemperatureUnit.CELSIUS ? "°C" : "°F";
      int temperature = params.unit() == TemperatureUnit.CELSIUS ? 25 : 77;
      return FunctionToolCallOutput.success(
          String.format(
              "The weather in %s is %d%s and sunny.", params.location(), temperature, unitSymbol));
    }
  }

  /**
   * Example function tool that formats a comparison report from two data sources. Used to
   * demonstrate tool planning with $ref dependencies between steps.
   */
  @FunctionMetadata(
      name = "format_report",
      description =
          "Formats a comparison report from two data sources. Takes two text inputs and produces a structured comparison.")
  public static class FormatReportTool extends FunctionTool<FormatReportParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable FormatReportParams params) {
      if (params == null) return FunctionToolCallOutput.error("No parameters provided");
      return FunctionToolCallOutput.success(
          String.format(
              "{\"report\": \"Comparison Report\", \"source_a\": \"%s\", \"source_b\": \"%s\", \"conclusion\": \"Both data sources have been analyzed and compared.\"}",
              params.data_a().replace("\"", "\\\""),
              params.data_b().replace("\"", "\\\"")));
    }
  }

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n🤖 Example 17: Agent with Tool Planning");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Create an agent with tool planning enabled.
    // The LLM gets access to get_weather and format_report tools,
    // PLUS the execute_tool_plan meta-tool for batching calls.
    Agent agent =
        Agent.builder()
            .name("WeatherComparator")
            .instructions(
                """
                You are a weather comparison assistant.
                When asked to compare weather in multiple cities, use execute_tool_plan
                to batch all weather lookups and the final report into a single plan.
                Always use CELSIUS for temperature units.
                """)
            .model(DEFAULT_MODEL)
            .responder(responder)
            .addTool(new GetWeatherTool())
            .addTool(new FormatReportTool())
            .enableToolPlanning() // Registers the execute_tool_plan meta-tool
            .build();

    AgenticContext context = AgenticContext.create();
    context.addInput(
        Message.user(
            "Compare the current weather in Tokyo and London. Get both and then format a comparison report."));

    System.out.println("📤 Sending: 'Compare the weather in Tokyo and London'");
    System.out.println("🔄 The LLM should create an execution plan with $ref dependencies...\n");

    AgentResult result = agent.interact(context);

    System.out.println("📥 Response: " + result.output());
    System.out.println("\n📊 Turns used: " + result.turnsUsed());
    System.out.println(
        "💡 With tool planning, the weather lookups ran in parallel in a single plan!");
  }
}
