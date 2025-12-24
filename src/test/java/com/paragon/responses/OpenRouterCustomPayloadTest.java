package com.paragon.responses;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for OpenRouterCustomPayload.
 */
@DisplayName("OpenRouterCustomPayload Tests")
class OpenRouterCustomPayloadTest {

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builder creates empty payload")
    void builderCreatesEmptyPayload() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder().build();
      
      assertNotNull(payload);
      assertNull(payload.plugins());
      assertNull(payload.user());
      assertNull(payload.sessionId());
    }

    @Test
    @DisplayName("builder sets user")
    void builderSetsUser() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder()
          .user("user-123")
          .build();
      
      assertEquals("user-123", payload.user());
    }

    @Test
    @DisplayName("builder sets sessionId")
    void builderSetsSessionId() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder()
          .sessionId("session-abc")
          .build();
      
      assertEquals("session-abc", payload.sessionId());
    }

    @Test
    @DisplayName("builder chains correctly")
    void builderChains() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder()
          .user("user-123")
          .sessionId("session-abc")
          .build();
      
      assertEquals("user-123", payload.user());
      assertEquals("session-abc", payload.sessionId());
    }
  }

  @Nested
  @DisplayName("isEmpty")
  class IsEmptyTests {

    @Test
    @DisplayName("isEmpty returns true when all fields null")
    void isEmptyWhenAllNull() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder().build();
      
      assertTrue(payload.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns false when user set")
    void notEmptyWhenUserSet() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder()
          .user("user")
          .build();
      
      assertFalse(payload.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns false when sessionId set")
    void notEmptyWhenSessionIdSet() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder()
          .sessionId("session")
          .build();
      
      assertFalse(payload.isEmpty());
    }
  }

  @Nested
  @DisplayName("orNullIfEmpty")
  class OrNullIfEmptyTests {

    @Test
    @DisplayName("returns null when empty")
    void returnsNullWhenEmpty() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder().build();
      
      assertNull(payload.orNullIfEmpty());
    }

    @Test
    @DisplayName("returns self when not empty")
    void returnsSelfWhenNotEmpty() {
      OpenRouterCustomPayload payload = OpenRouterCustomPayload.builder()
          .user("user")
          .build();
      
      assertSame(payload, payload.orNullIfEmpty());
    }
  }
}
