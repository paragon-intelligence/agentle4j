package com.paragon.responses.json;

import com.paragon.responses.spec.*;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

/**
 * Centralized test data generators (jqwik Arbitraries) for property-based testing of the Responses
 * API Jackson serialization.
 *
 * <p>This class provides reusable generators for all major types in the Responses API, including
 * CreateResponse, Response, discriminated union subtypes, and special types like Coordinate.
 *
 * <p>Requirements: All testing requirements
 */
public class TestDataGenerators {

  // ===== CreateResponse Generators =====

  /**
   * Generates CreateResponse objects with random valid values. Includes minimal, typical, and
   * complex configurations.
   */
  public static Arbitrary<CreateResponsePayload> createResponses() {
    return Arbitraries.oneOf(
        minimalCreateResponse(), typicalCreateResponse(), complexCreateResponse());
  }

  /** Generates minimal CreateResponse with only required fields. */
  public static Arbitrary<CreateResponsePayload> minimalCreateResponse() {
    return models()
        .map(
            model ->
                new CreateResponsePayload(
                    null, null, null, null, null, null, null, null, model, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null));
  }

  /** Generates typical CreateResponse with commonly used fields populated. */
  public static Arbitrary<CreateResponsePayload> typicalCreateResponse() {
    return Combinators.combine(
            models(),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100).injectNull(0.2),
            Arbitraries.integers().between(100, 4000).injectNull(0.2),
            Arbitraries.doubles().between(0.0, 2.0).injectNull(0.2),
            Arbitraries.of(ServiceTierType.values()).injectNull(0.3),
            Arbitraries.of(Truncation.values()).injectNull(0.3))
        .as(
            (model, instructions, maxTokens, temp, serviceTier, truncation) ->
                new CreateResponsePayload(
                    null,
                    null,
                    null,
                    null,
                    instructions,
                    maxTokens,
                    null,
                    null,
                    model,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    serviceTier,
                    null,
                    null,
                    null,
                    temp,
                    null,
                    null,
                    null,
                    null,
                    null,
                    truncation,
                    null));
  }

  /** Generates complex CreateResponse with nested structures and discriminated unions. */
  public static Arbitrary<CreateResponsePayload> complexCreateResponse() {
    return Combinators.combine(
            models(),
            inputItems().list().ofMaxSize(3).injectNull(0.2),
            Arbitraries.integers().between(100, 5000).injectNull(0.2),
            Arbitraries.doubles().between(0.0, 2.0).injectNull(0.2),
            metadata().injectNull(0.3))
        .as(
            (model, input, maxTokens, temp, meta) ->
                new CreateResponsePayload(
                    null,
                    null,
                    null,
                    input,
                    "Complex test",
                    maxTokens,
                    null,
                    meta,
                    model,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    temp,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
  }

  /** Generates CreateResponse with many null fields for testing null exclusion. */
  public static Arbitrary<CreateResponsePayload> createResponsesWithNulls() {
    return Arbitraries.oneOf(
        // All nulls except model
        models()
            .map(
                model ->
                    new CreateResponsePayload(
                        null, null, null, null, null, null, null, null, model, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null)),
        // Some nulls
        Combinators.combine(
                models(),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50).injectNull(0.5),
                Arbitraries.integers().between(100, 2000).injectNull(0.5))
            .as(
                (model, instructions, maxTokens) ->
                    new CreateResponsePayload(
                        null,
                        null,
                        null,
                        null,
                        instructions,
                        maxTokens,
                        null,
                        null,
                        model,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));
  }

  // ===== Response Generators =====

  /** Generates Response objects with random valid values. */
  public static Arbitrary<Response> responses() {
    return Arbitraries.oneOf(minimalResponse(), typicalResponse(), complexResponse());
  }

  /** Generates minimal Response with only required fields. */
  public static Arbitrary<Response> minimalResponse() {
    return Combinators.combine(responseIds(), models())
        .as(
            (id, model) ->
                new Response(
                    null, null, null, null, id, null, null, null, null, null, model, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null));
  }

  /** Generates typical Response with commonly used fields. */
  public static Arbitrary<Response> typicalResponse() {
    return Combinators.combine(
            responseIds(),
            models(),
            Arbitraries.longs().between(1600000000L, 1700000000L),
            Arbitraries.of(ResponseGenerationStatus.values()),
            metadata().injectNull(0.3))
        .as(
            (id, model, createdAt, status, meta) ->
                new Response(
                    null,
                    null,
                    createdAt,
                    null,
                    id,
                    null,
                    null,
                    null,
                    null,
                    meta,
                    model,
                    ResponseObject.RESPONSE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    status,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
  }

  /** Generates complex Response with output items. */
  public static Arbitrary<Response> complexResponse() {
    return Combinators.combine(
            responseIds(),
            models(),
            Arbitraries.of(ResponseGenerationStatus.values()),
            outputItems().list().ofMinSize(1).ofMaxSize(3))
        .as(
            (id, model, status, output) ->
                new Response(
                    null,
                    null,
                    System.currentTimeMillis() / 1000,
                    null,
                    id,
                    null,
                    null,
                    null,
                    null,
                    null,
                    model,
                    ResponseObject.RESPONSE,
                    output,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    status,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
  }

  /** Generates Response with many null fields for testing null exclusion. */
  public static Arbitrary<Response> responsesWithNulls() {
    return Combinators.combine(
            responseIds().injectNull(0.3),
            models().injectNull(0.3),
            Arbitraries.of(ResponseGenerationStatus.values()).injectNull(0.5))
        .as(
            (id, model, status) ->
                new Response(
                    null, null, null, null, id, null, null, null, null, null, model, null, null,
                    null, null, null, null, null, null, null, status, null, null, null, null, null,
                    null, null));
  }

  // ===== Discriminated Union: ResponseInputItem =====

  /** Generates ResponseInputItem (discriminated union). */
  public static Arbitrary<ResponseInputItem> inputItems() {
    return Arbitraries.oneOf(userMessages(), developerMessages(), assistantMessages());
  }

  public static Arbitrary<UserMessage> userMessages() {
    return messageContents().list().ofMinSize(1).ofMaxSize(2).map(UserMessage::new);
  }

  public static Arbitrary<DeveloperMessage> developerMessages() {
    return messageContents()
        .list()
        .ofMinSize(1)
        .ofMaxSize(2)
        .map(content -> new DeveloperMessage(content, null));
  }

  public static Arbitrary<AssistantMessage> assistantMessages() {
    return Combinators.combine(
            messageContents().list().ofMinSize(1).ofMaxSize(2),
            Arbitraries.of(InputMessageStatus.values()).injectNull(0.3))
        .as(AssistantMessage::new);
  }

  // ===== Discriminated Union: ResponseOutput =====

  /** Generates ResponseOutput (discriminated union). */
  public static Arbitrary<ResponseOutput> outputItems() {
    return Arbitraries.oneOf(
        outputMessages().map(m -> (ResponseOutput) m), reasonings().map(r -> (ResponseOutput) r));
  }

  public static Arbitrary<OutputMessage<Void>> outputMessages() {
    return Combinators.combine(
            messageContents().list().ofMinSize(1).ofMaxSize(2),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.of(InputMessageStatus.values()))
        .as((content, id, status) -> new OutputMessage<>(content, id, status, null));
  }

  public static Arbitrary<Reasoning> reasonings() {
    return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100),
            Arbitraries.of(ReasoningStatus.values()))
        .as(
            (id, text, status) ->
                new Reasoning(
                    id,
                    List.of(new ReasoningSummaryText(text)),
                    List.of(new ReasoningTextContent(text)),
                    null,
                    status));
  }

  // ===== Discriminated Union: MessageContent =====

  /** Generates MessageContent (discriminated union). */
  public static Arbitrary<MessageContent> messageContents() {
    return Arbitraries.oneOf(texts(), images(), files());
  }

  public static Arbitrary<Text> texts() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100).map(Text::new);
  }

  public static Arbitrary<Image> images() {
    return Combinators.combine(
            Arbitraries.of(ImageDetail.values()),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50).injectNull(0.3))
        .as(Image::new);
  }

  public static Arbitrary<File> files() {
    return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20).injectNull(0.3))
        .as(File::new);
  }

  // ===== Discriminated Union: Tool =====

  /**
   * Generates Tool (discriminated union). Note: FunctionTool is abstract and requires concrete
   * implementation, so we skip it. CodeInterpreterTool and FileSearchTool require non-null
   * parameters.
   */
  public static Arbitrary<Tool> tools() {
    return Arbitraries.oneOf(webSearchTools(), fileSearchTools(), codeInterpreterTools());
  }

  public static Arbitrary<WebSearchTool> webSearchTools() {
    return Arbitraries.just(new WebSearchTool(null, null, null));
  }

  public static Arbitrary<FileSearchTool> fileSearchTools() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(10)
        .ofMaxLength(30)
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(ids -> new FileSearchTool(ids, null, null, null));
  }

  public static Arbitrary<CodeInterpreterTool> codeInterpreterTools() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(10)
        .ofMaxLength(30)
        .map(CodeInterpreterTool::new);
  }

  // ===== Discriminated Union: ComputerUseAction =====

  /** Generates ComputerUseAction (discriminated union). */
  public static Arbitrary<ComputerUseAction> computerUseActions() {
    return Arbitraries.oneOf(
        clickActions(),
        doubleClickActions(),
        dragActions(),
        keyPressActions(),
        moveActions(),
        screenshotActions(),
        scrollActions(),
        typeActions(),
        waitActions());
  }

  public static Arbitrary<ClickAction> clickActions() {
    return Combinators.combine(Arbitraries.of(ClickButton.values()), coordinates())
        .as(ClickAction::new);
  }

  public static Arbitrary<DoubleClickAction> doubleClickActions() {
    return coordinates().map(DoubleClickAction::new);
  }

  public static Arbitrary<DragAction> dragActions() {
    return coordinates().list().ofMinSize(2).ofMaxSize(5).map(DragAction::new);
  }

  public static Arbitrary<KeyPressAction> keyPressActions() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(10)
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(KeyPressAction::new);
  }

  public static Arbitrary<MoveAction> moveActions() {
    return coordinates().map(MoveAction::new);
  }

  public static Arbitrary<ScreenshotAction> screenshotActions() {
    return Arbitraries.just(new ScreenshotAction());
  }

  public static Arbitrary<ScrollAction> scrollActions() {
    return Combinators.combine(
            Arbitraries.integers().between(-1000, 1000),
            Arbitraries.integers().between(-1000, 1000),
            coordinates())
        .as(ScrollAction::new);
  }

  public static Arbitrary<TypeAction> typeActions() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(TypeAction::new);
  }

  public static Arbitrary<WaitAction> waitActions() {
    return Arbitraries.just(new WaitAction());
  }

  // ===== Special Types =====

  /** Generates Coordinate objects with random x and y values. */
  public static Arbitrary<Coordinate> coordinates() {
    return Combinators.combine(
            Arbitraries.integers().between(-10000, 10000),
            Arbitraries.integers().between(-10000, 10000))
        .as(Coordinate.Factory::getCoordinate);
  }

  /** Generates Coordinate objects with positive values (typical screen coordinates). */
  public static Arbitrary<Coordinate> positiveCoordinates() {
    return Combinators.combine(
            Arbitraries.integers().between(0, 1920), Arbitraries.integers().between(0, 1080))
        .as(Coordinate.Factory::getCoordinate);
  }

  // ===== Enum Generators =====

  public static Arbitrary<MessageRole> messageRoles() {
    return Arbitraries.of(MessageRole.values());
  }

  public static Arbitrary<AllowedToolsMode> allowedToolsModes() {
    return Arbitraries.of(AllowedToolsMode.values());
  }

  public static Arbitrary<ImageDetail> imageDetails() {
    return Arbitraries.of(ImageDetail.values());
  }

  public static Arbitrary<ServiceTierType> serviceTiers() {
    return Arbitraries.of(ServiceTierType.values());
  }

  public static Arbitrary<Truncation> truncations() {
    return Arbitraries.of(Truncation.values());
  }

  public static Arbitrary<ResponseGenerationStatus> responseStatuses() {
    return Arbitraries.of(ResponseGenerationStatus.values());
  }

  public static Arbitrary<InputMessageStatus> inputMessageStatuses() {
    return Arbitraries.of(InputMessageStatus.values());
  }

  public static Arbitrary<ReasoningStatus> reasoningStatuses() {
    return Arbitraries.of(ReasoningStatus.values());
  }

  public static Arbitrary<ClickButton> clickButtons() {
    return Arbitraries.of(ClickButton.values());
  }

  // ===== Primitive and Common Type Generators =====

  /** Generates valid model names. */
  public static Arbitrary<String> models() {
    return Arbitraries.of("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo");
  }

  /** Generates response IDs. */
  public static Arbitrary<String> responseIds() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .numeric()
        .withChars('-', '_')
        .ofMinLength(10)
        .ofMaxLength(30);
  }

  /** Generates metadata maps. */
  public static Arbitrary<Map<String, String>> metadata() {
    return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20))
        .as((key, value) -> Map.of(key, value));
  }

  // ===== JSON String Generators =====

  /** Generates valid JSON strings with various field combinations for Response objects. */
  public static Arbitrary<String> validResponseJsonStrings() {
    return responses()
        .map(
            response -> {
              try {
                return com.paragon.responses.ResponsesApiObjectMapper.create()
                    .writeValueAsString(response);
              } catch (Exception e) {
                throw new RuntimeException("Failed to generate JSON", e);
              }
            });
  }

  /** Generates valid JSON strings with unknown fields added. */
  public static Arbitrary<String> jsonStringsWithUnknownFields() {
    return Combinators.combine(
            validResponseJsonStrings(),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20))
        .as(
            (json, unknownKey, unknownValue) -> {
              // Insert unknown field into JSON
              int insertPos = json.lastIndexOf('}');
              String unknownField = ",\"" + unknownKey + "\":\"" + unknownValue + "\"";
              return json.substring(0, insertPos) + unknownField + json.substring(insertPos);
            });
  }

  /** Generates valid JSON strings with optional fields omitted. */
  public static Arbitrary<String> jsonStringsWithMissingOptionalFields() {
    return Arbitraries.oneOf(
        // Minimal JSON with only required fields
        Arbitraries.just("{\"id\":\"resp-123\",\"model\":\"gpt-4o\"}"),
        // JSON with some optional fields
        Arbitraries.just("{\"id\":\"resp-456\",\"model\":\"gpt-4o\",\"status\":\"completed\"}"),
        // JSON with different optional fields
        Arbitraries.just(
            "{\"id\":\"resp-789\",\"model\":\"gpt-4o-mini\",\"created_at\":1638360000}"));
  }
}
