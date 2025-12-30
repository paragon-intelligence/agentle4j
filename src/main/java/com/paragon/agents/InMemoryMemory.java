package com.paragon.agents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * Thread-safe in-memory implementation of {@link Memory} with user isolation.
 *
 * <p>Memories are stored per-user in separate maps to ensure complete isolation. For production use
 * with large amounts of data or semantic search requirements, consider implementations backed by
 * vector databases.
 *
 * @see Memory
 * @see MemoryEntry
 * @since 1.0
 */
public final class InMemoryMemory implements Memory {

  // userId -> (memoryId -> MemoryEntry)
  private final Map<String, Map<String, MemoryEntry>> userStorage = new ConcurrentHashMap<>();

  private InMemoryMemory() {}

  /**
   * Creates a new empty in-memory storage.
   *
   * @return a new InMemoryMemory instance
   */
  public static @NonNull InMemoryMemory create() {
    return new InMemoryMemory();
  }

  @Override
  public void add(@NonNull String userId, @NonNull MemoryEntry entry) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(entry, "entry cannot be null");
    getUserStorage(userId).put(entry.id(), entry);
  }

  @Override
  public @NonNull List<MemoryEntry> retrieve(
      @NonNull String userId, @NonNull String query, int limit) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(query, "query cannot be null");
    if (limit <= 0) {
      return List.of();
    }

    Map<String, MemoryEntry> storage = getUserStorage(userId);
    if (storage.isEmpty()) {
      return List.of();
    }

    String queryLower = query.toLowerCase();

    return storage.values().stream()
        .map(entry -> new ScoredEntry(entry, scoreRelevance(entry, queryLower)))
        .filter(scored -> scored.score > 0)
        .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed())
        .limit(limit)
        .map(ScoredEntry::entry)
        .collect(Collectors.toList());
  }

  private double scoreRelevance(MemoryEntry entry, String queryLower) {
    String contentLower = entry.content().toLowerCase();

    if (contentLower.equals(queryLower)) {
      return 1.0;
    }
    if (contentLower.contains(queryLower)) {
      return 0.8;
    }

    String[] queryWords = queryLower.split("\\s+");
    int matches = 0;
    for (String word : queryWords) {
      if (word.length() > 2 && contentLower.contains(word)) {
        matches++;
      }
    }

    if (matches > 0) {
      return 0.3 + (0.4 * matches / queryWords.length);
    }

    return 0;
  }

  @Override
  public void update(@NonNull String userId, @NonNull String id, @NonNull MemoryEntry entry) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(entry, "entry cannot be null");

    Map<String, MemoryEntry> storage = getUserStorage(userId);
    if (!storage.containsKey(id)) {
      throw new IllegalArgumentException(
          "Memory with id '" + id + "' not found for user '" + userId + "'");
    }

    storage.remove(id);
    storage.put(entry.id(), entry);
  }

  @Override
  public boolean delete(@NonNull String userId, @NonNull String id) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(id, "id cannot be null");
    return getUserStorage(userId).remove(id) != null;
  }

  @Override
  public @NonNull List<MemoryEntry> all(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return new ArrayList<>(getUserStorage(userId).values());
  }

  @Override
  public int size(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return getUserStorage(userId).size();
  }

  @Override
  public void clear(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    getUserStorage(userId).clear();
  }

  @Override
  public void clearAll() {
    userStorage.clear();
  }

  private Map<String, MemoryEntry> getUserStorage(String userId) {
    return userStorage.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
  }

  private record ScoredEntry(MemoryEntry entry, double score) {}
}
