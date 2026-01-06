package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Streaming wrapper for AgentNetwork that provides event callbacks during network discussions.
 *
 * <pre>{@code
 * network.discussStream("Should AI be regulated?")
 *     .onPeerTextDelta((peer, delta) -> System.out.print("[" + peer.name() + "] " + delta))
 *     .onRoundStart(round -> System.out.println("=== Round " + round + " ==="))
 *     .onComplete(result -> System.out.println("Discussion finished!"))
 *     .start();
 * }</pre>
 *
 * @since 1.0
 */
public final class NetworkStream {

  /** Execution mode for the network stream. */
  enum Mode {
    /** Sequential peer contributions across rounds. */
    DISCUSS,
    /** Parallel broadcast to all peers. */
    BROADCAST
  }

  private final AgentNetwork network;
  private final AgentContext context;
  private final Mode mode;

  // Callbacks
  private BiConsumer<Agent, String> onPeerTextDelta;
  private BiConsumer<Agent, AgentResult> onPeerComplete;
  private Consumer<Integer> onRoundStart;
  private Consumer<List<AgentNetwork.Contribution>> onRoundComplete;
  private Consumer<String> onSynthesisTextDelta;
  private Consumer<AgentNetwork.NetworkResult> onComplete;
  private Consumer<Throwable> onError;

  NetworkStream(AgentNetwork network, AgentContext context, Mode mode) {
    this.network = Objects.requireNonNull(network, "network cannot be null");
    this.context = Objects.requireNonNull(context, "context cannot be null");
    this.mode = Objects.requireNonNull(mode, "mode cannot be null");
  }

