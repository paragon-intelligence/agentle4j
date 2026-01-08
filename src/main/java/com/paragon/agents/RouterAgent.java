package com.paragon.agents;

import com.paragon.prompts.Prompt;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A specialized agent for routing inputs to appropriate target agents.
 *
 * <p>Unlike general agents with complex instructions, RouterAgent focuses purely on classification
 * and routing, avoiding the noise of full agent instructions.
 *
 * <p><b>Virtual Thread Design:</b> Uses synchronous API optimized for Java 21+ virtual threads.
 * Blocking calls are cheap and efficient with virtual threads.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Agent billingAgent = Agent.builder().name("Billing")...build();
 * Agent techSupport = Agent.builder().name("TechSupport")...build();
 * Agent salesAgent = Agent.builder().name("Sales")...build();
 *
 * RouterAgent router = RouterAgent.builder()
 *     .model("openai/gpt-4o")
 *     .responder(responder)
 *     .addRoute(billingAgent, "billing, invoices, payments, charges")
 *     .addRoute(techSupport, "technical issues, bugs, errors, not working")
 *     .addRoute(salesAgent, "pricing, new features, demos, upgrades")
 *     .build();
 *
 * // Route and execute - blocking, uses virtual threads
 * AgentResult result = router.route("I have a question about my invoice");
 * System.out.println("Handled by: " + result.handoffTarget().name());
 *
 * // Or just classify without executing
 * Optional<Agent> agent = router.classify("My app keeps crashing");
 * System.out.println("Would route to: " + agent.map(Agent::name).orElse("none"));
 *
 * // Streaming support
 * router.routeStream("Help me with billing")
 *     .onTextDelta(System.out::print)
 *     .onComplete(result -> System.out.println("\nDone!"))
 *     .start();
 * }</pre>
 *
 * @since 1.0
 */
public final class RouterAgent implements Interactable {

  private final @NonNull String model;
  private final @NonNull List<Route> routes;
  private final @NonNull Responder responder;
  private final @Nullable Agent fallbackAgent;

  private RouterAgent(Builder builder) {
    if (builder.routes.isEmpty()) {
      throw new IllegalArgumentException("At least one route is required");
    }
    this.model = Objects.requireNonNull(builder.model, "model is required");
    this.responder = Objects.requireNonNull(builder.responder, "responder is required");
    this.routes = List.copyOf(builder.routes);
    this.fallbackAgent = builder.fallbackAgent;
  }

  /**
   * Creates a new RouterAgent builder.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  // ===== Route Methods =====

  /**
   * Routes the input to the most appropriate agent and executes it.
   *
   * @param input the user input to route
   * @return the result from the selected agent
   */
  public @NonNull AgentResult route(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return route(context);
  }

  /**
   * Routes the Text content to the most appropriate agent and executes it.
   *
   * @param text the text content to route
   * @return the result from the selected agent
   */
  public @NonNull AgentResult route(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return route(context);
  }

  /**
   * Routes the Message to the most appropriate agent and executes it.
   *
   * @param message the message to route
   * @return the result from the selected agent
   */
  public @NonNull AgentResult route(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return route(context);
  }

  /**
   * Routes the Prompt to the most appropriate agent and executes it.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt to route
   * @return the result from the selected agent
   */
  public @NonNull AgentResult route(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return route(prompt.text());
  }

  /**
   * Routes using an existing context. Extracts the last user message for classification.
   *
   * <p>This is the core routing method. All other route overloads delegate here.
   *
   * @param context the conversation context
   * @return the result from the selected agent
   */
  public @NonNull AgentResult route(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Extract the last user message text for classification
    Optional<String> inputTextOpt = extractLastUserMessage(context);
    if (inputTextOpt.isEmpty() || inputTextOpt.get().isBlank()) {
      return AgentResult.error(
          new IllegalStateException("No user message found in context for routing"), context, 0);
    }

    String inputText = inputTextOpt.get();
    Optional<Agent> selectedOpt = classify(inputText);

    if (selectedOpt.isEmpty()) {
      return AgentResult.error(
          new IllegalStateException("No suitable agent found for input"), context, 0);
    }

    Agent selected = selectedOpt.get();
    AgentResult result = selected.interact(context);
    return AgentResult.handoff(selected, result, context);
  }

