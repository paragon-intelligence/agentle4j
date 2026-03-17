package com.paragon.responses.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define function tool metadata. Apply this to FunctionTool subclasses to specify
 * name, description, and confirmation requirements.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionMetadata {
  /** The name of the function tool. */
  String name();

  /** The description of the function tool. */
  String description() default "";

  /**
   * Whether this tool requires human confirmation before execution.
   *
   * <p>When true, the tool will trigger the {@code onToolCallPending} or {@code onPause} callback
   * in AgentStream, allowing human-in-the-loop approval workflows.
   *
   * <p>When false (default), the tool executes automatically without confirmation.
   *
   * <p>Example:
   *
   * <pre>{@code
   * @FunctionMetadata(
   *     name = "delete_records",
   *     description = "Deletes database records permanently",
   *     requiresConfirmation = true  // User must approve before execution
   * )
   * public class DeleteRecordsTool extends FunctionTool<DeleteParams> { ... }
   * }</pre>
   */
  boolean requiresConfirmation() default false;

  /**
   * Whether this tool is client-side only and terminates the agentic loop immediately when called.
   *
   * <p>When true, the framework will:
   * <ol>
   *   <li>Detect the tool call in the output
   *   <li>Skip persisting the call to conversation history
   *   <li>Skip executing the tool's {@code call()} method
   *   <li>Return {@code AgentResult.clientSideTool()} as a clean, non-error exit
   * </ol>
   *
   * <p>This mirrors the {@code ask_user_input_v0} pattern used by Claude.ai, where a tool call
   * acts as a UI signal to the frontend rather than server-side logic.
   *
   * <p>Example:
   *
   * <pre>{@code
   * @FunctionMetadata(
   *     name = "ask_user",
   *     description = "Ask the user a clarifying question",
   *     stopsLoop = true
   * )
   * public class AskUserTool extends FunctionTool<AskUserParams> {
   *     @Override
   *     public @Nullable FunctionToolCallOutput call(@Nullable AskUserParams params) {
   *         return null; // never called
   *     }
   * }
   * }</pre>
   */
  boolean stopsLoop() default false;
}
