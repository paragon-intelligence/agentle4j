package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.OpenRouterCustomPayload;
import com.paragon.responses.spec.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Custom deserializer for CreateResponsePayload to handle @JsonUnwrapped OpenRouterCustomPayload.
 *
 * <p>Jackson doesn't support @JsonUnwrapped with constructor parameters, so we need a custom
 * deserializer to read the unwrapped OpenRouter fields directly from the JSON.
 */
public class CreateResponsePayloadDeserializer extends JsonDeserializer<CreateResponsePayload> {

  @Override
  public CreateResponsePayload deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    ObjectMapper mapper = (ObjectMapper) p.getCodec();
    JsonNode node = mapper.readTree(p);

    // Standard fields
    @Nullable Boolean background = getBoolean(node, "background");
    @Nullable String conversation = getString(node, "conversation");
    @Nullable List<OutputDataInclude> include =
        getList(mapper, node, "include", new TypeReference<>() {});
    @Nullable List<ResponseInputItem> input =
        getListWithType(mapper, node, "input", ResponseInputItem.class);
    @Nullable String instructions = getString(node, "instructions");
    @Nullable Integer maxOutputTokens = getInteger(node, "max_output_tokens");
    @Nullable Integer maxToolCalls = getInteger(node, "max_tool_calls");
    @Nullable Map<String, String> metadata =
        getObject(mapper, node, "metadata", new TypeReference<>() {});
    @Nullable String model = getString(node, "model");
    @Nullable Boolean parallelToolCalls = getBoolean(node, "parallel_tool_calls");
    @Nullable PromptTemplate prompt = getObject(mapper, node, "prompt", new TypeReference<>() {});
    @Nullable String promptCacheKey = getString(node, "prompt_cache_key");
    @Nullable String promptCacheRetention = getString(node, "prompt_cache_retention");
    @Nullable ReasoningConfig reasoning =
        getObject(mapper, node, "reasoning", new TypeReference<>() {});
    @Nullable String safetyIdentifier = getString(node, "safety_identifier");
    @Nullable ServiceTierType serviceTier =
        getObject(mapper, node, "service_tier", new TypeReference<>() {});
    @Nullable Boolean store = getBoolean(node, "store");
    @Nullable Boolean stream = getBoolean(node, "stream");
    @Nullable StreamOptions streamOptions =
        getObject(mapper, node, "stream_options", new TypeReference<>() {});
    @Nullable Double temperature = getDouble(node, "temperature");
    @Nullable TextConfigurationOptions text =
        getObject(mapper, node, "text", new TypeReference<>() {});
    @Nullable ToolChoice toolChoice = getToolChoice(mapper, node);
    @Nullable List<Tool> tools = getList(mapper, node, "tools", new TypeReference<>() {});
    @Nullable Integer topLogprobs = getInteger(node, "top_logprobs");
    @Nullable Number topP = getNumber(node, "top_p");
    @Nullable Truncation truncation =
        getObject(mapper, node, "truncation", new TypeReference<>() {});

    // OpenRouter custom payload - unwrapped fields
    @Nullable List<OpenRouterPlugin> plugins =
        getList(mapper, node, "plugins", new TypeReference<>() {});
    @Nullable OpenRouterProviderConfig providerConfig =
        getObject(mapper, node, "provider_config", new TypeReference<>() {});
    @Nullable OpenRouterRouteStrategy route =
        getObject(mapper, node, "route", new TypeReference<>() {});
    @Nullable String user = getString(node, "user");
    @Nullable String sessionId = getString(node, "session_id");

    OpenRouterCustomPayload openRouterCustomPayload = null;
    if (plugins != null
        || providerConfig != null
        || route != null
        || user != null
        || sessionId != null) {
      openRouterCustomPayload =
          new OpenRouterCustomPayload(plugins, providerConfig, route, user, sessionId);
    }

    return new CreateResponsePayload(
        background,
        conversation,
        include,
        input,
        instructions,
        maxOutputTokens,
        maxToolCalls,
        metadata,
        model,
        parallelToolCalls,
        prompt,
        promptCacheKey,
        promptCacheRetention,
        reasoning,
        safetyIdentifier,
        serviceTier,
        store,
        stream,
        streamOptions,
        temperature,
        text,
        toolChoice,
        tools,
        topLogprobs,
        topP,
        truncation,
        openRouterCustomPayload);
  }

  private @Nullable ToolChoice getToolChoice(ObjectMapper mapper, JsonNode node)
      throws IOException {
    if (!node.has("tool_choice") || node.get("tool_choice").isNull()) {
      return null;
    }

    JsonNode toolChoiceNode = node.get("tool_choice");

    // If it's a string, parse as ToolChoiceMode
    if (toolChoiceNode.isTextual()) {
      String value = toolChoiceNode.asText().toUpperCase();
      return ToolChoiceMode.valueOf(value);
    }

    // Otherwise it's an object, parse as AllowedTools
    return mapper.treeToValue(toolChoiceNode, AllowedTools.class);
  }

  private @Nullable String getString(JsonNode node, String fieldName) {
    return node.has(fieldName) && !node.get(fieldName).isNull()
        ? node.get(fieldName).asText()
        : null;
  }

  private @Nullable Boolean getBoolean(JsonNode node, String fieldName) {
    return node.has(fieldName) && !node.get(fieldName).isNull()
        ? node.get(fieldName).asBoolean()
        : null;
  }

  private @Nullable Integer getInteger(JsonNode node, String fieldName) {
    return node.has(fieldName) && !node.get(fieldName).isNull()
        ? node.get(fieldName).asInt()
        : null;
  }

  private @Nullable Double getDouble(JsonNode node, String fieldName) {
    return node.has(fieldName) && !node.get(fieldName).isNull()
        ? node.get(fieldName).asDouble()
        : null;
  }

  private @Nullable Number getNumber(JsonNode node, String fieldName) {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    JsonNode valueNode = node.get(fieldName);
    if (valueNode.isInt()) {
      return valueNode.asInt();
    } else if (valueNode.isDouble() || valueNode.isFloat()) {
      return valueNode.asDouble();
    }
    return valueNode.asDouble();
  }

  private @Nullable <T> List<T> getList(
      ObjectMapper mapper, JsonNode node, String fieldName, TypeReference<List<T>> typeRef)
      throws IOException {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    return mapper.treeToValue(node.get(fieldName), mapper.constructType(typeRef.getType()));
  }

  private @Nullable <T> List<T> getListWithType(
      ObjectMapper mapper, JsonNode node, String fieldName, Class<T> elementClass)
      throws IOException {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    var listType = mapper.getTypeFactory().constructCollectionType(List.class, elementClass);
    return mapper.treeToValue(node.get(fieldName), listType);
  }

  private @Nullable <T> T getObject(
      ObjectMapper mapper, JsonNode node, String fieldName, TypeReference<T> typeRef)
      throws IOException {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    return mapper.treeToValue(node.get(fieldName), mapper.constructType(typeRef.getType()));
  }
}
