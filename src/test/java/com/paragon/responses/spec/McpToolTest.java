package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for McpTool.
 *
 * <p>Tests cover: - Record construction with all fields - toToolChoice() serialization - toString()
 * formatting - Record equality
 */
@DisplayName("McpTool")
class McpToolTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // ═══════════════════════════════════════════════════════════════════════════
  // CONSTRUCTION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("creates with required fields only")
    void createsWithRequiredFieldsOnly() {
      McpTool tool = new McpTool("my-server", null, null, null, null, null, null, null);

      assertNotNull(tool);
      assertEquals("my-server", tool.serverLabel());
    }

    @Test
    @DisplayName("creates with all fields populated")
    void createsWithAllFieldsPopulated() {
      McpToolFilter allowedTools = new McpToolFilter(true, List.of("tool1", "tool2"));
      Map<String, String> headers = Map.of("Authorization", "Bearer token");

      McpTool tool =
          new McpTool(
              "my-server",
              allowedTools,
              "access-token",
              "connector_gmail",
              headers,
              null,
              "Test server",
              "https://mcp.example.com");

      assertNotNull(tool);
      assertEquals("my-server", tool.serverLabel());
      assertEquals(allowedTools, tool.allowedTools());
      assertEquals("access-token", tool.authorization());
      assertEquals("connector_gmail", tool.connectorId());
      assertEquals(headers, tool.headers());
      assertEquals("Test server", tool.serverDescription());
      assertEquals("https://mcp.example.com", tool.serverUrl());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSOR TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @Test
    @DisplayName("serverLabel returns label")
    void serverLabelReturnsLabel() {
      McpTool tool = new McpTool("my-label", null, null, null, null, null, null, null);
      assertEquals("my-label", tool.serverLabel());
    }

    @Test
    @DisplayName("allowedTools returns null when not set")
    void allowedToolsReturnsNullWhenNotSet() {
      McpTool tool = new McpTool("server", null, null, null, null, null, null, null);
      assertNull(tool.allowedTools());
    }

    @Test
    @DisplayName("authorization returns value when set")
    void authorizationReturnsValueWhenSet() {
      McpTool tool = new McpTool("server", null, "auth-token", null, null, null, null, null);
      assertEquals("auth-token", tool.authorization());
    }

    @Test
    @DisplayName("connectorId returns value when set")
    void connectorIdReturnsValueWhenSet() {
      McpTool tool = new McpTool("server", null, null, "connector_dropbox", null, null, null, null);
      assertEquals("connector_dropbox", tool.connectorId());
    }

    @Test
    @DisplayName("headers returns value when set")
    void headersReturnsValueWhenSet() {
      Map<String, String> headers = Map.of("X-Custom", "value");
      McpTool tool = new McpTool("server", null, null, null, headers, null, null, null);
      assertEquals(headers, tool.headers());
    }

    @Test
    @DisplayName("serverDescription returns value when set")
    void serverDescriptionReturnsValueWhenSet() {
      McpTool tool = new McpTool("server", null, null, null, null, null, "My description", null);
      assertEquals("My description", tool.serverDescription());
    }

    @Test
    @DisplayName("serverUrl returns value when set")
    void serverUrlReturnsValueWhenSet() {
      McpTool tool =
          new McpTool("server", null, null, null, null, null, null, "https://mcp.test.com");
      assertEquals("https://mcp.test.com", tool.serverUrl());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOTOOLCHOICE TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("toToolChoice()")
  class ToToolChoiceTests {

    @Test
    @DisplayName("toToolChoice without allowedTools returns server_label and type")
    void toToolChoiceWithoutAllowedToolsReturnsServerLabelAndType() throws JsonProcessingException {
      McpTool tool = new McpTool("my-server", null, null, null, null, null, null, null);
      String choice = tool.toToolChoice(objectMapper);

      assertNotNull(choice);
      Map<?, ?> parsed = objectMapper.readValue(choice, Map.class);
      assertEquals("my-server", parsed.get("server_label"));
      assertEquals("mcp", parsed.get("type"));
      assertNull(parsed.get("name"));
    }

    @Test
    @DisplayName("toToolChoice with allowedTools includes name")
    void toToolChoiceWithAllowedToolsIncludesName() throws JsonProcessingException {
      McpToolFilter filter = new McpToolFilter(true, List.of("tool_one", "tool_two"));
      McpTool tool = new McpTool("my-server", filter, null, null, null, null, null, null);
      String choice = tool.toToolChoice(objectMapper);

      assertNotNull(choice);
      Map<?, ?> parsed = objectMapper.readValue(choice, Map.class);
      assertEquals("my-server", parsed.get("server_label"));
      assertEquals("mcp", parsed.get("type"));
      assertEquals("tool_one", parsed.get("name")); // First tool name
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOSTRING TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("toString()")
  class ToStringTests {

    @Test
    @DisplayName("toString contains serverLabel")
    void toStringContainsServerLabel() {
      McpTool tool = new McpTool("test-server", null, null, null, null, null, null, null);
      String str = tool.toString();

      assertTrue(str.contains("test-server"));
    }

    @Test
    @DisplayName("toString contains McpTool prefix")
    void toStringContainsMcpToolPrefix() {
      McpTool tool = new McpTool("server", null, null, null, null, null, null, null);
      String str = tool.toString();

      assertTrue(str.startsWith("McpTool["));
    }

    @Test
    @DisplayName("toString contains all field names")
    void toStringContainsAllFieldNames() {
      McpTool tool = new McpTool("server", null, null, null, null, null, null, "https://test.com");
      String str = tool.toString();

      assertTrue(str.contains("serverLabel="));
      assertTrue(str.contains("serverUrl="));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EQUALITY TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Equality")
  class EqualityTests {

    @Test
    @DisplayName("equal tools are equal")
    void equalToolsAreEqual() {
      McpTool t1 = new McpTool("server", null, null, null, null, null, null, null);
      McpTool t2 = new McpTool("server", null, null, null, null, null, null, null);

      assertEquals(t1, t2);
      assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    @DisplayName("different serverLabels are not equal")
    void differentServerLabelsAreNotEqual() {
      McpTool t1 = new McpTool("server1", null, null, null, null, null, null, null);
      McpTool t2 = new McpTool("server2", null, null, null, null, null, null, null);

      assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("different serverUrls are not equal")
    void differentServerUrlsAreNotEqual() {
      McpTool t1 = new McpTool("server", null, null, null, null, null, null, "https://a.com");
      McpTool t2 = new McpTool("server", null, null, null, null, null, null, "https://b.com");

      assertNotEquals(t1, t2);
    }
  }
}
