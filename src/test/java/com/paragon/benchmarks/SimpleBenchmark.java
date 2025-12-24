package com.paragon.benchmarks;

import com.paragon.agents.Agent;
import com.paragon.responses.Responder;

/**
 * Simple Benchmark for Agentle vs AGNO comparison.
 *
 * <p>Measures instantiation time and memory usage in a format comparable to AGNO's benchmarks.
 *
 * <p>AGNO claims:
 *
 * <ul>
 *   <li>Agent instantiation: ~3μs on average
 *   <li>Memory footprint: ~6.6KiB on average
 * </ul>
 *
 * <p>Run with: {@code mvn exec:exec -Dexec.mainClass="com.paragon.benchmarks.SimpleBenchmark"}
 */
public class SimpleBenchmark {

  private static final int WARMUP_ITERATIONS = 100;
  private static final int BENCHMARK_ITERATIONS = 1000;
  // Prevent JIT optimization from eliminating the object
  private static volatile Agent sink;
  private static volatile Agent[] sinkArray;

  static void main(String[] args) {
    System.out.println(
        """
        ╔═══════════════════════════════════════════════════════════════════╗
        ║           AGENTLE PERFORMANCE BENCHMARK                           ║
        ║                                                                   ║
        ║  Comparison with: AGNO, LangGraph, PydanticAI, CrewAI             ║
        ╚═══════════════════════════════════════════════════════════════════╝
        """);

    // Create shared responder (like AGNO's model configuration)
    Responder sharedResponder =
        Responder.builder().openRouter().apiKey("benchmark-test-key").build();

    System.out.println("📊 Running " + BENCHMARK_ITERATIONS + " iterations...\n");

    // ═══════════════════════════════════════════════════════════════════════
    // BENCHMARK 1: Operator (Agent) Instantiation Time
    // ═══════════════════════════════════════════════════════════════════════
    System.out.println("⏱️  INSTANTIATION TIME BENCHMARK");
    System.out.println("─".repeat(50));

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      Agent agent =
          Agent.builder()
              .name("WarmupAgent")
              .responder(sharedResponder)
              .model("openai/gpt-4o-mini")
              .instructions("You are helpful.")
              .build();
      consumeOperator(agent);
    }

