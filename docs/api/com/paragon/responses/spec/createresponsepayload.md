# :material-code-braces: CreateResponsePayload

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
| `background` | Whether to run the model response in the background. See [https://platform.openai.com/docs/guides/background](https://platform.openai.com/docs/guides/background) |
| `conversation` | The conversation that this response belongs to. Items from this conversation are prepended to input_items for this response request. Input items and output items from this response are automatically added to this conversation after this response completes. |
| `include` | Specify additional output data to include in the model response. Currently supported values are: web_search_call.action.sources: Include the sources of the web search tool call. code_interpreter_call.outputs: Includes the outputs of python code execution in code interpreter tool call items. computer_call_output.output.image_url: Include image urls from the computer call output. file_search_call.results: Include the search results of the file search tool call. message.input_image.image_url: Include image urls from the input message. message.output_text.logprobs: Include logprobs with assistant messages. reasoning.encrypted_content: Includes an encrypted version of reasoning tokens in reasoning item outputs. This enables reasoning items to be used in multi-turn conversations when using the Responses API statelessly (like when the store parameter is set to false, or when an organization is enrolled in the zero data retention program). |
| `input` | Text, image, or file inputs to the model, used to generate a response. Guides: text [https://platform.openai.com/docs/guides/text,](https://platform.openai.com/docs/guides/text,) images [https://platform.openai.com/docs/guides/images-vision?api-mode=responses,](https://platform.openai.com/docs/guides/images-vision?api-mode=responses,) files [https://platform.openai.com/docs/guides/pdf-files?api-mode=responses,](https://platform.openai.com/docs/guides/pdf-files?api-mode=responses,) conversation state [https://platform.openai.com/docs/guides/conversation-state?api-mode=responses,](https://platform.openai.com/docs/guides/conversation-state?api-mode=responses,) function calling [https://platform.openai.com/docs/guides/function-calling](https://platform.openai.com/docs/guides/function-calling) |
| `instructions` | When using along with previous_response_id, the instructions from a previous response will not be carried over to the next response. This makes it simple to swap out system (or developer) messages in new responses. |
| `maxOutputTokens` | An upper bound for the number of tokens that can be generated for a response, including visible output tokens and reasoning tokens. See [https://platform.openai.com/docs/guides/reasoning](https://platform.openai.com/docs/guides/reasoning) |
| `maxToolCalls` | The maximum number of total calls to built-in tools that can be processed in a response. This maximum number applies across all built-in tool calls, not per individual tool. Any further attempts to call a tool by the model will be ignored. |
| `metadata` | Set of 16 key-value pairs that can be attached to an object. This can be useful for storing additional information about the object in a structured format, and querying for objects via API or the dashboard.  Keys are strings with a maximum length of 64 characters. Values are strings with a maximum length of 512 characters. |
| `model` | Model ID used to generate the response, like gpt-4o or o3. OpenAI offers a wide range of models with different capabilities, performance characteristics, and price points. Refer to the model guide at [https://openrouter.ai/models](https://openrouter.ai/models) to browse and compare available models. |
| `parallelToolCalls` | Whether to allow the model to run tool calls in parallel. |
| `prompt` | Reference to a prompt template and its variables. See [https://platform.openai.com/docs/guides/text?api-mode=responses#reusable-prompts](https://platform.openai.com/docs/guides/text?api-mode=responses#reusable-prompts) |
| `promptCacheKey` | Used by OpenAI to cache responses for similar requests to optimize your cache hit rates. Replaces the user field. See [https://platform.openai.com/docs/guides/prompt-caching](https://platform.openai.com/docs/guides/prompt-caching) |
| `promptCacheRetention` | The retention policy for the prompt cache. Set to 24h to enable extended prompt caching, which keeps cached prefixes active for longer, up to a maximum of 24 hours. |
| `reasoning` | Configuration options for reasoning models. See [https://platform.openai.com/docs/guides/reasoning](https://platform.openai.com/docs/guides/reasoning) |
| `safetyIdentifier` | A stable identifier used to help detect users of your application that may be violating OpenAI's usage policies. The IDs should be a string that uniquely identifies each user. We recommend hashing their username or email address, in order to avoid sending us any identifying information. See [https://platform.openai.com/docs/guides/safety-best-practices#safety-identifiers](https://platform.openai.com/docs/guides/safety-best-practices#safety-identifiers) |
| `serviceTier` | Specifies the processing type used for serving the request.   If set to 'auto', then the request will be processed with the service tier configured in the Project settings. Unless otherwise configured, the Project will use 'default'. If set to 'default', then the request will be processed with the standard pricing and performance for the selected model. If set to 'flex' or 'priority', then the request will be processed with the corresponding service tier. When not set, the default behavior is 'auto'. When the service_tier parameter is set, the response body will include the service_tier value based on the processing mode actually used to serve the request. This response value may be different from the value set in the parameter. |
| `store` | Whether to store the generated model response for later retrieval via API. |
| `stream` | If set to true, the model response data will be streamed to the client as it is generated using server-sent events. See [https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#Event_stream_format](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#Event_stream_format) and [https://platform.openai.com/docs/api-reference/responses-streaming](https://platform.openai.com/docs/api-reference/responses-streaming) |
| `streamOptions` | Options for streaming responses. Only set this when you set stream: true. |
| `temperature` | What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic. We generally recommend altering this or top_p but not both. |
| `text` | Configuration options for a text response from the model. Can be plain text or structured JSON data. See [https://platform.openai.com/docs/guides/text](https://platform.openai.com/docs/guides/text) and [https://platform.openai.com/docs/guides/structured-outputs](https://platform.openai.com/docs/guides/structured-outputs) |
| `toolChoice` | How the model should select which tool (or tools) to use when generating a response. See the tools parameter to see how to specify which tools the model can call. |
| `tools` | An array of tools the model may call while generating a response. You can specify which tool to use by setting the tool_choice parameter. Supported categories include built-in tools, MCP tools, and function tools. References: web search [https://platform.openai.com/docs/guides/tools-web-search?api-mode=responses,](https://platform.openai.com/docs/guides/tools-web-search?api-mode=responses,) file search [https://platform.openai.com/docs/guides/tools-file-search,](https://platform.openai.com/docs/guides/tools-file-search,) built-in tools [https://platform.openai.com/docs/guides/tools,](https://platform.openai.com/docs/guides/tools,) MCP tools [https://platform.openai.com/docs/guides/tools-connectors-mcp,](https://platform.openai.com/docs/guides/tools-connectors-mcp,) function calling [https://platform.openai.com/docs/guides/function-calling](https://platform.openai.com/docs/guides/function-calling) |
| `topLogprobs` | An integer between 0 and 20 specifying the number of most likely tokens to return at each token position, each with an associated log probability. |
| `topP` | An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  We generally recommend altering this or temperature but not both. |
| `truncation` | The truncation strategy to use for the model response. `auto` drops items from the beginning of the conversation when needed to fit the context window; `disabled` fails the request instead. |
| `openRouterCustomPayload` | custom payload designed for OpenRouter Responses. |

---

### `sanitizeSchemaName`

```java
private static String sanitizeSchemaName(@NonNull Class<?> structuredOutput)
```

Ensures the JSON schema name matches ^[a-zA-Z0-9_-]+$ as required by the API.

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
