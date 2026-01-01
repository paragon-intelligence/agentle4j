package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for FileSearchToolCall DTO (27 missed lines).
 */
@DisplayName("FileSearchToolCall DTO")
class FileSearchToolCallTest {

  @Nested
  @DisplayName("Constructor and Accessors")
  class ConstructorAndAccessors {

    @Test
    @DisplayName("constructor creates instance with all fields")
    void constructorCreatesInstance() {
      FileSearchToolCall call = new FileSearchToolCall(
          "fs_123",
          List.of("query1", "query2"),
          FileSearchToolCallStatus.IN_PROGRESS,
          null
      );

      assertEquals("fs_123", call.id());
      assertEquals(2, call.queries().size());
      assertEquals(FileSearchToolCallStatus.IN_PROGRESS, call.status());
    }

    @Test
    @DisplayName("id returns non-null value")
    void idReturnsValue() {
      FileSearchToolCall call = createFileSearchToolCall();
      assertEquals("fs_123", call.id());
    }

    @Test
    @DisplayName("queries returns list")
    void queriesReturnsList() {
      FileSearchToolCall call = createFileSearchToolCall();
      assertNotNull(call.queries());
      assertEquals(2, call.queries().size());
    }

    @Test
    @DisplayName("status returns value")
    void statusReturnsValue() {
      FileSearchToolCall call = createFileSearchToolCall();
      assertEquals(FileSearchToolCallStatus.SEARCHING, call.status());
    }

    @Test
    @DisplayName("fileSearchToolCallResultList can be null")
    void resultListCanBeNull() {
      FileSearchToolCall call = createFileSearchToolCall();
      assertNull(call.fileSearchToolCallResultList());
    }
  }

  @Nested
  @DisplayName("Equals and HashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same values")
    void equalsReturnsTrueForSame() {
      FileSearchToolCall call1 = createFileSearchToolCall();
      FileSearchToolCall call2 = createFileSearchToolCall();
      assertEquals(call1, call2);
    }

    @Test
    @DisplayName("equals returns false for different id")
    void equalsReturnsFalseForDifferentId() {
      FileSearchToolCall call1 = createFileSearchToolCall();
      FileSearchToolCall call2 = new FileSearchToolCall(
          "different_id",
          List.of("query1", "query2"),
          FileSearchToolCallStatus.SEARCHING,
          null
      );
      assertNotEquals(call1, call2);
    }

    @Test
    @DisplayName("hashCode is consistent")
    void hashCodeConsistent() {
      FileSearchToolCall call1 = createFileSearchToolCall();
      FileSearchToolCall call2 = createFileSearchToolCall();
      assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSameInstance() {
      FileSearchToolCall call = createFileSearchToolCall();
      assertEquals(call, call);
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsReturnsFalseForNull() {
      FileSearchToolCall call = createFileSearchToolCall();
      assertNotEquals(call, null);
    }

    @Test
    @DisplayName("equals returns false for different class")
    void equalsReturnsFalseForDifferentClass() {
      FileSearchToolCall call = createFileSearchToolCall();
      assertNotEquals(call, "string");
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("toString contains query info")
    void toStringContainsQueries() {
      FileSearchToolCall call = createFileSearchToolCall();
      String str = call.toString();
      assertTrue(str.contains("query1"));
    }

    @Test
    @DisplayName("toString works with empty results")
    void toStringWithEmptyResults() {
      FileSearchToolCall call = new FileSearchToolCall(
          "fs_123",
          List.of("single query"),
          FileSearchToolCallStatus.INCOMPLETE,
          null
      );
      String str = call.toString();
      assertTrue(str.contains("single query"));
    }

    @Test
    @DisplayName("toString with results list")
    void toStringWithResults() {
      FileSearchToolCall call = new FileSearchToolCall(
          "fs_123",
          List.of("query"),
          FileSearchToolCallStatus.FAILED,
          List.of()
      );
      String str = call.toString();
      assertNotNull(str);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FILE SEARCH TOOL CALL RESULT (to push past 80%)
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FileSearchToolCallResult Record")
  class FileSearchToolCallResultTests {

    @Test
    @DisplayName("toString formats all fields")
    void toStringFormatsAllFields() {
      FileSearchToolCallResult result = new FileSearchToolCallResult(
          java.util.Map.of("key", "value"),
          "file_123",
          "document.pdf",
          0.95,
          "Sample text content"
      );

      String str = result.toString();
      assertTrue(str.contains("file_123"));
      assertTrue(str.contains("document.pdf"));
      assertTrue(str.contains("0.95"));
      assertTrue(str.contains("Sample text content"));
    }

    @Test
    @DisplayName("toString handles null fields")
    void toStringHandlesNullFields() {
      FileSearchToolCallResult result = new FileSearchToolCallResult(
          null, null, null, null, null
      );

      String str = result.toString();
      assertTrue(str.contains("null"));
    }
  }

  private FileSearchToolCall createFileSearchToolCall() {
    return new FileSearchToolCall(
        "fs_123",
        List.of("query1", "query2"),
        FileSearchToolCallStatus.SEARCHING,
        null
    );
  }
}
