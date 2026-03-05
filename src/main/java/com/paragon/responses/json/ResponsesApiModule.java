package com.paragon.responses.json;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.paragon.responses.spec.ApplyPatchToolCall;
import com.paragon.responses.spec.AssistantMessage;
import com.paragon.responses.spec.ClickAction;
import com.paragon.responses.spec.CodeInterpreterToolCall;
import com.paragon.responses.spec.ComputerToolCall;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.CustomToolCall;
import com.paragon.responses.spec.DeveloperMessage;
import com.paragon.responses.spec.DoubleClickAction;
import com.paragon.responses.spec.FileSearchToolCall;
import com.paragon.responses.spec.FunctionShellToolCall;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.ImageGenerationCall;
import com.paragon.responses.spec.LocalShellCall;
import com.paragon.responses.spec.McpToolCall;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.MoveAction;
import com.paragon.responses.spec.ScrollAction;
import com.paragon.responses.spec.UserMessage;
import com.paragon.responses.spec.WebSearchToolCall;

/**
 * Jackson module that registers all custom serializers and deserializers for the Responses API
 * specification classes.
 *
 * <p>This module includes:
 *
 * <ul>
 *   <li>LowercaseEnumSerializer/Deserializer for enum handling
 *   <li>Custom deserializers for action classes with @JsonUnwrapped Coordinate fields
 *   <li>CreateResponsePayloadDeserializer for @JsonUnwrapped OpenRouterCustomPayload
 *   <li>MessageDeserializer for role-based Message subclass selection
 *   <li>MessageSerializer for writing content as a plain string (required by OpenRouter)
 * </ul>
 */
public class ResponsesApiModule extends SimpleModule {

  public ResponsesApiModule() {
    super("ResponsesApiModule");

    // Register custom deserializers for classes with @JsonUnwrapped fields
    // Jackson doesn't support @JsonUnwrapped with @JsonCreator (record constructors),
    // so we need custom deserializers
    addDeserializer(MoveAction.class, new MoveActionDeserializer());
    addDeserializer(ClickAction.class, new ClickActionDeserializer());
    addDeserializer(DoubleClickAction.class, new DoubleClickActionDeserializer());
    addDeserializer(ScrollAction.class, new ScrollActionDeserializer());
    addDeserializer(CreateResponsePayload.class, new CreateResponsePayloadDeserializer());

    // Register custom serializer for FunctionToolCallOutput (output must be a plain string)
    addSerializer(FunctionToolCallOutput.class, new FunctionToolCallOutputSerializer());

    // Register custom serializers for tool calls so that the "type" discriminator and
    // wire format always match the Responses API spec, even when serialized as
    // ResponseInputItem (which uses EXISTING_PROPERTY).
    addSerializer(FunctionToolCall.class, new FunctionToolCallSerializer());
    addSerializer(ApplyPatchToolCall.class, new ApplyPatchToolCallSerializer());
    addSerializer(FileSearchToolCall.class, new FileSearchToolCallSerializer());
    addSerializer(ComputerToolCall.class, new ComputerToolCallSerializer());
    addSerializer(WebSearchToolCall.class, new WebSearchToolCallSerializer());
    addSerializer(LocalShellCall.class, new LocalShellCallSerializer());
    addSerializer(FunctionShellToolCall.class, new FunctionShellToolCallSerializer());
    addSerializer(CustomToolCall.class, new CustomToolCallSerializer());
    addSerializer(McpToolCall.class, new McpToolCallSerializer());
    addSerializer(ImageGenerationCall.class, new ImageGenerationCallSerializer());
    addSerializer(CodeInterpreterToolCall.class, new CodeInterpreterToolCallSerializer());

    // Register custom serializer for Message (content must be a plain string, not an array)
    MessageSerializer messageSerializer = new MessageSerializer();
    addSerializer(Message.class, messageSerializer);
    addSerializer(UserMessage.class, messageSerializer);
    addSerializer(DeveloperMessage.class, messageSerializer);
    addSerializer(AssistantMessage.class, messageSerializer);

    // Register MessageDeserializer for Message and all its subclasses
    MessageDeserializer messageDeserializer = new MessageDeserializer();
    addDeserializer(Message.class, messageDeserializer);
    addDeserializer(
        DeveloperMessage.class, new ConcreteMessageDeserializer<>(DeveloperMessage.class));
    addDeserializer(UserMessage.class, new ConcreteMessageDeserializer<>(UserMessage.class));
    addDeserializer(
        AssistantMessage.class, new ConcreteMessageDeserializer<>(AssistantMessage.class));
  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    // Register custom serializers for enums
    context.addSerializers(
        new Serializers.Base() {
          @Override
          public JsonSerializer<?> findSerializer(
              SerializationConfig config,
              com.fasterxml.jackson.databind.JavaType type,
              BeanDescription beanDesc) {
            Class<?> rawClass = type.getRawClass();
            if (rawClass.isEnum()) {
              return new LowercaseEnumSerializer();
            }
            return null;
          }
        });

    // Register custom deserializers for enums
    context.addDeserializers(
        new Deserializers.Base() {
          @Override
          public JsonDeserializer<?> findEnumDeserializer(
              Class<?> type, DeserializationConfig config, BeanDescription beanDesc) {
            return new LowercaseEnumDeserializer();
          }
        });

    // Note: Coordinate serialization is handled by @JsonUnwrapped annotation on fields
    // No custom serializer needed - Jackson handles unwrapping natively
  }
}
