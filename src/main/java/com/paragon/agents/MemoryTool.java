package com.paragon.agents;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;

/**
 * Memory exposed as FunctionTools for agent use.
 *
 * <p>This class provides 4 tools for memory operations. The userId is set
 * securely by the developer (not by the LLM) to prevent prompt injection attacks.
 *
 * <p>Usage:
 * <pre>{@code
 * Memory storage = InMemoryMemory.create();
 * 
 * Agent agent = Agent.builder()
 *     .addMemoryTools(storage)  // Adds all 4 memory tools
 *     .build();
 * }</pre>
 *
 * @since 1.0
 */
public final class MemoryTool {

  private MemoryTool() {}

  /**
   * Creates all four memory tools.
   *
   * @param memory the memory storage
   * @return list of all memory tools
   */
  public static @NonNull List<FunctionTool<?>> all(@NonNull Memory memory) {
    Objects.requireNonNull(memory, "memory cannot be null");
    return List.of(
        new AddMemoryTool(memory),
        new RetrieveMemoriesTool(memory),
        new UpdateMemoryTool(memory),
        new DeleteMemoryTool(memory)
    );
  }

  // ===== Request Records =====

  public record AddMemoryRequest(@NonNull String content) {}

  public record RetrieveMemoriesRequest(
      @NonNull String query,
      @Nullable Integer limit
  ) {}

  public record UpdateMemoryRequest(
      @NonNull String id,
      @NonNull String content
  ) {}

  public record DeleteMemoryRequest(@NonNull String id) {}

  // ===== Tool Implementations =====

  @FunctionMetadata(
      name = "add_memory",
      description = "Store a new memory for the current user. Use this to remember important " +
          "information, preferences, or facts the user has shared."
  )
  public static final class AddMemoryTool extends FunctionTool<AddMemoryRequest> {
    private final Memory memory;
    private String userId; // Injected by Agent per-call

    public AddMemoryTool(@NonNull Memory memory) {
      this.memory = Objects.requireNonNull(memory);
    }

    public void setUserId(@Nullable String userId) {
      this.userId = userId;
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable AddMemoryRequest params) {
      if (userId == null) {
        return FunctionToolCallOutput.error("userId not set");
      }
      if (params == null) {
        return FunctionToolCallOutput.error("no content provided");
      }
      MemoryEntry entry = MemoryEntry.of(params.content());
      memory.add(userId, entry);
      return FunctionToolCallOutput.success("Memory stored successfully with id: " + entry.id());
    }
  }

  @FunctionMetadata(
      name = "retrieve_memories",
      description = "Search and retrieve relevant memories for the current user. " +
          "Use this to recall information from previous conversations."
  )
  public static final class RetrieveMemoriesTool extends FunctionTool<RetrieveMemoriesRequest> {
    private final Memory memory;
    private String userId;

    public RetrieveMemoriesTool(@NonNull Memory memory) {
      this.memory = Objects.requireNonNull(memory);
    }

    public void setUserId(@Nullable String userId) {
      this.userId = userId;
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable RetrieveMemoriesRequest params) {
      if (userId == null) {
        return FunctionToolCallOutput.error("userId not set");
      }
      if (params == null) {
        return FunctionToolCallOutput.error("no query provided");
      }
      int limit = params.limit() != null ? params.limit() : 5;
      List<MemoryEntry> memories = memory.retrieve(userId, params.query(), limit);
      if (memories.isEmpty()) {
        return FunctionToolCallOutput.success("No relevant memories found.");
      }
      StringBuilder sb = new StringBuilder("Found " + memories.size() + " memories:\n");
      for (MemoryEntry entry : memories) {
        sb.append("- [").append(entry.id()).append("] ").append(entry.content()).append("\n");
      }
      return FunctionToolCallOutput.success(sb.toString());
    }
  }

  @FunctionMetadata(
      name = "update_memory",
      description = "Update an existing memory by its ID. Use this to correct or update " +
          "previously stored information."
  )
  public static final class UpdateMemoryTool extends FunctionTool<UpdateMemoryRequest> {
    private final Memory memory;
    private String userId;

    public UpdateMemoryTool(@NonNull Memory memory) {
      this.memory = Objects.requireNonNull(memory);
    }

    public void setUserId(@Nullable String userId) {
      this.userId = userId;
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable UpdateMemoryRequest params) {
      if (userId == null) {
        return FunctionToolCallOutput.error("userId not set");
      }
      if (params == null) {
        return FunctionToolCallOutput.error("no parameters provided");
      }
      try {
        MemoryEntry newEntry = MemoryEntry.withId(params.id(), params.content());
        memory.update(userId, params.id(), newEntry);
        return FunctionToolCallOutput.success("Memory updated successfully.");
      } catch (IllegalArgumentException e) {
        return FunctionToolCallOutput.error(e.getMessage());
      }
    }
  }

  @FunctionMetadata(
      name = "delete_memory",
      description = "Delete a memory by its ID. Use this when the user asks to forget something."
  )
  public static final class DeleteMemoryTool extends FunctionTool<DeleteMemoryRequest> {
    private final Memory memory;
    private String userId;

    public DeleteMemoryTool(@NonNull Memory memory) {
      this.memory = Objects.requireNonNull(memory);
    }

    public void setUserId(@Nullable String userId) {
      this.userId = userId;
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable DeleteMemoryRequest params) {
      if (userId == null) {
        return FunctionToolCallOutput.error("userId not set");
      }
      if (params == null) {
        return FunctionToolCallOutput.error("no id provided");
      }
      boolean deleted = memory.delete(userId, params.id());
      return FunctionToolCallOutput.success(
          deleted ? "Memory deleted successfully." : "Memory not found.");
    }
  }
}
