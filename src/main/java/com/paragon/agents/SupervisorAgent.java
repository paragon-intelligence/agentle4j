package com.paragon.agents;

import com.paragon.prompts.Prompt;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Implements the Supervisor pattern: a central agent that coordinates multiple worker agents.
 *
 * <p>Unlike {@link ParallelAgents} which runs agents concurrently without coordination, or {@link
 * Handoff} which transfers control permanently, SupervisorAgent maintains central control and can
 * delegate tasks to workers, receive their outputs, and make decisions about next steps.
 *
 * <p>The supervisor uses its instructions to:
 *
 * <ul>
 *   <li>Decompose complex tasks into subtasks
 *   <li>Delegate subtasks to appropriate workers (as tools)
 *   <li>Aggregate and synthesize worker outputs
 *   <li>Make decisions about task completion
 * </ul>
 *
 * <p><b>Virtual Thread Design:</b> Uses synchronous API optimized for Java 21+ virtual threads.
 * Blocking calls are cheap and efficient with virtual threads.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Agent researcher = Agent.builder().name("Researcher")...build();
 * Agent writer = Agent.builder().name("Writer")...build();
 *
 * SupervisorAgent supervisor = SupervisorAgent.builder()
 *     .model("openai/gpt-4o")
 *     .responder(responder)
 *     .instructions("Coordinate workers to research and write reports")
 *     .addWorker(researcher, "research and gather facts")
 *     .addWorker(writer, "write and format content")
 *     .build();
 *
 * // Supervisor orchestrates workers to complete the task
 * AgentResult result = supervisor.orchestrate("Write a report on AI trends");
 * }</pre>
 *
 * @see ParallelAgents
 * @see Handoff
 * @see SubAgentTool
 * @since 1.0
 */
public final class SupervisorAgent implements Interactable {

  private final @NonNull String name;
  private final @NonNull String model;
  private final @NonNull String instructions;
  private final @NonNull Responder responder;
  private final @NonNull List<Worker> workers;
  private final int maxTurns;
  private final @NonNull Agent supervisorAgent;

  private SupervisorAgent(Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "name cannot be null");
    this.model = Objects.requireNonNull(builder.model, "model cannot be null");
    this.instructions = Objects.requireNonNull(builder.instructions, "instructions cannot be null");
    this.responder = Objects.requireNonNull(builder.responder, "responder cannot be null");
    this.workers = List.copyOf(builder.workers);
    this.maxTurns = builder.maxTurns;

    if (workers.isEmpty()) {
      throw new IllegalArgumentException("At least one worker is required");
    }

    // Build internal supervisor agent with workers as sub-agents
    Agent.Builder agentBuilder =
        Agent.builder()
            .name(name)
            .model(model)
            .instructions(buildSupervisorInstructions())
            .responder(responder)
            .maxTurns(maxTurns);

    // Add workers as sub-agents (tools)
    for (Worker worker : workers) {
      agentBuilder.addSubAgent(
          worker.agent(),
          SubAgentTool.Config.builder()
              .description(worker.description())
              .shareState(true)
              .shareHistory(false)
              .build());
    }

