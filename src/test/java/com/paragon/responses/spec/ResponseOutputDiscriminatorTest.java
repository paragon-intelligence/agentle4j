package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Basic verification test for ResponseOutput discriminated union annotations. Tests that the
 * Jackson annotations correctly handle polymorphic serialization/deserialization.
 */
class ResponseOutputDiscriminatorTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  @Test
  void functionToolCallSerializesWithTypeField() throws Exception {
    // Create a FunctionToolCall
    FunctionToolCall toolCall =
        new FunctionToolCall(
            "{\"arg\":\"value\"}",
            "call_123",
            "test_function",
            "id_123",
            FunctionToolCallStatus.COMPLETED);

    // Serialize directly (Jackson will use polymorphic serialization based on interface
    // annotations)
    String json = mapper.writeValueAsString(toolCall);

    // Verify type field is present
    assertTrue(
        json.contains("\"type\":\"function_call\"") || json.contains("\"type\": \"function_call\""),
        "Serialized JSON should contain type field with value 'function_call'. Actual JSON:"
            + " "
            + json);
  }

  @Test
  void reasoningSerializesWithTypeField() throws Exception {
    // Create a Reasoning object
    Reasoning reasoning =
        new Reasoning(
            "reasoning_123", Collections.emptyList(), null, null, ReasoningStatus.COMPLETED);

    // Serialize directly (Jackson will use polymorphic serialization based on interface
    // annotations)
    String json = mapper.writeValueAsString(reasoning);

    // Verify type field is present
    assertTrue(
        json.contains("\"type\":\"reasoning\"") || json.contains("\"type\": \"reasoning\""),
        "Serialized JSON should contain type field with value 'reasoning'");
  }

  @Test
  void mcpApprovalRequestSerializesWithTypeField() throws Exception {
    // Create an McpApprovalRequest
    McpApprovalRequest request =
        new McpApprovalRequest("{\"param\":\"value\"}", "approval_123", "test_tool", "server_1");

    // Serialize directly (Jackson will use polymorphic serialization based on interface
    // annotations)
    String json = mapper.writeValueAsString(request);

    // Verify type field is present
    assertTrue(
        json.contains("\"type\":\"mcp_approval_request\"")
            || json.contains("\"type\": \"mcp_approval_request\""),
        "Serialized JSON should contain type field with value 'mcp_approval_request'");
  }

  @Test
  void deserializesFunctionToolCallFromJson() throws Exception {
    // JSON with type discriminator
    String json =
        """
        {
            "type": "function_call",
            "id": "id_123",
            "call_id": "call_123",
            "name": "test_function",
            "arguments": "{\\"arg\\":\\"value\\"}",
            "status": "completed"
        }
        """;

    // Deserialize as ResponseOutput
    ResponseOutput output = mapper.readValue(json, ResponseOutput.class);

    // Verify correct type
    assertInstanceOf(
        FunctionToolCall.class,
        output,
        "Should deserialize to FunctionToolCall based on type field");
  }

  @Test
  void deserializesReasoningFromJson() throws Exception {
    // JSON with type discriminator
    String json =
        """
        {
            "type": "reasoning",
            "id": "reasoning_123",
            "summary": [],
            "status": "completed"
        }
        """;

    // Deserialize as ResponseOutput
    ResponseOutput output = mapper.readValue(json, ResponseOutput.class);

    // Verify correct type
    assertInstanceOf(
        Reasoning.class, output, "Should deserialize to Reasoning based on type field");
  }
}
