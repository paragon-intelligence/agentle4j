import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;
import com.paragon.agents.Interactable;
import com.paragon.agents.RouterAgent;
import com.paragon.responses.Responder;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 15: Router Agent (Classification)
 *
 * <p>Demonstrates intelligent routing of user inputs to the most appropriate
 * specialised agent. The {@link RouterAgent} uses LLM classification to pick
 * the right handler from provided routes, with an optional fallback agent.
 *
 * <p>Also shows how to use {@code classify} to preview the routing decision
 * without executing the agent.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class RouterAgent {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n🤖 Example 15: Router Agent (Classification)");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Create specialist agents
    Agent techSupport =
        Agent.builder()
            .name("TechSupport")
            .instructions("You help with technical issues. Be technical and precise.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    Agent salesAgent =
        Agent.builder()
            .name("Sales")
            .instructions("You help with pricing and purchases. Be persuasive and helpful.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    Agent generalAgent =
        Agent.builder()
            .name("GeneralSupport")
            .instructions("You handle general inquiries.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    // Build the router with topic-based routes and a fallback
    RouterAgent router =
        RouterAgent.builder()
            .model(DEFAULT_MODEL)
            .responder(responder)
            .addRoute(techSupport, "technical issues, bugs, errors, crashes, not working")
            .addRoute(salesAgent, "pricing, purchase, buy, upgrade, subscription")
            .fallback(generalAgent)
            .build();

    // Test routing with a technical query
    String techQuery = "My application keeps crashing when I try to save files.";
    System.out.println("📤 Query: '" + techQuery + "'");

    // Classify without executing to preview the routing decision
    var classified = router.classify(techQuery);
    System.out.println(
        "📍 Would route to: " + classified.map(Interactable::name).orElse("fallback"));

    // Route and execute
    AgentResult result = router.interact(techQuery);
    System.out.println("📥 Response: " + result.output());
  }
}
