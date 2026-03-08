package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.AgentResult;
import com.paragon.agents.AgenticContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SelfCorrectionConfig")
class SelfCorrectionConfigTest {

  @Nested
  @DisplayName("builder")
  class BuilderTests {

    @Test
    @DisplayName("defaults: maxRetries=3, retryOn=error, default template")
    void defaults() {
      SelfCorrectionConfig config = SelfCorrectionConfig.builder().build();
      assertEquals(3, config.maxRetries());
      assertNotNull(config.retryOn());
      assertTrue(config.feedbackTemplate().contains("{error}"));
    }

    @Test
    @DisplayName("rejects maxRetries <= 0")
    void rejectsNonPositiveMaxRetries() {
      assertThrows(IllegalArgumentException.class,
          () -> SelfCorrectionConfig.builder().maxRetries(0).build());
    }

    @Test
    @DisplayName("rejects feedbackTemplate without {error} placeholder")
    void rejectsTemplateWithoutPlaceholder() {
      assertThrows(IllegalArgumentException.class,
          () -> SelfCorrectionConfig.builder()
              .feedbackTemplate("no placeholder here")
              .build());
    }
  }

  @Nested
  @DisplayName("formatFeedback")
  class FormatFeedback {

    @Test
    @DisplayName("replaces {error} with actual error message")
    void replacesPlaceholder() {
      SelfCorrectionConfig config = SelfCorrectionConfig.builder()
          .feedbackTemplate("Error was: {error}. Please fix it.")
          .build();

      String result = config.formatFeedback("NullPointerException at line 42");
      assertEquals("Error was: NullPointerException at line 42. Please fix it.", result);
    }
  }

  @Nested
  @DisplayName("retryOn predicate")
  class RetryOnPredicate {

    @Test
    @DisplayName("default retryOn predicate fires on error results")
    void defaultRetryOnFiresOnError() {
      SelfCorrectionConfig config = SelfCorrectionConfig.builder().build();
      AgenticContext ctx = AgenticContext.create();
      AgentResult error = AgentResult.error(new RuntimeException("oops"), ctx, 1);
      assertTrue(config.retryOn().test(error));
    }

    @Test
    @DisplayName("custom retryOn predicate is respected")
    void customRetryOnIsRespected() {
      SelfCorrectionConfig config = SelfCorrectionConfig.builder()
          .retryOn(result -> false) // never retry
          .build();
      AgenticContext ctx = AgenticContext.create();
      AgentResult error = AgentResult.error(new RuntimeException("oops"), ctx, 1);
      assertFalse(config.retryOn().test(error));
    }
  }
}
