package com.paragon.responses.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.DeveloperMessage;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SimpleSerializationTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  @Test
  void testDeveloperMessageSerialization() throws Exception {
    // Create a simple developer message
    DeveloperMessage message = new DeveloperMessage(List.of(new Text("Hello world")), null);

    // Serialize
    String json = objectMapper.writeValueAsString(message);
    System.out.println("Serialized JSON: " + json);

    // Deserialize
    DeveloperMessage deserialized = objectMapper.readValue(json, DeveloperMessage.class);
    System.out.println("Deserialized: " + deserialized);
    System.out.println("Content size: " + deserialized.content().size());
    System.out.println("Status: " + deserialized.status());

    // Check equality
    System.out.println("Equal: " + message.equals(deserialized));
  }

  @Test
  void testCreateResponseWithInputSerialization() throws Exception {
    // Create a CreateResponse with input
    CreateResponsePayload request =
        new CreateResponsePayload(
            null,
            null,
            null,
            List.of(new DeveloperMessage(List.of(new Text("Test")), null)),
            "Instructions",
            null,
            null,
            null,
            "gpt-4o",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    // Serialize
    String json = objectMapper.writeValueAsString(request);
    System.out.println("Serialized JSON: " + json);

    // Deserialize
    CreateResponsePayload deserialized = objectMapper.readValue(json, CreateResponsePayload.class);
    System.out.println("Deserialized: " + deserialized);

    if (deserialized.input() != null && !deserialized.input().isEmpty()) {
      ResponseInputItem item = (ResponseInputItem) deserialized.input().getFirst();
      System.out.println("Input item class: " + item.getClass());
      if (item instanceof DeveloperMessage dm) {
        System.out.println("DeveloperMessage content size: " + dm.content().size());
        System.out.println("DeveloperMessage status: " + dm.status());
      }
    }

    // Check equality
    System.out.println("Equal: " + request.equals(deserialized));
  }
}
