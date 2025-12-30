package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Orchestrates parallel execution of multiple agents.
 *
 * <p>From Chapter 7 (Multi-Agent Collaboration): "Multiple agents work on different parts of a
 * problem simultaneously, and their results are later combined."
 *
 * <p><b>All methods are async by default</b> - they return {@link CompletableFuture}. For blocking
 * calls, use {@code .join()}.
 *
 * <p><b>Trace Correlation:</b> All parallel agents share the same parent traceId, enabling
 * end-to-end debugging of fan-out patterns.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Agent researcher = Agent.builder().name("Researcher")...build();
 * Agent analyst = Agent.builder().name("Analyst")...build();
 * Agent writer = Agent.builder().name("Writer")...build();
 *
 * ParallelAgents team = ParallelAgents.of(researcher, analyst);
 *
 * // Async (returns immediately)
 * team.run("Analyze market trends")
 *     .thenAccept(results -> results.forEach(r -> System.out.println(r.output())));
 *
 * // Blocking (if you need sync)
 * List<AgentResult> results = team.run("...").join();
 *
 * // Synthesize with another agent
 * team.runAndSynthesize("What's the outlook?", writer)
 *     .thenAccept(combined -> System.out.println(combined.output()));
 * }</pre>
 *
 * @since 1.0
 */
public final class ParallelAgents {

  private final @NonNull List<Agent> agents;

  private ParallelAgents(@NonNull List<Agent> agents) {
    if (agents.isEmpty()) {
      throw new IllegalArgumentException("At least one agent is required");
    }
    this.agents = List.copyOf(agents);
  }

  /**
   * Creates a parallel agent orchestrator with the given agents.
   *
   * @param agents the agents to run in parallel (at least one required)
   * @return a new ParallelAgents instance
   */
  public static @NonNull ParallelAgents of(@NonNull Agent... agents) {
    Objects.requireNonNull(agents, "agents cannot be null");
    return new ParallelAgents(Arrays.asList(agents));
  }

  /**
   * Creates a parallel agent orchestrator from a list of agents.
   *
   * @param agents the list of agents (at least one required)
   * @return a new ParallelAgents instance
   */
  public static @NonNull ParallelAgents of(@NonNull List<Agent> agents) {
    Objects.requireNonNull(agents, "agents cannot be null");
    return new ParallelAgents(agents);
  }

  /**
   * Returns the agents in this orchestrator.
   *
   * @return unmodifiable list of agents
   */
  public @NonNull List<Agent> agents() {
    return agents;
  }

  // ===== Run Methods (All Async) =====

  /**
   * Runs all agents concurrently with the same input.
   *
   * <p>Each agent receives the same input and processes it independently.
   *
   * @param input the input text for all agents
   * @return future completing with list of results, in the same order as agents()
   */
  public @NonNull CompletableFuture<List<AgentResult>> run(@NonNull String input) {
    return run(input, null);
  }

  /**
   * Runs all agents concurrently with the same input and shared context.
   *
   * <p>Each agent receives a copy of the context to prevent interference. All parallel agents share
   * the same parent traceId for trace correlation.
   *
   * @param input the input text for all agents
   * @param sharedContext optional shared context (each agent gets a copy)
   * @return future completing with list of results, in the same order as agents()
   */
  public @NonNull CompletableFuture<List<AgentResult>> run(
      @NonNull String input, @Nullable AgentContext sharedContext) {
    Objects.requireNonNull(input, "input cannot be null");

    // Generate a shared parent traceId for all parallel agents
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    // Create futures for all agents
    List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
    for (Agent agent : agents) {
      AgentContext ctx = sharedContext != null ? sharedContext.copy() : AgentContext.create();
      // Each agent gets a forked context with shared parent trace
      ctx.withTraceContext(parentTraceId, parentSpanId);
      ctx.addInput(Message.user(input));
      futures.add(agent.interact(ctx));
    }

    // Combine all futures
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              List<AgentResult> results = new ArrayList<>();
              for (CompletableFuture<AgentResult> future : futures) {
                results.add(future.join());
              }
              return results;
            });
  }

  /**
   * Runs all agents concurrently and returns the first to complete.
   *
   * <p>Useful when you want the fastest response and don't need all results.
   *
   * @param input the input text for all agents
   * @return future completing with the first result
   */
  public @NonNull CompletableFuture<AgentResult> runFirst(@NonNull String input) {
    return runFirst(input, null);
  }

  /**
   * Runs all agents concurrently and returns the first to complete.
   *
   * @param input the input text for all agents
   * @param sharedContext optional shared context
   * @return future completing with the first result
   */
  public @NonNull CompletableFuture<AgentResult> runFirst(
      @NonNull String input, @Nullable AgentContext sharedContext) {
    Objects.requireNonNull(input, "input cannot be null");

    // Create futures for all agents
    List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
    for (Agent agent : agents) {
      AgentContext ctx = sharedContext != null ? sharedContext.copy() : AgentContext.create();
      ctx.addInput(Message.user(input));
      futures.add(agent.interact(ctx));
    }

    // Return first to complete
    return CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(result -> (AgentResult) result);
  }

  /**
   * Runs all agents concurrently, then synthesizes their outputs with a synthesizer agent.
   *
   * <p>The synthesizer receives a formatted summary of all agent outputs and produces a combined
   * result. This is the "fan-out, fan-in" pattern.
   *
   * @param input the input text for all agents
   * @param synthesizer the agent that combines results
   * @return future completing with the synthesized result
   */
  public @NonNull CompletableFuture<AgentResult> runAndSynthesize(
      @NonNull String input, @NonNull Agent synthesizer) {
    return runAndSynthesize(input, synthesizer, null);
  }

  /**
   * Runs all agents concurrently, then synthesizes their outputs.
   *
   * @param input the input text for all agents
   * @param synthesizer the agent that combines results
   * @param sharedContext optional shared context
   * @return future completing with the synthesized result
   */
  public @NonNull CompletableFuture<AgentResult> runAndSynthesize(
      @NonNull String input, @NonNull Agent synthesizer, @Nullable AgentContext sharedContext) {
    Objects.requireNonNull(input, "input cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");

    // Run all agents in parallel, then synthesize
    return run(input, sharedContext)
        .thenCompose(
            results -> {
              // Build synthesis prompt with all outputs
              StringBuilder synthesisPrompt = new StringBuilder();
              synthesisPrompt.append("Original query: ").append(input).append("\n\n");
              synthesisPrompt.append("The following agents have provided their outputs:\n\n");

              for (int i = 0; i < agents.size(); i++) {
                Agent agent = agents.get(i);
                AgentResult result = results.get(i);
                synthesisPrompt.append("--- ").append(agent.name()).append(" ---\n");
                if (result.isError()) {
                  synthesisPrompt
                      .append("[ERROR: ")
                      .append(result.error().getMessage())
                      .append("]\n");
                } else {
                  synthesisPrompt
                      .append(result.output() != null ? result.output() : "[No output]")
                      .append("\n");
                }
                synthesisPrompt.append("\n");
              }

              synthesisPrompt.append("Please synthesize these outputs into a coherent response.");

              // Run synthesizer
              AgentContext synthContext =
                  sharedContext != null ? sharedContext.copy() : AgentContext.create();
              synthContext.addInput(Message.user(synthesisPrompt.toString()));
              return synthesizer.interact(synthContext);
            });
  }
}
