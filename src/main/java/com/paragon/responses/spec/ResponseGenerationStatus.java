package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResponseGenerationStatus {
  @JsonProperty("completed")
  COMPLETED,
  @JsonProperty("failed")
  FAILED,
  @JsonProperty("in_progress")
  IN_PROGRESS,
  @JsonProperty("cancelled")
  CANCELLED,
  @JsonProperty("queued")
  QUEUED,
  @JsonProperty("incomplete")
  INCOMPLETE
}
