package com.paragon.benchmarks;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Benchmark Runner for all Agentle benchmarks.
 *
 * <p>This class runs all benchmarks and generates a comprehensive report. Use this for competition
 * comparison with other AI libraries like AGNO.
 *
 * <h2>How to Run</h2>
 *
 * <h3>Option 1: From IDE</h3>
 *
 * <pre>
 * Run this main() method directly from your IDE
 * </pre>
 *
 * <h3>Option 2: From Command Line</h3>
 *
 * <pre>
 * # Compile the project
 * mvn clean test-compile
 *
 * # Run all benchmarks
 * mvn exec:java -Dexec.mainClass="com.paragon.benchmarks.BenchmarkRunner"
 * </pre>
 *
 * <h3>Option 3: Build and run JAR</h3>
 *
 * <pre>
 * mvn clean package -DskipTests
 * java --enable-preview -jar target/benchmarks.jar
 * </pre>
 *
 * <h2>Output</h2>
 *
 * Results are saved to: {@code benchmark-results/} directory in JSON format.
 *
 * <h2>Benchmark Categories</h2>
 *
 * <ul>
 *   <li><b>Responder</b>: HTTP client wrapper instantiation
 *   <li><b>Operator</b>: Agent class instantiation
 *   <li><b>Memory</b>: Heap allocation profiling
 * </ul>
 */
public class BenchmarkRunner {

  public static void main(String[] args) throws RunnerException {
    System.out.println(
        """
        ╔═══════════════════════════════════════════════════════════════╗
        ║              AGENTLE BENCHMARK SUITE                          ║
        ║                                                               ║
        ║  Measuring: Instantiation Time & Memory Usage                 ║
        ║  Comparison target: AGNO and other AI libraries               ║
        ╚═══════════════════════════════════════════════════════════════╝
        """);

    // Create results directory
    java.io.File resultsDir = new java.io.File("benchmark-results");
    if (!resultsDir.exists()) {
      resultsDir.mkdirs();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RUN: Quick Benchmark (for development/CI)
    // ═══════════════════════════════════════════════════════════════════════
    if (args.length > 0 && args[0].equals("--quick")) {
      runQuickBenchmarks();
      return;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RUN: Full Benchmark Suite
    // ═══════════════════════════════════════════════════════════════════════
    runFullBenchmarks();
  }

  /**
   * Quick benchmark for development and CI pipelines. Runs fewer iterations with smaller warmup.
   */
  private static void runQuickBenchmarks() throws RunnerException {
    System.out.println("🚀 Running QUICK benchmarks (development mode)...\n");

    Options opt =
        new OptionsBuilder()
            .include(".*Benchmark.*")
            .warmupIterations(1)
            .warmupTime(TimeValue.milliseconds(500))
            .measurementIterations(3)
            .measurementTime(TimeValue.milliseconds(500))
            .forks(1)
            .jvmArgs("--enable-preview")
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-results/quick-benchmark.json")
            .build();

    new Runner(opt).run();

    System.out.println(
        "\n✅ Quick benchmarks complete! Results: benchmark-results/quick-benchmark.json");
  }

  /**
   * Full benchmark suite for production comparison. Uses recommended JMH settings for accurate
   * measurements.
   */
  private static void runFullBenchmarks() throws RunnerException {
    System.out.println("🔬 Running FULL benchmark suite...\n");

    // Run time-based benchmarks
    System.out.println("📊 Phase 1: Instantiation Time Benchmarks");
    Options timingOpt =
        new OptionsBuilder()
            .include(ResponderBenchmark.class.getSimpleName())
            .include(OperatorBenchmark.class.getSimpleName())
            .warmupIterations(3)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(5)
            .measurementTime(TimeValue.seconds(1))
            .forks(2)
            .jvmArgs("--enable-preview", "-Xms512m", "-Xmx512m")
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-results/timing-benchmark.json")
            .build();

    new Runner(timingOpt).run();

    // Run memory benchmarks with GC profiler
    System.out.println("\n📊 Phase 2: Memory Allocation Benchmarks");
    Options memoryOpt =
        new OptionsBuilder()
            .include(MemoryBenchmark.class.getSimpleName())
            .warmupIterations(5)
            .measurementIterations(20)
            .forks(2)
            .jvmArgs("--enable-preview", "-Xms256m", "-Xmx256m", "-XX:+UseG1GC")
            .addProfiler(GCProfiler.class)
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-results/memory-benchmark.json")
            .build();

    new Runner(memoryOpt).run();

    System.out.println(
        """

        ╔═══════════════════════════════════════════════════════════════╗
        ║  ✅ BENCHMARK COMPLETE                                        ║
        ╠═══════════════════════════════════════════════════════════════╣
        ║  Results saved to:                                            ║
        ║    • benchmark-results/timing-benchmark.json                 ║
        ║    • benchmark-results/memory-benchmark.json                 ║
        ║                                                               ║
        ║  View results: https://jmh.morethan.io/                       ║
        ╚═══════════════════════════════════════════════════════════════╝
        """);
  }
}
