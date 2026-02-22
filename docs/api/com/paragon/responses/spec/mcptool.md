# :material-database: McpTool

`com.paragon.responses.spec.McpTool` &nbsp;·&nbsp; **Record**

---

Give the model access to additional tools via remote Model Context Protocol (MCP) servers. Learn
more about MCP.

## Fields

### `McpTool`

```java
public McpTool
```

@param serverLabel A label for this MCP server, used to identify it in tool calls.

**Parameters**

| Name | Description |
|------|-------------|
| `allowedTools` | List of allowed tool names or a filter object. See `McpToolFilter` |
| `authorization` | An OAuth access token that can be used with a remote MCP server, either with a custom MCP server URL or a service connector. Your application must handle the OAuth     authorization flow and provide the token here. |
| `connectorId` | Identifier for service connectors, like those available in ChatGPT. One of server_url or connector_id must be provided. Learn more about service connectors here.       Currently supported connector_id values are:             - Dropbox: `connector_dropbox` - Gmail: `connector_gmail` - Google Calendar: `connector_googlecalendar` - Google Drive: `connector_googledrive` - Microsoft Teams: `connector_microsoftteams` - Outlook Calendar: `connector_outlookcalendar` - Outlook Email: `connector_outlookemail` - SharePoint: `connector_sharepoint` |
| `headers` | Optional HTTP headers to send to the MCP server. Use for authentication or other purposes. |
| `requireApproval` | Specify which of the MCP server's tools require approval. See `McpToolApprovalFilter` |
| `serverDescription` | Optional description of the MCP server, used to provide more context. |
| `serverUrl` | The URL for the MCP server. One of `server_url` or `connector_id` must be provided. |

