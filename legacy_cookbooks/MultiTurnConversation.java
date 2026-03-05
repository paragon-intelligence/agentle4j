import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Response;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 5: Multi-turn Conversation
 *
 * <p>Demonstrates how to maintain conversation context across multiple turns by manually
 * appending the assistant's previous reply to the next request's message history.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class MultiTurnConversation {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 5: Multi-turn Conversation");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Turn 1
    CreateResponsePayload turn1 =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful math tutor.")
            .addUserMessage("What is the Pythagorean theorem?")
            .build();

    Response response1 = responder.respond(turn1);
    String reply1 = response1.outputText();
    System.out.println("📚 Turn 1: " + reply1);

    // Turn 2 (includes context from turn 1)
    CreateResponsePayload turn2 =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful math tutor.")
            .addUserMessage("What is the Pythagorean theorem?")
            .addAssistantMessage(reply1)
            .addUserMessage("Can you give me an example?")
            .build();

    Response response2 = responder.respond(turn2);
    System.out.println("\n📚 Turn 2: " + response2.outputText());
  }
}
