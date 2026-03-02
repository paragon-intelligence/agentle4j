import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 9: Streaming Response
 *
 * <p>Demonstrates real-time streaming of text deltas using virtual threads.
 * Attach callbacks via {@code onTextDelta}, {@code onComplete}, and {@code onError},
 * then call {@code start()} to begin the stream.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class StreamingResponse {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 9: Streaming Response");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    var payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful assistant.")
            .addUserMessage("Tell me a short poem about the ocean.")
            .streaming()
            .build();

    System.out.println("🎬 Streaming:");
    responder
        .respond(payload)
        .onTextDelta(
            delta -> {
              System.out.print(delta);
              System.out.flush();
            })
        .onComplete(response -> System.out.println("\n✅ Stream completed!"))
        .onError(error -> System.out.println("\n❌ Error: " + error.getMessage()))
        .start();

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
