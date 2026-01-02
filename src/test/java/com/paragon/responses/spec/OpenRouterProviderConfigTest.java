package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.LlmProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for OpenRouterProviderConfig and its Builder.
 *
 * <p>Tests cover: - Builder pattern - All configuration fields - Build method
 */
@DisplayName("OpenRouterProviderConfig Tests")
class OpenRouterProviderConfigTest {

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("creates empty config with builder")
    void createsEmptyConfigWithBuilder() {
      OpenRouterProviderConfig config = OpenRouterProviderConfig.builder().build();

      assertNotNull(config);
      assertNull(config.allowFallbacks());
      assertNull(config.requireParameters());
      assertNull(config.dataCollection());
      assertNull(config.zdr());
      assertNull(config.enforceDistillableText());
      assertNull(config.order());
      assertNull(config.only());
      assertNull(config.ignore());
      assertNull(config.quantizations());
      assertNull(config.sort());
      assertNull(config.maxPrice());
    }

    @Test
    @DisplayName("builder allows setting allowFallbacks")
    void builderAllowsSettingAllowFallbacks() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.allowFallbacks = true;
      OpenRouterProviderConfig config = builder.build();

      assertTrue(config.allowFallbacks());
    }

    @Test
    @DisplayName("builder allows setting requireParameters")
    void builderAllowsSettingRequireParameters() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.requireParameters = true;
      OpenRouterProviderConfig config = builder.build();

      assertTrue(config.requireParameters());
    }

    @Test
    @DisplayName("builder allows setting dataCollection")
    void builderAllowsSettingDataCollection() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.dataCollection = DataCollectionSetting.ALLOW;
      OpenRouterProviderConfig config = builder.build();

      assertEquals(DataCollectionSetting.ALLOW, config.dataCollection());
    }

    @Test
    @DisplayName("builder allows setting zdr")
    void builderAllowsSettingZdr() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.zdr = true;
      OpenRouterProviderConfig config = builder.build();

      assertTrue(config.zdr());
    }

    @Test
    @DisplayName("builder allows setting enforceDistillableText")
    void builderAllowsSettingEnforceDistillableText() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.enforceDistillableText = false;
      OpenRouterProviderConfig config = builder.build();

      assertFalse(config.enforceDistillableText());
    }

    @Test
    @DisplayName("builder allows setting order")
    void builderAllowsSettingOrder() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.order = List.of(LlmProvider.ANTHROPIC, LlmProvider.OPENAI);
      OpenRouterProviderConfig config = builder.build();

      assertEquals(2, config.order().size());
      assertEquals(LlmProvider.ANTHROPIC, config.order().get(0));
    }

    @Test
    @DisplayName("builder allows setting only")
    void builderAllowsSettingOnly() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.only = List.of(LlmProvider.GOOGLE);
      OpenRouterProviderConfig config = builder.build();

      assertEquals(1, config.only().size());
      assertEquals(LlmProvider.GOOGLE, config.only().get(0));
    }

    @Test
    @DisplayName("builder allows setting ignore")
    void builderAllowsSettingIgnore() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.ignore = List.of(LlmProvider.AZURE);
      OpenRouterProviderConfig config = builder.build();

      assertEquals(1, config.ignore().size());
    }

    @Test
    @DisplayName("builder allows setting quantizations")
    void builderAllowsSettingQuantizations() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.quantizations = List.of(Quantization.INT4, Quantization.INT8);
      OpenRouterProviderConfig config = builder.build();

      assertEquals(2, config.quantizations().size());
    }

    @Test
    @DisplayName("builder allows setting sort")
    void builderAllowsSettingSort() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.sort = OpenRouterProviderSortingStrategy.PRICE;
      OpenRouterProviderConfig config = builder.build();

      assertEquals(OpenRouterProviderSortingStrategy.PRICE, config.sort());
    }

    @Test
    @DisplayName("builder allows setting maxPrice")
    void builderAllowsSettingMaxPrice() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.maxPrice = new OpenRouterRequestMaxPriceSettings(0.01, 0.02, null, null, null);
      OpenRouterProviderConfig config = builder.build();

      assertNotNull(config.maxPrice());
    }

    @Test
    @DisplayName("builder with all fields set")
    void builderWithAllFieldsSet() {
      OpenRouterProviderConfig.Builder builder = OpenRouterProviderConfig.builder();
      builder.allowFallbacks = true;
      builder.requireParameters = false;
      builder.dataCollection = DataCollectionSetting.DENY;
      builder.zdr = true;
      builder.enforceDistillableText = true;
      builder.order = List.of(LlmProvider.ANTHROPIC);
      builder.only = List.of(LlmProvider.OPENAI);
      builder.ignore = List.of(LlmProvider.AZURE);
      builder.quantizations = List.of();
      builder.sort = OpenRouterProviderSortingStrategy.THROUGHPUT;
      builder.maxPrice = new OpenRouterRequestMaxPriceSettings(0.05, 0.10, null, null, null);

      OpenRouterProviderConfig config = builder.build();

      assertTrue(config.allowFallbacks());
      assertFalse(config.requireParameters());
      assertEquals(DataCollectionSetting.DENY, config.dataCollection());
      assertTrue(config.zdr());
      assertTrue(config.enforceDistillableText());
      assertEquals(1, config.order().size());
      assertEquals(1, config.only().size());
      assertEquals(1, config.ignore().size());
      assertNotNull(config.quantizations());
      assertEquals(OpenRouterProviderSortingStrategy.THROUGHPUT, config.sort());
      assertNotNull(config.maxPrice());
    }
  }
}
