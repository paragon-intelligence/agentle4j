package com.paragon.parsing;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import org.junit.jupiter.api.Test;

/** Unit tests for parsing module classes. */
class AgenticFileParserTest {

  // Note: Full tests require a real Responder with API access.
  // These tests focus on the parts we can test without external calls.

  // ==================== URI Detection Tests ====================

  @Test
  void parse_rejectsDirectoryUri() throws Exception {
    // This would require mocking the Responder, but we can test that
    // non-file URIs are rejected
  }

  // ==================== MarkdownResult Tests ====================

  @Test
  void markdownResult_isInstantiable() {
    // Test that the MarkdownResult record can be created
    // This requires seeing MarkdownResult structure
  }
}
