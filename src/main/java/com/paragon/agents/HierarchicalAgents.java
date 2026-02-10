package com.paragon.agents;

import com.paragon.prompts.Prompt;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Implements the Hierarchical pattern: multi-layered supervisor structure.
 *
 * <p>Expands upon the supervisor concept to create a multi-layered organizational structure with
 * multiple levels of supervisors. Higher-level supervisors oversee lower-level ones, and
 * ultimately, operational agents at the lowest tier.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li>Tree structure with executive at root
 *   <li>Managers delegate to their team workers
 *   <li>Escalation when workers cannot complete tasks
 *   <li>Distributed decision-making within defined boundaries
 * </ul>
 *
 * <p><b>Virtual Thread Design:</b> Uses synchronous API optimized for Java 21+ virtual threads.
 * Blocking calls are cheap and efficient with virtual threads.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create worker agents
 * Agent devAgent = Agent.builder().name("Developer")...build();
 * Agent qaAgent = Agent.builder().name("QA")...build();
 * Agent salesRep = Agent.builder().name("SalesRep")...build();
 *
 * // Create managers
 * Agent techManager = Agent.builder().name("TechManager")...build();
 * Agent salesManager = Agent.builder().name("SalesManager")...build();
 *
 * // Create executive
 * Agent executive = Agent.builder().name("CEO")...build();
 *
 * HierarchicalAgents hierarchy = HierarchicalAgents.builder()
 *     .executive(executive)
 *     .addDepartment("Engineering", techManager, devAgent, qaAgent)
 *     .addDepartment("Sales", salesManager, salesRep)
 *     .build();
 *
 * // Executive delegates through managers to workers
 * AgentResult result = hierarchy.execute("Launch new product feature");
 * }</pre>
 *
 * @since 1.0
 */
public final class HierarchicalAgents implements Interactable {

  private final @NonNull Agent executive;
  private final @NonNull Map<String, Department> departments;
  private final @NonNull Map<String, SupervisorAgent> departmentSupervisors;
  private final int maxTurns;
  private final @NonNull SupervisorAgent rootSupervisor;
  private final @Nullable TraceMetadata traceMetadata;

  private HierarchicalAgents(Builder builder) {
    this.executive = Objects.requireNonNull(builder.executive, "executive cannot be null");
    this.departments = Map.copyOf(builder.departments);
    this.maxTurns = builder.maxTurns;
    this.departmentSupervisors = new HashMap<>();
    this.traceMetadata = builder.traceMetadata;

    if (departments.isEmpty()) {
      throw new IllegalArgumentException("At least one department is required");
    }

    // Build the hierarchical structure using SupervisorAgents
    this.rootSupervisor = buildHierarchy();
  }

  /**
   * Creates a new HierarchicalAgents builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Returns the executive agent at the top of the hierarchy.
   *
   * @return the executive agent
   */
  public @NonNull Agent executive() {
    return executive;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return executive.name() + "_Hierarchy";
  }

  /**
   * Returns all departments in this hierarchy.
   *
   * @return unmodifiable map of department names to departments
   */
  public @NonNull Map<String, Department> departments() {
    return departments;
  }

  /**
   * Executes a task through the hierarchy.
   *
   * <p>The executive receives the task and delegates through managers to workers as appropriate.
   *
   * @param task the task description
   * @return the execution result
   */
  public @NonNull AgentResult execute(@NonNull String task) {
    Objects.requireNonNull(task, "task cannot be null");
    return rootSupervisor.orchestrate(task);
  }

  /**
   * Executes a task with Text content.
   *
   * @param text the task text
   * @return the execution result
   */
  public @NonNull AgentResult execute(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    return rootSupervisor.orchestrate(text);
  }

  /**
   * Executes a task with a Message.
   *
   * @param message the task message
   * @return the execution result
   */
  public @NonNull AgentResult execute(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    return rootSupervisor.orchestrate(message);
  }

  /**
   * Executes a task with a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the task prompt
   * @return the execution result
   */
  public @NonNull AgentResult execute(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return execute(prompt.text());
  }

  /**
   * Executes a task using an existing context.
   *
   * @param context the context with task history
   * @return the execution result
   */
  public @NonNull AgentResult execute(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Ensure trace correlation
    if (!context.hasTraceContext()) {
      context.withTraceContext(
              TraceIdGenerator.generateTraceId(), TraceIdGenerator.generateSpanId());
    }

    return rootSupervisor.orchestrate(context);
  }

