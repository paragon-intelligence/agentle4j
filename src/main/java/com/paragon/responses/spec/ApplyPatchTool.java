package com.paragon.responses.spec;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Allows the assistant to create, delete, or update files using unified diffs. */
public final class ApplyPatchTool extends FunctionTool<ApplyPatchParams> {
  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable ApplyPatchParams params) {
    return null;
  }

  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JacksonException {
    return mapper.writeValueAsString(Map.of("type", "apply_patch"));
  }
}
