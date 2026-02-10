package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

  private final AgentNetwork network;
  private final AgenticContext context;
  private final Mode mode;
  // Callbacks
  private BiConsumer<Interactable, String> onPeerTextDelta;
  private BiConsumer<Interactable, AgentResult> onPeerComplete;
  private Consumer<Integer> onRoundStart;
  private Consumer<List<AgentNetwork.Contribution>> onRoundComplete;
  private Consumer<String> onSynthesisTextDelta;
  private Consumer<AgentNetwork.NetworkResult> onComplete;
  private Consumer<Throwable> onError;
  NetworkStream(AgentNetwork network, AgenticContext context, Mode mode) {
    this.network = Objects.requireNonNull(network, "network cannot be null");
    this.context = Objects.requireNonNull(context, "context cannot be null");
    this.mode = Objects.requireNonNull(mode, "mode cannot be null");
  }

  /**
   * Called for each text delta from any peer.
   *
   * @param callback receives the peer and text chunk
   * @return this stream
   */
  public @NonNull NetworkStream onPeerTextDelta(@NonNull BiConsumer<Interactable, String> callback) {
    this.onPeerTextDelta = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when an individual peer completes its contribution.
   *
   * @param callback receives the peer and its result
   * @return this stream
   */
  public @NonNull NetworkStream onPeerComplete(@NonNull BiConsumer<Interactable, AgentResult> callback) {
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
   * Starts the streaming network execution. Blocks until completion.
   *
   * <p>On virtual threads, blocking is efficient and does not consume platform threads.
   *
   * @return the network result
   */
  public AgentNetwork.NetworkResult start() {
    try {
      return switch (mode) {
        case DISCUSS -> startDiscuss();
        case BROADCAST -> startBroadcast();
      };
    } catch (Exception ex) {
      if (onError != null) {
        onError.accept(ex);
      }
      throw new RuntimeException(ex);
    }
  }

  private AgentNetwork.NetworkResult startDiscuss() {
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();
    context.withTraceContext(parentTraceId, parentSpanId);

    String originalTopic = extractLastUserMessage();
    List<AgentNetwork.Contribution> allContributions = new ArrayList<>();

    // Run rounds sequentially (blocking)
    runStreamingRounds(allContributions, parentTraceId, parentSpanId);

    // Synthesize if configured
    Interactable synthesizer = network.getSynthesizer();
    if (synthesizer != null) {
      return synthesizeWithStreaming(allContributions, originalTopic, synthesizer);
    }

    AgentNetwork.NetworkResult result = new AgentNetwork.NetworkResult(allContributions, null);
    if (onComplete != null) {
      onComplete.accept(result);
    }
    return result;
  }

  private void runStreamingRounds(
          List<AgentNetwork.Contribution> allContributions,
          String parentTraceId,
          String parentSpanId) {

    List<Interactable> peers = network.peers();
    int maxRounds = network.maxRounds();

    for (int round = 1; round <= maxRounds; round++) {
      final int currentRound = round;
      List<AgentNetwork.Contribution> roundContributions = new ArrayList<>();

      // Start round callback
      if (onRoundStart != null) {
        onRoundStart.accept(currentRound);
      }

      // Process each peer sequentially within the round
      for (Interactable peer : peers) {
        AgenticContext peerContext = buildPeerContext(peer, currentRound);
        peerContext.withTraceContext(parentTraceId, parentSpanId);

        AgentStream stream = peer.interactStream(peerContext);

        if (onPeerTextDelta != null) {
          stream.onTextDelta(delta -> onPeerTextDelta.accept(peer, delta));
        }

        // Execute synchronously (blocking)
        AgentResult result = stream.start();

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
      }

      // End round callback
      if (onRoundComplete != null) {
        onRoundComplete.accept(new ArrayList<>(roundContributions));
      }
    }
  }

  private AgentNetwork.NetworkResult synthesizeWithStreaming(
          List<AgentNetwork.Contribution> contributions, String originalTopic, Interactable synthesizer) {

    StringBuilder synthPrompt = new StringBuilder();
    synthPrompt.append("Original discussion topic: ").append(originalTopic).append("\n\n");
    synthPrompt.append("The following contributions were made:\n\n");

    for (AgentNetwork.Contribution c : contributions) {
      synthPrompt.append("**").append(c.peer().name()).append("** (Round ").append(c.round());
      synthPrompt.append("): ");
      if (c.isError()) {
        synthPrompt.append("[Error occurred]\n");
      } else {
        synthPrompt.append(c.output()).append("\n");
      }
      synthPrompt.append("\n");
    }

    synthPrompt.append("Please synthesize these viewpoints into a coherent summary.");

    AgenticContext synthContext = AgenticContext.create();
    synthContext.addInput(Message.user(synthPrompt.toString()));

    AgentStream synthStream = synthesizer.interactStream(synthContext);

    if (onSynthesisTextDelta != null) {
      synthStream.onTextDelta(onSynthesisTextDelta);
    }

    // Execute synchronously (blocking)
    AgentResult synthResult = synthStream.start();

    String synthesis = synthResult.output();
    AgentNetwork.NetworkResult networkResult =
            new AgentNetwork.NetworkResult(contributions, synthesis);

    if (onComplete != null) {
      onComplete.accept(networkResult);
    }

    return networkResult;
  }

  private AgentNetwork.NetworkResult startBroadcast() {
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    List<Interactable> peers = network.peers();
    List<AgentNetwork.Contribution> contributions = new ArrayList<>();

    // Run all peers in parallel using virtual threads
    List<Thread> threads = new ArrayList<>();
    List<AgentNetwork.Contribution> syncContributions =
            java.util.Collections.synchronizedList(contributions);

    for (Interactable peer : peers) {
      Thread thread = Thread.startVirtualThread(() -> {
        AgenticContext peerContext = context.copy();
        peerContext.withTraceContext(parentTraceId, parentSpanId);

        AgentStream stream = peer.interactStream(peerContext);

        if (onPeerTextDelta != null) {
          stream.onTextDelta(delta -> onPeerTextDelta.accept(peer, delta));
        }

        AgentResult result = stream.start();

        AgentNetwork.Contribution contrib =
                new AgentNetwork.Contribution(peer, 1, result.output(), result.isError());
        syncContributions.add(contrib);

        if (onPeerComplete != null) {
          onPeerComplete.accept(peer, result);
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
        throw new RuntimeException("Broadcast interrupted", e);
      }
    }

    AgentNetwork.NetworkResult result = new AgentNetwork.NetworkResult(contributions, null);
    if (onComplete != null) {
      onComplete.accept(result);
    }
    return result;
  }

  private AgenticContext buildPeerContext(Interactable peer, int round) {
    AgenticContext peerContext = context.copy();

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

  /**
   * Execution mode for the network stream.
   */
  enum Mode {
    /**
     * Sequential peer contributions across rounds.
     */
    DISCUSS,
    /**
     * Parallel broadcast to all peers.
     */
    BROADCAST
  }
}
