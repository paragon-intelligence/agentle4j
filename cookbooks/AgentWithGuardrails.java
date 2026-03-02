import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;
import com.paragon.agents.GuardrailResult;
import com.paragon.responses.Responder;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cookbook Example 12: Agent with Guardrails
 *
 * <p>Demonstrates input and output validation using guardrail lambdas.
 * <ul>
 *   <li>Input guardrails run before the model is called and can block the request.
 *   <li>Output guardrails run after the model responds and can block the result.
 * </ul>
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY
 * </ul>
 */
public class AgentWithGuardrails {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n🤖 Example 12: Agent with Guardrails");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Create an agent with input and output guardrails
    Agent agent =
        Agent.builder()
            .name("SecureAssistant")
            .instructions("You are a helpful assistant.")
            .model(DEFAULT_MODEL)
            .responder(responder)
            // Input guardrail: block requests containing "password"
            .addInputGuardrail(
                (input, ctx) -> {
                  if (input.toLowerCase().contains("password")) {
                    return GuardrailResult.failed("Cannot discuss passwords for security reasons");
                  }
                  return GuardrailResult.passed();
                })
            // Output guardrail: ensure responses are not too long
            .addOutputGuardrail(
                (output, ctx) -> {
                  if (output.length() > 500) {
                    return GuardrailResult.failed("Response too long");
                  }
                  return GuardrailResult.passed();
                })
            .build();

    // Test 1: Valid input
    System.out.println("📤 Sending: 'Tell me a joke.'");
    AgentResult result1 = agent.interact("Tell me a joke.");
    System.out.println("📥 Response: " + result1.output());

    // Test 2: Blocked by input guardrail
    System.out.println("\n📤 Sending: 'What is my password?'");
    AgentResult result2 = agent.interact("What is my password?");
    if (result2.isError()) {
      System.out.println("🚫 Blocked: " + result2.error().getMessage());
    } else {
      System.out.println("📥 Response: " + result2.output());
    }
  }
}
