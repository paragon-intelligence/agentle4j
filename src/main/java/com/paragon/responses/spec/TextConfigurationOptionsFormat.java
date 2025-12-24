package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = TextConfigurationOptionsJsonSchemaFormat.class, name = "json_schema"),
  @JsonSubTypes.Type(value = TextConfigurationOptionsTextFormat.class, name = "text")
})
public sealed interface TextConfigurationOptionsFormat
    permits TextConfigurationOptionsJsonSchemaFormat, TextConfigurationOptionsTextFormat {}
