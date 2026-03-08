package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.FilesystemMemory;
import com.paragon.agents.MemoryEntry;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FilesystemMemory")
class FilesystemMemoryTest {

  @TempDir Path tempDir;
  FilesystemMemory memory;

  @BeforeEach
  void setUp() {
    memory = FilesystemMemory.create(tempDir);
  }

  @Nested
  @DisplayName("add and retrieve")
  class AddAndRetrieve {

    @Test
    @DisplayName("stores and retrieves by keyword")
    void storesAndRetrievesByKeyword() {
      memory.add("user1", "The user prefers dark mode");
      List<MemoryEntry> results = memory.retrieve("user1", "dark mode", 5);
      assertEquals(1, results.size());
      assertTrue(results.get(0).content().contains("dark mode"));
    }

    @Test
    @DisplayName("returns empty list when no match")
    void returnsEmptyWhenNoMatch() {
      memory.add("user1", "Java is great");
      List<MemoryEntry> results = memory.retrieve("user1", "python", 5);
      assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("isolates memories by userId")
    void isolatesMemoriesByUserId() {
      memory.add("user1", "user1 secret");
      memory.add("user2", "user2 data");

      List<MemoryEntry> user1Memories = memory.all("user1");
      List<MemoryEntry> user2Memories = memory.all("user2");

      assertEquals(1, user1Memories.size());
      assertEquals(1, user2Memories.size());
      assertTrue(user1Memories.get(0).content().contains("user1"));
      assertTrue(user2Memories.get(0).content().contains("user2"));
    }

    @Test
    @DisplayName("persists across re-creation (durable)")
    void persistsAcrossReCreation() {
      memory.add("userA", "Persisted memory content");

      // Re-create from same directory to simulate restart
      FilesystemMemory reloaded = FilesystemMemory.create(tempDir);
      List<MemoryEntry> results = reloaded.all("userA");

      assertEquals(1, results.size());
      assertEquals("Persisted memory content", results.get(0).content());
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("updates existing entry")
    void updatesExistingEntry() {
      MemoryEntry original = MemoryEntry.of("original content");
      memory.add("u1", original);

      MemoryEntry updated = MemoryEntry.withId(original.id(), "updated content");
      memory.update("u1", original.id(), updated);

      List<MemoryEntry> all = memory.all("u1");
      assertEquals(1, all.size());
      assertEquals("updated content", all.get(0).content());
    }

    @Test
    @DisplayName("throws when id not found")
    void throwsWhenIdNotFound() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.update("u1", "nonexistent-id", MemoryEntry.of("x")));
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("deletes existing entry and returns true")
    void deletesAndReturnsTrue() {
      MemoryEntry entry = MemoryEntry.of("to be deleted");
      memory.add("u1", entry);

      boolean deleted = memory.delete("u1", entry.id());

      assertTrue(deleted);
      assertEquals(0, memory.size("u1"));
    }

    @Test
    @DisplayName("returns false for nonexistent id")
    void returnsFalseForNonexistent() {
      assertFalse(memory.delete("u1", "no-such-id"));
    }
  }

  @Nested
  @DisplayName("clear")
  class Clear {

    @Test
    @DisplayName("clears all entries for a user")
    void clearsUserEntries() {
      memory.add("u1", "a");
      memory.add("u1", "b");
      memory.clear("u1");
      assertEquals(0, memory.size("u1"));
    }

    @Test
    @DisplayName("clearAll removes all users' memories")
    void clearAllRemovesEverything() {
      memory.add("u1", "a");
      memory.add("u2", "b");
      memory.clearAll();
      assertEquals(0, memory.size("u1"));
      assertEquals(0, memory.size("u2"));
    }
  }
}
