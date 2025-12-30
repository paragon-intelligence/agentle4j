package com.paragon.broadcasting;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface Trace {
  @NonNull String getId();

  @NonNull String getName();

  long getStartTimeMs();

  long getEndTimeMs();

  @Nullable String getUserId();

  @Nullable String getSessionId();

  @Nullable String getVersion();

  @Nullable String getRelease();

  @Nullable String getEnvironment();

  @NonNull Map<String, Object> getMetadata();

  @NonNull List<String> getTags();

  @NonNull List<Observation> getObservations();

  boolean isPublic();
}
