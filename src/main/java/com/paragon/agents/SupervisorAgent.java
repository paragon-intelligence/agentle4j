package com.paragon.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.TraceMetadata;
import com.paragon.skills.Skill;
import com.paragon.skills.SkillProvider;
import com.paragon.skills.SkillStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
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
  private final @Nullable TraceMetadata traceMetadata;

  // Worker-level streaming callbacks (mutable, set after construction)
  private @Nullable BiConsumer<Worker, ToolExecution> onWorkerInvoked;
  private @Nullable BiConsumer<Worker, ToolExecution> onWorkerComplete;

  private SupervisorAgent(Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "name cannot be null");
    this.model = Objects.requireNonNull(builder.model, "model cannot be null");
    this.instructions = Objects.requireNonNull(builder.instructions, "instructions cannot be null");
    this.responder = Objects.requireNonNull(builder.responder, "responder cannot be null");
    this.workers = List.copyOf(builder.workers);
    this.maxTurns = builder.maxTurns;
    this.traceMetadata = builder.traceMetadata;

    if (workers.isEmpty()) {
      throw new IllegalArgumentException("At least one worker is required");
    }

    // Build internal supervisor agent with workers as tools
    Agent.Builder agentBuilder =
        Agent.builder()
            .name(name)
            .model(model)
            .instructions(buildSupervisorInstructions())
            .responder(responder)
            .maxTurns(maxTurns);

    // Add workers as interactable tools
    for (Worker worker : workers) {
      agentBuilder.addTool(
          new InteractableSubAgentTool(
              worker.worker(),
              InteractableSubAgentTool.Config.builder()
                  .description(worker.description())
                  .shareState(true)
                  .shareHistory(false)
                  .build()));
    }

    // Add skills
    for (Skill skill : builder.pendingSkills) {
      agentBuilder.addSkill(skill);
    }

    if (builder.outputType != null) {
      agentBuilder.outputType(builder.outputType);
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

  @Override
  public @NonNull InteractableBlueprint toBlueprint() {
    List<InteractableBlueprint.WorkerBlueprint> workerBlueprints =
        workers.stream()
            .map(
                w ->
                    new InteractableBlueprint.WorkerBlueprint(
                        w.worker().toBlueprint(), w.description()))
            .toList();
    return new InteractableBlueprint.SupervisorAgentBlueprint(
        name,
        model,
        instructions,
        maxTurns,
        workerBlueprints,
        InteractableBlueprint.ResponderBlueprint.from(responder),
        traceMetadata);
  }

  /**
   * Returns the workers managed by this supervisor.
   *
   * @return unmodifiable list of workers
   */
  public @NonNull List<Worker> workers() {
    return workers;
  }

  /** Returns the underlying supervisor agent for advanced usage. */
  @NonNull Agent underlyingAgent() {
    return supervisorAgent;
  }

  // ===== Interactable Interface Implementation =====

  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    context.ensureTraceContext();
    return supervisorAgent.interact(context);
  }

  @Override
  public @NonNull AgentResult interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    Objects.requireNonNull(context, "context cannot be null");
    context.ensureTraceContext();
    return supervisorAgent.interact(context, trace);
  }

  /**
   * Returns a streaming view of this supervisor.
   *
   * @return an {@link Interactable.Streaming} that delegates to the underlying agent's streaming
   */
  public Interactable.@NonNull Streaming asStreaming() {
    return (ctx, trace) -> {
      Objects.requireNonNull(ctx, "context cannot be null");
      AgentStream stream = supervisorAgent.asStreaming().interact(ctx, trace);
      return wireWorkerCallbacks(stream);
    };
  }

  /**
   * Registers a callback that fires when a worker is invoked by the supervisor (streaming path).
   *
   * <p>Fires at the same moment as {@code onWorkerComplete} because worker invocation is
   * synchronous in the current model.
   *
   * @param handler receives the worker and the tool execution result
   * @return this supervisor (for chaining)
   */
  public @NonNull SupervisorAgent onWorkerInvoked(
      @NonNull BiConsumer<Worker, ToolExecution> handler) {
    this.onWorkerInvoked = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a callback that fires when a worker completes its execution (streaming path).
   *
   * @param handler receives the worker and the tool execution result
   * @return this supervisor (for chaining)
   */
  public @NonNull SupervisorAgent onWorkerComplete(
      @NonNull BiConsumer<Worker, ToolExecution> handler) {
    this.onWorkerComplete = Objects.requireNonNull(handler);
    return this;
  }

  private @NonNull AgentStream wireWorkerCallbacks(@NonNull AgentStream stream) {
    if (onWorkerInvoked == null && onWorkerComplete == null) {
      return stream;
    }
    Map<String, Worker> workersByToolName = buildWorkerToolNameMap();
    return stream.onToolExecuted(exec -> {
      Worker worker = workersByToolName.get(exec.toolName());
      if (worker != null) {
        if (onWorkerInvoked != null) {
          onWorkerInvoked.accept(worker, exec);
        }
        if (onWorkerComplete != null) {
          onWorkerComplete.accept(worker, exec);
        }
      }
    });
  }

  private @NonNull Map<String, Worker> buildWorkerToolNameMap() {
    Map<String, Worker> map = new HashMap<>();
    for (Worker worker : workers) {
      map.put("invoke_" + toSnakeCase(worker.worker().name()), worker);
    }
    return map;
  }

  private static String toSnakeCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          result.append('_');
        }
        result.append(Character.toLowerCase(c));
      } else if (Character.isWhitespace(c)) {
        result.append('_');
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  private String buildSupervisorInstructions() {
    StringBuilder sb = new StringBuilder();
    sb.append(instructions).append("\n\n");
    sb.append("You are a supervisor agent with the following workers available:\n\n");

    for (Worker worker : workers) {
      sb.append("- **").append(worker.worker().name()).append("**: ");
      sb.append(worker.description()).append("\n");
    }

    sb.append("\nTo complete tasks:\n");
    sb.append("1. Analyze the task and break it into subtasks\n");
    sb.append("2. Delegate subtasks to appropriate workers using their tools\n");
    sb.append("3. Wait for worker outputs and synthesize them\n");
    sb.append("4. Provide the final coordinated response\n");

    return sb.toString();
  }

  /** Represents a worker with its description. */
  public record Worker(@NonNull Interactable worker, @NonNull String description) {
    public Worker {
      Objects.requireNonNull(worker, "worker cannot be null");
      Objects.requireNonNull(description, "description cannot be null");
    }
  }

  /** Builder for SupervisorAgent. */
  public static final class Builder {
    private final List<Worker> workers = new ArrayList<>();
    // Skills to pass to the internal Agent
    private final List<Skill> pendingSkills = new ArrayList<>();
    private @Nullable String name;
    private @Nullable String model;
    private @Nullable String instructions;
    private @Nullable Responder responder;
    private int maxTurns = 10;
    private @Nullable TraceMetadata traceMetadata;
    @Nullable Class<?> outputType;

    private Builder() {}

    @NonNull Builder outputType(@NonNull Class<?> outputType) {
      this.outputType = Objects.requireNonNull(outputType);
      return this;
    }

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
     * Adds a worker with a description of its capabilities.
     *
     * <p>Workers can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.
     *
     * @param worker the worker
     * @param description when to use this worker
     * @return this builder
     */
    public @NonNull Builder addWorker(@NonNull Interactable worker, @NonNull String description) {
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
     * Adds a skill that augments the supervisor's capabilities.
     *
     * @param skill the skill to add
     * @return this builder
     * @see Skill
     */
    public @NonNull Builder addSkill(@NonNull Skill skill) {
      Objects.requireNonNull(skill, "skill cannot be null");
      this.pendingSkills.add(skill);
      return this;
    }

    /**
     * Loads and adds a skill from a provider.
     *
     * @param provider the skill provider
     * @param skillId the skill identifier
     * @return this builder
     */
    public @NonNull Builder addSkillFrom(@NonNull SkillProvider provider, @NonNull String skillId) {
      Objects.requireNonNull(provider, "provider cannot be null");
      Objects.requireNonNull(skillId, "skillId cannot be null");
      return addSkill(provider.provide(skillId));
    }

    /**
     * Registers all skills from a SkillStore.
     *
     * @param store the skill store containing skills to add
     * @return this builder
     */
    public @NonNull Builder skillStore(@NonNull SkillStore store) {
      Objects.requireNonNull(store, "store cannot be null");
      for (Skill skill : store.all()) {
        addSkill(skill);
      }
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
     * Configures this supervisor to produce structured output of the specified type.
     *
     * <p>Returns a {@link StructuredBuilder} that builds a {@link SupervisorAgent.Structured}
     * instead of a regular SupervisorAgent.
     *
     * <p>Example:
     *
     * <pre>{@code
     * var supervisor = SupervisorAgent.builder()
     *     .model("openai/gpt-4o")
     *     .responder(responder)
     *     .instructions("Coordinate workers to produce a report")
     *     .addWorker(researcher, "research facts")
     *     .structured(Report.class)
     *     .build();
     *
     * StructuredAgentResult<Report> result = supervisor.interactStructured("AI trends");
     * Report report = result.output();
     * }</pre>
     *
     * @param <T> the output type
     * @param outputType the class of the structured output
     * @return a structured builder
     */
    public <T> @NonNull StructuredBuilder<T> structured(@NonNull Class<T> outputType) {
      return new StructuredBuilder<>(this, outputType);
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

  /**
   * Builder for creating type-safe structured output supervisor agents.
   *
   * <p>Returned from {@code SupervisorAgent.builder().structured(Class)}.
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

    public @NonNull StructuredBuilder<T> instructions(@NonNull String instructions) {
      parentBuilder.instructions(instructions);
      return this;
    }

    public @NonNull StructuredBuilder<T> responder(@NonNull Responder responder) {
      parentBuilder.responder(responder);
      return this;
    }

    public @NonNull StructuredBuilder<T> addWorker(
        @NonNull Interactable worker, @NonNull String description) {
      parentBuilder.addWorker(worker, description);
      return this;
    }

    public @NonNull StructuredBuilder<T> maxTurns(int maxTurns) {
      parentBuilder.maxTurns(maxTurns);
      return this;
    }

    public @NonNull StructuredBuilder<T> addSkill(@NonNull Skill skill) {
      parentBuilder.addSkill(skill);
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
     * Builds the type-safe structured supervisor agent.
     *
     * @return the configured Structured supervisor
     */
    public SupervisorAgent.Structured<T> build() {
      parentBuilder.outputType(outputType);
      SupervisorAgent supervisor = parentBuilder.build();
      ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
      return new SupervisorAgent.Structured<>(supervisor, outputType, mapper);
    }
  }

  /**
   * Type-safe wrapper for supervisor agents with structured output.
   *
   * <p>Delegates all interaction to the wrapped SupervisorAgent and parses the final output as the
   * specified type.
   *
   * @param <T> the output type
   */
  public static final class Structured<T> implements Interactable.Structured<T> {
    private final SupervisorAgent supervisor;
    private final Class<T> outputType;
    private final ObjectMapper objectMapper;

    Structured(
        @NonNull SupervisorAgent supervisor,
        @NonNull Class<T> outputType,
        @NonNull ObjectMapper objectMapper) {
      this.supervisor = Objects.requireNonNull(supervisor);
      this.outputType = Objects.requireNonNull(outputType);
      this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public @NonNull String name() {
      return supervisor.name();
    }

    @Override
    public @NonNull StructuredAgentResult<T> interact(
        @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      AgentResult result = supervisor.interact(context, trace);
      return result.toStructured(outputType, objectMapper);
    }

    /**
     * Returns a streaming view of the underlying supervisor.
     *
     * @return an {@link Interactable.Streaming} that delegates to the supervisor's streaming
     */
    public Interactable.@NonNull Streaming asStreaming() {
      return supervisor.asStreaming();
    }

    /** Returns the structured output type. */
    public @NonNull Class<T> outputType() {
      return outputType;
    }
  }
}
