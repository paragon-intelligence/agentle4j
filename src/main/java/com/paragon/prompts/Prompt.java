package com.paragon.prompts;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an immutable text prompt that can contain template expressions.
 *
 * <p>A Prompt instance manages text content that can include various template
 * features like variable placeholders ({@code {{variable_name}}}), conditional blocks
 * ({@code {{#if condition}}...{{/if}}}), and iteration blocks
 * ({@code {{#each items}}...{{/each}}}).
 *
 * <p>This class is immutable and thread-safe. All template processing methods
 * return new Prompt instances.
 *
 * <h2>Template Syntax</h2>
 * <ul>
 *   <li>Variable interpolation: {@code {{variable_name}}}</li>
 *   <li>Nested property access: {@code {{object.property}}}</li>
 *   <li>Conditional blocks: {@code {{#if condition}}content{{/if}}}</li>
 *   <li>Iteration blocks: {@code {{#each items}}{{this.property}}{{/each}}}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple variable replacement
 * Prompt prompt = Prompt.of("Hello, {{name}}!");
 * Prompt compiled = prompt.compile(Map.of("name", "World"));
 * // Result: "Hello, World!"
 *
 * // Conditional blocks
 * Prompt prompt = Prompt.of("{{#if showGreeting}}Hello{{/if}}, {{name}}!");
 * Prompt compiled = prompt.compile(Map.of("showGreeting", true, "name", "World"));
 * // Result: "Hello, World!"
 *
 * // Iteration
 * Prompt prompt = Prompt.of("Items: {{#each items}}- {{this.name}}\n{{/each}}");
 * List<Map<String, Object>> items = List.of(
 *     Map.of("name", "Apple"),
 *     Map.of("name", "Banana")
 * );
 * Prompt compiled = prompt.compile(Map.of("items", items));
 * // Result: "Items: - Apple\n- Banana\n"
 * }</pre>
 *
 * @author Agentle Framework
 * @since 1.0
 */
public final class Prompt {

  // Precompiled patterns for better performance and security (avoid ReDoS)
  private static final Pattern VARIABLE_PATTERN =
          Pattern.compile("\\{\\{([^#/}][^}]*?)\\}\\}");

  private static final Pattern CONDITIONAL_PATTERN =
          Pattern.compile("\\{\\{#if\\s+([^}]+)\\}\\}(.*?)\\{\\{/if\\}\\}", Pattern.DOTALL);

  private static final Pattern EACH_PATTERN =
          Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}(.*?)\\{\\{/each\\}\\}", Pattern.DOTALL);

  private static final Pattern IF_MARKER = Pattern.compile("\\{\\{#if");
  private static final Pattern EACH_MARKER = Pattern.compile("\\{\\{#each");

  private static final int MAX_TEMPLATE_DEPTH = 100;
  private static final int MAX_ITERATIONS = 10_000;

  private final String content;
  private final boolean compiled;

  /**
   * Private constructor - use factory methods instead.
   *
   * @param content  the prompt content
   * @param compiled whether this prompt has been compiled
   */
  private Prompt(String content, boolean compiled) {
    this.content = Objects.requireNonNull(content, "content must not be null");
    this.compiled = compiled;
  }

  /**
   * Creates a new Prompt from text content.
   *
   * @param text the template text
   * @return a new Prompt instance
   * @throws NullPointerException if text is null
   */
  public static Prompt of(String text) {
    return new Prompt(text, false);
  }

  /**
   * Creates a new Prompt from text content (alias for {@link #of(String)}).
   *
   * @param text the template text
   * @return a new Prompt instance
   * @throws NullPointerException if text is null
   */
  public static Prompt fromText(String text) {
    return of(text);
  }

  /**
   * Creates a new empty Prompt.
   *
   * @return an empty Prompt instance
   */
  public static Prompt empty() {
    return new Prompt("", false);
  }

  private static String getRootKey(String path) {
    int dotIndex = path.indexOf('.');
    return dotIndex > 0 ? path.substring(0, dotIndex).trim() : path.trim();
  }

  private static boolean isTruthy(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof String s) {
      return !s.isEmpty();
    }
    if (value instanceof Number n) {
      return n.doubleValue() != 0;
    }
    if (value instanceof Collection<?> c) {
      return !c.isEmpty();
    }
    if (value instanceof Map<?, ?> m) {
      return !m.isEmpty();
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  private static Iterable<?> toIterable(Object obj) {
    if (obj instanceof Iterable) {
      return (Iterable<?>) obj;
    }
    if (obj instanceof Object[] array) {
      return Arrays.asList(array);
    }
    if (obj instanceof Map<?, ?> map) {
      return map.entrySet();
    }
    return null;
  }

  private static String escapeReplacement(String text) {
    // Escape special characters in replacement string
    return Matcher.quoteReplacement(text);
  }

  /**
   * Gets the raw content of this prompt.
   *
   * @return the prompt content
   */
  public String content() {
    return content;
  }

  /**
   * Gets the content as text (alias for {@link #content()}).
   *
   * @return the prompt content
   */
  public String text() {
    return content;
  }

  /**
   * Returns whether this prompt has been compiled.
   *
   * @return true if compiled, false otherwise
   */
  public boolean isCompiled() {
    return compiled;
  }

  /**
   * Compiles this prompt by processing all template expressions.
   *
   * <p>This method supports a Handlebars-like templating system with:
   * <ul>
   *   <li>Variable interpolation: {@code {{variable_name}}}</li>
   *   <li>Conditional blocks: {@code {{#if condition}}...{{/if}}}</li>
   *   <li>Iteration blocks: {@code {{#each items}}...{{/each}}}</li>
   *   <li>Nested property access: {@code {{object.property}}}</li>
   * </ul>
   *
   * @param context a map with values for template variables
   * @return a new compiled Prompt instance
   * @throws NullPointerException if context is null
   * @throws TemplateException    if the template syntax is invalid or processing fails
   */
  public Prompt compile(Map<String, Object> context) {
    Objects.requireNonNull(context, "context must not be null");

    // Create defensive copy to prevent external modification during processing
    Map<String, Object> safeContext = new HashMap<>(context);

    // Fast path for simple templates without control structures
    if (!hasControlStructures()) {
      return simpleCompile(safeContext);
    }

    // Full template processing
    String result = content;

    // Process conditional blocks
    result = processConditionals(result, safeContext, 0);

    // Process iteration blocks
    result = processIterations(result, safeContext, 0);

    // Process simple variable interpolation
    result = processVariables(result, safeContext);

    return new Prompt(result, true);
  }

  /**
   * Compiles this prompt with variable key-value pairs.
   *
   * <p>This is a convenience method that accepts alternating keys and values,
   * providing a cleaner syntax for simple cases.
   *
   * <p>Example usage:
   * <pre>{@code
   * Prompt result = prompt.compile("name", "Alice", "age", 30, "active", true);
   * }</pre>
   *
   * @param firstKey the first variable name
   * @param firstValue the first variable value
   * @param rest additional alternating keys (String) and values (Object)
   * @return a new compiled Prompt instance
   * @throws IllegalArgumentException if rest has an odd number of arguments
   *                                  or if a key is not a String
   * @throws TemplateException if the template syntax is invalid or processing fails
   */
  public Prompt compile(String firstKey, Object firstValue, Object... rest) {
    Objects.requireNonNull(firstKey, "firstKey must not be null");
    
    if (rest.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Must provide an even number of additional arguments as key-value pairs");
    }

    Map<String, Object> context = new HashMap<>();
    context.put(firstKey, firstValue);
    
    for (int i = 0; i < rest.length; i += 2) {
      Object key = rest[i];
      if (!(key instanceof String)) {
        throw new IllegalArgumentException(
            "Keys must be strings, got: " + (key == null ? "null" : key.getClass().getName()));
      }
      context.put((String) key, rest[i + 1]);
    }
    return compile(context);
  }

  /**
   * Compiles this prompt using a fluent builder for the context.
   *
   * <p>Example usage:
   * <pre>{@code
   * Prompt result = prompt.compile()
   *     .with("name", "World")
   *     .with("count", 42)
   *     .build();
   * }</pre>
   *
   * @return a new CompileBuilder for fluent context construction
   */
  public CompileBuilder compile() {
    return new CompileBuilder(this);
  }

  /**
   * Compiles only template variables that exist in the prompt content.
   *
   * <p>Variables in the context that don't have corresponding placeholders
   * in the prompt are ignored. This is useful when you have a large context
   * but want to only apply relevant values.
   *
   * @param context a map with values for template variables
   * @return a new compiled Prompt instance
   * @throws NullPointerException if context is null
   */
  public Prompt compileIf(Map<String, Object> context) {
    Objects.requireNonNull(context, "context must not be null");

    Set<String> variablesInPrompt = extractVariableNames();

    // Filter context to only include variables that exist in the prompt
    Map<String, Object> filteredContext = new HashMap<>();
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (variablesInPrompt.contains(entry.getKey())) {
        filteredContext.put(entry.getKey(), entry.getValue());
      }
    }

    return compile(filteredContext);
  }

  /**
   * Extracts all variable names referenced in this prompt.
   *
   * @return a set of variable names (root keys only)
   */
  public Set<String> extractVariableNames() {
    Set<String> variables = new HashSet<>();

    // Extract from variable placeholders
    Matcher varMatcher = VARIABLE_PATTERN.matcher(content);
    while (varMatcher.find()) {
      String varName = varMatcher.group(1).trim();
      variables.add(getRootKey(varName));
    }

    // Extract from conditional blocks
    Matcher ifMatcher = Pattern.compile("\\{\\{#if\\s+([^}]+)\\}\\}").matcher(content);
    while (ifMatcher.find()) {
      String conditionVar = ifMatcher.group(1).trim();
      variables.add(getRootKey(conditionVar));
    }

    // Extract from iteration blocks
    Matcher eachMatcher = Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}").matcher(content);
    while (eachMatcher.find()) {
      String itemsVar = eachMatcher.group(1).trim();
      variables.add(getRootKey(itemsVar));
    }

    return Collections.unmodifiableSet(variables);
  }

  /**
   * Concatenates this prompt with another prompt.
   *
   * @param other the prompt to append
   * @return a new Prompt with concatenated content
   * @throws NullPointerException if other is null
   */
  public Prompt append(Prompt other) {
    Objects.requireNonNull(other, "other must not be null");
    return new Prompt(this.content + other.content, false);
  }

  /**
   * Concatenates this prompt with a string.
   *
   * @param text the text to append
   * @return a new Prompt with concatenated content
   * @throws NullPointerException if text is null
   */
  public Prompt append(String text) {
    Objects.requireNonNull(text, "text must not be null");
    return new Prompt(this.content + text, false);
  }

  /**
   * Returns a substring of this prompt's content.
   *
   * @param beginIndex the beginning index, inclusive
   * @param endIndex   the ending index, exclusive
   * @return a new Prompt with the substring
   * @throws IndexOutOfBoundsException if indices are invalid
   */
  public Prompt substring(int beginIndex, int endIndex) {
    return new Prompt(content.substring(beginIndex, endIndex), compiled);
  }

  /**
   * Returns a substring of this prompt's content from the given index.
   *
   * @param beginIndex the beginning index, inclusive
   * @return a new Prompt with the substring
   * @throws IndexOutOfBoundsException if index is invalid
   */
  public Prompt substring(int beginIndex) {
    return new Prompt(content.substring(beginIndex), compiled);
  }

  // ========== Private Helper Methods ==========

  /**
   * Returns the character at the specified index.
   *
   * @param index the index of the character
   * @return the character at the specified index
   * @throws IndexOutOfBoundsException if index is invalid
   */
  public char charAt(int index) {
    return content.charAt(index);
  }

  /**
   * Returns the length of this prompt's content.
   *
   * @return the length in characters
   */
  public int length() {
    return content.length();
  }

  /**
   * Checks if this prompt's content is empty.
   *
   * @return true if content is empty, false otherwise
   */
  public boolean isEmpty() {
    return content.isEmpty();
  }

  /**
   * Checks if this prompt's content is blank (empty or only whitespace).
   *
   * @return true if content is blank, false otherwise
   */
  public boolean isBlank() {
    return content.isBlank();
  }

  private boolean hasControlStructures() {
    return IF_MARKER.matcher(content).find() || EACH_MARKER.matcher(content).find();
  }

  private Prompt simpleCompile(Map<String, Object> context) {
    StringBuffer result = new StringBuffer();
    Matcher matcher = VARIABLE_PATTERN.matcher(content);

    while (matcher.find()) {
      String varName = matcher.group(1).trim();
      Object value = getNestedValue(varName, context);
      String replacement = value != null ? escapeReplacement(String.valueOf(value)) : matcher.group(0);
      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);

    return new Prompt(result.toString(), true);
  }


  private String processConditionals(String template, Map<String, Object> context, int depth) {
    if (depth > MAX_TEMPLATE_DEPTH) {
      throw new TemplateException("Maximum template nesting depth exceeded: " + MAX_TEMPLATE_DEPTH);
    }

    StringBuilder result = new StringBuilder();
    int pos = 0;

    while (pos < template.length()) {
      int ifStart = template.indexOf("{{#if ", pos);
      if (ifStart == -1) {
        result.append(template.substring(pos));
        break;
      }

      // Append content before the if block
      result.append(template, pos, ifStart);

      // Find the end of the opening tag
      int tagEnd = template.indexOf("}}", ifStart);
      if (tagEnd == -1) {
        result.append(template.substring(ifStart));
        break;
      }

      String conditionVar = template.substring(ifStart + 6, tagEnd).trim();
      int blockStart = tagEnd + 2;

      // Find matching {{/if}} using balanced matching
      int[] matchResult = findMatchingBlock(template, blockStart, "if");
      if (matchResult == null) {
        result.append(template.substring(ifStart));
        break;
      }

      int blockEnd = matchResult[0];
      int closeEnd = matchResult[1];
      String blockContent = template.substring(blockStart, blockEnd);

      Object value = getNestedValue(conditionVar, context);
      if (isTruthy(value)) {
        // Recursively process nested conditionals in the block content
        String processedBlock = processConditionals(blockContent, context, depth + 1);
        result.append(processedBlock);
      }

      pos = closeEnd;
    }

    return result.toString();
  }

  private String processIterations(String template, Map<String, Object> context, int depth) {
    if (depth > MAX_TEMPLATE_DEPTH) {
      throw new TemplateException("Maximum template nesting depth exceeded: " + MAX_TEMPLATE_DEPTH);
    }

    StringBuilder result = new StringBuilder();
    int pos = 0;

    while (pos < template.length()) {
      int eachStart = template.indexOf("{{#each ", pos);
      if (eachStart == -1) {
        result.append(template.substring(pos));
        break;
      }

      // Append content before the each block
      result.append(template, pos, eachStart);

      // Find the end of the opening tag
      int tagEnd = template.indexOf("}}", eachStart);
      if (tagEnd == -1) {
        result.append(template.substring(eachStart));
        break;
      }

      String itemsVar = template.substring(eachStart + 8, tagEnd).trim();
      int blockStart = tagEnd + 2;

      // Find matching {{/each}} using balanced matching
      int[] matchResult = findMatchingBlock(template, blockStart, "each");
      if (matchResult == null) {
        result.append(template.substring(eachStart));
        break;
      }

      int blockEnd = matchResult[0];
      int closeEnd = matchResult[1];
      String blockContent = template.substring(blockStart, blockEnd);

      Object items = getNestedValue(itemsVar, context);
      String replacement = evaluateIteration(items, blockContent, context);
      result.append(replacement);

      pos = closeEnd;
    }

    return result.toString();
  }

  /**
   * Finds the matching closing tag for a block, handling nested blocks.
   * 
   * @param template the template string
   * @param startPos position after the opening tag
   * @param blockType "if" or "each"
   * @return int array with [blockEndPos, closeTagEndPos] or null if not found
   */
  private int[] findMatchingBlock(String template, int startPos, String blockType) {
    String openTag = "{{#" + blockType;
    String closeTag = "{{/" + blockType + "}}";
    
    int nestLevel = 1;
    int pos = startPos;
    
    while (pos < template.length() && nestLevel > 0) {
      int nextOpen = template.indexOf(openTag, pos);
      int nextClose = template.indexOf(closeTag, pos);
      
      if (nextClose == -1) {
        return null; // No matching close tag
      }
      
      if (nextOpen != -1 && nextOpen < nextClose) {
        // Found another opening tag before the close
        nestLevel++;
        pos = nextOpen + openTag.length();
      } else {
        // Found a close tag
        nestLevel--;
        if (nestLevel == 0) {
          return new int[] { nextClose, nextClose + closeTag.length() };
        }
        pos = nextClose + closeTag.length();
      }
    }
    
    return null;
  }

  private String evaluateIteration(Object items, String itemTemplate, Map<String, Object> context) {
    if (items == null) {
      return "";
    }

    Iterable<?> iterable = toIterable(items);
    if (iterable == null) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    int iterationCount = 0;

    for (Object item : iterable) {
      if (++iterationCount > MAX_ITERATIONS) {
        throw new TemplateException("Maximum iteration count exceeded: " + MAX_ITERATIONS);
      }

      // Create temporary context with 'this' referring to current item
      Map<String, Object> tempContext = new HashMap<>(context);
      tempContext.put("this", item);

      // Process nested each blocks first
      String processed = processIterations(itemTemplate, tempContext, 0);
      
      // Then process variables
      processed = processVariables(processed, tempContext);
      result.append(processed);
    }

    return result.toString();
  }

  private String processVariables(String template, Map<String, Object> context) {
    StringBuffer result = new StringBuffer();
    Matcher matcher = VARIABLE_PATTERN.matcher(template);

    while (matcher.find()) {
      String varName = matcher.group(1).trim();
      Object value = getNestedValue(varName, context);
      String replacement = value != null ? escapeReplacement(String.valueOf(value)) : "";
      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);

    return result.toString();
  }

  @SuppressWarnings("unchecked")
  private Object getNestedValue(String path, Map<String, Object> context) {
    if (path == null || path.isEmpty()) {
      return null;
    }

    // Handle special 'this' keyword
    if ("this".equals(path)) {
      return context.get("this");
    }

    String[] keys = path.split("\\.");
    Object current = context;

    for (String key : keys) {
      key = key.trim();
      if (key.isEmpty()) {
        continue;
      }

      if (current instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) current;
        if (!map.containsKey(key)) {
          return null;
        }
        current = map.get(key);
      } else if (current != null) {
        // Try to access as a bean property via reflection (with caching for performance)
        current = getPropertyValue(current, key);
        if (current == null) {
          return null;
        }
      } else {
        return null;
      }
    }

    return current;
  }

  private Object getPropertyValue(Object obj, String propertyName) {
    if (obj == null) {
      return null;
    }

    try {
      // Try getter method first (getProperty or isProperty for booleans)
      String capitalizedName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

      // Try getProperty()
      try {
        var method = obj.getClass().getMethod("get" + capitalizedName);
        return method.invoke(obj);
      } catch (NoSuchMethodException ignored) {
        // Try isProperty() for booleans
      }

      // Try isProperty()
      try {
        var method = obj.getClass().getMethod("is" + capitalizedName);
        return method.invoke(obj);
      } catch (NoSuchMethodException ignored) {
        // Try direct field access
      }

      // Try record accessor (propertyName())
      try {
        var method = obj.getClass().getMethod(propertyName);
        return method.invoke(obj);
      } catch (NoSuchMethodException ignored) {
        // Property not found
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }

  // ========== Object Methods ==========

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Prompt other)) return false;
    return compiled == other.compiled && content.equals(other.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, compiled);
  }

  @Override
  public String toString() {
    return content;
  }

  // ========== Nested Classes ==========

  /**
   * Fluent builder for compiling prompts with context values.
   */
  public static final class CompileBuilder {
    private final Prompt prompt;
    private final Map<String, Object> context;

    private CompileBuilder(Prompt prompt) {
      this.prompt = prompt;
      this.context = new HashMap<>();
    }

    /**
     * Adds a variable to the compilation context.
     *
     * @param key   the variable name
     * @param value the variable value
     * @return this builder for chaining
     */
    public CompileBuilder with(String key, Object value) {
      context.put(key, value);
      return this;
    }

    /**
     * Adds all entries from a map to the compilation context.
     *
     * @param values the map of values to add
     * @return this builder for chaining
     */
    public CompileBuilder withAll(Map<String, Object> values) {
      context.putAll(values);
      return this;
    }

    /**
     * Compiles the prompt with the accumulated context.
     *
     * @return a new compiled Prompt instance
     */
    public Prompt build() {
      return prompt.compile(context);
    }

    /**
     * Compiles only matching variables with the accumulated context.
     *
     * @return a new compiled Prompt instance
     */
    public Prompt buildIf() {
      return prompt.compileIf(context);
    }
  }

  /**
   * Exception thrown when template processing fails.
   */
  public static class TemplateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TemplateException(String message) {
      super(message);
    }

    public TemplateException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}