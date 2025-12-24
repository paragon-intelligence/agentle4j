package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paragon.responses.spec.Response;
import org.jspecify.annotations.NonNull;

/** Emitted when a response is created. */
public record ResponseCreatedEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("response") @NonNull Response response,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public ResponseCreatedEvent {}
}
