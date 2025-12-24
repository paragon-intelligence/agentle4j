package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Give the model access to additional tools via remote Model Context Protocol (MCP) servers. Learn
 * more about MCP.
 */
public record McpTool(
    @NonNull String serverLabel,
    @Nullable McpToolFilter allowedTools,
    @Nullable String authorization,
    @Nullable String connectorId,
    @Nullable Map<String, String> headers,
    @Nullable McpToolApprovalFilter requireApproval,
    @Nullable String serverDescription,
    @Nullable String serverUrl)
    implements Tool {
  /**
   * @param serverLabel A label for this MCP server, used to identify it in tool calls.
   * @param allowedTools List of allowed tool names or a filter object. See {@link McpToolFilter}
   * @param authorization An OAuth access token that can be used with a remote MCP server, either
   *     with a custom MCP server URL or a service connector. Your application must handle the OAuth
   *     authorization flow and provide the token here.
   * @param connectorId Identifier for service connectors, like those available in ChatGPT. One of
   *     server_url or connector_id must be provided. Learn more about service connectors here.
   *     <p>Currently supported connector_id values are:
   *     <ul>
   *       <li>Dropbox: {@code connector_dropbox}
   *       <li>Gmail: {@code connector_gmail}
   *       <li>Google Calendar: {@code connector_googlecalendar}
   *       <li>Google Drive: {@code connector_googledrive}
   *       <li>Microsoft Teams: {@code connector_microsoftteams}
   *       <li>Outlook Calendar: {@code connector_outlookcalendar}
   *       <li>Outlook Email: {@code connector_outlookemail}
   *       <li>SharePoint: {@code connector_sharepoint}
   *     </ul>
   *
   * @param headers Optional HTTP headers to send to the MCP server. Use for authentication or other
   *     purposes.
   * @param requireApproval Specify which of the MCP server's tools require approval. See {@link
   *     McpToolApprovalFilter}
   * @param serverDescription Optional description of the MCP server, used to provide more context.
   * @param serverUrl The URL for the MCP server. One of {@code server_url} or {@code connector_id}
   *     must be provided.
   */
  public McpTool {}

  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    if (allowedTools != null) {
      String name = allowedTools.toolNames().getFirst();
      return mapper.writeValueAsString(
          Map.of("server_label", serverLabel, "type", "mcp", "name", name));
    }
    return mapper.writeValueAsString(Map.of("server_label", serverLabel, "type", "mcp"));
  }

  @Override
  public @NonNull String toString() {
    return "McpTool["
        + "serverLabel="
        + serverLabel
        + ", "
        + "allowedTools="
        + allowedTools
        + ", "
        + "authorization="
        + authorization
        + ", "
        + "connectorId="
        + connectorId
        + ", "
        + "headers="
        + headers
        + ", "
        + "requireApproval="
        + requireApproval
        + ", "
        + "serverDescription="
        + serverDescription
        + ", "
        + "serverUrl="
        + serverUrl
        + ']';
  }
}
