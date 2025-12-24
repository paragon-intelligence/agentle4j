package com.paragon.responses.json;

import java.util.Map;

public interface JsonSchemaProducer {
  Map<String, Object> produce(Class<?> clazz);
}
