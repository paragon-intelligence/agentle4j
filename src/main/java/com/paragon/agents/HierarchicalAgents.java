package com.paragon.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.TraceMetadata;
import java.util.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

  /** Creates a new HierarchicalAgents builder. */
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

  /** {@inheritDoc} */
  @Override
  public @NonNull String name() {
    return executive.name() + "_Hierarchy";
  }

  @Override
  public @NonNull InteractableBlueprint toBlueprint() {
    InteractableBlueprint.AgentBlueprint execBlueprint =
        (InteractableBlueprint.AgentBlueprint) executive.toBlueprint();
    Map<String, InteractableBlueprint.DepartmentBlueprint> deptBlueprints =
        new java.util.LinkedHashMap<>();
    for (var entry : departments.entrySet()) {
      Department dept = entry.getValue();
      InteractableBlueprint.AgentBlueprint mgrBlueprint =
          (InteractableBlueprint.AgentBlueprint) dept.manager().toBlueprint();
      List<InteractableBlueprint> workerBlueprints =
          dept.workers().stream().map(Interactable::toBlueprint).toList();
      deptBlueprints.put(
          entry.getKey(),
          new InteractableBlueprint.DepartmentBlueprint(mgrBlueprint, workerBlueprints));
    }
    return new InteractableBlueprint.HierarchicalAgentsBlueprint(
        execBlueprint, deptBlueprints, maxTurns, traceMetadata);
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
   * Sends a task directly to a specific department.
   *
   * <p>Bypasses the executive and sends the task directly to the department manager.
   *
   * @param departmentName the department name
   * @param task the task description
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

    return deptSupervisor.interact(task);
  }

  // ===== Interactable Interface Implementation =====

  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context) {
    return interact(context, null);
  }

  @Override
  public @NonNull AgentResult interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    Objects.requireNonNull(context, "context cannot be null");
    context.ensureTraceContext();
    return rootSupervisor.interact(context, trace);
  }

  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context) {
    return interactStream(context, null);
  }

  @Override
  public @NonNull AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    Objects.requireNonNull(context, "context cannot be null");
    context.ensureTraceContext();
    return rootSupervisor.interactStream(context, trace);
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
              .responder(dept.manager().responder())
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
            .responder(executive.responder())
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

  /** Represents a department with a manager and workers. */
  public record Department(
      @NonNull Agent manager,
      @NonNull List<Interactable> workers,
      @Nullable SupervisorAgent supervisor) {
    public Department {
      Objects.requireNonNull(manager, "manager cannot be null");
      Objects.requireNonNull(workers, "workers cannot be null");
      if (workers.isEmpty()) {
        throw new IllegalArgumentException("At least one worker is required per department");
      }
    }

    /** Creates a department (supervisor is set internally). */
    public Department(@NonNull Agent manager, @NonNull List<Interactable> workers) {
      this(manager, List.copyOf(workers), null);
    }
  }

  /** Builder for HierarchicalAgents. */
  public static final class Builder {
    private final Map<String, Department> departments = new HashMap<>();
    private @Nullable Agent executive;
    private int maxTurns = 10;
    private @Nullable TraceMetadata traceMetadata;

    private Builder() {}

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
     * @param name the department name
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
     * @param name the department name
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
     * Configures this hierarchy to produce structured output of the specified type.
     *
     * <p>Returns a {@link StructuredBuilder} that builds a {@link HierarchicalAgents.Structured}
     * instead of a regular HierarchicalAgents.
     *
     * <p>The executive's final output is parsed as the specified type.
     *
     * <p>Example:
     *
     * <pre>{@code
     * var hierarchy = HierarchicalAgents.builder()
     *     .executive(executive)
     *     .addDepartment("Engineering", techManager, devAgent)
     *     .structured(ProjectPlan.class)
     *     .build();
     *
     * StructuredAgentResult<ProjectPlan> result = hierarchy.interactStructured("Plan sprint");
     * ProjectPlan plan = result.output();
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
     * Builds the HierarchicalAgents.
     *
     * @return the configured hierarchy
     */
    public @NonNull HierarchicalAgents build() {
      return new HierarchicalAgents(this);
    }
  }

  /**
   * Builder for creating type-safe structured output hierarchical agents.
   *
   * <p>Returned from {@code HierarchicalAgents.builder().structured(Class)}.
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

    public @NonNull StructuredBuilder<T> executive(@NonNull Agent executive) {
      parentBuilder.executive(executive);
      return this;
    }

    public @NonNull StructuredBuilder<T> addDepartment(
        @NonNull String name, @NonNull Agent manager, @NonNull Interactable... workers) {
      parentBuilder.addDepartment(name, manager, workers);
      return this;
    }

    public @NonNull StructuredBuilder<T> addDepartment(
        @NonNull String name, @NonNull Agent manager, @NonNull List<Interactable> workers) {
      parentBuilder.addDepartment(name, manager, workers);
      return this;
    }

    public @NonNull StructuredBuilder<T> maxTurns(int maxTurns) {
      parentBuilder.maxTurns(maxTurns);
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
     * Builds the type-safe structured hierarchy.
     *
     * @return the configured Structured hierarchy
     */
    public HierarchicalAgents.Structured<T> build() {
      HierarchicalAgents hierarchy = parentBuilder.build();
      ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
      return new HierarchicalAgents.Structured<>(hierarchy, outputType, mapper);
    }
  }

  /**
   * Type-safe wrapper for hierarchical agents with structured output.
   *
   * <p>Delegates all interaction to the wrapped HierarchicalAgents and parses the executive's final
   * output as the specified type.
   *
   * @param <T> the output type
   */
  public static final class Structured<T> implements Interactable.Structured<T> {
    private final HierarchicalAgents hierarchy;
    private final Class<T> outputType;
    private final ObjectMapper objectMapper;

    private Structured(
        @NonNull HierarchicalAgents hierarchy,
        @NonNull Class<T> outputType,
        @NonNull ObjectMapper objectMapper) {
      this.hierarchy = Objects.requireNonNull(hierarchy);
      this.outputType = Objects.requireNonNull(outputType);
      this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public @NonNull String name() {
      return hierarchy.name();
    }

    @Override
    public @NonNull AgentResult interact(
        @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      return hierarchy.interact(context, trace);
    }

    @Override
    public @NonNull AgentStream interactStream(
        @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      return hierarchy.interactStream(context, trace);
    }

    @Override
    public @NonNull StructuredAgentResult<T> interactStructured(
        @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      AgentResult result = hierarchy.interact(context, trace);
      return result.toStructured(outputType, objectMapper);
    }

    /** Returns the structured output type. */
    public @NonNull Class<T> outputType() {
      return outputType;
    }

    /** Returns the executive agent at the top of the hierarchy. */
    public @NonNull Agent executive() {
      return hierarchy.executive();
    }

    /** Returns all departments in this hierarchy. */
    public @NonNull Map<String, Department> departments() {
      return hierarchy.departments();
    }
  }
}