    this.supervisorAgent = agentBuilder.build();
  }

  /** Creates a new SupervisorAgent builder. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Returns the supervisor's name.
   *
   * @return the name
   */
  public @NonNull String name() {
    return name;
  }

  /**
   * Returns the workers managed by this supervisor.
   *
   * @return unmodifiable list of workers
   */
  public @NonNull List<Worker> workers() {
    return workers;
  }

  /**
   * Orchestrates workers to complete a task.
   *
   * <p>The supervisor receives the task and uses its instructions to delegate subtasks to workers
   * as needed, aggregating their outputs into a final result.
   *
   * @param input the task description
   * @return the orchestrated result
   */
  public @NonNull AgentResult orchestrate(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    return supervisorAgent.interact(input);
  }

  /**
   * Orchestrates workers with Text content.
   *
   * @param text the task text
   * @return the orchestrated result
   */
  public @NonNull AgentResult orchestrate(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    return supervisorAgent.interact(text);
  }

  /**
   * Orchestrates workers with a Message.
   *
   * @param message the task message
   * @return the orchestrated result
   */
  public @NonNull AgentResult orchestrate(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    return supervisorAgent.interact(message);
  }

  /**
   * Orchestrates workers with a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the task prompt
   * @return the orchestrated result
   */
  public @NonNull AgentResult orchestrate(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return orchestrate(prompt.text());
  }

  /**
   * Orchestrates workers using an existing context.
   *
   * @param context the context with task history
   * @return the orchestrated result
   */
  public @NonNull AgentResult orchestrate(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Ensure trace correlation
    if (!context.hasTraceContext()) {
      context.withTraceContext(
          TraceIdGenerator.generateTraceId(), TraceIdGenerator.generateSpanId());
    }

    return supervisorAgent.interact(context);
  }

  /**
   * Orchestrates workers with streaming support.
   *
   * @param input the task description
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream orchestrateStream(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    return supervisorAgent.interactStream(input);
  }

  /**
   * Orchestrates workers with streaming using a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the task prompt
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream orchestrateStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return orchestrateStream(prompt.text());
  }

  /**
   * Orchestrates workers with streaming using an existing context.
   *
   * @param context the context with task history
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream orchestrateStream(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return supervisorAgent.interactStream(context);
  }

  /** Returns the underlying supervisor agent for advanced usage. */
  @NonNull
  Agent underlyingAgent() {
    return supervisorAgent;
  }

  // ===== Interactable Interface Implementation =====

  /** {@inheritDoc} Delegates to {@link #orchestrate(String)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull String input) {
    return orchestrate(input);
  }

  /** {@inheritDoc} Delegates to {@link #orchestrate(Text)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull Text text) {
    return orchestrate(text);
  }

  /** {@inheritDoc} Delegates to {@link #orchestrate(Message)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull Message message) {
    return orchestrate(message);
  }

  /** {@inheritDoc} Delegates to {@link #orchestrate(Prompt)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull Prompt prompt) {
    return orchestrate(prompt);
  }

  /** {@inheritDoc} Delegates to {@link #orchestrate(AgentContext)}. */
  @Override
  public @NonNull AgentResult interact(@NonNull AgentContext context) {
    return orchestrate(context);
  }

  /** {@inheritDoc} Delegates to {@link #orchestrateStream(String)}. */
  @Override
  public @NonNull AgentStream interactStream(@NonNull String input) {
    return orchestrateStream(input);
  }

  /** {@inheritDoc} Delegates to {@link #orchestrateStream(Prompt)}. */
  @Override
  public @NonNull AgentStream interactStream(@NonNull Prompt prompt) {
    return orchestrateStream(prompt);
  }

  /** {@inheritDoc} Delegates to {@link #orchestrateStream(AgentContext)}. */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgentContext context) {
    return orchestrateStream(context);
  }

  private String buildSupervisorInstructions() {
    StringBuilder sb = new StringBuilder();
    sb.append(instructions).append("\n\n");
    sb.append("You are a supervisor agent with the following workers available:\n\n");

    for (Worker worker : workers) {
      sb.append("- **").append(worker.agent().name()).append("**: ");
      sb.append(worker.description()).append("\n");
    }

    sb.append("\nTo complete tasks:\n");
    sb.append("1. Analyze the task and break it into subtasks\n");
    sb.append("2. Delegate subtasks to appropriate workers using their tools\n");
    sb.append("3. Wait for worker outputs and synthesize them\n");
    sb.append("4. Provide the final coordinated response\n");

    return sb.toString();
  }

  /** Represents a worker agent with its description. */
  public record Worker(@NonNull Agent agent, @NonNull String description) {
    public Worker {
      Objects.requireNonNull(agent, "agent cannot be null");
      Objects.requireNonNull(description, "description cannot be null");
    }
  }

  /** Builder for SupervisorAgent. */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable String model;
    private @Nullable String instructions;
    private @Nullable Responder responder;
    private final List<Worker> workers = new ArrayList<>();
    private int maxTurns = 10;

    private Builder() {}

    /**
     * Sets the supervisor name.
     *
     * @param name the name
     * @return this builder
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    /**
     * Sets the model for the supervisor agent.
     *
     * @param model the model identifier
     * @return this builder
     */
    public @NonNull Builder model(@NonNull String model) {
      this.model = Objects.requireNonNull(model);
      return this;
    }

    /**
     * Sets the supervisor's instructions for coordinating workers.
     *
     * @param instructions the instructions
     * @return this builder
     */
    public @NonNull Builder instructions(@NonNull String instructions) {
      this.instructions = Objects.requireNonNull(instructions);
      return this;
    }

    /**
     * Sets the responder for API calls.
     *
     * @param responder the responder
     * @return this builder
     */
    public @NonNull Builder responder(@NonNull Responder responder) {
      this.responder = Objects.requireNonNull(responder);
      return this;
    }

    /**
     * Adds a worker agent with a description of its capabilities.
     *
     * @param worker the worker agent
     * @param description when to use this worker
     * @return this builder
     */
    public @NonNull Builder addWorker(@NonNull Agent worker, @NonNull String description) {
      Objects.requireNonNull(worker, "worker cannot be null");
      Objects.requireNonNull(description, "description cannot be null");
      this.workers.add(new Worker(worker, description));
      return this;
    }

    /**
     * Sets the maximum number of turns for the supervisor.
     *
     * @param maxTurns the max turns
     * @return this builder
     */
    public @NonNull Builder maxTurns(int maxTurns) {
      if (maxTurns < 1) {
        throw new IllegalArgumentException("maxTurns must be at least 1");
      }
      this.maxTurns = maxTurns;
      return this;
    }

    /**
     * Builds the SupervisorAgent.
     *
     * @return the configured supervisor
     */
    public @NonNull SupervisorAgent build() {
      if (name == null) {
        name = "Supervisor";
      }
      return new SupervisorAgent(this);
    }
  }
}
