import com.paragon.agents.Agent;
import com.paragon.agents.AgenticContext;
import com.paragon.agents.AgentResult;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 11: Basic Agent Interaction
 *
 * <p>Demonstrates creating an {@link Agent} and conducting a multi-turn conversation
 * by reusing the same {@link AgenticContext}. The context accumulates history
 * across turns, enabling the agent to remember previous exchanges.
 *
 * <p>Key insight: create ONE context and REUSE it for multi-turn conversations.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class BasicAgentInteraction {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n🤖 Example 11: Basic Agent Interaction");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Create a simple agent
    Agent agent =
        Agent.builder()
            .name("Assistant")
            .instructions("You are a helpful AI assistant. Be concise and friendly.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    // ⚠️ KEY: Create ONE context and REUSE it for multi-turn conversation
    AgenticContext context = AgenticContext.create();

    System.out.println("📤 Sending: 'What is the capital of France?'");
    context.addInput(Message.user("What is the capital of France?"));
    AgentResult result1 = agent.interact(context);
    System.out.println("📥 Response: " + result1.output());
    System.out.println("📊 Turns used: " + result1.turnsUsed());

    // The context now contains the previous exchange in its history
    System.out.println("\n📤 Sending: 'And what about Germany?' (using same context)");
    context.addInput(Message.user("And what about Germany?"));
    AgentResult result2 = agent.interact(context);
    System.out.println("📥 Response: " + result2.output());
    System.out.println("📊 Context history size: " + context.historySize());
  }
}
