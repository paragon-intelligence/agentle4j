package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/** Emitted when a partial code snippet is streamed by the code interpreter. */
public record CodeInterpreterCallCodeDeltaEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("delta") @NonNull String delta,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public CodeInterpreterCallCodeDeltaEvent {}
}
