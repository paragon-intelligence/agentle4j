package com.paragon.responses.json;

import com.fasterxml.jackson.annotation.JsonFormat;
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
import com.paragon.responses.spec.MessageContent;
import com.paragon.responses.spec.MoveAction;
import com.paragon.responses.spec.OutputMessage;
import com.paragon.responses.spec.ScrollAction;
import com.paragon.responses.spec.UserMessage;
import com.paragon.responses.spec.WebSearchToolCall;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.Serializers;

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
    addDeserializer(FunctionToolCallOutput.class, new FunctionToolCallOutputDeserializer());

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
    addSerializer(OutputMessage.class, messageSerializer);

    // Register MessageDeserializer for Message and all its subclasses
    MessageDeserializer messageDeserializer = new MessageDeserializer();
    addDeserializer(Message.class, messageDeserializer);
    addDeserializer(
        DeveloperMessage.class, new ConcreteMessageDeserializer<>(DeveloperMessage.class));
    addDeserializer(UserMessage.class, new ConcreteMessageDeserializer<>(UserMessage.class));
    addDeserializer(
        AssistantMessage.class, new ConcreteMessageDeserializer<>(AssistantMessage.class));
    addDeserializer(OutputMessage.class, new ConcreteMessageDeserializer<>(OutputMessage.class));

    // Register tolerant deserializer for MessageContent (accepts both string and object forms)
    addDeserializer(MessageContent.class, new MessageContentDeserializer());
  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    // Register custom serializers for enums
    context.addSerializers(
        new Serializers.Base() {
          @Override
          public ValueSerializer<?> findEnumSerializer(
              SerializationConfig config,
              JavaType type,
              BeanDescription.Supplier beanDesc,
              JsonFormat.Value formatOverrides) {
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
          public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueClass) {
            return valueClass.isEnum();
          }

          @Override
          public ValueDeserializer<?> findEnumDeserializer(
              JavaType type, DeserializationConfig config, BeanDescription.Supplier beanDesc) {
            return type.getRawClass().isEnum() ? new LowercaseEnumDeserializer() : null;
          }
        });

    // Note: Coordinate serialization is handled by @JsonUnwrapped annotation on fields
    // No custom serializer needed - Jackson handles unwrapping natively
  }
}
