package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Streaming wrapper for ParallelAgents that provides event callbacks during parallel execution.
 *
 * <pre>{@code
 * team.runStream("Analyze market")
 *     .onAgentTextDelta((agent, delta) -> System.out.print("[" + agent.name() + "] " + delta))
 *     .onAgentComplete((agent, result) -> System.out.println(agent.name() + " done!"))
 *     .onComplete(results -> System.out.println("All done!"))
 *     .start();
 * }</pre>
 *
 * @since 1.0
 */
public final class ParallelStream {

  private final ParallelAgents orchestrator;
  private final AgenticContext context;
  private final Mode mode;
  private final @Nullable Interactable synthesizer;
  // Callbacks
  private BiConsumer<Interactable, String> onAgentTextDelta;
  private BiConsumer<Interactable, AgentResult> onAgentComplete;
  private Consumer<List<AgentResult>> onAllComplete;
  private Consumer<AgentResult> onFirstComplete;
  private Consumer<AgentResult> onSynthesisComplete;
  private Consumer<Throwable> onError;
  private BiConsumer<Interactable, Integer> onAgentTurnStart;
  ParallelStream(ParallelAgents orchestrator, AgenticContext context, Mode mode) {
    this(orchestrator, context, mode, null);
  }

  ParallelStream(
          ParallelAgents orchestrator, AgenticContext context, Mode mode, @Nullable Interactable synthesizer) {
    this.orchestrator = Objects.requireNonNull(orchestrator);
    this.context = Objects.requireNonNull(context);
    this.mode = Objects.requireNonNull(mode);
    this.synthesizer = synthesizer;
  }

