package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for McpToolCall DTO (27 missed lines).
 */
@DisplayName("McpToolCall DTO")
class McpToolCallTest {

  @Nested
  @DisplayName("Constructor and Accessors")
  class ConstructorAndAccessors {

    @Test
    @DisplayName("constructor creates instance with all fields")
    void constructorCreatesInstance() {
      McpToolCall call = new McpToolCall(
          "{\"key\":\"value\"}", // arguments
          "mcp_123",            // id
          "search_files",       // name
          "server1",            // serverLabel
          "approval_456",       // approvalRequestId
          null,                 // error
          "search results",     // output
          McpToolCallStatus.COMPLETED
      );

      assertEquals("mcp_123", call.id());
      assertEquals("search_files", call.name());
      assertNull(call.error());
      assertEquals("search results", call.output());
      assertEquals(McpToolCallStatus.COMPLETED, call.status());
    }

    @Test
    @DisplayName("id returns non-null")
    void idReturnsValue() {
      McpToolCall call = createMcpToolCall();
      assertNotNull(call.id());
      assertEquals("mcp_123", call.id());
    }

    @Test
    @DisplayName("name returns non-null")
    void nameReturnsValue() {
      McpToolCall call = createMcpToolCall();
      assertNotNull(call.name());
      assertEquals("search_files", call.name());
    }

    @Test
    @DisplayName("error can be null")
    void errorCanBeNull() {
      McpToolCall call = createMcpToolCall();
      assertNull(call.error());
    }

    @Test
    @DisplayName("output returns value")
    void outputReturnsValue() {
      McpToolCall call = createMcpToolCall();
      assertEquals("output", call.output());
    }

    @Test
    @DisplayName("output can be null")
    void outputCanBeNull() {
      McpToolCall call = new McpToolCall(
          "{}", "id", "name", "server", null, null, null, null);
      assertNull(call.output());
    }

    @Test
    @DisplayName("status returns value")
    void statusReturnsValue() {
      McpToolCall call = createMcpToolCall();
      assertEquals(McpToolCallStatus.COMPLETED, call.status());
    }

    @Test
    @DisplayName("status can be null")
    void statusCanBeNull() {
      McpToolCall call = new McpToolCall(
          "{}", "id", "name", "server", null, null, null, null);
      assertNull(call.status());
    }
  }

  @Nested
  @DisplayName("Equals and HashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same values")
    void equalsReturnsTrueForSameValues() {
      McpToolCall call1 = createMcpToolCall();
      McpToolCall call2 = createMcpToolCall();
      assertEquals(call1, call2);
    }

    @Test
    @DisplayName("equals returns false for different values")
    void equalsReturnsFalseForDifferent() {
      McpToolCall call1 = createMcpToolCall();
      McpToolCall call2 = new McpToolCall("{}", "different", "name", "server", null, null, null, null);
      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("hashCode is consistent with equals")
    void hashCodeConsistent() {
      McpToolCall call1 = createMcpToolCall();
      McpToolCall call2 = createMcpToolCall();
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSameInstance() {
      McpToolCall call = createMcpToolCall();
      assertEquals(call, call);
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsReturnsFalseForNull() {
      McpToolCall call = createMcpToolCall();
      assertNotEquals(call, null);
    }

    @Test
    @DisplayName("equals returns false for different class")
    void equalsReturnsFalseForDifferentClass() {
      McpToolCall call = createMcpToolCall();
      assertNotEquals(call, "string");
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("toString contains id and name")
    void toStringContainsFields() {
      McpToolCall call = createMcpToolCall();
      String str = call.toString();
      assertTrue(str.contains("mcp_123"));
      assertTrue(str.contains("search_files"));
    }

    @Test
    @DisplayName("toString contains all fields")
    void toStringContainsAllFields() {
      McpToolCall call = createMcpToolCall();
      String str = call.toString();
      assertTrue(str.contains("McpToolCall"));
      assertTrue(str.contains("server1"));
    }
  }

  private McpToolCall createMcpToolCall() {
    return new McpToolCall(
        "{\"key\":\"value\"}",
        "mcp_123",
        "search_files",
        "server1",
        null,
        null,
        "output",
        McpToolCallStatus.COMPLETED
    );
  }
}
