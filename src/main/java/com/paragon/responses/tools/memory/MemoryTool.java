package com.paragon.responses.tools.memory;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.json.JsonSchemaProducer;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionMetadata(
    name = "memory_tool",
    description = "Stores and retrieves information from memory for future reference")
public final class MemoryTool extends FunctionTool<MemoryToolParams> {

  private static final Logger log = LoggerFactory.getLogger(MemoryTool.class);
  private final @NonNull MemoryStore memoryStore;

  public MemoryTool(@NonNull MemoryStore memoryStore) {
    this.memoryStore = memoryStore;
  }

  public MemoryTool(
      @NonNull MemoryStore memoryStore, @NonNull JsonSchemaProducer jsonSchemaProducer) {
    super(jsonSchemaProducer);
    this.memoryStore = memoryStore;
  }

  @Override
  public @NonNull FunctionToolCallOutput call(@Nullable MemoryToolParams params) {
    if (params == null) {
      log.warn("MemoryTool called with null parameters");
      return FunctionToolCallOutput.error("Parameters are required");
    }

    try {
      log.info("MemoryTool called with action: {}", params.action());

      String result =
          switch (params.action()) {
            case STORE -> {
              assert params.value() != null;
              yield memoryStore.store(params.key(), params.value());
            }
            case RETRIEVE -> {
              String value = memoryStore.retrieve(params.key());
              yield value != null ? value : "No value found for key: " + params.key();
            }
            case DELETE -> memoryStore.delete(params.key());
          };

      return FunctionToolCallOutput.success(result);

    } catch (Exception e) {
      log.error("Error executing MemoryTool", e);
      return FunctionToolCallOutput.error("Failed to execute memory operation: " + e.getMessage());
    }
  }
}
