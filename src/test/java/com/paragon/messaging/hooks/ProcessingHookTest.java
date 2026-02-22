package com.paragon.messaging.hooks;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ProcessingHook}. */
@DisplayName("ProcessingHook")
class ProcessingHookTest {

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodsTests {

    @Test
    @DisplayName("noOp() creates hook that does nothing")
    void noOp_doesNothing() {
      ProcessingHook hook = ProcessingHook.noOp();
      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> hook.execute(context));
    }

    @Test
    @DisplayName("logging() creates hook that logs")
    void logging_logs() {
      List<String> logs = new ArrayList<>();
      ProcessingHook hook = ProcessingHook.logging(logs::add);

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> hook.execute(context));
      assertEquals(1, logs.size());
      assertTrue(logs.get(0).contains("user123"));
      assertTrue(logs.get(0).contains("0 messages"));
    }

    @Test
    @DisplayName("logging() includes retry information")
    void logging_includesRetryInfo() {
      List<String> logs = new ArrayList<>();
      ProcessingHook hook = ProcessingHook.logging(logs::add);

      HookContext original = HookContext.create("user456", List.of());
      HookContext retry = HookContext.forRetry(original, 1);

      assertDoesNotThrow(() -> hook.execute(retry));

      String logMessage = logs.get(0);
      assertTrue(logMessage.contains("user456"));
      assertTrue(logMessage.contains("true") || logMessage.contains("retry"));
    }

    @Test
    @DisplayName("timing() creates pre and post hooks")
    void timing_createsPair() {
      AtomicReference<String> userId = new AtomicReference<>();
      AtomicReference<Duration> duration = new AtomicReference<>();

      ProcessingHook.HookPair pair =
          ProcessingHook.timing(
              (user, dur) -> {
                userId.set(user);
                duration.set(dur);
              });

      assertNotNull(pair.preHook());
      assertNotNull(pair.postHook());
    }

    @Test
    @DisplayName("timing() measures elapsed time")
    void timing_measuresTime() throws Exception {
      AtomicReference<Duration> recordedDuration = new AtomicReference<>();

      ProcessingHook.HookPair pair =
          ProcessingHook.timing((user, dur) -> recordedDuration.set(dur));

      HookContext context = HookContext.create("user123", List.of());

      // Execute pre-hook
      pair.preHook().execute(context);

      // Simulate processing time
      Thread.sleep(50);

      // Execute post-hook
      pair.postHook().execute(context);

      assertNotNull(recordedDuration.get());
      assertTrue(recordedDuration.get().toMillis() >= 50);
    }

    @Test
    @DisplayName("timing() records user ID")
    void timing_recordsUserId() throws Exception {
      AtomicReference<String> recordedUserId = new AtomicReference<>();

      ProcessingHook.HookPair pair = ProcessingHook.timing((user, dur) -> recordedUserId.set(user));

      HookContext context = HookContext.create("user789", List.of());

      pair.preHook().execute(context);
      pair.postHook().execute(context);

      assertEquals("user789", recordedUserId.get());
    }
  }

  @Nested
  @DisplayName("Hook Composition")
  class HookCompositionTests {

    @Test
    @DisplayName("compose() creates hook that executes all hooks")
    void compose_executesAll() {
      AtomicInteger counter = new AtomicInteger(0);

      ProcessingHook hook1 = context -> counter.incrementAndGet();
      ProcessingHook hook2 = context -> counter.incrementAndGet();
      ProcessingHook hook3 = context -> counter.incrementAndGet();

      ProcessingHook composed = ProcessingHook.compose(hook1, hook2, hook3);

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> composed.execute(context));
      assertEquals(3, counter.get());
    }

    @Test
    @DisplayName("compose() executes hooks in order")
    void compose_executesInOrder() {
      List<Integer> executionOrder = new ArrayList<>();

      ProcessingHook hook1 = context -> executionOrder.add(1);
      ProcessingHook hook2 = context -> executionOrder.add(2);
      ProcessingHook hook3 = context -> executionOrder.add(3);

      ProcessingHook composed = ProcessingHook.compose(hook1, hook2, hook3);

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> composed.execute(context));
      assertEquals(List.of(1, 2, 3), executionOrder);
    }

    @Test
    @DisplayName("compose() propagates exceptions")
    void compose_propagatesExceptions() {
      ProcessingHook hook1 = context -> {};
      ProcessingHook hook2 =
          context -> {
            throw new RuntimeException("Error in hook2");
          };
      ProcessingHook hook3 = context -> {};

      ProcessingHook composed = ProcessingHook.compose(hook1, hook2, hook3);

      HookContext context = HookContext.create("user123", List.of());

      Exception exception = assertThrows(Exception.class, () -> composed.execute(context));

      assertTrue(exception.getMessage().contains("Error in hook2"));
    }

    @Test
    @DisplayName("compose() stops on first exception")
    void compose_stopsOnException() {
      AtomicInteger counter = new AtomicInteger(0);

      ProcessingHook hook1 = context -> counter.incrementAndGet();
      ProcessingHook hook2 =
          context -> {
            counter.incrementAndGet();
            throw new RuntimeException("Stop here");
          };
      ProcessingHook hook3 = context -> counter.incrementAndGet();

      ProcessingHook composed = ProcessingHook.compose(hook1, hook2, hook3);

      HookContext context = HookContext.create("user123", List.of());

      assertThrows(Exception.class, () -> composed.execute(context));
      assertEquals(2, counter.get(), "Should have executed only first two hooks");
    }

    @Test
    @DisplayName("compose() with no hooks does nothing")
    void compose_noHooks_doesNothing() {
      ProcessingHook composed = ProcessingHook.compose();

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> composed.execute(context));
    }
  }

  @Nested
  @DisplayName("Hook Execution")
  class HookExecutionTests {

    @Test
    @DisplayName("execute() receives correct context")
    void execute_receivesContext() {
      AtomicReference<HookContext> receivedContext = new AtomicReference<>();

      ProcessingHook hook = context -> receivedContext.set(context);

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> hook.execute(context));
      assertEquals(context, receivedContext.get());
    }

    @Test
    @DisplayName("execute() can throw HookInterruptedException")
    void execute_canThrowInterrupted() {
      ProcessingHook hook =
          context -> {
            throw new HookInterruptedException("Interrupted", "TEST_REASON");
          };

      HookContext context = HookContext.create("user123", List.of());

      assertThrows(HookInterruptedException.class, () -> hook.execute(context));
    }

    @Test
    @DisplayName("execute() can throw generic exception")
    void execute_canThrowException() {
      ProcessingHook hook =
          context -> {
            throw new IllegalStateException("Generic error");
          };

      HookContext context = HookContext.create("user123", List.of());

      assertThrows(IllegalStateException.class, () -> hook.execute(context));
    }

    @Test
    @DisplayName("execute() can modify context metadata")
    void execute_modifiesMetadata() {
      ProcessingHook hook =
          context -> {
            context.putMetadata("processed", true);
            context.putMetadata("timestamp", System.currentTimeMillis());
          };

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> hook.execute(context));
      assertTrue(context.hasMetadata("processed"));
      assertTrue(context.hasMetadata("timestamp"));
      assertEquals(true, context.getMetadata("processed", Boolean.class).orElse(false));
    }
  }

  @Nested
  @DisplayName("Real-World Scenarios")
  class RealWorldScenariosTests {

    @Test
    @DisplayName("content moderation hook")
    void contentModerationHook() {
      ProcessingHook moderationHook =
          context -> {
            for (var msg : context.messages()) {
              String content = msg.extractTextContent();
              if (content != null && content.toLowerCase().contains("spam")) {
                throw new HookInterruptedException("Spam detected", "CONTENT_MODERATION");
              }
            }
          };

      HookContext validContext = HookContext.create("user123", List.of());
      assertDoesNotThrow(() -> moderationHook.execute(validContext));
    }

    @Test
    @DisplayName("metrics collection hook")
    void metricsCollectionHook() {
      AtomicInteger messageCount = new AtomicInteger(0);

      ProcessingHook metricsHook =
          context -> {
            messageCount.set(context.batchSize());
            context.putMetadata("metricsRecorded", true);
          };

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> metricsHook.execute(context));
      assertEquals(0, messageCount.get());
      assertTrue(context.hasMetadata("metricsRecorded"));
    }

    @Test
    @DisplayName("data enrichment hook")
    void dataEnrichmentHook() {
      ProcessingHook enrichmentHook =
          context -> {
            // Simulate fetching user tier
            String userTier = "premium"; // Would normally fetch from DB
            context.putMetadata("userTier", userTier);
            context.putMetadata("enrichedAt", System.currentTimeMillis());
          };

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> enrichmentHook.execute(context));
      assertEquals("premium", context.getMetadata("userTier").orElse(null));
      assertTrue(context.hasMetadata("enrichedAt"));
    }

    @Test
    @DisplayName("audit logging hook")
    void auditLoggingHook() {
      List<String> auditLog = new ArrayList<>();

      ProcessingHook auditHook =
          context -> {
            String entry =
                String.format(
                    "User %s: %d messages, retry: %s",
                    context.userId(), context.batchSize(), context.isRetry());
            auditLog.add(entry);
          };

      HookContext context = HookContext.create("user456", List.of());

      assertDoesNotThrow(() -> auditHook.execute(context));
      assertEquals(1, auditLog.size());
      assertTrue(auditLog.get(0).contains("user456"));
    }

    @Test
    @DisplayName("chained pre and post hooks")
    void chainedPrePostHooks() {
      List<String> events = new ArrayList<>();

      ProcessingHook preHook =
          context -> {
            events.add("PRE");
            context.putMetadata("startTime", System.currentTimeMillis());
          };

      ProcessingHook postHook =
          context -> {
            events.add("POST");
            Long startTime = context.getMetadata("startTime", Long.class).orElse(0L);
            long duration = System.currentTimeMillis() - startTime;
            context.putMetadata("duration", duration);
          };

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(
          () -> {
            preHook.execute(context);
            // Simulate processing
            postHook.execute(context);
          });

      assertEquals(List.of("PRE", "POST"), events);
      assertTrue(context.hasMetadata("duration"));
    }
  }

  @Nested
  @DisplayName("HookPair")
  class HookPairTests {

    @Test
    @DisplayName("HookPair contains pre and post hooks")
    void hookPair_containsHooks() {
      ProcessingHook pre = context -> {};
      ProcessingHook post = context -> {};

      ProcessingHook.HookPair pair = new ProcessingHook.HookPair(pre, post);

      assertEquals(pre, pair.preHook());
      assertEquals(post, pair.postHook());
    }

    @Test
    @DisplayName("timing() creates functional HookPair")
    void timing_functionalPair() {
      AtomicInteger callCount = new AtomicInteger(0);

      ProcessingHook.HookPair pair =
          ProcessingHook.timing((user, dur) -> callCount.incrementAndGet());

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(
          () -> {
            pair.preHook().execute(context);
            pair.postHook().execute(context);
          });

      assertEquals(1, callCount.get());
    }
  }

  @Nested
  @DisplayName("Lambda Hooks")
  class LambdaHooksTests {

    @Test
    @DisplayName("lambda hook can be created inline")
    void lambdaHook_inline() {
      AtomicBoolean executed = new AtomicBoolean(false);

      ProcessingHook hook = context -> executed.set(true);

      HookContext context = HookContext.create("user123", List.of());

      assertDoesNotThrow(() -> hook.execute(context));
      assertTrue(executed.get());
    }

    @Test
    @DisplayName("lambda hook can access context properties")
    void lambdaHook_accessContext() {
      AtomicReference<String> capturedUserId = new AtomicReference<>();
      AtomicInteger capturedBatchSize = new AtomicInteger();

      ProcessingHook hook =
          context -> {
            capturedUserId.set(context.userId());
            capturedBatchSize.set(context.batchSize());
          };

      HookContext context = HookContext.create("user789", List.of());

      assertDoesNotThrow(() -> hook.execute(context));
      assertEquals("user789", capturedUserId.get());
      assertEquals(0, capturedBatchSize.get());
    }
  }
}
