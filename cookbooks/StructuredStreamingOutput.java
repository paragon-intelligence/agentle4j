import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 10: Structured Streaming Output
 *
 * <p>Demonstrates streaming a structured JSON response from the model and parsing it
 * into a typed record via {@code onParsedComplete}. This is useful when you need
 * real-time token display AND a final parsed object.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class StructuredStreamingOutput {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  /** Structured output record streamed and parsed by the model. */
  public record StreamedPerson(String name, int age, String occupation, String bio) {}

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 10: Structured Streaming Output");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    var payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful assistant that generates person data.")
            .addUserMessage("Create a fictional software engineer from Brazil named João.")
            .withStructuredOutput(StreamedPerson.class)
            .streaming()
            .build();

    System.out.println("🎬 Streaming JSON:");
    responder
        .respond(payload)
        .onTextDelta(
            delta -> {
              System.out.print(delta);
              System.out.flush();
            })
        .onParsedComplete(
            parsed -> {
              StreamedPerson person = parsed.outputParsed();
              System.out.println(
                  "\n✅ Parsed: "
                      + person.name()
                      + ", "
                      + person.age()
                      + " yo, "
                      + person.occupation());
            })
        .onError(error -> System.out.println("\n❌ Error: " + error.getMessage()))
        .start();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
