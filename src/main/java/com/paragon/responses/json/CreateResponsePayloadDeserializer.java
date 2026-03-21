package com.paragon.responses.json;

import com.paragon.responses.OpenRouterCustomPayload;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.*;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Custom deserializer for CreateResponsePayload to handle @JsonUnwrapped OpenRouterCustomPayload.
 *
 * <p>Jackson doesn't support @JsonUnwrapped with constructor parameters, so we need a custom
 * deserializer to read the unwrapped OpenRouter fields directly from the JSON.
 */
public class CreateResponsePayloadDeserializer extends ValueDeserializer<CreateResponsePayload> {

  @Override
  public CreateResponsePayload deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    JsonNode node = p.readValueAsTree();

    // Standard fields
    @Nullable Boolean background = getBoolean(node, "background");
    @Nullable String conversation = getString(node, "conversation");
    @Nullable List<OutputDataInclude> include =
        getList(ctxt, node, "include", new TypeReference<>() {});
    @Nullable List<ResponseInputItem> input =
        getListWithType(ctxt, node, "input", ResponseInputItem.class);
    @Nullable String instructions = getString(node, "instructions");
    @Nullable Integer maxOutputTokens = getInteger(node, "max_output_tokens");
    @Nullable Integer maxToolCalls = getInteger(node, "max_tool_calls");
    @Nullable Map<String, String> metadata =
        getObject(ctxt, node, "metadata", new TypeReference<>() {});
    @Nullable String model = getString(node, "model");
    @Nullable Boolean parallelToolCalls = getBoolean(node, "parallel_tool_calls");
    @Nullable PromptTemplate prompt = getObject(ctxt, node, "prompt", new TypeReference<>() {});
    @Nullable String promptCacheKey = getString(node, "prompt_cache_key");
    @Nullable String promptCacheRetention = getString(node, "prompt_cache_retention");
    @Nullable ReasoningConfig reasoning =
        getObject(ctxt, node, "reasoning", new TypeReference<>() {});
    @Nullable String safetyIdentifier = getString(node, "safety_identifier");
    @Nullable ServiceTierType serviceTier =
        getObject(ctxt, node, "service_tier", new TypeReference<>() {});
    @Nullable Boolean store = getBoolean(node, "store");
    @Nullable Boolean stream = getBoolean(node, "stream");
    @Nullable StreamOptions streamOptions =
        getObject(ctxt, node, "stream_options", new TypeReference<>() {});
    @Nullable Double temperature = getDouble(node, "temperature");
    @Nullable TextConfigurationOptions text =
        getObject(ctxt, node, "text", new TypeReference<>() {});
    @Nullable ToolChoice toolChoice = getToolChoice(ctxt, node);
    @Nullable List<Tool> tools = getList(ctxt, node, "tools", new TypeReference<>() {});
    @Nullable Integer topLogprobs = getInteger(node, "top_logprobs");
    @Nullable Number topP = getNumber(node, "top_p");
    @Nullable Truncation truncation = getObject(ctxt, node, "truncation", new TypeReference<>() {});

    // OpenRouter custom payload - unwrapped fields
    @Nullable List<OpenRouterPlugin> plugins =
        getList(ctxt, node, "plugins", new TypeReference<>() {});
    @Nullable OpenRouterProviderConfig providerConfig =
        getObject(ctxt, node, "provider_config", new TypeReference<>() {});
    @Nullable OpenRouterRouteStrategy route =
        getObject(ctxt, node, "route", new TypeReference<>() {});
    @Nullable String user = getString(node, "user");
    @Nullable String sessionId = getString(node, "session_id");
    @Nullable TraceMetadata trace =
        getObject(ctxt, node, "trace", new TypeReference<TraceMetadata>() {});

    OpenRouterCustomPayload openRouterCustomPayload = null;
    if (plugins != null
        || providerConfig != null
        || route != null
        || user != null
        || sessionId != null
        || trace != null) {
      openRouterCustomPayload =
          new OpenRouterCustomPayload(plugins, providerConfig, route, user, sessionId, trace);
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

  private @Nullable ToolChoice getToolChoice(DeserializationContext ctxt, JsonNode node)
      throws tools.jackson.core.JacksonException {
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
    return ctxt.readTreeAsValue(toolChoiceNode, AllowedTools.class);
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
      DeserializationContext ctxt, JsonNode node, String fieldName, TypeReference<List<T>> typeRef)
      throws tools.jackson.core.JacksonException {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    return ctxt.readTreeAsValue(
        node.get(fieldName), ctxt.getTypeFactory().constructType(typeRef.getType()));
  }

  private @Nullable <T> List<T> getListWithType(
      DeserializationContext ctxt, JsonNode node, String fieldName, Class<T> elementClass)
      throws tools.jackson.core.JacksonException {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    var listType = ctxt.getTypeFactory().constructCollectionType(List.class, elementClass);
    return ctxt.readTreeAsValue(node.get(fieldName), listType);
  }

  private @Nullable <T> T getObject(
      DeserializationContext ctxt, JsonNode node, String fieldName, TypeReference<T> typeRef)
      throws tools.jackson.core.JacksonException {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    return ctxt.readTreeAsValue(
        node.get(fieldName), ctxt.getTypeFactory().constructType(typeRef.getType()));
  }
}
