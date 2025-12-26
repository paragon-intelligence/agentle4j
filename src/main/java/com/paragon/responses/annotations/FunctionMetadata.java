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
   * <p>When true, the tool will trigger the {@code onToolCallPending} or {@code onPause}
   * callback in AgentStream, allowing human-in-the-loop approval workflows.
   *
   * <p>When false (default), the tool executes automatically without confirmation.
   *
   * <p>Example:
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
}

