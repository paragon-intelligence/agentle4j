package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/** Emitted when the system is in the process of retrieving the list of available MCP tools. */
public record McpListToolsInProgressEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public McpListToolsInProgressEvent {}
}
