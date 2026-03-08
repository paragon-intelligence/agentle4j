package com.paragon.harness;

import com.paragon.agents.AgenticContext;
import com.paragon.agents.AgentResult;
import com.paragon.agents.ToolExecution;
import com.paragon.responses.spec.FunctionToolCall;
import org.jspecify.annotations.NonNull;

/**
 * Lifecycle hook that intercepts agent and tool execution events.
 *
 * <p>Implement this interface to inject cross-cutting concerns (logging, rate limiting,
 * cost tracking, circuit breakers, etc.) without modifying agent logic.
 *
 * <p>Hooks are executed in registration order. All default methods are no-ops, so
 * you only override the lifecycle events you care about.
 *
 * <p>Example — log every tool call:
 *
 * <pre>{@code
 * AgentHook loggingHook = new AgentHook() {
 *     @Override
 *     public void beforeToolCall(FunctionToolCall call, AgenticContext ctx) {
 *         System.out.println("Calling tool: " + call.name() + " args=" + call.arguments());
 *     }
 *
 *     @Override
 *     public void afterToolCall(FunctionToolCall call, ToolExecution result, AgenticContext ctx) {
 *         System.out.println("Tool result: " + result.output());
 *     }
 * };
 * }</pre>
 *
 * @see HookRegistry
 * @since 1.0
 */
public interface AgentHook {

  /**
   * Called before the agent's agentic loop starts.
   *
   * @param context the conversation context at the start of this run
   */
  default void beforeRun(@NonNull AgenticContext context) {}

  /**
   * Called after the agent's agentic loop completes (success or failure).
   *
   * @param result the result of the run
   * @param context the conversation context at completion
   */
  default void afterRun(@NonNull AgentResult result, @NonNull AgenticContext context) {}

  /**
   * Called before a tool is invoked. Can be used to block or log tool calls.
   *
   * @param call the tool call that is about to be executed
   * @param context the current conversation context
   */
  default void beforeToolCall(@NonNull FunctionToolCall call, @NonNull AgenticContext context) {}

  /**
   * Called after a tool has been invoked.
   *
   * @param call the tool call that was executed
   * @param execution the execution record containing output and timing
   * @param context the current conversation context
   */
  default void afterToolCall(
      @NonNull FunctionToolCall call,
      @NonNull ToolExecution execution,
      @NonNull AgenticContext context) {}
}
