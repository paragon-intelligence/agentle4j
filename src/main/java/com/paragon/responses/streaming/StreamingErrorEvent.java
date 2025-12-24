package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Emitted when an error occurs during streaming. */
public record StreamingErrorEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("code") @Nullable String code,
    @JsonProperty("message") @Nullable String message,
    @JsonProperty("param") @Nullable String param,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public StreamingErrorEvent {}
}
