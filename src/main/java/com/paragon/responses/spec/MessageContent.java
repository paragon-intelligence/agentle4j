package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Text.class, name = "input_text"),
  @JsonSubTypes.Type(value = Image.class, name = "input_image"),
  @JsonSubTypes.Type(value = File.class, name = "input_file"),
  // Output variants - API uses different type names for output messages
  @JsonSubTypes.Type(value = Text.class, name = "output_text"),
  @JsonSubTypes.Type(value = Image.class, name = "output_image"),
  @JsonSubTypes.Type(value = File.class, name = "output_file")
})
public sealed interface MessageContent permits Text, Image, File {}
