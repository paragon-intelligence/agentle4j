package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** The input format for the custom tool. Default is unconstrained text. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = CustomToolInputFormatText.class, name = "text"),
  @JsonSubTypes.Type(value = CustomToolInputFormatGrammar.class, name = "grammar")
})
public sealed interface CustomToolInputFormat
    permits CustomToolInputFormatText, CustomToolInputFormatGrammar {}
