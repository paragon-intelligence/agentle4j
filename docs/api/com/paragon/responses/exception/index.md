# Package `com.paragon.responses.exception`

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`AgentExecutionException`](agentexecutionexception.md) | Exception thrown when an agent execution fails |
| [`AgentleException`](agentleexception.md) | Base exception for all Agentle4j errors |
| [`ApiException`](apiexception.md) | Exception thrown when an API request fails |
| [`AuthenticationException`](authenticationexception.md) | Exception thrown when authentication fails (HTTP 401/403) |
| [`ConfigurationException`](configurationexception.md) | Exception thrown when required configuration is missing or invalid |
| [`GuardrailException`](guardrailexception.md) | Exception thrown when a guardrail validation fails |
| [`InvalidRequestException`](invalidrequestexception.md) | Exception thrown when the request is invalid (HTTP 4xx, excluding 401/403/429) |
| [`RateLimitException`](ratelimitexception.md) | Exception thrown when rate limited by the API (HTTP 429) |
| [`ServerException`](serverexception.md) | Exception thrown when the server encounters an error (HTTP 5xx) |
| [`StreamingException`](streamingexception.md) | Exception thrown when an error occurs during streaming |
| [`ToolExecutionException`](toolexecutionexception.md) | Exception thrown when a tool execution fails |
