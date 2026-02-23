# :material-code-braces: CreateResponsePayload

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.CreateResponsePayload` &nbsp;·&nbsp; **Class**

---

Creates a model response. Provide text or image inputs to generate text or JSON outputs. Have the
model call your own custom code or use built-in tools like web search or file search to use your
own data as input for the model's response.

## Methods

### `CreateResponsePayload`

```java
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
      @Nullable ReasoningConfig reasonin
```

Creates a model response. Provide text or image inputs to generate text or JSON outputs. Have
the model call your own custom code or use built-in tools like web search or file search to use
your own data as input for the model's response.

**Parameters**

| Name | Description |
|------|-------------|
| `background` | Whether to run the model response in the href="https://platform.openai.com/docs/guides/background">background. |
| `conversation` | The conversation that this response belongs to. Items from this conversation are prepended to input_items for this response request. Input items and output     items from this response are automatically added to this conversation after this response     completes. |
| `include` | Specify additional output data to include in the model response. Currently supported values are: web_search_call.action.sources: Include the sources of the web search     tool call. code_interpreter_call.outputs: Includes the outputs of python code execution in     code interpreter tool call items. computer_call_output.output.image_url: Include image urls     from the computer call output. file_search_call.results: Include the search results of the     file search tool call. message.input_image.image_url: Include image urls from the input     message. message.output_text.logprobs: Include logprobs with assistant messages.     reasoning.encrypted_content: Includes an encrypted version of reasoning tokens in reasoning     item outputs. This enables reasoning items to be used in multi-turn conversations when     using the Responses API statelessly (like when the store parameter is set to false, or when     an organization is enrolled in the zero data retention program). |
| `input` | Text, image, or file inputs to the model, used to generate a response.   Learn more:                   - Text inputs and outputs - href="https://platform.openai           .com/docs/guides/images-vision?api-mode=responses">Image inputs - File           inputs - Conversation state - Function           calling |
| `instructions` | When using along with previous_response_id, the instructions from a previous response will not be carried over to the next response. This makes it simple to     swap out system (or developer) messages in new responses. |
| `maxOutputTokens` | An upper bound for the number of tokens that can be generated for a response, including visible output tokens and     href="https://platform.openai.com/docs/guides/reasoning">reasoning tokens. |
| `maxToolCalls` | The maximum number of total calls to built-in tools that can be processed in a response. This maximum number applies across all built-in tool calls, not per     individual tool. Any further attempts to call a tool by the model will be ignored. |
| `metadata` | Set of 16 key-value pairs that can be attached to an object. This can be useful for storing additional information about the object in a structured format, and querying     for objects via API or the dashboard.       Keys are strings with a maximum length of 64 characters. Values are strings with a     maximum length of 512 characters. |
| `model` | Model ID used to generate the response, like gpt-4o or o3. OpenAI offers a wide range of models with different capabilities, performance characteristics, and price points.     Refer to the model guide to browse and compare     available models. |
| `parallelToolCalls` | Whether to allow the model to run tool calls in parallel. |
| `prompt` | Reference to a prompt template and its variables. Learn more. |
| `promptCacheKey` | Used by OpenAI to cache responses for similar requests to optimize your cache hit rates. Replaces the user field.     href="https://platform.openai.com/docs/guides/prompt-caching">Learn more. |
| `promptCacheRetention` | The retention policy for the prompt cache. Set to 24h to enable extended prompt caching, which keeps cached prefixes active for longer, up to a maximum of     24 hours. |
| `reasoning` | Configuration options for reasoning models. Learn more. |
| `safetyIdentifier` | A stable identifier used to help detect users of your application that may be violating OpenAI's usage policies. The IDs should be a string that uniquely     identifies each user. We recommend hashing their username or email address, in order to     avoid sending us any identifying information. Learn more |
| `serviceTier` | Specifies the processing type used for serving the request.   If set to 'auto', then the request will be processed with the service tier configured in     the Project settings. Unless otherwise configured, the Project will use 'default'. If set     to 'default', then the request will be processed with the standard pricing and performance     for the selected model. If set to 'flex' or 'priority', then the request will be processed     with the corresponding service tier. When not set, the default behavior is 'auto'. When the     service_tier parameter is set, the response body will include the service_tier value based     on the processing mode actually used to serve the request. This response value may be     different from the value set in the parameter. |
| `store` | Whether to store the generated model response for later retrieval via API. |
| `stream` | If set to true, the model response data will be streamed to the client as it is generated using server-sent events. See the     href="https://platform.openai.com/docs/api-reference/responses-streaming">Streaming section      for more information. |
| `streamOptions` | Options for streaming responses. Only set this when you set stream: true. |
| `temperature` | What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and     deterministic. We generally recommend altering this or top_p but not both. |
| `text` | Configuration options for a text response from the model. Can be plain text or structured JSON data. Learn more:             - Text inputs and outputs - Structured           Outputs |
| `toolChoice` | How the model should select which tool (or tools) to use when generating a response. See the tools parameter to see how to specify which tools the model can call. |
| `tools` | An array of tools the model may call while generating a response. You can specify which tool to use by setting the tool_choice parameter.       We support the following categories of tools:       Built-in tools: Tools that are provided by OpenAI that extend the model's capabilities,     like href="https://platform.openai.com/docs/guides/tools-web-search?api-mode=responses">web     search or file     search. Learn more about href="https://platform.openai.com/docs/guides/tools">built-in     tools. MCP Tools: Integrations with third-party systems via custom MCP servers or     predefined connectors such as Google Drive and SharePoint. Learn more about     href="https://platform.openai.com/docs/guides/tools-connectors-mcp">MCP Tools. Function     calls (custom tools): Functions that are defined by you, enabling the model to call your     own code with strongly typed arguments and outputs. Learn more about     href="https://platform.openai.com/docs/guides/function-calling">function calling. You     can also use custom tools to call your own code. |
| `topLogprobs` | An integer between 0 and 20 specifying the number of most likely tokens to return at each token position, each with an associated log probability. |
| `topP` | An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass. So 0.1 means only     the tokens comprising the top 10% probability mass are considered.       We generally recommend altering this or temperature but not both. |
| `truncation` | The truncation strategy to use for the model response.   auto: If the input to this Response exceeds the model's context window size, the model     will truncate the response to fit the context window by dropping items from the beginning     of the conversation. disabled (default): If the input size will exceed the context window     size for a model, the request will fail with a 400 error. |
| `openRouterCustomPayload` | custom payload designed for OpenRouter Responses. |

---

### `streaming`

```java
public StreamingBuilder streaming()
```

Returns a StreamingBuilder for building streaming payloads with compile-time type safety.

Example:

```java
var payload = CreateResponsePayload.builder()
    .model("gpt-4o")
    .addUserMessage("Hello")
    .streaming()  // Returns StreamingBuilder
    .build();     // Returns Streaming type