  // ===== Streaming Route Methods =====

  /**
   * Routes the input to the most appropriate agent and executes it with streaming.
   *
   * @param input the user input to route
   * @return a RouterStream for processing streaming events
   */
  public @NonNull RouterStream routeStream(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return routeStream(context);
  }

  /**
   * Routes the Prompt to the most appropriate agent and executes it with streaming.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt to route
   * @return a RouterStream for processing streaming events
   */
  public @NonNull RouterStream routeStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return routeStream(prompt.text());
  }

  /**
   * Routes using an existing context with streaming. Extracts the last user message for
   * classification.
   *
   * @param context the conversation context
   * @return a RouterStream for processing streaming events
   */
  public @NonNull RouterStream routeStream(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new RouterStream(this, context, responder);
  }

  /**
   * Classifies the input and returns the selected agent without executing.
   *
   * <p>Useful when you need to know which agent would handle the input before committing.
   *
   * @param input the user input to classify
   * @return Optional containing the selected agent, or empty if no match and no fallback
   */
  public @NonNull Optional<Agent> classify(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    return classifyInternal(input);
  }

  /**
   * Classifies the Prompt and returns the selected agent without executing.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt to classify
   * @return Optional containing the selected agent, or empty if no match and no fallback
   */
  public @NonNull Optional<Agent> classify(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return classifyInternal(prompt.text());
  }

  private @NonNull Optional<Agent> classifyInternal(String input) {
    // Build routing prompt
    StringBuilder routingPrompt = new StringBuilder();
    routingPrompt.append(
        "You are a routing classifier. Based on the user input, select the most appropriate"
            + " handler.\n\n");
    routingPrompt.append("Available handlers:\n");
    for (int i = 0; i < routes.size(); i++) {
      Route route = routes.get(i);
      routingPrompt
          .append(i + 1)
          .append(". ")
          .append(route.agent.name())
          .append(" - handles: ")
          .append(route.description)
          .append("\n");
    }
    routingPrompt.append("\nUser input: \"").append(input).append("\"\n\n");
    routingPrompt.append("Respond with ONLY the handler number (e.g., \"1\" or \"2\"). Nothing else.");

    // Call LLM for classification
    CreateResponsePayload payload =
        CreateResponsePayload.builder().model(model).addUserMessage(routingPrompt.toString()).build();

    Response response = responder.respond(payload);

    try {
      String output = response.outputText().trim();
      int selectedIndex = Integer.parseInt(output) - 1;
      if (selectedIndex >= 0 && selectedIndex < routes.size()) {
        return Optional.of(routes.get(selectedIndex).agent);
      }
    } catch (Exception e) {
      // Fall through to fallback
    }
    return Optional.ofNullable(fallbackAgent);
  }

  /**
   * Returns the routes configured for this router.
   *
   * @return unmodifiable list of routes
   */
  public @NonNull List<Route> routes() {
    return routes;
  }

  // ===== Helper Methods =====

  /** Extracts the last user message text from context. */
  private Optional<String> extractLastUserMessage(AgentContext context) {
    List<ResponseInputItem> history = context.getHistory();
    for (int i = history.size() - 1; i >= 0; i--) {
      ResponseInputItem item = history.get(i);
      if (item instanceof Message msg && "user".equals(msg.role())) {
        // Extract text from message content
        if (msg.content() != null) {
          for (var content : msg.content()) {
            if (content instanceof Text text) {
              return Optional.of(text.text());
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  // ===== Package-private accessors for RouterStream =====

  @NonNull List<Route> getRoutes() {
    return routes;
  }

  Optional<Agent> getFallbackAgent() {
    return Optional.ofNullable(fallbackAgent);
  }

  @NonNull String getModel() {
    return model;
  }

  @NonNull Responder getResponder() {
    return responder;
  }

  // ===== Interactable Interface Implementation =====

  /** {@inheritDoc} Delegates to {@link #route(String)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull String input) {
    return route(input);
  }

  /** {@inheritDoc} Delegates to {@link #route(Text)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull Text text) {
    return route(text);
  }

  /** {@inheritDoc} Delegates to {@link #route(Message)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull Message message) {
    return route(message);
  }

  /** {@inheritDoc} Delegates to {@link #route(Prompt)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull Prompt prompt) {
    return route(prompt);
  }

  /** {@inheritDoc} Delegates to {@link #route(AgentContext)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull AgentContext context) {
    return route(context);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Classifies the input and returns the selected agent's stream. If classification fails,
   * returns a failed AgentStream.
   */
  @Override
  public @NonNull AgentStream interactStream(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    Optional<Agent> selected = classify(input);
    if (selected.isEmpty()) {
      AgentContext ctx = AgentContext.create();
      return AgentStream.failed(
          AgentResult.error(
              new IllegalStateException("No suitable agent found for input"), ctx, 0));
    }
    return selected.get().interactStream(input);
  }

  /** {@inheritDoc} Classifies the prompt and returns the selected agent's stream. */
  @Override
  public @NonNull AgentStream interactStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return interactStream(prompt.text());
  }

  /** {@inheritDoc} Extracts the last user message for classification and streams from the selected agent. */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    Optional<String> inputTextOpt = extractLastUserMessage(context);
    if (inputTextOpt.isEmpty() || inputTextOpt.get().isBlank()) {
      return AgentStream.failed(
          AgentResult.error(
              new IllegalStateException("No user message found in context for routing"),
              context,
              0));
    }
    Optional<Agent> selected = classify(inputTextOpt.get());
    if (selected.isEmpty()) {
      return AgentStream.failed(
          AgentResult.error(
              new IllegalStateException("No suitable agent found for input"), context, 0));
    }
    return selected.get().interactStream(context);
  }

  // ===== Inner Classes =====

  /** Represents a route to a target agent. */
  public record Route(@NonNull Agent agent, @NonNull String description) {
    public Route {
      Objects.requireNonNull(agent, "agent cannot be null");
      Objects.requireNonNull(description, "description cannot be null");
    }
  }

  /** Builder for RouterAgent. */
  public static final class Builder {
    private String model;
    private Responder responder;
    private final List<Route> routes = new ArrayList<>();
    private Agent fallbackAgent;

    private Builder() {}

    /**
     * Sets the model for classification.
     *
     * @param model the model identifier (e.g., "openai/gpt-4o-mini")
     * @return this builder
     */
    public @NonNull Builder model(@NonNull String model) {
      this.model = model;
      return this;
    }

    /**
     * Sets the responder for LLM calls.
     *
     * @param responder the responder
     * @return this builder
     */
    public @NonNull Builder responder(@NonNull Responder responder) {
      this.responder = responder;
      return this;
    }

    /**
     * Adds a route to a target agent.
     *
     * @param agent the target agent
     * @param description keywords/phrases this agent handles (e.g., "billing, invoices, payments")
     * @return this builder
     */
    public @NonNull Builder addRoute(@NonNull Agent agent, @NonNull String description) {
      routes.add(new Route(agent, description));
      return this;
    }

    /**
     * Sets the fallback agent when no route matches.
     *
     * @param fallback the fallback agent
     * @return this builder
     */
    public @NonNull Builder fallback(@NonNull Agent fallback) {
      this.fallbackAgent = fallback;
      return this;
    }

    /**
     * Builds the RouterAgent.
     *
     * @return a new RouterAgent
     */
    public @NonNull RouterAgent build() {
      return new RouterAgent(this);
    }
  }
}
