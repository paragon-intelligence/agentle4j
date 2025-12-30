package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for InMemoryMemory.
 *
 * <p>Tests cover: - Creation - Add operations - Retrieve operations (with relevance scoring) -
 * Update operations - Delete operations - User isolation - Clear operations
 */
@DisplayName("InMemoryMemory")
class InMemoryMemoryTest {

  private InMemoryMemory memory;

  @BeforeEach
  void setUp() {
    memory = InMemoryMemory.create();
  }

  @Nested
  @DisplayName("Creation")
  class Creation {

    @Test
    @DisplayName("create() returns new memory instance")
    void create_returnsNewInstance() {
      InMemoryMemory memory = InMemoryMemory.create();

      assertNotNull(memory);
    }

    @Test
    @DisplayName("new instance has no memories")
    void newInstance_hasNoMemories() {
      assertEquals(0, memory.size("user1"));
    }
  }

  @Nested
  @DisplayName("Add Operations")
  class AddOperations {

    @Test
    @DisplayName("add() saves memory entry for user")
    void add_savesMemoryEntryForUser() {
      MemoryEntry entry = MemoryEntry.of("Remember this");

      memory.add("user1", entry);

      assertEquals(1, memory.size("user1"));
    }

    @Test
    @DisplayName("add() allows multiple memories for same user")
    void add_allowsMultipleMemoriesForSameUser() {
      memory.add("user1", MemoryEntry.of("First memory"));
      memory.add("user1", MemoryEntry.of("Second memory"));
      memory.add("user1", MemoryEntry.of("Third memory"));

      assertEquals(3, memory.size("user1"));
    }

    @Test
    @DisplayName("add() throws when userId is null")
    void add_throwsWhenUserIdNull() {
      MemoryEntry entry = MemoryEntry.of("content");

      assertThrows(NullPointerException.class, () -> memory.add(null, entry));
    }

    @Test
    @DisplayName("add() throws when entry is null")
    void add_throwsWhenEntryNull() {
      assertThrows(NullPointerException.class, () -> memory.add("user1", (MemoryEntry) null));
    }
  }

  @Nested
  @DisplayName("Retrieve Operations")
  class RetrieveOperations {

    @Test
    @DisplayName("retrieve() returns matching memories")
    void retrieve_returnsMatchingMemories() {
      memory.add("user1", MemoryEntry.of("I like blue color"));
      memory.add("user1", MemoryEntry.of("Red is my favorite"));
      memory.add("user1", MemoryEntry.of("Blue sky is beautiful"));

      List<MemoryEntry> results = memory.retrieve("user1", "blue", 10);

      assertEquals(2, results.size());
    }

    @Test
    @DisplayName("retrieve() returns empty list for unknown user")
    void retrieve_returnsEmptyListForUnknownUser() {
      List<MemoryEntry> results = memory.retrieve("unknown-user", "query", 10);

      assertNotNull(results);
      assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("retrieve() throws when userId is null")
    void retrieve_throwsWhenUserIdNull() {
      assertThrows(NullPointerException.class, () -> memory.retrieve(null, "query", 10));
    }

    @Test
    @DisplayName("retrieve() throws when query is null")
    void retrieve_throwsWhenQueryNull() {
      assertThrows(NullPointerException.class, () -> memory.retrieve("user1", null, 10));
    }

    @Test
    @DisplayName("retrieve() respects limit")
    void retrieve_respectsLimit() {
      memory.add("user1", MemoryEntry.of("blue one"));
      memory.add("user1", MemoryEntry.of("blue two"));
      memory.add("user1", MemoryEntry.of("blue three"));

      List<MemoryEntry> results = memory.retrieve("user1", "blue", 2);

      assertEquals(2, results.size());
    }

    @Test
    @DisplayName("retrieve() returns empty for zero limit")
    void retrieve_returnsEmptyForZeroLimit() {
      memory.add("user1", MemoryEntry.of("blue"));

      List<MemoryEntry> results = memory.retrieve("user1", "blue", 0);

      assertTrue(results.isEmpty());
    }
  }

