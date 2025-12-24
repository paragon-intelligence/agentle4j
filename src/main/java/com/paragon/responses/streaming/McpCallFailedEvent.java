package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/** Emitted when an MCP tool call has failed. */
public record McpCallFailedEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public McpCallFailedEvent {}
}
