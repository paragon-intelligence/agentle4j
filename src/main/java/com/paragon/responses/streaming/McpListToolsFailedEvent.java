package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/** Emitted when the attempt to list available MCP tools has failed. */
public record McpListToolsFailedEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public McpListToolsFailedEvent {}
}