  /**
   * Executes a task with streaming support.
   *
   * @param task the task description
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream executeStream(@NonNull String task) {
    Objects.requireNonNull(task, "task cannot be null");
    return rootSupervisor.orchestrateStream(task);
  }

  /**
   * Executes a task with streaming using a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the task prompt
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream executeStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return executeStream(prompt.text());
  }

  /**
   * Executes a task with streaming using an existing context.
   *
   * @param context the context with task history
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream executeStream(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return rootSupervisor.orchestrateStream(context);
  }

  /**
   * Sends a task directly to a specific department.
   *
   * <p>Bypasses the executive and sends the task directly to the department manager.
   *
   * @param departmentName the department name
   * @param task           the task description
   * @return the department result
   * @throws IllegalArgumentException if department doesn't exist
   */
  public @NonNull AgentResult sendToDepartment(
          @NonNull String departmentName, @NonNull String task) {
    Objects.requireNonNull(departmentName, "departmentName cannot be null");
    Objects.requireNonNull(task, "task cannot be null");

    SupervisorAgent deptSupervisor = departmentSupervisors.get(departmentName);
    if (deptSupervisor == null) {
      throw new IllegalArgumentException("Department not found: " + departmentName);
    }

    return deptSupervisor.orchestrate(task);
  }

  /**
   * Sends a Prompt directly to a specific department.
   *
   * <p>Bypasses the executive and sends the prompt directly to the department manager.
   *
   * @param departmentName the department name
   * @param prompt         the task prompt
   * @return the department result
   * @throws IllegalArgumentException if department doesn't exist
   */
  public @NonNull AgentResult sendToDepartment(
          @NonNull String departmentName, @NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return sendToDepartment(departmentName, prompt.text());
  }

  // ===== Interactable Interface Implementation =====

