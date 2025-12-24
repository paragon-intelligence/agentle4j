package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record ShellCallOutput(
    @Nullable String callId,
    @Nullable String id,
    @Nullable Integer maxOutputLength,
    @Nullable List<FunctionShellToolCallOutputChunk> output,
    @Nullable String createdBy)
    implements ResponseOutput, ToolCallOutput {}
