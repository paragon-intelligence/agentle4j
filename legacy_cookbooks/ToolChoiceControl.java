import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.FunctionTool;
import com.paragon.agents.FunctionToolFactory;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ToolChoiceMode;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Cookbook Example 7: Tool Choice Control
 *
 * <p>Demonstrates the three {@link ToolChoiceMode} options:
 * <ul>
 *   <li>{@code AUTO} – model decides whether to call a tool (default)
 *   <li>{@code REQUIRED} – model must call at least one tool
 *   <li>{@code NONE} – model must not call any tools
 * </ul>
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class ToolChoiceControl {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  /** Temperature unit enumeration. */
  public enum TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
  }

  /** Parameters for the get_weather function tool. */
  public record GetWeatherParams(@NonNull String location, @NonNull TemperatureUnit unit) {}

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

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 7: Tool Choice Control");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaProducer schemaProducer = new JacksonJsonSchemaProducer(objectMapper);
    FunctionToolFactory toolFactory = FunctionToolFactory.withProducer(schemaProducer);
    FunctionTool<GetWeatherParams> weatherTool = toolFactory.create(GetWeatherTool.class);

    List<Message> messages =
        List.of(
            Message.developer("You are a weather assistant."),
            Message.user("What's the weather in Tokyo?"));

    // REQUIRED: Force tool call
    CreateResponsePayload requiredPayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addMessages(messages)
            .addTool(weatherTool)
            .toolChoice(ToolChoiceMode.REQUIRED)
            .build();

    Response required = responder.respond(requiredPayload);
    System.out.println(
        "🔧 ToolChoice=REQUIRED - Tool called: " + !required.functionToolCalls().isEmpty());

    // NONE: Prevent tool calling
    CreateResponsePayload nonePayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addMessages(messages)
            .addTool(weatherTool)
            .toolChoice(ToolChoiceMode.NONE)
            .build();

    Response none = responder.respond(nonePayload);
    System.out.println("🚫 ToolChoice=NONE - Response: " + none.outputText());
  }
}
