package com.paragon.responses.streaming;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.NonNull;

/**
 * Base sealed interface for all OpenAI Responses API streaming events.
 *
 * <p>Events are emitted as Server-Sent Events (SSE) when streaming is enabled. Each event has a
 * type discriminator and sequence number for ordering.
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/responses-streaming">Streaming
 *     Events</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
  // Response lifecycle events
  @JsonSubTypes.Type(value = ResponseCreatedEvent.class, name = "response.created"),
  @JsonSubTypes.Type(value = ResponseQueuedEvent.class, name = "response.queued"),
  @JsonSubTypes.Type(value = ResponseInProgressEvent.class, name = "response.in_progress"),
  @JsonSubTypes.Type(value = ResponseCompletedEvent.class, name = "response.completed"),
  @JsonSubTypes.Type(value = ResponseFailedEvent.class, name = "response.failed"),
  @JsonSubTypes.Type(value = ResponseIncompleteEvent.class, name = "response.incomplete"),

  // Output item events
  @JsonSubTypes.Type(value = OutputItemAddedEvent.class, name = "response.output_item.added"),
  @JsonSubTypes.Type(value = OutputItemDoneEvent.class, name = "response.output_item.done"),

  // Content part events
  @JsonSubTypes.Type(value = ContentPartAddedEvent.class, name = "response.content_part.added"),
  @JsonSubTypes.Type(value = ContentPartDoneEvent.class, name = "response.content_part.done"),

  // Text streaming events
  @JsonSubTypes.Type(value = OutputTextDeltaEvent.class, name = "response.output_text.delta"),
  @JsonSubTypes.Type(value = OutputTextDoneEvent.class, name = "response.output_text.done"),
  @JsonSubTypes.Type(
      value = OutputTextAnnotationAddedEvent.class,
      name = "response.output_text.annotation.added"),

  // Refusal events
  @JsonSubTypes.Type(value = RefusalDeltaEvent.class, name = "response.refusal.delta"),
  @JsonSubTypes.Type(value = RefusalDoneEvent.class, name = "response.refusal.done"),

  // Function call events
  @JsonSubTypes.Type(
      value = FunctionCallArgumentsDeltaEvent.class,
      name = "response.function_call_arguments.delta"),
  @JsonSubTypes.Type(
      value = FunctionCallArgumentsDoneEvent.class,
      name = "response.function_call_arguments.done"),

  // File search events
  @JsonSubTypes.Type(
      value = FileSearchCallInProgressEvent.class,
      name = "response.file_search_call.in_progress"),
  @JsonSubTypes.Type(
      value = FileSearchCallSearchingEvent.class,
      name = "response.file_search_call.searching"),
  @JsonSubTypes.Type(
      value = FileSearchCallCompletedEvent.class,
      name = "response.file_search_call.completed"),

  // Web search events
  @JsonSubTypes.Type(
      value = WebSearchCallInProgressEvent.class,
      name = "response.web_search_call.in_progress"),
  @JsonSubTypes.Type(
      value = WebSearchCallSearchingEvent.class,
      name = "response.web_search_call.searching"),
  @JsonSubTypes.Type(
      value = WebSearchCallCompletedEvent.class,
      name = "response.web_search_call.completed"),

  // Code interpreter events
  @JsonSubTypes.Type(
      value = CodeInterpreterCallInProgressEvent.class,
      name = "response.code_interpreter_call.in_progress"),
  @JsonSubTypes.Type(
      value = CodeInterpreterCallInterpretingEvent.class,
      name = "response.code_interpreter_call.interpreting"),
  @JsonSubTypes.Type(
      value = CodeInterpreterCallCompletedEvent.class,
      name = "response.code_interpreter_call.completed"),
  @JsonSubTypes.Type(
      value = CodeInterpreterCallCodeDeltaEvent.class,
      name = "response.code_interpreter_call_code.delta"),
  @JsonSubTypes.Type(
      value = CodeInterpreterCallCodeDoneEvent.class,
      name = "response.code_interpreter_call_code.done"),

  // Reasoning events
  @JsonSubTypes.Type(
      value = ReasoningSummaryPartAddedEvent.class,
      name = "response.reasoning_summary_part.added"),
  @JsonSubTypes.Type(
      value = ReasoningSummaryPartDoneEvent.class,
      name = "response.reasoning_summary_part.done"),
  @JsonSubTypes.Type(
      value = ReasoningSummaryTextDeltaEvent.class,
      name = "response.reasoning_summary_text.delta"),
  @JsonSubTypes.Type(
      value = ReasoningSummaryTextDoneEvent.class,
      name = "response.reasoning_summary_text.done"),
  @JsonSubTypes.Type(value = ReasoningTextDeltaEvent.class, name = "response.reasoning_text.delta"),
  @JsonSubTypes.Type(value = ReasoningTextDoneEvent.class, name = "response.reasoning_text.done"),

  // Image generation events
  @JsonSubTypes.Type(
      value = ImageGenerationCallInProgressEvent.class,
      name = "response.image_generation_call.in_progress"),
  @JsonSubTypes.Type(
      value = ImageGenerationCallGeneratingEvent.class,
      name = "response.image_generation_call.generating"),
  @JsonSubTypes.Type(
      value = ImageGenerationCallPartialImageEvent.class,
      name = "response.image_generation_call.partial_image"),
  @JsonSubTypes.Type(
      value = ImageGenerationCallCompletedEvent.class,
      name = "response.image_generation_call.completed"),

  // MCP events
  @JsonSubTypes.Type(value = McpCallInProgressEvent.class, name = "response.mcp_call.in_progress"),
  @JsonSubTypes.Type(value = McpCallCompletedEvent.class, name = "response.mcp_call.completed"),
  @JsonSubTypes.Type(value = McpCallFailedEvent.class, name = "response.mcp_call.failed"),
  @JsonSubTypes.Type(
      value = McpCallArgumentsDeltaEvent.class,
      name = "response.mcp_call_arguments.delta"),
  @JsonSubTypes.Type(
      value = McpCallArgumentsDoneEvent.class,
      name = "response.mcp_call_arguments.done"),
  @JsonSubTypes.Type(
      value = McpListToolsInProgressEvent.class,
      name = "response.mcp_list_tools.in_progress"),
  @JsonSubTypes.Type(
      value = McpListToolsCompletedEvent.class,
      name = "response.mcp_list_tools.completed"),
  @JsonSubTypes.Type(
      value = McpListToolsFailedEvent.class,
      name = "response.mcp_list_tools.failed"),

  // Custom tool call events
  @JsonSubTypes.Type(
      value = CustomToolCallInputDeltaEvent.class,
      name = "response.custom_tool_call_input.delta"),
  @JsonSubTypes.Type(
      value = CustomToolCallInputDoneEvent.class,
      name = "response.custom_tool_call_input.done"),

  // Error event
  @JsonSubTypes.Type(value = StreamingErrorEvent.class, name = "error")
})
public sealed interface StreamingEvent
    permits
        // Response lifecycle
        ResponseCreatedEvent,
        ResponseQueuedEvent,
        ResponseInProgressEvent,
        ResponseCompletedEvent,
        ResponseFailedEvent,
        ResponseIncompleteEvent,
        // Output items
        OutputItemAddedEvent,
        OutputItemDoneEvent,
        // Content parts
        ContentPartAddedEvent,
        ContentPartDoneEvent,
        // Text streaming
        OutputTextDeltaEvent,
        OutputTextDoneEvent,
        OutputTextAnnotationAddedEvent,
        // Refusal
        RefusalDeltaEvent,
        RefusalDoneEvent,
        // Function calls
        FunctionCallArgumentsDeltaEvent,
        FunctionCallArgumentsDoneEvent,
        // File search
        FileSearchCallInProgressEvent,
        FileSearchCallSearchingEvent,
        FileSearchCallCompletedEvent,
        // Web search
        WebSearchCallInProgressEvent,
        WebSearchCallSearchingEvent,
        WebSearchCallCompletedEvent,
        // Code interpreter
        CodeInterpreterCallInProgressEvent,
        CodeInterpreterCallInterpretingEvent,
        CodeInterpreterCallCompletedEvent,
        CodeInterpreterCallCodeDeltaEvent,
        CodeInterpreterCallCodeDoneEvent,
        // Reasoning
        ReasoningSummaryPartAddedEvent,
        ReasoningSummaryPartDoneEvent,
        ReasoningSummaryTextDeltaEvent,
        ReasoningSummaryTextDoneEvent,
        ReasoningTextDeltaEvent,
        ReasoningTextDoneEvent,
        // Image generation
        ImageGenerationCallInProgressEvent,
        ImageGenerationCallGeneratingEvent,
        ImageGenerationCallPartialImageEvent,
        ImageGenerationCallCompletedEvent,
        // MCP
        McpCallInProgressEvent,
        McpCallCompletedEvent,
        McpCallFailedEvent,
        McpCallArgumentsDeltaEvent,
        McpCallArgumentsDoneEvent,
        McpListToolsInProgressEvent,
        McpListToolsCompletedEvent,
        McpListToolsFailedEvent,
        // Custom tool calls
        CustomToolCallInputDeltaEvent,
        CustomToolCallInputDoneEvent,
        // Error
        StreamingErrorEvent {

  /** The event type identifier. */
  @NonNull
  String type();

  /** Sequence number for ordering events. */
  int sequenceNumber();
}
