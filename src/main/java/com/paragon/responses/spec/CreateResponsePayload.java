package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.Messages;
import com.paragon.responses.OpenRouterCustomPayload;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import java.util.*;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Creates a model response. Provide text or image inputs to generate text or JSON outputs. Have the
 * model call your own custom code or use built-in tools like web search or file search to use your
 * own data as input for the model's response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CreateResponsePayload {
  private final @Nullable Boolean background;
  private final @Nullable String conversation;
  private final @Nullable List<OutputDataInclude> include;
  private final @Nullable List<ResponseInputItem> input;
  private final @Nullable String instructions;
  private final @Nullable Integer maxOutputTokens;
  private final @Nullable Integer maxToolCalls;
  private final @Nullable Map<String, String> metadata;
  private final @Nullable String model;
  private final @Nullable Boolean parallelToolCalls;
  private final @Nullable PromptTemplate prompt;
  private final @Nullable String promptCacheKey;
  private final @Nullable String promptCacheRetention;
  private final @Nullable ReasoningConfig reasoning;
  private final @Nullable String safetyIdentifier;
  private final @Nullable ServiceTierType serviceTier;
  private final @Nullable Boolean store;
  private final @Nullable Boolean stream;
  private final @Nullable StreamOptions streamOptions;
  private final @Nullable Double temperature;
  private final @Nullable TextConfigurationOptions text;
  private final @Nullable ToolChoice toolChoice;
  private final @Nullable List<Tool> tools;
  private final @Nullable Integer topLogprobs;
  private final @Nullable Number topP;
  private final @Nullable Truncation truncation;
  @JsonUnwrapped private final @Nullable OpenRouterCustomPayload openRouterCustomPayload;

  /**
   * Creates a model response. Provide text or image inputs to generate text or JSON outputs. Have
   * the model call your own custom code or use built-in tools like web search or file search to use
   * your own data as input for the model's response.
   *
   * @param background Whether to run the model response in the
   *     href="https://platform.openai.com/docs/guides/background">background</a>.
   * @param conversation The conversation that this response belongs to. Items from this
   *     conversation are prepended to input_items for this response request. Input items and output
   *     items from this response are automatically added to this conversation after this response
   *     completes.
   * @param include Specify additional output data to include in the model response. Currently
   *     supported values are: web_search_call.action.sources: Include the sources of the web search
   *     tool call. code_interpreter_call.outputs: Includes the outputs of python code execution in
   *     code interpreter tool call items. computer_call_output.output.image_url: Include image urls
   *     from the computer call output. file_search_call.results: Include the search results of the
   *     file search tool call. message.input_image.image_url: Include image urls from the input
   *     message. message.output_text.logprobs: Include logprobs with assistant messages.
   *     reasoning.encrypted_content: Includes an encrypted version of reasoning tokens in reasoning
   *     item outputs. This enables reasoning items to be used in multi-turn conversations when
   *     using the Responses API statelessly (like when the store parameter is set to false, or when
   *     an organization is enrolled in the zero data retention program).
   * @param input Text, image, or file inputs to the model, used to generate a response.
   *     <p>Learn more:
   *     <p>
   *     <ul>
   *       <li><a href="https://platform.openai.com/docs/guides/text">Text inputs and outputs</a>
   *       <li>href="https://platform.openai
   *           .com/docs/guides/images-vision?api-mode=responses">Image inputs</a>
   *       <li><a href="https://platform.openai.com/docs/guides/pdf-files?api-mode=responses">File
   *           inputs</a>
   *       <li><a href="https://platform.openai
   *           .com/docs/guides/conversation-state?api-mode=responses">Conversation state</a>
   *       <li><a href="https://platform.openai.com/docs/guides/function-calling">Function
   *           calling</a>
   *     </ul>
   *
   * @param instructions When using along with previous_response_id, the instructions from a
   *     previous response will not be carried over to the next response. This makes it simple to
   *     swap out system (or developer) messages in new responses.
   * @param maxOutputTokens An upper bound for the number of tokens that can be generated for a
   *     response, including visible output tokens and
   *     href="https://platform.openai.com/docs/guides/reasoning">reasoning tokens.</a>
   * @param maxToolCalls The maximum number of total calls to built-in tools that can be processed
   *     in a response. This maximum number applies across all built-in tool calls, not per
   *     individual tool. Any further attempts to call a tool by the model will be ignored.
   * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful
   *     for storing additional information about the object in a structured format, and querying
   *     for objects via API or the dashboard.
   *     <p>Keys are strings with a maximum length of 64 characters. Values are strings with a
   *     maximum length of 512 characters.
   * @param model Model ID used to generate the response, like gpt-4o or o3. OpenAI offers a wide
   *     range of models with different capabilities, performance characteristics, and price points.
   *     Refer to the <a href="https://openrouter.ai/models">model guide</a> to browse and compare
   *     available models.
   * @param parallelToolCalls Whether to allow the model to run tool calls in parallel.
   * @param prompt Reference to a prompt template and its variables. <a
   *     href="https://platform.openai
   *     .com/docs/guides/text?api-mode=responses#reusable-prompts">Learn more</a>.
   * @param promptCacheKey Used by OpenAI to cache responses for similar requests to optimize your
   *     cache hit rates. Replaces the user field.
   *     href="https://platform.openai.com/docs/guides/prompt-caching">Learn more</a>.
   * @param promptCacheRetention The retention policy for the prompt cache. Set to 24h to enable
   *     extended prompt caching, which keeps cached prefixes active for longer, up to a maximum of
   *     24 hours.
   * @param reasoning Configuration options for reasoning models. <a href="https://platform.openai
   *     .com/docs/guides/prompt-caching#prompt-cache-retention">Learn more</a>.
   * @param safetyIdentifier A stable identifier used to help detect users of your application that
   *     may be violating OpenAI's usage policies. The IDs should be a string that uniquely
   *     identifies each user. We recommend hashing their username or email address, in order to
   *     avoid sending us any identifying information. <a href="https://platform.openai
   *     .com/docs/guides/safety-best-practices#safety-identifiers">Learn more</a>
   * @param serviceTier Specifies the processing type used for serving the request.
   *     <p>If set to 'auto', then the request will be processed with the service tier configured in
   *     the Project settings. Unless otherwise configured, the Project will use 'default'. If set
   *     to 'default', then the request will be processed with the standard pricing and performance
   *     for the selected model. If set to 'flex' or 'priority', then the request will be processed
   *     with the corresponding service tier. When not set, the default behavior is 'auto'. When the
   *     service_tier parameter is set, the response body will include the service_tier value based
   *     on the processing mode actually used to serve the request. This response value may be
   *     different from the value set in the parameter.
   * @param store Whether to store the generated model response for later retrieval via API.
   * @param stream If set to true, the model response data will be streamed to the client as it is
   *     generated using <a href="https://developer.mozilla
   *     .org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#Event_stream_format
   *     ">server-sent events</a>. See the
   *     href="https://platform.openai.com/docs/api-reference/responses-streaming">Streaming section
   *     </a> for more information.
   * @param streamOptions Options for streaming responses. Only set this when you set stream: true.
   * @param temperature What sampling temperature to use, between 0 and 2. Higher values like 0.8
   *     will make the output more random, while lower values like 0.2 will make it more focused and
   *     deterministic. We generally recommend altering this or top_p but not both.
   * @param text Configuration options for a text response from the model. Can be plain text or
   *     structured JSON data. Learn more:
   *     <ul>
   *       <li><a href="https://platform.openai.com/docs/guides/text">Text inputs and outputs</a>
   *       <li><a href="https://platform.openai.com/docs/guides/structured-outputs">Structured
   *           Outputs</a>
   *     </ul>
   *
   * @param toolChoice How the model should select which tool (or tools) to use when generating a
   *     response. See the tools parameter to see how to specify which tools the model can call.
   * @param tools An array of tools the model may call while generating a response. You can specify
   *     which tool to use by setting the tool_choice parameter.
   *     <p>We support the following categories of tools:
   *     <p>Built-in tools: Tools that are provided by OpenAI that extend the model's capabilities,
   *     like href="https://platform.openai.com/docs/guides/tools-web-search?api-mode=responses">web
   *     search</a> or <a href="https://platform.openai.com/docs/guides/tools-file-search">file
   *     search</a>. Learn more about href="https://platform.openai.com/docs/guides/tools">built-in
   *     tools</a>. MCP Tools: Integrations with third-party systems via custom MCP servers or
   *     predefined connectors such as Google Drive and SharePoint. Learn more about
   *     href="https://platform.openai.com/docs/guides/tools-connectors-mcp">MCP Tools</a>. Function
   *     calls (custom tools): Functions that are defined by you, enabling the model to call your
   *     own code with strongly typed arguments and outputs. Learn more about
   *     href="https://platform.openai.com/docs/guides/function-calling">function calling</a>. You
   *     can also use custom tools to call your own code.
   * @param topLogprobs An integer between 0 and 20 specifying the number of most likely tokens to
   *     return at each token position, each with an associated log probability.
   * @param topP An alternative to sampling with temperature, called nucleus sampling, where the
   *     model considers the results of the tokens with top_p probability mass. So 0.1 means only
   *     the tokens comprising the top 10% probability mass are considered.
   *     <p>We generally recommend altering this or temperature but not both.
   * @param truncation The truncation strategy to use for the model response.
   *     <p>auto: If the input to this Response exceeds the model's context window size, the model
   *     will truncate the response to fit the context window by dropping items from the beginning
   *     of the conversation. disabled (default): If the input size will exceed the context window
   *     size for a model, the request will fail with a 400 error.
   * @param openRouterCustomPayload custom payload designed for OpenRouter Responses.
   */
  public CreateResponsePayload(
      @Nullable Boolean background,
      @Nullable String conversation,
      @Nullable List<OutputDataInclude> include,
      @Nullable List<ResponseInputItem> input,
      @Nullable String instructions,
      @Nullable Integer maxOutputTokens,
      @Nullable Integer maxToolCalls,
      @Nullable Map<String, String> metadata,
      @Nullable String model,
      @Nullable Boolean parallelToolCalls,
      @Nullable PromptTemplate prompt,
      @Nullable String promptCacheKey,
      @Nullable String promptCacheRetention,
      @Nullable ReasoningConfig reasoning,
      @Nullable String safetyIdentifier,
      @Nullable ServiceTierType serviceTier,
      @Nullable Boolean store,
      @Nullable Boolean stream,
      @Nullable StreamOptions streamOptions,
      @Nullable Double temperature,
      @Nullable TextConfigurationOptions text,
      @Nullable ToolChoice toolChoice,
      @Nullable List<Tool> tools,
      @Nullable Integer topLogprobs,
      @Nullable Number topP,
      @Nullable Truncation truncation,
      @Nullable @JsonUnwrapped OpenRouterCustomPayload openRouterCustomPayload) {
    // Normalize empty OpenRouterCustomPayload to null for consistent equality comparison
    if (openRouterCustomPayload != null && openRouterCustomPayload.isEmpty()) {
      openRouterCustomPayload = null;
    }
    this.background = background;
    this.conversation = conversation;
    this.include = include;
    this.input = input;
    this.instructions = instructions;
    this.maxOutputTokens = maxOutputTokens;
    this.maxToolCalls = maxToolCalls;
    this.metadata = metadata;
    this.model = model;
    this.parallelToolCalls = parallelToolCalls;
    this.prompt = prompt;
    this.promptCacheKey = promptCacheKey;
    this.promptCacheRetention = promptCacheRetention;
    this.reasoning = reasoning;
    this.safetyIdentifier = safetyIdentifier;
    this.serviceTier = serviceTier;
    this.store = store;
    this.stream = stream;
    this.streamOptions = streamOptions;
    this.temperature = temperature;
    this.text = text;
    this.toolChoice = toolChoice;
    this.tools = tools;
    this.topLogprobs = topLogprobs;
    this.topP = topP;
    this.truncation = truncation;
    this.openRouterCustomPayload = openRouterCustomPayload;
  }

  public static Builder builder() {
    return builder(new JacksonJsonSchemaProducer(new ObjectMapper()));
  }

  public static Builder builder(JsonSchemaProducer jsonSchemaProducer) {
    return new Builder(jsonSchemaProducer);
  }

  public Request toRequest(
      ObjectMapper objectMapper, MediaType mediaType, HttpUrl baseUrl, Headers headers) {
    String jsonPayload = null;
    try {
      jsonPayload = objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    RequestBody body = RequestBody.create(jsonPayload, mediaType);
    return new Request.Builder().url(baseUrl).headers(headers).post(body).build();
  }

  // ===== Getters for DTO/Value Object fields =====
  // These are appropriate because CreateResponsePayload is an immutable data carrier

  public @Nullable Boolean background() {
    return background;
  }

  public @Nullable String conversation() {
    return conversation;
  }

  public @Nullable List<OutputDataInclude> include() {
    return include;
  }

  public @Nullable List<ResponseInputItem> input() {
    return input;
  }

  public @Nullable String instructions() {
    return instructions;
  }

  public @Nullable Integer maxOutputTokens() {
    return maxOutputTokens;
  }

  public @Nullable Integer maxToolCalls() {
    return maxToolCalls;
  }

  public @Nullable Map<String, String> metadata() {
    return metadata;
  }

  public @Nullable String model() {
    return model;
  }

  public @Nullable Boolean parallelToolCalls() {
    return parallelToolCalls;
  }

  public @Nullable PromptTemplate prompt() {
    return prompt;
  }

  public @Nullable String promptCacheKey() {
    return promptCacheKey;
  }

  public @Nullable String promptCacheRetention() {
    return promptCacheRetention;
  }

  public @Nullable ReasoningConfig reasoning() {
    return reasoning;
  }

  public @Nullable String safetyIdentifier() {
    return safetyIdentifier;
  }

  public @Nullable ServiceTierType serviceTier() {
    return serviceTier;
  }

  public @Nullable Boolean store() {
    return store;
  }

  public @Nullable Boolean stream() {
    return stream;
  }

  public @Nullable StreamOptions streamOptions() {
    return streamOptions;
  }

  public @Nullable Double temperature() {
    return temperature;
  }

  public @Nullable TextConfigurationOptions text() {
    return text;
  }

  public @Nullable ToolChoice toolChoice() {
    return toolChoice;
  }

  public @Nullable List<Tool> tools() {
    return tools;
  }

  public @Nullable Integer topLogprobs() {
    return topLogprobs;
  }

  public @Nullable Number topP() {
    return topP;
  }

  public @Nullable Truncation truncation() {
    return truncation;
  }

  public @Nullable OpenRouterCustomPayload openRouterCustomPayload() {
    return openRouterCustomPayload;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (CreateResponsePayload) obj;
    return Objects.equals(this.background, that.background)
        && Objects.equals(this.conversation, that.conversation)
        && Objects.equals(this.include, that.include)
        && Objects.equals(this.input, that.input)
        && Objects.equals(this.instructions, that.instructions)
        && Objects.equals(this.maxOutputTokens, that.maxOutputTokens)
        && Objects.equals(this.maxToolCalls, that.maxToolCalls)
        && Objects.equals(this.metadata, that.metadata)
        && Objects.equals(this.model, that.model)
        && Objects.equals(this.parallelToolCalls, that.parallelToolCalls)
        && Objects.equals(this.prompt, that.prompt)
        && Objects.equals(this.promptCacheKey, that.promptCacheKey)
        && Objects.equals(this.promptCacheRetention, that.promptCacheRetention)
        && Objects.equals(this.reasoning, that.reasoning)
        && Objects.equals(this.safetyIdentifier, that.safetyIdentifier)
        && Objects.equals(this.serviceTier, that.serviceTier)
        && Objects.equals(this.store, that.store)
        && Objects.equals(this.stream, that.stream)
        && Objects.equals(this.streamOptions, that.streamOptions)
        && Objects.equals(this.temperature, that.temperature)
        && Objects.equals(this.text, that.text)
        && Objects.equals(this.toolChoice, that.toolChoice)
        && Objects.equals(this.tools, that.tools)
        && Objects.equals(this.topLogprobs, that.topLogprobs)
        && Objects.equals(this.topP, that.topP)
        && Objects.equals(this.truncation, that.truncation)
        && Objects.equals(this.openRouterCustomPayload, that.openRouterCustomPayload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
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

  @Override
  public String toString() {
    return "CreateResponsePayload["
        + "background="
        + background
        + ", "
        + "conversation="
        + conversation
        + ", "
        + "include="
        + include
        + ", "
        + "input="
        + input
        + ", "
        + "instructions="
        + instructions
        + ", "
        + "maxOutputTokens="
        + maxOutputTokens
        + ", "
        + "maxToolCalls="
        + maxToolCalls
        + ", "
        + "metadata="
        + metadata
        + ", "
        + "model="
        + model
        + ", "
        + "parallelToolCalls="
        + parallelToolCalls
        + ", "
        + "prompt="
        + prompt
        + ", "
        + "promptCacheKey="
        + promptCacheKey
        + ", "
        + "promptCacheRetention="
        + promptCacheRetention
        + ", "
        + "reasoning="
        + reasoning
        + ", "
        + "safetyIdentifier="
        + safetyIdentifier
        + ", "
        + "serviceTier="
        + serviceTier
        + ", "
        + "store="
        + store
        + ", "
        + "stream="
        + stream
        + ", "
        + "streamOptions="
        + streamOptions
        + ", "
        + "temperature="
        + temperature
        + ", "
        + "text="
        + text
        + ", "
        + "toolChoice="
        + toolChoice
        + ", "
        + "tools="
        + tools
        + ", "
        + "topLogprobs="
        + topLogprobs
        + ", "
        + "topP="
        + topP
        + ", "
        + "truncation="
        + truncation
        + ", "
        + "openRouterCustomPayload="
        + openRouterCustomPayload
        + ']';
  }

  public Boolean streamEnabled() {
    return stream;
  }

  public boolean hasEmptyText() {
    return text == null;
  }

  public boolean hasEmptyTextFormat() {
    if (text == null) {
      return true;
    }

    return !text.hasFormat();
  }

  public boolean hasJsonSchemaTextFormat() {
    if (hasEmptyText()) {
      return false;
    }

    if (hasEmptyTextFormat()) {
      return false;
    }

    assert text != null;
    return text.hasJsonSchemaTextFormat();
  }

  public static class Builder {
    @Nullable Boolean background = false;
    @Nullable String conversation = null;
    @Nullable List<OutputDataInclude> include = null;
    @Nullable List<ResponseInputItem> input = null;
    @Nullable String instructions = null;
    @Nullable Integer maxOutputTokens = null;
    @Nullable Integer maxToolCalls = null;
    @Nullable Map<String, String> metadata = null;
    @Nullable String model = null;
    @Nullable Boolean parallelToolCalls = true;
    @Nullable PromptTemplate prompt = null;
    @Nullable String promptCacheKey = null;
    @Nullable ReasoningConfig reasoning = null;
    @Nullable String safetyIdentifier = null;
    @Nullable ServiceTierType serviceTier = ServiceTierType.AUTO;
    @Nullable Boolean store = false;
    @Nullable Boolean stream = false;
    @Nullable StreamOptions streamOptions = null;
    @Nullable Double temperature = 2.0;
    @Nullable TextConfigurationOptions text = null;
    @Nullable ToolChoice toolChoice = null;
    @Nullable List<Tool> tools = null;
    @Nullable Integer topLogprobs = null;
    @Nullable Number topP = 1;
    @Nullable Truncation truncation = Truncation.DISABLED;
    @NonNull JsonSchemaProducer jsonSchemaProducer;
    @Nullable OpenRouterCustomPayload openRouterCustomPayload = null;
    @Nullable String promptCacheRetention = null;

    public Builder(@NonNull JsonSchemaProducer jsonSchemaProducer) {
      this.jsonSchemaProducer = jsonSchemaProducer;
    }

    public Builder background(Boolean background) {
      this.background = background;
      return this;
    }

    public Builder conversation(String conversation) {
      this.conversation = conversation;
      return this;
    }

    public Builder include(List<OutputDataInclude> include) {
      this.include = include;
      return this;
    }

    public Builder input(List<ResponseInputItem> input) {
      this.input = input;
      return this;
    }

    public Builder instructions(String instructions) {
      this.instructions = instructions;
      return this;
    }

    public Builder maxOutputTokens(Integer maxOutputTokens) {
      this.maxOutputTokens = maxOutputTokens;
      return this;
    }

    public Builder maxToolCalls(Integer maxToolCalls) {
      this.maxToolCalls = maxToolCalls;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder parallelToolCalls(Boolean parallelToolCalls) {
      this.parallelToolCalls = parallelToolCalls;
      return this;
    }

    public Builder prompt(PromptTemplate prompt) {
      this.prompt = prompt;
      return this;
    }

    public Builder promptCacheKey(String promptCacheKey) {
      this.promptCacheKey = promptCacheKey;
      return this;
    }

    public Builder reasoning(ReasoningConfig reasoning) {
      this.reasoning = reasoning;
      return this;
    }

    public Builder safetyIdentifier(String safetyIdentifier) {
      this.safetyIdentifier = safetyIdentifier;
      return this;
    }

    public Builder serviceTier(ServiceTierType serviceTier) {
      this.serviceTier = serviceTier;
      return this;
    }

    public Builder store(Boolean store) {
      this.store = store;
      return this;
    }

    public Builder stream(@NonNull Boolean stream) {
      this.stream = Objects.requireNonNull(stream);
      return this;
    }

    public Builder streamOptions(StreamOptions streamOptions) {
      this.streamOptions = streamOptions;
      return this;
    }

    public Builder temperature(Double temperature) {
      this.temperature = temperature;
      return this;
    }

    public Builder text(TextConfigurationOptions text) {
      this.text = text;
      return this;
    }

    public Builder toolChoice(ToolChoice toolChoice) {
      this.toolChoice = toolChoice;
      return this;
    }

    public Builder tools(@NonNull List<Tool> tools) {
      this.tools = tools;
      return this;
    }

    public Builder addTool(@NonNull Tool tool) {
      if (this.tools == null) {
        this.tools = new ArrayList<>();
      }

      this.tools.add(tool);
      return this;
    }

    public Builder topLogprobs(Integer topLogprobs) {
      this.topLogprobs = topLogprobs;
      return this;
    }

    public Builder topP(Number topP) {
      this.topP = topP;
      return this;
    }

    public Builder truncation(Truncation truncation) {
      this.truncation = truncation;
      return this;
    }

    public <T> StructuredTextFormatBuilder<T> withStructuredOutput(
        @NonNull Class<T> structuredOutput) {
      this.text =
          new TextConfigurationOptions(
              new TextConfigurationOptionsJsonSchemaFormat(
                  structuredOutput.getName(), jsonSchemaProducer.produce(structuredOutput)),
              ModelVerbosityConfig.MEDIUM);

      return new StructuredTextFormatBuilder<>(this, structuredOutput);
    }

    public Builder withStructuredOutput(
        @NonNull Class<?> structuredOutput, @Nullable ModelVerbosityConfig verbosityConfig) {
      this.text =
          new TextConfigurationOptions(
              new TextConfigurationOptionsJsonSchemaFormat(
                  structuredOutput.getName(), jsonSchemaProducer.produce(structuredOutput)),
              verbosityConfig);

      return this;
    }

    public Builder addDeveloperMessage(
        String developerMessage, InputMessageStatus inputMessageStatus) {
      DeveloperMessage message = Message.developer(developerMessage, inputMessageStatus);
      return addDeveloperMessage(message);
    }

    public Builder addDeveloperMessage(String developerMessage) {
      return addDeveloperMessage(developerMessage, InputMessageStatus.COMPLETED);
    }

    public Builder addDeveloperMessage(DeveloperMessage developerMessage) {
      if (input == null) {
        return addMessage(developerMessage);
      }

      Optional<DeveloperMessage> existing =
          input.stream()
              .filter(DeveloperMessage.class::isInstance)
              .map(DeveloperMessage.class::cast)
              .findAny();

      if (existing.isPresent()) {
        throw new IllegalArgumentException(
            "DeveloperMessage already exists at position " + input.indexOf(existing.get()));
      }

      input.addFirst(developerMessage);
      return this;
    }

    public Builder addAssistantMessage(String assistantMessage, InputMessageStatus status) {
      AssistantMessage message = Message.assistant(assistantMessage, status);
      return addMessage(message);
    }

    public Builder addAssistantMessage(String assistantMessage) {
      return addAssistantMessage(assistantMessage, InputMessageStatus.COMPLETED);
    }

    public Builder addAssistantMessage(AssistantMessage assistantMessage) {
      return addMessage(assistantMessage);
    }

    public Builder addUserMessage(String userMessage, InputMessageStatus status) {
      UserMessage message = Message.user(userMessage, status);
      return addUserMessage(message);
    }

    public Builder addUserMessage(String userMessage) {
      UserMessage message = Message.user(userMessage, InputMessageStatus.COMPLETED);
      return addUserMessage(message);
    }

    public Builder addUserMessage(UserMessage userMessage) {
      return addMessage(userMessage);
    }

    public Builder addMessages(@NonNull Messages messages) {
      return addMessages(messages.messages());
    }

    public Builder addMessages(@NonNull List<Message> messages) {
      if (input == null) {
        input = new ArrayList<>();
        input.addAll(messages);
        return this;
      }

      boolean success = input.addAll(messages);

      if (!success) {
        throw new IllegalArgumentException("Could not add messages to messages list");
      }

      return this;
    }

    public Builder addMessage(Message message) {
      if (input == null) {
        input = new ArrayList<>();
        input.add(message);
        return this;
      }
      input.add(message);
      return this;
    }

    public Builder openRouterCustomPayload(OpenRouterCustomPayload openRouterCustomPayload) {
      this.openRouterCustomPayload = openRouterCustomPayload;
      return this;
    }

    public Builder promptCacheRetention(String promptCacheRetention) {
      this.promptCacheRetention = promptCacheRetention;
      return this;
    }

    public CreateResponsePayload build() {
      var basePayload =
          new CreateResponsePayload(
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

      // Runtime decision based on streaming
      if (Boolean.TRUE.equals(stream)) {
        return new Streaming(basePayload);
      }

      return basePayload;
    }

    /**
     * Returns a StreamingBuilder for building streaming payloads with compile-time type safety.
     *
     * <p>Example:
     *
     * <pre>{@code
     * var payload = CreateResponsePayload.builder()
     *     .model("gpt-4o")
     *     .addUserMessage("Hello")
     *     .streaming()  // Returns StreamingBuilder
     *     .build();     // Returns Streaming type
     *
     * responder.respond(payload)  // No cast needed
     *     .onTextDelta(System.out::print)
     *     .start();
     * }</pre>
     *
     * @return a StreamingBuilder that produces Streaming payloads
     */
    public StreamingBuilder streaming() {
      this.stream = true;
      return new StreamingBuilder(this);
    }
  }

  /**
   * A builder that produces Streaming payloads with compile-time type safety. Obtained via {@link
   * Builder#streaming()}.
   */
  public static class StreamingBuilder extends Builder {

    // Constructor that copies state from parent Builder
    public StreamingBuilder(Builder builder) {
      super(builder.jsonSchemaProducer);

      // Copy all fields from the parent builder
      this.background = builder.background;
      this.conversation = builder.conversation;
      this.include = builder.include;
      this.input = builder.input;
      this.instructions = builder.instructions;
      this.maxOutputTokens = builder.maxOutputTokens;
      this.maxToolCalls = builder.maxToolCalls;
      this.metadata = builder.metadata;
      this.model = builder.model;
      this.parallelToolCalls = builder.parallelToolCalls;
      this.prompt = builder.prompt;
      this.promptCacheKey = builder.promptCacheKey;
      this.reasoning = builder.reasoning;
      this.safetyIdentifier = builder.safetyIdentifier;
      this.serviceTier = builder.serviceTier;
      this.store = builder.store;
      this.stream = true; // Always true for streaming
      this.streamOptions = builder.streamOptions;
      this.temperature = builder.temperature;
      this.text = builder.text;
      this.toolChoice = builder.toolChoice;
      this.tools = builder.tools;
      this.topLogprobs = builder.topLogprobs;
      this.topP = builder.topP;
      this.truncation = builder.truncation;
      this.openRouterCustomPayload = builder.openRouterCustomPayload;
      this.promptCacheRetention = builder.promptCacheRetention;
    }

    @Override
    public Streaming build() {
      var basePayload =
          new CreateResponsePayload(
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
              true, // Always true for streaming
              streamOptions,
              temperature,
              text,
              toolChoice,
              tools,
              topLogprobs,
              topP,
              truncation,
              openRouterCustomPayload);
      return new Streaming(basePayload);
    }
  }

  public static class StructuredTextFormatBuilder<T> extends Builder {

    protected final Class<T> structuredOutput;

    // Constructor that copies state from parent Builder
    public StructuredTextFormatBuilder(Builder builder, Class<T> structuredOutput) {
      super(builder.jsonSchemaProducer);

      // Copy all fields from the parent builder
      this.background = builder.background;
      this.conversation = builder.conversation;
      this.include = builder.include;
      this.input = builder.input;
      this.instructions = builder.instructions;
      this.maxOutputTokens = builder.maxOutputTokens;
      this.maxToolCalls = builder.maxToolCalls;
      this.metadata = builder.metadata;
      this.model = builder.model;
      this.parallelToolCalls = builder.parallelToolCalls;
      this.prompt = builder.prompt;
      this.promptCacheKey = builder.promptCacheKey;
      this.reasoning = builder.reasoning;
      this.safetyIdentifier = builder.safetyIdentifier;
      this.serviceTier = builder.serviceTier;
      this.store = builder.store;
      this.stream = builder.stream;
      this.streamOptions = builder.streamOptions;
      this.temperature = builder.temperature;
      this.text = builder.text;
      this.toolChoice = builder.toolChoice;
      this.tools = builder.tools;
      this.topLogprobs = builder.topLogprobs;
      this.topP = builder.topP;
      this.truncation = builder.truncation;
      this.openRouterCustomPayload = builder.openRouterCustomPayload;
      this.promptCacheRetention = builder.promptCacheRetention;
      this.structuredOutput = structuredOutput;
    }

    @Override
    public CreateResponsePayload.Structured<T> build() {
      var basePayload = super.build();
      return new Structured<>(basePayload, this.structuredOutput);
    }

    /**
     * Returns a StructuredStreamingBuilder for building structured streaming payloads with
     * compile-time type safety.
     *
     * <p>Example:
     *
     * <pre>{@code
     * var payload = CreateResponsePayload.builder()
     *     .model("gpt-4o")
     *     .addUserMessage("Create a person")
     *     .withStructuredOutput(Person.class)
     *     .streaming()  // Returns StructuredStreamingBuilder<Person>
     *     .build();     // Returns StructuredStreaming<Person>
     *
     * responder.respond(payload)  // No cast needed!
     *     .onTextDelta(System.out::print)
     *     .start();
     * }</pre>
     *
     * @return a StructuredStreamingBuilder that produces StructuredStreaming payloads
     */
    @Override
    public StructuredStreamingBuilder<T> streaming() {
      this.stream = true;
      return new StructuredStreamingBuilder<>(this, structuredOutput);
    }
  }

  /**
   * A builder that produces StructuredStreaming payloads with compile-time type safety. Obtained
   * via {@link StructuredTextFormatBuilder#streaming()}.
   *
   * @param <T> the structured output type
   */
  public static class StructuredStreamingBuilder<T> extends StreamingBuilder {

    private final Class<T> structuredOutput;

    public StructuredStreamingBuilder(Builder builder, Class<T> structuredOutput) {
      super(builder);
      this.structuredOutput = structuredOutput;
    }

    @Override
    public StructuredStreaming<T> build() {
      var basePayload =
          new CreateResponsePayload(
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
              true, // Always true for streaming
              streamOptions,
              temperature,
              text,
              toolChoice,
              tools,
              topLogprobs,
              topP,
              truncation,
              openRouterCustomPayload);
      return new StructuredStreaming<>(basePayload, structuredOutput);
    }
  }

  /**
   * A variant for streaming responses (without structured output). This allows special handling for
   * responses that use streaming.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class Streaming extends CreateResponsePayload {

    /** Creates a Streaming response payload by copying from a base payload. */
    public Streaming(@NonNull CreateResponsePayload payload) {
      super(
          payload.background(),
          payload.conversation(),
          payload.include(),
          payload.input(),
          payload.instructions(),
          payload.maxOutputTokens(),
          payload.maxToolCalls(),
          payload.metadata(),
          payload.model(),
          payload.parallelToolCalls(),
          payload.prompt(),
          payload.promptCacheKey(),
          payload.promptCacheRetention(),
          payload.reasoning(),
          payload.safetyIdentifier(),
          payload.serviceTier(),
          payload.store(),
          payload.stream(),
          payload.streamOptions(),
          payload.temperature(),
          payload.text(),
          payload.toolChoice(),
          payload.tools(),
          payload.topLogprobs(),
          payload.topP(),
          payload.truncation(),
          payload.openRouterCustomPayload());
    }

    /** Creates a Streaming response payload with all individual parameters. */
    public Streaming(
        @Nullable Boolean background,
        @Nullable String conversation,
        @Nullable List<OutputDataInclude> include,
        @Nullable List<ResponseInputItem> input,
        @Nullable String instructions,
        @Nullable Integer maxOutputTokens,
        @Nullable Integer maxToolCalls,
        @Nullable Map<String, String> metadata,
        @Nullable String model,
        @Nullable Boolean parallelToolCalls,
        @Nullable PromptTemplate prompt,
        @Nullable String promptCacheKey,
        @Nullable String promptCacheRetention,
        @Nullable ReasoningConfig reasoning,
        @Nullable String safetyIdentifier,
        @Nullable ServiceTierType serviceTier,
        @Nullable Boolean store,
        @Nullable Boolean stream,
        @Nullable StreamOptions streamOptions,
        @Nullable Double temperature,
        @Nullable TextConfigurationOptions text,
        @Nullable ToolChoice toolChoice,
        @Nullable List<Tool> tools,
        @Nullable Integer topLogprobs,
        @Nullable Number topP,
        @Nullable Truncation truncation,
        @Nullable OpenRouterCustomPayload openRouterCustomPayload) {
      super(
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
  }

  /**
   * A variant of CreateResponsePayload that includes type information for structured JSON
   * responses. This allows for type-safe handling of structured outputs from the model.
   *
   * @param <T> The type of the structured response
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class Structured<T> extends CreateResponsePayload {
    private final Class<T> responseType;

    /** Creates a Structured response payload with all individual parameters. */
    public Structured(
        @Nullable Boolean background,
        @Nullable String conversation,
        @Nullable List<OutputDataInclude> include,
        @Nullable List<ResponseInputItem> input,
        @Nullable String instructions,
        @Nullable Integer maxOutputTokens,
        @Nullable Integer maxToolCalls,
        @Nullable Map<String, String> metadata,
        @Nullable String model,
        @Nullable Boolean parallelToolCalls,
        @Nullable PromptTemplate prompt,
        @Nullable String promptCacheKey,
        @Nullable String promptCacheRetention,
        @Nullable ReasoningConfig reasoning,
        @Nullable String safetyIdentifier,
        @Nullable ServiceTierType serviceTier,
        @Nullable Boolean store,
        @Nullable Boolean stream,
        @Nullable StreamOptions streamOptions,
        @Nullable Double temperature,
        @Nullable TextConfigurationOptions text,
        @Nullable ToolChoice toolChoice,
        @Nullable List<Tool> tools,
        @Nullable Integer topLogprobs,
        @Nullable Number topP,
        @Nullable Truncation truncation,
        @NonNull Class<T> responseType) {
      super(
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
          null); // openRouterCustomPayload not in this constructor
      this.responseType = responseType;
    }

    /**
     * Creates a Structured response payload by copying all fields from a CreateResponsePayload and
     * adding the responseType parameter.
     *
     * @param payload the CreateResponsePayload to copy from
     * @param responseType the Class representing the structured response type
     */
    public Structured(@NonNull CreateResponsePayload payload, @NonNull Class<T> responseType) {
      super(
          payload.background(),
          payload.conversation(),
          payload.include(),
          payload.input(),
          payload.instructions(),
          payload.maxOutputTokens(),
          payload.maxToolCalls(),
          payload.metadata(),
          payload.model(),
          payload.parallelToolCalls(),
          payload.prompt(),
          payload.promptCacheKey(),
          payload.promptCacheRetention(),
          payload.reasoning(),
          payload.safetyIdentifier(),
          payload.serviceTier(),
          payload.store(),
          payload.stream(),
          payload.streamOptions(),
          payload.temperature(),
          payload.text(),
          payload.toolChoice(),
          payload.tools(),
          payload.topLogprobs(),
          payload.topP(),
          payload.truncation(),
          payload.openRouterCustomPayload());
      this.responseType = responseType;
    }

    public Class<T> responseType() {
      return responseType;
    }
  }

  /**
   * A variant for structured streaming responses. This allows special handling for responses that
   * use both structured outputs and streaming.
   *
   * @param <T> The type of the structured response
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class StructuredStreaming<T> extends Streaming {

    private final @NonNull Class<T> responseType;

    /** Creates a StructuredStreaming response payload by copying from a base payload. */
    public StructuredStreaming(
        @NonNull CreateResponsePayload payload, @NonNull Class<T> responseType) {
      super(payload);
      this.responseType = responseType;
    }

    public Class<T> responseType() {
      return responseType;
    }
  }
}
