package com.paragon.benchmarks;

import com.paragon.agents.Agent;
import com.paragon.responses.Responder;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH Benchmarks for Operator (Agent) class.
 *
 * <p>Measures:
 *
 * <ul>
 *   <li>Instantiation time (builder + build)
 *   <li>Builder creation time
 *   <li>Fully configured agent instantiation
 *   <li>Memory allocation per instance
 *   <li>Comparison with minimal vs full configuration
 * </ul>
 *
 * <p>Run with: {@code mvn test-compile exec:java
 * -Dexec.mainClass="com.paragon.benchmarks.OperatorBenchmark"}
 *
 * <p>Or from IDE: Run the main() method
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"--enable-preview", "-Xms512m", "-Xmx512m"})
public class OperatorBenchmark {

  // Pre-created Responder to isolate Operator instantiation time
  private Responder sharedResponder;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(OperatorBenchmark.class.getSimpleName())
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-results/operator-benchmark.json")
            .build();

    new Runner(opt).run();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Minimal Operator Instantiation
  // ═══════════════════════════════════════════════════════════════════════════

  @Setup(Level.Trial)
  public void setup() {
    sharedResponder = Responder.builder().openRouter().apiKey("benchmark-test-key").build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Operator with Model Configuration
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Measures the time to create a minimal Operator (Agent) instance. This is the baseline for
   * comparison with other AI agent libraries like AGNO.
   */
  @Benchmark
  public void instantiateMinimalOperator(Blackhole bh) {
    Agent agent =
        Agent.builder()
            .name("MinimalAgent")
            .responder(sharedResponder)
            .model("openai/gpt-4o-mini")
            .instructions("You are a minimal agent.")
            .build();
    bh.consume(agent);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Fully Configured Operator
  // ═══════════════════════════════════════════════════════════════════════════

  /** Measures instantiation with model and basic configuration. */
  @Benchmark
  public void instantiateOperatorWithModel(Blackhole bh) {
    Agent agent =
        Agent.builder()
            .name("ModelAgent")
            .responder(sharedResponder)
            .model("openai/gpt-4o-mini")
            .instructions("You are a helpful assistant.")
            .temperature(0.7)
            .maxTurns(10)
            .build();
    bh.consume(agent);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Builder Creation Only
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Measures instantiation with full configuration (instructions, tools, metadata). This represents
   * a real-world production scenario.
   */
  @Benchmark
  public void instantiateFullyConfiguredOperator(Blackhole bh) {
    Agent agent =
        Agent.builder()
            .name("FullAgent")
            .responder(sharedResponder)
            .model("openai/gpt-4o-mini")
            .instructions("You are a helpful assistant.")
            .temperature(0.7)
            .maxTurns(20)
            .build();
    bh.consume(agent);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Builder Chain (without build)
  // ═══════════════════════════════════════════════════════════════════════════

  /** Measures just the builder creation overhead. */
  @Benchmark
  public void createBuilderOnly(Blackhole bh) {
    Agent.Builder builder = Agent.builder();
    bh.consume(builder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Memory Allocation Profiling
  // ═══════════════════════════════════════════════════════════════════════════

  /** Measures the fluent builder chain time (excluding final build). */
  @Benchmark
  public void builderChainWithoutBuild(Blackhole bh) {
    Agent.Builder builder =
        Agent.builder()
            .name("ChainAgent")
            .responder(sharedResponder)
            .model("openai/gpt-4o-mini")
            .instructions("You are helpful.")
            .temperature(0.7);
    bh.consume(builder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Batch Instantiation (100 agents)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Measures memory allocation for Operator creation. Use with -prof gc to see allocation rates.
   */
  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Measurement(iterations = 100)
  public void memoryAllocationProfile(Blackhole bh) {
    Agent agent =
        Agent.builder()
            .name("MemoryAgent")
            .responder(sharedResponder)
            .model("openai/gpt-4o-mini")
            .instructions("Memory profile agent.")
            .build();
    bh.consume(agent);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: End-to-End Agent Creation (including Responder)
  // ═══════════════════════════════════════════════════════════════════════════

  /** Measures time to create 100 Operator instances. Useful for multi-agent scenarios. */
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void batchInstantiation100(Blackhole bh) {
    for (int i = 0; i < 100; i++) {
      Agent agent =
          Agent.builder()
              .name("BatchAgent" + i)
              .responder(sharedResponder)
              .model("openai/gpt-4o-mini")
              .instructions("Agent " + i)
              .build();
      bh.consume(agent);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MAIN: Run Benchmarks
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Measures complete agent setup from scratch (Responder + Operator). This is the most realistic
   * comparison with other agent libraries.
   */
  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void endToEndAgentCreation(Blackhole bh) {
    // Create responder
    Responder responder = Responder.builder().openRouter().apiKey("benchmark-test-key").build();

    // Create operator/agent
    Agent agent =
        Agent.builder()
            .name("EndToEndAgent")
            .responder(responder)
            .model("openai/gpt-4o-mini")
            .instructions("You are a helpful assistant.")
            .temperature(0.7)
            .build();

    bh.consume(agent);
  }
}
