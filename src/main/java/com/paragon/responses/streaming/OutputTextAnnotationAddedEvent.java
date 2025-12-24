package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/** Emitted when an annotation is added to output text content. */
public record OutputTextAnnotationAddedEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("content_index") int contentIndex,
    @JsonProperty("annotation_index") int annotationIndex,
    @JsonProperty("annotation") @NonNull Map<String, Object> annotation,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public OutputTextAnnotationAddedEvent {}
}
