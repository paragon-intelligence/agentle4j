package com.paragon.broadcasting;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface Observation {
  @NonNull
  String getId();

  @NonNull
  String getTraceId();

  @Nullable
  String getParentObservationId(); // null if root observation

  @NonNull
  String getName();

  @NonNull
  ObservationType getType();

  long getStartTimeMs();

  long getEndTimeMs();

  // Input/Output
  @Nullable
  Object getInput();

  @Nullable
  Object getOutput();

  @NonNull
  List<MultiModalContent> getInputModalities();

  @NonNull
  List<MultiModalContent> getOutputModalities();

  // Metadata & Attributes
  @NonNull
  Map<String, Object> getMetadata();

  @NonNull
  Map<String, Object> getAttributes();

  // LLM-specific (for GENERATION type)
  @Nullable
  String getModel();

  @NonNull
  Map<String, Object> getModelParameters();

  @Nullable
  TokenUsage getTokenUsage();

  @Nullable
  CostDetails getCost();

  @Nullable
  Long getCompletionStartTimeMs();

  // Status
  @NonNull
  ObservationLevel getLevel();

  @Nullable
  String getStatusMessage();

  @NonNull
  ObservationStatus getStatus();

  // Versioning
  @Nullable
  String getVersion();

  @Nullable
  String getEnvironment();
}
