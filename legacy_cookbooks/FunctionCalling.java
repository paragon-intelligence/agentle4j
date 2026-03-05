import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.FunctionTool;
import com.paragon.agents.FunctionToolFactory;
import com.paragon.agents.FunctionToolStore;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import com.paragon.responses.spec.BoundedFunctionCall;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Response;
import io.github.cdimascio.dotenv.Dotenv;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Cookbook Example 3: Function Calling
 *
 * <p>Demonstrates how to expose custom tools to the model using {@link FunctionTool},
 * then execute the model-requested tool calls via {@link FunctionToolStore}.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class FunctionCalling {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  /** Temperature unit enumeration. */
  public enum TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
  }

  /** Parameters for the get_weather function tool. */
  public record GetWeatherParams(@NonNull String location, @NonNull TemperatureUnit unit) {}

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
      if (params == null) {
        return FunctionToolCallOutput.error("No parameters provided");
      }

      String unitSymbol = params.unit() == TemperatureUnit.CELSIUS ? "°C" : "°F";
      int temperature = params.unit() == TemperatureUnit.CELSIUS ? 25 : 77;

      return FunctionToolCallOutput.success(
          String.format(
              "The weather in %s is %d%s and sunny.", params.location(), temperature, unitSymbol));
    }
  }

  public static void main(String[] args) throws JsonProcessingException {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 3: Function Calling");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaProducer schemaProducer = new JacksonJsonSchemaProducer(objectMapper);
    FunctionToolFactory toolFactory = FunctionToolFactory.withProducer(schemaProducer);
    FunctionToolStore toolStore = FunctionToolStore.create(objectMapper);

    FunctionTool<GetWeatherParams> weatherTool = toolFactory.create(GetWeatherTool.class);
    toolStore.add(weatherTool);

    CreateResponsePayload payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful weather assistant.")
            .addUserMessage("What's the weather like in Tokyo? Use celsius.")
            .addTool(weatherTool)
            .build();

    Response response = responder.respond(payload);

    var calledFunctions = response.functionToolCalls(toolStore);
    for (BoundedFunctionCall calledFunction : calledFunctions) {
      var output = calledFunction.call();
      System.out.println("🔧 Tool Result: " + output);
    }
  }
}
