package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ConfigurationException}. */
@DisplayName("ConfigurationException")
class ConfigurationExceptionTest {

  @Nested
  @DisplayName("Constructor")
  class Constructor {

    @Test
    @DisplayName("should create with all fields")
    void shouldCreateWithAllFields() {
      ConfigurationException e =
          new ConfigurationException("API key is missing", "apiKey", "Set OPENAI_API_KEY env var");

      assertEquals("API key is missing", e.getMessage());
      assertEquals("apiKey", e.configKey());
      assertEquals("Set OPENAI_API_KEY env var", e.suggestion());
      assertEquals(AgentleException.ErrorCode.MISSING_CONFIGURATION, e.code());
      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("should create with null config key")
    void shouldCreateWithNullConfigKey() {
      ConfigurationException e = new ConfigurationException("Invalid config", null, null);

      assertNull(e.configKey());
      assertNull(e.suggestion());
    }
  }

  @Nested
  @DisplayName("Factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("missing should create with descriptive message")
    void missingShouldCreateDescriptive() {
      ConfigurationException e = ConfigurationException.missing("apiKey");

      assertEquals("apiKey", e.configKey());
      assertTrue(e.getMessage().contains("apiKey"));
      assertTrue(e.getMessage().toLowerCase().contains("missing"));
      assertTrue(e.suggestion().contains("apiKey"));
    }

    @Test
    @DisplayName("invalid should create with reason")
    void invalidShouldCreateWithReason() {
      ConfigurationException e =
          ConfigurationException.invalid("temperature", "Must be between 0 and 2");

      assertEquals("temperature", e.configKey());
      assertTrue(e.getMessage().contains("temperature"));
      assertTrue(e.getMessage().contains("Must be between 0 and 2"));
    }
  }

  @Nested
  @DisplayName("Inheritance")
  class Inheritance {

    @Test
    @DisplayName("should extend AgentleException")
    void shouldExtendAgentleException() {
      ConfigurationException e = ConfigurationException.missing("test");

      assertInstanceOf(AgentleException.class, e);
    }
  }
}
