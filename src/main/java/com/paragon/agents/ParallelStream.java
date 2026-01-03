package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

  /** Execution mode for the parallel stream. */
  enum Mode {
    /** Wait for all agents to complete. */
    ALL,
    /** Return when first agent completes. */
    FIRST,
    /** Run all, then synthesize. */
    SYNTHESIZE
  }

  private final ParallelAgents orchestrator;
  private final AgentContext context;
  private final Mode mode;
  private final @Nullable Agent synthesizer;

  // Callbacks
  private BiConsumer<Agent, String> onAgentTextDelta;
  private BiConsumer<Agent, AgentResult> onAgentComplete;
  private Consumer<List<AgentResult>> onAllComplete;
  private Consumer<AgentResult> onFirstComplete;
  private Consumer<AgentResult> onSynthesisComplete;
  private Consumer<Throwable> onError;
  private BiConsumer<Agent, Integer> onAgentTurnStart;

  ParallelStream(ParallelAgents orchestrator, AgentContext context, Mode mode) {
    this(orchestrator, context, mode, null);
  }

  ParallelStream(
      ParallelAgents orchestrator, AgentContext context, Mode mode, @Nullable Agent synthesizer) {
    this.orchestrator = Objects.requireNonNull(orchestrator);
    this.context = Objects.requireNonNull(context);
    this.mode = Objects.requireNonNull(mode);
    this.synthesizer = synthesizer;
  }

  /**
   * Called for each text delta from any agent.
   *
   * @param callback receives the agent and text chunk
   * @return this stream
   */
  public @NonNull ParallelStream onAgentTextDelta(@NonNull BiConsumer<Agent, String> callback) {
    this.onAgentTextDelta = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when an individual agent completes.
   *
   * @param callback receives the agent and its result
   * @return this stream
   */
  public @NonNull ParallelStream onAgentComplete(@NonNull BiConsumer<Agent, AgentResult> callback) {
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
   * Called at the start of each turn for any agent.
   *
   * @param callback receives the agent and turn number
   * @return this stream
   */
  public @NonNull ParallelStream onAgentTurnStart(@NonNull BiConsumer<Agent, Integer> callback) {
    this.onAgentTurnStart = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Starts the streaming parallel execution.
   *
   * @return future completing with results based on mode
   */
  public @NonNull CompletableFuture<?> start() {
    return switch (mode) {
      case ALL -> startAll();
      case FIRST -> startFirst();
      case SYNTHESIZE -> startSynthesize();
    };
  }

  private CompletableFuture<List<AgentResult>> startAll() {
    List<Agent> agents = orchestrator.agents();
    List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
    List<AgentResult> results = new ArrayList<>();

    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    for (Agent agent : agents) {
      AgentContext ctx = context.copy();
      ctx.withTraceContext(parentTraceId, parentSpanId);

      CompletableFuture<AgentResult> future = new CompletableFuture<>();
      futures.add(future);

      AgentStream stream = agent.interactStream(ctx);

      if (onAgentTextDelta != null) {
        final Agent currentAgent = agent;
        stream.onTextDelta(delta -> onAgentTextDelta.accept(currentAgent, delta));
      }

      if (onAgentTurnStart != null) {
        final Agent currentAgent = agent;
        stream.onTurnStart(turn -> onAgentTurnStart.accept(currentAgent, turn));
      }

      final Agent currentAgent = agent;
      stream.onComplete(
          result -> {
            synchronized (results) {
              results.add(result);
            }
            if (onAgentComplete != null) {
              onAgentComplete.accept(currentAgent, result);
            }
            future.complete(result);
          });

      if (onError != null) {
        stream.onError(onError);
      }

      stream.start();
    }

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              if (onAllComplete != null) {
                onAllComplete.accept(results);
              }
              return results;
            });
  }

  private CompletableFuture<AgentResult> startFirst() {
    List<Agent> agents = orchestrator.agents();
    List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
    AtomicBoolean firstCompleted = new AtomicBoolean(false);
    CompletableFuture<AgentResult> resultFuture = new CompletableFuture<>();

    for (Agent agent : agents) {
      AgentContext ctx = context.copy();
      CompletableFuture<AgentResult> future = new CompletableFuture<>();
      futures.add(future);

      AgentStream stream = agent.interactStream(ctx);

      if (onAgentTextDelta != null) {
        final Agent currentAgent = agent;
        stream.onTextDelta(delta -> onAgentTextDelta.accept(currentAgent, delta));
      }

      final Agent currentAgent = agent;
      stream.onComplete(
          result -> {
            if (onAgentComplete != null) {
              onAgentComplete.accept(currentAgent, result);
            }
            future.complete(result);

            if (firstCompleted.compareAndSet(false, true)) {
              if (onFirstComplete != null) {
                onFirstComplete.accept(result);
              }
              resultFuture.complete(result);
            }
          });

      if (onError != null) {
        stream.onError(onError);
      }

      stream.start();
    }

    return resultFuture;
  }

  private CompletableFuture<AgentResult> startSynthesize() {
    if (synthesizer == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Synthesizer is required for SYNTHESIZE mode"));
    }

    List<Agent> agents = orchestrator.agents();
    List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
    List<AgentResult> results = new ArrayList<>();

    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    // First phase: run all agents in parallel
    for (Agent agent : agents) {
      AgentContext ctx = context.copy();
      ctx.withTraceContext(parentTraceId, parentSpanId);

      CompletableFuture<AgentResult> future = new CompletableFuture<>();
      futures.add(future);

      AgentStream stream = agent.interactStream(ctx);

      if (onAgentTextDelta != null) {
        final Agent currentAgent = agent;
        stream.onTextDelta(delta -> onAgentTextDelta.accept(currentAgent, delta));
      }

      final Agent currentAgent = agent;
      stream.onComplete(
          result -> {
            synchronized (results) {
              results.add(result);
            }
            if (onAgentComplete != null) {
              onAgentComplete.accept(currentAgent, result);
            }
            future.complete(result);
          });

      if (onError != null) {
        stream.onError(onError);
      }

      stream.start();
    }

    // Second phase: synthesize results
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenCompose(
            v -> {
              // Build synthesis prompt
              String originalQuery = extractLastUserMessage();
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

              // Stream synthesizer
              AgentContext synthContext = AgentContext.create();
              synthContext.addInput(Message.user(synthesisPrompt.toString()));

              CompletableFuture<AgentResult> synthFuture = new CompletableFuture<>();
              AgentStream synthStream = synthesizer.interactStream(synthContext);

              if (onAgentTextDelta != null) {
                synthStream.onTextDelta(delta -> onAgentTextDelta.accept(synthesizer, delta));
              }

              synthStream.onComplete(
                  synthResult -> {
                    if (onSynthesisComplete != null) {
                      onSynthesisComplete.accept(synthResult);
                    }
                    synthFuture.complete(synthResult);
                  });

              if (onError != null) {
                synthStream.onError(onError);
              }

              synthStream.start();
              return synthFuture;
            });
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
}
