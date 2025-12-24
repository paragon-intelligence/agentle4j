# OpenAI API

The OpenAI REST API. Please see https://platform.openai.com/docs/api-reference for more details.

**Version:** 2.3.0

---

## Endpoints

### POST /responses

Create a model response

Creates a model response. Provide [text](https://platform.openai.com/docs/guides/text) or
[image](https://platform.openai.com/docs/guides/images) inputs to
generate [text](https://platform.openai.com/docs/guides/text)
or [JSON](https://platform.openai.com/docs/guides/structured-outputs) outputs. Have the model call
your own [custom code](https://platform.openai.com/docs/guides/function-calling) or use built-in
[tools](https://platform.openai.com/docs/guides/tools)
like [web search](https://platform.openai.com/docs/guides/tools-web-search)
or [file search](https://platform.openai.com/docs/guides/tools-file-search) to use your own data
as input for the model's response.

**Operation ID:** `createResponsePayload`

#### Request Body

**Content-Type:** `application/json`

**Schema:** [CreateResponse](#createresponse)

#### Responses

**200** - OK

- Content-Type: `application/json`
    - Schema: [Response](#response)
- Content-Type: `text/event-stream`
    - Schema: [ResponseStreamEvent](#responsestreamevent)

---

## Schemas

### Annotation

**anyOf:**

- [FileCitationBody](#filecitationbody)
- [UrlCitationBody](#urlcitationbody)
- [ContainerFileCitationBody](#containerfilecitationbody)
- [FilePath](#filepath)

---

### ApplyPatchCallOutputStatus

**Type:** `string`

**Possible values:**

- `completed`
- `failed`

---

### ApplyPatchCallOutputStatusParam

Outcome values reported for apply_patch tool call outputs.

**Type:** `string`

**Possible values:**

- `completed`
- `failed`

---

### ApplyPatchCallStatus

**Type:** `string`

**Possible values:**

- `in_progress`
- `completed`

---

### ApplyPatchCallStatusParam

Status values reported for apply_patch tool calls.

**Type:** `string`

**Possible values:**

- `in_progress`
- `completed`

---

### ApplyPatchCreateFileOperation

Instruction describing how to create a file via the apply_patch tool.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                               |
|----------|---------------|----------|-------------------------------------------|
| `type`   | string (enum) | ✓        | Create a new file with the provided diff. |
| `path`   | string        | ✓        | Path of the file to create.               |
| `diff`   | string        | ✓        | Diff to apply.                            |

---

### ApplyPatchCreateFileOperationParam

Instruction for creating a new file via the apply_patch tool.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                |
|----------|---------------|----------|------------------------------------------------------------|
| `type`   | string (enum) | ✓        | The operation type. Always `create_file`.                  |
| `path`   | string        | ✓        | Path of the file to create relative to the workspace root. |
| `diff`   | string        | ✓        | Unified diff content to apply when creating the file.      |

---

### ApplyPatchDeleteFileOperation

Instruction describing how to delete a file via the apply_patch tool.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                 |
|----------|---------------|----------|-----------------------------|
| `type`   | string (enum) | ✓        | Delete the specified file.  |
| `path`   | string        | ✓        | Path of the file to delete. |

---

### ApplyPatchDeleteFileOperationParam

Instruction for deleting an existing file via the apply_patch tool.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                |
|----------|---------------|----------|------------------------------------------------------------|
| `type`   | string (enum) | ✓        | The operation type. Always `delete_file`.                  |
| `path`   | string        | ✓        | Path of the file to delete relative to the workspace root. |

---

### ApplyPatchOperationParam

One of the create_file, delete_file, or update_file operations supplied to the apply_patch tool.

**anyOf:**

- [ApplyPatchCreateFileOperationParam](#applypatchcreatefileoperationparam)
- [ApplyPatchDeleteFileOperationParam](#applypatchdeletefileoperationparam)
- [ApplyPatchUpdateFileOperationParam](#applypatchupdatefileoperationparam)

---

### ApplyPatchToolCall

A tool call that applies file diffs by creating, deleting, or updating files.

**Type:** `object`

**Properties:**

| Property     | Type                                          | Required | Description                                                                               |
|--------------|-----------------------------------------------|----------|-------------------------------------------------------------------------------------------|
| `type`       | string (enum)                                 | ✓        | The type of the item. Always `apply_patch_call`.                                          |
| `id`         | string                                        | ✓        | The unique ID of the apply patch tool call. Populated when this item is returned via API. |
| `call_id`    | string                                        | ✓        | The unique ID of the apply patch tool call generated by the model.                        |
| `status`     | [ApplyPatchCallStatus](#applypatchcallstatus) | ✓        | The status of the apply patch tool call. One of `in_progress` or `completed`.             |
| `operation`  | any                                           | ✓        | One of the create_file, delete_file, or update_file operations applied via apply_patch.   |
| `created_by` | string                                        |          | The ID of the entity that created this tool call.                                         |

---

### ApplyPatchToolCallItemParam

A tool call representing a request to create, delete, or update files using diff patches.

**Type:** `object`

**Properties:**

| Property    | Type                                                    | Required | Description                                                                       |
|-------------|---------------------------------------------------------|----------|-----------------------------------------------------------------------------------|
| `type`      | string (enum)                                           | ✓        | The type of the item. Always `apply_patch_call`.                                  |
| `id`        | any                                                     |          |                                                                                   |
| `call_id`   | string                                                  | ✓        | The unique ID of the apply patch tool call generated by the model.                |
| `status`    | [ApplyPatchCallStatusParam](#applypatchcallstatusparam) | ✓        | The status of the apply patch tool call. One of `in_progress` or `completed`.     |
| `operation` | [ApplyPatchOperationParam](#applypatchoperationparam)   | ✓        | The specific create, delete, or update instruction for the apply_patch tool call. |

---

### ApplyPatchToolCallOutput

The output emitted by an apply patch tool call.

**Type:** `object`

**Properties:**

| Property     | Type                                                      | Required | Description                                                                                      |
|--------------|-----------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------|
| `type`       | string (enum)                                             | ✓        | The type of the item. Always `apply_patch_call_output`.                                          |
| `id`         | string                                                    | ✓        | The unique ID of the apply patch tool call output. Populated when this item is returned via API. |
| `call_id`    | string                                                    | ✓        | The unique ID of the apply patch tool call generated by the model.                               |
| `status`     | [ApplyPatchCallOutputStatus](#applypatchcalloutputstatus) | ✓        | The status of the apply patch tool call output. One of `completed` or `failed`.                  |
| `output`     | any                                                       |          |                                                                                                  |
| `created_by` | string                                                    |          | The ID of the entity that created this tool call output.                                         |

---

### ApplyPatchToolCallOutputItemParam

The streamed output emitted by an apply patch tool call.

**Type:** `object`

**Properties:**

| Property  | Type                                                                | Required | Description                                                                     |
|-----------|---------------------------------------------------------------------|----------|---------------------------------------------------------------------------------|
| `type`    | string (enum)                                                       | ✓        | The type of the item. Always `apply_patch_call_output`.                         |
| `id`      | any                                                                 |          |                                                                                 |
| `call_id` | string                                                              | ✓        | The unique ID of the apply patch tool call generated by the model.              |
| `status`  | [ApplyPatchCallOutputStatusParam](#applypatchcalloutputstatusparam) | ✓        | The status of the apply patch tool call output. One of `completed` or `failed`. |
| `output`  | any                                                                 |          |                                                                                 |

---

### ApplyPatchToolParam

Allows the assistant to create, delete, or update files using unified diffs.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                 |
|----------|---------------|----------|---------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the tool. Always `apply_patch`. |

---

### ApplyPatchUpdateFileOperation

Instruction describing how to update a file via the apply_patch tool.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                     |
|----------|---------------|----------|-------------------------------------------------|
| `type`   | string (enum) | ✓        | Update an existing file with the provided diff. |
| `path`   | string        | ✓        | Path of the file to update.                     |
| `diff`   | string        | ✓        | Diff to apply.                                  |

---

### ApplyPatchUpdateFileOperationParam

Instruction for updating an existing file via the apply_patch tool.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                |
|----------|---------------|----------|------------------------------------------------------------|
| `type`   | string (enum) | ✓        | The operation type. Always `update_file`.                  |
| `path`   | string        | ✓        | Path of the file to update relative to the workspace root. |
| `diff`   | string        | ✓        | Unified diff content to apply to the existing file.        |

---

### ApproximateLocation

**Type:** `object`

**Properties:**

| Property   | Type          | Required | Description                                               |
|------------|---------------|----------|-----------------------------------------------------------|
| `type`     | string (enum) | ✓        | The type of location approximation. Always `approximate`. |
| `country`  | any           |          |                                                           |
| `region`   | any           |          |                                                           |
| `city`     | any           |          |                                                           |
| `timezone` | any           |          |                                                           |

---

### ChatModel

**Type:** `string`

**Possible values:**

- `gpt-5.1`
- `gpt-5.1-2025-11-13`
- `gpt-5.1-codex`
- `gpt-5.1-mini`
- `gpt-5.1-chat-latest`
- `gpt-5`
- `gpt-5-mini`
- `gpt-5-nano`
- `gpt-5-2025-08-07`
- `gpt-5-mini-2025-08-07`
- `gpt-5-nano-2025-08-07`
- `gpt-5-chat-latest`
- `gpt-4.1`
- `gpt-4.1-mini`
- `gpt-4.1-nano`
- `gpt-4.1-2025-04-14`
- `gpt-4.1-mini-2025-04-14`
- `gpt-4.1-nano-2025-04-14`
- `o4-mini`
- `o4-mini-2025-04-16`
- `o3`
- `o3-2025-04-16`
- `o3-mini`
- `o3-mini-2025-01-31`
- `o1`
- `o1-2024-12-17`
- `o1-preview`
- `o1-preview-2024-09-12`
- `o1-mini`
- `o1-mini-2024-09-12`
- `gpt-4o`
- `gpt-4o-2024-11-20`
- `gpt-4o-2024-08-06`
- `gpt-4o-2024-05-13`
- `gpt-4o-audio-preview`
- `gpt-4o-audio-preview-2024-10-01`
- `gpt-4o-audio-preview-2024-12-17`
- `gpt-4o-audio-preview-2025-06-03`
- `gpt-4o-mini-audio-preview`
- `gpt-4o-mini-audio-preview-2024-12-17`
- `gpt-4o-search-preview`
- `gpt-4o-mini-search-preview`
- `gpt-4o-search-preview-2025-03-11`
- `gpt-4o-mini-search-preview-2025-03-11`
- `chatgpt-4o-latest`
- `codex-mini-latest`
- `gpt-4o-mini`
- `gpt-4o-mini-2024-07-18`
- `gpt-4-turbo`
- `gpt-4-turbo-2024-04-09`
- `gpt-4-0125-preview`
- `gpt-4-turbo-preview`
- `gpt-4-1106-preview`
- `gpt-4-vision-preview`
- `gpt-4`
- `gpt-4-0314`
- `gpt-4-0613`
- `gpt-4-32k`
- `gpt-4-32k-0314`
- `gpt-4-32k-0613`
- `gpt-3.5-turbo`
- `gpt-3.5-turbo-16k`
- `gpt-3.5-turbo-0301`
- `gpt-3.5-turbo-0613`
- `gpt-3.5-turbo-1106`
- `gpt-3.5-turbo-0125`
- `gpt-3.5-turbo-16k-0613`

---

### ClickButtonType

**Type:** `string`

**Possible values:**

- `left`
- `right`
- `wheel`
- `back`
- `forward`

---

### ClickParam

A click action.

**Type:** `object`

**Properties:**

| Property | Type                                | Required | Description                                                                                                       |
|----------|-------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------|
| `type`   | string (enum)                       | ✓        | Specifies the event type. For a click action, this property is always `click`.                                    |
| `button` | [ClickButtonType](#clickbuttontype) | ✓        | Indicates which mouse button was pressed during the click. One of `left`, `right`, `wheel`, `back`, or `forward`. |
| `x`      | integer                             | ✓        | The x-coordinate where the click occurred.                                                                        |
| `y`      | integer                             | ✓        | The y-coordinate where the click occurred.                                                                        |

---

### CodeInterpreterContainerAuto

Configuration for a code interpreter container. Optionally specify the IDs of the files to run the code on.

**Type:** `object`

**Properties:**

| Property       | Type          | Required | Description                                                        |
|----------------|---------------|----------|--------------------------------------------------------------------|
| `type`         | string (enum) | ✓        | Always `auto`.                                                     |
| `file_ids`     | array[string] |          | An optional list of uploaded files to make available to your code. |
| `memory_limit` | any           |          |                                                                    |

---

### CodeInterpreterOutputImage

The image output from the code interpreter.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                            |
|----------|---------------|----------|--------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the output. Always `image`.                |
| `url`    | string        | ✓        | The URL of the image output from the code interpreter. |

---

### CodeInterpreterOutputLogs

The logs output from the code interpreter.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                |
|----------|---------------|----------|--------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the output. Always `logs`.     |
| `logs`   | string        | ✓        | The logs output from the code interpreter. |

---

### CodeInterpreterTool

A tool that runs Python code to help generate a response to a prompt.

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                                                                                                                         |
|-------------|---------------|----------|-------------------------------------------------------------------------------------------------------------------------------------|
| `type`      | string (enum) | ✓        | The type of the code interpreter tool. Always `code_interpreter`.                                                                   |
| `container` | any           | ✓        | The code interpreter container. Can be a container ID or an object that specifies uploaded file IDs to make available to your code. |

---

### CodeInterpreterToolCall

A tool call to run code.

**Type:** `object`

**Properties:**

| Property       | Type          | Required | Description                                                                                                                            |
|----------------|---------------|----------|----------------------------------------------------------------------------------------------------------------------------------------|
| `type`         | string (enum) | ✓        | The type of the code interpreter tool call. Always `code_interpreter_call`.                                                            |
| `id`           | string        | ✓        | The unique ID of the code interpreter tool call.                                                                                       |
| `status`       | string (enum) | ✓        | The status of the code interpreter tool call. Valid values are `in_progress`, `completed`, `incomplete`, `interpreting`, and `failed`. |
| `container_id` | string        | ✓        | The ID of the container used to run the code.                                                                                          |
| `code`         | any           | ✓        |                                                                                                                                        |
| `outputs`      | any           | ✓        |                                                                                                                                        |

---

### ComparisonFilter

A filter used to compare a specified attribute key to a given value using a defined comparison operation.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                                                                                                                                                                                      |
|----------|---------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Specifies the comparison agent: `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `in`, `nin`. - `eq`: equals - `ne`: not equal - `gt`: greater than - `gte`: greater than or equal - `lt`: less than - `lte`: less than or equal - `in`: in - `nin`: not in |
| `key`    | string        | ✓        | The key to compare against the value.                                                                                                                                                                                                            |
| `value`  | any           | ✓        | The value to compare against the attribute key; supports string, number, or boolean types.                                                                                                                                                       |

---

### ComparisonFilterValueItems

**anyOf:**

- string
- number

---

### CompoundFilter

Combine multiple filters using `and` or `or`.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                                                       |
|-----------|---------------|----------|-----------------------------------------------------------------------------------|
| `type`    | string (enum) | ✓        | Type of operation: `and` or `or`.                                                 |
| `filters` | array[any]    | ✓        | Array of filters to combine. Items can be `ComparisonFilter` or `CompoundFilter`. |

---

### ComputerAction

**anyOf:**

- [ClickParam](#clickparam)
- [DoubleClickAction](#doubleclickaction)
- [Drag](#drag)
- [KeyPressAction](#keypressaction)
- [Move](#move)
- [Screenshot](#screenshot)
- [Scroll](#scroll)
- [Type](#type)
- [Wait](#wait)

---

### ComputerCallOutputItemParam

The output of a computer tool call.

**Type:** `object`

**Properties:**

| Property                     | Type                                                | Required | Description                                                               |
|------------------------------|-----------------------------------------------------|----------|---------------------------------------------------------------------------|
| `id`                         | any                                                 |          |                                                                           |
| `call_id`                    | string                                              | ✓        | The ID of the computer tool call that produced the output.                |
| `type`                       | string (enum)                                       | ✓        | The type of the computer tool call output. Always `computer_call_output`. |
| `output`                     | [ComputerScreenshotImage](#computerscreenshotimage) | ✓        |                                                                           |
| `acknowledged_safety_checks` | any                                                 |          |                                                                           |
| `status`                     | any                                                 |          |                                                                           |

---

### ComputerCallSafetyCheckParam

A pending safety check for the computer call.

**Type:** `object`

**Properties:**

| Property  | Type   | Required | Description                         |
|-----------|--------|----------|-------------------------------------|
| `id`      | string | ✓        | The ID of the pending safety check. |
| `code`    | any    |          |                                     |
| `message` | any    |          |                                     |

---

### ComputerEnvironment

**Type:** `string`

**Possible values:**

- `windows`
- `mac`
- `linux`
- `ubuntu`
- `browser`

---

### ComputerScreenshotImage

A computer screenshot image used with the computer use tool.

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                                                                                                 |
|-------------|---------------|----------|-------------------------------------------------------------------------------------------------------------|
| `type`      | string (enum) | ✓        | Specifies the event type. For a computer screenshot, this property is  always set to `computer_screenshot`. |
| `image_url` | string        |          | The URL of the screenshot image.                                                                            |
| `file_id`   | string        |          | The identifier of an uploaded file that contains the screenshot.                                            |

---

### ComputerToolCall

A tool call to a computer use tool. See the
[computer use guide](https://platform.openai.com/docs/guides/tools-computer-use) for more information.

**Type:** `object`

**Properties:**

| Property                | Type                                                                 | Required | Description                                                                                                            |
|-------------------------|----------------------------------------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------|
| `type`                  | string (enum)                                                        | ✓        | The type of the computer call. Always `computer_call`.                                                                 |
| `id`                    | string                                                               | ✓        | The unique ID of the computer call.                                                                                    |
| `call_id`               | string                                                               | ✓        | An identifier used when responding to the tool call with output.                                                       |
| `action`                | [ComputerAction](#computeraction)                                    | ✓        |                                                                                                                        |
| `pending_safety_checks` | array[[ComputerCallSafetyCheckParam](#computercallsafetycheckparam)] | ✓        | The pending safety checks for the computer call.                                                                       |
| `status`                | string (enum)                                                        | ✓        | The status of the item. One of `in_progress`, `completed`, or `incomplete`. Populated when items are returned via API. |

---

### ComputerUsePreviewTool

A tool that controls a virtual computer. Learn more about
the [computer tool](https://platform.openai.com/docs/guides/tools-computer-use).

**Type:** `object`

**Properties:**

| Property         | Type                                        | Required | Description                                                       |
|------------------|---------------------------------------------|----------|-------------------------------------------------------------------|
| `type`           | string (enum)                               | ✓        | The type of the computer use tool. Always `computer_use_preview`. |
| `environment`    | [ComputerEnvironment](#computerenvironment) | ✓        | The type of computer environment to control.                      |
| `display_width`  | integer                                     | ✓        | The width of the computer display.                                |
| `display_height` | integer                                     | ✓        | The height of the computer display.                               |

---

### ContainerFileCitationBody

A citation for a container file used to generate a model response.

**Type:** `object`

**Properties:**

| Property       | Type          | Required | Description                                                                     |
|----------------|---------------|----------|---------------------------------------------------------------------------------|
| `type`         | string (enum) | ✓        | The type of the container file citation. Always `container_file_citation`.      |
| `container_id` | string        | ✓        | The ID of the container file.                                                   |
| `file_id`      | string        | ✓        | The ID of the file.                                                             |
| `start_index`  | integer       | ✓        | The index of the first character of the container file citation in the message. |
| `end_index`    | integer       | ✓        | The index of the last character of the container file citation in the message.  |
| `filename`     | string        | ✓        | The filename of the container file cited.                                       |

---

### ContainerMemoryLimit

**Type:** `string`

**Possible values:**

- `1g`
- `4g`
- `16g`
- `64g`

---

### Conversation-2

The conversation that this response belongs to. Input items and output items from this response are automatically added
to this conversation.

**Type:** `object`

**Properties:**

| Property | Type   | Required | Description                        |
|----------|--------|----------|------------------------------------|
| `id`     | string | ✓        | The unique ID of the conversation. |

---

### ConversationParam

The conversation that this response belongs to. Items from this conversation are prepended to `input_items` for this
response request.
Input items and output items from this response are automatically added to this conversation after this response
completes.

**anyOf:**

- string
- [ConversationParam-2](#conversationparam-2)

---

### ConversationParam-2

The conversation that this response belongs to.

**Type:** `object`

**Properties:**

| Property | Type   | Required | Description                        |
|----------|--------|----------|------------------------------------|
| `id`     | string | ✓        | The unique ID of the conversation. |

---

### CreateModelResponseProperties

**allOf:**

- [ModelResponseProperties](#modelresponseproperties)
- object

---

### CreateResponse

**allOf:**

- [CreateModelResponseProperties](#createmodelresponseproperties)
- [ResponseProperties](#responseproperties)
- object

---

### CustomGrammarFormatParam

A grammar defined by the user.

**Type:** `object`

**Properties:**

| Property     | Type                              | Required | Description                                                     |
|--------------|-----------------------------------|----------|-----------------------------------------------------------------|
| `type`       | string (enum)                     | ✓        | Grammar format. Always `grammar`.                               |
| `syntax`     | [GrammarSyntax1](#grammarsyntax1) | ✓        | The syntax of the grammar definition. One of `lark` or `regex`. |
| `definition` | string                            | ✓        | The grammar definition.                                         |

---

### CustomTextFormatParam

Unconstrained free-form text.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                               |
|----------|---------------|----------|-------------------------------------------|
| `type`   | string (enum) | ✓        | Unconstrained text format. Always `text`. |

---

### CustomToolCall

A call to a custom tool created by the model.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                                            |
|-----------|---------------|----------|------------------------------------------------------------------------|
| `type`    | string (enum) | ✓        | The type of the custom tool call. Always `custom_tool_call`.           |
| `id`      | string        |          | The unique ID of the custom tool call in the OpenAI platform.          |
| `call_id` | string        | ✓        | An identifier used to map this custom tool call to a tool call output. |
| `name`    | string        | ✓        | The name of the custom tool being called.                              |
| `input`   | string        | ✓        | The input for the custom tool call generated by the model.             |

---

### CustomToolCallOutput

The output of a custom tool call from your code, being sent back to the model.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                                                                                |
|-----------|---------------|----------|------------------------------------------------------------------------------------------------------------|
| `type`    | string (enum) | ✓        | The type of the custom tool call output. Always `custom_tool_call_output`.                                 |
| `id`      | string        |          | The unique ID of the custom tool call output in the OpenAI platform.                                       |
| `call_id` | string        | ✓        | The call ID, used to map this custom tool call output to a custom tool call.                               |
| `output`  | any           | ✓        | The output from the custom tool call generated by your code. Can be a string or an list of output content. |

---

### CustomToolParam

A custom tool that processes input using a specified format. Learn more
about   [custom tools](https://platform.openai.com/docs/guides/function-calling#custom-tools)

**Type:** `object`

**Properties:**

| Property      | Type          | Required | Description                                                            |
|---------------|---------------|----------|------------------------------------------------------------------------|
| `type`        | string (enum) | ✓        | The type of the custom tool. Always `custom`.                          |
| `name`        | string        | ✓        | The name of the custom tool, used to identify it in tool calls.        |
| `description` | string        |          | Optional description of the custom tool, used to provide more context. |
| `format`      | any           |          | The input format for the custom tool. Default is unconstrained text.   |

---

### DetailEnum

**Type:** `string`

**Possible values:**

- `low`
- `high`
- `auto`

---

### DoubleClickAction

A double click action.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                                         |
|----------|---------------|----------|-----------------------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Specifies the event type. For a double click action, this property is always set to `double_click`. |
| `x`      | integer       | ✓        | The x-coordinate where the double click occurred.                                                   |
| `y`      | integer       | ✓        | The y-coordinate where the double click occurred.                                                   |

---

### Drag

A drag action.

**Type:** `object`

**Properties:**

| Property | Type                           | Required | Description                                                                                                                                                                 |
|----------|--------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`   | string (enum)                  | ✓        | Specifies the event type. For a drag action, this property is  always set to `drag`.                                                                                        |
| `path`   | array[[DragPoint](#dragpoint)] | ✓        | An array of coordinates representing the path of the drag action. Coordinates will appear as an array of objects, eg ``` [   { x: 100, y: 200 },   { x: 200, y: 300 } ] ``` |

---

### DragPoint

An x/y coordinate pair, e.g. `{ x: 100, y: 200 }`.

**Type:** `object`

**Properties:**

| Property | Type    | Required | Description       |
|----------|---------|----------|-------------------|
| `x`      | integer | ✓        | The x-coordinate. |
| `y`      | integer | ✓        | The y-coordinate. |

---

### EasyInputMessage

A message input to the model with a role indicating instruction following
hierarchy. Instructions given with the `developer` or `system` role take
precedence over instructions given with the `user` role. Messages with the
`assistant` role are presumed to have been generated by the model in previous
interactions.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                                                                                           |
|-----------|---------------|----------|-----------------------------------------------------------------------------------------------------------------------|
| `role`    | string (enum) | ✓        | The role of the message input. One of `user`, `assistant`, `system`, or `developer`.                                  |
| `content` | any           | ✓        | Text, image, or audio input to the model, used to generate a response. Can also contain previous assistant responses. |
| `type`    | string (enum) |          | The type of the message input. Always `message`.                                                                      |

---

### FileCitationBody

A citation to a file.

**Type:** `object`

**Properties:**

| Property   | Type          | Required | Description                                            |
|------------|---------------|----------|--------------------------------------------------------|
| `type`     | string (enum) | ✓        | The type of the file citation. Always `file_citation`. |
| `file_id`  | string        | ✓        | The ID of the file.                                    |
| `index`    | integer       | ✓        | The index of the file in the list of files.            |
| `filename` | string        | ✓        | The filename of the file cited.                        |

---

### FilePath

A path to a file.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                    |
|-----------|---------------|----------|------------------------------------------------|
| `type`    | string (enum) | ✓        | The type of the file path. Always `file_path`. |
| `file_id` | string        | ✓        | The ID of the file.                            |
| `index`   | integer       | ✓        | The index of the file in the list of files.    |

---

### FileSearchTool

A tool that searches for relevant content from uploaded files. Learn more about
the [file search tool](https://platform.openai.com/docs/guides/tools-file-search).

**Type:** `object`

**Properties:**

| Property           | Type                              | Required | Description                                                                                |
|--------------------|-----------------------------------|----------|--------------------------------------------------------------------------------------------|
| `type`             | string (enum)                     | ✓        | The type of the file search tool. Always `file_search`.                                    |
| `vector_store_ids` | array[string]                     | ✓        | The IDs of the vector stores to search.                                                    |
| `max_num_results`  | integer                           |          | The maximum number of results to return. This number should be between 1 and 50 inclusive. |
| `ranking_options`  | [RankingOptions](#rankingoptions) |          | Ranking options for search.                                                                |
| `filters`          | any                               |          |                                                                                            |

---

### FileSearchToolCall

The results of a file search tool call. See the
[file search guide](https://platform.openai.com/docs/guides/tools-file-search) for more information.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                                                                           |
|-----------|---------------|----------|-------------------------------------------------------------------------------------------------------|
| `id`      | string        | ✓        | The unique ID of the file search tool call.                                                           |
| `type`    | string (enum) | ✓        | The type of the file search tool call. Always `file_search_call`.                                     |
| `status`  | string (enum) | ✓        | The status of the file search tool call. One of `in_progress`, `searching`, `incomplete` or `failed`, |
| `queries` | array[string] | ✓        | The queries used to search for files.                                                                 |
| `results` | any           |          |                                                                                                       |

---

### Filters

**anyOf:**

- [ComparisonFilter](#comparisonfilter)
- [CompoundFilter](#compoundfilter)

---

### FunctionAndCustomToolCallOutput

**anyOf:**

- [InputTextContent](#inputtextcontent)
- [InputImageContent](#inputimagecontent)
- [InputFileContent](#inputfilecontent)

---

### FunctionCallItemStatus

**Type:** `string`

**Possible values:**

- `in_progress`
- `completed`
- `incomplete`

---

### FunctionCallOutputItemParam

The output of a function tool call.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                                               |
|-----------|---------------|----------|---------------------------------------------------------------------------|
| `id`      | any           |          |                                                                           |
| `call_id` | string        | ✓        | The unique ID of the function tool call generated by the model.           |
| `type`    | string (enum) | ✓        | The type of the function tool call output. Always `function_call_output`. |
| `output`  | any           | ✓        | Text, image, or file output of the function tool call.                    |
| `status`  | any           |          |                                                                           |

---

### FunctionShellAction

Execute a shell command.

**Type:** `object`

**Properties:**

| Property            | Type          | Required | Description |
|---------------------|---------------|----------|-------------|
| `commands`          | array[string] | ✓        |             |
| `timeout_ms`        | any           | ✓        |             |
| `max_output_length` | any           | ✓        |             |

---

### FunctionShellActionParam

Commands and limits describing how to run the function shell tool call.

**Type:** `object`

**Properties:**

| Property            | Type          | Required | Description                                                  |
|---------------------|---------------|----------|--------------------------------------------------------------|
| `commands`          | array[string] | ✓        | Ordered shell commands for the execution environment to run. |
| `timeout_ms`        | any           |          |                                                              |
| `max_output_length` | any           |          |                                                              |

---

### FunctionShellCall

A tool call that executes one or more shell commands in a managed environment.

**Type:** `object`

**Properties:**

| Property     | Type                                          | Required | Description                                                                                  |
|--------------|-----------------------------------------------|----------|----------------------------------------------------------------------------------------------|
| `type`       | string (enum)                                 | ✓        | The type of the item. Always `shell_call`.                                                   |
| `id`         | string                                        | ✓        | The unique ID of the function shell tool call. Populated when this item is returned via API. |
| `call_id`    | string                                        | ✓        | The unique ID of the function shell tool call generated by the model.                        |
| `action`     | [FunctionShellAction](#functionshellaction)   | ✓        | The shell commands and limits that describe how to run the tool call.                        |
| `status`     | [LocalShellCallStatus](#localshellcallstatus) | ✓        | The status of the shell call. One of `in_progress`, `completed`, or `incomplete`.            |
| `created_by` | string                                        |          | The ID of the entity that created this tool call.                                            |

---

### FunctionShellCallItemParam

A tool representing a request to execute one or more shell commands.

**Type:** `object`

**Properties:**

| Property  | Type                                                  | Required | Description                                                           |
|-----------|-------------------------------------------------------|----------|-----------------------------------------------------------------------|
| `id`      | any                                                   |          |                                                                       |
| `call_id` | string                                                | ✓        | The unique ID of the function shell tool call generated by the model. |
| `type`    | string (enum)                                         | ✓        | The type of the item. Always `function_shell_call`.                   |
| `action`  | [FunctionShellActionParam](#functionshellactionparam) | ✓        | The shell commands and limits that describe how to run the tool call. |
| `status`  | any                                                   |          |                                                                       |

---

### FunctionShellCallItemStatus

Status values reported for function shell tool calls.

**Type:** `string`

**Possible values:**

- `in_progress`
- `completed`
- `incomplete`

---

### FunctionShellCallOutput

The output of a shell tool call.

**Type:** `object`

**Properties:**

| Property            | Type                                                                     | Required | Description                                                                           |
|---------------------|--------------------------------------------------------------------------|----------|---------------------------------------------------------------------------------------|
| `type`              | string (enum)                                                            | ✓        | The type of the shell call output. Always `shell_call_output`.                        |
| `id`                | string                                                                   | ✓        | The unique ID of the shell call output. Populated when this item is returned via API. |
| `call_id`           | string                                                                   | ✓        | The unique ID of the shell tool call generated by the model.                          |
| `output`            | array[[FunctionShellCallOutputContent](#functionshellcalloutputcontent)] | ✓        | An array of shell call output contents                                                |
| `max_output_length` | any                                                                      | ✓        |                                                                                       |
| `created_by`        | string                                                                   |          |                                                                                       |

---

### FunctionShellCallOutputContent

The content of a shell call output.

**Type:** `object`

**Properties:**

| Property     | Type   | Required | Description                                                                                               |
|--------------|--------|----------|-----------------------------------------------------------------------------------------------------------|
| `stdout`     | string | ✓        |                                                                                                           |
| `stderr`     | string | ✓        |                                                                                                           |
| `outcome`    | any    | ✓        | Represents either an exit outcome (with an exit code) or a timeout outcome for a shell call output chunk. |
| `created_by` | string |          |                                                                                                           |

---

### FunctionShellCallOutputContentParam

Captured stdout and stderr for a portion of a function shell tool call output.

**Type:** `object`

**Properties:**

| Property  | Type                                                                        | Required | Description                                              |
|-----------|-----------------------------------------------------------------------------|----------|----------------------------------------------------------|
| `stdout`  | string                                                                      | ✓        | Captured stdout output for this chunk of the shell call. |
| `stderr`  | string                                                                      | ✓        | Captured stderr output for this chunk of the shell call. |
| `outcome` | [FunctionShellCallOutputOutcomeParam](#functionshellcalloutputoutcomeparam) | ✓        | The exit or timeout outcome associated with this chunk.  |

---

### FunctionShellCallOutputExitOutcome

Indicates that the shell commands finished and returned an exit code.

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                       |
|-------------|---------------|----------|-----------------------------------|
| `type`      | string (enum) | ✓        | The outcome type. Always `exit`.  |
| `exit_code` | integer       | ✓        | Exit code from the shell process. |

---

### FunctionShellCallOutputExitOutcomeParam

Indicates that the shell commands finished and returned an exit code.

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                                  |
|-------------|---------------|----------|----------------------------------------------|
| `type`      | string (enum) | ✓        | The outcome type. Always `exit`.             |
| `exit_code` | integer       | ✓        | The exit code returned by the shell process. |

---

### FunctionShellCallOutputItemParam

The streamed output items emitted by a function shell tool call.

**Type:** `object`

**Properties:**

| Property            | Type                                                                               | Required | Description                                                                        |
|---------------------|------------------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------|
| `id`                | any                                                                                |          |                                                                                    |
| `call_id`           | string                                                                             | ✓        | The unique ID of the function shell tool call generated by the model.              |
| `type`              | string (enum)                                                                      | ✓        | The type of the item. Always `function_shell_call_output`.                         |
| `output`            | array[[FunctionShellCallOutputContentParam](#functionshellcalloutputcontentparam)] | ✓        | Captured chunks of stdout and stderr output, along with their associated outcomes. |
| `max_output_length` | any                                                                                |          |                                                                                    |

---

### FunctionShellCallOutputOutcomeParam

The exit or timeout outcome associated with this chunk.

**anyOf:**

- [FunctionShellCallOutputTimeoutOutcomeParam](#functionshellcalloutputtimeoutoutcomeparam)
- [FunctionShellCallOutputExitOutcomeParam](#functionshellcalloutputexitoutcomeparam)

---

### FunctionShellCallOutputTimeoutOutcome

Indicates that the function shell call exceeded its configured time limit.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                         |
|----------|---------------|----------|-------------------------------------|
| `type`   | string (enum) | ✓        | The outcome type. Always `timeout`. |

---

### FunctionShellCallOutputTimeoutOutcomeParam

Indicates that the function shell call exceeded its configured time limit.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                         |
|----------|---------------|----------|-------------------------------------|
| `type`   | string (enum) | ✓        | The outcome type. Always `timeout`. |

---

### FunctionShellToolParam

A tool that allows the model to execute shell commands.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                 |
|----------|---------------|----------|---------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the shell tool. Always `shell`. |

---

### FunctionTool

Defines a function in your own code the model can choose to call. Learn more
about [function calling](https://platform.openai.com/docs/guides/function-calling).

**Type:** `object`

**Properties:**

| Property      | Type          | Required | Description                                       |
|---------------|---------------|----------|---------------------------------------------------|
| `type`        | string (enum) | ✓        | The type of the function tool. Always `function`. |
| `name`        | string        | ✓        | The name of the function to call.                 |
| `description` | any           |          |                                                   |
| `parameters`  | any           | ✓        |                                                   |
| `strict`      | any           | ✓        |                                                   |

---

### FunctionToolCall

A tool call to run a function. See the
[function calling guide](https://platform.openai.com/docs/guides/function-calling) for more information.

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                                                                                                            |
|-------------|---------------|----------|------------------------------------------------------------------------------------------------------------------------|
| `id`        | string        |          | The unique ID of the function tool call.                                                                               |
| `type`      | string (enum) | ✓        | The type of the function tool call. Always `function_call`.                                                            |
| `call_id`   | string        | ✓        | The unique ID of the function tool call generated by the model.                                                        |
| `name`      | string        | ✓        | The name of the function to run.                                                                                       |
| `arguments` | string        | ✓        | A JSON string of the arguments to pass to the function.                                                                |
| `status`    | string (enum) |          | The status of the item. One of `in_progress`, `completed`, or `incomplete`. Populated when items are returned via API. |

---

### GrammarSyntax1

**Type:** `string`

**Possible values:**

- `lark`
- `regex`

---

### HybridSearchOptions

**Type:** `object`

**Properties:**

| Property           | Type   | Required | Description                                                   |
|--------------------|--------|----------|---------------------------------------------------------------|
| `embedding_weight` | number | ✓        | The weight of the embedding in the reciprocal ranking fusion. |
| `text_weight`      | number | ✓        | The weight of the text in the reciprocal ranking fusion.      |

---

### ImageDetail

**Type:** `string`

**Possible values:**

- `low`
- `high`
- `auto`

---

### ImageGenTool

A tool that generates images using a model like `gpt-image-1`.

**Type:** `object`

**Properties:**

| Property             | Type                              | Required | Description                                                                                                |
|----------------------|-----------------------------------|----------|------------------------------------------------------------------------------------------------------------|
| `type`               | string (enum)                     | ✓        | The type of the image generation tool. Always `image_generation`.                                          |
| `model`              | string (enum)                     |          | The image generation model to use. Default: `gpt-image-1`.                                                 |
| `quality`            | string (enum)                     |          | The quality of the generated image. One of `low`, `medium`, `high`, or `auto`. Default: `auto`.            |
| `size`               | string (enum)                     |          | The size of the generated image. One of `1024x1024`, `1024x1536`, `1536x1024`, or `auto`. Default: `auto`. |
| `output_format`      | string (enum)                     |          | The output format of the generated image. One of `png`, `webp`, or `jpeg`. Default: `png`.                 |
| `output_compression` | integer                           |          | Compression level for the output image. Default: 100.                                                      |
| `moderation`         | string (enum)                     |          | Moderation level for the generated image. Default: `auto`.                                                 |
| `background`         | string (enum)                     |          | Background type for the generated image. One of `transparent`, `opaque`, or `auto`. Default: `auto`.       |
| `input_fidelity`     | any                               |          |                                                                                                            |
| `input_image_mask`   | object (no additional properties) |          | Optional mask for inpainting. Contains `image_url` (string, optional) and `file_id` (string, optional).    |
| `partial_images`     | integer                           |          | Number of partial images to generate in streaming mode, from 0 (default value) to 3.                       |

---

### ImageGenToolCall

An image generation request made by the model.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                            |
|----------|---------------|----------|------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the image generation call. Always `image_generation_call`. |
| `id`     | string        | ✓        | The unique ID of the image generation call.                            |
| `status` | string (enum) | ✓        | The status of the image generation call.                               |
| `result` | any           | ✓        |                                                                        |

---

### IncludeEnum

Specify additional output data to include in the model response. Currently supported values are:

- `web_search_call.action.sources`: Include the sources of the web search tool call.
- `code_interpreter_call.outputs`: Includes the outputs of python code execution in code interpreter tool call items.
- `computer_call_output.output.image_url`: Include image urls from the computer call output.
- `file_search_call.results`: Include the search results of the file search tool call.
- `message.input_image.image_url`: Include image urls from the input message.
- `message.output_text.logprobs`: Include logprobs with assistant messages.
- `reasoning.encrypted_content`: Includes an encrypted version of reasoning tokens in reasoning item outputs. This
  enables reasoning items to be used in multi-turn conversations when using the Responses API statelessly (like when the
  `store` parameter is set to `false`, or when an organization is enrolled in the zero data retention program).

**Type:** `string`

**Possible values:**

- `file_search_call.results`
- `web_search_call.results`
- `web_search_call.action.sources`
- `message.input_image.image_url`
- `computer_call_output.output.image_url`
- `code_interpreter_call.outputs`
- `reasoning.encrypted_content`
- `message.output_text.logprobs`

---

### InputContent

**anyOf:**

- [InputTextContent](#inputtextcontent)
- [InputImageContent](#inputimagecontent)
- [InputFileContent](#inputfilecontent)

---

### InputFidelity

Control how much effort the model will exert to match the style and features, especially facial features, of input
images. This parameter is only supported for `gpt-image-1`. Unsupported for `gpt-image-1-mini`. Supports `high` and
`low`. Defaults to `low`.

**Type:** `string`

**Possible values:**

- `high`
- `low`

---

### InputFileContent

A file input to the model.

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                                      |
|-------------|---------------|----------|--------------------------------------------------|
| `type`      | string (enum) | ✓        | The type of the input item. Always `input_file`. |
| `file_id`   | any           |          |                                                  |
| `filename`  | string        |          | The name of the file to be sent to the model.    |
| `file_url`  | string        |          | The URL of the file to be sent to the model.     |
| `file_data` | string        |          | The content of the file to be sent to the model. |

---

### InputFileContentParam

A file input to the model.

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                                      |
|-------------|---------------|----------|--------------------------------------------------|
| `type`      | string (enum) | ✓        | The type of the input item. Always `input_file`. |
| `file_id`   | any           |          |                                                  |
| `filename`  | any           |          |                                                  |
| `file_data` | any           |          |                                                  |
| `file_url`  | any           |          |                                                  |

---

### InputImageContent

An image input to the model. Learn about [image inputs](https://platform.openai.com/docs/guides/vision).

**Type:** `object`

**Properties:**

| Property    | Type                        | Required | Description                                                                                                 |
|-------------|-----------------------------|----------|-------------------------------------------------------------------------------------------------------------|
| `type`      | string (enum)               | ✓        | The type of the input item. Always `input_image`.                                                           |
| `image_url` | any                         |          |                                                                                                             |
| `file_id`   | any                         |          |                                                                                                             |
| `detail`    | [ImageDetail](#imagedetail) | ✓        | The detail level of the image to be sent to the model. One of `high`, `low`, or `auto`. Defaults to `auto`. |

---

### InputImageContentParamAutoParam

An image input to the model. Learn about [image inputs](https://platform.openai.com/docs/guides/vision)

**Type:** `object`

**Properties:**

| Property    | Type          | Required | Description                                       |
|-------------|---------------|----------|---------------------------------------------------|
| `type`      | string (enum) | ✓        | The type of the input item. Always `input_image`. |
| `image_url` | any           |          |                                                   |
| `file_id`   | any           |          |                                                   |
| `detail`    | any           |          |                                                   |

---

### InputItem

**anyOf:**

- [EasyInputMessage](#easyinputmessage)
- [Item](#item)
- [ItemReferenceParam](#itemreferenceparam)

---

### InputMessage

A message input to the model with a role indicating instruction following
hierarchy. Instructions given with the `developer` or `system` role take
precedence over instructions given with the `user` role.

**Type:** `object`

**Properties:**

| Property  | Type                                                | Required | Description                                                                                                        |
|-----------|-----------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------|
| `type`    | string (enum)                                       |          | The type of the message input. Always set to `message`.                                                            |
| `role`    | string (enum)                                       | ✓        | The role of the message input. One of `user`, `system`, or `developer`.                                            |
| `status`  | string (enum)                                       |          | The status of item. One of `in_progress`, `completed`, or `incomplete`. Populated when items are returned via API. |
| `content` | [InputMessageContentList](#inputmessagecontentlist) | ✓        |                                                                                                                    |

---

### InputMessageContentList

A list of one or many input items to the model, containing different content
types.

**Type:** `array`

---

### InputParam

Text, image, or file inputs to the model, used to generate a response.

Learn more:

- [Text inputs and outputs](https://platform.openai.com/docs/guides/text)
- [Image inputs](https://platform.openai.com/docs/guides/images)
- [File inputs](https://platform.openai.com/docs/guides/pdf-files)
- [Conversation state](https://platform.openai.com/docs/guides/conversation-state)
- [Function calling](https://platform.openai.com/docs/guides/function-calling)

**anyOf:**

- string
- array[[InputItem](#inputitem)]

---

### InputTextContent

A text input to the model.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                      |
|----------|---------------|----------|--------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the input item. Always `input_text`. |
| `text`   | string        | ✓        | The text input to the model.                     |

---

### InputTextContentParam

A text input to the model.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                      |
|----------|---------------|----------|--------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the input item. Always `input_text`. |
| `text`   | string        | ✓        | The text input to the model.                     |

---

### Item

Content item used to generate a response.

**Type:** `object`

**anyOf:**

- [InputMessage](#inputmessage)
- [OutputMessage](#outputmessage)
- [FileSearchToolCall](#filesearchtoolcall)
- [ComputerToolCall](#computertoolcall)
- [ComputerCallOutputItemParam](#computercalloutputitemparam)
- [WebSearchToolCall](#websearchtoolcall)
- [FunctionToolCall](#functiontoolcall)
- [FunctionCallOutputItemParam](#functioncalloutputitemparam)
- [ReasoningItem](#reasoningitem)
- [ImageGenToolCall](#imagegentoolcall)
- [CodeInterpreterToolCall](#codeinterpretertoolcall)
- [LocalShellToolCall](#localshelltoolcall)
- [LocalShellToolCallOutput](#localshelltoolcalloutput)
- [FunctionShellCallItemParam](#functionshellcallitemparam)
- [FunctionShellCallOutputItemParam](#functionshellcalloutputitemparam)
- [ApplyPatchToolCallItemParam](#applypatchtoolcallitemparam)
- [ApplyPatchToolCallOutputItemParam](#applypatchtoolcalloutputitemparam)
- [MCPListTools](#mcplisttools)
- [MCPApprovalRequest](#mcpapprovalrequest)
- [MCPApprovalResponse](#mcpapprovalresponse)
- [MCPToolCall](#mcptoolcall)
- [CustomToolCallOutput](#customtoolcalloutput)
- [CustomToolCall](#customtoolcall)

---

### ItemReferenceParam

An internal identifier for an item to reference.

**Type:** `object`

**Properties:**

| Property | Type   | Required | Description                      |
|----------|--------|----------|----------------------------------|
| `type`   | any    |          |                                  |
| `id`     | string | ✓        | The ID of the item to reference. |

---

### KeyPressAction

A collection of keypresses the model would like to perform.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                                                          |
|----------|---------------|----------|----------------------------------------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Specifies the event type. For a keypress action, this property is always set to `keypress`.                          |
| `keys`   | array[string] | ✓        | The combination of keys the model is requesting to be pressed. This is an array of strings, each representing a key. |

---

### LocalShellCallStatus

**Type:** `string`

**Possible values:**

- `in_progress`
- `completed`
- `incomplete`

---

### LocalShellExecAction

Execute a shell command on the server.

**Type:** `object`

**Properties:**

| Property            | Type                   | Required | Description                                        |
|---------------------|------------------------|----------|----------------------------------------------------|
| `type`              | string (enum)          | ✓        | The type of the local shell action. Always `exec`. |
| `command`           | array[string]          | ✓        | The command to run.                                |
| `timeout_ms`        | any                    |          |                                                    |
| `working_directory` | any                    |          |                                                    |
| `env`               | object (map of string) | ✓        | Environment variables to set for the command.      |
| `user`              | any                    |          |                                                    |

---

### LocalShellToolCall

A tool call to run a command on the local shell.

**Type:** `object`

**Properties:**

| Property  | Type                                          | Required | Description                                                        |
|-----------|-----------------------------------------------|----------|--------------------------------------------------------------------|
| `type`    | string (enum)                                 | ✓        | The type of the local shell call. Always `local_shell_call`.       |
| `id`      | string                                        | ✓        | The unique ID of the local shell call.                             |
| `call_id` | string                                        | ✓        | The unique ID of the local shell tool call generated by the model. |
| `action`  | [LocalShellExecAction](#localshellexecaction) | ✓        |                                                                    |
| `status`  | string (enum)                                 | ✓        | The status of the local shell call.                                |

---

### LocalShellToolCallOutput

The output of a local shell tool call.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                     |
|----------|---------------|----------|---------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the local shell tool call output. Always `local_shell_call_output`. |
| `id`     | string        | ✓        | The unique ID of the local shell tool call generated by the model.              |
| `output` | string        | ✓        | A JSON string of the output of the local shell tool call.                       |
| `status` | any           |          |                                                                                 |

---

### LocalShellToolParam

A tool that allows the model to execute shell commands in a local environment.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                             |
|----------|---------------|----------|---------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the local shell tool. Always `local_shell`. |

---

### LogProb

The log probability of a token.

**Type:** `object`

**Properties:**

| Property       | Type                             | Required | Description |
|----------------|----------------------------------|----------|-------------|
| `token`        | string                           | ✓        |             |
| `logprob`      | number                           | ✓        |             |
| `bytes`        | array[integer]                   | ✓        |             |
| `top_logprobs` | array[[TopLogProb](#toplogprob)] | ✓        |             |

---

### MCPApprovalRequest

A request for human approval of a tool invocation.

**Type:** `object`

**Properties:**

| Property       | Type          | Required | Description                                          |
|----------------|---------------|----------|------------------------------------------------------|
| `type`         | string (enum) | ✓        | The type of the item. Always `mcp_approval_request`. |
| `id`           | string        | ✓        | The unique ID of the approval request.               |
| `server_label` | string        | ✓        | The label of the MCP server making the request.      |
| `name`         | string        | ✓        | The name of the tool to run.                         |
| `arguments`    | string        | ✓        | A JSON string of arguments for the tool.             |

---

### MCPApprovalResponse

A response to an MCP approval request.

**Type:** `object`

**Properties:**

| Property              | Type          | Required | Description                                           |
|-----------------------|---------------|----------|-------------------------------------------------------|
| `type`                | string (enum) | ✓        | The type of the item. Always `mcp_approval_response`. |
| `id`                  | any           |          |                                                       |
| `approval_request_id` | string        | ✓        | The ID of the approval request being answered.        |
| `approve`             | boolean       | ✓        | Whether the request was approved.                     |
| `reason`              | any           |          |                                                       |

---

### MCPListTools

A list of tools available on an MCP server.

**Type:** `object`

**Properties:**

| Property       | Type                                         | Required | Description                                    |
|----------------|----------------------------------------------|----------|------------------------------------------------|
| `type`         | string (enum)                                | ✓        | The type of the item. Always `mcp_list_tools`. |
| `id`           | string                                       | ✓        | The unique ID of the list.                     |
| `server_label` | string                                       | ✓        | The label of the MCP server.                   |
| `tools`        | array[[MCPListToolsTool](#mcplisttoolstool)] | ✓        | The tools available on the server.             |
| `error`        | any                                          |          |                                                |

---

### MCPListToolsTool

A tool available on an MCP server.

**Type:** `object`

**Properties:**

| Property       | Type   | Required | Description                                  |
|----------------|--------|----------|----------------------------------------------|
| `name`         | string | ✓        | The name of the tool.                        |
| `description`  | any    |          |                                              |
| `input_schema` | object | ✓        | The JSON schema describing the tool's input. |
| `annotations`  | any    |          |                                              |

---

### MCPTool

Give the model access to additional tools via remote Model Context Protocol
(MCP) servers. [Learn more about MCP](https://platform.openai.com/docs/guides/tools-remote-mcp).

**Type:** `object`

**Properties:**

| Property             | Type          | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|----------------------|---------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`               | string (enum) | ✓        | The type of the MCP tool. Always `mcp`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `server_label`       | string        | ✓        | A label for this MCP server, used to identify it in tool calls.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `server_url`         | string        |          | The URL for the MCP server. One of `server_url` or `connector_id` must be provided.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `connector_id`       | string (enum) |          | Identifier for service connectors, like those available in ChatGPT. One of `server_url` or `connector_id` must be provided. Learn more about service connectors [here](https://platform.openai.com/docs/guides/tools-remote-mcp#connectors).  Currently supported `connector_id` values are:  - Dropbox: `connector_dropbox` - Gmail: `connector_gmail` - Google Calendar: `connector_googlecalendar` - Google Drive: `connector_googledrive` - Microsoft Teams: `connector_microsoftteams` - Outlook Calendar: `connector_outlookcalendar` - Outlook Email: `connector_outlookemail` - SharePoint: `connector_sharepoint` |
| `authorization`      | string        |          | An OAuth access token that can be used with a remote MCP server, either with a custom MCP server URL or a service connector. Your application must handle the OAuth authorization flow and provide the token here.                                                                                                                                                                                                                                                                                                                                                                                                         |
| `server_description` | string        |          | Optional description of the MCP server, used to provide more context.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `headers`            | any           |          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `allowed_tools`      | any           |          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `require_approval`   | any           |          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

---

### MCPToolCall

An invocation of a tool on an MCP server.

**Type:** `object`

**Properties:**

| Property              | Type                                    | Required | Description                                                                                           |
|-----------------------|-----------------------------------------|----------|-------------------------------------------------------------------------------------------------------|
| `type`                | string (enum)                           | ✓        | The type of the item. Always `mcp_call`.                                                              |
| `id`                  | string                                  | ✓        | The unique ID of the tool call.                                                                       |
| `server_label`        | string                                  | ✓        | The label of the MCP server running the tool.                                                         |
| `name`                | string                                  | ✓        | The name of the tool that was run.                                                                    |
| `arguments`           | string                                  | ✓        | A JSON string of the arguments passed to the tool.                                                    |
| `output`              | any                                     |          |                                                                                                       |
| `error`               | any                                     |          |                                                                                                       |
| `status`              | [MCPToolCallStatus](#mcptoolcallstatus) |          | The status of the tool call. One of `in_progress`, `completed`, `incomplete`, `calling`, or `failed`. |
| `approval_request_id` | any                                     |          |                                                                                                       |

---

### MCPToolCallStatus

**Type:** `string`

**Possible values:**

- `in_progress`
- `completed`
- `incomplete`
- `calling`
- `failed`

---

### MCPToolFilter

A filter object to specify which tools are allowed.

**Type:** `object`

**Properties:**

| Property     | Type          | Required | Description                                                                                                                                                                                                                                  |
|--------------|---------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tool_names` | array[string] |          | List of allowed tool names.                                                                                                                                                                                                                  |
| `read_only`  | boolean       |          | Indicates whether or not a tool modifies data or is read-only. If an MCP server is [annotated with `readOnlyHint`](https://modelcontextprotocol.io/specification/2025-06-18/schema#toolannotations-readonlyhint), it will match this filter. |

---

### Metadata

**anyOf:**

- object (map of string)
- null

---

### ModelIdsResponses

**anyOf:**

- [ModelIdsShared](#modelidsshared)
- string (enum)

---

### ModelIdsShared

**anyOf:**

- string
- [ChatModel](#chatmodel)

---

### ModelResponseProperties

**Type:** `object`

**Properties:**

| Property                 | Type                        | Required | Description                                                                                                                                                                                                                                                                                                                                                                                            |
|--------------------------|-----------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `metadata`               | [Metadata](#metadata)       |          |                                                                                                                                                                                                                                                                                                                                                                                                        |
| `top_logprobs`           | any                         |          |                                                                                                                                                                                                                                                                                                                                                                                                        |
| `temperature`            | any                         |          |                                                                                                                                                                                                                                                                                                                                                                                                        |
| `top_p`                  | any                         |          |                                                                                                                                                                                                                                                                                                                                                                                                        |
| `user`                   | string                      |          | This field is being replaced by `safety_identifier` and `prompt_cache_key`. Use `prompt_cache_key` instead to maintain caching optimizations. A stable identifier for your end-users. Used to boost cache hit rates by better bucketing similar requests and  to help OpenAI detect and prevent abuse. [Learn more](https://platform.openai.com/docs/guides/safety-best-practices#safety-identifiers). |
| `safety_identifier`      | string                      |          | A stable identifier used to help detect users of your application that may be violating OpenAI's usage policies. The IDs should be a string that uniquely identifies each user. We recommend hashing their username or email address, in order to avoid sending us any identifying information. [Learn more](https://platform.openai.com/docs/guides/safety-best-practices#safety-identifiers).        |
| `prompt_cache_key`       | string                      |          | Used by OpenAI to cache responses for similar requests to optimize your cache hit rates. Replaces the `user` field. [Learn more](https://platform.openai.com/docs/guides/prompt-caching).                                                                                                                                                                                                              |
| `service_tier`           | [ServiceTier](#servicetier) |          |                                                                                                                                                                                                                                                                                                                                                                                                        |
| `prompt_cache_retention` | any                         |          |                                                                                                                                                                                                                                                                                                                                                                                                        |

---

### Move

A mouse move action.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                          |
|----------|---------------|----------|--------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Specifies the event type. For a move action, this property is  always set to `move`. |
| `x`      | integer       | ✓        | The x-coordinate to move to.                                                         |
| `y`      | integer       | ✓        | The y-coordinate to move to.                                                         |

---

### OutputContent

**anyOf:**

- [OutputTextContent](#outputtextcontent)
- [RefusalContent](#refusalcontent)
- [ReasoningTextContent](#reasoningtextcontent)

---

### OutputItem

**anyOf:**

- [OutputMessage](#outputmessage)
- [FileSearchToolCall](#filesearchtoolcall)
- [FunctionToolCall](#functiontoolcall)
- [WebSearchToolCall](#websearchtoolcall)
- [ComputerToolCall](#computertoolcall)
- [ReasoningItem](#reasoningitem)
- [ImageGenToolCall](#imagegentoolcall)
- [CodeInterpreterToolCall](#codeinterpretertoolcall)
- [LocalShellToolCall](#localshelltoolcall)
- [FunctionShellCall](#functionshellcall)
- [FunctionShellCallOutput](#functionshellcalloutput)
- [ApplyPatchToolCall](#applypatchtoolcall)
- [ApplyPatchToolCallOutput](#applypatchtoolcalloutput)
- [MCPToolCall](#mcptoolcall)
- [MCPListTools](#mcplisttools)
- [MCPApprovalRequest](#mcpapprovalrequest)
- [CustomToolCall](#customtoolcall)

---

### OutputMessage

An output message from the model.

**Type:** `object`

**Properties:**

| Property  | Type                                                 | Required | Description                                                                                                                           |
|-----------|------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------|
| `id`      | string                                               | ✓        | The unique ID of the output message.                                                                                                  |
| `type`    | string (enum)                                        | ✓        | The type of the output message. Always `message`.                                                                                     |
| `role`    | string (enum)                                        | ✓        | The role of the output message. Always `assistant`.                                                                                   |
| `content` | array[[OutputMessageContent](#outputmessagecontent)] | ✓        | The content of the output message.                                                                                                    |
| `status`  | string (enum)                                        | ✓        | The status of the message input. One of `in_progress`, `completed`, or `incomplete`. Populated when input items are returned via API. |

---

### OutputMessageContent

**anyOf:**

- [OutputTextContent](#outputtextcontent)
- [RefusalContent](#refusalcontent)

---

### OutputTextContent

A text output from the model.

**Type:** `object`

**Properties:**

| Property      | Type                             | Required | Description                                        |
|---------------|----------------------------------|----------|----------------------------------------------------|
| `type`        | string (enum)                    | ✓        | The type of the output text. Always `output_text`. |
| `text`        | string                           | ✓        | The text output from the model.                    |
| `annotations` | array[[Annotation](#annotation)] | ✓        | The annotations of the text output.                |
| `logprobs`    | array[[LogProb](#logprob)]       |          |                                                    |

---

### Prompt

**anyOf:**

- object
- null

---

### RankerVersionType

**Type:** `string`

**Possible values:**

- `auto`
- `default-2024-11-15`

---

### RankingOptions

**Type:** `object`

**Properties:**

| Property          | Type                                        | Required | Description                                                                                                                                                                 |
|-------------------|---------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ranker`          | [RankerVersionType](#rankerversiontype)     |          | The ranker to use for the file search.                                                                                                                                      |
| `score_threshold` | number                                      |          | The score threshold for the file search, a number between 0 and 1. Numbers closer to 1 will attempt to return only the most relevant results, but may return fewer results. |
| `hybrid_search`   | [HybridSearchOptions](#hybridsearchoptions) |          | Weights that control how reciprocal rank fusion balances semantic embedding matches versus sparse keyword matches when hybrid search is enabled.                            |

---

### Reasoning

**gpt-5 and o-series models only**

Configuration options for
[reasoning models](https://platform.openai.com/docs/guides/reasoning).

**Type:** `object`

**Properties:**

| Property           | Type                                | Required | Description |
|--------------------|-------------------------------------|----------|-------------|
| `effort`           | [ReasoningEffort](#reasoningeffort) |          |             |
| `summary`          | any                                 |          |             |
| `generate_summary` | any                                 |          |             |

---

### ReasoningEffort

**anyOf:**

- string (enum)
- null

---

### ReasoningItem

A description of the chain of thought used by a reasoning model while generating
a response. Be sure to include these items in your `input` to the Responses API
for subsequent turns of a conversation if you are manually
[managing context](https://platform.openai.com/docs/guides/conversation-state).

**Type:** `object`

**Properties:**

| Property            | Type                                                 | Required | Description                                                                                                            |
|---------------------|------------------------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------|
| `type`              | string (enum)                                        | ✓        | The type of the object. Always `reasoning`.                                                                            |
| `id`                | string                                               | ✓        | The unique identifier of the reasoning content.                                                                        |
| `encrypted_content` | any                                                  |          |                                                                                                                        |
| `summary`           | array[[Summary](#summary)]                           | ✓        | Reasoning summary content.                                                                                             |
| `content`           | array[[ReasoningTextContent](#reasoningtextcontent)] |          | Reasoning text content.                                                                                                |
| `status`            | string (enum)                                        |          | The status of the item. One of `in_progress`, `completed`, or `incomplete`. Populated when items are returned via API. |

---

### ReasoningTextContent

Reasoning text from the model.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                              |
|----------|---------------|----------|----------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the reasoning text. Always `reasoning_text`. |
| `text`   | string        | ✓        | The reasoning text from the model.                       |

---

### RefusalContent

A refusal from the model.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                |
|-----------|---------------|----------|--------------------------------------------|
| `type`    | string (enum) | ✓        | The type of the refusal. Always `refusal`. |
| `refusal` | string        | ✓        | The refusal explanation from the model.    |

---

### Response

**allOf:**

- [ModelResponseProperties](#modelresponseproperties)
- [ResponseProperties](#responseproperties)
- object

---

### ResponseAudioDeltaEvent

Emitted when there is a partial audio response.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                              |
|-------------------|---------------|----------|----------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.audio.delta`.    |
| `sequence_number` | integer       | ✓        | A sequence number for this chunk of the stream response. |
| `delta`           | string        | ✓        | A chunk of Base64 encoded response audio bytes.          |

---

### ResponseAudioDoneEvent

Emitted when the audio response is complete.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                          |
|-------------------|---------------|----------|------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.audio.done`. |
| `sequence_number` | integer       | ✓        | The sequence number of the delta.                    |

---

### ResponseAudioTranscriptDeltaEvent

Emitted when there is a partial transcript of audio.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                      |
|-------------------|---------------|----------|------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.audio.transcript.delta`. |
| `delta`           | string        | ✓        | The partial transcript of the audio response.                    |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                               |

---

### ResponseAudioTranscriptDoneEvent

Emitted when the full audio transcript is completed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                     |
|-------------------|---------------|----------|-----------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.audio.transcript.done`. |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                              |

---

### ResponseCodeInterpreterCallCodeDeltaEvent

Emitted when a partial code snippet is streamed by the code interpreter.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                        |
|-------------------|---------------|----------|------------------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.code_interpreter_call_code.delta`.         |
| `output_index`    | integer       | ✓        | The index of the output item in the response for which the code is being streamed. |
| `item_id`         | string        | ✓        | The unique identifier of the code interpreter tool call item.                      |
| `delta`           | string        | ✓        | The partial code snippet being streamed by the code interpreter.                   |
| `sequence_number` | integer       | ✓        | The sequence number of this event, used to order streaming events.                 |

---

### ResponseCodeInterpreterCallCodeDoneEvent

Emitted when the code snippet is finalized by the code interpreter.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                   |
|-------------------|---------------|----------|-------------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.code_interpreter_call_code.done`.     |
| `output_index`    | integer       | ✓        | The index of the output item in the response for which the code is finalized. |
| `item_id`         | string        | ✓        | The unique identifier of the code interpreter tool call item.                 |
| `code`            | string        | ✓        | The final code snippet output by the code interpreter.                        |
| `sequence_number` | integer       | ✓        | The sequence number of this event, used to order streaming events.            |

---

### ResponseCodeInterpreterCallCompletedEvent

Emitted when the code interpreter call is completed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                                    |
|-------------------|---------------|----------|------------------------------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.code_interpreter_call.completed`.                      |
| `output_index`    | integer       | ✓        | The index of the output item in the response for which the code interpreter call is completed. |
| `item_id`         | string        | ✓        | The unique identifier of the code interpreter tool call item.                                  |
| `sequence_number` | integer       | ✓        | The sequence number of this event, used to order streaming events.                             |

---

### ResponseCodeInterpreterCallInProgressEvent

Emitted when a code interpreter call is in progress.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                                      |
|-------------------|---------------|----------|--------------------------------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.code_interpreter_call.in_progress`.                      |
| `output_index`    | integer       | ✓        | The index of the output item in the response for which the code interpreter call is in progress. |
| `item_id`         | string        | ✓        | The unique identifier of the code interpreter tool call item.                                    |
| `sequence_number` | integer       | ✓        | The sequence number of this event, used to order streaming events.                               |

---

### ResponseCodeInterpreterCallInterpretingEvent

Emitted when the code interpreter is actively interpreting the code snippet.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                                       |
|-------------------|---------------|----------|---------------------------------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.code_interpreter_call.interpreting`.                      |
| `output_index`    | integer       | ✓        | The index of the output item in the response for which the code interpreter is interpreting code. |
| `item_id`         | string        | ✓        | The unique identifier of the code interpreter tool call item.                                     |
| `sequence_number` | integer       | ✓        | The sequence number of this event, used to order streaming events.                                |

---

### ResponseCompletedEvent

Emitted when the model response is complete.

**Type:** `object`

**Properties:**

| Property          | Type                  | Required | Description                                         |
|-------------------|-----------------------|----------|-----------------------------------------------------|
| `type`            | string (enum)         | ✓        | The type of the event. Always `response.completed`. |
| `response`        | [Response](#response) | ✓        | Properties of the completed response.               |
| `sequence_number` | integer               | ✓        | The sequence number for this event.                 |

---

### ResponseContentPartAddedEvent

Emitted when a new content part is added.

**Type:** `object`

**Properties:**

| Property          | Type                            | Required | Description                                                      |
|-------------------|---------------------------------|----------|------------------------------------------------------------------|
| `type`            | string (enum)                   | ✓        | The type of the event. Always `response.content_part.added`.     |
| `item_id`         | string                          | ✓        | The ID of the output item that the content part was added to.    |
| `output_index`    | integer                         | ✓        | The index of the output item that the content part was added to. |
| `content_index`   | integer                         | ✓        | The index of the content part that was added.                    |
| `part`            | [OutputContent](#outputcontent) | ✓        | The content part that was added.                                 |
| `sequence_number` | integer                         | ✓        | The sequence number of this event.                               |

---

### ResponseContentPartDoneEvent

Emitted when a content part is done.

**Type:** `object`

**Properties:**

| Property          | Type                            | Required | Description                                                      |
|-------------------|---------------------------------|----------|------------------------------------------------------------------|
| `type`            | string (enum)                   | ✓        | The type of the event. Always `response.content_part.done`.      |
| `item_id`         | string                          | ✓        | The ID of the output item that the content part was added to.    |
| `output_index`    | integer                         | ✓        | The index of the output item that the content part was added to. |
| `content_index`   | integer                         | ✓        | The index of the content part that is done.                      |
| `sequence_number` | integer                         | ✓        | The sequence number of this event.                               |
| `part`            | [OutputContent](#outputcontent) | ✓        | The content part that is done.                                   |

---

### ResponseCreatedEvent

An event that is emitted when a response is created.

**Type:** `object`

**Properties:**

| Property          | Type                  | Required | Description                                       |
|-------------------|-----------------------|----------|---------------------------------------------------|
| `type`            | string (enum)         | ✓        | The type of the event. Always `response.created`. |
| `response`        | [Response](#response) | ✓        | The response that was created.                    |
| `sequence_number` | integer               | ✓        | The sequence number for this event.               |

---

### ResponseCustomToolCallInputDeltaEvent

Event representing a delta (partial update) to the input of a custom tool call.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                    |
|-------------------|---------------|----------|----------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The event type identifier.                                     |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                             |
| `output_index`    | integer       | ✓        | The index of the output this delta applies to.                 |
| `item_id`         | string        | ✓        | Unique identifier for the API item associated with this event. |
| `delta`           | string        | ✓        | The incremental input data (delta) for the custom tool call.   |

---

### ResponseCustomToolCallInputDoneEvent

Event indicating that input for a custom tool call is complete.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                    |
|-------------------|---------------|----------|----------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The event type identifier.                                     |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                             |
| `output_index`    | integer       | ✓        | The index of the output this event applies to.                 |
| `item_id`         | string        | ✓        | Unique identifier for the API item associated with this event. |
| `input`           | string        | ✓        | The complete input data for the custom tool call.              |

---

### ResponseError

**anyOf:**

- object
- null

---

### ResponseErrorCode

The error code for the response.

**Type:** `string`

**Possible values:**

- `server_error`
- `rate_limit_exceeded`
- `invalid_prompt`
- `vector_store_timeout`
- `invalid_image`
- `invalid_image_format`
- `invalid_base64_image`
- `invalid_image_url`
- `image_too_large`
- `image_too_small`
- `image_parse_error`
- `image_content_policy_violation`
- `invalid_image_mode`
- `image_file_too_large`
- `unsupported_image_media_type`
- `empty_image_file`
- `failed_to_download_image`
- `image_file_not_found`

---

### ResponseErrorEvent

Emitted when an error occurs.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                            |
|-------------------|---------------|----------|----------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `error`. |
| `code`            | any           | ✓        |                                        |
| `message`         | string        | ✓        | The error message.                     |
| `param`           | any           | ✓        |                                        |
| `sequence_number` | integer       | ✓        | The sequence number of this event.     |

---

### ResponseFailedEvent

An event that is emitted when a response fails.

**Type:** `object`

**Properties:**

| Property          | Type                  | Required | Description                                      |
|-------------------|-----------------------|----------|--------------------------------------------------|
| `type`            | string (enum)         | ✓        | The type of the event. Always `response.failed`. |
| `sequence_number` | integer               | ✓        | The sequence number of this event.               |
| `response`        | [Response](#response) | ✓        | The response that failed.                        |

---

### ResponseFileSearchCallCompletedEvent

Emitted when a file search call is completed (results found).

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                          |
|-------------------|---------------|----------|----------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.file_search_call.completed`. |
| `output_index`    | integer       | ✓        | The index of the output item that the file search call is initiated. |
| `item_id`         | string        | ✓        | The ID of the output item that the file search call is initiated.    |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                   |

---

### ResponseFileSearchCallInProgressEvent

Emitted when a file search call is initiated.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                            |
|-------------------|---------------|----------|------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.file_search_call.in_progress`. |
| `output_index`    | integer       | ✓        | The index of the output item that the file search call is initiated.   |
| `item_id`         | string        | ✓        | The ID of the output item that the file search call is initiated.      |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                     |

---

### ResponseFileSearchCallSearchingEvent

Emitted when a file search is currently searching.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                          |
|-------------------|---------------|----------|----------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.file_search_call.searching`. |
| `output_index`    | integer       | ✓        | The index of the output item that the file search call is searching. |
| `item_id`         | string        | ✓        | The ID of the output item that the file search call is initiated.    |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                   |

---

### ResponseFormatJsonObject

JSON object response format. An older method of generating JSON responses.
Using `json_schema` is recommended for models that support it. Note that the
model will not generate JSON without a system or user message instructing it
to do so.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                      |
|----------|---------------|----------|------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of response format being defined. Always `json_object`. |

---

### ResponseFormatJsonSchemaSchema

The schema for the response format, described as a JSON Schema object.
Learn how to build JSON schemas [here](https://json-schema.org/).

**Type:** `object`

---

### ResponseFormatText

Default response format. Used to generate text responses.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                               |
|----------|---------------|----------|-----------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of response format being defined. Always `text`. |

---

### ResponseFunctionCallArgumentsDeltaEvent

Emitted when there is a partial function-call arguments delta.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                      |
|-------------------|---------------|----------|----------------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.function_call_arguments.delta`.          |
| `item_id`         | string        | ✓        | The ID of the output item that the function-call arguments delta is added to.    |
| `output_index`    | integer       | ✓        | The index of the output item that the function-call arguments delta is added to. |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                               |
| `delta`           | string        | ✓        | The function-call arguments delta that is added.                                 |

---

### ResponseFunctionCallArgumentsDoneEvent

Emitted when function-call arguments are finalized.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                               |
|-------------------|---------------|----------|-------------------------------------------|
| `type`            | string (enum) | ✓        |                                           |
| `item_id`         | string        | ✓        | The ID of the item.                       |
| `name`            | string        | ✓        | The name of the function that was called. |
| `output_index`    | integer       | ✓        | The index of the output item.             |
| `sequence_number` | integer       | ✓        | The sequence number of this event.        |
| `arguments`       | string        | ✓        | The function-call arguments.              |

---

### ResponseImageGenCallCompletedEvent

Emitted when an image generation tool call has completed and the final image is available.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                               |
|-------------------|---------------|----------|---------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.image_generation_call.completed'. |
| `output_index`    | integer       | ✓        | The index of the output item in the response's output array.              |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                        |
| `item_id`         | string        | ✓        | The unique identifier of the image generation item being processed.       |

---

### ResponseImageGenCallGeneratingEvent

Emitted when an image generation tool call is actively generating an image (intermediate state).

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                |
|-------------------|---------------|----------|----------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.image_generation_call.generating'. |
| `output_index`    | integer       | ✓        | The index of the output item in the response's output array.               |
| `item_id`         | string        | ✓        | The unique identifier of the image generation item being processed.        |
| `sequence_number` | integer       | ✓        | The sequence number of the image generation item being processed.          |

---

### ResponseImageGenCallInProgressEvent

Emitted when an image generation tool call is in progress.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                 |
|-------------------|---------------|----------|-----------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.image_generation_call.in_progress'. |
| `output_index`    | integer       | ✓        | The index of the output item in the response's output array.                |
| `item_id`         | string        | ✓        | The unique identifier of the image generation item being processed.         |
| `sequence_number` | integer       | ✓        | The sequence number of the image generation item being processed.           |

---

### ResponseImageGenCallPartialImageEvent

Emitted when a partial image is available during image generation streaming.

**Type:** `object`

**Properties:**

| Property              | Type          | Required | Description                                                                                 |
|-----------------------|---------------|----------|---------------------------------------------------------------------------------------------|
| `type`                | string (enum) | ✓        | The type of the event. Always 'response.image_generation_call.partial_image'.               |
| `output_index`        | integer       | ✓        | The index of the output item in the response's output array.                                |
| `item_id`             | string        | ✓        | The unique identifier of the image generation item being processed.                         |
| `sequence_number`     | integer       | ✓        | The sequence number of the image generation item being processed.                           |
| `partial_image_index` | integer       | ✓        | 0-based index for the partial image (backend is 1-based, but this is 0-based for the user). |
| `partial_image_b64`   | string        | ✓        | Base64-encoded partial image data, suitable for rendering as an image.                      |

---

### ResponseInProgressEvent

Emitted when the response is in progress.

**Type:** `object`

**Properties:**

| Property          | Type                  | Required | Description                                           |
|-------------------|-----------------------|----------|-------------------------------------------------------|
| `type`            | string (enum)         | ✓        | The type of the event. Always `response.in_progress`. |
| `response`        | [Response](#response) | ✓        | The response that is in progress.                     |
| `sequence_number` | integer               | ✓        | The sequence number of this event.                    |

---

### ResponseIncompleteEvent

An event that is emitted when a response finishes as incomplete.

**Type:** `object`

**Properties:**

| Property          | Type                  | Required | Description                                          |
|-------------------|-----------------------|----------|------------------------------------------------------|
| `type`            | string (enum)         | ✓        | The type of the event. Always `response.incomplete`. |
| `response`        | [Response](#response) | ✓        | The response that was incomplete.                    |
| `sequence_number` | integer               | ✓        | The sequence number of this event.                   |

---

### ResponseLogProb

A logprob is the logarithmic probability that the model assigns to producing
a particular token at a given position in the sequence. Less-negative (higher)
logprob values indicate greater model confidence in that token choice.

**Type:** `object`

**Properties:**

| Property       | Type          | Required | Description                                           |
|----------------|---------------|----------|-------------------------------------------------------|
| `token`        | string        | ✓        | A possible text token.                                |
| `logprob`      | number        | ✓        | The log probability of this token.                    |
| `top_logprobs` | array[object] |          | The log probability of the top 20 most likely tokens. |

---

### ResponseMCPCallArgumentsDeltaEvent

Emitted when there is a delta (partial update) to the arguments of an MCP tool call.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                         |
|-------------------|---------------|----------|-------------------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_call_arguments.delta'.                  |
| `output_index`    | integer       | ✓        | The index of the output item in the response's output array.                        |
| `item_id`         | string        | ✓        | The unique identifier of the MCP tool call item being processed.                    |
| `delta`           | string        | ✓        | A JSON string containing the partial update to the arguments for the MCP tool call. |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                                  |

---

### ResponseMCPCallArgumentsDoneEvent

Emitted when the arguments for an MCP tool call are finalized.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                             |
|-------------------|---------------|----------|-------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_call_arguments.done'.       |
| `output_index`    | integer       | ✓        | The index of the output item in the response's output array.            |
| `item_id`         | string        | ✓        | The unique identifier of the MCP tool call item being processed.        |
| `arguments`       | string        | ✓        | A JSON string containing the finalized arguments for the MCP tool call. |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                      |

---

### ResponseMCPCallCompletedEvent

Emitted when an MCP tool call has completed successfully.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                  |
|-------------------|---------------|----------|--------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_call.completed'. |
| `item_id`         | string        | ✓        | The ID of the MCP tool call item that completed.             |
| `output_index`    | integer       | ✓        | The index of the output item that completed.                 |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                           |

---

### ResponseMCPCallFailedEvent

Emitted when an MCP tool call has failed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                               |
|-------------------|---------------|----------|-----------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_call.failed'. |
| `item_id`         | string        | ✓        | The ID of the MCP tool call item that failed.             |
| `output_index`    | integer       | ✓        | The index of the output item that failed.                 |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                        |

---

### ResponseMCPCallInProgressEvent

Emitted when an MCP tool call is in progress.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                      |
|-------------------|---------------|----------|------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_call.in_progress'.   |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                               |
| `output_index`    | integer       | ✓        | The index of the output item in the response's output array.     |
| `item_id`         | string        | ✓        | The unique identifier of the MCP tool call item being processed. |

---

### ResponseMCPListToolsCompletedEvent

Emitted when the list of available MCP tools has been successfully retrieved.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                        |
|-------------------|---------------|----------|--------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_list_tools.completed'. |
| `item_id`         | string        | ✓        | The ID of the MCP tool call item that produced this output.        |
| `output_index`    | integer       | ✓        | The index of the output item that was processed.                   |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                 |

---

### ResponseMCPListToolsFailedEvent

Emitted when the attempt to list available MCP tools has failed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                     |
|-------------------|---------------|----------|-----------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_list_tools.failed'. |
| `item_id`         | string        | ✓        | The ID of the MCP tool call item that failed.                   |
| `output_index`    | integer       | ✓        | The index of the output item that failed.                       |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                              |

---

### ResponseMCPListToolsInProgressEvent

Emitted when the system is in the process of retrieving the list of available MCP tools.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                          |
|-------------------|---------------|----------|----------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always 'response.mcp_list_tools.in_progress'. |
| `item_id`         | string        | ✓        | The ID of the MCP tool call item that is being processed.            |
| `output_index`    | integer       | ✓        | The index of the output item that is being processed.                |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                   |

---

### ResponseOutputItemAddedEvent

Emitted when a new output item is added.

**Type:** `object`

**Properties:**

| Property          | Type                      | Required | Description                                                 |
|-------------------|---------------------------|----------|-------------------------------------------------------------|
| `type`            | string (enum)             | ✓        | The type of the event. Always `response.output_item.added`. |
| `output_index`    | integer                   | ✓        | The index of the output item that was added.                |
| `sequence_number` | integer                   | ✓        | The sequence number of this event.                          |
| `item`            | [OutputItem](#outputitem) | ✓        | The output item that was added.                             |

---

### ResponseOutputItemDoneEvent

Emitted when an output item is marked done.

**Type:** `object`

**Properties:**

| Property          | Type                      | Required | Description                                                |
|-------------------|---------------------------|----------|------------------------------------------------------------|
| `type`            | string (enum)             | ✓        | The type of the event. Always `response.output_item.done`. |
| `output_index`    | integer                   | ✓        | The index of the output item that was marked done.         |
| `sequence_number` | integer                   | ✓        | The sequence number of this event.                         |
| `item`            | [OutputItem](#outputitem) | ✓        | The output item that was marked done.                      |

---

### ResponseOutputTextAnnotationAddedEvent

Emitted when an annotation is added to output text content.

**Type:** `object`

**Properties:**

| Property           | Type          | Required | Description                                                               |
|--------------------|---------------|----------|---------------------------------------------------------------------------|
| `type`             | string (enum) | ✓        | The type of the event. Always 'response.output_text.annotation.added'.    |
| `item_id`          | string        | ✓        | The unique identifier of the item to which the annotation is being added. |
| `output_index`     | integer       | ✓        | The index of the output item in the response's output array.              |
| `content_index`    | integer       | ✓        | The index of the content part within the output item.                     |
| `annotation_index` | integer       | ✓        | The index of the annotation within the content part.                      |
| `sequence_number`  | integer       | ✓        | The sequence number of this event.                                        |
| `annotation`       | object        | ✓        | The annotation object being added. (See annotation schema for details.)   |

---

### ResponsePromptVariables

**anyOf:**

- object (map of any)
- null

---

### ResponseProperties

**Type:** `object`

**Properties:**

| Property               | Type                                    | Required | Description                                                                                                                                                                                                                                                                                 |
|------------------------|-----------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `previous_response_id` | any                                     |          |                                                                                                                                                                                                                                                                                             |
| `model`                | [ModelIdsResponses](#modelidsresponses) |          | Model ID used to generate the response, like `gpt-4o` or `o3`. OpenAI offers a wide range of models with different capabilities, performance characteristics, and price points. Refer to the [model guide](https://platform.openai.com/docs/models) to browse and compare available models. |
| `reasoning`            | any                                     |          |                                                                                                                                                                                                                                                                                             |
| `background`           | any                                     |          |                                                                                                                                                                                                                                                                                             |
| `max_output_tokens`    | any                                     |          |                                                                                                                                                                                                                                                                                             |
| `max_tool_calls`       | any                                     |          |                                                                                                                                                                                                                                                                                             |
| `text`                 | [ResponseTextParam](#responsetextparam) |          |                                                                                                                                                                                                                                                                                             |
| `tools`                | [ToolsArray](#toolsarray)               |          |                                                                                                                                                                                                                                                                                             |
| `tool_choice`          | [ToolChoiceParam](#toolchoiceparam)     |          |                                                                                                                                                                                                                                                                                             |
| `prompt`               | [Prompt](#prompt)                       |          |                                                                                                                                                                                                                                                                                             |
| `truncation`           | any                                     |          |                                                                                                                                                                                                                                                                                             |

---

### ResponseQueuedEvent

Emitted when a response is queued and waiting to be processed.

**Type:** `object`

**Properties:**

| Property          | Type                  | Required | Description                                      |
|-------------------|-----------------------|----------|--------------------------------------------------|
| `type`            | string (enum)         | ✓        | The type of the event. Always 'response.queued'. |
| `response`        | [Response](#response) | ✓        | The full response object that is queued.         |
| `sequence_number` | integer               | ✓        | The sequence number for this event.              |

---

### ResponseReasoningSummaryPartAddedEvent

Emitted when a new reasoning summary part is added.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                            |
|-------------------|---------------|----------|------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.reasoning_summary_part.added`. |
| `item_id`         | string        | ✓        | The ID of the item this summary part is associated with.               |
| `output_index`    | integer       | ✓        | The index of the output item this summary part is associated with.     |
| `summary_index`   | integer       | ✓        | The index of the summary part within the reasoning summary.            |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                     |
| `part`            | object        | ✓        | The summary part that was added.                                       |

---

### ResponseReasoningSummaryPartDoneEvent

Emitted when a reasoning summary part is completed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                           |
|-------------------|---------------|----------|-----------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.reasoning_summary_part.done`. |
| `item_id`         | string        | ✓        | The ID of the item this summary part is associated with.              |
| `output_index`    | integer       | ✓        | The index of the output item this summary part is associated with.    |
| `summary_index`   | integer       | ✓        | The index of the summary part within the reasoning summary.           |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                    |
| `part`            | object        | ✓        | The completed summary part.                                           |

---

### ResponseReasoningSummaryTextDeltaEvent

Emitted when a delta is added to a reasoning summary text.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                              |
|-------------------|---------------|----------|--------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.reasoning_summary_text.delta`.   |
| `item_id`         | string        | ✓        | The ID of the item this summary text delta is associated with.           |
| `output_index`    | integer       | ✓        | The index of the output item this summary text delta is associated with. |
| `summary_index`   | integer       | ✓        | The index of the summary part within the reasoning summary.              |
| `delta`           | string        | ✓        | The text delta that was added to the summary.                            |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                       |

---

### ResponseReasoningSummaryTextDoneEvent

Emitted when a reasoning summary text is completed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                           |
|-------------------|---------------|----------|-----------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.reasoning_summary_text.done`. |
| `item_id`         | string        | ✓        | The ID of the item this summary text is associated with.              |
| `output_index`    | integer       | ✓        | The index of the output item this summary text is associated with.    |
| `summary_index`   | integer       | ✓        | The index of the summary part within the reasoning summary.           |
| `text`            | string        | ✓        | The full text of the completed reasoning summary.                     |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                    |

---

### ResponseReasoningTextDeltaEvent

Emitted when a delta is added to a reasoning text.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                                |
|-------------------|---------------|----------|----------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.reasoning_text.delta`.             |
| `item_id`         | string        | ✓        | The ID of the item this reasoning text delta is associated with.           |
| `output_index`    | integer       | ✓        | The index of the output item this reasoning text delta is associated with. |
| `content_index`   | integer       | ✓        | The index of the reasoning content part this delta is associated with.     |
| `delta`           | string        | ✓        | The text delta that was added to the reasoning content.                    |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                         |

---

### ResponseReasoningTextDoneEvent

Emitted when a reasoning text is completed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                          |
|-------------------|---------------|----------|----------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.reasoning_text.done`.        |
| `item_id`         | string        | ✓        | The ID of the item this reasoning text is associated with.           |
| `output_index`    | integer       | ✓        | The index of the output item this reasoning text is associated with. |
| `content_index`   | integer       | ✓        | The index of the reasoning content part.                             |
| `text`            | string        | ✓        | The full text of the completed reasoning content.                    |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                   |

---

### ResponseRefusalDeltaEvent

Emitted when there is a partial refusal text.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                      |
|-------------------|---------------|----------|------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.refusal.delta`.          |
| `item_id`         | string        | ✓        | The ID of the output item that the refusal text is added to.     |
| `output_index`    | integer       | ✓        | The index of the output item that the refusal text is added to.  |
| `content_index`   | integer       | ✓        | The index of the content part that the refusal text is added to. |
| `delta`           | string        | ✓        | The refusal text that is added.                                  |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                               |

---

### ResponseRefusalDoneEvent

Emitted when refusal text is finalized.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                       |
|-------------------|---------------|----------|-------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.refusal.done`.            |
| `item_id`         | string        | ✓        | The ID of the output item that the refusal text is finalized.     |
| `output_index`    | integer       | ✓        | The index of the output item that the refusal text is finalized.  |
| `content_index`   | integer       | ✓        | The index of the content part that the refusal text is finalized. |
| `refusal`         | string        | ✓        | The refusal text that is finalized.                               |
| `sequence_number` | integer       | ✓        | The sequence number of this event.                                |

---

### ResponseStreamEvent

**anyOf:**

- [ResponseAudioDeltaEvent](#responseaudiodeltaevent)
- [ResponseAudioDoneEvent](#responseaudiodoneevent)
- [ResponseAudioTranscriptDeltaEvent](#responseaudiotranscriptdeltaevent)
- [ResponseAudioTranscriptDoneEvent](#responseaudiotranscriptdoneevent)
- [ResponseCodeInterpreterCallCodeDeltaEvent](#responsecodeinterpretercallcodedeltaevent)
- [ResponseCodeInterpreterCallCodeDoneEvent](#responsecodeinterpretercallcodedoneevent)
- [ResponseCodeInterpreterCallCompletedEvent](#responsecodeinterpretercallcompletedevent)
- [ResponseCodeInterpreterCallInProgressEvent](#responsecodeinterpretercallinprogressevent)
- [ResponseCodeInterpreterCallInterpretingEvent](#responsecodeinterpretercallinterpretingevent)
- [ResponseCompletedEvent](#responsecompletedevent)
- [ResponseContentPartAddedEvent](#responsecontentpartaddedevent)
- [ResponseContentPartDoneEvent](#responsecontentpartdoneevent)
- [ResponseCreatedEvent](#responsecreatedevent)
- [ResponseErrorEvent](#responseerrorevent)
- [ResponseFileSearchCallCompletedEvent](#responsefilesearchcallcompletedevent)
- [ResponseFileSearchCallInProgressEvent](#responsefilesearchcallinprogressevent)
- [ResponseFileSearchCallSearchingEvent](#responsefilesearchcallsearchingevent)
- [ResponseFunctionCallArgumentsDeltaEvent](#responsefunctioncallargumentsdeltaevent)
- [ResponseFunctionCallArgumentsDoneEvent](#responsefunctioncallargumentsdoneevent)
- [ResponseInProgressEvent](#responseinprogressevent)
- [ResponseFailedEvent](#responsefailedevent)
- [ResponseIncompleteEvent](#responseincompleteevent)
- [ResponseOutputItemAddedEvent](#responseoutputitemaddedevent)
- [ResponseOutputItemDoneEvent](#responseoutputitemdoneevent)
- [ResponseReasoningSummaryPartAddedEvent](#responsereasoningsummarypartaddedevent)
- [ResponseReasoningSummaryPartDoneEvent](#responsereasoningsummarypartdoneevent)
- [ResponseReasoningSummaryTextDeltaEvent](#responsereasoningsummarytextdeltaevent)
- [ResponseReasoningSummaryTextDoneEvent](#responsereasoningsummarytextdoneevent)
- [ResponseReasoningTextDeltaEvent](#responsereasoningtextdeltaevent)
- [ResponseReasoningTextDoneEvent](#responsereasoningtextdoneevent)
- [ResponseRefusalDeltaEvent](#responserefusaldeltaevent)
- [ResponseRefusalDoneEvent](#responserefusaldoneevent)
- [ResponseTextDeltaEvent](#responsetextdeltaevent)
- [ResponseTextDoneEvent](#responsetextdoneevent)
- [ResponseWebSearchCallCompletedEvent](#responsewebsearchcallcompletedevent)
- [ResponseWebSearchCallInProgressEvent](#responsewebsearchcallinprogressevent)
- [ResponseWebSearchCallSearchingEvent](#responsewebsearchcallsearchingevent)
- [ResponseImageGenCallCompletedEvent](#responseimagegencallcompletedevent)
- [ResponseImageGenCallGeneratingEvent](#responseimagegencallgeneratingevent)
- [ResponseImageGenCallInProgressEvent](#responseimagegencallinprogressevent)
- [ResponseImageGenCallPartialImageEvent](#responseimagegencallpartialimageevent)
- [ResponseMCPCallArgumentsDeltaEvent](#responsemcpcallargumentsdeltaevent)
- [ResponseMCPCallArgumentsDoneEvent](#responsemcpcallargumentsdoneevent)
- [ResponseMCPCallCompletedEvent](#responsemcpcallcompletedevent)
- [ResponseMCPCallFailedEvent](#responsemcpcallfailedevent)
- [ResponseMCPCallInProgressEvent](#responsemcpcallinprogressevent)
- [ResponseMCPListToolsCompletedEvent](#responsemcplisttoolscompletedevent)
- [ResponseMCPListToolsFailedEvent](#responsemcplisttoolsfailedevent)
- [ResponseMCPListToolsInProgressEvent](#responsemcplisttoolsinprogressevent)
- [ResponseOutputTextAnnotationAddedEvent](#responseoutputtextannotationaddedevent)
- [ResponseQueuedEvent](#responsequeuedevent)
- [ResponseCustomToolCallInputDeltaEvent](#responsecustomtoolcallinputdeltaevent)
- [ResponseCustomToolCallInputDoneEvent](#responsecustomtoolcallinputdoneevent)

---

### ResponseStreamOptions

**anyOf:**

- object
- null

---

### ResponseTextDeltaEvent

Emitted when there is an additional text delta.

**Type:** `object`

**Properties:**

| Property          | Type                                       | Required | Description                                                     |
|-------------------|--------------------------------------------|----------|-----------------------------------------------------------------|
| `type`            | string (enum)                              | ✓        | The type of the event. Always `response.output_text.delta`.     |
| `item_id`         | string                                     | ✓        | The ID of the output item that the text delta was added to.     |
| `output_index`    | integer                                    | ✓        | The index of the output item that the text delta was added to.  |
| `content_index`   | integer                                    | ✓        | The index of the content part that the text delta was added to. |
| `delta`           | string                                     | ✓        | The text delta that was added.                                  |
| `sequence_number` | integer                                    | ✓        | The sequence number for this event.                             |
| `logprobs`        | array[[ResponseLogProb](#responselogprob)] | ✓        | The log probabilities of the tokens in the delta.               |

---

### ResponseTextDoneEvent

Emitted when text content is finalized.

**Type:** `object`

**Properties:**

| Property          | Type                                       | Required | Description                                                       |
|-------------------|--------------------------------------------|----------|-------------------------------------------------------------------|
| `type`            | string (enum)                              | ✓        | The type of the event. Always `response.output_text.done`.        |
| `item_id`         | string                                     | ✓        | The ID of the output item that the text content is finalized.     |
| `output_index`    | integer                                    | ✓        | The index of the output item that the text content is finalized.  |
| `content_index`   | integer                                    | ✓        | The index of the content part that the text content is finalized. |
| `text`            | string                                     | ✓        | The text content that is finalized.                               |
| `sequence_number` | integer                                    | ✓        | The sequence number for this event.                               |
| `logprobs`        | array[[ResponseLogProb](#responselogprob)] | ✓        | The log probabilities of the tokens in the delta.                 |

---

### ResponseTextParam

Configuration options for a text response from the model. Can be plain
text or structured JSON data. Learn more:

- [Text inputs and outputs](https://platform.openai.com/docs/guides/text)
- [Structured Outputs](https://platform.openai.com/docs/guides/structured-outputs)

**Type:** `object`

**Properties:**

| Property    | Type                                                                | Required | Description |
|-------------|---------------------------------------------------------------------|----------|-------------|
| `format`    | [TextResponseFormatConfiguration](#textresponseformatconfiguration) |          |             |
| `verbosity` | [Verbosity](#verbosity)                                             |          |             |

---

### ResponseUsage

Represents token usage details including input tokens, output tokens,
a breakdown of output tokens, and the total tokens used.

**Type:** `object`

**Properties:**

| Property                | Type    | Required | Description                                |
|-------------------------|---------|----------|--------------------------------------------|
| `input_tokens`          | integer | ✓        | The number of input tokens.                |
| `input_tokens_details`  | object  | ✓        | A detailed breakdown of the input tokens.  |
| `output_tokens`         | integer | ✓        | The number of output tokens.               |
| `output_tokens_details` | object  | ✓        | A detailed breakdown of the output tokens. |
| `total_tokens`          | integer | ✓        | The total number of tokens used.           |

---

### ResponseWebSearchCallCompletedEvent

Emitted when a web search call is completed.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                               |
|-------------------|---------------|----------|---------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.web_search_call.completed`.       |
| `output_index`    | integer       | ✓        | The index of the output item that the web search call is associated with. |
| `item_id`         | string        | ✓        | Unique ID for the output item associated with the web search call.        |
| `sequence_number` | integer       | ✓        | The sequence number of the web search call being processed.               |

---

### ResponseWebSearchCallInProgressEvent

Emitted when a web search call is initiated.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                               |
|-------------------|---------------|----------|---------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.web_search_call.in_progress`.     |
| `output_index`    | integer       | ✓        | The index of the output item that the web search call is associated with. |
| `item_id`         | string        | ✓        | Unique ID for the output item associated with the web search call.        |
| `sequence_number` | integer       | ✓        | The sequence number of the web search call being processed.               |

---

### ResponseWebSearchCallSearchingEvent

Emitted when a web search call is executing.

**Type:** `object`

**Properties:**

| Property          | Type          | Required | Description                                                               |
|-------------------|---------------|----------|---------------------------------------------------------------------------|
| `type`            | string (enum) | ✓        | The type of the event. Always `response.web_search_call.searching`.       |
| `output_index`    | integer       | ✓        | The index of the output item that the web search call is associated with. |
| `item_id`         | string        | ✓        | Unique ID for the output item associated with the web search call.        |
| `sequence_number` | integer       | ✓        | The sequence number of the web search call being processed.               |

---

### Screenshot

A screenshot action.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                                      |
|----------|---------------|----------|--------------------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Specifies the event type. For a screenshot action, this property is  always set to `screenshot`. |

---

### Scroll

A scroll action.

**Type:** `object`

**Properties:**

| Property   | Type          | Required | Description                                                                              |
|------------|---------------|----------|------------------------------------------------------------------------------------------|
| `type`     | string (enum) | ✓        | Specifies the event type. For a scroll action, this property is  always set to `scroll`. |
| `x`        | integer       | ✓        | The x-coordinate where the scroll occurred.                                              |
| `y`        | integer       | ✓        | The y-coordinate where the scroll occurred.                                              |
| `scroll_x` | integer       | ✓        | The horizontal scroll distance.                                                          |
| `scroll_y` | integer       | ✓        | The vertical scroll distance.                                                            |

---

### SearchContextSize

**Type:** `string`

**Possible values:**

- `low`
- `medium`
- `high`

---

### ServiceTier

**anyOf:**

- string (enum)
- null

---

### SpecificApplyPatchParam

Forces the model to call the apply_patch tool when executing a tool call.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                             |
|----------|---------------|----------|-----------------------------------------|
| `type`   | string (enum) | ✓        | The tool to call. Always `apply_patch`. |

---

### SpecificFunctionShellParam

Forces the model to call the function shell tool when a tool call is required.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                       |
|----------|---------------|----------|-----------------------------------|
| `type`   | string (enum) | ✓        | The tool to call. Always `shell`. |

---

### Summary

A summary text from the model.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                              |
|----------|---------------|----------|----------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of the object. Always `summary_text`.           |
| `text`   | string        | ✓        | A summary of the reasoning output from the model so far. |

---

### TextResponseFormatConfiguration

An object specifying the format that the model must output.

Configuring `{ "type": "json_schema" }` enables Structured Outputs,
which ensures the model will match your supplied JSON schema. Learn more in the
[Structured Outputs guide](https://platform.openai.com/docs/guides/structured-outputs).

The default format is `{ "type": "text" }` with no additional options.

**Not recommended for gpt-4o and newer models:**

Setting to `{ "type": "json_object" }` enables the older JSON mode, which
ensures the message the model generates is valid JSON. Using `json_schema`
is preferred for models that support it.

**anyOf:**

- [ResponseFormatText](#responseformattext)
- [TextResponseFormatJsonSchema](#textresponseformatjsonschema)
- [ResponseFormatJsonObject](#responseformatjsonobject)

---

### TextResponseFormatJsonSchema

JSON Schema response format. Used to generate structured JSON responses.
Learn more about [Structured Outputs](https://platform.openai.com/docs/guides/structured-outputs).

**Type:** `object`

**Properties:**

| Property      | Type                                                              | Required | Description                                                                                                             |
|---------------|-------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------|
| `type`        | string (enum)                                                     | ✓        | The type of response format being defined. Always `json_schema`.                                                        |
| `description` | string                                                            |          | A description of what the response format is for, used by the model to determine how to respond in the format.          |
| `name`        | string                                                            | ✓        | The name of the response format. Must be a-z, A-Z, 0-9, or contain underscores and dashes, with a maximum length of 64. |
| `schema`      | [ResponseFormatJsonSchemaSchema](#responseformatjsonschemaschema) | ✓        |                                                                                                                         |
| `strict`      | any                                                               |          |                                                                                                                         |

---

### Tool

A tool that can be used to generate a response.

**anyOf:**

- [FunctionTool](#functiontool)
- [FileSearchTool](#filesearchtool)
- [ComputerUsePreviewTool](#computerusepreviewtool)
- [WebSearchTool](#websearchtool)
- [MCPTool](#mcptool)
- [CodeInterpreterTool](#codeinterpretertool)
- [ImageGenTool](#imagegentool)
- [LocalShellToolParam](#localshelltoolparam)
- [FunctionShellToolParam](#functionshelltoolparam)
- [CustomToolParam](#customtoolparam)
- [WebSearchPreviewTool](#websearchpreviewtool)
- [ApplyPatchToolParam](#applypatchtoolparam)

---

### ToolChoiceAllowed

Constrains the tools available to the model to a pre-defined set.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                                                                                                                                                                                                                                   |
|----------|---------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Allowed tool configuration type. Always `allowed_tools`.                                                                                                                                                                                                                                      |
| `mode`   | string (enum) | ✓        | Constrains the tools available to the model to a pre-defined set.  `auto` allows the model to pick from among the allowed tools and generate a message.  `required` requires the model to call one or more of the allowed tools.                                                              |
| `tools`  | array[object] | ✓        | A list of tool definitions that the model should be allowed to call.  For the Responses API, the list of tool definitions might look like: ```json [   { "type": "function", "name": "get_weather" },   { "type": "mcp", "server_label": "deepwiki" },   { "type": "image_generation" } ] ``` |

---

### ToolChoiceCustom

Use this option to force the model to call a specific custom tool.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                           |
|----------|---------------|----------|-------------------------------------------------------|
| `type`   | string (enum) | ✓        | For custom tool calling, the type is always `custom`. |
| `name`   | string        | ✓        | The name of the custom tool to call.                  |

---

### ToolChoiceFunction

Use this option to force the model to call a specific function.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                          |
|----------|---------------|----------|------------------------------------------------------|
| `type`   | string (enum) | ✓        | For function calling, the type is always `function`. |
| `name`   | string        | ✓        | The name of the function to call.                    |

---

### ToolChoiceMCP

Use this option to force the model to call a specific tool on a remote MCP server.

**Type:** `object`

**Properties:**

| Property       | Type          | Required | Description                              |
|----------------|---------------|----------|------------------------------------------|
| `type`         | string (enum) | ✓        | For MCP tools, the type is always `mcp`. |
| `server_label` | string        | ✓        | The label of the MCP server to use.      |
| `name`         | any           |          |                                          |

---

### ToolChoiceOptions

Controls which (if any) tool is called by the model.

`none` means the model will not call any tool and instead generates a message.

`auto` means the model can pick between generating a message or calling one or
more tools.

`required` means the model must call one or more tools.

**Type:** `string`

**Possible values:**

- `none`
- `auto`
- `required`

---

### ToolChoiceParam

How the model should select which tool (or tools) to use when generating
a response. See the `tools` parameter to see how to specify which tools
the model can call.

**anyOf:**

- [ToolChoiceOptions](#toolchoiceoptions)
- [ToolChoiceAllowed](#toolchoiceallowed)
- [ToolChoiceTypes](#toolchoicetypes)
- [ToolChoiceFunction](#toolchoicefunction)
- [ToolChoiceMCP](#toolchoicemcp)
- [ToolChoiceCustom](#toolchoicecustom)
- [SpecificApplyPatchParam](#specificapplypatchparam)
- [SpecificFunctionShellParam](#specificfunctionshellparam)

---

### ToolChoiceTypes

Indicates that the model should use a built-in tool to generate a response.
[Learn more about built-in tools](https://platform.openai.com/docs/guides/tools).

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                                                                                                                                                                                                       |
|----------|---------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | The type of hosted tool the model should to use. Learn more about [built-in tools](https://platform.openai.com/docs/guides/tools).  Allowed values are: - `file_search` - `web_search_preview` - `computer_use_preview` - `code_interpreter` - `image_generation` |

---

### ToolsArray

An array of tools the model may call while generating a response. You
can specify which tool to use by setting the `tool_choice` parameter.

We support the following categories of tools:

- **Built-in tools**: Tools that are provided by OpenAI that extend the
  model's capabilities, like [web search](https://platform.openai.com/docs/guides/tools-web-search)
  or [file search](https://platform.openai.com/docs/guides/tools-file-search). Learn more about
  [built-in tools](https://platform.openai.com/docs/guides/tools).
- **MCP Tools**: Integrations with third-party systems via custom MCP servers
  or predefined connectors such as Google Drive and SharePoint. Learn more about
  [MCP Tools](https://platform.openai.com/docs/guides/tools-connectors-mcp).
- **Function calls (custom tools)**: Functions that are defined by you,
  enabling the model to call your own code with strongly typed arguments
  and outputs. Learn more about
  [function calling](https://platform.openai.com/docs/guides/function-calling). You can also use
  custom tools to call your own code.

**Type:** `array`

---

### TopLogProb

The top log probability of a token.

**Type:** `object`

**Properties:**

| Property  | Type           | Required | Description |
|-----------|----------------|----------|-------------|
| `token`   | string         | ✓        |             |
| `logprob` | number         | ✓        |             |
| `bytes`   | array[integer] | ✓        |             |

---

### Type

An action to type in text.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                          |
|----------|---------------|----------|--------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Specifies the event type. For a type action, this property is  always set to `type`. |
| `text`   | string        | ✓        | The text to type.                                                                    |

---

### UrlCitationBody

A citation for a web resource used to generate a model response.

**Type:** `object`

**Properties:**

| Property      | Type          | Required | Description                                                          |
|---------------|---------------|----------|----------------------------------------------------------------------|
| `type`        | string (enum) | ✓        | The type of the URL citation. Always `url_citation`.                 |
| `url`         | string        | ✓        | The URL of the web resource.                                         |
| `start_index` | integer       | ✓        | The index of the first character of the URL citation in the message. |
| `end_index`   | integer       | ✓        | The index of the last character of the URL citation in the message.  |
| `title`       | string        | ✓        | The title of the web resource.                                       |

---

### VectorStoreFileAttributes

**anyOf:**

- object (map of any)
- null

---

### Verbosity

**anyOf:**

- string (enum)
- null

---

### Wait

A wait action.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                          |
|----------|---------------|----------|--------------------------------------------------------------------------------------|
| `type`   | string (enum) | ✓        | Specifies the event type. For a wait action, this property is  always set to `wait`. |

---

### WebSearchActionFind

Action type "find": Searches for a pattern within a loaded page.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                                        |
|-----------|---------------|----------|----------------------------------------------------|
| `type`    | string (enum) | ✓        | The action type.                                   |
| `url`     | string (uri)  | ✓        | The URL of the page searched for the pattern.      |
| `pattern` | string        | ✓        | The pattern or text to search for within the page. |

---

### WebSearchActionOpenPage

Action type "open_page" - Opens a specific URL from search results.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                  |
|----------|---------------|----------|------------------------------|
| `type`   | string (enum) | ✓        | The action type.             |
| `url`    | string (uri)  | ✓        | The URL opened by the model. |

---

### WebSearchActionSearch

Action type "search" - Performs a web search query.

**Type:** `object`

**Properties:**

| Property  | Type          | Required | Description                     |
|-----------|---------------|----------|---------------------------------|
| `type`    | string (enum) | ✓        | The action type.                |
| `query`   | string        | ✓        | The search query.               |
| `sources` | array[object] |          | The sources used in the search. |

---

### WebSearchApproximateLocation

**anyOf:**

- object
- null

---

### WebSearchPreviewTool

This tool searches the web for relevant results to use in a response. Learn more about
the [web search tool](https://platform.openai.com/docs/guides/tools-web-search).

**Type:** `object`

**Properties:**

| Property              | Type                                    | Required | Description                                                                                                                                   |
|-----------------------|-----------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `type`                | string (enum)                           | ✓        | The type of the web search tool. One of `web_search_preview` or `web_search_preview_2025_03_11`.                                              |
| `user_location`       | any                                     |          |                                                                                                                                               |
| `search_context_size` | [SearchContextSize](#searchcontextsize) |          | High level guidance for the amount of context window space to use for the search. One of `low`, `medium`, or `high`. `medium` is the default. |

---

### WebSearchTool

Search the Internet for sources related to the prompt. Learn more about the
[web search tool](https://platform.openai.com/docs/guides/tools-web-search).

**Type:** `object`

**Properties:**

| Property              | Type                                                          | Required | Description                                                                                                                                   |
|-----------------------|---------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `type`                | string (enum)                                                 | ✓        | The type of the web search tool. One of `web_search` or `web_search_2025_08_26`.                                                              |
| `filters`             | any                                                           |          |                                                                                                                                               |
| `user_location`       | [WebSearchApproximateLocation](#websearchapproximatelocation) |          |                                                                                                                                               |
| `search_context_size` | string (enum)                                                 |          | High level guidance for the amount of context window space to use for the search. One of `low`, `medium`, or `high`. `medium` is the default. |

---

### WebSearchToolCall

The results of a web search tool call. See the
[web search guide](https://platform.openai.com/docs/guides/tools-web-search) for more information.

**Type:** `object`

**Properties:**

| Property | Type          | Required | Description                                                                                                                                       |
|----------|---------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`     | string        | ✓        | The unique ID of the web search tool call.                                                                                                        |
| `type`   | string (enum) | ✓        | The type of the web search tool call. Always `web_search_call`.                                                                                   |
| `status` | string (enum) | ✓        | The status of the web search tool call.                                                                                                           |
| `action` | object        | ✓        | An object describing the specific action taken in this web search call. Includes details on how the model used the web (search, open_page, find). |

---

