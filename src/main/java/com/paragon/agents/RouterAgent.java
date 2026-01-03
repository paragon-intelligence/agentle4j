package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A specialized agent for routing inputs to appropriate target agents.
 *
 * <p>Unlike general agents with complex instructions, RouterAgent focuses purely on classification
 * and routing, avoiding the noise of full agent instructions.
 *
 * <p><b>All methods are async by default</b> - they return {@link CompletableFuture}. For blocking
 * calls, use {@code .join()}.
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
 * // Route and execute (async)
 * router.route("I have a question about my invoice")
 *     .thenAccept(result -> System.out.println("Handled by: " + result.handoffTarget().name()));
 *
 * // Or just classify without executing
 * router.classify("My app keeps crashing")
 *     .thenAccept(agent -> System.out.println("Would route to: " + agent.name()));
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
public final class RouterAgent {

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

  // ===== Route Methods (All Async) =====

  /**
   * Routes the input to the most appropriate agent and executes it.
   *
   * @param input the user input to route
   * @return future completing with the result from the selected agent
   */
  public @NonNull CompletableFuture<AgentResult> route(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return route(context);
  }

  /**
   * Routes the Text content to the most appropriate agent and executes it.
   *
   * @param text the text content to route
   * @return future completing with the result from the selected agent
   */
  public @NonNull CompletableFuture<AgentResult> route(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return route(context);
  }

  /**
   * Routes the Message to the most appropriate agent and executes it.
   *
   * @param message the message to route
   * @return future completing with the result from the selected agent
   */
  public @NonNull CompletableFuture<AgentResult> route(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return route(context);
  }

  /**
   * Routes using an existing context. Extracts the last user message for classification.
   *
   * <p>This is the core routing method. All other route overloads delegate here.
   *
   * @param context the conversation context
   * @return future completing with the result from the selected agent
   */
  public @NonNull CompletableFuture<AgentResult> route(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Extract the last user message text for classification
    String inputText = extractLastUserMessage(context);
    if (inputText == null || inputText.isBlank()) {
      return CompletableFuture.completedFuture(
          AgentResult.error(
              new IllegalStateException("No user message found in context for routing"),
              context,
              0));
    }

    return classify(inputText)
        .thenCompose(
            selected -> {
              if (selected == null) {
                return CompletableFuture.completedFuture(
                    AgentResult.error(
                        new IllegalStateException("No suitable agent found for input"),
                        context,
                        0));
              }

              return selected
                  .interact(context)
                  .thenApply(result -> AgentResult.handoff(selected, result, context));
            });
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
   * @return future completing with the selected agent, or fallback/null if no match
   */
  public @NonNull CompletableFuture<Agent> classify(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");

    // Build routing prompt
    StringBuilder prompt = new StringBuilder();
    prompt.append(
        "You are a routing classifier. Based on the user input, select the most appropriate"
            + " handler.\n\n");
    prompt.append("Available handlers:\n");
    for (int i = 0; i < routes.size(); i++) {
      Route route = routes.get(i);
      prompt
          .append(i + 1)
          .append(". ")
          .append(route.agent.name())
          .append(" - handles: ")
          .append(route.description)
          .append("\n");
    }
    prompt.append("\nUser input: \"").append(input).append("\"\n\n");
    prompt.append("Respond with ONLY the handler number (e.g., \"1\" or \"2\"). Nothing else.");

    // Call LLM for classification
    CreateResponsePayload payload =
        CreateResponsePayload.builder().model(model).addUserMessage(prompt.toString()).build();

    return responder
        .respond(payload)
        .thenApply(
            response -> {
              try {
                String output = response.outputText().trim();
                int selectedIndex = Integer.parseInt(output) - 1;
                if (selectedIndex >= 0 && selectedIndex < routes.size()) {
                  return routes.get(selectedIndex).agent;
                }
              } catch (Exception e) {
                // Fall through to fallback
              }
              return fallbackAgent;
            });
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

  /**
   * Extracts the last user message text from context.
   */
  private String extractLastUserMessage(AgentContext context) {
    List<ResponseInputItem> history = context.getHistory();
    for (int i = history.size() - 1; i >= 0; i--) {
      ResponseInputItem item = history.get(i);
      if (item instanceof Message msg && "user".equals(msg.role())) {
        // Extract text from message content
        if (msg.content() != null) {
          for (var content : msg.content()) {
            if (content instanceof Text text) {
              return text.text();
            }
          }
        }
      }
    }
    return null;
  }

  // ===== Package-private accessors for RouterStream =====

  @NonNull List<Route> getRoutes() {
    return routes;
  }

  @Nullable Agent getFallbackAgent() {
    return fallbackAgent;
  }

  @NonNull String getModel() {
    return model;
  }

  @NonNull Responder getResponder() {
    return responder;
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
