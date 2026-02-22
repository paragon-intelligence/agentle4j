package com.paragon.messaging.core;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.messaging.whatsapp.payload.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link MessageProcessor} and its processing contract. */
@DisplayName("MessageProcessor")
class MessageProcessorTest {

  private List<InboundMessage> testMessages;
  private String testUserId;

  @BeforeEach
  void setUp() {
    testUserId = "5511999999999";
    testMessages =
        Arrays.asList(
            createTextMessage("wamid.1", "Hello"), createTextMessage("wamid.2", "How are you?"));
  }

  @Nested
  @DisplayName("Static Factory Methods")
  class FactoryMethodsTests {

    @Test
    @DisplayName("noOp() creates processor that does nothing")
    void noOpCreatesProcessor() throws Exception {
      MessageProcessor processor = MessageProcessor.noOp();

      // Should not throw exception
      assertDoesNotThrow(
          () ->
              processor.process(
                  testUserId, testMessages, MessageProcessor.ProcessingContext.empty()));
    }

    @Test
    @DisplayName("logging() creates processor that logs messages")
    void loggingCreatesProcessor() throws Exception {
      AtomicInteger logCount = new AtomicInteger(0);
      StringBuilder logBuilder = new StringBuilder();

      MessageProcessor processor =
          MessageProcessor.logging(
              log -> {
                logCount.incrementAndGet();
                logBuilder.append(log);
              });

      processor.process(testUserId, testMessages, MessageProcessor.ProcessingContext.empty());

      assertEquals(1, logCount.get());
      assertTrue(logBuilder.toString().contains(testUserId));
      assertTrue(logBuilder.toString().contains("2 messages"));
    }

    @Test
    @DisplayName("logging() includes message content in logs")
    void loggingIncludesContent() throws Exception {
      StringBuilder logBuilder = new StringBuilder();
      MessageProcessor processor = MessageProcessor.logging(logBuilder::append);

      processor.process(testUserId, testMessages, MessageProcessor.ProcessingContext.empty());

      assertTrue(logBuilder.toString().contains("Hello"));
      assertTrue(logBuilder.toString().contains("How are you?"));
    }
  }

  @Nested
  @DisplayName("Process Method")
  class ProcessMethodTests {

    @Test
    @DisplayName("process() with context receives all parameters")
    void processWithContextReceivesParameters() throws Exception {
      AtomicInteger callCount = new AtomicInteger(0);
      String[] receivedUserId = new String[1];
      List<?>[] receivedMessages = new List[1];
      MessageProcessor.ProcessingContext[] receivedContext =
          new MessageProcessor.ProcessingContext[1];

      MessageProcessor processor =
          (userId, messages, context) -> {
            callCount.incrementAndGet();
            receivedUserId[0] = userId;
            receivedMessages[0] = messages;
            receivedContext[0] = context;
          };

      MessageProcessor.ProcessingContext context =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "wamid.1", "wamid.2", MessageProcessor.ProcessingReason.TIMEOUT);

      processor.process(testUserId, testMessages, context);

      assertEquals(1, callCount.get());
      assertEquals(testUserId, receivedUserId[0]);
      assertEquals(testMessages, receivedMessages[0]);
      assertEquals(context, receivedContext[0]);
    }

    @Test
    @DisplayName("process() simplified method delegates to full method")
    void processSimplifiedDelegates() throws Exception {
      AtomicInteger callCount = new AtomicInteger(0);
      MessageProcessor.ProcessingContext[] receivedContext =
          new MessageProcessor.ProcessingContext[1];

      MessageProcessor processor =
          (userId, messages, context) -> {
            callCount.incrementAndGet();
            receivedContext[0] = context;
          };

      processor.process(testUserId, testMessages);

      assertEquals(1, callCount.get());
      assertNotNull(receivedContext[0]);
      assertEquals("", receivedContext[0].batchId());
      assertEquals(
          MessageProcessor.ProcessingReason.UNKNOWN, receivedContext[0].processingReason());
    }

    @Test
    @DisplayName("process() can throw exceptions")
    void processCanThrowExceptions() {
      MessageProcessor processor =
          (userId, messages, context) -> {
            throw new RuntimeException("Processing error");
          };

      assertThrows(
          RuntimeException.class,
          () ->
              processor.process(
                  testUserId, testMessages, MessageProcessor.ProcessingContext.empty()));
    }

