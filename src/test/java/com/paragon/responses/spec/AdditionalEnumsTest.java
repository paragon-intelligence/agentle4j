package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Additional tests for more spec package enums. */
@DisplayName("Additional Spec Enums Tests")
class AdditionalEnumsTest {

  @Nested
  @DisplayName("Background enum")
  class BackgroundTests {
    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertEquals(3, Background.values().length);
      assertNotNull(Background.valueOf("TRANSPARENT"));
      assertNotNull(Background.valueOf("OPAQUE"));
      assertNotNull(Background.valueOf("AUTO"));
    }
  }

  @Nested
  @DisplayName("AllowedToolsMode enum")
  class AllowedToolsModeTests {
    @Test
    @DisplayName("has values and can be iterated")
    void hasValues() {
      assertTrue(AllowedToolsMode.values().length > 0);
      for (AllowedToolsMode mode : AllowedToolsMode.values()) {
        assertNotNull(mode.name());
      }
    }
  }

  @Nested
  @DisplayName("ApplyPatchToolCallStatus enum")
  class ApplyPatchToolCallStatusTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ApplyPatchToolCallStatus.values().length > 0);
      for (ApplyPatchToolCallStatus status : ApplyPatchToolCallStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  @Nested
  @DisplayName("ApplyPatchType enum")
  class ApplyPatchTypeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ApplyPatchType.values().length > 0);
      for (ApplyPatchType type : ApplyPatchType.values()) {
        assertNotNull(type.name());
      }
    }
  }

  @Nested
  @DisplayName("ClickButton enum")
  class ClickButtonTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ClickButton.values().length > 0);
      for (ClickButton button : ClickButton.values()) {
        assertNotNull(button.name());
      }
    }
  }

  @Nested
  @DisplayName("CodeInterpreterToolCallStatus enum")
  class CodeInterpreterToolCallStatusTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(CodeInterpreterToolCallStatus.values().length > 0);
      for (CodeInterpreterToolCallStatus status : CodeInterpreterToolCallStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  @Nested
  @DisplayName("ComputerToolCallOutputStatus enum")
  class ComputerToolCallOutputStatusTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ComputerToolCallOutputStatus.values().length > 0);
      for (ComputerToolCallOutputStatus status : ComputerToolCallOutputStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  @Nested
  @DisplayName("ComputerToolCallStatus enum")
  class ComputerToolCallStatusTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ComputerToolCallStatus.values().length > 0);
      for (ComputerToolCallStatus status : ComputerToolCallStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  @Nested
  @DisplayName("ComputerUseEnvironment enum")
  class ComputerUseEnvironmentTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ComputerUseEnvironment.values().length > 0);
      for (ComputerUseEnvironment env : ComputerUseEnvironment.values()) {
        assertNotNull(env.name());
      }
    }
  }

  @Nested
  @DisplayName("ErrorCode enum")
  class ErrorCodeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ErrorCode.values().length > 0);
      for (ErrorCode code : ErrorCode.values()) {
        assertNotNull(code.name());
      }
    }
  }

  @Nested
  @DisplayName("FileSearchToolCallStatus enum")
  class FileSearchToolCallStatusTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(FileSearchToolCallStatus.values().length > 0);
      for (FileSearchToolCallStatus status : FileSearchToolCallStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  @Nested
  @DisplayName("ImageSize enum")
  class ImageSizeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ImageSize.values().length > 0);
      for (ImageSize size : ImageSize.values()) {
        assertNotNull(size.name());
      }
    }
  }

  @Nested
  @DisplayName("InputFidelity enum")
  class InputFidelityTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(InputFidelity.values().length > 0);
      for (InputFidelity fidelity : InputFidelity.values()) {
        assertNotNull(fidelity.name());
      }
    }
  }

  @Nested
  @DisplayName("McpToolCallStatus enum")
  class McpToolCallStatusTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(McpToolCallStatus.values().length > 0);
      for (McpToolCallStatus status : McpToolCallStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  @Nested
  @DisplayName("ModelVerbosityConfig enum")
  class ModelVerbosityConfigTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ModelVerbosityConfig.values().length > 0);
      for (ModelVerbosityConfig config : ModelVerbosityConfig.values()) {
        assertNotNull(config.name());
      }
    }
  }

  @Nested
  @DisplayName("OutputDataInclude enum")
  class OutputDataIncludeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(OutputDataInclude.values().length > 0);
      for (OutputDataInclude include : OutputDataInclude.values()) {
        assertNotNull(include.name());
      }
    }
  }

  @Nested
  @DisplayName("OutputFormat enum")
  class OutputFormatTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(OutputFormat.values().length > 0);
      for (OutputFormat format : OutputFormat.values()) {
        assertNotNull(format.name());
      }
    }
  }

  @Nested
  @DisplayName("Quality enum")
  class QualityTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(Quality.values().length > 0);
      for (Quality quality : Quality.values()) {
        assertNotNull(quality.name());
      }
    }
  }

  @Nested
  @DisplayName("Quantization enum")
  class QuantizationTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(Quantization.values().length > 0);
      for (Quantization quantization : Quantization.values()) {
        assertNotNull(quantization.name());
      }
    }
  }

  @Nested
  @DisplayName("ReasoningStatus enum")
  class ReasoningStatusTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ReasoningStatus.values().length > 0);
      for (ReasoningStatus status : ReasoningStatus.values()) {
        assertNotNull(status.name());
      }
    }
  }

  @Nested
  @DisplayName("ResponseOutputType enum")
  class ResponseOutputTypeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ResponseOutputType.values().length > 0);
      for (ResponseOutputType type : ResponseOutputType.values()) {
        assertNotNull(type.name());
      }
    }
  }

  @Nested
  @DisplayName("ServiceTierType enum")
  class ServiceTierTypeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ServiceTierType.values().length > 0);
      for (ServiceTierType tier : ServiceTierType.values()) {
        assertNotNull(tier.name());
      }
    }
  }

  @Nested
  @DisplayName("ToolChoiceMode enum")
  class ToolChoiceModeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ToolChoiceMode.values().length > 0);
      for (ToolChoiceMode mode : ToolChoiceMode.values()) {
        assertNotNull(mode.name());
      }
    }
  }

  @Nested
  @DisplayName("WebSearchSearchContextSize enum")
  class WebSearchSearchContextSizeTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(WebSearchSearchContextSize.values().length > 0);
      for (WebSearchSearchContextSize size : WebSearchSearchContextSize.values()) {
        assertNotNull(size.name());
      }
    }
  }

  @Nested
  @DisplayName("OpenRouter enums")
  class OpenRouterEnumTests {
    @Test
    @DisplayName("OpenRouterProviderSortingStrategy has values")
    void providerSortingStrategy() {
      assertTrue(OpenRouterProviderSortingStrategy.values().length > 0);
      for (OpenRouterProviderSortingStrategy strategy :
          OpenRouterProviderSortingStrategy.values()) {
        assertNotNull(strategy.name());
      }
    }

    @Test
    @DisplayName("OpenRouterRouteStrategy has values")
    void routeStrategy() {
      assertTrue(OpenRouterRouteStrategy.values().length > 0);
      for (OpenRouterRouteStrategy strategy : OpenRouterRouteStrategy.values()) {
        assertNotNull(strategy.name());
      }
    }

    @Test
    @DisplayName("OpenRouterWebEngine has values")
    void webEngine() {
      assertTrue(OpenRouterWebEngine.values().length > 0);
      for (OpenRouterWebEngine engine : OpenRouterWebEngine.values()) {
        assertNotNull(engine.name());
      }
    }

    @Test
    @DisplayName("OpenRouterPdfEngine has values")
    void pdfEngine() {
      assertTrue(OpenRouterPdfEngine.values().length > 0);
      for (OpenRouterPdfEngine engine : OpenRouterPdfEngine.values()) {
        assertNotNull(engine.name());
      }
    }
  }

  @Nested
  @DisplayName("DataCollectionSetting enum")
  class DataCollectionSettingTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(DataCollectionSetting.values().length > 0);
      for (DataCollectionSetting setting : DataCollectionSetting.values()) {
        assertNotNull(setting.name());
      }
    }
  }

  @Nested
  @DisplayName("ImageGenerationModeration enum")
  class ImageGenerationModerationTests {
    @Test
    @DisplayName("has values")
    void hasValues() {
      assertTrue(ImageGenerationModeration.values().length > 0);
      for (ImageGenerationModeration moderation : ImageGenerationModeration.values()) {
        assertNotNull(moderation.name());
      }
    }
  }
}
