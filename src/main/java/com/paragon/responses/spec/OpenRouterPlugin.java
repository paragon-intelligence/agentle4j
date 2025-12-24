package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "id")
@JsonSubTypes({
  @JsonSubTypes.Type(value = OpenRouterModerationPlugin.class, name = "moderation"),
  @JsonSubTypes.Type(value = OpenRouterWebPlugin.class, name = "web"),
  @JsonSubTypes.Type(value = OpenRouterFileParserPlugin.class, name = "file-parser"),
  @JsonSubTypes.Type(value = OpenRouterResponseHealingPlugin.class, name = "response-healing")
})
public sealed interface OpenRouterPlugin
    permits OpenRouterModerationPlugin,
        OpenRouterWebPlugin,
        OpenRouterFileParserPlugin,
        OpenRouterResponseHealingPlugin {}
