# Package `com.paragon.responses.streaming`

> This docs was updated at: 2026-02-23

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`PartialJsonParser`](partialjsonparser.md) | A lenient JSON parser that attempts to parse incomplete JSON strings |
| [`ResponseStream`](responsestream.md) | A streaming response wrapper for OpenAI Responses API Server-Sent Events (SSE) |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`CodeInterpreterCallCodeDeltaEvent`](codeinterpretercallcodedeltaevent.md) | Emitted when a partial code snippet is streamed by the code interpreter |
| [`CodeInterpreterCallCodeDoneEvent`](codeinterpretercallcodedoneevent.md) | Emitted when the code snippet is finalized by the code interpreter |
| [`CodeInterpreterCallCompletedEvent`](codeinterpretercallcompletedevent.md) | Emitted when the code interpreter call is completed |
| [`CodeInterpreterCallInProgressEvent`](codeinterpretercallinprogressevent.md) | Emitted when a code interpreter call is in progress |
| [`CodeInterpreterCallInterpretingEvent`](codeinterpretercallinterpretingevent.md) | Emitted when the code interpreter is actively interpreting the code snippet |
| [`ContentPartAddedEvent`](contentpartaddedevent.md) | Emitted when a new content part is added |
| [`ContentPartDoneEvent`](contentpartdoneevent.md) | Emitted when a content part is done |
| [`CustomToolCallInputDeltaEvent`](customtoolcallinputdeltaevent.md) | Event representing a delta (partial update) to the input of a custom tool call |
| [`CustomToolCallInputDoneEvent`](customtoolcallinputdoneevent.md) | Event indicating that input for a custom tool call is complete |
| [`FileSearchCallCompletedEvent`](filesearchcallcompletedevent.md) | Emitted when a file search call is completed (results found) |
| [`FileSearchCallInProgressEvent`](filesearchcallinprogressevent.md) | Emitted when a file search call is initiated |
| [`FileSearchCallSearchingEvent`](filesearchcallsearchingevent.md) | Emitted when a file search is currently searching |
| [`FunctionCallArgumentsDeltaEvent`](functioncallargumentsdeltaevent.md) | Emitted when there is a partial function-call arguments delta |
| [`FunctionCallArgumentsDoneEvent`](functioncallargumentsdoneevent.md) | Emitted when function-call arguments are finalized |
| [`ImageGenerationCallCompletedEvent`](imagegenerationcallcompletedevent.md) | Emitted when an image generation tool call has completed and the final image is available |
| [`ImageGenerationCallGeneratingEvent`](imagegenerationcallgeneratingevent.md) | Emitted when an image generation tool call is actively generating an image |
| [`ImageGenerationCallInProgressEvent`](imagegenerationcallinprogressevent.md) | Emitted when an image generation tool call is in progress |
| [`ImageGenerationCallPartialImageEvent`](imagegenerationcallpartialimageevent.md) | Emitted when a partial image is available during image generation streaming |
| [`McpCallArgumentsDeltaEvent`](mcpcallargumentsdeltaevent.md) | Emitted when there is a delta (partial update) to the arguments of an MCP tool call |
| [`McpCallArgumentsDoneEvent`](mcpcallargumentsdoneevent.md) | Emitted when the arguments for an MCP tool call are finalized |
| [`McpCallCompletedEvent`](mcpcallcompletedevent.md) | Emitted when an MCP tool call has completed successfully |
| [`McpCallFailedEvent`](mcpcallfailedevent.md) | Emitted when an MCP tool call has failed |
| [`McpCallInProgressEvent`](mcpcallinprogressevent.md) | Emitted when an MCP tool call is in progress |
| [`McpListToolsCompletedEvent`](mcplisttoolscompletedevent.md) | Emitted when the list of available MCP tools has been successfully retrieved |
| [`McpListToolsFailedEvent`](mcplisttoolsfailedevent.md) | Emitted when the attempt to list available MCP tools has failed |
| [`McpListToolsInProgressEvent`](mcplisttoolsinprogressevent.md) | Emitted when the system is in the process of retrieving the list of available MCP tools |
| [`OutputItemAddedEvent`](outputitemaddedevent.md) | Emitted when a new output item is added |
| [`OutputItemDoneEvent`](outputitemdoneevent.md) | Emitted when an output item is marked done |
| [`OutputTextAnnotationAddedEvent`](outputtextannotationaddedevent.md) | Emitted when an annotation is added to output text content |
| [`OutputTextDeltaEvent`](outputtextdeltaevent.md) | Emitted when there is an additional text delta |
| [`OutputTextDoneEvent`](outputtextdoneevent.md) | Emitted when text content is finalized |
| [`ReasoningSummaryPartAddedEvent`](reasoningsummarypartaddedevent.md) | Emitted when a new reasoning summary part is added |
| [`ReasoningSummaryPartDoneEvent`](reasoningsummarypartdoneevent.md) | Emitted when a reasoning summary part is completed |
| [`ReasoningSummaryTextDeltaEvent`](reasoningsummarytextdeltaevent.md) | Emitted when a delta is added to a reasoning summary text |
| [`ReasoningSummaryTextDoneEvent`](reasoningsummarytextdoneevent.md) | Emitted when a reasoning summary text is completed |
| [`ReasoningTextDeltaEvent`](reasoningtextdeltaevent.md) | Emitted when a delta is added to a reasoning text |
| [`ReasoningTextDoneEvent`](reasoningtextdoneevent.md) | Emitted when a reasoning text is completed |
| [`RefusalDeltaEvent`](refusaldeltaevent.md) | Emitted when there is a partial refusal text |
| [`RefusalDoneEvent`](refusaldoneevent.md) | Emitted when refusal text is finalized |
| [`ResponseCompletedEvent`](responsecompletedevent.md) | Emitted when the model response is complete |
| [`ResponseCreatedEvent`](responsecreatedevent.md) | Emitted when a response is created |
| [`ResponseFailedEvent`](responsefailedevent.md) | Emitted when a response fails |
| [`ResponseInProgressEvent`](responseinprogressevent.md) | Emitted when the response is in progress |
| [`ResponseIncompleteEvent`](responseincompleteevent.md) | Emitted when a response finishes as incomplete |
| [`ResponseQueuedEvent`](responsequeuedevent.md) | Emitted when a response is queued and waiting to be processed |
| [`StreamingErrorEvent`](streamingerrorevent.md) | Emitted when an error occurs during streaming |
| [`WebSearchCallCompletedEvent`](websearchcallcompletedevent.md) | Emitted when a web search call is completed |
| [`WebSearchCallInProgressEvent`](websearchcallinprogressevent.md) | Emitted when a web search call is initiated |
| [`WebSearchCallSearchingEvent`](websearchcallsearchingevent.md) | Emitted when a web search call is executing |
