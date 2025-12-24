package com.paragon.responses.spec;

/** Indicates that the shell call exceeded its configured time limit. */
public record FunctionShellToolCallChunkTimeoutOutcome()
    implements FunctionShellToolCallChunkOutcome {}
