package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/** Emitted when a reasoning summary part is completed. */
public record ReasoningSummaryPartDoneEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("summary_index") int summaryIndex,
    @JsonProperty("part") @NonNull Map<String, Object> part,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public ReasoningSummaryPartDoneEvent {}
}
