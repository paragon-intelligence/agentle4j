package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DebugRoundTripTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  @Test
  void debugResponseEquality() throws Exception {
    // Create a simple Response
    Response original =
        new Response(
            null,
            null,
            1638360000L,
            null,
            "resp-456",
            null,
            null,
            null,
            null,
            null,
            "gpt-4o",
            ResponseObject.RESPONSE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ResponseGenerationStatus.COMPLETED,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(original);
    System.out.println("JSON: " + json);

    // Deserialize back
    Response deserialized = objectMapper.readValue(json, Response.class);

    // Debug output
    System.out.println("Original class: " + original.getClass());
    System.out.println("Deserialized class: " + deserialized.getClass());
    System.out.println("Original hashCode: " + original.hashCode());
    System.out.println("Deserialized hashCode: " + deserialized.hashCode());
    System.out.println("Classes equal: " + (original.getClass() == deserialized.getClass()));
    System.out.println("instanceof check: " + (deserialized instanceof Response));
    System.out.println("Equals result: " + original.equals(deserialized));

    // Check each field
    System.out.println("\nField comparison:");
    System.out.println("id equal: " + java.util.Objects.equals(original.id(), deserialized.id()));
    System.out.println(
        "model equal: " + java.util.Objects.equals(original.model(), deserialized.model()));
    System.out.println(
        "status equal: " + java.util.Objects.equals(original.status(), deserialized.status()));
    System.out.println(
        "createdAt equal: "
            + java.util.Objects.equals(original.createdAt(), deserialized.createdAt()));

    assertEquals(original, deserialized);
  }

  @Test
  void debugResponseWithOutputEquality() throws Exception {
    // Create OutputMessage
    OutputMessage<Void> outputMsg =
        new OutputMessage<>(List.of(new Text("A")), "msg-123", InputMessageStatus.COMPLETED, null);

    // Create Response with output
    Response original =
        new Response(
            null,
            null,
            System.currentTimeMillis() / 1000,
            null,
            "AAAAA",
            null,
            null,
            null,
            null,
            null,
            "gpt-4o",
            ResponseObject.RESPONSE,
            List.of(outputMsg),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ResponseGenerationStatus.COMPLETED,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    // Serialize to JSON
    String json = objectMapper.writeValueAsString(original);
    System.out.println("JSON: " + json);

    // Deserialize back
    Response deserialized = objectMapper.readValue(json, Response.class);

    // Debug output
    System.out.println("Original class: " + original.getClass());
    System.out.println("Deserialized class: " + deserialized.getClass());
    System.out.println("Original output: " + original.output());
    System.out.println("Deserialized output: " + deserialized.output());

    if (original.output() != null && deserialized.output() != null) {
      System.out.println(
          "Output sizes: " + original.output().size() + " vs " + deserialized.output().size());
      if (!original.output().isEmpty() && !deserialized.output().isEmpty()) {
        var origOut = original.output().get(0);
        var deserOut = deserialized.output().get(0);
        System.out.println("Output[0] class: " + origOut.getClass() + " vs " + deserOut.getClass());
        System.out.println("Output[0] equals: " + origOut.equals(deserOut));
        System.out.println(
            "Output[0] hashCode: " + origOut.hashCode() + " vs " + deserOut.hashCode());
      }
    }

    System.out.println("Equals result: " + original.equals(deserialized));

    assertEquals(original, deserialized);
  }
}
