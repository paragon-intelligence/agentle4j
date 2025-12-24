package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paragon.responses.spec.Response;
import org.jspecify.annotations.NonNull;

/** Emitted when a response fails. */
public record ResponseFailedEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("response") @NonNull Response response,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public ResponseFailedEvent {}
}