  /**
   * Called for each text delta from any peer agent.
   *
   * @param callback receives the peer agent and text chunk
   * @return this stream
   */
  public @NonNull NetworkStream onPeerTextDelta(@NonNull BiConsumer<Agent, String> callback) {
    this.onPeerTextDelta = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when an individual peer agent completes its contribution.
   *
   * @param callback receives the peer agent and its result
   * @return this stream
   */
  public @NonNull NetworkStream onPeerComplete(@NonNull BiConsumer<Agent, AgentResult> callback) {
    this.onPeerComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when a new discussion round begins.
   *
   * @param callback receives the round number (1-indexed)
   * @return this stream
   */
  public @NonNull NetworkStream onRoundStart(@NonNull Consumer<Integer> callback) {
    this.onRoundStart = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when a discussion round completes.
   *
   * @param callback receives the contributions from that round
   * @return this stream
   */
  public @NonNull NetworkStream onRoundComplete(
      @NonNull Consumer<List<AgentNetwork.Contribution>> callback) {
    this.onRoundComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called for each text delta from the synthesizer agent (if configured).
   *
   * @param callback receives the text chunk
   * @return this stream
   */
  public @NonNull NetworkStream onSynthesisTextDelta(@NonNull Consumer<String> callback) {
    this.onSynthesisTextDelta = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when the network discussion completes.
   *
   * @param callback receives the final network result
   * @return this stream
   */
  public @NonNull NetworkStream onComplete(@NonNull Consumer<AgentNetwork.NetworkResult> callback) {
    this.onComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when an error occurs.
   *
   * @param callback receives the error
   * @return this stream
   */
  public @NonNull NetworkStream onError(@NonNull Consumer<Throwable> callback) {
    this.onError = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Starts the streaming network execution.
   *
   * @return future completing with the network result
   */
  public @NonNull CompletableFuture<AgentNetwork.NetworkResult> start() {
    return switch (mode) {
      case DISCUSS -> startDiscuss();
      case BROADCAST -> startBroadcast();
    };
  }

  private CompletableFuture<AgentNetwork.NetworkResult> startDiscuss() {
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();
    context.withTraceContext(parentTraceId, parentSpanId);

    String originalTopic = extractLastUserMessage();
    List<AgentNetwork.Contribution> allContributions =
        Collections.synchronizedList(new ArrayList<>());

    return runStreamingRounds(allContributions, parentTraceId, parentSpanId)
        .thenCompose(
            contributions -> {
              Agent synthesizer = network.getSynthesizer();
              if (synthesizer != null) {
                return synthesizeWithStreaming(contributions, originalTopic, synthesizer);
              }
              AgentNetwork.NetworkResult result =
                  new AgentNetwork.NetworkResult(contributions, null);
              if (onComplete != null) {
                onComplete.accept(result);
              }
              return CompletableFuture.completedFuture(result);
            })
        .exceptionally(
            ex -> {
              if (onError != null) {
                onError.accept(ex);
              }
              throw new RuntimeException(ex);
            });
  }

  private CompletableFuture<List<AgentNetwork.Contribution>> runStreamingRounds(
      List<AgentNetwork.Contribution> allContributions,
      String parentTraceId,
      String parentSpanId) {

    List<Agent> peers = network.peers();
    int maxRounds = network.maxRounds();

    // Build a chain of futures for sequential round execution
    CompletableFuture<Void> roundChain = CompletableFuture.completedFuture(null);

    for (int round = 1; round <= maxRounds; round++) {
      final int currentRound = round;
      List<AgentNetwork.Contribution> roundContributions =
          Collections.synchronizedList(new ArrayList<>());

      // Start round callback
      roundChain =
          roundChain.thenRun(
              () -> {
                if (onRoundStart != null) {
                  onRoundStart.accept(currentRound);
                }
              });

      // Process each peer sequentially within the round
      for (Agent peer : peers) {
        roundChain =
            roundChain.thenCompose(
                v -> {
                  AgentContext peerContext = buildPeerContext(peer, currentRound);
                  peerContext.withTraceContext(parentTraceId, parentSpanId);

                  CompletableFuture<AgentResult> peerFuture = new CompletableFuture<>();
                  AgentStream stream = peer.interactStream(peerContext);

                  if (onPeerTextDelta != null) {
                    stream.onTextDelta(delta -> onPeerTextDelta.accept(peer, delta));
                  }

                  stream.onComplete(
                      result -> {
                        String output = result.output();
                        boolean isError = result.isError();

                        // Add contribution to shared context for next peers
                        if (!isError && output != null) {
                          Message contribution =
                              Message.assistant(Text.valueOf("[" + peer.name() + "]: " + output));
                          context.addInput(contribution);
                        }

                        AgentNetwork.Contribution contrib =
                            new AgentNetwork.Contribution(peer, currentRound, output, isError);
                        allContributions.add(contrib);
                        roundContributions.add(contrib);

                        if (onPeerComplete != null) {
                          onPeerComplete.accept(peer, result);
                        }

                        peerFuture.complete(result);
                      });

                  if (onError != null) {
                    stream.onError(
                        ex -> {
                          onError.accept(ex);
                          peerFuture.completeExceptionally(ex);
                        });
                  }

                  stream.start();
                  return peerFuture.thenApply(r -> null);
                });
      }

      // End round callback
      final List<AgentNetwork.Contribution> finalRoundContributions = roundContributions;
      roundChain =
          roundChain.thenRun(
              () -> {
                if (onRoundComplete != null) {
                  onRoundComplete.accept(new ArrayList<>(finalRoundContributions));
                }
              });
    }

    return roundChain.thenApply(v -> new ArrayList<>(allContributions));
  }

  private CompletableFuture<AgentNetwork.NetworkResult> synthesizeWithStreaming(
      List<AgentNetwork.Contribution> contributions, String originalTopic, Agent synthesizer) {

    StringBuilder synthPrompt = new StringBuilder();
    synthPrompt.append("Original discussion topic: ").append(originalTopic).append("\n\n");
    synthPrompt.append("The following contributions were made:\n\n");

    for (AgentNetwork.Contribution c : contributions) {
      synthPrompt.append("**").append(c.agent().name()).append("** (Round ").append(c.round());
      synthPrompt.append("): ");
      if (c.isError()) {
        synthPrompt.append("[Error occurred]\n");
      } else {
        synthPrompt.append(c.output()).append("\n");
      }
      synthPrompt.append("\n");
    }

    synthPrompt.append("Please synthesize these viewpoints into a coherent summary.");

    AgentContext synthContext = AgentContext.create();
    synthContext.addInput(Message.user(synthPrompt.toString()));

    CompletableFuture<AgentNetwork.NetworkResult> resultFuture = new CompletableFuture<>();
    StringBuilder synthesisOutput = new StringBuilder();

    AgentStream synthStream = synthesizer.interactStream(synthContext);

    if (onSynthesisTextDelta != null) {
      synthStream.onTextDelta(
          delta -> {
            synthesisOutput.append(delta);
            onSynthesisTextDelta.accept(delta);
          });
    } else {
      synthStream.onTextDelta(synthesisOutput::append);
    }

    synthStream.onComplete(
        synthResult -> {
          String synthesis = synthResult.output();
          AgentNetwork.NetworkResult networkResult =
              new AgentNetwork.NetworkResult(contributions, synthesis);
          if (onComplete != null) {
            onComplete.accept(networkResult);
          }
          resultFuture.complete(networkResult);
        });

    if (onError != null) {
      synthStream.onError(
          ex -> {
            onError.accept(ex);
            resultFuture.completeExceptionally(ex);
          });
    }

    synthStream.start();
    return resultFuture;
  }

  private CompletableFuture<AgentNetwork.NetworkResult> startBroadcast() {
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    List<Agent> peers = network.peers();
    List<CompletableFuture<AgentNetwork.Contribution>> futures = new ArrayList<>();

    for (Agent peer : peers) {
      AgentContext peerContext = context.copy();
      peerContext.withTraceContext(parentTraceId, parentSpanId);

      CompletableFuture<AgentNetwork.Contribution> future = new CompletableFuture<>();
      futures.add(future);

      AgentStream stream = peer.interactStream(peerContext);

      if (onPeerTextDelta != null) {
        stream.onTextDelta(delta -> onPeerTextDelta.accept(peer, delta));
      }

      stream.onComplete(
          result -> {
            AgentNetwork.Contribution contrib =
                new AgentNetwork.Contribution(peer, 1, result.output(), result.isError());
            if (onPeerComplete != null) {
              onPeerComplete.accept(peer, result);
            }
            future.complete(contrib);
          });

      if (onError != null) {
        stream.onError(
            ex -> {
              onError.accept(ex);
              future.completeExceptionally(ex);
            });
      }

      stream.start();
    }

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              List<AgentNetwork.Contribution> contributions = new ArrayList<>();
              for (CompletableFuture<AgentNetwork.Contribution> f : futures) {
                contributions.add(f.join());
              }
              AgentNetwork.NetworkResult result =
                  new AgentNetwork.NetworkResult(contributions, null);
              if (onComplete != null) {
                onComplete.accept(result);
              }
              return result;
            })
        .exceptionally(
            ex -> {
              if (onError != null) {
                onError.accept(ex);
              }
              throw new RuntimeException(ex);
            });
  }

  private AgentContext buildPeerContext(Agent peer, int round) {
    AgentContext peerContext = context.copy();

    String roleReminder =
        String.format(
            "You are %s participating in round %d of a discussion. "
                + "Consider the previous contributions and add your unique perspective. "
                + "Be constructive and build on others' ideas.",
            peer.name(), round);

    peerContext.addInput(Message.developer(Text.valueOf(roleReminder)));
    return peerContext;
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
    return "[No topic provided]";
  }
}
