package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/** Emitted when a partial image is available during image generation streaming. */
public record ImageGenerationCallPartialImageEvent(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("item_id") @NonNull String itemId,
    @JsonProperty("output_index") int outputIndex,
    @JsonProperty("partial_image_index") int partialImageIndex,
    @JsonProperty("partial_image_b64") @NonNull String partialImageB64,
    @JsonProperty("sequence_number") int sequenceNumber)
    implements StreamingEvent {

  @JsonCreator
  public ImageGenerationCallPartialImageEvent {}
}
