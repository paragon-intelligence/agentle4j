import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Response;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 8: Max Tokens & Truncation
 *
 * <p>Demonstrates how to limit the length of the model's response using
 * {@code maxOutputTokens}. This is useful for keeping responses concise or
 * controlling API costs.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class MaxTokens {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 8: Max Tokens & Truncation");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    CreateResponsePayload shortPayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful assistant.")
            .addUserMessage("Explain quantum computing.")
            .maxOutputTokens(50)
            .build();

    Response response = responder.respond(shortPayload);
    System.out.println("📏 Response (max 50 tokens): " + response.outputText());
  }
}