responder.respond(payload)  // No cast needed
    .onTextDelta(System.out::print)
    .start();
```

**Returns**

a StreamingBuilder that produces Streaming payloads

---

### `streaming`

```java
public StructuredStreamingBuilder<T> streaming()
```

Returns a StructuredStreamingBuilder for building structured streaming payloads with
compile-time type safety.

Example:

```java
var payload = CreateResponsePayload.builder()
    .model("gpt-4o")
    .addUserMessage("Create a person")
    .withStructuredOutput(Person.class)
    .streaming()  // Returns StructuredStreamingBuilder
    .build();     // Returns StructuredStreaming
responder.respond(payload)  // No cast needed!
    .onTextDelta(System.out::print)
    .start();
```

**Returns**

a StructuredStreamingBuilder that produces StructuredStreaming payloads

---

### `Streaming`

```java
public Streaming(@NonNull CreateResponsePayload payload)
```

Creates a Streaming response payload by copying from a base payload.

---

### `Streaming`

```java
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
        @Nullable Reason
```

Creates a Streaming response payload with all individual parameters.

---

### `Structured`

```java
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
        @Nullable Reaso
```

Creates a Structured response payload with all individual parameters.

---

### `Structured`

```java
public Structured(@NonNull CreateResponsePayload payload, @NonNull Class<T> responseType)
```

Creates a Structured response payload by copying all fields from a CreateResponsePayload and
adding the responseType parameter.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the CreateResponsePayload to copy from |
| `responseType` | the Class representing the structured response type |

---

### `StructuredStreaming`

```java
public StructuredStreaming(
        @NonNull CreateResponsePayload payload, @NonNull Class<T> responseType)
```

Creates a StructuredStreaming response payload by copying from a base payload.
