package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Emitted when there is an additional text delta. */
public record OutputTextDeltaEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("content_index") int contentIndex,
    @JsonProperty("delta") @NonNull String delta,
    @JsonProperty("logprobs") @Nullable List<Object> logprobs,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public OutputTextDeltaEvent {}
}
