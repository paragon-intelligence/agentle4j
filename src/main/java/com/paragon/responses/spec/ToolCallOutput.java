package com.paragon.responses.spec;

public sealed interface ToolCallOutput
    permits ApplyPatchToolCallOutput,
        ComputerToolCallOutput,
        CustomToolCallOutput,
        FunctionShellToolCallOutput,
        FunctionToolCallOutput,
        LocalShellCallOutput,
        ShellCallOutput {}
