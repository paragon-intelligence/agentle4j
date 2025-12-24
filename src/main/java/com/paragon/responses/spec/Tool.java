package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;

/**
 * Represents a tool that can be called by a Responder.
 *
 * @param <P> The parameters the tool accepts.
 * @param <R> The return of the tool call.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  // FunctionToolDefinition is listed first for deserialization (concrete class)
  @JsonSubTypes.Type(value = FunctionToolDefinition.class, name = "function"),
  @JsonSubTypes.Type(value = FileSearchTool.class, name = "file_search"),
  @JsonSubTypes.Type(value = CodeInterpreterTool.class, name = "code_interpreter"),
  @JsonSubTypes.Type(value = ComputerUseTool.class, name = "computer_use_preview"),
  @JsonSubTypes.Type(value = WebSearchTool.class, name = "web_search"),
  @JsonSubTypes.Type(value = McpTool.class, name = "mcp"),
  @JsonSubTypes.Type(value = CustomTool.class, name = "custom"),
  @JsonSubTypes.Type(value = ImageGenerationTool.class, name = "image_generation"),
  @JsonSubTypes.Type(value = LocalShellTool.class, name = "local_shell"),
  @JsonSubTypes.Type(value = ShellTool.class, name = "shell")
})
public sealed interface Tool extends ToolChoiceRepresentable
    permits FunctionTool,
        FunctionToolDefinition,
        FileSearchTool,
        ComputerUseTool,
        WebSearchTool,
        McpTool,
        CodeInterpreterTool,
        ImageGenerationTool,
        LocalShellTool,
        ShellTool,
        CustomTool {
  @NonNull
  String toToolChoice(ObjectMapper mapper) throws JsonProcessingException;
}
