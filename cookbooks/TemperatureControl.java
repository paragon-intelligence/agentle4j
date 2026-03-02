import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Message;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;

/**
 * Cookbook Example 4: Temperature & Sampling Control
 *
 * <p>Demonstrates how {@code temperature} and {@code topP} sampling parameters affect
 * the creativity and determinism of model responses. Low temperature produces focused,
 * consistent outputs; high temperature produces diverse, creative outputs.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class TemperatureControl {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 4: Temperature & Sampling Control");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    List<Message> messages =
        List.of(
            Message.developer("You are a creative storyteller."),
            Message.user("Write a one-sentence story about a robot."));

    // Low temperature = deterministic
    CreateResponsePayload focusedPayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addMessages(messages)
            .temperature(0.2)
            .build();

    // High temperature = creative
    CreateResponsePayload creativePayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addMessages(messages)
            .temperature(1.5)
            .topP(0.9)
            .build();

    System.out.println("🎯 Low Temperature (0.2) - Focused:");
    System.out.println("   " + responder.respond(focusedPayload).outputText());

    System.out.println("\n🎨 High Temperature (1.5) - Creative:");
    System.out.println("   " + responder.respond(creativePayload).outputText());
  }
}