  /**
   * {@inheritDoc} Delegates to {@link #execute(AgenticContext)}.
   */
  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context) {
    return execute(context);
  }

  /**
   * {@inheritDoc} Delegates to {@link #execute(AgenticContext)}. Trace propagated via hierarchy.
   */
  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // HierarchicalAgents doesn't directly use trace; it's passed through supervisor hierarchy
    return execute(context);
  }

  /**
   * {@inheritDoc} Delegates to {@link #executeStream(AgenticContext)}.
   */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context) {
    return executeStream(context);
  }

  /**
   * {@inheritDoc} Delegates to {@link #executeStream(AgenticContext)}. Trace propagated via hierarchy.
   */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // HierarchicalAgents doesn't directly use trace; it's passed through supervisor hierarchy
    return executeStream(context);
  }


  private SupervisorAgent buildHierarchy() {
    // Build department supervisors first
    for (Map.Entry<String, Department> entry : departments.entrySet()) {
      String deptName = entry.getKey();
      Department dept = entry.getValue();

      // Create supervisor for this department
      SupervisorAgent.Builder deptSupervisorBuilder =
              SupervisorAgent.builder()
                      .name(dept.manager().name() + "_Supervisor")
                      .model(dept.manager().model())
                      .instructions(buildManagerInstructions(dept))
                      .responder(getResponderFromAgent(dept.manager()))
                      .maxTurns(maxTurns);

      // Add workers to department supervisor
      for (Interactable worker : dept.workers()) {
        deptSupervisorBuilder.addWorker(worker, "Worker in " + deptName + " department");
      }

      departmentSupervisors.put(deptName, deptSupervisorBuilder.build());
    }

    // Build root supervisor (executive) with department supervisors as workers
    SupervisorAgent.Builder rootBuilder =
            SupervisorAgent.builder()
                    .name(executive.name() + "_Executive")
                    .model(executive.model())
                    .instructions(buildExecutiveInstructions())
                    .responder(getResponderFromAgent(executive))
                    .maxTurns(maxTurns);

    for (Map.Entry<String, Department> entry : departments.entrySet()) {
      String deptName = entry.getKey();
      SupervisorAgent deptSupervisor = departmentSupervisors.get(deptName);
      rootBuilder.addWorker(
              deptSupervisor.underlyingAgent(),
              deptName + " department - " + entry.getValue().manager().instructions().text());
    }

    return rootBuilder.build();
  }

  private String buildExecutiveInstructions() {
    StringBuilder sb = new StringBuilder();
    sb.append(executive.instructions().text()).append("\n\n");
    sb.append("You are the executive overseeing the following departments:\n\n");

    for (Map.Entry<String, Department> entry : departments.entrySet()) {
      sb.append("- **").append(entry.getKey()).append("**: ");
      sb.append("Managed by ").append(entry.getValue().manager().name());
      sb.append(" with ").append(entry.getValue().workers().size()).append(" workers\n");
    }

    sb.append("\nDelegate tasks to appropriate departments. ");
    sb.append("Aggregate their results for final response.");
    return sb.toString();
  }

  private String buildManagerInstructions(Department dept) {
    StringBuilder sb = new StringBuilder();
    sb.append(dept.manager().instructions().text()).append("\n\n");
    sb.append("You manage the following team:\n\n");

    for (Interactable worker : dept.workers()) {
      sb.append("- **").append(worker.name()).append("\n");
    }

    sb.append("\nDelegate subtasks to your team members. ");
    sb.append("Coordinate their outputs into a cohesive result.");
    return sb.toString();
  }

  // Helper to get responder from agent - uses reflection since field is private
  private com.paragon.responses.Responder getResponderFromAgent(Agent agent) {
    // The agent doesn't expose its responder, so we need to use the first available
    // For production code, we'd want the Agent to provide access to its responder
    // For now, we use the executive's responder configuration
    try {
      var field = Agent.class.getDeclaredField("responder");
      field.setAccessible(true);
      return (com.paragon.responses.Responder) field.get(agent);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get responder from agent", e);
    }
  }

  /**
   * Represents a department with a manager and workers.
   */
  public record Department(
          @NonNull Agent manager, @NonNull List<Interactable> workers, @Nullable SupervisorAgent supervisor) {
    public Department {
      Objects.requireNonNull(manager, "manager cannot be null");
      Objects.requireNonNull(workers, "workers cannot be null");
      if (workers.isEmpty()) {
        throw new IllegalArgumentException("At least one worker is required per department");
      }
    }

    /**
     * Creates a department (supervisor is set internally).
     */
    public Department(@NonNull Agent manager, @NonNull List<Interactable> workers) {
      this(manager, List.copyOf(workers), null);
    }
  }

  /**
   * Builder for HierarchicalAgents.
   */
  public static final class Builder {
    private final Map<String, Department> departments = new HashMap<>();
    private @Nullable Agent executive;
    private int maxTurns = 10;
    private @Nullable TraceMetadata traceMetadata;

    private Builder() {
    }

    /**
     * Sets the executive agent at the top of the hierarchy.
     *
     * @param executive the executive agent
     * @return this builder
     */
    public @NonNull Builder executive(@NonNull Agent executive) {
      this.executive = Objects.requireNonNull(executive);
      return this;
    }

    /**
     * Adds a department with a manager and workers.
     *
     * <p>Workers can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.
     *
     * @param name    the department name
     * @param manager the department manager (must be an Agent for responder/model access)
     * @param workers the workers in this department
     * @return this builder
     */
    public @NonNull Builder addDepartment(
            @NonNull String name, @NonNull Agent manager, @NonNull Interactable... workers) {
      Objects.requireNonNull(name, "name cannot be null");
      Objects.requireNonNull(manager, "manager cannot be null");
      Objects.requireNonNull(workers, "workers cannot be null");

      List<Interactable> workerList = new ArrayList<>();
      for (Interactable worker : workers) {
        Objects.requireNonNull(worker, "worker cannot be null");
        workerList.add(worker);
      }

      if (workerList.isEmpty()) {
        throw new IllegalArgumentException("At least one worker is required per department");
      }

      this.departments.put(name, new Department(manager, workerList));
      return this;
    }

    /**
     * Adds a department with a manager and worker list.
     *
     * @param name    the department name
     * @param manager the department manager (must be an Agent for responder/model access)
     * @param workers the workers in this department
     * @return this builder
     */
    public @NonNull Builder addDepartment(
            @NonNull String name, @NonNull Agent manager, @NonNull List<Interactable> workers) {
      Objects.requireNonNull(name, "name cannot be null");
      Objects.requireNonNull(manager, "manager cannot be null");
      Objects.requireNonNull(workers, "workers cannot be null");

      if (workers.isEmpty()) {
        throw new IllegalArgumentException("At least one worker is required per department");
      }

      this.departments.put(name, new Department(manager, workers));
      return this;
    }

    /**
     * Sets the maximum turns for each level of the hierarchy.
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
     * Builds the HierarchicalAgents.
     *
     * @return the configured hierarchy
     */
    public @NonNull HierarchicalAgents build() {
      return new HierarchicalAgents(this);
    }
  }
}
