package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * This is a sealed abstract class with eighteen permitted implementations:
 *
 * <ul>
 *   <li>{@link Message} - A message input to the model with a role indicating instruction following
 *       hierarchy. Instructions given with the developer or system role take precedence over
 *       instructions given with the user role .
 *   <li>{@link OutputMessage} - An output message from the model.
 *   <li>{@link FileSearchToolCall} - The results of a file search tool call. See the <a
 *       href="https://platform.openai.com/docs/guides/tools-file-search">file search guide</a> for
 *       more information.
 *   <li>{@link ComputerToolCall} - A tool call to a computer use tool. See the <a
 *       href="https://platform.openai.com/docs/guides/tools-computer-use">computer use guide</a>
 *       for more information.
 *   <li>{@link ComputerToolCallOutput} - The output of a computer tool call.
 *   <li>{@link WebSearchToolCall} - The results of a web search tool call. See the <a
 *       href="https://platform.openai.com/docs/guides/tools-web-search?api-mode=responses">web
 *       search guide</a> for more information.
 *   <li>{@link FunctionToolCall} - A tool call to run a function. See the <a
 *       href="https://platform.openai.com/docs/guides/function-calling">function calling guide</a>
 *       for more information.
 *   <li>{@link FunctionToolCallOutput} - The output of a function tool call.
 *   <li>{@link Reasoning} - A description of the chain of thought used by a reasoning model while
 *       generating a response. Be sure to include these items in your input to the Responses API
 *       for subsequent turns of a conversation if you are manually <a
 *       href="https://platform.openai.com/docs/guides/conversation-state?api-mode=responses">managing
 *       context.</a>
 *   <li>{@link ImageGenerationCall} - An image generation request made by the model.
 *   <li>{@link CodeInterpreterToolCall} - A tool call to run code.
 *   <li>{@link LocalShellCall} - A tool call to run a command on the local shell.
 *   <li>{@link LocalShellCallOutput} - The output of a local shell tool call.
 *   <li>{@link FunctionShellToolCall} - A tool representing a request to execute one or more shell
 *       commands.
 *   <li>{@link FunctionShellToolCallOutput} - The streamed output items emitted by a function shell
 *       tool call.
 *   <li>{@link ApplyPatchToolCall} - A tool call representing a request to create, delete, or
 *       update files using diff patches.
 *   <li>{@link ApplyPatchToolCallOutput} - The streamed output emitted by an apply patch tool call.
 *   <li>{@link McpListTools} - A list of tools available on an MCP server.
 *   <li>{@link McpApprovalRequest} - A request for human approval of a tool invocation.
 *   <li>{@link McpApprovalResponse} - A response to an MCP approval request.
 *   <li>{@link McpToolCall} - An invocation of a tool on an MCP server.
 *   <li>{@link CustomToolCallOutput} - The output of a custom tool call from your code, being sent
 *       back to the model .
 *   <li>{@link CustomToolCall} - A call to a custom tool created by the model.
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = Message.class, name = "message")})
public sealed interface Item extends ResponseInputItem
    permits Message,
        OutputMessage,
        FileSearchToolCall,
        ComputerToolCall,
        ComputerToolCallOutput,
        WebSearchToolCall,
        FunctionToolCall,
        FunctionToolCallOutput,
        Reasoning,
        ImageGenerationCall,
        CodeInterpreterToolCall,
        LocalShellCall,
        LocalShellCallOutput,
        FunctionShellToolCall,
        FunctionShellToolCallOutput,
        ApplyPatchToolCall,
        ApplyPatchToolCallOutput,
        McpListTools,
        McpApprovalRequest,
        McpApprovalResponse,
        McpToolCall,
        CustomToolCallOutput,
        CustomToolCall {}
