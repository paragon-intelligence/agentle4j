package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;

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
 * // Or get just the first to complete
 * AgentResult fastest = team.runFirst("Quick analysis needed").join();
 *
 * // Synthesize with another agent
 * AgentResult combined = team.runAndSynthesize("What's the outlook?", writer).join();
 *
 * // Streaming support
 * team.runStream("Analyze trends")
 *     .onAgentTextDelta((agent, delta) -> System.out.print("[" + agent.name() + "] " + delta))
 *     .onComplete(results -> System.out.println("Done!"))
 *     .start();
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
   * <p>Each agent receives the same input and processes it independently with a fresh context.
   *
   * @param input the input text for all agents
   * @return future completing with list of results, in the same order as agents()
   */
  public @NonNull CompletableFuture<List<AgentResult>> run(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return run(context);
  }

  /**
   * Runs all agents concurrently with Text content.
   *
   * @param text the text content for all agents
   * @return future completing with list of results
   */
  public @NonNull CompletableFuture<List<AgentResult>> run(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return run(context);
  }

  /**
   * Runs all agents concurrently with a Message.
   *
   * @param message the message for all agents
   * @return future completing with list of results
   */
  public @NonNull CompletableFuture<List<AgentResult>> run(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return run(context);
  }

  /**
   * Runs all agents concurrently using an existing context.
   *
   * <p>Each agent receives a copy of the context to prevent interference. All parallel agents share
   * the same parent traceId for trace correlation.
   *
   * <p>This is the core method. All other run overloads delegate here.
   *
   * @param context the context to copy for each agent
   * @return future completing with list of results, in the same order as agents()
   */
  public @NonNull CompletableFuture<List<AgentResult>> run(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Generate a shared parent traceId for all parallel agents
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    // Create futures for all agents
    List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
    for (Agent agent : agents) {
      AgentContext ctx = context.copy();
      ctx.withTraceContext(parentTraceId, parentSpanId);
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

  // ===== RunFirst Methods (Racing) =====

  /**
   * Runs all agents concurrently and returns the first to complete.
   *
   * <p>Useful when you want the fastest response and don't need all results.
   *
   * @param input the input text for all agents
   * @return future completing with the first result
   */
  public @NonNull CompletableFuture<AgentResult> runFirst(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return runFirst(context);
  }

  /**
   * Runs all agents concurrently with Text content and returns the first to complete.
   *
   * @param text the text content for all agents
   * @return future completing with the first result
   */
  public @NonNull CompletableFuture<AgentResult> runFirst(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return runFirst(context);
  }

  /**
   * Runs all agents concurrently with a Message and returns the first to complete.
   *
   * @param message the message for all agents
   * @return future completing with the first result
   */
  public @NonNull CompletableFuture<AgentResult> runFirst(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return runFirst(context);
  }

  /**
   * Runs all agents concurrently using an existing context and returns the first to complete.
   *
   * @param context the context to copy for each agent
   * @return future completing with the first result
   */
  public @NonNull CompletableFuture<AgentResult> runFirst(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Create futures for all agents
    List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
    for (Agent agent : agents) {
      AgentContext ctx = context.copy();
      futures.add(agent.interact(ctx));
    }

    // Return first to complete
    return CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(result -> (AgentResult) result);
  }

  // ===== RunAndSynthesize Methods (Fan-out/Fan-in) =====

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
    Objects.requireNonNull(input, "input cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return runAndSynthesize(context, synthesizer);
  }

  /**
   * Runs all agents concurrently with Text content, then synthesizes with a synthesizer agent.
   *
   * @param text the text content for all agents
   * @param synthesizer the agent that combines results
   * @return future completing with the synthesized result
   */
  public @NonNull CompletableFuture<AgentResult> runAndSynthesize(
      @NonNull Text text, @NonNull Agent synthesizer) {
    Objects.requireNonNull(text, "text cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return runAndSynthesize(context, synthesizer);
  }

  /**
   * Runs all agents concurrently with a Message, then synthesizes with a synthesizer agent.
   *
   * @param message the message for all agents
   * @param synthesizer the agent that combines results
   * @return future completing with the synthesized result
   */
  public @NonNull CompletableFuture<AgentResult> runAndSynthesize(
      @NonNull Message message, @NonNull Agent synthesizer) {
    Objects.requireNonNull(message, "message cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return runAndSynthesize(context, synthesizer);
  }

  /**
   * Runs all agents concurrently using an existing context, then synthesizes with a synthesizer.
   *
   * <p>This is the core method. All other runAndSynthesize overloads delegate here.
   *
   * @param context the context to copy for each agent
   * @param synthesizer the agent that combines results
   * @return future completing with the synthesized result
   */
  public @NonNull CompletableFuture<AgentResult> runAndSynthesize(
      @NonNull AgentContext context, @NonNull Agent synthesizer) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");

    // Extract original query for synthesis prompt
    String originalQuery = extractLastUserMessage(context);

    // Run all agents in parallel, then synthesize
    return run(context)
        .thenCompose(
            results -> {
              // Build synthesis prompt with all outputs
              StringBuilder synthesisPrompt = new StringBuilder();
              synthesisPrompt.append("Original query: ").append(originalQuery).append("\n\n");
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

              // Run synthesizer with fresh context
              AgentContext synthContext = AgentContext.create();
              synthContext.addInput(Message.user(synthesisPrompt.toString()));
              return synthesizer.interact(synthContext);
            });
  }

  // ===== Streaming Methods =====

  /**
   * Runs all agents concurrently with streaming.
   *
   * @param input the input text for all agents
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runStream(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return runStream(context);
  }

  /**
   * Runs all agents concurrently with streaming using an existing context.
   *
   * @param context the context to copy for each agent
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runStream(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new ParallelStream(this, context, ParallelStream.Mode.ALL);
  }

  /**
   * Runs all agents concurrently with streaming and returns when first completes.
   *
   * @param input the input text for all agents
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runFirstStream(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return runFirstStream(context);
  }

  /**
   * Runs all agents concurrently with streaming and returns when first completes.
   *
   * @param context the context to copy for each agent
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runFirstStream(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new ParallelStream(this, context, ParallelStream.Mode.FIRST);
  }

  /**
   * Runs all agents concurrently with streaming, then synthesizes results.
   *
   * @param input the input text for all agents
   * @param synthesizer the agent that combines results
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAndSynthesizeStream(
      @NonNull String input, @NonNull Agent synthesizer) {
    Objects.requireNonNull(input, "input cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return runAndSynthesizeStream(context, synthesizer);
  }

  /**
   * Runs all agents concurrently with streaming, then synthesizes results.
   *
   * @param context the context to copy for each agent
   * @param synthesizer the agent that combines results
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAndSynthesizeStream(
      @NonNull AgentContext context, @NonNull Agent synthesizer) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    return new ParallelStream(this, context, ParallelStream.Mode.SYNTHESIZE, synthesizer);
  }

  // ===== Helper Methods =====

  private String extractLastUserMessage(AgentContext context) {
    List<ResponseInputItem> history = context.getHistory();
    for (int i = history.size() - 1; i >= 0; i--) {
      ResponseInputItem item = history.get(i);
      if (item instanceof Message msg && "user".equals(msg.role())) {
        if (msg.content() != null) {
          for (var content : msg.content()) {
            if (content instanceof Text text) {
              return text.text();
            }
          }
        }
      }
    }
    return "[No query provided]";
  }
}
