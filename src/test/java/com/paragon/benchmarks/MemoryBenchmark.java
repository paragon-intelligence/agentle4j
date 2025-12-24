package com.paragon.benchmarks;

import com.paragon.agents.Agent;
import com.paragon.responses.Responder;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Memory-focused benchmarks for Agentle classes.
 *
 * <p>Measures heap memory allocation for:
 *
 * <ul>
 *   <li>Responder instances
 *   <li>Operator (Agent) instances
 *   <li>Combined Responder + Operator setup
 * </ul>
 *
 * <p>Run with GC profiler: {@code java -jar benchmarks.jar -prof gc}
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
@Fork(
    value = 2,
    jvmArgs = {"--enable-preview", "-Xms256m", "-Xmx256m", "-XX:+UseG1GC"})
public class MemoryBenchmark {

  private Responder sharedResponder;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(MemoryBenchmark.class.getSimpleName())
            .addProfiler(GCProfiler.class)
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-results/memory-benchmark.json")
            .build();

    new Runner(opt).run();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY: Single Responder Allocation
  // ═══════════════════════════════════════════════════════════════════════════

  @Setup(Level.Trial)
  public void setup() {
    sharedResponder = Responder.builder().openRouter().apiKey("memory-benchmark-key").build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY: Single Operator Allocation
  // ═══════════════════════════════════════════════════════════════════════════

  @Benchmark
  public void singleResponderAllocation(Blackhole bh) {
    Responder responder = Responder.builder().openRouter().apiKey("memory-test-key").build();
    bh.consume(responder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY: Combined Responder + Operator
  // ═══════════════════════════════════════════════════════════════════════════

  @Benchmark
  public void singleOperatorAllocation(Blackhole bh) {
    Agent agent =
        Agent.builder()
            .responder(sharedResponder)
            .model("openai/gpt-4o-mini")
            .instructions("You are helpful.")
            .build();
    bh.consume(agent);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY: Batch Allocation (10 Operators)
  // ═══════════════════════════════════════════════════════════════════════════

  @Benchmark
  public void combinedAllocation(Blackhole bh) {
    Responder responder = Responder.builder().openRouter().apiKey("memory-test-key").build();

    Agent agent =
        Agent.builder()
            .responder(responder)
            .model("openai/gpt-4o-mini")
            .instructions("You are helpful.")
            .temperature(0.7)
            .build();

    bh.consume(agent);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY: Batch Allocation (100 Operators)
  // ═══════════════════════════════════════════════════════════════════════════

  @Benchmark
  public void batchAllocation10Operators(Blackhole bh) {
    Agent[] agents = new Agent[10];
    for (int i = 0; i < 10; i++) {
      agents[i] =
          Agent.builder()
              .responder(sharedResponder)
              .model("openai/gpt-4o-mini")
              .instructions("Agent " + i)
              .build();
    }
    bh.consume(agents);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MAIN: Run Memory Benchmarks with GC Profiler
  // ═══════════════════════════════════════════════════════════════════════════

  @Benchmark
  public void batchAllocation100Operators(Blackhole bh) {
    Agent[] agents = new Agent[100];
    for (int i = 0; i < 100; i++) {
      agents[i] =
          Agent.builder()
              .responder(sharedResponder)
              .model("openai/gpt-4o-mini")
              .instructions("Agent " + i)
              .build();
    }
    bh.consume(agents);
  }
}
