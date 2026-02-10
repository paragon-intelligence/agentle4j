package com.paragon.agents;

import com.paragon.prompts.Prompt;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;

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
public final class ParallelAgents implements Interactable {

  private final @NonNull String name;
  private final @NonNull List<Interactable> members;
  private final @Nullable TraceMetadata traceMetadata;

  private ParallelAgents(@NonNull String name, @NonNull List<Interactable> members, @Nullable TraceMetadata traceMetadata) {
    if (members.isEmpty()) {
      throw new IllegalArgumentException("At least one member is required");
    }
    this.name = name;
    this.members = List.copyOf(members);
    this.traceMetadata = traceMetadata;
  }

  /**
   * Creates a parallel orchestrator with the given members.
   *
   * <p>Members can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.
   *
   * @param members the members to run in parallel (at least one required)
   * @return a new ParallelAgents instance
   */
  public static @NonNull ParallelAgents of(@NonNull Interactable... members) {
    Objects.requireNonNull(members, "members cannot be null");
    return new ParallelAgents("ParallelAgents", Arrays.asList(members), null);
  }

  /**
   * Creates a parallel orchestrator from a list of members.
   *
   * @param members the list of members (at least one required)
   * @return a new ParallelAgents instance
   */
  public static @NonNull ParallelAgents of(@NonNull List<Interactable> members) {
    Objects.requireNonNull(members, "members cannot be null");
    return new ParallelAgents("ParallelAgents", members, null);
  }

  /**
   * Creates a named parallel orchestrator with the given members.
   *
   * @param name    the name for this orchestrator
   * @param members the members to run in parallel
   * @return a new ParallelAgents instance
   */
  public static @NonNull ParallelAgents named(@NonNull String name, @NonNull Interactable... members) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(members, "members cannot be null");
    return new ParallelAgents(name, Arrays.asList(members), null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return name;
  }

  /**
   * Returns the members in this orchestrator.
   *
   * @return unmodifiable list of members
   */
  public @NonNull List<Interactable> members() {
    return members;
  }

  // ===== RunAll Methods (All results) =====

