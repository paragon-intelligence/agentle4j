package com.paragon.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.prompts.Prompt;
import com.paragon.responses.Responder;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

  private final @NonNull String name;
  private final @NonNull String model;
  private final @NonNull List<Route> routes;
  private final @NonNull Responder responder;
  private final @Nullable Interactable fallback;
  private final @Nullable TraceMetadata traceMetadata;

  private RouterAgent(Builder builder) {
    if (builder.routes.isEmpty()) {
      throw new IllegalArgumentException("At least one route is required");
    }
    this.name = builder.name != null ? builder.name : "RouterAgent";
    this.model = Objects.requireNonNull(builder.model, "model is required");
    this.responder = Objects.requireNonNull(builder.responder, "responder is required");
    this.routes = List.copyOf(builder.routes);
    this.fallback = builder.fallback;
    this.traceMetadata = builder.traceMetadata;
  }

  /**
   * Creates a new RouterAgent builder.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return name;
  }

  /**
   * Routes the input to the most appropriate agent and executes it.
   *
   * @param input the user input to route
   * @return the result from the selected agent
   */
  /**
   * Classifies the input and returns the selected route target without executing.
   *
   * <p>Useful when you need to know which target would handle the input before committing.
   *
   * @param input the user input to classify
   * @return Optional containing the selected Interactable, or empty if no match and no fallback
   */
  public @NonNull Optional<Interactable> classify(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    return classifyInternal(input);
  }

  /**
   * Classifies the Prompt and returns the selected route target without executing.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt to classify
   * @return Optional containing the selected Interactable, or empty if no match and no fallback
   */
  public @NonNull Optional<Interactable> classify(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return classifyInternal(prompt.text());
  }

  private @NonNull Optional<Interactable> classifyInternal(String input) {
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
              .append(route.target.name())
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
        return Optional.of(routes.get(selectedIndex).target);
      }
    } catch (Exception e) {
      // Fall through to fallback
    }
    return Optional.ofNullable(fallback);
  }

  /**
   * Returns the routes configured for this router.
   *
   * @return unmodifiable list of routes
   */
  public @NonNull List<Route> routes() {
    return routes;
  }

  /**
   * Creates a RouterStream for streaming route execution with callback support.
   *
   * @param context the conversation context
   * @return a RouterStream for processing streaming events
   */
  public @NonNull RouterStream routeStream(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new RouterStream(this, context, responder);
  }

  // ===== Package-private accessors for RouterStream =====

  @NonNull
  List<Route> getRoutes() {
    return routes;
  }

  Optional<Interactable> getFallback() {
    return Optional.ofNullable(fallback);
  }

  @NonNull
  String getModel() {
    return model;
  }

  @NonNull
  Responder getResponder() {
    return responder;
  }

  // ===== Interactable Interface Implementation =====

  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context) {
    return interact(context, null);
  }

  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    Objects.requireNonNull(context, "context cannot be null");

    Optional<String> inputTextOpt = context.extractLastUserMessageText();
    if (inputTextOpt.isEmpty() || inputTextOpt.get().isBlank()) {
      return AgentResult.error(
              new IllegalStateException("No user message found in context for routing"), context, 0);
    }

    Optional<Interactable> selectedOpt = classify(inputTextOpt.get());
    if (selectedOpt.isEmpty()) {
      return AgentResult.error(
              new IllegalStateException("No suitable route found for input"), context, 0);
    }

    return selectedOpt.get().interact(context, trace);
  }

  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context) {
    return interactStream(context, null);
  }

  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    Objects.requireNonNull(context, "context cannot be null");

    Optional<String> inputTextOpt = context.extractLastUserMessageText();
    if (inputTextOpt.isEmpty() || inputTextOpt.get().isBlank()) {
      return AgentStream.failed(
              AgentResult.error(
                      new IllegalStateException("No user message found in context for routing"),
                      context, 0));
    }

    Optional<Interactable> selected = classify(inputTextOpt.get());
    if (selected.isEmpty()) {
      return AgentStream.failed(
              AgentResult.error(
                      new IllegalStateException("No suitable route found for input"), context, 0));
    }

    return selected.get().interactStream(context, trace);
  }


  // ===== Inner Classes =====

  /**
   * Represents a route to a target.
   */
  public record Route(@NonNull Interactable target, @NonNull String description) {
    public Route {
      Objects.requireNonNull(target, "target cannot be null");
      Objects.requireNonNull(description, "description cannot be null");
    }
  }

  /**
   * Builder for RouterAgent.
   */
  public static final class Builder {
    private final List<Route> routes = new ArrayList<>();
    private @Nullable String name;
    private String model;
    private Responder responder;
    private Interactable fallback;
    private TraceMetadata traceMetadata;

    private Builder() {
    }

    /**
     * Sets the name for this router.
     *
     * @param name the router name
     * @return this builder
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = Objects.requireNonNull(name, "name cannot be null");
      return this;
    }

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
     * Adds a route to a target.
     *
     * <p>The target can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.
     *
     * @param target      the target Interactable
     * @param description keywords/phrases this target handles (e.g., "billing, invoices, payments")
     * @return this builder
     */
    public @NonNull Builder addRoute(@NonNull Interactable target, @NonNull String description) {
      routes.add(new Route(target, description));
      return this;
    }

    /**
     * Sets the fallback when no route matches.
     *
     * <p>The fallback can be any Interactable.
     *
     * @param fallback the fallback Interactable
     * @return this builder
     */
    public @NonNull Builder fallback(@NonNull Interactable fallback) {
      this.fallback = fallback;
      return this;
    }

    /**
     * Sets the trace metadata for API requests (optional).
     *
     * @param trace the trace metadata
     * @return this builder
     */
    public @NonNull Builder traceMetadata(@Nullable TraceMetadata trace) {
      this.traceMetadata = trace;
      return this;
    }

    /**
     * Configures this router to produce structured output of the specified type.
     *
     * <p>Returns a {@link StructuredBuilder} that builds a {@link RouterAgent.Structured}
     * instead of a regular RouterAgent.
     *
     * <p>All routed agents are expected to produce output parseable as the specified type.
     *
     * <p>Example:
     *
     * <pre>{@code
     * var router = RouterAgent.builder()
     *     .model("openai/gpt-4o")
     *     .responder(responder)
     *     .addRoute(billingAgent, "billing questions")
     *     .addRoute(techAgent, "technical issues")
     *     .structured(TicketResponse.class)
     *     .build();
     *
     * StructuredAgentResult<TicketResponse> result = router.interactStructured("Invoice issue");
     * TicketResponse ticket = result.output();
     * }</pre>
     *
     * @param <T>        the output type
     * @param outputType the class of the structured output
     * @return a structured builder
     */
    public <T> @NonNull StructuredBuilder<T> structured(@NonNull Class<T> outputType) {
      return new StructuredBuilder<>(this, outputType);
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

  /**
   * Builder for creating type-safe structured output router agents.
   *
   * <p>Returned from {@code RouterAgent.builder().structured(Class)}.
   *
   * @param <T> the output type
   */
  public static final class StructuredBuilder<T> {
    private final Builder parentBuilder;
    private final Class<T> outputType;
    private @Nullable ObjectMapper objectMapper;

    private StructuredBuilder(@NonNull Builder parentBuilder, @NonNull Class<T> outputType) {
      this.parentBuilder = Objects.requireNonNull(parentBuilder);
      this.outputType = Objects.requireNonNull(outputType);
    }

    public @NonNull StructuredBuilder<T> name(@NonNull String name) {
      parentBuilder.name(name);
      return this;
    }

    public @NonNull StructuredBuilder<T> model(@NonNull String model) {
      parentBuilder.model(model);
      return this;
    }

    public @NonNull StructuredBuilder<T> responder(@NonNull Responder responder) {
      parentBuilder.responder(responder);
      return this;
    }

    public @NonNull StructuredBuilder<T> addRoute(@NonNull Interactable target, @NonNull String description) {
      parentBuilder.addRoute(target, description);
      return this;
    }

    public @NonNull StructuredBuilder<T> fallback(@NonNull Interactable fallback) {
      parentBuilder.fallback(fallback);
      return this;
    }

    public @NonNull StructuredBuilder<T> traceMetadata(@Nullable TraceMetadata trace) {
      parentBuilder.traceMetadata(trace);
      return this;
    }

    public @NonNull StructuredBuilder<T> objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /**
     * Builds the type-safe structured router agent.
     *
     * @return the configured Structured router
     */
    public RouterAgent.Structured<T> build() {
      RouterAgent router = parentBuilder.build();
      ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
      return new RouterAgent.Structured<>(router, outputType, mapper);
    }
  }

  /**
   * Type-safe wrapper for router agents with structured output.
   *
   * <p>Delegates all interaction to the wrapped RouterAgent and parses the routed agent's
   * output as the specified type.
   *
   * @param <T> the output type
   */
  public static final class Structured<T> implements Interactable.Structured<T> {
    private final RouterAgent router;
    private final Class<T> outputType;
    private final ObjectMapper objectMapper;

    private Structured(@NonNull RouterAgent router, @NonNull Class<T> outputType, @NonNull ObjectMapper objectMapper) {
      this.router = Objects.requireNonNull(router);
      this.outputType = Objects.requireNonNull(outputType);
      this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public @NonNull String name() {
      return router.name();
    }

    @Override
    public @NonNull AgentResult interact(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      return router.interact(context, trace);
    }

    @Override
    public @NonNull AgentStream interactStream(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      return router.interactStream(context, trace);
    }

    @Override
    public @NonNull StructuredAgentResult<T> interactStructured(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      AgentResult result = router.interact(context, trace);
      return result.toStructured(outputType, objectMapper);
    }

    /**
     * Returns the structured output type.
     */
    public @NonNull Class<T> outputType() {
      return outputType;
    }

    /**
     * Classifies the input and returns the selected route target without executing.
     *
     * <p>Delegates to the underlying RouterAgent.
     *
     * @param input the user input to classify
     * @return Optional containing the selected Interactable, or empty if no match
     */
    public @NonNull Optional<Interactable> classify(@NonNull String input) {
      return router.classify(input);
    }
  }
}