  /**
   * Called for each text delta from any member.
   *
   * @param callback receives the member and text chunk
   * @return this stream
   */
  public @NonNull ParallelStream onAgentTextDelta(@NonNull BiConsumer<Interactable, String> callback) {
    this.onAgentTextDelta = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when an individual member completes.
   *
   * @param callback receives the member and its result
   * @return this stream
   */
  public @NonNull ParallelStream onAgentComplete(@NonNull BiConsumer<Interactable, AgentResult> callback) {
    this.onAgentComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when all agents complete (for ALL mode).
   *
   * @param callback receives list of all results
   * @return this stream
   */
  public @NonNull ParallelStream onComplete(@NonNull Consumer<List<AgentResult>> callback) {
    this.onAllComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when the first agent completes (for FIRST mode).
   *
   * @param callback receives the first result
   * @return this stream
   */
  public @NonNull ParallelStream onFirstComplete(@NonNull Consumer<AgentResult> callback) {
    this.onFirstComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when synthesis completes (for SYNTHESIZE mode).
   *
   * @param callback receives the synthesized result
   * @return this stream
   */
  public @NonNull ParallelStream onSynthesisComplete(@NonNull Consumer<AgentResult> callback) {
    this.onSynthesisComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when an error occurs.
   *
   * @param callback receives the error
   * @return this stream
   */
  public @NonNull ParallelStream onError(@NonNull Consumer<Throwable> callback) {
    this.onError = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called at the start of each turn for any member.
   *
   * @param callback receives the member and turn number
   * @return this stream
   */
  public @NonNull ParallelStream onAgentTurnStart(@NonNull BiConsumer<Interactable, Integer> callback) {
    this.onAgentTurnStart = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Starts the streaming parallel execution. Blocks until completion.
   *
   * <p>On virtual threads, blocking is efficient and does not consume platform threads.
   *
   * @return results based on mode: List&lt;AgentResult&gt; for ALL, AgentResult for FIRST/SYNTHESIZE
   */
  public @NonNull Object start() {
    try {
      return switch (mode) {
        case ALL -> startAll();
        case FIRST -> startFirst();
        case SYNTHESIZE -> startSynthesize();
      };
    } catch (Exception ex) {
      if (onError != null) {
        onError.accept(ex);
      }
      throw new RuntimeException(ex);
    }
  }

  private List<AgentResult> startAll() {
    List<Interactable> members = orchestrator.members();
    List<AgentResult> results = Collections.synchronizedList(new ArrayList<>());

    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    // Run all members in parallel using virtual threads
    List<Thread> threads = new ArrayList<>();

    for (Interactable member : members) {
      Thread thread = Thread.startVirtualThread(() -> {
        AgenticContext ctx = context.copy();
        ctx.withTraceContext(parentTraceId, parentSpanId);

        AgentStream stream = member.interactStream(ctx);

        if (onAgentTextDelta != null) {
          stream.onTextDelta(delta -> onAgentTextDelta.accept(member, delta));
        }

        if (onAgentTurnStart != null) {
          stream.onTurnStart(turn -> onAgentTurnStart.accept(member, turn));
        }

        AgentResult result = stream.start();
        results.add(result);

        if (onAgentComplete != null) {
          onAgentComplete.accept(member, result);
        }
      });
      threads.add(thread);
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Parallel execution interrupted", e);
      }
    }

    if (onAllComplete != null) {
      onAllComplete.accept(results);
    }

    return results;
  }

  private AgentResult startFirst() {
    List<Interactable> members = orchestrator.members();
    AtomicBoolean firstCompleted = new AtomicBoolean(false);
    AtomicReference<AgentResult> firstResult = new AtomicReference<>();

    // Run all members in parallel using virtual threads
    List<Thread> threads = new ArrayList<>();

    for (Interactable member : members) {
      Thread thread = Thread.startVirtualThread(() -> {
        AgenticContext ctx = context.copy();
        AgentStream stream = member.interactStream(ctx);

        if (onAgentTextDelta != null) {
          stream.onTextDelta(delta -> onAgentTextDelta.accept(member, delta));
        }

        AgentResult result = stream.start();

        if (onAgentComplete != null) {
          onAgentComplete.accept(member, result);
        }

        // Only the first to complete wins
        if (firstCompleted.compareAndSet(false, true)) {
          firstResult.set(result);
          if (onFirstComplete != null) {
            onFirstComplete.accept(result);
          }
        }
      });
      threads.add(thread);
    }

    // Wait for first result (polling)
    while (firstResult.get() == null) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Wait for first result interrupted", e);
      }
    }

    return firstResult.get();
  }

  private AgentResult startSynthesize() {
    if (synthesizer == null) {
      throw new IllegalStateException("Synthesizer is required for SYNTHESIZE mode");
    }

    List<Interactable> members = orchestrator.members();
    List<AgentResult> results = Collections.synchronizedList(new ArrayList<>());

    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    // First phase: run all members in parallel using virtual threads
    List<Thread> threads = new ArrayList<>();

    for (Interactable member : members) {
      Thread thread = Thread.startVirtualThread(() -> {
        AgenticContext ctx = context.copy();
        ctx.withTraceContext(parentTraceId, parentSpanId);

        AgentStream stream = member.interactStream(ctx);

        if (onAgentTextDelta != null) {
          stream.onTextDelta(delta -> onAgentTextDelta.accept(member, delta));
        }

        AgentResult result = stream.start();
        results.add(result);

        if (onAgentComplete != null) {
          onAgentComplete.accept(member, result);
        }
      });
      threads.add(thread);
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Parallel execution interrupted", e);
      }
    }

    // Second phase: synthesize results
    String originalQuery = extractLastUserMessage();
    StringBuilder synthesisPrompt = new StringBuilder();
    synthesisPrompt.append("Original query: ").append(originalQuery).append("\n\n");
    synthesisPrompt.append("The following participants have provided their outputs:\n\n");

    for (int i = 0; i < members.size(); i++) {
      Interactable member = members.get(i);
      AgentResult result = results.get(i);
      synthesisPrompt.append("--- ").append(member.name()).append(" ---\n");
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

    // Stream synthesizer
    AgenticContext synthContext = AgenticContext.create();
    synthContext.addInput(Message.user(synthesisPrompt.toString()));

    AgentStream synthStream = synthesizer.interactStream(synthContext);

    if (onAgentTextDelta != null) {
      synthStream.onTextDelta(delta -> onAgentTextDelta.accept(synthesizer, delta));
    }

    AgentResult synthResult = synthStream.start();

    if (onSynthesisComplete != null) {
      onSynthesisComplete.accept(synthResult);
    }

    return synthResult;
  }

  private String extractLastUserMessage() {
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

  /**
   * Execution mode for the parallel stream.
   */
  enum Mode {
    /**
     * Wait for all agents to complete.
     */
    ALL,
    /**
     * Return when first agent completes.
     */
    FIRST,
    /**
     * Run all, then synthesize.
     */
    SYNTHESIZE
  }
}
