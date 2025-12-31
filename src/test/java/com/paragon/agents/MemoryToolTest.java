package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link MemoryTool} and all its inner tool classes. */
@DisplayName("MemoryTool")
class MemoryToolTest {

  private Memory memory;

  @BeforeEach
  void setUp() {
    memory = InMemoryMemory.create();
  }

  // ==================== all() Factory Method ====================

  @Nested
  @DisplayName("all() Factory")
  class AllFactoryTests {

    @Test
    @DisplayName("creates all four memory tools")
    void createsAllFourTools() {
      List<FunctionTool<?>> tools = MemoryTool.all(memory);

      assertEquals(4, tools.size());
      assertInstanceOf(MemoryTool.AddMemoryTool.class, tools.get(0));
      assertInstanceOf(MemoryTool.RetrieveMemoriesTool.class, tools.get(1));
      assertInstanceOf(MemoryTool.UpdateMemoryTool.class, tools.get(2));
      assertInstanceOf(MemoryTool.DeleteMemoryTool.class, tools.get(3));
    }

    @Test
    @DisplayName("throws on null memory")
    void throwsOnNullMemory() {
      assertThrows(NullPointerException.class, () -> MemoryTool.all(null));
    }
  }

  // ==================== AddMemoryTool ====================

  @Nested
  @DisplayName("AddMemoryTool")
  class AddMemoryToolTests {

    private MemoryTool.AddMemoryTool tool;

    @BeforeEach
    void setUp() {
      tool = new MemoryTool.AddMemoryTool(memory);
    }

    @Test
    @DisplayName("returns error when userId not set")
    void returnsErrorWhenUserIdNotSet() {
      MemoryTool.AddMemoryRequest request = new MemoryTool.AddMemoryRequest("test content");

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("userId not set"));
    }

    @Test
    @DisplayName("returns error when params null")
    void returnsErrorWhenParamsNull() {
      tool.setUserId("user123");

      FunctionToolCallOutput result = tool.call(null);

      assertNotNull(result);
      assertTrue(result.toString().contains("no content provided"));
    }

