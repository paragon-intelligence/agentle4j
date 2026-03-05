import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Response;
import com.paragon.telemetry.langfuse.LangfuseProcessor;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 1: Simple Text Generation
 *
 * <p>Demonstrates basic text generation using the Responder API with OpenRouter.
 * Also shows how to attach a Langfuse telemetry processor to observe requests.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class SimpleTextGeneration {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 1: Simple Text Generation");
    System.out.println("─".repeat(40));

    var langfuseProcessor = LangfuseProcessor.fromEnv();
    Responder responder =
        Responder.builder()
            .openRouter()
            .apiKey(apiKey)
            .addTelemetryProcessor(langfuseProcessor)
            .build();

    var payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful assistant.")
            .addUserMessage("Hello, how are you?")
            .build();

    Response response = responder.respond(payload);
    System.out.println("🤖 Response: " + response.outputText());
  }
}
