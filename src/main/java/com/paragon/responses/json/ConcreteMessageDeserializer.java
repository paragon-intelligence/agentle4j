package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.paragon.responses.spec.Message;
import java.io.IOException;

/**
 * Deserializer for concrete Message subclasses that delegates to MessageDeserializer and casts to
 * the expected type.
 */
public class ConcreteMessageDeserializer<T extends Message> extends JsonDeserializer<T> {

  private final Class<T> targetClass;
  private final MessageDeserializer messageDeserializer = new MessageDeserializer();

  public ConcreteMessageDeserializer(Class<T> targetClass) {
    this.targetClass = targetClass;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    Message message = messageDeserializer.deserialize(p, ctxt);

    if (!targetClass.isInstance(message)) {
      throw new IOException(
          String.format(
              "Expected %s but got %s (role mismatch)",
              targetClass.getSimpleName(), message.getClass().getSimpleName()));
    }

    return (T) message;
  }
}