    @Test
    @DisplayName("successfully adds memory")
    void successfullyAddsMemory() {
      tool.setUserId("user123");
      MemoryTool.AddMemoryRequest request = new MemoryTool.AddMemoryRequest("Remember this");

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("Memory stored successfully"));
    }
  }

  // ==================== RetrieveMemoriesTool ====================

  @Nested
  @DisplayName("RetrieveMemoriesTool")
  class RetrieveMemoriesToolTests {

    private MemoryTool.RetrieveMemoriesTool tool;

    @BeforeEach
    void setUp() {
      tool = new MemoryTool.RetrieveMemoriesTool(memory);
    }

    @Test
    @DisplayName("returns error when userId not set")
    void returnsErrorWhenUserIdNotSet() {
      MemoryTool.RetrieveMemoriesRequest request =
          new MemoryTool.RetrieveMemoriesRequest("query", null);

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("userId not set"));
    }

    @Test
    @DisplayName("returns error when params null")
    void returnsErrorWhenParamsNull() {
      tool.setUserId("user123");

      FunctionToolCallOutput result = tool.call(null);

      assertNotNull(result);
      assertTrue(result.toString().contains("no query provided"));
    }

    @Test
    @DisplayName("returns empty result when no memories")
    void returnsEmptyResultWhenNoMemories() {
      tool.setUserId("user123");
      MemoryTool.RetrieveMemoriesRequest request =
          new MemoryTool.RetrieveMemoriesRequest("any query", 5);

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("No relevant memories found"));
    }

    @Test
    @DisplayName("retrieves stored memories")
    void retrievesStoredMemories() {
      String userId = "user123";
      memory.add(userId, MemoryEntry.of("Important fact"));
      tool.setUserId(userId);

      MemoryTool.RetrieveMemoriesRequest request =
          new MemoryTool.RetrieveMemoriesRequest("fact", null);

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("Found"));
    }
  }

  // ==================== UpdateMemoryTool ====================

  @Nested
  @DisplayName("UpdateMemoryTool")
  class UpdateMemoryToolTests {

    private MemoryTool.UpdateMemoryTool tool;

    @BeforeEach
    void setUp() {
      tool = new MemoryTool.UpdateMemoryTool(memory);
    }

    @Test
    @DisplayName("returns error when userId not set")
    void returnsErrorWhenUserIdNotSet() {
      MemoryTool.UpdateMemoryRequest request =
          new MemoryTool.UpdateMemoryRequest("id", "new content");

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("userId not set"));
    }

    @Test
    @DisplayName("returns error when params null")
    void returnsErrorWhenParamsNull() {
      tool.setUserId("user123");

      FunctionToolCallOutput result = tool.call(null);

      assertNotNull(result);
      assertTrue(result.toString().contains("no parameters provided"));
    }

    @Test
    @DisplayName("successfully updates memory")
    void successfullyUpdatesMemory() {
      String userId = "user123";
      MemoryEntry entry = MemoryEntry.of("Original content");
      memory.add(userId, entry);
      tool.setUserId(userId);

      MemoryTool.UpdateMemoryRequest request =
          new MemoryTool.UpdateMemoryRequest(entry.id(), "Updated content");

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("Memory updated successfully"));
    }
  }

  // ==================== DeleteMemoryTool ====================

  @Nested
  @DisplayName("DeleteMemoryTool")
  class DeleteMemoryToolTests {

    private MemoryTool.DeleteMemoryTool tool;

    @BeforeEach
    void setUp() {
      tool = new MemoryTool.DeleteMemoryTool(memory);
    }

    @Test
    @DisplayName("returns error when userId not set")
    void returnsErrorWhenUserIdNotSet() {
      MemoryTool.DeleteMemoryRequest request = new MemoryTool.DeleteMemoryRequest("id");

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("userId not set"));
    }

    @Test
    @DisplayName("returns error when params null")
    void returnsErrorWhenParamsNull() {
      tool.setUserId("user123");

      FunctionToolCallOutput result = tool.call(null);

      assertNotNull(result);
      assertTrue(result.toString().contains("no id provided"));
    }

    @Test
    @DisplayName("returns not found for non-existent memory")
    void returnsNotFoundForNonExistentMemory() {
      tool.setUserId("user123");
      MemoryTool.DeleteMemoryRequest request =
          new MemoryTool.DeleteMemoryRequest("non-existent-id");

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("not found"));
    }

    @Test
    @DisplayName("successfully deletes memory")
    void successfullyDeletesMemory() {
      String userId = "user123";
      MemoryEntry entry = MemoryEntry.of("To be deleted");
      memory.add(userId, entry);
      tool.setUserId(userId);

      MemoryTool.DeleteMemoryRequest request = new MemoryTool.DeleteMemoryRequest(entry.id());

      FunctionToolCallOutput result = tool.call(request);

      assertNotNull(result);
      assertTrue(result.toString().contains("Memory deleted successfully"));
    }
  }

  // ==================== Request Record Tests ====================

  @Nested
  @DisplayName("Request Records")
  class RequestRecordTests {

    @Test
    @DisplayName("AddMemoryRequest stores content")
    void addMemoryRequestStoresContent() {
      MemoryTool.AddMemoryRequest request = new MemoryTool.AddMemoryRequest("test content");
      assertEquals("test content", request.content());
    }

    @Test
    @DisplayName("RetrieveMemoriesRequest stores query and limit")
    void retrieveMemoriesRequestStoresQueryAndLimit() {
      MemoryTool.RetrieveMemoriesRequest request =
          new MemoryTool.RetrieveMemoriesRequest("query", 10);
      assertEquals("query", request.query());
      assertEquals(10, request.limit());
    }

    @Test
    @DisplayName("UpdateMemoryRequest stores id and content")
    void updateMemoryRequestStoresIdAndContent() {
      MemoryTool.UpdateMemoryRequest request =
          new MemoryTool.UpdateMemoryRequest("id123", "new content");
      assertEquals("id123", request.id());
      assertEquals("new content", request.content());
    }

    @Test
    @DisplayName("DeleteMemoryRequest stores id")
    void deleteMemoryRequestStoresId() {
      MemoryTool.DeleteMemoryRequest request = new MemoryTool.DeleteMemoryRequest("id123");
      assertEquals("id123", request.id());
    }
  }
}
