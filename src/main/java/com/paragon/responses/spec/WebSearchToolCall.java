package com.paragon.responses.spec;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** The results of a web search tool call. See the web search guide for more information. */
public final class WebSearchToolCall extends ToolCall implements Item, ResponseOutput {
  private final @NonNull WebAction action;
  private final @NonNull String status;

  /**
   * @param action An object describing the specific action taken in this web search call. Includes
   *     details on how the model used the web (search, open_page, find).
   * @param id The unique ID of the web search tool call.
   * @param status The status of the web search tool call.
   */
  public WebSearchToolCall(@NonNull WebAction action, @NonNull String id, @NonNull String status) {
    super(id);
    this.action = action;
    this.status = status;
  }

  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <web_search_tool_call>
            <id>%s</id>
            <status>%s</status>
            <action>%s</action>
        </web_search_tool_call>
        """,
        id, status, action);
  }

  public @NonNull WebAction action() {
    return action;
  }

  @Override
  public @NonNull String id() {
    return id;
  }

  public @NonNull String status() {
    return status;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (WebSearchToolCall) obj;
    return Objects.equals(this.action, that.action)
        && Objects.equals(this.id, that.id)
        && Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, id, status);
  }
}
