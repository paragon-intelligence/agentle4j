package com.paragon.responses.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents which tool the AI need to run. Only contains tool names and not args. Args are present
 * in xxxToolCall
 */
@JsonDeserialize(using = ToolChoiceDeserializer.class)
public sealed interface ToolChoice extends ToolChoiceRepresentable
    permits ToolChoiceMode, AllowedTools {}
