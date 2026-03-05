package com.paragon.responses.spec;

import java.util.Map;
import org.jspecify.annotations.NonNull;

public record TextConfigurationOptionsJsonSchemaFormat(
    @NonNull String name, @NonNull Map<String, Object> schema, @NonNull Boolean strict)
    implements TextConfigurationOptionsFormat {
  public static TextConfigurationOptionsJsonSchemaFormat create(
      @NonNull String name, @NonNull Map<String, Object> schema) {
    return new TextConfigurationOptionsJsonSchemaFormat(name, schema, true);
  }
}
