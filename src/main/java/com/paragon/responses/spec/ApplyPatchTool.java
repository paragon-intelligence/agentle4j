package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Allows the assistant to create, delete, or update files using unified diffs. */
public final class ApplyPatchTool extends FunctionTool<ApplyPatchParams> {
  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable ApplyPatchParams params) {
    return null;
  }

  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("type", "apply_patch"));
  }
}
