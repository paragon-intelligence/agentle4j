import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Response;
import io.github.cdimascio.dotenv.Dotenv;
import org.jspecify.annotations.NonNull;

/**
 * Cookbook Example 2: Structured Output Generation
 *
 * <p>Demonstrates how to instruct the model to return JSON output that conforms to a specific
 * Java record schema using {@code withStructuredOutput}.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class StructuredOutputGeneration {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  /** Example structured output record for weather information. */
  public record WeatherInfo(
      @NonNull String location, @NonNull String description, int temperatureCelsius) {}

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 2: Structured Output Generation");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    CreateResponsePayload payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage(
                "You are a helpful assistant. Always respond with structured data.")
            .addUserMessage("Tell me about the weather in Paris.")
            .withStructuredOutput(WeatherInfo.class)
            .build();

    Response response = responder.respond(payload);
    System.out.println("🌤️ Structured Response: " + response.outputText());
  }
}
