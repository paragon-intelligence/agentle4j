/**
 * Agent Skills for extending agent capabilities.
 *
 * <p>This package provides a modular system for packaging and loading agent capabilities
 * as reusable "skills". Skills follow the progressive disclosure pattern: only metadata
 * is loaded upfront, while instructions and tools are loaded on-demand when invoked.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.paragon.skills.Skill} - Value object for skill definition (name, description, instructions, tools)
 *   <li>{@link com.paragon.skills.SkillTool} - FunctionTool wrapper for on-demand skill invocation
 *   <li>{@link com.paragon.skills.SkillStore} - Registry for managing available skills
 *   <li>{@link com.paragon.skills.SkillProvider} - Interface for loading skills from various sources
 * </ul>
 *
 * <h2>Providers</h2>
 *
 * <ul>
 *   <li>{@link com.paragon.skills.FilesystemSkillProvider} - Load from SKILL.md files on disk
 *   <li>{@link com.paragon.skills.UrlSkillProvider} - Load from remote URLs
 *   <li>{@link com.paragon.skills.InMemorySkillProvider} - Store code-defined skills
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Define a skill in code
 * Skill pdfSkill = Skill.builder()
 *     .name("pdf-processor")
 *     .description("Process PDF files, extract text, fill forms")
 *     .instructions("You are a PDF processing expert...")
 *     .addTool(new ExtractTextTool())
 *     .build();
 *
 * // Add to agent
 * Agent agent = Agent.builder()
 *     .name("DocumentAssistant")
 *     .addSkill(pdfSkill)
 *     .build();
 *
 * // Or load from filesystem
 * SkillProvider provider = FilesystemSkillProvider.create(Path.of("./skills"));
 * Skill skill = provider.provide("pdf-processor");
 * }</pre>
 *
 * <h2>SKILL.md Format</h2>
 *
 * <pre>
 * ---
 * name: pdf-processor
 * description: Process PDF files, extract text, fill forms.
 * ---
 *
 * # PDF Processing
 *
 * You are a PDF processing expert...
 * </pre>
 *
 * @see com.paragon.skills.Skill
 * @see com.paragon.skills.SkillProvider
 * @since 1.0
 */
package com.paragon.skills;
