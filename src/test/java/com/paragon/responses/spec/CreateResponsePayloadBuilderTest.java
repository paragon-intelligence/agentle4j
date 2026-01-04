package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.Messages;
import com.paragon.responses.OpenRouterCustomPayload;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for CreateResponsePayload.Builder methods.
 *
 * <p>Covers all builder methods, especially those with 0 coverage.
 */
@DisplayName("CreateResponsePayload.Builder")
class CreateResponsePayloadBuilderTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // BASIC BUILDER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Basic Builder")
  class BasicBuilderTests {

    @Test
    @DisplayName("builder() creates new builder instance")
    void builder_createsNewInstance() {
      CreateResponsePayload.Builder builder = CreateResponsePayload.builder();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() creates payload with defaults")
    void build_createsPayloadWithDefaults() {
      CreateResponsePayload payload = CreateResponsePayload.builder().model("gpt-4o").build();

      assertEquals("gpt-4o", payload.model());
      assertFalse(payload.background());
      assertEquals(Truncation.DISABLED, payload.truncation());
      assertEquals(ServiceTierType.AUTO, payload.serviceTier());
    }

    @Test
    @DisplayName("model() sets model")
    void model_setsModel() {
      CreateResponsePayload payload = CreateResponsePayload.builder().model("gpt-4o-mini").build();

      assertEquals("gpt-4o-mini", payload.model());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INPUT METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Input Methods")
  class InputMethodsTests {

    @Test
    @DisplayName("addUserMessage(String) adds user message")
    void addUserMessage_String_addsUserMessage() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Hello").build();

      assertEquals(1, payload.input().size());
      assertTrue(payload.input().getFirst() instanceof UserMessage);
    }

    @Test
    @DisplayName("addUserMessage(String, status) adds user message with status")
    void addUserMessage_withStatus_addsUserMessageWithStatus() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Hello", InputMessageStatus.COMPLETED)
              .build();

      assertEquals(1, payload.input().size());
    }

    @Test
    @DisplayName("addUserMessage(UserMessage) adds user message object")
    void addUserMessage_object_addsUserMessageObject() {
      UserMessage userMessage = Message.user("Hello world");
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addUserMessage(userMessage).build();

      assertEquals(1, payload.input().size());
      assertEquals(userMessage, payload.input().getFirst());
    }

    @Test
    @DisplayName("addDeveloperMessage(String) adds developer message")
    void addDeveloperMessage_String_addsDeveloperMessage() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addDeveloperMessage("System instructions")
              .build();

      assertEquals(1, payload.input().size());
      assertTrue(payload.input().getFirst() instanceof DeveloperMessage);
    }

    @Test
    @DisplayName("addDeveloperMessage(String, status) adds developer message with status")
    void addDeveloperMessage_withStatus_addsDeveloperMessageWithStatus() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addDeveloperMessage("Instructions", InputMessageStatus.COMPLETED)
              .addUserMessage("Hello")
              .build();

      assertEquals(2, payload.input().size());
      // Developer message should be first
      assertTrue(payload.input().getFirst() instanceof DeveloperMessage);
    }

    @Test
    @DisplayName("addDeveloperMessage throws when duplicate developer message exists")
    void addDeveloperMessage_throwsWhenDuplicate() {
      CreateResponsePayload.Builder builder =
          CreateResponsePayload.builder().model("gpt-4o").addDeveloperMessage("First");

      assertThrows(IllegalArgumentException.class, () -> builder.addDeveloperMessage("Second"));
    }

    @Test
    @DisplayName("addAssistantMessage(String) adds assistant message")
    void addAssistantMessage_String_addsAssistantMessage() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addAssistantMessage("I can help with that")
              .build();

      assertEquals(1, payload.input().size());
      assertTrue(payload.input().getFirst() instanceof AssistantMessage);
    }

    @Test
    @DisplayName("addAssistantMessage(String, status) adds assistant message with status")
    void addAssistantMessage_withStatus_addsAssistantMessageWithStatus() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addAssistantMessage("Response", InputMessageStatus.COMPLETED)
              .build();

      assertEquals(1, payload.input().size());
    }

    @Test
    @DisplayName("addAssistantMessage(AssistantMessage) adds assistant message object")
    void addAssistantMessage_object_addsAssistantMessageObject() {
      AssistantMessage msg = Message.assistant("Testing");
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addAssistantMessage(msg).build();

      assertEquals(1, payload.input().size());
      assertEquals(msg, payload.input().getFirst());
    }

    @Test
    @DisplayName("addMessage() adds any message type")
    void addMessage_addsAnyMessageType() {
      UserMessage user = Message.user("Hello");
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addMessage(user).build();

      assertEquals(1, payload.input().size());
    }

    @Test
    @DisplayName("addMessages(List) adds multiple messages")
    void addMessages_list_addsMultipleMessages() {
      List<Message> messages = List.of(Message.user("Hello"), Message.assistant("Hi there"));

      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addMessages(messages).build();

      assertEquals(2, payload.input().size());
    }

    @Test
    @DisplayName("addMessages(Messages) adds messages from Messages object")
    void addMessages_messagesObject_addsMessages() {
      Messages messages = Messages.of(Message.user("First"), Message.assistant("Second"));

      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addMessages(messages).build();

      assertEquals(2, payload.input().size());
    }

    @Test
    @DisplayName("input() sets input list directly")
    void input_setsInputListDirectly() {
      List<ResponseInputItem> items = List.of(Message.user("Direct input"));

      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").input(items).build();

      assertEquals(1, payload.input().size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONFIGURATION METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Configuration Methods")
  class ConfigurationMethodsTests {

    @Test
    @DisplayName("instructions() sets instructions")
    void instructions_setsInstructions() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").instructions("Be helpful").build();

      assertEquals("Be helpful", payload.instructions());
    }

    @Test
    @DisplayName("temperature() sets temperature")
    void temperature_setsTemperature() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").temperature(0.7).build();

      assertEquals(0.7, payload.temperature());
    }

    @Test
    @DisplayName("maxOutputTokens() sets max output tokens")
    void maxOutputTokens_setsMaxOutputTokens() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").maxOutputTokens(1000).build();

      assertEquals(1000, payload.maxOutputTokens());
    }

    @Test
    @DisplayName("topP() sets top P")
    void topP_setsTopP() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").topP(0.9).build();

      assertEquals(0.9, payload.topP());
    }

    @Test
    @DisplayName("topLogprobs() sets top logprobs")
    void topLogprobs_setsTopLogprobs() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").topLogprobs(5).build();

      assertEquals(5, payload.topLogprobs());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL CONFIGURATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Configuration")
  class ToolConfigurationTests {

    @Test
    @DisplayName("maxToolCalls() sets max tool calls")
    void maxToolCalls_setsMaxToolCalls() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").maxToolCalls(5).build();

      assertEquals(5, payload.maxToolCalls());
    }

    @Test
    @DisplayName("parallelToolCalls() sets parallel tool calls")
    void parallelToolCalls_setsParallelToolCalls() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").parallelToolCalls(false).build();

      assertFalse(payload.parallelToolCalls());
    }

    @Test
    @DisplayName("tools() sets tools list")
    void tools_setsToolsList() {
      WebSearchTool webSearch = new WebSearchTool(null, null, null);
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").tools(List.of(webSearch)).build();

      assertEquals(1, payload.tools().size());
    }

    @Test
    @DisplayName("addTool() adds single tool")
    void addTool_addsSingleTool() {
      WebSearchTool webSearch = new WebSearchTool(null, null, null);
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").addTool(webSearch).build();

      assertEquals(1, payload.tools().size());
    }

    @Test
    @DisplayName("addTool() can add multiple tools")
    void addTool_canAddMultipleTools() {
      WebSearchTool webSearch = new WebSearchTool(null, null, null);
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addTool(webSearch)
              .addTool(webSearch)
              .build();

      assertEquals(2, payload.tools().size());
    }

    @Test
    @DisplayName("toolChoice() sets tool choice")
    void toolChoice_setsToolChoice() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").toolChoice(ToolChoiceMode.AUTO).build();

      assertEquals(ToolChoiceMode.AUTO, payload.toolChoice());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // METADATA AND OTHER OPTIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Metadata and Other Options")
  class MetadataTests {

    @Test
    @DisplayName("metadata() sets metadata")
    void metadata_setsMetadata() {
      Map<String, String> meta = Map.of("key", "value", "user_id", "123");
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").metadata(meta).build();

      assertEquals(meta, payload.metadata());
    }

    @Test
    @DisplayName("store() sets store flag")
    void store_setsStoreFlag() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").store(true).build();

      assertTrue(payload.store());
    }

    @Test
    @DisplayName("background() sets background flag")
    void background_setsBackgroundFlag() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").background(true).build();

      assertTrue(payload.background());
    }

    @Test
    @DisplayName("conversation() sets conversation ID")
    void conversation_setsConversationId() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").conversation("conv-123").build();

      assertEquals("conv-123", payload.conversation());
    }

    @Test
    @DisplayName("serviceTier() sets service tier")
    void serviceTier_setsServiceTier() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .serviceTier(ServiceTierType.DEFAULT)
              .build();

      assertEquals(ServiceTierType.DEFAULT, payload.serviceTier());
    }

    @Test
    @DisplayName("truncation() sets truncation strategy")
    void truncation_setsTruncationStrategy() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").truncation(Truncation.AUTO).build();

      assertEquals(Truncation.AUTO, payload.truncation());
    }

    @Test
    @DisplayName("safetyIdentifier() sets safety identifier")
    void safetyIdentifier_setsSafetyIdentifier() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").safetyIdentifier("safety-123").build();

      assertEquals("safety-123", payload.safetyIdentifier());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STREAMING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Streaming Configuration")
  class StreamingConfigurationTests {

    @Test
    @DisplayName("stream() sets stream flag")
    void stream_setsStreamFlag() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").stream(true).build();

      assertTrue(payload.stream());
      assertTrue(payload instanceof CreateResponsePayload.Streaming);
    }

    @Test
    @DisplayName("stream(false) returns base payload")
    void stream_falseReturnsBasePayload() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").stream(false).build();

      assertFalse(payload.stream());
      assertFalse(payload instanceof CreateResponsePayload.Streaming);
    }

    @Test
    @DisplayName("streaming() returns StreamingBuilder")
    void streaming_returnsStreamingBuilder() {
      CreateResponsePayload.StreamingBuilder builder =
          CreateResponsePayload.builder().model("gpt-4o").streaming();

      assertNotNull(builder);

      CreateResponsePayload payload = builder.build();
      assertTrue(payload.stream());
    }

    @Test
    @DisplayName("streamOptions() sets stream options")
    void streamOptions_setsStreamOptions() {
      StreamOptions options = new StreamOptions(true);
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").stream(true)
              .streamOptions(options)
              .build();

      assertEquals(options, payload.streamOptions());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STRUCTURED OUTPUT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Structured Output")
  class StructuredOutputTests {

    record TestOutput(String name, int age) {}

    @Test
    @DisplayName("withStructuredOutput(Class) configures text format")
    void withStructuredOutput_Class_configuresTextFormat() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Extract person info")
              .withStructuredOutput(TestOutput.class)
              .build();

      assertNotNull(payload.text());
      assertTrue(payload.hasJsonSchemaTextFormat());
    }

    @Test
    @DisplayName("withStructuredOutput with verbosity config")
    void withStructuredOutput_withVerbosityConfig() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Extract")
              .withStructuredOutput(TestOutput.class, ModelVerbosityConfig.HIGH)
              .build();

      assertNotNull(payload.text());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CACHING AND REASONING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Caching and Reasoning")
  class CachingAndReasoningTests {

    @Test
    @DisplayName("promptCacheKey() sets prompt cache key")
    void promptCacheKey_setsPromptCacheKey() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").promptCacheKey("cache-key-123").build();

      assertEquals("cache-key-123", payload.promptCacheKey());
    }

    @Test
    @DisplayName("promptCacheRetention() sets retention")
    void promptCacheRetention_setsRetention() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").promptCacheRetention("1d").build();

      assertEquals("1d", payload.promptCacheRetention());
    }

    @Test
    @DisplayName("reasoning() sets reasoning config")
    void reasoning_setsReasoningConfig() {
      ReasoningConfig config = new ReasoningConfig(ReasoningEffort.MEDIUM, null);
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").reasoning(config).build();

      assertEquals(config, payload.reasoning());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INCLUDE AND PROMPT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Include and Prompt")
  class IncludeAndPromptTests {

    @Test
    @DisplayName("include() sets output data includes")
    void include_setsOutputDataIncludes() {
      List<OutputDataInclude> includes = List.of(OutputDataInclude.FILE_SEARCH_CALL_RESULTS);
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").include(includes).build();

      assertEquals(includes, payload.include());
    }

    @Test
    @DisplayName("prompt() sets prompt template")
    void prompt_setsPromptTemplate() {
      PromptTemplate template = new PromptTemplate("template-id", null, null);
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").prompt(template).build();

      assertEquals(template, payload.prompt());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OPENROUTER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("OpenRouter Configuration")
  class OpenRouterTests {

    @Test
    @DisplayName("openRouterCustomPayload() sets OpenRouter payload")
    void openRouterCustomPayload_setsPayload() {
      OpenRouterCustomPayload orPayload = new OpenRouterCustomPayload(null, null, null, null, null);
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .openRouterCustomPayload(orPayload)
              .build();

      // Empty payload gets normalized to null
      assertNull(payload.openRouterCustomPayload());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PAYLOAD METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Payload Methods")
  class PayloadMethodsTests {

    @Test
    @DisplayName("hasEmptyText() returns true when text is null")
    void hasEmptyText_returnsTrueWhenTextNull() {
      CreateResponsePayload payload = CreateResponsePayload.builder().model("gpt-4o").build();

      assertTrue(payload.hasEmptyText());
    }

    @Test
    @DisplayName("hasEmptyTextFormat() returns true when no format")
    void hasEmptyTextFormat_returnsTrueWhenNoFormat() {
      CreateResponsePayload payload = CreateResponsePayload.builder().model("gpt-4o").build();

      assertTrue(payload.hasEmptyTextFormat());
    }

    @Test
    @DisplayName("streamEnabled() returns stream value")
    void streamEnabled_returnsStreamValue() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").stream(true).build();

      assertTrue(payload.streamEnabled());
    }

    @Test
    @DisplayName("equals() works correctly")
    void equals_worksCorrectly() {
      CreateResponsePayload payload1 =
          CreateResponsePayload.builder().model("gpt-4o").temperature(0.7).build();
      CreateResponsePayload payload2 =
          CreateResponsePayload.builder().model("gpt-4o").temperature(0.7).build();

      assertEquals(payload1, payload2);
      assertEquals(payload1.hashCode(), payload2.hashCode());
    }

    @Test
    @DisplayName("toString() returns formatted string")
    void toString_returnsFormattedString() {
      CreateResponsePayload payload = CreateResponsePayload.builder().model("gpt-4o").build();

      String result = payload.toString();

      assertTrue(result.contains("CreateResponsePayload"));
      assertTrue(result.contains("gpt-4o"));
    }
  }
}
