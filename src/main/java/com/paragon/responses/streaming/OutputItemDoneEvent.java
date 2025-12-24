package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/** Emitted when an output item is marked done. */
public record OutputItemDoneEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("item") @NonNull Map<String, Object> item,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public OutputItemDoneEvent {}
}
