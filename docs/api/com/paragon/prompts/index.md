# Package `com.paragon.prompts`

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`FilesystemPromptProvider`](filesystempromptprovider.md) | A `PromptProvider` that reads prompts from the local filesystem |
| [`LangfusePromptListResponse`](langfusepromptlistresponse.md) | Response from the Langfuse prompts list API (v2) |
| [`LangfusePromptProvider`](langfusepromptprovider.md) | A `PromptProvider` that retrieves prompts from the Langfuse API |
| [`Prompt`](prompt.md) | Represents an immutable text prompt that can contain template expressions |
| [`PromptProviderException`](promptproviderexception.md) | Exception thrown when a prompt cannot be retrieved from a `PromptProvider` |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`PromptProvider`](promptprovider.md) | Provider interface for retrieving prompts from various sources |
| [`PromptStore`](promptstore.md) | Store interface for persisting prompts to various storage backends |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`LangfusePromptResponse`](langfusepromptresponse.md) | Response DTO for Langfuse prompt API |
