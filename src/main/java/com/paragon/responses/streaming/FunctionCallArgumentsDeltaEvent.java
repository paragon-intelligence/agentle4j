package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/** Emitted when there is a partial function-call arguments delta. */
public record FunctionCallArgumentsDeltaEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("delta") @NonNull String delta,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public FunctionCallArgumentsDeltaEvent {}
}
