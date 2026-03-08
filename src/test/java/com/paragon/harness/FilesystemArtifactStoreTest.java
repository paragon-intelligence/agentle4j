package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FilesystemArtifactStore")
class FilesystemArtifactStoreTest {

  @TempDir Path tempDir;
  FilesystemArtifactStore store;

  @BeforeEach
  void setUp() {
    store = FilesystemArtifactStore.create(tempDir);
  }

  @Nested
  @DisplayName("write and read")
  class WriteAndRead {

    @Test
    @DisplayName("writes artifact and reads it back")
    void writesAndReadsLatest() {
      String version = store.write("schema.sql", "CREATE TABLE users (id INT)");
      assertNotNull(version);
      assertFalse(version.isBlank());

      Optional<String> content = store.read("schema.sql");
      assertTrue(content.isPresent());
      assertEquals("CREATE TABLE users (id INT)", content.get());
    }

    @Test
    @DisplayName("returns empty for non-existent artifact")
    void returnsEmptyForMissing() {
      assertTrue(store.read("nonexistent.txt").isEmpty());
    }

    @Test
    @DisplayName("reads specific version")
    void readsSpecificVersion() throws InterruptedException {
      String v1 = store.write("doc.md", "Version 1 content");
      Thread.sleep(2); // ensure different timestamps
      store.write("doc.md", "Version 2 content");

      Optional<String> v1Content = store.read("doc.md", v1);
      assertTrue(v1Content.isPresent());
      assertEquals("Version 1 content", v1Content.get());
    }

    @Test
    @DisplayName("latest read returns newest version")
    void latestReadReturnsNewest() throws InterruptedException {
      store.write("notes.txt", "first");
      Thread.sleep(2);
      store.write("notes.txt", "second");

      Optional<String> latest = store.read("notes.txt");
      assertTrue(latest.isPresent());
      assertEquals("second", latest.get());
    }
  }

  @Nested
  @DisplayName("list")
  class ListArtifacts {

    @Test
    @DisplayName("lists all artifact names")
    void listsAllNames() {
      store.write("alpha.md", "a");
      store.write("beta.md", "b");

      List<String> names = store.list();
      assertEquals(2, names.size());
      assertTrue(names.contains("alpha.md"));
      assertTrue(names.contains("beta.md"));
    }

    @Test
    @DisplayName("returns empty list when store is empty")
    void returnsEmptyWhenEmpty() {
      assertTrue(store.list().isEmpty());
    }
  }

  @Nested
  @DisplayName("versions")
  class Versions {

    @Test
    @DisplayName("returns versions in chronological order")
    void versionsInOrder() throws InterruptedException {
      String v1 = store.write("x.txt", "v1");
      Thread.sleep(2);
      String v2 = store.write("x.txt", "v2");

      List<String> versions = store.versions("x.txt");
      assertEquals(2, versions.size());
      assertEquals(v1, versions.get(0));
      assertEquals(v2, versions.get(1));
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("deletes artifact and returns true")
    void deletesAndReturnsTrue() {
      store.write("temp.txt", "data");
      assertTrue(store.delete("temp.txt"));
      assertTrue(store.read("temp.txt").isEmpty());
    }

    @Test
    @DisplayName("returns false for non-existent artifact")
    void returnsFalseForMissing() {
      assertFalse(store.delete("ghost.txt"));
    }
  }
}
