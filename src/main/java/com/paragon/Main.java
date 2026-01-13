/** AI Generated/Updated file to help with the development of the Agentle library. */
package com.paragon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.*;
import com.paragon.responses.Responder;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import com.paragon.responses.spec.*;
import com.paragon.telemetry.langfuse.LangfuseProcessor;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Scanner;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Agentle Usage Examples
 *
 * <p>This class demonstrates the core capabilities of the Agentle library. Use the CLI menu to
 * select and run different examples.
 *
 * <p>Examples included:
 *
 * <ul>
 *   <li>1-10: Responder API Examples (text generation, structured output, streaming, etc.)
 *   <li>11-16: Agent API Examples (basic agent, guardrails, handoffs, parallel agents, etc.)
 * </ul>
 */
public class Main {

  private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    printMenu();
    runCLI(apiKey);
  }

  private static void printMenu() {
    System.out.println("\n" + "═".repeat(70));
    System.out.println("              🚀 AGENTLE USAGE EXAMPLES                ");
    System.out.println("═".repeat(70));
    System.out.println("\n📋 RESPONDER API EXAMPLES:");
    System.out.println("   1.  Simple Text Generation");
    System.out.println("   2.  Structured Output Generation");
    System.out.println("   3.  Function Calling");
    System.out.println("   4.  Temperature & Sampling Control");
    System.out.println("   5.  Multi-turn Conversation");
    System.out.println("   6.  Vision (Image Input)");
    System.out.println("   7.  Tool Choice Control");
    System.out.println("   8.  Max Tokens & Truncation");
    System.out.println("   9.  Streaming Response");
    System.out.println("   10. Structured Streaming Output");
    System.out.println("\n🤖 AGENT API EXAMPLES:");
    System.out.println("   11. Basic Agent Interaction");
    System.out.println("   12. Agent with Guardrails");
    System.out.println("   13. Agent with Handoffs");
    System.out.println("   14. Parallel Agents (Fan-out/Fan-in)");
    System.out.println("   15. Router Agent (Classification)");
    System.out.println("   16. Agent with Memory");
    System.out.println("\n   0.  Exit");
    System.out.println("\n" + "─".repeat(70));
  }

  private static void runCLI(String apiKey) {
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.print("\n👉 Enter example number (0 to exit): ");
      String input = scanner.nextLine().trim();

      if (input.equals("0") || input.equalsIgnoreCase("exit")) {
        System.out.println("\n👋 Goodbye!");
        break;
      }

      try {
        int choice = Integer.parseInt(input);
        runExample(choice, apiKey);
      } catch (NumberFormatException e) {
        System.out.println("❌ Invalid input. Please enter a number.");
      } catch (Exception e) {
        System.out.println("❌ Error running example: " + e.getMessage());
        e.printStackTrace();
      }

      System.out.println("\n" + "─".repeat(70));
      System.out.println("Press Enter to see the menu again...");
      scanner.nextLine();
      printMenu();
    }

    scanner.close();
  }

  private static void runExample(int choice, String apiKey) throws Exception {
    switch (choice) {
      case 1 -> simpleTextGeneration(apiKey);
      case 2 -> structuredOutputGeneration(apiKey);
      case 3 -> functionCallingExample(apiKey);
      case 4 -> temperatureControlExample(apiKey);
      case 5 -> multiTurnConversationExample(apiKey);
      case 6 -> visionExample(apiKey);
      case 7 -> toolChoiceExample(apiKey);
      case 8 -> maxTokensExample(apiKey);
      case 9 -> streamingExample(apiKey);
      case 10 -> structuredStreamingExample(apiKey);
      case 11 -> basicAgentExample(apiKey);
      case 12 -> agentWithGuardrailsExample(apiKey);
      case 13 -> agentWithHandoffsExample(apiKey);
      case 14 -> parallelAgentsExample(apiKey);
      case 15 -> routerAgentExample(apiKey);
      case 16 -> agentWithMemoryExample(apiKey);
      default -> System.out.println("❌ Unknown example number: " + choice);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RESPONDER API EXAMPLES (1-10)
  // ═══════════════════════════════════════════════════════════════════════════

  /** Example 1: Basic text generation with the Responses API. */
  private static void simpleTextGeneration(String apiKey) {
    System.out.println("\n📝 Example 1: Simple Text Generation");
    System.out.println("─".repeat(40));

    var langfuseProcessor = LangfuseProcessor.fromEnv();
    Responder responder =
        Responder.builder()
            .openRouter()
            .apiKey(apiKey)
            .addTelemetryProcessor(langfuseProcessor)
            .build();

    var payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful assistant.")
            .addUserMessage("Hello, how are you?")
            .build();

    Response response = responder.respond(payload);
    System.out.println("🤖 Response: " + response.outputText());
  }

  /** Example 2: Generate structured JSON output matching a specific schema. */
  private static void structuredOutputGeneration(String apiKey) {
    System.out.println("\n📝 Example 2: Structured Output Generation");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    CreateResponsePayload payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage(
                "You are a helpful assistant. Always respond with structured data.")
            .addUserMessage("Tell me about the weather in Paris.")
            .withStructuredOutput(WeatherInfo.class)
            .build();

    Response response = responder.respond(payload);
    System.out.println("🌤️ Structured Response: " + response.outputText());
  }

  /** Example 3: Function calling allows the model to invoke custom tools. */
  private static void functionCallingExample(String apiKey) throws JsonProcessingException {
    System.out.println("\n📝 Example 3: Function Calling");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaProducer schemaProducer = new JacksonJsonSchemaProducer(objectMapper);
    FunctionToolFactory toolFactory = FunctionToolFactory.withProducer(schemaProducer);
    FunctionToolStore toolStore = FunctionToolStore.create(objectMapper);

    FunctionTool<GetWeatherParams> weatherTool = toolFactory.create(GetWeatherTool.class);
    toolStore.add(weatherTool);

    CreateResponsePayload payload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful weather assistant.")
            .addUserMessage("What's the weather like in Tokyo? Use celsius.")
            .addTool(weatherTool)
            .build();

    Response response = responder.respond(payload);

    var calledFunctions = response.functionToolCalls(toolStore);
    for (BoundedFunctionCall calledFunction : calledFunctions) {
      var output = calledFunction.call();
      System.out.println("🔧 Tool Result: " + output);
    }
  }

  /** Example 4: Control randomness using temperature and topP. */
  private static void temperatureControlExample(String apiKey) {
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

  /** Example 5: Multi-turn conversation with context. */
  private static void multiTurnConversationExample(String apiKey) {
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

    // Turn 2 (includes context)
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

  /** Example 6: Vision - send images to vision-capable models. */
  private static void visionExample(String apiKey) {
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

  /** Example 7: Tool choice control - AUTO, REQUIRED, or NONE. */
  private static void toolChoiceExample(String apiKey) {
    System.out.println("\n📝 Example 7: Tool Choice Control");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaProducer schemaProducer = new JacksonJsonSchemaProducer(objectMapper);
    FunctionToolFactory toolFactory = FunctionToolFactory.withProducer(schemaProducer);
    FunctionTool<GetWeatherParams> weatherTool = toolFactory.create(GetWeatherTool.class);

    List<Message> messages =
        List.of(
            Message.developer("You are a weather assistant."),
            Message.user("What's the weather in Tokyo?"));

    // REQUIRED: Force tool call
    CreateResponsePayload requiredPayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addMessages(messages)
            .addTool(weatherTool)
            .toolChoice(ToolChoiceMode.REQUIRED)
            .build();

    Response required = responder.respond(requiredPayload);
    System.out.println(
        "🔧 ToolChoice=REQUIRED - Tool called: " + !required.functionToolCalls().isEmpty());

    // NONE: Prevent tool calling
    CreateResponsePayload nonePayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addMessages(messages)
            .addTool(weatherTool)
            .toolChoice(ToolChoiceMode.NONE)
            .build();

    Response none = responder.respond(nonePayload);
    System.out.println("🚫 ToolChoice=NONE - Response: " + none.outputText());
  }

  /** Example 8: Control response length with max tokens. */
  private static void maxTokensExample(String apiKey) {
    System.out.println("\n📝 Example 8: Max Tokens & Truncation");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    CreateResponsePayload shortPayload =
        CreateResponsePayload.builder()
            .model(DEFAULT_MODEL)
            .addDeveloperMessage("You are a helpful assistant.")
            .addUserMessage("Explain quantum computing.")
            .maxOutputTokens(50)
            .build();

    Response response = responder.respond(shortPayload);
    System.out.println("📏 Response (max 50 tokens): " + response.outputText());
  }

  /** Example 9: Stream responses in real-time using virtual threads. */
  private static void streamingExample(String apiKey) {
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

  /** Example 10: Stream structured output and parse to typed object. */
  private static void structuredStreamingExample(String apiKey) {
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

  // ═══════════════════════════════════════════════════════════════════════════
  // AGENT API EXAMPLES (11-16)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Example 11: Basic Agent Interaction. Demonstrates creating an agent and interacting with it
   * using the async API. Shows how to maintain conversation history by reusing AgentContext.
   */
  private static void basicAgentExample(String apiKey) {
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
    AgentContext context = AgentContext.create();

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

  /** Example 12: Agent with Guardrails. Demonstrates input/output validation using guardrails. */
  private static void agentWithGuardrailsExample(String apiKey) {
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

  /**
   * Example 13: Agent with Handoffs. Demonstrates agent-to-agent transfer when conditions are met.
   */
  private static void agentWithHandoffsExample(String apiKey) {
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

  /**
   * Example 14: Parallel Agents (Fan-out/Fan-in). Demonstrates running multiple agents concurrently
   * and synthesizing results.
   */
  private static void parallelAgentsExample(String apiKey) {
    System.out.println("\n🤖 Example 14: Parallel Agents (Fan-out/Fan-in)");
    System.out.println("─".repeat(40));

    Responder responder = Responder.builder().openRouter().apiKey(apiKey).build();

    // Create specialized agents
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

    // Run all agents and synthesize
    AgentResult synthesized = team.runAndSynthesize(question, synthesizerAgent);
    System.out.println("📥 Synthesized Response:\n" + synthesized.output());
  }

  /**
   * Example 15: Router Agent (Classification). Demonstrates intelligent routing of inputs to
   * appropriate agents.
   */
  private static void routerAgentExample(String apiKey) {
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

    // Create router
    RouterAgent router =
        RouterAgent.builder()
            .model(DEFAULT_MODEL)
            .responder(responder)
            .addRoute(techSupport, "technical issues, bugs, errors, crashes, not working")
            .addRoute(salesAgent, "pricing, purchase, buy, upgrade, subscription")
            .fallback(generalAgent)
            .build();

    // Test routing
    String techQuery = "My application keeps crashing when I try to save files.";
    System.out.println("📤 Query: '" + techQuery + "'");

    // Classify without executing
    var classified = router.classify(techQuery);
    System.out.println(
        "📍 Would route to: " + classified.map(Interactable::name).orElse("fallback"));

    // Route and execute
    AgentResult result = router.route(techQuery);
    System.out.println("📥 Response: " + result.output());
  }

  /** Example 16: Agent with Memory. Demonstrates using InMemoryMemory for conversation context. */
  private static void agentWithMemoryExample(String apiKey) {
    System.out.println("\n🤖 Example 16: Agent with Memory");
    System.out.println("─".repeat(40));

    // Create an in-memory store
    InMemoryMemory memory = InMemoryMemory.create();
    String userId = "user-123"; // All memory operations are user-scoped

    // Add some memories for this user
    memory.add(userId, MemoryEntry.of("User prefers responses in Spanish."));
    memory.add(userId, MemoryEntry.of("User's name is Carlos."));
    memory.add(userId, MemoryEntry.of("User is interested in machine learning."));

    System.out.println("📝 Stored memories for user '" + userId + "':");
    memory.all(userId).forEach(entry -> System.out.println("   • " + entry.content()));

    // Retrieve relevant memories based on query
    System.out.println("\n🔍 Querying for 'name':");
    List<MemoryEntry> relevant = memory.retrieve(userId, "name", 2);
    relevant.forEach(entry -> System.out.println("   • " + entry.content()));

    // You can use these memories to enrich agent context
    System.out.println("\n💡 These memories can be injected into agent instructions or context.");

    // Demonstrate memory operations
    System.out.println("\n🗑️ Clearing all memories for user...");
    memory.clear(userId);
    System.out.println("   Memory size after clear: " + memory.size(userId));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SUPPORTING RECORDS AND CLASSES
  // ═══════════════════════════════════════════════════════════════════════════

  private static void printResponse(String exampleName, Response response) {
    System.out.println("\n" + "═".repeat(60));
    System.out.println("📋 " + exampleName + " - Response");
    System.out.println("═".repeat(60));
    try {
      System.out.println(
          ResponsesApiObjectMapper.create()
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(response));
    } catch (JsonProcessingException e) {
      System.out.println("Error serializing response: " + e.getMessage());
    }
  }

  /** Temperature unit enumeration. */
  public enum TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
  }

  /** Structured output for the streaming example. */
  public record StreamedPerson(String name, int age, String occupation, String bio) {}

  /** Example structured output record for weather information. */
  public record WeatherInfo(
      @NonNull String location, @NonNull String description, int temperatureCelsius) {}

  /** Parameters for the get_weather function tool. */
  public record GetWeatherParams(@NonNull String location, @NonNull TemperatureUnit unit) {}

  /**
   * Example function tool that retrieves weather information. In a real application, this would
   * call an actual weather API.
   */
  @FunctionMetadata(
      name = "get_weather",
      description = "Gets the current weather in a given location.")
  public static class GetWeatherTool extends FunctionTool<GetWeatherParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable GetWeatherParams params) {
      if (params == null) {
        return FunctionToolCallOutput.error("No parameters provided");
      }

      String unitSymbol = params.unit() == TemperatureUnit.CELSIUS ? "°C" : "°F";
      int temperature = params.unit() == TemperatureUnit.CELSIUS ? 25 : 77;

      return FunctionToolCallOutput.success(
          String.format(
              "The weather in %s is %d%s and sunny.", params.location(), temperature, unitSymbol));
    }
  }
}