  /**
   * Runs all agents concurrently with the same input.
   *
   * <p>Each agent receives the same input and processes it independently with a fresh context.
   * Uses virtual threads for parallel execution.
   *
   * @param input the input text for all agents
   * @return list of results, in the same order as agents()
   */
  public @NonNull List<AgentResult> runAll(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(input));
    return runAll(context);
  }

  /**
   * Runs all agents concurrently with Text content.
   *
   * @param text the text content for all agents
   * @return list of results
   */
  public @NonNull List<AgentResult> runAll(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(text));
    return runAll(context);
  }

  /**
   * Runs all agents concurrently with a Message.
   *
   * @param message the message for all agents
   * @return list of results
   */
  public @NonNull List<AgentResult> runAll(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(message);
    return runAll(context);
  }

  /**
   * Runs all agents concurrently with a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt for all agents
   * @return list of results
   */
  public @NonNull List<AgentResult> runAll(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return runAll(prompt.text());
  }

  /**
   * Runs all agents concurrently using an existing context.
   *
   * <p>Each agent receives a copy of the context to prevent interference. All parallel agents share
   * the same parent traceId for trace correlation.
   *
   * <p>Uses structured concurrency (Java 21+) to run all agents on virtual threads.
   *
   * <p>This is the core method. All other runAll overloads delegate here.
   *
   * @param context the context to copy for each agent
   * @return list of results, in the same order as agents()
   */
  public @NonNull List<AgentResult> runAll(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Generate a shared parent traceId for all parallel agents
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
      // Fork all members as subtasks
      List<StructuredTaskScope.Subtask<AgentResult>> subtasks = new ArrayList<>();
      for (Interactable member : members) {
        AgenticContext ctx = context.copy();
        ctx.withTraceContext(parentTraceId, parentSpanId);
        subtasks.add(scope.fork(() -> member.interact(ctx)));
      }

      // Wait for all to complete
      scope.join();

      // Collect results in order
      List<AgentResult> results = new ArrayList<>();
      for (var subtask : subtasks) {
        results.add(subtask.get());
      }
      return results;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Parallel execution interrupted", e);
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
    AgenticContext context = AgenticContext.create();
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
    AgenticContext context = AgenticContext.create();
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
    AgenticContext context = AgenticContext.create();
    context.addInput(message);
    return runFirst(context);
  }

  /**
   * Runs all agents concurrently with a Prompt and returns the first to complete.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt for all agents
   * @return the first result
   */
  public @NonNull AgentResult runFirst(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return runFirst(prompt.text());
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
  public @NonNull AgentResult runFirst(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<AgentResult>anySuccessfulResultOrThrow())) {
      // Fork all members as subtasks
      for (Interactable member : members) {
        AgenticContext ctx = context.copy();
        scope.fork(() -> member.interact(ctx));
      }

      // Wait for first to complete
      return scope.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Parallel execution interrupted", e);
    }
  }

  // ===== RunAndSynthesize Methods (Fan-out/Fan-in) =====

  /**
   * Runs all members concurrently, then synthesizes their outputs with a synthesizer.
   *
   * <p>The synthesizer receives a formatted summary of all member outputs and produces a combined
   * result. This is the "fan-out, fan-in" pattern.
   *
   * @param input       the input text for all members
   * @param synthesizer the Interactable that combines results
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(@NonNull String input, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(input, "input cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(input));
    return runAndSynthesize(context, synthesizer);
  }

  /**
   * Runs all members concurrently with Text content, then synthesizes with a synthesizer.
   *
   * @param text        the text content for all members
   * @param synthesizer the Interactable that combines results
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(@NonNull Text text, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(text, "text cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(text));
    return runAndSynthesize(context, synthesizer);
  }

  /**
   * Runs all members concurrently with a Message, then synthesizes with a synthesizer.
   *
   * @param message     the message for all members
   * @param synthesizer the Interactable that combines results
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(
          @NonNull Message message, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(message, "message cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(message);
    return runAndSynthesize(context, synthesizer);
  }

  /**
   * Runs all members concurrently with a Prompt, then synthesizes with a synthesizer.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt      the prompt for all members
   * @param synthesizer the Interactable that combines results
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(
          @NonNull Prompt prompt, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    return runAndSynthesize(prompt.text(), synthesizer);
  }

  /**
   * Runs all members concurrently using an existing context, then synthesizes with a synthesizer.
   *
   * <p>This is the core method. All other runAndSynthesize overloads delegate here.
   *
   * @param context     the context to copy for each member
   * @param synthesizer the Interactable that combines results
   * @return the synthesized result
   */
  public @NonNull AgentResult runAndSynthesize(
          @NonNull AgenticContext context, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");

    // Extract original query for synthesis prompt
    String originalQuery = extractLastUserMessage(context);

    // Run all members in parallel
    List<AgentResult> results = runAll(context);

    // Build synthesis prompt with all outputs
    StringBuilder synthesisPrompt = new StringBuilder();
    synthesisPrompt.append("Original query: ").append(originalQuery).append("\n\n");
    synthesisPrompt.append("The following participants have provided their outputs:\n\n");

    for (int i = 0; i < members.size(); i++) {
      Interactable member = members.get(i);
      AgentResult result = results.get(i);
      synthesisPrompt.append("--- ").append(member.name()).append(" ---\n");
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
    AgenticContext synthContext = AgenticContext.create();
    synthContext.addInput(Message.user(synthesisPrompt.toString()));
    return synthesizer.interact(synthContext);
  }

  // ===== Streaming Methods (All agents) =====

  /**
   * Runs all agents concurrently with streaming.
   *
   * @param input the input text for all agents
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAllStream(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(input));
    return runAllStream(context);
  }

  /**
   * Runs all agents concurrently with streaming using a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt for all agents
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAllStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return runAllStream(prompt.text());
  }

  /**
   * Runs all agents concurrently with streaming using an existing context.
   *
   * @param context the context to copy for each agent
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAllStream(@NonNull AgenticContext context) {
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
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(input));
    return runFirstStream(context);
  }

  /**
   * Runs all agents concurrently with streaming using a Prompt and returns when first completes.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt for all agents
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runFirstStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return runFirstStream(prompt.text());
  }

  /**
   * Runs all agents concurrently with streaming and returns when first completes.
   *
   * @param context the context to copy for each agent
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runFirstStream(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new ParallelStream(this, context, ParallelStream.Mode.FIRST);
  }

  /**
   * Runs all agents concurrently with streaming, then synthesizes results.
   *
   * @param input       the input text for all agents
   * @param synthesizer the agent that combines results
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAndSynthesizeStream(
          @NonNull String input, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(input, "input cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(input));
    return runAndSynthesizeStream(context, synthesizer);
  }

  /**
   * Runs all agents concurrently with streaming using a Prompt, then synthesizes results.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt      the prompt for all agents
   * @param synthesizer the agent that combines results
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAndSynthesizeStream(
          @NonNull Prompt prompt, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    return runAndSynthesizeStream(prompt.text(), synthesizer);
  }

  /**
   * Runs all agents concurrently with streaming, then synthesizes results.
   *
   * @param context     the context to copy for each agent
   * @param synthesizer the agent that combines results
   * @return a ParallelStream for processing streaming events
   */
  public @NonNull ParallelStream runAndSynthesizeStream(
          @NonNull AgenticContext context, @NonNull Interactable synthesizer) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(synthesizer, "synthesizer cannot be null");
    return new ParallelStream(this, context, ParallelStream.Mode.SYNTHESIZE, synthesizer);
  }

  // ===== Helper Methods =====

  private String extractLastUserMessage(AgenticContext context) {
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

  // ===== Interactable Interface Implementation =====
  // Note: interact() runs all agents and returns a composite result.
  // The primary result is the first to complete; all others are in relatedResults().

  /**
   * {@inheritDoc} Runs all agents; first to complete is primary, others in relatedResults().
   */
  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context) {
    List<AgentResult> allResults = runAll(context);
    return toCompositeResult(allResults);
  }

  /**
   * {@inheritDoc} Runs all agents; trace propagated through peer interactions.
   */
  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // ParallelAgents doesn't directly use trace; it's passed through peer interactions
    List<AgentResult> allResults = runAll(context);
    return toCompositeResult(allResults);
  }

  private AgentResult toCompositeResult(List<AgentResult> allResults) {
    if (allResults.isEmpty()) {
      AgenticContext ctx = AgenticContext.create();
      return AgentResult.error(new IllegalStateException("No agents configured"), ctx, 0);
    }
    AgentResult primary = allResults.get(0);
    List<AgentResult> related = allResults.size() > 1
            ? allResults.subList(1, allResults.size())
            : List.of();
    return AgentResult.composite(primary, related);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Streams and returns when the first agent completes. For streaming all agents, use {@link
   * #runAllStream(String)} with the explicit ParallelStream return type.
   */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    if (!members.isEmpty()) {
      return members.get(0).interactStream(context.copy());
    }
    return AgentStream.failed(
            AgentResult.error(new IllegalStateException("No members configured"), context, 0));
  }

  /**
   * {@inheritDoc} Streams first member; trace propagated through peer interactions.
   */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // ParallelAgents doesn't directly use trace; it's passed through peer interactions
    Objects.requireNonNull(context, "context cannot be null");
    if (!members.isEmpty()) {
      return members.get(0).interactStream(context.copy());
    }
    return AgentStream.failed(
            AgentResult.error(new IllegalStateException("No members configured"), context, 0));
  }
}
