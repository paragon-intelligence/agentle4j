import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;
import com.paragon.agents.ParallelAgents;
import com.paragon.responses.Responder;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 14: Parallel Agents (Fan-out / Fan-in)
 *
 * <p>Demonstrates running multiple specialised agents concurrently (fan-out) and
 * then synthesising their outputs with an additional synthesizer agent (fan-in).
 *
 * <p>Flow: Optimist + Pessimist run in parallel → Synthesizer combines results.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class ParallelAgents {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n🤖 Example 14: Parallel Agents (Fan-out/Fan-in)");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Create specialised agents
    Agent optimistAgent =
        Agent.builder()
            .name("Optimist")
            .instructions("You always see the positive side. Give a brief optimistic perspective.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    Agent pessimistAgent =
        Agent.builder()
            .name("Pessimist")
            .instructions("You always consider the risks. Give a brief cautionary perspective.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    Agent synthesizerAgent =
        Agent.builder()
            .name("Synthesizer")
            .instructions("Combine different perspectives into a balanced summary.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            .build();

    // Create parallel orchestrator
    ParallelAgents team = ParallelAgents.of(optimistAgent, pessimistAgent);

    String question = "Should I start a business during a recession?";
    System.out.println("📤 Query: '" + question + "'");
    System.out.println("🔄 Running agents in parallel...\n");

    // Run all agents and synthesize results
    AgentResult synthesized = team.runAndSynthesize(question, synthesizerAgent);
    System.out.println("📥 Synthesized Response:\n" + synthesized.output());
  }
}
