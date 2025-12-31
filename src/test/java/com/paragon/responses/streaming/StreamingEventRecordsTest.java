package com.paragon.responses.streaming;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for all streaming event record classes. */
@DisplayName("Streaming Event Records")
class StreamingEventRecordsTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  // ==================== Response Lifecycle Events ====================

  @Nested
  @DisplayName("Response Lifecycle Events")
  class ResponseLifecycleTests {

    @Test
    @DisplayName("ResponseCreatedEvent deserializes")
    void responseCreatedDeserializes() throws Exception {
      String json = "{\"type\":\"response.created\",\"sequence_number\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ResponseCreatedEvent.class, event);
      assertEquals("response.created", event.type());
      assertEquals(0, event.sequenceNumber());
    }

    @Test
    @DisplayName("ResponseQueuedEvent deserializes")
    void responseQueuedDeserializes() throws Exception {
      String json = "{\"type\":\"response.queued\",\"sequence_number\":1}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ResponseQueuedEvent.class, event);
      assertEquals("response.queued", event.type());
    }

    @Test
    @DisplayName("ResponseInProgressEvent deserializes")
    void responseInProgressDeserializes() throws Exception {
      String json = "{\"type\":\"response.in_progress\",\"sequence_number\":2}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ResponseInProgressEvent.class, event);
    }

    @Test
    @DisplayName("ResponseCompletedEvent deserializes")
    void responseCompletedDeserializes() throws Exception {
      String json = "{\"type\":\"response.completed\",\"sequence_number\":10}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ResponseCompletedEvent.class, event);
      assertEquals(10, event.sequenceNumber());
    }

    @Test
    @DisplayName("ResponseFailedEvent deserializes")
    void responseFailedDeserializes() throws Exception {
      String json = "{\"type\":\"response.failed\",\"sequence_number\":5}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ResponseFailedEvent.class, event);
    }

    @Test
    @DisplayName("ResponseIncompleteEvent deserializes")
    void responseIncompleteDeserializes() throws Exception {
      String json = "{\"type\":\"response.incomplete\",\"sequence_number\":8}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ResponseIncompleteEvent.class, event);
    }
  }

  // ==================== Text Streaming Events ====================

  @Nested
  @DisplayName("Text Streaming Events")
  class TextStreamingTests {

    @Test
    @DisplayName("OutputTextDeltaEvent deserializes with delta")
    void outputTextDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.output_text.delta\",\"sequence_number\":5,\"item_id\":\"item1\",\"output_index\":0,\"content_index\":0,\"delta\":\"Hello\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(OutputTextDeltaEvent.class, event);
      OutputTextDeltaEvent textEvent = (OutputTextDeltaEvent) event;
      assertEquals("Hello", textEvent.delta());
      assertEquals("item1", textEvent.itemId());
    }

    @Test
    @DisplayName("OutputTextDoneEvent deserializes with full text")
    void outputTextDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.output_text.done\",\"sequence_number\":10,\"item_id\":\"item1\",\"output_index\":0,\"content_index\":0,\"text\":\"Complete text\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(OutputTextDoneEvent.class, event);
      OutputTextDoneEvent textEvent = (OutputTextDoneEvent) event;
      assertEquals("Complete text", textEvent.text());
    }

    @Test
    @DisplayName("RefusalDeltaEvent deserializes")
    void refusalDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.refusal.delta\",\"sequence_number\":3,\"item_id\":\"item1\",\"output_index\":0,\"content_index\":0,\"delta\":\"I cannot\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(RefusalDeltaEvent.class, event);
    }

    @Test
    @DisplayName("RefusalDoneEvent deserializes")
    void refusalDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.refusal.done\",\"sequence_number\":5,\"item_id\":\"item1\",\"output_index\":0,\"content_index\":0,\"refusal\":\"I cannot help with that\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(RefusalDoneEvent.class, event);
    }
  }

  // ==================== Function Call Events ====================

  @Nested
  @DisplayName("Function Call Events")
  class FunctionCallTests {

    @Test
    @DisplayName("FunctionCallArgumentsDeltaEvent deserializes")
    void functionCallArgumentsDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.function_call_arguments.delta\",\"sequence_number\":3,\"item_id\":\"call1\",\"output_index\":0,\"delta\":\"{\\\"q\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(FunctionCallArgumentsDeltaEvent.class, event);
      FunctionCallArgumentsDeltaEvent funcEvent = (FunctionCallArgumentsDeltaEvent) event;
      assertEquals("{\"q", funcEvent.delta());
    }

    @Test
    @DisplayName("FunctionCallArgumentsDoneEvent deserializes")
    void functionCallArgumentsDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.function_call_arguments.done\",\"sequence_number\":5,\"item_id\":\"call1\",\"output_index\":0,\"arguments\":\"{\\\"query\\\":\\\"test\\\"}\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(FunctionCallArgumentsDoneEvent.class, event);
    }
  }

  // ==================== Tool Call Events ====================

  @Nested
  @DisplayName("Tool Call Events")
  class ToolCallTests {

    @Test
    @DisplayName("FileSearchCallInProgressEvent deserializes")
    void fileSearchInProgressDeserializes() throws Exception {
      String json = "{\"type\":\"response.file_search_call.in_progress\",\"sequence_number\":2,\"item_id\":\"search1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(FileSearchCallInProgressEvent.class, event);
    }

    @Test
    @DisplayName("FileSearchCallSearchingEvent deserializes")
    void fileSearchSearchingDeserializes() throws Exception {
      String json = "{\"type\":\"response.file_search_call.searching\",\"sequence_number\":3,\"item_id\":\"search1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(FileSearchCallSearchingEvent.class, event);
    }

    @Test
    @DisplayName("FileSearchCallCompletedEvent deserializes")
    void fileSearchCompletedDeserializes() throws Exception {
      String json = "{\"type\":\"response.file_search_call.completed\",\"sequence_number\":5,\"item_id\":\"search1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(FileSearchCallCompletedEvent.class, event);
    }

    @Test
    @DisplayName("WebSearchCallInProgressEvent deserializes")
    void webSearchInProgressDeserializes() throws Exception {
      String json = "{\"type\":\"response.web_search_call.in_progress\",\"sequence_number\":2,\"item_id\":\"web1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(WebSearchCallInProgressEvent.class, event);
    }

    @Test
    @DisplayName("WebSearchCallCompletedEvent deserializes")
    void webSearchCompletedDeserializes() throws Exception {
      String json = "{\"type\":\"response.web_search_call.completed\",\"sequence_number\":5,\"item_id\":\"web1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(WebSearchCallCompletedEvent.class, event);
    }
  }

  // ==================== Code Interpreter Events ====================

  @Nested
  @DisplayName("Code Interpreter Events")
  class CodeInterpreterTests {

    @Test
    @DisplayName("CodeInterpreterCallInProgressEvent deserializes")
    void codeInterpreterInProgressDeserializes() throws Exception {
      String json = "{\"type\":\"response.code_interpreter_call.in_progress\",\"sequence_number\":2,\"item_id\":\"code1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(CodeInterpreterCallInProgressEvent.class, event);
    }

    @Test
    @DisplayName("CodeInterpreterCallInterpretingEvent deserializes")
    void codeInterpreterInterpretingDeserializes() throws Exception {
      String json = "{\"type\":\"response.code_interpreter_call.interpreting\",\"sequence_number\":3,\"item_id\":\"code1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(CodeInterpreterCallInterpretingEvent.class, event);
    }

    @Test
    @DisplayName("CodeInterpreterCallCompletedEvent deserializes")
    void codeInterpreterCompletedDeserializes() throws Exception {
      String json = "{\"type\":\"response.code_interpreter_call.completed\",\"sequence_number\":5,\"item_id\":\"code1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(CodeInterpreterCallCompletedEvent.class, event);
    }

    @Test
    @DisplayName("CodeInterpreterCallCodeDeltaEvent deserializes")
    void codeInterpreterCodeDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.code_interpreter_call_code.delta\",\"sequence_number\":3,\"item_id\":\"code1\",\"output_index\":0,\"delta\":\"print(\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(CodeInterpreterCallCodeDeltaEvent.class, event);
    }

    @Test
    @DisplayName("CodeInterpreterCallCodeDoneEvent deserializes")
    void codeInterpreterCodeDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.code_interpreter_call_code.done\",\"sequence_number\":5,\"item_id\":\"code1\",\"output_index\":0,\"code\":\"print('Hello')\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(CodeInterpreterCallCodeDoneEvent.class, event);
    }
  }

  // ==================== Reasoning Events ====================

  @Nested
  @DisplayName("Reasoning Events")
  class ReasoningTests {

    @Test
    @DisplayName("ReasoningSummaryPartAddedEvent deserializes")
    void reasoningSummaryPartAddedDeserializes() throws Exception {
      String json = "{\"type\":\"response.reasoning_summary_part.added\",\"sequence_number\":2,\"item_id\":\"r1\",\"output_index\":0,\"summary_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ReasoningSummaryPartAddedEvent.class, event);
    }

    @Test
    @DisplayName("ReasoningSummaryTextDeltaEvent deserializes")
    void reasoningSummaryTextDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.reasoning_summary_text.delta\",\"sequence_number\":3,\"item_id\":\"r1\",\"output_index\":0,\"summary_index\":0,\"delta\":\"First\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ReasoningSummaryTextDeltaEvent.class, event);
    }

    @Test
    @DisplayName("ReasoningTextDeltaEvent deserializes")
    void reasoningTextDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.reasoning_text.delta\",\"sequence_number\":3,\"item_id\":\"r1\",\"output_index\":0,\"content_index\":0,\"delta\":\"Thinking\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ReasoningTextDeltaEvent.class, event);
    }

    @Test
    @DisplayName("ReasoningTextDoneEvent deserializes")
    void reasoningTextDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.reasoning_text.done\",\"sequence_number\":5,\"item_id\":\"r1\",\"output_index\":0,\"content_index\":0,\"text\":\"Complete reasoning\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ReasoningTextDoneEvent.class, event);
    }
  }

  // ==================== Image Generation Events ====================

  @Nested
  @DisplayName("Image Generation Events")
  class ImageGenerationTests {

    @Test
    @DisplayName("ImageGenerationCallInProgressEvent deserializes")
    void imageGenInProgressDeserializes() throws Exception {
      String json = "{\"type\":\"response.image_generation_call.in_progress\",\"sequence_number\":2,\"item_id\":\"img1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ImageGenerationCallInProgressEvent.class, event);
    }

    @Test
    @DisplayName("ImageGenerationCallGeneratingEvent deserializes")
    void imageGenGeneratingDeserializes() throws Exception {
      String json = "{\"type\":\"response.image_generation_call.generating\",\"sequence_number\":3,\"item_id\":\"img1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ImageGenerationCallGeneratingEvent.class, event);
    }

    @Test
    @DisplayName("ImageGenerationCallPartialImageEvent deserializes")
    void imageGenPartialImageDeserializes() throws Exception {
      String json = "{\"type\":\"response.image_generation_call.partial_image\",\"sequence_number\":4,\"item_id\":\"img1\",\"output_index\":0,\"partial_image_b64\":\"abc123\",\"partial_image_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ImageGenerationCallPartialImageEvent.class, event);
    }

    @Test
    @DisplayName("ImageGenerationCallCompletedEvent deserializes")
    void imageGenCompletedDeserializes() throws Exception {
      String json = "{\"type\":\"response.image_generation_call.completed\",\"sequence_number\":5,\"item_id\":\"img1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ImageGenerationCallCompletedEvent.class, event);
    }
  }

  // ==================== MCP Events ====================

  @Nested
  @DisplayName("MCP Events")
  class McpTests {

    @Test
    @DisplayName("McpCallInProgressEvent deserializes")
    void mcpCallInProgressDeserializes() throws Exception {
      String json = "{\"type\":\"response.mcp_call.in_progress\",\"sequence_number\":2,\"item_id\":\"mcp1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(McpCallInProgressEvent.class, event);
    }

    @Test
    @DisplayName("McpCallCompletedEvent deserializes")
    void mcpCallCompletedDeserializes() throws Exception {
      String json = "{\"type\":\"response.mcp_call.completed\",\"sequence_number\":5,\"item_id\":\"mcp1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(McpCallCompletedEvent.class, event);
    }

    @Test
    @DisplayName("McpCallFailedEvent deserializes")
    void mcpCallFailedDeserializes() throws Exception {
      String json = "{\"type\":\"response.mcp_call.failed\",\"sequence_number\":5,\"item_id\":\"mcp1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(McpCallFailedEvent.class, event);
    }

    @Test
    @DisplayName("McpCallArgumentsDeltaEvent deserializes")
    void mcpCallArgumentsDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.mcp_call_arguments.delta\",\"sequence_number\":3,\"item_id\":\"mcp1\",\"output_index\":0,\"delta\":\"{\\\"key\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(McpCallArgumentsDeltaEvent.class, event);
    }

    @Test
    @DisplayName("McpListToolsInProgressEvent deserializes")
    void mcpListToolsInProgressDeserializes() throws Exception {
      String json = "{\"type\":\"response.mcp_list_tools.in_progress\",\"sequence_number\":1,\"item_id\":\"list1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(McpListToolsInProgressEvent.class, event);
    }

    @Test
    @DisplayName("McpListToolsCompletedEvent deserializes")
    void mcpListToolsCompletedDeserializes() throws Exception {
      String json = "{\"type\":\"response.mcp_list_tools.completed\",\"sequence_number\":2,\"item_id\":\"list1\",\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(McpListToolsCompletedEvent.class, event);
    }
  }

  // ==================== Content Part Events ====================

  @Nested
  @DisplayName("Content Part Events")
  class ContentPartTests {

    @Test
    @DisplayName("ContentPartAddedEvent deserializes")
    void contentPartAddedDeserializes() throws Exception {
      String json = "{\"type\":\"response.content_part.added\",\"sequence_number\":3,\"item_id\":\"item1\",\"output_index\":0,\"content_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ContentPartAddedEvent.class, event);
    }

    @Test
    @DisplayName("ContentPartDoneEvent deserializes")
    void contentPartDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.content_part.done\",\"sequence_number\":8,\"item_id\":\"item1\",\"output_index\":0,\"content_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(ContentPartDoneEvent.class, event);
    }

    @Test
    @DisplayName("OutputItemAddedEvent deserializes")
    void outputItemAddedDeserializes() throws Exception {
      String json = "{\"type\":\"response.output_item.added\",\"sequence_number\":2,\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(OutputItemAddedEvent.class, event);
    }

    @Test
    @DisplayName("OutputItemDoneEvent deserializes")
    void outputItemDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.output_item.done\",\"sequence_number\":9,\"output_index\":0}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(OutputItemDoneEvent.class, event);
    }
  }

  // ==================== Error Events ====================

  @Nested
  @DisplayName("Error Events")
  class ErrorTests {

    @Test
    @DisplayName("StreamingErrorEvent deserializes")
    void streamingErrorDeserializes() throws Exception {
      String json = "{\"type\":\"error\",\"sequence_number\":0,\"code\":\"server_error\",\"message\":\"An error occurred\",\"param\":null}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(StreamingErrorEvent.class, event);
    }
  }

  // ==================== Custom Tool Call Events ====================

  @Nested
  @DisplayName("Custom Tool Call Events")
  class CustomToolCallTests {

    @Test
    @DisplayName("CustomToolCallInputDeltaEvent deserializes")
    void customToolCallInputDeltaDeserializes() throws Exception {
      String json = "{\"type\":\"response.custom_tool_call_input.delta\",\"sequence_number\":3,\"item_id\":\"tool1\",\"output_index\":0,\"delta\":\"{\\\"arg\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(CustomToolCallInputDeltaEvent.class, event);
    }

    @Test
    @DisplayName("CustomToolCallInputDoneEvent deserializes")
    void customToolCallInputDoneDeserializes() throws Exception {
      String json = "{\"type\":\"response.custom_tool_call_input.done\",\"sequence_number\":5,\"item_id\":\"tool1\",\"output_index\":0,\"input\":\"{\\\"arg\\\":\\\"value\\\"}\"}";
      StreamingEvent event = mapper.readValue(json, StreamingEvent.class);
      assertInstanceOf(CustomToolCallInputDoneEvent.class, event);
    }
  }
}
