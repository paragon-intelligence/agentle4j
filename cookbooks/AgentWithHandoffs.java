import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;
import com.paragon.agents.Handoff;
import com.paragon.responses.Responder;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 13: Agent with Handoffs
 *
 * <p>Demonstrates agent-to-agent transfer (handoff) where a triage agent routes
 * incoming conversations to specialised agents based on the topic.
 *
 * <p>Pattern: TriageAgent → BillingSpecialist (when billing topic detected)
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class AgentWithHandoffs {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n🤖 Example 13: Agent with Handoffs");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Create a specialist agent
    Agent billingAgent =
        Agent.builder()
            .name("BillingSpecialist")
            .instructions("You are a billing specialist. Help with invoices and payments.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    // Create a triage agent that can hand off to billing
    Agent triageAgent =
        Agent.builder()
            .name("TriageAgent")
            .instructions(
                "You are a front-desk agent. Route billing questions to the billing specialist.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            // Add a handoff to billing agent
            .addHandoff(
                Handoff.to(billingAgent)
                    .withDescription("Hand off billing-related questions to the billing specialist")
                    .build())
            .build();

    System.out.println("📤 Sending: 'I have a question about my invoice from last month.'");

    AgentResult result =
        triageAgent.interact("I have a question about my invoice from last month.");
    System.out.println("📥 Response: " + result.output());

    if (result.handoffAgent() != null) {
      System.out.println("🔄 Handed off to: " + result.handoffAgent().name());
    }
  }
}
