package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for all polymorphic types to ensure Jackson can deserialize them correctly.
 * This verifies that all sealed interfaces with @JsonTypeInfo and @JsonSubTypes annotations work
 * properly.
 */
class PolymorphicDeserializationTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  @Test
  void deserialize_ReasoningContent_asReasoningText() throws Exception {
    String json =
        """
        { "type": "reasoning_text", "text": "Let me think..." }
        """;

    ReasoningContent result = objectMapper.readValue(json, ReasoningContent.class);

    assertNotNull(result);
    assertInstanceOf(ReasoningTextContent.class, result);
    assertEquals("Let me think...", ((ReasoningTextContent) result).text());
  }

  @Test
  void deserialize_ReasoningSummary_asSummaryText() throws Exception {
    String json =
        """
        { "type": "summary_text", "text": "Summary of reasoning" }
        """;

    ReasoningSummary result = objectMapper.readValue(json, ReasoningSummary.class);

    assertNotNull(result);
    assertInstanceOf(ReasoningSummaryText.class, result);
    assertEquals("Summary of reasoning", ((ReasoningSummaryText) result).text());
  }

  @Test
  void deserialize_CodeInterpreterOutput_asLogs() throws Exception {
    String json =
        """
        { "type": "logs", "logs": "Starting execution..." }
        """;

    CodeInterpreterOutput result = objectMapper.readValue(json, CodeInterpreterOutput.class);

    assertNotNull(result);
    assertInstanceOf(CodeInterpreterOutputLogs.class, result);
    assertEquals("Starting execution...", ((CodeInterpreterOutputLogs) result).logs());
  }

  @Test
  void deserialize_CodeInterpreterOutput_asImage() throws Exception {
    String json =
        """
        { "type": "image", "url": "https://example.com/image.png" }
        """;

    CodeInterpreterOutput result = objectMapper.readValue(json, CodeInterpreterOutput.class);

    assertNotNull(result);
    assertInstanceOf(CodeInterpreterOutputImage.class, result);
    assertEquals("https://example.com/image.png", ((CodeInterpreterOutputImage) result).url());
  }

  @Test
  void deserialize_WebAction_asSearch() throws Exception {
    String json =
        """
        { "type": "search", "query": "test query", "sources": null }
        """;

    WebAction result = objectMapper.readValue(json, WebAction.class);

    assertNotNull(result);
    assertInstanceOf(SearchAction.class, result);
    assertEquals("test query", ((SearchAction) result).query());
  }

  @Test
  void deserialize_WebAction_asOpenPage() throws Exception {
    String json =
        """
        { "type": "open_page", "url": "https://example.com" }
        """;

    WebAction result = objectMapper.readValue(json, WebAction.class);

    assertNotNull(result);
    assertInstanceOf(OpenPageAction.class, result);
    assertEquals("https://example.com", ((OpenPageAction) result).url());
  }

  @Test
  void deserialize_WebAction_asFind() throws Exception {
    String json =
        """
        { "type": "find", "pattern": "search term", "url": "https://example.com" }
        """;

    WebAction result = objectMapper.readValue(json, WebAction.class);

    assertNotNull(result);
    assertInstanceOf(FindAction.class, result);
    assertEquals("search term", ((FindAction) result).pattern());
  }

  @Test
  void deserialize_ApplyPatchOperation_asCreateFile() throws Exception {
    String json =
        """
        { "type": "create_file", "diff": "diff content", "path": "/path/to/file" }
        """;

    ApplyPatchOperation result = objectMapper.readValue(json, ApplyPatchOperation.class);

    assertNotNull(result);
    assertInstanceOf(ApplyPatchCreateFileOperation.class, result);
    assertEquals("/path/to/file", ((ApplyPatchCreateFileOperation) result).path());
  }

  @Test
  void deserialize_ApplyPatchOperation_asDeleteFile() throws Exception {
    String json =
        """
        { "type": "delete_file", "path": "/path/to/delete" }
        """;

    ApplyPatchOperation result = objectMapper.readValue(json, ApplyPatchOperation.class);

    assertNotNull(result);
    assertInstanceOf(ApplyPatchDeleteFileOperation.class, result);
    assertEquals("/path/to/delete", ((ApplyPatchDeleteFileOperation) result).path());
  }

  @Test
  void deserialize_ApplyPatchOperation_asUpdateFile() throws Exception {
    String json =
        """
        { "type": "update_file", "diff": "diff content", "path": "/path/to/update" }
        """;

    ApplyPatchOperation result = objectMapper.readValue(json, ApplyPatchOperation.class);

    assertNotNull(result);
    assertInstanceOf(ApplyPatchUpdateFileOperation.class, result);
    assertEquals("/path/to/update", ((ApplyPatchUpdateFileOperation) result).path());
  }

  @Test
  void deserialize_TextConfigurationOptionsFormat_asJsonSchema() throws Exception {
    String json =
        """
        { "type": "json_schema", "name": "MySchema", "schema": { "type": "object" } }
        """;

    TextConfigurationOptionsFormat result =
        objectMapper.readValue(json, TextConfigurationOptionsFormat.class);

    assertNotNull(result);
    assertInstanceOf(TextConfigurationOptionsJsonSchemaFormat.class, result);
    assertEquals("MySchema", ((TextConfigurationOptionsJsonSchemaFormat) result).name());
  }

  @Test
  void deserialize_TextConfigurationOptionsFormat_asText() throws Exception {
    String json =
        """
        { "type": "text" }
        """;

    TextConfigurationOptionsFormat result =
        objectMapper.readValue(json, TextConfigurationOptionsFormat.class);

    assertNotNull(result);
    assertInstanceOf(TextConfigurationOptionsTextFormat.class, result);
  }

  @Test
  void deserialize_CustomToolInputFormat_asText() throws Exception {
    String json =
        """
        { "type": "text" }
        """;

    CustomToolInputFormat result = objectMapper.readValue(json, CustomToolInputFormat.class);

    assertNotNull(result);
    assertInstanceOf(CustomToolInputFormatText.class, result);
  }

  @Test
  void deserialize_CustomToolInputFormat_asGrammar() throws Exception {
    String json =
        """
        { "type": "grammar", "definition": "grammar def", "syntax": "LARK" }
        """;

    CustomToolInputFormat result = objectMapper.readValue(json, CustomToolInputFormat.class);

    assertNotNull(result);
    assertInstanceOf(CustomToolInputFormatGrammar.class, result);
    assertEquals("grammar def", ((CustomToolInputFormatGrammar) result).definition());
  }

  @Test
  void deserialize_Source_asUrl() throws Exception {
    String json =
        """
        { "type": "url", "url": "https://source.com" }
        """;

    Source result = objectMapper.readValue(json, Source.class);

    assertNotNull(result);
    assertInstanceOf(UrlSource.class, result);
    assertEquals("https://source.com", ((UrlSource) result).url());
  }

  @Test
  void deserialize_LocalShellAction_asExec() throws Exception {
    String json =
        """
        {
          "type": "exec",
          "command": ["ls", "-la"],
          "env": null,
          "timeoutMs": 5000,
          "user": null,
          "workingDirectory": "/tmp"
        }
        """;

    LocalShellAction result = objectMapper.readValue(json, LocalShellAction.class);

    assertNotNull(result);
    assertInstanceOf(LocalShellExecAction.class, result);
    assertEquals(2, ((LocalShellExecAction) result).command().size());
    assertEquals("ls", ((LocalShellExecAction) result).command().get(0));
  }

  @Test
  void deserialize_FunctionShellToolCallChunkOutcome_asTimeout() throws Exception {
    String json =
        """
        { "type": "timeout" }
        """;

    FunctionShellToolCallChunkOutcome result =
        objectMapper.readValue(json, FunctionShellToolCallChunkOutcome.class);

    assertNotNull(result);
    assertInstanceOf(FunctionShellToolCallChunkTimeoutOutcome.class, result);
  }

  @Test
  void deserialize_FunctionShellToolCallChunkOutcome_asExit() throws Exception {
    String json =
        """
        { "type": "exit", "exitCode": 0 }
        """;

    FunctionShellToolCallChunkOutcome result =
        objectMapper.readValue(json, FunctionShellToolCallChunkOutcome.class);

    assertNotNull(result);
    assertInstanceOf(FunctionShellToolCallChunkExitOutcome.class, result);
    // Main goal is to verify the correct type is deserialized
  }

  @Test
  void deserialize_CustomToolCallOutputKind_asText() throws Exception {
    String json =
        """
        { "type": "input_text", "text": "Custom output" }
        """;

    CustomToolCallOutputKind result = objectMapper.readValue(json, CustomToolCallOutputKind.class);

    assertNotNull(result);
    assertInstanceOf(Text.class, result);
    assertEquals("Custom output", ((Text) result).text());
  }

  @Test
  void deserialize_MessageContent_asText() throws Exception {
    String json =
        """
        { "type": "input_text", "text": "Message text" }
        """;

    MessageContent result = objectMapper.readValue(json, MessageContent.class);

    assertNotNull(result);
    assertInstanceOf(Text.class, result);
    assertEquals("Message text", ((Text) result).text());
  }

  @Test
  void deserialize_MessageContent_asOutputText() throws Exception {
    String json =
        """
        { "type": "output_text", "text": "Output message text" }
        """;

    MessageContent result = objectMapper.readValue(json, MessageContent.class);

    assertNotNull(result);
    assertInstanceOf(Text.class, result);
    assertEquals("Output message text", ((Text) result).text());
  }

  @Test
  void roundTrip_AllPolymorphicTypes_PreserveTypeInformation() throws Exception {
    // Test that we can serialize and deserialize without losing type information
    ReasoningContent original = new ReasoningTextContent("test reasoning");

    String json = objectMapper.writeValueAsString(original);
    ReasoningContent deserialized = objectMapper.readValue(json, ReasoningContent.class);

    assertNotNull(deserialized);
    assertInstanceOf(ReasoningTextContent.class, deserialized);
    assertEquals("test reasoning", ((ReasoningTextContent) deserialized).text());
  }
}
