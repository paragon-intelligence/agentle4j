package com.paragon.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * Durable filesystem-backed implementation of {@link Memory}.
 *
 * <p>Persists each user's memories as a JSON file at {@code baseDir/{userId}.json}. Survives JVM
 * restarts and enables long-running, multi-session agent workflows.
 *
 * <p>Thread-safe via per-user read/write locks. Writes are atomic (write to temp file, then rename).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Memory memory = FilesystemMemory.create(Path.of("/var/agent-data/memory"));
 *
 * Agent agent = Agent.builder()
 *     .addMemoryTools(memory)
 *     .build();
 * }</pre>
 *
 * @see Memory
 * @see MemoryEntry
 * @since 1.0
 */
public final class FilesystemMemory implements Memory {

  private final Path baseDir;
  private final ObjectMapper objectMapper;
  // Per-user locks to allow concurrent access across different users
  private final Map<String, ReentrantReadWriteLock> userLocks = new ConcurrentHashMap<>();

  private FilesystemMemory(Path baseDir, ObjectMapper objectMapper) {
    this.baseDir = Objects.requireNonNull(baseDir, "baseDir cannot be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    try {
      Files.createDirectories(baseDir);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create memory base directory: " + baseDir, e);
    }
  }

  /**
   * Creates a FilesystemMemory storing data under {@code baseDir}.
   *
   * @param baseDir directory where per-user JSON files will be stored
   * @return a new FilesystemMemory instance
   */
  public static @NonNull FilesystemMemory create(@NonNull Path baseDir) {
    ObjectMapper mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new FilesystemMemory(baseDir, mapper);
  }

  /**
   * Creates a FilesystemMemory with a custom ObjectMapper.
   *
   * @param baseDir directory where per-user JSON files will be stored
   * @param objectMapper the Jackson mapper to use for serialization
   * @return a new FilesystemMemory instance
   */
  public static @NonNull FilesystemMemory create(
      @NonNull Path baseDir, @NonNull ObjectMapper objectMapper) {
    return new FilesystemMemory(baseDir, objectMapper);
  }

  @Override
  public void add(@NonNull String userId, @NonNull MemoryEntry entry) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(entry, "entry cannot be null");
    withWriteLock(userId, () -> {
      Map<String, MemoryEntry> storage = loadUserStorage(userId);
      storage.put(entry.id(), entry);
      saveUserStorage(userId, storage);
    });
  }

  @Override
  public @NonNull List<MemoryEntry> retrieve(
      @NonNull String userId, @NonNull String query, int limit) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(query, "query cannot be null");
    if (limit <= 0) {
      return List.of();
    }

    return withReadLock(userId, () -> {
      Map<String, MemoryEntry> storage = loadUserStorage(userId);
      if (storage.isEmpty()) {
        return List.of();
      }

      String queryLower = query.toLowerCase();
      return storage.values().stream()
          .map(e -> new ScoredEntry(e, scoreRelevance(e, queryLower)))
          .filter(s -> s.score > 0)
          .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed())
          .limit(limit)
          .map(ScoredEntry::entry)
          .collect(Collectors.toList());
    });
  }

  @Override
  public void update(@NonNull String userId, @NonNull String id, @NonNull MemoryEntry entry) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(entry, "entry cannot be null");
    withWriteLock(userId, () -> {
      Map<String, MemoryEntry> storage = loadUserStorage(userId);
      if (!storage.containsKey(id)) {
        throw new IllegalArgumentException(
            "Memory with id '" + id + "' not found for user '" + userId + "'");
      }
      storage.remove(id);
      storage.put(entry.id(), entry);
      saveUserStorage(userId, storage);
    });
  }

  @Override
  public boolean delete(@NonNull String userId, @NonNull String id) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(id, "id cannot be null");
    return withWriteLock(userId, () -> {
      Map<String, MemoryEntry> storage = loadUserStorage(userId);
      boolean removed = storage.remove(id) != null;
      if (removed) {
        saveUserStorage(userId, storage);
      }
      return removed;
    });
  }

  @Override
  public @NonNull List<MemoryEntry> all(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return withReadLock(userId, () -> new ArrayList<>(loadUserStorage(userId).values()));
  }

  @Override
  public int size(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return withReadLock(userId, () -> loadUserStorage(userId).size());
  }

  @Override
  public void clear(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    withWriteLock(userId, () -> {
      Path userFile = userFilePath(userId);
      try {
        Files.deleteIfExists(userFile);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to clear memory for user: " + userId, e);
      }
    });
  }

  @Override
  public void clearAll() {
    try {
      if (Files.exists(baseDir)) {
        try (var stream = Files.list(baseDir)) {
          stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
            try {
              Files.deleteIfExists(p);
            } catch (IOException e) {
              throw new IllegalStateException("Failed to delete memory file: " + p, e);
            }
          });
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to clear all memories", e);
    }
    userLocks.clear();
  }

  // ===== Private Helpers =====

  private Path userFilePath(String userId) {
    // Sanitize userId to be a safe filename
    String safeUserId = userId.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    return baseDir.resolve(safeUserId + ".json");
  }

  private Map<String, MemoryEntry> loadUserStorage(String userId) {
    Path file = userFilePath(userId);
    if (!Files.exists(file)) {
      return new LinkedHashMap<>();
    }
    try {
      TypeReference<Map<String, MemoryEntry>> typeRef = new TypeReference<>() {};
      return objectMapper.readValue(file.toFile(), typeRef);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load memory for user: " + userId, e);
    }
  }

  private void saveUserStorage(String userId, Map<String, MemoryEntry> storage) {
    Path file = userFilePath(userId);
    Path tempFile = file.getParent().resolve(file.getFileName() + ".tmp");
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), storage);
      Files.move(tempFile, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
          java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException ignored) {}
      throw new IllegalStateException("Failed to save memory for user: " + userId, e);
    }
  }

  private ReentrantReadWriteLock getLock(String userId) {
    return userLocks.computeIfAbsent(userId, k -> new ReentrantReadWriteLock());
  }

  private void withWriteLock(String userId, Runnable action) {
    ReentrantReadWriteLock.WriteLock lock = getLock(userId).writeLock();
    lock.lock();
    try {
      action.run();
    } finally {
      lock.unlock();
    }
  }

  private <T> T withWriteLock(String userId, java.util.concurrent.Callable<T> action) {
    ReentrantReadWriteLock.WriteLock lock = getLock(userId).writeLock();
    lock.lock();
    try {
      return action.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      lock.unlock();
    }
  }

  private <T> T withReadLock(String userId, java.util.concurrent.Callable<T> action) {
    ReentrantReadWriteLock.ReadLock lock = getLock(userId).readLock();
    lock.lock();
    try {
      return action.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      lock.unlock();
    }
  }

  private double scoreRelevance(MemoryEntry entry, String queryLower) {
    String contentLower = entry.content().toLowerCase();
    if (contentLower.equals(queryLower)) return 1.0;
    if (contentLower.contains(queryLower)) return 0.8;
    String[] queryWords = queryLower.split("\\s+");
    int matches = 0;
    for (String word : queryWords) {
      if (word.length() > 2 && contentLower.contains(word)) matches++;
    }
    if (matches > 0) return 0.3 + (0.4 * matches / queryWords.length);
    return 0;
  }

  private record ScoredEntry(MemoryEntry entry, double score) {}
}
