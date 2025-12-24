package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ApplyPatchToolCall.class, name = "apply_patch_call"),
  @JsonSubTypes.Type(value = CodeInterpreterToolCall.class, name = "code_interpreter_call"),
  @JsonSubTypes.Type(value = ComputerToolCall.class, name = "computer_call"),
  @JsonSubTypes.Type(value = CustomToolCall.class, name = "custom_tool_call"),
  @JsonSubTypes.Type(value = FileSearchToolCall.class, name = "file_search_call"),
  @JsonSubTypes.Type(value = FunctionShellToolCall.class, name = "function_shell_call"),
  @JsonSubTypes.Type(value = FunctionToolCall.class, name = "function_call"),
  @JsonSubTypes.Type(value = ImageGenerationCall.class, name = "image_generation_call"),
  @JsonSubTypes.Type(value = LocalShellCall.class, name = "local_shell_call"),
  @JsonSubTypes.Type(value = McpToolCall.class, name = "mcp_call"),
  @JsonSubTypes.Type(value = WebSearchToolCall.class, name = "web_search_call")
})
public abstract sealed class ToolCall
    permits ApplyPatchToolCall,
        CodeInterpreterToolCall,
        ComputerToolCall,
        CustomToolCall,
        FileSearchToolCall,
        FunctionShellToolCall,
        FunctionToolCall,
        ImageGenerationCall,
        LocalShellCall,
        McpToolCall,
        WebSearchToolCall {

  protected final String id;

  protected ToolCall(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