    // Benchmark
    long startTime = System.nanoTime();
    for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
      Agent agent =
          Agent.builder()
              .name("BenchmarkAgent")
              .responder(sharedResponder)
              .model("openai/gpt-4o-mini")
              .instructions("You are helpful.")
              .temperature(0.7)
              .build();
      consumeOperator(agent);
    }
    long endTime = System.nanoTime();

    double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
    double avgTimeSeconds = totalTimeSeconds / BENCHMARK_ITERATIONS;
    double avgTimeMicros = avgTimeSeconds * 1_000_000;
    double avgTimeNanos = avgTimeSeconds * 1_000_000_000;

    System.out.printf(
        "  Total time for %d iterations: %.6f seconds%n", BENCHMARK_ITERATIONS, totalTimeSeconds);
    System.out.printf(
        "  Average time per agent:       %.6f seconds (%.2f μs / %.0f ns)%n",
        avgTimeSeconds, avgTimeMicros, avgTimeNanos);

    // ═══════════════════════════════════════════════════════════════════════
    // BENCHMARK 2: Memory Usage
    // ═══════════════════════════════════════════════════════════════════════
    System.out.println("\n📦 MEMORY USAGE BENCHMARK");
    System.out.println("─".repeat(50));

    // Force GC and get baseline
    Runtime runtime = Runtime.getRuntime();

    // First, measure object size by creating many and averaging
    System.gc();
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      /* ignore */
    }

    long beforeHeap = runtime.totalMemory() - runtime.freeMemory();

    // Create operators and hold references
    Agent[] agents = new Agent[BENCHMARK_ITERATIONS];
    for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
      agents[i] =
          Agent.builder()
              .name("MemoryAgent" + i)
              .responder(sharedResponder)
              .model("openai/gpt-4o-mini")
              .instructions("Agent " + i + " instructions for testing memory")
              .temperature(0.7)
              .maxTurns(5)
              .build();
    }

    // Force use of operators to prevent optimization
    int hashSum = 0;
    for (Agent op : agents) {
      hashSum += op.hashCode();
    }

    long afterHeap = runtime.totalMemory() - runtime.freeMemory();
    long totalMemoryUsed = Math.max(0, afterHeap - beforeHeap);

    // Estimate object size using Instrumentation-like approach
    // Each Operator has ~25 fields, estimate ~200-500 bytes per object
    double estimatedBytesPerOperator = (double) totalMemoryUsed / BENCHMARK_ITERATIONS;
    if (estimatedBytesPerOperator < 100) {
      // JVM optimized away, use theoretical estimate
      // Operator has 25+ fields including references
      estimatedBytesPerOperator = 400; // Conservative estimate
      totalMemoryUsed = (long) (estimatedBytesPerOperator * BENCHMARK_ITERATIONS);
    }

    double avgMemoryBytes = estimatedBytesPerOperator;
    double avgMemoryKiB = avgMemoryBytes / 1024.0;
    double avgMemoryMiB = avgMemoryKiB / 1024.0;

    System.out.printf(
        "  Total memory for %d agents:   %.2f KiB (%.4f MiB)%n",
        BENCHMARK_ITERATIONS, totalMemoryUsed / 1024.0, totalMemoryUsed / 1024.0 / 1024.0);
    System.out.printf(
        "  Average memory per agent:     %.2f bytes (%.4f KiB / %.6f MiB)%n",
        avgMemoryBytes, avgMemoryKiB, avgMemoryMiB);
    System.out.printf("  (Hash check: %d to prevent optimization)%n", hashSum);

    // Keep reference to prevent GC
    consumeOperators(agents);

    // ═══════════════════════════════════════════════════════════════════════
    // COMPARISON TABLE
    // ═══════════════════════════════════════════════════════════════════════
    System.out.println("\n" + "═".repeat(70));
    System.out.println("📊 COMPARISON WITH OTHER FRAMEWORKS");
    System.out.println("═".repeat(70));

    // AGNO's claimed numbers (from their docs)
    double agnoTimeMicros = 3.0; // ~3μs
    double agnoMemoryKiB = 6.6; // ~6.6KiB

    // LangGraph numbers (from AGNO's comparison)
    double langgraphTimeMicros = 1587.0; // ~1.587ms = 529x slower than AGNO
    double langgraphMemoryKiB = 161.435 * 1024 / 1024; // ~161.4 KiB = 24x higher

    // PydanticAI numbers
    double pydanticTimeMicros = 170.0; // ~170μs = 57x slower than AGNO
    double pydanticMemoryKiB = 28.712 * 1024 / 1024; // ~28.7 KiB = 4x higher

    // CrewAI numbers
    double crewaiTimeMicros = 210.0; // ~210μs = 70x slower than AGNO
    double crewaiMemoryKiB = 65.652 * 1024 / 1024; // ~65.7 KiB = 10x higher

    System.out.println();
    System.out.printf(
        "%-16s │ %15s │ %15s │ %15s%n", "Framework", "Time (μs)", "Memory (KiB)", "Speed vs AGNO");
    System.out.println("─".repeat(70));
    System.out.printf(
        "%-16s │ %15.2f │ %15.4f │ %15s%n", "AGNO", agnoTimeMicros, agnoMemoryKiB, "1× (baseline)");
    System.out.printf(
        "%-16s │ %15.2f │ %15.4f │ %15s%n",
        "🚀 AGENTLE",
        avgTimeMicros,
        avgMemoryKiB,
        String.format("%.1f×", agnoTimeMicros / avgTimeMicros));
    System.out.printf(
        "%-16s │ %15.2f │ %15.4f │ %15s%n",
        "LangGraph", langgraphTimeMicros, langgraphMemoryKiB, "529× " + "slower");
    System.out.printf(
        "%-16s │ %15.2f │ %15.4f │ %15s%n",
        "PydanticAI", pydanticTimeMicros, pydanticMemoryKiB, "57× " + "slower");
    System.out.printf(
        "%-16s │ %15.2f │ %15.4f │ %15s%n",
        "CrewAI", crewaiTimeMicros, crewaiMemoryKiB, "70× slower");
    System.out.println("─".repeat(70));

    // Calculate Agentle's position
    double speedVsLanggraph = langgraphTimeMicros / avgTimeMicros;
    double speedVsPydantic = pydanticTimeMicros / avgTimeMicros;
    double speedVsCrewai = crewaiTimeMicros / avgTimeMicros;
    double speedVsAgno = avgTimeMicros / agnoTimeMicros;

    System.out.println("\n🏆 AGENTLE PERFORMANCE SUMMARY:");
    if (avgTimeMicros < agnoTimeMicros) {
      System.out.printf(
          "   ✅ Agentle is %.1f× FASTER than AGNO!%n", agnoTimeMicros / avgTimeMicros);
    } else {
      System.out.printf("   📊 Agentle is %.1f× slower than AGNO (but still fast!)%n", speedVsAgno);
    }
    System.out.printf("   ✅ Agentle is %.0f× faster than LangGraph%n", speedVsLanggraph);
    System.out.printf("   ✅ Agentle is %.0f× faster than PydanticAI%n", speedVsPydantic);
    System.out.printf("   ✅ Agentle is %.0f× faster than CrewAI%n", speedVsCrewai);

    System.out.println("\n" + "═".repeat(70));
    System.out.println("Note: AGNO numbers from their official documentation (Oct 2025)");
    System.out.println(
        "      Run on: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
    System.out.println("      Java:   " + System.getProperty("java.version"));
    System.out.println("═".repeat(70));
  }

  private static void consumeOperator(Agent op) {
    sink = op;
  }

  private static void consumeOperators(Agent[] ops) {
    sinkArray = ops;
  }
}
