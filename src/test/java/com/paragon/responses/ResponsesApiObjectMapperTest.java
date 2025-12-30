package com.paragon.responses;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.spec.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for ResponsesApiObjectMapper factory. */
@DisplayName("ResponsesApiObjectMapper Tests")
class ResponsesApiObjectMapperTest {

  @Nested
  @DisplayName("create")
  class CreateTests {

    @Test
    @DisplayName("creates ObjectMapper instance")
    void createsInstance() {
      ObjectMapper mapper = ResponsesApiObjectMapper.create();

      assertNotNull(mapper);
    }

    @Test
    @DisplayName("creates different instances each time")
    void createsDifferentInstances() {
      ObjectMapper mapper1 = ResponsesApiObjectMapper.create();
      ObjectMapper mapper2 = ResponsesApiObjectMapper.create();

      assertNotSame(mapper1, mapper2);
    }
  }

  @Nested
  @DisplayName("Serialization")
  class Serialization {

    @Test
    @DisplayName("serializes to snake_case")
    void serializesToSnakeCase() throws JsonProcessingException {
      ObjectMapper mapper = ResponsesApiObjectMapper.create();
      Message message = Message.user("Hello");

      String json = mapper.writeValueAsString(message);

      // Should contain snake_case field names
      assertTrue(json.contains("role"));
    }

    @Test
    @DisplayName("excludes null fields")
    void excludesNullFields() throws JsonProcessingException {
      ObjectMapper mapper = ResponsesApiObjectMapper.create();
      Message message = Message.user("Hello");

      String json = mapper.writeValueAsString(message);

      // Should not contain fields explicitly set to null
      assertFalse(json.contains("\"null\""));
    }
  }

  @Nested
  @DisplayName("Deserialization")
  class Deserialization {

    @Test
    @DisplayName("ignores unknown properties")
    void ignoresUnknownProperties() throws JsonProcessingException {
      ObjectMapper mapper = ResponsesApiObjectMapper.create();
      String json =
          "{\"role\":\"user\",\"content\":[{\"type\":\"input_text\",\"text\":\"Hi\"}],\"unknown_field\":\"value\"}";

      // Should not throw
      Message message = mapper.readValue(json, Message.class);
      assertNotNull(message);
    }
  }
}
