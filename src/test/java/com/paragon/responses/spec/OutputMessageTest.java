package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for OutputMessage class. */
@DisplayName("OutputMessage Tests")
class OutputMessageTest {

  private static List<MessageContent> sampleContent() {
    return List.of(new Text("Hello"));
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("creates with content, id, status, and parsed")
    void createsWithAllParams() {
      List<MessageContent> content = sampleContent();

      OutputMessage<String> msg =
          new OutputMessage<>(content, "msg_123", InputMessageStatus.COMPLETED, "parsed_value");

      assertEquals("msg_123", msg.id());
      assertEquals(InputMessageStatus.COMPLETED, msg.status());
      assertEquals("parsed_value", msg.parsed());
      assertEquals(content, msg.content());
    }

    @Test
    @DisplayName("creates with null parsed value")
    void createsWithNullParsed() {
      List<MessageContent> content = sampleContent();

      OutputMessage<Void> msg =
          new OutputMessage<>(content, "msg_456", InputMessageStatus.IN_PROGRESS, null);

      assertNull(msg.parsed());
    }
  }

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("id returns the message id")
    void idReturnsId() {
      OutputMessage<Void> msg =
          new OutputMessage<>(sampleContent(), "msg_abc", InputMessageStatus.COMPLETED, null);

      assertEquals("msg_abc", msg.id());
    }

    @Test
    @DisplayName("status returns the status")
    void statusReturnsStatus() {
      OutputMessage<Void> msg =
          new OutputMessage<>(sampleContent(), "id", InputMessageStatus.COMPLETED, null);

      assertEquals(InputMessageStatus.COMPLETED, msg.status());
    }

    @Test
    @DisplayName("parsed returns the parsed value")
    void parsedReturnsParsed() {
      record PersonInfo(String name, int age) {}
      PersonInfo person = new PersonInfo("Alice", 30);

      OutputMessage<PersonInfo> msg =
          new OutputMessage<>(sampleContent(), "id", InputMessageStatus.COMPLETED, person);

      assertEquals(person, msg.parsed());
      assertEquals("Alice", msg.parsed().name());
      assertEquals(30, msg.parsed().age());
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("returns concatenated content text")
    void returnsConcatenatedContent() {
      List<MessageContent> content = List.of(new Text("Hello "), new Text("World"));

      OutputMessage<Void> msg =
          new OutputMessage<>(content, "id", InputMessageStatus.COMPLETED, null);

      assertTrue(msg.toString().contains("Hello"));
      assertTrue(msg.toString().contains("World"));
    }
  }

  @Nested
  @DisplayName("Equality")
  class EqualityTests {

    @Test
    @DisplayName("equal messages are equal")
    void equalMessagesAreEqual() {
      List<MessageContent> content = List.of(new Text("Same"));

      OutputMessage<String> msg1 =
          new OutputMessage<>(content, "id_1", InputMessageStatus.COMPLETED, "parsed");
      OutputMessage<String> msg2 =
          new OutputMessage<>(content, "id_1", InputMessageStatus.COMPLETED, "parsed");

      assertEquals(msg1, msg2);
      assertEquals(msg1.hashCode(), msg2.hashCode());
    }

    @Test
    @DisplayName("different ids means not equal")
    void differentIdsNotEqual() {
      List<MessageContent> content = List.of(new Text("Same"));

      OutputMessage<Void> msg1 =
          new OutputMessage<>(content, "id_1", InputMessageStatus.COMPLETED, null);
      OutputMessage<Void> msg2 =
          new OutputMessage<>(content, "id_2", InputMessageStatus.COMPLETED, null);

      assertNotEquals(msg1, msg2);
    }

    @Test
    @DisplayName("same message equals itself")
    void sameMessageEqualsItself() {
      OutputMessage<Void> msg =
          new OutputMessage<>(sampleContent(), "id", InputMessageStatus.COMPLETED, null);

      assertEquals(msg, msg);
    }
  }
}
