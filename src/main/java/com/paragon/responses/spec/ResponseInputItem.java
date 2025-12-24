package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Text, image, or file inputs to the model, used to generate a response. */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = Message.class, name = "message"),
  @JsonSubTypes.Type(value = ItemReference.class, name = "item_reference")
})
public sealed interface ResponseInputItem permits Message, Item, ItemReference {}
