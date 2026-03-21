package com.paragon.responses.json;

import com.paragon.responses.spec.Message;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializer for concrete Message subclasses that delegates to MessageDeserializer and casts to
 * the expected type.
 */
public class ConcreteMessageDeserializer<T extends Message> extends ValueDeserializer<T> {

  private final Class<T> targetClass;
  private final MessageDeserializer messageDeserializer = new MessageDeserializer();

  public ConcreteMessageDeserializer(Class<T> targetClass) {
    this.targetClass = targetClass;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    Message message = messageDeserializer.deserialize(p, ctxt);

    if (!targetClass.isInstance(message)) {
      return ctxt.reportInputMismatch(
          targetClass,
          "Expected %s but got %s (role mismatch)",
          targetClass.getSimpleName(),
          message.getClass().getSimpleName());
    }

    return (T) message;
  }
}