    @Test
    @DisplayName("process() receives messages in order")
    void processReceivesMessagesInOrder() throws Exception {
      List<InboundMessage> receivedMessages = new java.util.ArrayList<>();

      MessageProcessor processor = (userId, messages, context) -> receivedMessages.addAll(messages);

      processor.process(testUserId, testMessages, MessageProcessor.ProcessingContext.empty());

      assertEquals(testMessages.size(), receivedMessages.size());
      assertEquals(testMessages.get(0).id(), receivedMessages.get(0).id());
      assertEquals(testMessages.get(1).id(), receivedMessages.get(1).id());
    }
  }

  @Nested
  @DisplayName("ProcessingContext")
  class ProcessingContextTests {

    @Test
    @DisplayName("empty() creates context with empty values")
    void emptyCreatesEmptyContext() {
      MessageProcessor.ProcessingContext context = MessageProcessor.ProcessingContext.empty();

      assertEquals("", context.batchId());
      assertEquals("", context.firstMessageId());
      assertEquals("", context.lastMessageId());
      assertEquals(MessageProcessor.ProcessingReason.UNKNOWN, context.processingReason());
      assertEquals(0, context.retryAttempt());
    }

    @Test
    @DisplayName("create() creates context with values")
    void createCreatesContextWithValues() {
      MessageProcessor.ProcessingContext context =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "msg-first", "msg-last", MessageProcessor.ProcessingReason.TIMEOUT);

      assertEquals("batch-123", context.batchId());
      assertEquals("msg-first", context.firstMessageId());
      assertEquals("msg-last", context.lastMessageId());
      assertEquals(MessageProcessor.ProcessingReason.TIMEOUT, context.processingReason());
      assertEquals(0, context.retryAttempt());
    }

    @Test
    @DisplayName("retry() increments retry attempt")
    void retryIncrementsAttempt() {
      MessageProcessor.ProcessingContext context =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "msg-1", "msg-2", MessageProcessor.ProcessingReason.TIMEOUT);

      MessageProcessor.ProcessingContext retryContext = context.retry();

      assertEquals(1, retryContext.retryAttempt());
      assertEquals("batch-123", retryContext.batchId());
      assertEquals(MessageProcessor.ProcessingReason.TIMEOUT, retryContext.processingReason());
    }

    @Test
    @DisplayName("retry() can be called multiple times")
    void retryCanBeCalledMultipleTimes() {
      MessageProcessor.ProcessingContext context =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "msg-1", "msg-2", MessageProcessor.ProcessingReason.TIMEOUT);

      MessageProcessor.ProcessingContext retry1 = context.retry();
      MessageProcessor.ProcessingContext retry2 = retry1.retry();
      MessageProcessor.ProcessingContext retry3 = retry2.retry();

      assertEquals(0, context.retryAttempt());
      assertEquals(1, retry1.retryAttempt());
      assertEquals(2, retry2.retryAttempt());
      assertEquals(3, retry3.retryAttempt());
    }

    @Test
    @DisplayName("isRetry() returns false for first attempt")
    void isRetryFalseForFirstAttempt() {
      MessageProcessor.ProcessingContext context =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "msg-1", "msg-2", MessageProcessor.ProcessingReason.TIMEOUT);

      assertFalse(context.isRetry());
    }

    @Test
    @DisplayName("isRetry() returns true for retry attempts")
    void isRetryTrueForRetryAttempts() {
      MessageProcessor.ProcessingContext context =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "msg-1", "msg-2", MessageProcessor.ProcessingReason.TIMEOUT);

      MessageProcessor.ProcessingContext retryContext = context.retry();

      assertTrue(retryContext.isRetry());
    }
  }

  @Nested
  @DisplayName("ProcessingReason Enum")
  class ProcessingReasonTests {

    @Test
    @DisplayName("has TIMEOUT reason")
    void hasTimeoutReason() {
      assertEquals(
          MessageProcessor.ProcessingReason.TIMEOUT,
          MessageProcessor.ProcessingReason.valueOf("TIMEOUT"));
    }

    @Test
    @DisplayName("has SILENCE reason")
    void hasSilenceReason() {
      assertEquals(
          MessageProcessor.ProcessingReason.SILENCE,
          MessageProcessor.ProcessingReason.valueOf("SILENCE"));
    }

    @Test
    @DisplayName("has BUFFER_FULL reason")
    void hasBufferFullReason() {
      assertEquals(
          MessageProcessor.ProcessingReason.BUFFER_FULL,
          MessageProcessor.ProcessingReason.valueOf("BUFFER_FULL"));
    }

    @Test
    @DisplayName("has UNKNOWN reason")
    void hasUnknownReason() {
      assertEquals(
          MessageProcessor.ProcessingReason.UNKNOWN,
          MessageProcessor.ProcessingReason.valueOf("UNKNOWN"));
    }
  }

  @Nested
  @DisplayName("Real-World Scenarios")
  class RealWorldScenariosTests {

    @Test
    @DisplayName("handles AI agent processing scenario")
    void handlesAIAgentProcessing() throws Exception {
      List<String> processedInputs = new java.util.ArrayList<>();

      MessageProcessor processor =
          (userId, messages, context) -> {
            // Simulate combining messages for AI
            String combined =
                messages.stream()
                    .map(InboundMessage::extractTextContent)
                    .reduce("", (a, b) -> a + "\n" + b);
            processedInputs.add(combined);
          };

      processor.process(testUserId, testMessages, MessageProcessor.ProcessingContext.empty());

      assertEquals(1, processedInputs.size());
      assertTrue(processedInputs.get(0).contains("Hello"));
      assertTrue(processedInputs.get(0).contains("How are you?"));
    }

    @Test
    @DisplayName("handles batch triggered by timeout")
    void handlesBatchTimeout() throws Exception {
      MessageProcessor.ProcessingReason[] receivedReason = new MessageProcessor.ProcessingReason[1];

      MessageProcessor processor =
          (userId, messages, context) -> receivedReason[0] = context.processingReason();

      MessageProcessor.ProcessingContext context =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "msg-1", "msg-2", MessageProcessor.ProcessingReason.TIMEOUT);

      processor.process(testUserId, testMessages, context);

      assertEquals(MessageProcessor.ProcessingReason.TIMEOUT, receivedReason[0]);
    }

    @Test
    @DisplayName("handles retry after failure")
    void handlesRetryAfterFailure() throws Exception {
      AtomicInteger attempts = new AtomicInteger(0);

      MessageProcessor processor =
          (userId, messages, context) -> {
            attempts.incrementAndGet();
            if (!context.isRetry()) {
              throw new RuntimeException("First attempt fails");
            }
            // Retry succeeds
          };

      MessageProcessor.ProcessingContext firstContext =
          MessageProcessor.ProcessingContext.create(
              "batch-123", "msg-1", "msg-2", MessageProcessor.ProcessingReason.TIMEOUT);

      // First attempt fails
      assertThrows(
          RuntimeException.class, () -> processor.process(testUserId, testMessages, firstContext));
      assertEquals(1, attempts.get());

      // Retry succeeds
      MessageProcessor.ProcessingContext retryContext = firstContext.retry();
      assertDoesNotThrow(() -> processor.process(testUserId, testMessages, retryContext));
      assertEquals(2, attempts.get());
    }
  }

  @Nested
  @DisplayName("Functional Interface")
  class FunctionalInterfaceTests {

    @Test
    @DisplayName("can be implemented as lambda")
    void canBeImplementedAsLambda() throws Exception {
      MessageProcessor processor =
          (userId, messages, context) -> {
            // Simple lambda implementation
          };

      assertDoesNotThrow(
          () ->
              processor.process(
                  testUserId, testMessages, MessageProcessor.ProcessingContext.empty()));
    }

    @Test
    @DisplayName("can be composed with other functions")
    void canBeComposed() throws Exception {
      AtomicInteger counter = new AtomicInteger(0);

      MessageProcessor baseProcessor = (userId, messages, context) -> counter.incrementAndGet();

      MessageProcessor loggingWrapper =
          (userId, messages, context) -> {
            System.out.println("Before processing");
            baseProcessor.process(userId, messages, context);
            System.out.println("After processing");
          };

      loggingWrapper.process(testUserId, testMessages, MessageProcessor.ProcessingContext.empty());

      assertEquals(1, counter.get());
    }
  }

  // Helper methods
  private TextMessage createTextMessage(String id, String body) {
    return new TextMessage(
        testUserId,
        id,
        String.valueOf(System.currentTimeMillis()),
        "text",
        null,
        new TextMessage.TextBody(body));
  }
}
