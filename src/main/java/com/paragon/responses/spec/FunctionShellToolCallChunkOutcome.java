package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** The exit or timeout outcome associated with this shell call. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = FunctionShellToolCallChunkTimeoutOutcome.class, name = "timeout"),
  @JsonSubTypes.Type(value = FunctionShellToolCallChunkExitOutcome.class, name = "exit")
})
public sealed interface FunctionShellToolCallChunkOutcome
    permits FunctionShellToolCallChunkTimeoutOutcome, FunctionShellToolCallChunkExitOutcome {}
