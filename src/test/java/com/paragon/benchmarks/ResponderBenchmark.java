package com.paragon.benchmarks;

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
 * JMH Benchmarks for Responder class.
 *
 * <p>Measures:
 *
 * <ul>
 *   <li>Instantiation time (builder + build)
 *   <li>Builder creation time
 *   <li>Memory allocation per instance
 *   <li>Throughput (instances per second)
 * </ul>
 *
 * <p>Run with: {@code mvn test-compile exec:java
 * -Dexec.mainClass="com.paragon.benchmarks.ResponderBenchmark"}
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
public class ResponderBenchmark {

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Minimal Responder Instantiation
  // ═══════════════════════════════════════════════════════════════════════════

  private static final okhttp3.OkHttpClient SHARED_CLIENT = new okhttp3.OkHttpClient();

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Full Responder Instantiation (OpenAI)
  // ═══════════════════════════════════════════════════════════════════════════

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(ResponderBenchmark.class.getSimpleName())
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-results/responder-benchmark.json")
            .build();

    new Runner(opt).run();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Builder Creation Only
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Measures the time to create a minimal Responder instance. This is the baseline for comparison
   * with other AI libraries.
   */
  @Benchmark
  public void instantiateMinimalResponder(Blackhole bh) {
    Responder responder = Responder.builder().openRouter().apiKey("benchmark-test-key").build();
    bh.consume(responder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Builder Chain (without build)
  // ═══════════════════════════════════════════════════════════════════════════

  /** Measures the time to create a Responder with OpenAI provider. */
  @Benchmark
  public void instantiateOpenAiResponder(Blackhole bh) {
    Responder responder = Responder.builder().openAi().apiKey("benchmark-test-key").build();
    bh.consume(responder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Memory Allocation Profiling
  // ═══════════════════════════════════════════════════════════════════════════

  /** Measures just the builder creation overhead. */
  @Benchmark
  public void createBuilderOnly(Blackhole bh) {
    Responder.Builder builder = Responder.builder();
    bh.consume(builder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Batch Instantiation (100 instances)
  // ═══════════════════════════════════════════════════════════════════════════

  /** Measures the fluent builder chain time (excluding final build). */
  @Benchmark
  public void builderChainWithoutBuild(Blackhole bh) {
    Responder.Builder builder = Responder.builder().openRouter().apiKey("benchmark-test-key");
    bh.consume(builder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BENCHMARK: Shared HttpClient Reuse
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Measures memory allocation for Responder creation. Use with -prof gc to see allocation rates.
   */
  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Measurement(iterations = 100)
  public void memoryAllocationProfile(Blackhole bh) {
    Responder responder = Responder.builder().openRouter().apiKey("benchmark-test-key").build();
    bh.consume(responder);
  }

  /** Measures time to create 100 Responder instances. Useful for understanding scaling behavior. */
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void batchInstantiation100(Blackhole bh) {
    for (int i = 0; i < 100; i++) {
      Responder responder =
          Responder.builder().openRouter().apiKey("benchmark-test-key-" + i).build();
      bh.consume(responder);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MAIN: Run Benchmarks
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Measures instantiation time when reusing a shared HttpClient. This is the recommended
   * production pattern.
   */
  @Benchmark
  public void instantiateWithSharedHttpClient(Blackhole bh) {
    Responder responder =
        Responder.builder()
            .openRouter()
            .apiKey("benchmark-test-key")
            .httpClient(SHARED_CLIENT)
            .build();
    bh.consume(responder);
  }
}