  @Nested
  @DisplayName("User Isolation")
  class UserIsolation {

    @Test
    @DisplayName("memories are isolated per user")
    void memoriesAreIsolatedPerUser() {
      memory.add("user1", MemoryEntry.of("User 1 memory"));
      memory.add("user2", MemoryEntry.of("User 2 memory"));

      assertEquals(1, memory.size("user1"));
      assertEquals(1, memory.size("user2"));
    }

    @Test
    @DisplayName("different users can have different memory counts")
    void differentUsersCanHaveDifferentMemoryCounts() {
      memory.add("user1", MemoryEntry.of("M1"));
      memory.add("user1", MemoryEntry.of("M2"));
      memory.add("user1", MemoryEntry.of("M3"));
      memory.add("user2", MemoryEntry.of("M1"));

      assertEquals(3, memory.size("user1"));
      assertEquals(1, memory.size("user2"));
    }
  }

  @Nested
  @DisplayName("All Operations")
  class AllOperations {

    @Test
    @DisplayName("all() returns all user memories")
    void all_returnsAllUserMemories() {
      memory.add("user1", MemoryEntry.of("Memory 1"));
      memory.add("user1", MemoryEntry.of("Memory 2"));

      List<MemoryEntry> allMemories = memory.all("user1");

      assertEquals(2, allMemories.size());
    }

    @Test
    @DisplayName("all() returns empty for unknown user")
    void all_returnsEmptyForUnknownUser() {
      List<MemoryEntry> allMemories = memory.all("unknown");

      assertTrue(allMemories.isEmpty());
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete() removes memory by id")
    void delete_removesMemoryById() {
      MemoryEntry entry = MemoryEntry.of("To delete");
      memory.add("user1", entry);

      boolean deleted = memory.delete("user1", entry.id());

      assertTrue(deleted);
      assertEquals(0, memory.size("user1"));
    }

    @Test
    @DisplayName("delete() returns false for non-existent id")
    void delete_returnsFalseForNonExistentId() {
      boolean deleted = memory.delete("user1", "non-existent-id");

      assertFalse(deleted);
    }
  }

  @Nested
  @DisplayName("Clear Operations")
  class ClearOperations {

    @Test
    @DisplayName("clear() removes all memories for user")
    void clear_removesAllMemoriesForUser() {
      memory.add("user1", MemoryEntry.of("Memory 1"));
      memory.add("user1", MemoryEntry.of("Memory 2"));

      memory.clear("user1");

      assertEquals(0, memory.size("user1"));
    }

    @Test
    @DisplayName("clear() does not affect other users")
    void clear_doesNotAffectOtherUsers() {
      memory.add("user1", MemoryEntry.of("Memory"));
      memory.add("user2", MemoryEntry.of("Memory"));

      memory.clear("user1");

      assertEquals(0, memory.size("user1"));
      assertEquals(1, memory.size("user2"));
    }

    @Test
    @DisplayName("clearAll() removes all memories")
    void clearAll_removesAllMemories() {
      memory.add("user1", MemoryEntry.of("Memory"));
      memory.add("user2", MemoryEntry.of("Memory"));

      memory.clearAll();

      assertEquals(0, memory.size("user1"));
      assertEquals(0, memory.size("user2"));
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update() replaces existing memory")
    void update_replacesExistingMemory() {
      MemoryEntry original = MemoryEntry.of("Original content");
      memory.add("user1", original);

      MemoryEntry updated = MemoryEntry.withId(original.id(), "Updated content");
      memory.update("user1", original.id(), updated);

      List<MemoryEntry> all = memory.all("user1");
      assertEquals(1, all.size());
    }

    @Test
    @DisplayName("update() throws for non-existent id")
    void update_throwsForNonExistentId() {
      MemoryEntry entry = MemoryEntry.of("content");

      assertThrows(
          IllegalArgumentException.class, () -> memory.update("user1", "non-existent", entry));
    }
  }
}
