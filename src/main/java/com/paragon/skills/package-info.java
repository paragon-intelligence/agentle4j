/**
 * Agent Skills for extending agent capabilities.
 *
 * <p>This package provides a modular system for packaging and loading agent capabilities as
 * reusable "skills". Skills follow the <b>progressive disclosure</b> pattern: the agent sees a
 * concise catalog (name + description) of available skills in its system prompt, and can call the
 * {@link com.paragon.skills.SkillReaderTool read_skill} tool to load full instructions on demand.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.paragon.skills.Skill} - Value object for skill definition (name, description,
 *       instructions, resources)
 *   <li>{@link com.paragon.skills.SkillStore} - Registry for managing available skills
 *   <li>{@link com.paragon.skills.SkillReaderTool} - Tool that loads full skill content on demand
 *   <li>{@link com.paragon.skills.SkillProvider} - Interface for loading skills from various
 *       sources
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
 *     .build();
 *
 * // Add to agent — skill catalog is shown in prompt,
 * // full content is loaded on-demand via read_skill tool
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
 * @see com.paragon.skills.SkillReaderTool
 * @since 1.0
 */
package com.paragon.skills;
