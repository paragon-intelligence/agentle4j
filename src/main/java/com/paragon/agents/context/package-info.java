/**
 * Context management for AI agents.
 *
 * <p>This package provides strategies for managing conversation context length, ensuring that agent
 * interactions stay within model token limits.
 *
 * <h2>Key Components</h2>
 *
 * <ul>
 *   <li>{@link com.paragon.agents.context.ContextManagementConfig} - Configuration for context
 *       management
 *   <li>{@link com.paragon.agents.context.TokenCounter} - Interface for counting tokens
 *   <li>{@link com.paragon.agents.context.SimpleTokenCounter} - Character-based token estimation
 *   <li>{@link com.paragon.agents.context.ContextWindowStrategy} - Strategy interface for context
 *       management
 *   <li>{@link com.paragon.agents.context.SlidingWindowStrategy} - Removes oldest messages
 *   <li>{@link com.paragon.agents.context.SummarizationStrategy} - Summarizes older messages
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .model("openai/gpt-4o")
 *     .responder(responder)
 *     .contextManagement(ContextManagementConfig.builder()
 *         .strategy(new SlidingWindowStrategy())
 *         .maxTokens(4000)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @since 1.0
 */
package com.paragon.agents.context;
