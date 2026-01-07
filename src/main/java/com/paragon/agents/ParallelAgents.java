package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
import org.jspecify.annotations.NonNull;

/**
 * Orchestrates parallel execution of multiple agents.
 *
 * <p>From Chapter 7 (Multi-Agent Collaboration): "Multiple agents work on different parts of a
 * problem simultaneously, and their results are later combined."
 *
 * <p><b>Virtual Thread Design:</b> Uses Java 21+ structured concurrency for parallel execution.
 * All agents run on virtual threads, making parallel execution cheap and efficient.
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
 * // Blocking call - uses virtual threads internally
 * List<AgentResult> results = team.run("Analyze market trends");
 *
 * // Get just the first to complete
 * AgentResult fastest = team.runFirst("Quick analysis needed");
 *
 * // Synthesize with another agent
 * AgentResult combined = team.runAndSynthesize("What's the outlook?", writer);
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

  // ===== Run Methods =====

  /**
   * Runs all agents concurrently with the same input.
   *
   * <p>Each agent receives the same input and processes it independently with a fresh context.
   * Uses virtual threads for parallel execution.
   *
   * @param input the input text for all agents
   * @return list of results, in the same order as agents()
   */
  public @NonNull List<AgentResult> run(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return run(context);
  }

  /**
   * Runs all agents concurrently with Text content.
   *
   * @param text the text content for all agents
   * @return list of results
   */
  public @NonNull List<AgentResult> run(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return run(context);
  }

  /**
   * Runs all agents concurrently with a Message.
   *
   * @param message the message for all agents
   * @return list of results
   */
  public @NonNull List<AgentResult> run(@NonNull Message message) {
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
   * <p>Uses structured concurrency (Java 21+) to run all agents on virtual threads.
   *
   * <p>This is the core method. All other run overloads delegate here.
   *
   * @param context the context to copy for each agent
   * @return list of results, in the same order as agents()
   */
  public @NonNull List<AgentResult> run(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Generate a shared parent traceId for all parallel agents
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      // Fork all agents as subtasks
      List<StructuredTaskScope.Subtask<AgentResult>> subtasks = new ArrayList<>();
      for (Agent agent : agents) {
        AgentContext ctx = context.copy();
        ctx.withTraceContext(parentTraceId, parentSpanId);
        subtasks.add(scope.fork(() -> agent.interact(ctx)));
      }

      // Wait for all to complete
      scope.join();
      scope.throwIfFailed();

      // Collect results in order
      List<AgentResult> results = new ArrayList<>();
      for (var subtask : subtasks) {
        results.add(subtask.get());
      }
      return results;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Parallel execution interrupted", e);
    } catch (java.util.concurrent.ExecutionException e) {
      throw new RuntimeException("Parallel execution failed", e.getCause() != null ? e.getCause() : e);
    }
  }

  // ===== RunFirst Methods (Racing) =====

  /**
   * Runs all agents concurrently and returns the first to complete.
   *
   * <p>Useful when you want the fastest response and don't need all results.
   *
   * @param input the input text for all agents
   * @return the first result
   */
  public @NonNull AgentResult runFirst(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(input));
    return runFirst(context);
  }

  /**
   * Runs all agents concurrently with Text content and returns the first to complete.
   *
   * @param text the text content for all agents
   * @return the first result
   */
  public @NonNull AgentResult runFirst(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return runFirst(context);
  }

  /**
   * Runs all agents concurrently with a Message and returns the first to complete.
   *
   * @param message the message for all agents
   * @return the first result
   */
  public @NonNull AgentResult runFirst(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return runFirst(context);
  }

  /**
   * Runs all agents concurrently using an existing context and returns the first to complete.
   *
   * <p>Uses structured concurrency with ShutdownOnSuccess to cancel other agents once one
   * completes.
   *
   * @param context the context to copy for each agent
   * @return the first result
   */
  public @NonNull AgentResult runFirst(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    try (var scope = new StructuredTaskScope.ShutdownOnSuccess<AgentResult>()) {
      // Fork all agents as subtasks
      for (Agent agent : agents) {
        AgentContext ctx = context.copy();
        scope.fork(() -> agent.interact(ctx));
      }

      // Wait for first to complete
      scope.join();
      return scope.result();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Parallel execution interrupted", e);
    } catch (java.util.concurrent.ExecutionException e) {
      throw new RuntimeException("Parallel execution failed", e.getCause() != null ? e.getCause() : e);
    }
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
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(@NonNull String input, @NonNull Agent synthesizer) {
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
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(@NonNull Text text, @NonNull Agent synthesizer) {
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
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(
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
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(
      @NonNull AgentContext context, @NonNull Agent synthesizer) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");

    // Extract original query for synthesis prompt
    String originalQuery = extractLastUserMessage(context);

    // Run all agents in parallel
    List<AgentResult> results = run(context);

    // Build synthesis prompt with all outputs
    StringBuilder synthesisPrompt = new StringBuilder();
    synthesisPrompt.append("Original query: ").append(originalQuery).append("\n\n");
    synthesisPrompt.append("The following agents have provided their outputs:\n\n");

    for (int i = 0; i < agents.size(); i++) {
      Agent agent = agents.get(i);
      AgentResult result = results.get(i);
      synthesisPrompt.append("--- ").append(agent.name()).append(" ---\n");
      if (result.isError()) {
        synthesisPrompt.append("[ERROR: ").append(result.error().getMessage()).append("]\n");
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
