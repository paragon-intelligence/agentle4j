import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Image;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.UserMessage;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 6: Vision (Image Input)
 *
 * <p>Demonstrates how to send image content to a vision-capable model by attaching
 * an {@link Image} URL to a user message. The model will describe the provided image.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class Vision {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n📝 Example 6: Vision (Image Analysis)");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    Image imageContent =
        Image.fromUrl(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/1200px-Cat03.jpg");

    UserMessage userMessage =
        Message.builder()
            .addText("What animal is in this image? Describe it briefly.")
            .addContent(imageContent)
            .asUser();

    var payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful image analyst.")
            .addMessage(userMessage)
            .build();

    var response = responder.respond(payload);
    System.out.println("🖼️ Vision Response: " + response.outputText());
  }
}
