package com.paragon.harness;

import com.paragon.agents.AgenticContext;
import com.paragon.agents.AgentResult;
import com.paragon.agents.ToolExecution;
import com.paragon.responses.spec.FunctionToolCall;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Ordered registry of {@link AgentHook} instances executed around agent lifecycle events.
 *
 * <p>Hooks are invoked in registration order for {@code before} events and in reverse order
 * for {@code after} events (stack semantics).
 *
 * <p>Hook failures are caught and logged but do not interrupt agent execution, ensuring
 * that harness infrastructure never breaks agent functionality.
 *
 * <p>Example:
 *
 * <pre>{@code
 * HookRegistry hooks = HookRegistry.create()
 *     .add(new LoggingHook())
 *     .add(new CostTrackingHook())
 *     .add(new RateLimitingHook(100));
 *
 * // Wire into agent via Harness builder or directly:
 * Agent agent = Agent.builder()
 *     .hookRegistry(hooks)
 *     .build();
 * }</pre>
 *
 * @see AgentHook
 * @since 1.0
 */
public final class HookRegistry {

  private final List<AgentHook> hooks;

  private HookRegistry(List<AgentHook> hooks) {
    this.hooks = new ArrayList<>(hooks);
  }

  /** Creates an empty registry. */
  public static @NonNull HookRegistry create() {
    return new HookRegistry(List.of());
  }

  /** Creates a registry pre-populated with the given hooks. */
  public static @NonNull HookRegistry of(@NonNull AgentHook... hooks) {
    return new HookRegistry(List.of(hooks));
  }

  /**
   * Adds a hook to the registry.
   *
   * @param hook the hook to add
   * @return this registry (for chaining)
   */
  public @NonNull HookRegistry add(@NonNull AgentHook hook) {
    Objects.requireNonNull(hook, "hook cannot be null");
    hooks.add(hook);
    return this;
  }

  /** Returns the number of registered hooks. */
  public int size() {
    return hooks.size();
  }

  /** Returns true if no hooks are registered. */
  public boolean isEmpty() {
    return hooks.isEmpty();
  }

  // ===== Dispatch Methods =====

  /** Dispatches {@link AgentHook#beforeRun} to all hooks in order. */
  public void fireBeforeRun(@NonNull AgenticContext context) {
    for (AgentHook hook : hooks) {
      try {
        hook.beforeRun(context);
      } catch (Exception e) {
        handleHookError("beforeRun", hook, e);
      }
    }
  }

  /** Dispatches {@link AgentHook#afterRun} to all hooks in reverse order. */
  public void fireAfterRun(@NonNull AgentResult result, @NonNull AgenticContext context) {
    for (int i = hooks.size() - 1; i >= 0; i--) {
      try {
        hooks.get(i).afterRun(result, context);
      } catch (Exception e) {
        handleHookError("afterRun", hooks.get(i), e);
      }
    }
  }

  /** Dispatches {@link AgentHook#beforeToolCall} to all hooks in order. */
  public void fireBeforeToolCall(@NonNull FunctionToolCall call, @NonNull AgenticContext context) {
    for (AgentHook hook : hooks) {
      try {
        hook.beforeToolCall(call, context);
      } catch (Exception e) {
        handleHookError("beforeToolCall", hook, e);
      }
    }
  }

  /** Dispatches {@link AgentHook#afterToolCall} to all hooks in reverse order. */
  public void fireAfterToolCall(
      @NonNull FunctionToolCall call,
      @NonNull ToolExecution execution,
      @NonNull AgenticContext context) {
    for (int i = hooks.size() - 1; i >= 0; i--) {
      try {
        hooks.get(i).afterToolCall(call, execution, context);
      } catch (Exception e) {
        handleHookError("afterToolCall", hooks.get(i), e);
      }
    }
  }

  private void handleHookError(String event, AgentHook hook, Exception e) {
    // Hooks must never crash the agent — log and continue
    System.err.println(
        "[HookRegistry] Hook "
            + hook.getClass().getSimpleName()
            + " threw during '"
            + event
            + "': "
            + e.getMessage());
  }
}
