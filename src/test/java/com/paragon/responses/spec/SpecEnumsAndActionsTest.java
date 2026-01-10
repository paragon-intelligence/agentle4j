package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for enums in the spec package.
 */
@DisplayName("Spec Enums Tests")
class SpecEnumsAndActionsTest {

  // =========================================================================
  // AllowedToolsMode Enum
  // =========================================================================
  @Nested
  @DisplayName("AllowedToolsMode Enum")
  class AllowedToolsModeTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(AllowedToolsMode.AUTO);
      assertNotNull(AllowedToolsMode.REQUIRED);
    }

    @Test
    @DisplayName("should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(AllowedToolsMode.AUTO, AllowedToolsMode.valueOf("AUTO"));
      assertEquals(AllowedToolsMode.REQUIRED, AllowedToolsMode.valueOf("REQUIRED"));
    }
  }

  // =========================================================================
  // Background Enum
  // =========================================================================
  @Nested
  @DisplayName("Background Enum")
  class BackgroundTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(Background.TRANSPARENT);
      assertNotNull(Background.OPAQUE);
      assertNotNull(Background.AUTO);
    }

    @Test
    @DisplayName("should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(Background.TRANSPARENT, Background.valueOf("TRANSPARENT"));
      assertEquals(Background.OPAQUE, Background.valueOf("OPAQUE"));
      assertEquals(Background.AUTO, Background.valueOf("AUTO"));
    }
  }

  // =========================================================================
  // DataCollectionSetting Enum
  // =========================================================================
  @Nested
  @DisplayName("DataCollectionSetting Enum")
  class DataCollectionSettingTest {

    @Test
    @DisplayName("should have DENY and ALLOW values")
    void shouldHaveDenyAndAllowValues() {
      assertNotNull(DataCollectionSetting.DENY);
      assertNotNull(DataCollectionSetting.ALLOW);
    }

    @Test
    @DisplayName("should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(DataCollectionSetting.DENY, DataCollectionSetting.valueOf("DENY"));
      assertEquals(DataCollectionSetting.ALLOW, DataCollectionSetting.valueOf("ALLOW"));
    }
  }

  // =========================================================================
  // ImageDetail Enum
  // =========================================================================
  @Nested
  @DisplayName("ImageDetail Enum")
  class ImageDetailTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(ImageDetail.LOW);
      assertNotNull(ImageDetail.HIGH);
      assertNotNull(ImageDetail.AUTO);
    }
  }

  // =========================================================================
  // ImageSize Enum
  // =========================================================================
  @Nested
  @DisplayName("ImageSize Enum")
  class ImageSizeTest {

    @Test
    @DisplayName("should have all expected size values")
    void shouldHaveAllExpectedSizeValues() {
      assertNotNull(ImageSize.AUTO);
      assertNotNull(ImageSize.SQUARE_1024);
      assertNotNull(ImageSize.PORTRAIT_1024);
      assertNotNull(ImageSize.LANDSCAPE_1024);
    }
  }

  // =========================================================================
  // Quality Enum
  // =========================================================================
  @Nested
  @DisplayName("Quality Enum")
  class QualityTest {

    @Test
    @DisplayName("should have quality values")
    void shouldHaveQualityValues() {
      assertNotNull(Quality.LOW);
      assertNotNull(Quality.MEDIUM);
      assertNotNull(Quality.HIGH);
      assertNotNull(Quality.AUTO);
    }
  }

  // =========================================================================
  // MessageRole Enum
  // =========================================================================
  @Nested
  @DisplayName("MessageRole Enum")
  class MessageRoleTest {

    @Test
    @DisplayName("should have standard roles")
    void shouldHaveStandardRoles() {
      assertNotNull(MessageRole.USER);
      assertNotNull(MessageRole.ASSISTANT);
      assertNotNull(MessageRole.DEVELOPER);
    }
  }

  // =========================================================================
  // ReasoningEffort Enum
  // =========================================================================
  @Nested
  @DisplayName("ReasoningEffort Enum")
  class ReasoningEffortTest {

    @Test
    @DisplayName("should have reasoning effort levels")
    void shouldHaveReasoningEffortLevels() {
      assertNotNull(ReasoningEffort.LOW);
      assertNotNull(ReasoningEffort.MEDIUM);
      assertNotNull(ReasoningEffort.HIGH);
    }
  }

  // =========================================================================
  // ServiceTierType Enum
  // =========================================================================
  @Nested
  @DisplayName("ServiceTierType Enum")
  class ServiceTierTypeTest {

    @Test
    @DisplayName("should have service tier types")
    void shouldHaveServiceTierTypes() {
      assertNotNull(ServiceTierType.AUTO);
      assertNotNull(ServiceTierType.DEFAULT);
      assertNotNull(ServiceTierType.FLEX);
    }
  }

  // =========================================================================
  // Truncation Enum
  // =========================================================================
  @Nested
  @DisplayName("Truncation Enum")
  class TruncationTest {

    @Test
    @DisplayName("should have truncation modes")
    void shouldHaveTruncationModes() {
      assertNotNull(Truncation.AUTO);
      assertNotNull(Truncation.DISABLED);
    }
  }

  // =========================================================================
  // OutputFormat Enum
  // =========================================================================
  @Nested
  @DisplayName("OutputFormat Enum")
  class OutputFormatTest {

    @Test
    @DisplayName("should have output format options")
    void shouldHaveOutputFormatOptions() {
      assertNotNull(OutputFormat.PNG);
      assertNotNull(OutputFormat.WEBP);
      assertNotNull(OutputFormat.JPEG);
    }
  }

  // =========================================================================
  // ClickButton Enum
  // =========================================================================
  @Nested
  @DisplayName("ClickButton Enum")
  class ClickButtonTest {

    @Test
    @DisplayName("should have click button options")
    void shouldHaveClickButtonOptions() {
      assertNotNull(ClickButton.LEFT);
      assertNotNull(ClickButton.RIGHT);
      assertNotNull(ClickButton.WHEEL);
      assertNotNull(ClickButton.BACK);
      assertNotNull(ClickButton.FORWARD);
    }
  }

  // =========================================================================
  // ComputerUseEnvironment Enum
  // =========================================================================
  @Nested
  @DisplayName("ComputerUseEnvironment Enum")
  class ComputerUseEnvironmentTest {

    @Test
    @DisplayName("should have environment values")
    void shouldHaveEnvironmentValues() {
      assertNotNull(ComputerUseEnvironment.BROWSER);
      assertNotNull(ComputerUseEnvironment.MAC);
      assertNotNull(ComputerUseEnvironment.UBUNTU);
      assertNotNull(ComputerUseEnvironment.WINDOWS);
    }
  }

  // =========================================================================
  // ApplyPatchType Enum
  // =========================================================================
  @Nested
  @DisplayName("ApplyPatchType Enum")
  class ApplyPatchTypeTest {

    @Test
    @DisplayName("should have patch type values")
    void shouldHavePatchTypeValues() {
      assertNotNull(ApplyPatchType.CREATE_FILE);
      assertNotNull(ApplyPatchType.UPDATE_FILE);
      assertNotNull(ApplyPatchType.DELETE_FILE);
    }
  }
}
