package com.paragon.responses.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DebugSerializationTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  @Test
  void debugUserMessageSerialization() throws Exception {
    // Create a UserMessage
    UserMessage original = new UserMessage(List.of(new Text("Hello")));

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(original);
    System.out.println("JSON: " + json);

    // Deserialize back
    UserMessage deserialized = objectMapper.readValue(json, UserMessage.class);

    // Check equality
    System.out.println("Original: " + original);
    System.out.println("Deserialized: " + deserialized);
    System.out.println("Original class: " + original.getClass());
    System.out.println("Deserialized class: " + deserialized.getClass());
    System.out.println("Are equal: " + original.equals(deserialized));
    System.out.println("Original content: " + original.content());
    System.out.println("Deserialized content: " + deserialized.content());
    System.out.println("Content equals: " + original.content().equals(deserialized.content()));
    System.out.println("Original status: " + original.status());
    System.out.println("Deserialized status: " + deserialized.status());
  }
}
