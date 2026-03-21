package com.paragon.agents;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * JDBC-backed durable implementation of {@link Memory}.
 *
 * <p>Stores memory entries in any JDBC-compatible database (PostgreSQL, MySQL, H2, SQLite, etc.).
 * The table is created automatically on first use if it does not exist.
 *
 * <p>Table schema (auto-created):
 *
 * <pre>{@code
 * CREATE TABLE IF NOT EXISTS agent_memory (
 *   id        VARCHAR(255) NOT NULL,
 *   user_id   VARCHAR(255) NOT NULL,
 *   timestamp TIMESTAMP    NOT NULL,
 *   content   TEXT         NOT NULL,
 *   metadata  TEXT         NOT NULL,
 *   PRIMARY KEY (id, user_id)
 * )
 * }</pre>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * DataSource ds = HikariDataSource(...);
 * Memory memory = JdbcMemory.create(ds);
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
public final class JdbcMemory implements Memory {

  private static final String DEFAULT_TABLE = "agent_memory";

  private final DataSource dataSource;
  private final String tableName;
  private final ObjectMapper objectMapper;

  private JdbcMemory(DataSource dataSource, String tableName, ObjectMapper objectMapper) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
    this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    ensureTableExists();
  }

  /**
   * Creates a JdbcMemory using the default table name {@code agent_memory}.
   *
   * @param dataSource the JDBC data source
   * @return a new JdbcMemory instance
   */
  public static @NonNull JdbcMemory create(@NonNull DataSource dataSource) {
    ObjectMapper mapper = new ObjectMapper();
    return new JdbcMemory(dataSource, DEFAULT_TABLE, mapper);
  }

  /**
   * Creates a JdbcMemory with a custom table name.
   *
   * @param dataSource the JDBC data source
   * @param tableName the table name to use
   * @return a new JdbcMemory instance
   */
  public static @NonNull JdbcMemory create(
      @NonNull DataSource dataSource, @NonNull String tableName) {
    ObjectMapper mapper = new ObjectMapper();
    return new JdbcMemory(dataSource, tableName, mapper);
  }

  @Override
  public void add(@NonNull String userId, @NonNull MemoryEntry entry) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(entry, "entry cannot be null");
    String sql =
        "INSERT INTO "
            + tableName
            + " (id, user_id, timestamp, content, metadata) VALUES (?, ?, ?, ?, ?)"
            + " ON CONFLICT (id, user_id) DO UPDATE SET content = EXCLUDED.content,"
            + " metadata = EXCLUDED.metadata, timestamp = EXCLUDED.timestamp";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, entry.id());
      ps.setString(2, userId);
      ps.setTimestamp(3, Timestamp.from(entry.timestamp()));
      ps.setString(4, entry.content());
      ps.setString(5, objectMapper.writeValueAsString(entry.metadata()));
      ps.executeUpdate();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to add memory entry", e);
    }
  }

  @Override
  public @NonNull List<MemoryEntry> retrieve(
      @NonNull String userId, @NonNull String query, int limit) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(query, "query cannot be null");
    if (limit <= 0) return List.of();

    List<MemoryEntry> all = all(userId);
    String queryLower = query.toLowerCase();
    return all.stream()
        .map(e -> new ScoredEntry(e, scoreRelevance(e, queryLower)))
        .filter(s -> s.score > 0)
        .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed())
        .limit(limit)
        .map(ScoredEntry::entry)
        .collect(Collectors.toList());
  }

  @Override
  public void update(@NonNull String userId, @NonNull String id, @NonNull MemoryEntry entry) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(entry, "entry cannot be null");

    // Verify exists first
    if (!existsById(userId, id)) {
      throw new IllegalArgumentException(
          "Memory with id '" + id + "' not found for user '" + userId + "'");
    }

    String sql = "DELETE FROM " + tableName + " WHERE id = ? AND user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.setString(2, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to delete old memory entry during update", e);
    }
    add(userId, entry);
  }

  @Override
  public boolean delete(@NonNull String userId, @NonNull String id) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(id, "id cannot be null");
    String sql = "DELETE FROM " + tableName + " WHERE id = ? AND user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.setString(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to delete memory entry", e);
    }
  }

  @Override
  public @NonNull List<MemoryEntry> all(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    String sql = "SELECT id, timestamp, content, metadata FROM " + tableName + " WHERE user_id = ?";
    List<MemoryEntry> entries = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String id = rs.getString("id");
          Instant timestamp = rs.getTimestamp("timestamp").toInstant();
          String content = rs.getString("content");
          String metadataJson = rs.getString("metadata");
          TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
          Map<String, Object> metadata = objectMapper.readValue(metadataJson, typeRef);
          entries.add(new MemoryEntry(id, timestamp, content, metadata));
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to retrieve all memories for user: " + userId, e);
    }
    return entries;
  }

  @Override
  public int size(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to count memories", e);
    }
  }

  @Override
  public void clear(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    String sql = "DELETE FROM " + tableName + " WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to clear memories for user: " + userId, e);
    }
  }

  @Override
  public void clearAll() {
    String sql = "DELETE FROM " + tableName;
    try (Connection conn = dataSource.getConnection();
        Statement st = conn.createStatement()) {
      st.executeUpdate(sql);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to clear all memories", e);
    }
  }

  // ===== Private Helpers =====

  private void ensureTableExists() {
    String sql =
        "CREATE TABLE IF NOT EXISTS "
            + tableName
            + " ("
            + "  id        VARCHAR(255) NOT NULL,"
            + "  user_id   VARCHAR(255) NOT NULL,"
            + "  timestamp TIMESTAMP    NOT NULL,"
            + "  content   TEXT         NOT NULL,"
            + "  metadata  TEXT         NOT NULL,"
            + "  PRIMARY KEY (id, user_id)"
            + ")";
    try (Connection conn = dataSource.getConnection();
        Statement st = conn.createStatement()) {
      st.executeUpdate(sql);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to create memory table: " + tableName, e);
    }
  }

  private boolean existsById(String userId, String id) {
    String sql = "SELECT 1 FROM " + tableName + " WHERE id = ? AND user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.setString(2, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to check memory existence", e);
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
