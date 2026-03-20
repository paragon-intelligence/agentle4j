package com.paragon.harness;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * Writes {@link AgentRunReport} instances to a filesystem directory for later analysis.
 *
 * <p>Reports are written as pretty-printed JSON files named
 * {@code {agentName}_{reportId}.json}. They can be read back via {@link #loadAll()} and
 * fed to a meta-agent via {@link com.paragon.harness.tools.FailureAnalysisTool}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * RunReportExporter exporter = RunReportExporter.create(Path.of("./reports"));
 *
 * AgentRunReport report = AgentRunReport.from(agentName, result, startedAt, Instant.now(), 0, 0);
 * exporter.export(report);
 * }</pre>
 *
 * @since 1.0
 */
public final class RunReportExporter {

  private final Path reportDir;
  private final ObjectMapper objectMapper;

  private RunReportExporter(Path reportDir, ObjectMapper objectMapper) {
    this.reportDir = Objects.requireNonNull(reportDir, "reportDir cannot be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    try {
      Files.createDirectories(reportDir);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create report directory: " + reportDir, e);
    }
  }

  /**
   * Creates a RunReportExporter that writes JSON reports to the given directory.
   *
   * @param reportDir the directory to write reports to
   * @return a new exporter
   */
  public static @NonNull RunReportExporter create(@NonNull Path reportDir) {
    ObjectMapper mapper = new ObjectMapper();
    return new RunReportExporter(reportDir, mapper);
  }

  /**
   * Exports a report to disk.
   *
   * @param report the report to export
   * @return the path of the written file
   */
  public @NonNull Path export(@NonNull AgentRunReport report) {
    Objects.requireNonNull(report, "report cannot be null");
    String safeName = report.agentName().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    String filename = safeName + "_" + report.reportId() + ".json";
    Path reportFile = reportDir.resolve(filename);
    try {
      String json =
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(buildReportMap(report));
      Files.writeString(reportFile, json, StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write report: " + reportFile, e);
    }
    return reportFile;
  }

  /**
   * Loads all reports from the report directory.
   *
   * @return list of report summaries (as raw JSON strings)
   */
  public @NonNull List<String> loadAll() {
    if (!Files.exists(reportDir)) return List.of();
    try (var stream = Files.list(reportDir)) {
      List<Path> jsonFiles = stream
          .filter(p -> p.toString().endsWith(".json"))
          .sorted()
          .collect(Collectors.toList());
      List<String> reports = new ArrayList<>();
      for (Path file : jsonFiles) {
        try {
          reports.add(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
          // Skip unreadable files
        }
      }
      return reports;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list reports in: " + reportDir, e);
    }
  }

  /**
   * Returns the number of reports stored in the report directory.
   *
   * @return report count
   */
  public int count() {
    if (!Files.exists(reportDir)) return 0;
    try (var stream = Files.list(reportDir)) {
      return (int) stream.filter(p -> p.toString().endsWith(".json")).count();
    } catch (IOException e) {
      return 0;
    }
  }

  // ===== Private Helpers =====

  private java.util.Map<String, Object> buildReportMap(AgentRunReport report) {
    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
    map.put("reportId", report.reportId());
    map.put("agentName", report.agentName());
    map.put("startedAt", report.startedAt().toString());
    map.put("completedAt", report.completedAt().toString());
    map.put("totalDurationMs", report.totalDuration().toMillis());
    map.put("success", report.isSuccess());
    map.put("errorMessage", report.errorMessage());
    map.put("turnsUsed", report.turnsUsed());
    map.put("retryCount", report.retryCount());
    map.put("totalToolCalls", report.totalToolCalls());
    map.put("failedToolCalls", report.failedToolCalls());
    map.put("toolCallCounts", report.toolCallCounts());
    map.put("toolErrorCounts", report.toolErrorCounts());
    map.put("guardrailTriggerCount", report.guardrailTriggerCount());
    map.put("finalOutput", report.finalOutput());
    return map;
  }
}
