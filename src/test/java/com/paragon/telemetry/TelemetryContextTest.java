package com.paragon.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for TelemetryContext.
 *
 * <p>Tests cover:
 * - Builder pattern
 * - Factory methods (empty, forUser)
 * - toAttributes() output
 * - Immutability
 * - Null handling
 */
@DisplayName("TelemetryContext Tests")
class TelemetryContextTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("empty() creates context with null userId and traceName")
    void empty_createsEmptyContext() {
      TelemetryContext context = TelemetryContext.empty();

      assertNull(context.userId());
      assertNull(context.traceName());
      assertTrue(context.metadata().isEmpty());
      assertTrue(context.tags().isEmpty());
    }

    @Test
    @DisplayName("forUser() creates context with userId only")
    void forUser_createsContextWithUserIdOnly() {
      TelemetryContext context = TelemetryContext.forUser("user-123");

      assertEquals("user-123", context.userId());
      assertNull(context.traceName());
      assertTrue(context.metadata().isEmpty());
      assertTrue(context.tags().isEmpty());
    }

    @Test
    @DisplayName("builder() creates new builder instance")
    void builder_createsNewBuilderInstance() {
      TelemetryContext.Builder builder = TelemetryContext.builder();

      assertNotNull(builder);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER PATTERN
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder Pattern")
  class BuilderPattern {

    @Test
    @DisplayName("builder sets userId")
    void builder_setsUserId() {
      TelemetryContext context = TelemetryContext.builder()
          .userId("user-456")
          .build();

      assertEquals("user-456", context.userId());
    }

    @Test
    @DisplayName("builder sets traceName")
    void builder_setsTraceName() {
      TelemetryContext context = TelemetryContext.builder()
          .traceName("chat.completion")
          .build();

      assertEquals("chat.completion", context.traceName());
    }

    @Test
    @DisplayName("builder adds single metadata entry")
    void builder_addsSingleMetadata() {
      TelemetryContext context = TelemetryContext.builder()
          .addMetadata("version", "1.0")
          .build();

      assertEquals("1.0", context.metadata().get("version"));
    }

    @Test
    @DisplayName("builder adds multiple metadata entries via addMetadata")
    void builder_addsMultipleMetadataViaAddMetadata() {
      TelemetryContext context = TelemetryContext.builder()
          .addMetadata("version", "1.0")
          .addMetadata("environment", "production")
          .build();

      assertEquals("1.0", context.metadata().get("version"));
      assertEquals("production", context.metadata().get("environment"));
    }

    @Test
    @DisplayName("builder adds all metadata from map")
    void builder_addsAllMetadataFromMap() {
      Map<String, Object> metadataMap = Map.of(
          "key1", "value1",
          "key2", 123
      );

      TelemetryContext context = TelemetryContext.builder()
          .metadata(metadataMap)
          .build();

      assertEquals("value1", context.metadata().get("key1"));
      assertEquals(123, context.metadata().get("key2"));
    }

    @Test
    @DisplayName("builder adds single tag")
    void builder_addsSingleTag() {
      TelemetryContext context = TelemetryContext.builder()
          .addTag("production")
          .build();

      assertEquals(1, context.tags().size());
      assertTrue(context.tags().contains("production"));
    }

    @Test
    @DisplayName("builder adds multiple tags via addTag")
    void builder_addsMultipleTagsViaAddTag() {
      TelemetryContext context = TelemetryContext.builder()
          .addTag("production")
          .addTag("critical")
          .build();

      assertEquals(2, context.tags().size());
      assertTrue(context.tags().contains("production"));
      assertTrue(context.tags().contains("critical"));
    }

    @Test
    @DisplayName("builder adds all tags from list")
    void builder_addsAllTagsFromList() {
      List<String> tagList = List.of("tag1", "tag2", "tag3");

      TelemetryContext context = TelemetryContext.builder()
          .tags(tagList)
          .build();

      assertEquals(3, context.tags().size());
      assertTrue(context.tags().containsAll(tagList));
    }

    @Test
    @DisplayName("builder chains all methods fluently")
    void builder_chainsFluently() {
      TelemetryContext context = TelemetryContext.builder()
          .userId("user-123")
          .traceName("agent.run")
          .addMetadata("version", "2.0")
          .addTag("test")
          .build();

      assertEquals("user-123", context.userId());
      assertEquals("agent.run", context.traceName());
      assertEquals("2.0", context.metadata().get("version"));
      assertTrue(context.tags().contains("test"));
    }

    @Test
    @DisplayName("builder throws on null userId")
    void builder_throwsOnNullUserId() {
      assertThrows(NullPointerException.class, () ->
          TelemetryContext.builder().userId(null));
    }

    @Test
    @DisplayName("builder throws on null traceName")
    void builder_throwsOnNullTraceName() {
      assertThrows(NullPointerException.class, () ->
          TelemetryContext.builder().traceName(null));
    }

    @Test
    @DisplayName("builder throws on null metadata key")
    void builder_throwsOnNullMetadataKey() {
      assertThrows(NullPointerException.class, () ->
          TelemetryContext.builder().addMetadata(null, "value"));
    }

    @Test
    @DisplayName("builder throws on null metadata value")
    void builder_throwsOnNullMetadataValue() {
      assertThrows(NullPointerException.class, () ->
          TelemetryContext.builder().addMetadata("key", null));
    }

    @Test
    @DisplayName("builder throws on null tag")
    void builder_throwsOnNullTag() {
      assertThrows(NullPointerException.class, () ->
          TelemetryContext.builder().addTag(null));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // toAttributes() METHOD
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("toAttributes Method")
  class ToAttributesMethod {

    @Test
    @DisplayName("toAttributes returns empty map for empty context")
    void toAttributes_returnsEmptyMapForEmptyContext() {
      TelemetryContext context = TelemetryContext.empty();

      Map<String, Object> attrs = context.toAttributes();

      assertTrue(attrs.isEmpty());
    }

    @Test
    @DisplayName("toAttributes includes user.id and langfuse.user.id")
    void toAttributes_includesUserIdAttributes() {
      TelemetryContext context = TelemetryContext.forUser("user-789");

      Map<String, Object> attrs = context.toAttributes();

      assertEquals("user-789", attrs.get("user.id"));
      assertEquals("user-789", attrs.get("langfuse.user.id"));
    }

    @Test
    @DisplayName("toAttributes includes span.name")
    void toAttributes_includesSpanName() {
      TelemetryContext context = TelemetryContext.builder()
          .traceName("my.span")
          .build();

      Map<String, Object> attrs = context.toAttributes();

      assertEquals("my.span", attrs.get("span.name"));
    }

    @Test
    @DisplayName("toAttributes includes langfuse.tags as comma-separated")
    void toAttributes_includesTagsAsCommaSeparated() {
      TelemetryContext context = TelemetryContext.builder()
          .addTag("prod")
          .addTag("critical")
          .build();

      Map<String, Object> attrs = context.toAttributes();

      assertEquals("prod,critical", attrs.get("langfuse.tags"));
    }

    @Test
    @DisplayName("toAttributes includes metadata with langfuse prefix")
    void toAttributes_includesMetadataWithPrefix() {
      TelemetryContext context = TelemetryContext.builder()
          .addMetadata("version", "1.0")
          .build();

      Map<String, Object> attrs = context.toAttributes();

      assertEquals("1.0", attrs.get("langfuse.metadata.version"));
      assertEquals("1.0", attrs.get("version")); // Also without prefix
    }

    @Test
    @DisplayName("toAttributes returns unmodifiable map")
    void toAttributes_returnsUnmodifiableMap() {
      TelemetryContext context = TelemetryContext.builder()
          .userId("user-123")
          .build();

      Map<String, Object> attrs = context.toAttributes();

      assertThrows(UnsupportedOperationException.class, () ->
          attrs.put("new.key", "value"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // getTraceNameOrDefault() METHOD
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("getTraceNameOrDefault Method")
  class GetTraceNameOrDefaultMethod {

    @Test
    @DisplayName("returns traceName when set")
    void returnsTraceNameWhenSet() {
      TelemetryContext context = TelemetryContext.builder()
          .traceName("custom.trace")
          .build();

      assertEquals("custom.trace", context.getTraceNameOrDefault("default"));
    }

    @Test
    @DisplayName("returns default when traceName is null")
    void returnsDefaultWhenTraceNameIsNull() {
      TelemetryContext context = TelemetryContext.empty();

      assertEquals("default.trace", context.getTraceNameOrDefault("default.trace"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // IMMUTABILITY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Immutability")
  class Immutability {

    @Test
    @DisplayName("metadata map is immutable")
    void metadataMapIsImmutable() {
      TelemetryContext context = TelemetryContext.builder()
          .addMetadata("key", "value")
          .build();

      assertThrows(UnsupportedOperationException.class, () ->
          context.metadata().put("new", "value"));
    }

    @Test
    @DisplayName("tags list is immutable")
    void tagsListIsImmutable() {
      TelemetryContext context = TelemetryContext.builder()
          .addTag("tag1")
          .build();

      assertThrows(UnsupportedOperationException.class, () ->
          context.tags().add("newTag"));
    }
  }
}
