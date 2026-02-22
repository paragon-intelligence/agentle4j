package com.paragon.prompts;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents an immutable text prompt that can contain template expressions.
 *
 * <p>A Prompt instance manages text content that can include various template features like
 * variable placeholders ({@code {{variable_name}}}), conditional blocks ({@code {{#if
 * condition}}...{{/if}}}), and iteration blocks ({@code {{#each items}}...{{/each}}}).
 *
 * <p>This class is immutable and thread-safe. All template processing methods return new Prompt
 * instances.
 *
 * <h2>Builder Features</h2>
 *
 * <ul>
 *   <li><b>Order Independent:</b> All builder methods can be called in any order
 *   <li><b>No Dependencies:</b> No method requires another method to be called first
 *   <li><b>Additive:</b> Methods add to existing configuration rather than replacing
 *   <li><b>Multi-language:</b> Supports English (US) and Portuguese (BR)
 * </ul>
 *
 * <h2>Builder Usage Examples</h2>
 *
 * <pre>{@code
 * // Methods can be called in any order - these produce equivalent results:
 * Prompt prompt1 = Prompt.builder()
 *     .task("Analyze the data")
 *     .role("data analyst")
 *     .withChainOfThought()
 *     .build();
 *
 * Prompt prompt2 = Prompt.builder()
 *     .withChainOfThought()
 *     .role("data analyst")
 *     .task("Analyze the data")
 *     .build();
 *
 * // Multi-language support
 * Prompt ptBrPrompt = Prompt.builder()
 *     .language(Language.PT_BR)
 *     .role("analista de dados")
 *     .task("Analise os dados de vendas")
 *     .build();
 * }</pre>
 *
 * @author Agentle Framework
 * @since 1.0
 */
public final class Prompt {

  // Precompiled patterns for better performance and security (avoid ReDoS)
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^#/}][^}]*?)\\}\\}");
  private static final Pattern IF_MARKER = Pattern.compile("\\{\\{#if");
  private static final Pattern EACH_MARKER = Pattern.compile("\\{\\{#each");

  private static final int MAX_TEMPLATE_DEPTH = 100;
  private static final int MAX_ITERATIONS = 10_000;

  private final String content;
  private final boolean compiled;

  private Prompt(String content, boolean compiled) {
    this.content = Objects.requireNonNull(content, "content must not be null");
    this.compiled = compiled;
  }

  /** Creates a new Prompt from text content. */
  public static Prompt of(String text) {
    return new Prompt(text, false);
  }

  /** Creates a new Prompt from text content (alias for {@link #of(String)}). */
  public static Prompt fromText(String text) {
    return of(text);
  }

  /** Creates a new empty Prompt. */
  public static Prompt empty() {
    return new Prompt("", false);
  }

  /**
   * Creates a new Builder for constructing prompts with best practices.
   *
   * <p><b>Order Independence:</b> All builder methods can be called in any order. The final prompt
   * is assembled when {@link Builder#build()} is called.
   *
   * <p><b>No Dependencies:</b> No method requires another method to be called. Each method is
   * independent and optional.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Creates a Builder pre-configured for a specific use case. */
  public static Builder builder(PromptTemplate template) {
    return template.configure(new Builder());
  }

  /**
   * Creates a Builder with a specific language.
   *
   * @param language the language for the prompt
   * @return a new Builder with the language set
   */
  public static Builder builder(Language language) {
    return new Builder().language(language);
  }

  // ==================== STATIC FACTORY METHODS ====================
  // All factory methods return independent Builders - no order requirements

  /** Creates a Builder starting with a specific task instruction. */
  public static Builder forTask(String taskInstruction) {
    return new Builder().task(taskInstruction);
  }

  /** Creates a Builder configured for extracting structured data. */
  public static Builder forExtraction(String whatToExtract) {
    return new Builder()
        .addRole("data extraction specialist")
        .task("Extract the following information: " + whatToExtract)
        .outputAs(OutputFormat.JSON)
        .instruct("Only extract explicitly mentioned information")
        .instruct("Use null for fields where information is not available");
  }

  /** Creates a Builder configured for classification tasks. */
  public static Builder forClassification(String... categories) {
    return new Builder()
        .addRole("classification expert")
        .task("Classify the input into one of these categories: " + String.join(", ", categories))
        .outputAs(OutputFormat.PLAIN_TEXT)
        .instruct("Respond with only the category name, nothing else")
        .require("Choose the single most appropriate category");
  }

  /** Creates a Builder configured for multi-step reasoning problems. */
  public static Builder forReasoning(String problem) {
    return new Builder().task(problem).withChainOfThought().instruct("Show your work clearly");
  }

  /** Creates a Builder configured for code generation. */
  public static Builder forCode(String language) {
    return new Builder()
        .addRole("expert " + language + " developer")
        .instructions(
            "Write clean, well-documented code",
            "Follow " + language + " best practices and conventions",
            "Include comments explaining key logic",
            "Handle edge cases appropriately");
  }

  /** Creates a Builder configured for RAG (Retrieval Augmented Generation). */
  public static Builder forRAG() {
    return new Builder()
        .addRole("research assistant")
        .instructions(
            "Base your answer only on the provided documents",
            "Cite which document contains the relevant information",
            "If the answer cannot be found in the documents, say so clearly")
        .constraint("Do not make claims not supported by the provided documents");
  }

  /** Creates a Builder configured for conversational AI. */
  public static Builder forConversation() {
    return new Builder()
        .addRole("helpful assistant")
        .addPersonality("friendly", "helpful", "conversational")
        .instruct("Engage naturally in conversation")
        .instruct("Ask clarifying questions when needed");
  }

  /** Creates a Builder configured for translation tasks. */
  public static Builder forTranslation(String sourceLanguage, String targetLanguage) {
    return new Builder()
        .addRole("professional translator fluent in " + sourceLanguage + " and " + targetLanguage)
        .task("Translate the following text from " + sourceLanguage + " to " + targetLanguage)
        .instructions(
            "Maintain the original meaning, tone, and intent",
            "Use natural, idiomatic expressions in " + targetLanguage,
            "Preserve formatting and structure");
  }

  /** Creates a Builder configured for summarization tasks. */
  public static Builder forSummarization() {
    return new Builder()
        .addRole("professional summarizer")
        .task("Summarize the following content")
        .instructions(
            "Capture the main points and key information",
            "Use clear, concise language",
            "Maintain the original meaning and intent")
        .concise();
  }

  /** Creates a Builder configured for question answering. */
  public static Builder forQA() {
    return new Builder()
        .addRole("knowledgeable assistant")
        .task("Answer the following question")
        .instruct("Provide accurate and helpful information")
        .instruct("If unsure, acknowledge uncertainty");
  }

  /** Creates a Builder configured for creative writing. */
  public static Builder forCreativeWriting() {
    return new Builder()
        .addRole("creative writer")
        .addPersonality("imaginative", "expressive", "engaging")
        .instruct("Use vivid descriptions and varied sentence structures");
  }

  /** Creates a Builder configured for data analysis. */
  public static Builder forDataAnalysis() {
    return new Builder()
        .addRole("data analyst")
        .addExpertise("statistical analysis", "data interpretation", "visualization")
        .withChainOfThought()
        .instruct("Identify patterns and insights in the data")
        .instruct("Support conclusions with specific data points");
  }

  // ==================== CORE PROMPT METHODS ====================

  public String content() {
    return content;
  }

  public String text() {
    return content;
  }

  public boolean isCompiled() {
    return compiled;
  }

  public Prompt compile(Map<String, Object> context) {
    Objects.requireNonNull(context, "context must not be null");
    Map<String, Object> safeContext = new HashMap<>(context);

    if (!hasControlStructures()) {
      return simpleCompile(safeContext);
    }

    String result = content;
    result = processConditionals(result, safeContext, 0);
    result = processIterations(result, safeContext, 0);
    result = processVariables(result, safeContext);

    return new Prompt(result, true);
  }

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

  public CompileBuilder compile() {
    return new CompileBuilder(this);
  }

  public Prompt compileIf(Map<String, Object> context) {
    Objects.requireNonNull(context, "context must not be null");
    Set<String> variablesInPrompt = extractVariableNames();
    Map<String, Object> filteredContext = new HashMap<>();
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (variablesInPrompt.contains(entry.getKey())) {
        filteredContext.put(entry.getKey(), entry.getValue());
      }
    }
    return compile(filteredContext);
  }

  public Set<String> extractVariableNames() {
    Set<String> variables = new HashSet<>();
    Matcher varMatcher = VARIABLE_PATTERN.matcher(content);
    while (varMatcher.find()) {
      String varName = varMatcher.group(1).trim();
      variables.add(getRootKey(varName));
    }
    Matcher ifMatcher = Pattern.compile("\\{\\{#if\\s+([^}]+)\\}\\}").matcher(content);
    while (ifMatcher.find()) {
      variables.add(getRootKey(ifMatcher.group(1).trim()));
    }
    Matcher eachMatcher = Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}").matcher(content);
    while (eachMatcher.find()) {
      variables.add(getRootKey(eachMatcher.group(1).trim()));
    }
    return Collections.unmodifiableSet(variables);
  }

  public Prompt append(Prompt other) {
    Objects.requireNonNull(other, "other must not be null");
    return new Prompt(this.content + other.content, false);
  }

  public Prompt append(String text) {
    Objects.requireNonNull(text, "text must not be null");
    return new Prompt(this.content + text, false);
  }

  public Prompt substring(int beginIndex, int endIndex) {
    return new Prompt(content.substring(beginIndex, endIndex), compiled);
  }

  public Prompt substring(int beginIndex) {
    return new Prompt(content.substring(beginIndex), compiled);
  }

  public char charAt(int index) {
    return content.charAt(index);
  }

  public int length() {
    return content.length();
  }

  public boolean isEmpty() {
    return content.isEmpty();
  }

  public boolean isBlank() {
    return content.isBlank();
  }

  // ==================== PRIVATE HELPERS ====================

  private static String getRootKey(String path) {
    int dotIndex = path.indexOf('.');
    return dotIndex > 0 ? path.substring(0, dotIndex).trim() : path.trim();
  }

  private static boolean isTruthy(Object value) {
    if (value == null) return false;
    if (value instanceof Boolean b) return b;
    if (value instanceof String s) return !s.isEmpty();
    if (value instanceof Number n) return n.doubleValue() != 0;
    if (value instanceof Collection<?> c) return !c.isEmpty();
    if (value instanceof Map<?, ?> m) return !m.isEmpty();
    return true;
  }

  @SuppressWarnings("unchecked")
  private static Iterable<?> toIterable(Object obj) {
    if (obj instanceof Iterable) return (Iterable<?>) obj;
    if (obj instanceof Object[] array) return Arrays.asList(array);
    if (obj instanceof Map<?, ?> map) return map.entrySet();
    return null;
  }

  private static String escapeReplacement(String text) {
    return Matcher.quoteReplacement(text);
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
      String replacement =
          value != null ? escapeReplacement(String.valueOf(value)) : matcher.group(0);
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

      result.append(template, pos, ifStart);
      int tagEnd = template.indexOf("}}", ifStart);
      if (tagEnd == -1) {
        result.append(template.substring(ifStart));
        break;
      }

      String conditionVar = template.substring(ifStart + 6, tagEnd).trim();
      int blockStart = tagEnd + 2;
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
        result.append(processConditionals(blockContent, context, depth + 1));
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

      result.append(template, pos, eachStart);
      int tagEnd = template.indexOf("}}", eachStart);
      if (tagEnd == -1) {
        result.append(template.substring(eachStart));
        break;
      }

      String itemsVar = template.substring(eachStart + 8, tagEnd).trim();
      int blockStart = tagEnd + 2;
      int[] matchResult = findMatchingBlock(template, blockStart, "each");
      if (matchResult == null) {
        result.append(template.substring(eachStart));
        break;
      }

      int blockEnd = matchResult[0];
      int closeEnd = matchResult[1];
      String blockContent = template.substring(blockStart, blockEnd);

      Object items = getNestedValue(itemsVar, context);
      result.append(evaluateIteration(items, blockContent, context));
      pos = closeEnd;
    }
    return result.toString();
  }

  private int[] findMatchingBlock(String template, int startPos, String blockType) {
    String openTag = "{{#" + blockType;
    String closeTag = "{{/" + blockType + "}}";
    int nestLevel = 1;
    int pos = startPos;

    while (pos < template.length() && nestLevel > 0) {
      int nextOpen = template.indexOf(openTag, pos);
      int nextClose = template.indexOf(closeTag, pos);

      if (nextClose == -1) return null;

      if (nextOpen != -1 && nextOpen < nextClose) {
        nestLevel++;
        pos = nextOpen + openTag.length();
      } else {
        nestLevel--;
        if (nestLevel == 0) {
          return new int[] {nextClose, nextClose + closeTag.length()};
        }
        pos = nextClose + closeTag.length();
      }
    }
    return null;
  }

  private String evaluateIteration(Object items, String itemTemplate, Map<String, Object> context) {
    if (items == null) return "";
    Iterable<?> iterable = toIterable(items);
    if (iterable == null) return "";

    StringBuilder result = new StringBuilder();
    int iterationCount = 0;

    for (Object item : iterable) {
      if (++iterationCount > MAX_ITERATIONS) {
        throw new TemplateException("Maximum iteration count exceeded: " + MAX_ITERATIONS);
      }
      Map<String, Object> tempContext = new HashMap<>(context);
      tempContext.put("this", item);
      String processed = processIterations(itemTemplate, tempContext, 0);
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
    if (path == null || path.isEmpty()) return null;
    if ("this".equals(path)) return context.get("this");

    String[] keys = path.split("\\.");
    Object current = context;

    for (String key : keys) {
      key = key.trim();
      if (key.isEmpty()) continue;

      if (current instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) current;
        if (!map.containsKey(key)) return null;
        current = map.get(key);
      } else if (current != null) {
        current = getPropertyValue(current, key);
        if (current == null) return null;
      } else {
        return null;
      }
    }
    return current;
  }

  private Object getPropertyValue(Object obj, String propertyName) {
    if (obj == null) return null;
    try {
      String capitalizedName =
          Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      try {
        return obj.getClass().getMethod("get" + capitalizedName).invoke(obj);
      } catch (NoSuchMethodException ignored) {
      }
      try {
        return obj.getClass().getMethod("is" + capitalizedName).invoke(obj);
      } catch (NoSuchMethodException ignored) {
      }
      try {
        return obj.getClass().getMethod(propertyName).invoke(obj);
      } catch (NoSuchMethodException ignored) {
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

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

  // ==================== ENUMS ====================

  /** Supported languages for prompt generation. */
  public enum Language {
    EN_US("en-US", "English"),
    PT_BR("pt-BR", "Português");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
      this.code = code;
      this.displayName = displayName;
    }

    public String getCode() {
      return code;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  /** Predefined output formats for structured responses. */
  public enum OutputFormat {
    JSON("JSON", "Respond with a valid JSON object.", "Responda com um objeto JSON válido."),
    JSON_ARRAY(
        "JSON array", "Respond with a valid JSON array.", "Responda com um array JSON válido."),
    XML("XML", "Respond with valid XML.", "Responda com XML válido."),
    MARKDOWN(
        "Markdown",
        "Format your response using Markdown.",
        "Formate sua resposta usando Markdown."),
    CSV(
        "CSV",
        "Respond in CSV format with headers in the first row.",
        "Responda em formato CSV com cabeçalhos na primeira linha."),
    YAML("YAML", "Respond in valid YAML format.", "Responda em formato YAML válido."),
    PLAIN_TEXT(
        "plain text",
        "Respond in plain text without any formatting.",
        "Responda em texto simples sem formatação."),
    NUMBERED_LIST(
        "numbered list", "Respond with a numbered list.", "Responda com uma lista numerada."),
    BULLET_LIST(
        "bullet list",
        "Respond with a bullet-point list.",
        "Responda com uma lista com marcadores."),
    TABLE("Markdown table", "Respond with a Markdown table.", "Responda com uma tabela Markdown.");

    private final String name;
    private final String instructionEn;
    private final String instructionPtBr;

    OutputFormat(String name, String instructionEn, String instructionPtBr) {
      this.name = name;
      this.instructionEn = instructionEn;
      this.instructionPtBr = instructionPtBr;
    }

    public String getName() {
      return name;
    }

    public String getInstruction(Language lang) {
      return lang == Language.PT_BR ? instructionPtBr : instructionEn;
    }
  }

  /** Delimiter styles for separating prompt sections. */
  public enum DelimiterStyle {
    XML_TAGS("<", ">", "</", ">"),
    TRIPLE_BACKTICKS("```", "\n", "```", ""),
    MARKDOWN_HEADERS("### ", "\n", "", ""),
    DASHES("--- ", " ---\n", "---", ""),
    BRACKETS("[", "]\n", "[/", "]"),
    CURLY_BRACES("{{", "}}\n", "{{/", "}}");

    private final String openPrefix;
    private final String openSuffix;
    private final String closePrefix;
    private final String closeSuffix;

    DelimiterStyle(String openPrefix, String openSuffix, String closePrefix, String closeSuffix) {
      this.openPrefix = openPrefix;
      this.openSuffix = openSuffix;
      this.closePrefix = closePrefix;
      this.closeSuffix = closeSuffix;
    }

    public String wrap(String name, String content) {
      if (this == MARKDOWN_HEADERS) {
        return openPrefix + name + openSuffix + content + "\n";
      }
      if (this == TRIPLE_BACKTICKS) {
        return openPrefix + name + openSuffix + content + "\n" + closePrefix;
      }
      return openPrefix + name + openSuffix + content + "\n" + closePrefix + name + closeSuffix;
    }
  }

  /** Reasoning strategies that can be applied to prompts. */
  public enum ReasoningStrategy {
    CHAIN_OF_THOUGHT("Let's think through this step by step:", "Vamos pensar nisso passo a passo:"),
    STEP_BACK(
        "Before answering, let's first consider the underlying principles and concepts involved.",
        "Antes de responder, vamos primeiro considerar os princípios e conceitos subjacentes"
            + " envolvidos."),
    SELF_VERIFY(
        "After providing your answer, verify it by checking each step of your reasoning.",
        "Após fornecer sua resposta, verifique-a checando cada passo do seu raciocínio."),
    DECOMPOSE(
        "Let's break this problem down into smaller, manageable parts:",
        "Vamos dividir este problema em partes menores e gerenciáveis:"),
    ANALOGICAL(
        "Think of an analogous situation that might help solve this problem.",
        "Pense em uma situação análoga que possa ajudar a resolver este problema."),
    PROS_CONS(
        "Consider both the advantages and disadvantages before concluding.",
        "Considere tanto as vantagens quanto as desvantagens antes de concluir."),
    MULTI_PERSPECTIVE(
        "Consider this from multiple perspectives before providing your answer.",
        "Considere isso de múltiplas perspectivas antes de fornecer sua resposta."),
    TREE_OF_THOUGHTS(
        "Explore multiple possible approaches, evaluate each, and pursue the most promising path.",
        "Explore múltiplas abordagens possíveis, avalie cada uma e siga o caminho mais promissor."),
    SELF_CONSISTENCY(
        "Solve this problem using multiple different approaches and compare the results.",
        "Resolva este problema usando múltiplas abordagens diferentes e compare os resultados.");

    private final String instructionEn;
    private final String instructionPtBr;

    ReasoningStrategy(String instructionEn, String instructionPtBr) {
      this.instructionEn = instructionEn;
      this.instructionPtBr = instructionPtBr;
    }

    public String getInstruction(Language lang) {
      return lang == Language.PT_BR ? instructionPtBr : instructionEn;
    }
  }

  /** Email tone options for email drafting. */
  public enum EmailTone {
    FORMAL("formal", "formal"),
    PROFESSIONAL("professional", "profissional"),
    FRIENDLY("friendly", "amigável"),
    CASUAL("casual", "casual"),
    URGENT("urgent", "urgente"),
    APOLOGETIC("apologetic", "de desculpas"),
    APPRECIATIVE("appreciative", "de agradecimento"),
    PERSUASIVE("persuasive", "persuasivo");

    private final String descriptionEn;
    private final String descriptionPtBr;

    EmailTone(String descriptionEn, String descriptionPtBr) {
      this.descriptionEn = descriptionEn;
      this.descriptionPtBr = descriptionPtBr;
    }

    public String getDescription(Language lang) {
      return lang == Language.PT_BR ? descriptionPtBr : descriptionEn;
    }
  }

  /** Citation styles for academic writing. */
  public enum CitationStyle {
    APA,
    MLA,
    CHICAGO,
    HARVARD,
    IEEE,
    VANCOUVER,
    ABNT // Brazilian standard
  }

  /** Question types for quiz generation. */
  public enum QuestionType {
    MULTIPLE_CHOICE("multiple choice", "múltipla escolha"),
    TRUE_FALSE("true/false", "verdadeiro/falso"),
    SHORT_ANSWER("short answer", "resposta curta"),
    ESSAY("essay", "dissertativa"),
    FILL_IN_THE_BLANK("fill in the blank", "preencher lacunas"),
    MATCHING("matching", "associação"),
    MIXED("mixed types", "tipos variados");

    private final String descriptionEn;
    private final String descriptionPtBr;

    QuestionType(String descriptionEn, String descriptionPtBr) {
      this.descriptionEn = descriptionEn;
      this.descriptionPtBr = descriptionPtBr;
    }

    public String getDescription(Language lang) {
      return lang == Language.PT_BR ? descriptionPtBr : descriptionEn;
    }
  }

  /** Strategies for handling ambiguous inputs. */
  public enum AmbiguityStrategy {
    ASK_CLARIFICATION,
    ASSUME_MOST_LIKELY,
    PROVIDE_ALL_OPTIONS,
    STATE_ASSUMPTIONS
  }

  /** Difficulty levels for content. */
  public enum DifficultyLevel {
    BEGINNER("beginner", "iniciante"),
    INTERMEDIATE("intermediate", "intermediário"),
    ADVANCED("advanced", "avançado"),
    EXPERT("expert", "especialista");

    private final String descriptionEn;
    private final String descriptionPtBr;

    DifficultyLevel(String descriptionEn, String descriptionPtBr) {
      this.descriptionEn = descriptionEn;
      this.descriptionPtBr = descriptionPtBr;
    }

    public String getDescription(Language lang) {
      return lang == Language.PT_BR ? descriptionPtBr : descriptionEn;
    }
  }

  /** Thinking styles for different approaches. */
  public enum ThinkingStyle {
    ANALYTICAL("analytical", "analítico"),
    CREATIVE("creative", "criativo"),
    CRITICAL("critical", "crítico"),
    PRACTICAL("practical", "prático"),
    SYSTEMATIC("systematic", "sistemático"),
    LATERAL("lateral thinking", "pensamento lateral");

    private final String descriptionEn;
    private final String descriptionPtBr;

    ThinkingStyle(String descriptionEn, String descriptionPtBr) {
      this.descriptionEn = descriptionEn;
      this.descriptionPtBr = descriptionPtBr;
    }

    public String getDescription(Language lang) {
      return lang == Language.PT_BR ? descriptionPtBr : descriptionEn;
    }
  }

  // ==================== RECORDS ====================

  public record Example(String input, String output, String explanation) {
    public Example(String input, String output) {
      this(input, output, null);
    }

    public static Example of(String input, String output) {
      return new Example(input, output);
    }

    public static Example withExplanation(String input, String output, String explanation) {
      return new Example(input, output, explanation);
    }
  }

  public record NegativeExample(String input, String badOutput, String reason) {
    public static NegativeExample of(String input, String badOutput, String reason) {
      return new NegativeExample(input, badOutput, reason);
    }
  }

  public record Tool(
      String name, String description, Map<String, ToolParameter> parameters, boolean required) {
    public Tool(String name, String description, Map<String, ToolParameter> parameters) {
      this(name, description, parameters, false);
    }

    public static Tool of(String name, String description) {
      return new Tool(name, description, Map.of(), false);
    }

    public static Tool of(String name, String description, Map<String, ToolParameter> parameters) {
      return new Tool(name, description, parameters, false);
    }
  }

  public record ToolParameter(
      String type, String description, boolean required, List<String> enumValues) {
    public ToolParameter(String type, String description) {
      this(type, description, false, null);
    }

    public static ToolParameter string(String description) {
      return new ToolParameter("string", description);
    }

    public static ToolParameter integer(String description) {
      return new ToolParameter("integer", description);
    }

    public static ToolParameter bool(String description) {
      return new ToolParameter("boolean", description);
    }

    public static ToolParameter enumeration(String description, String... values) {
      return new ToolParameter("string", description, false, Arrays.asList(values));
    }
  }

  public record Message(String role, String content) {
    public static Message user(String content) {
      return new Message("user", content);
    }

    public static Message assistant(String content) {
      return new Message("assistant", content);
    }

    public static Message system(String content) {
      return new Message("system", content);
    }
  }

  @FunctionalInterface
  public interface PromptTemplate {
    Builder configure(Builder builder);
  }

  // ==================== LOCALIZATION ====================

  public static final class L10n {
    private L10n() {}

    public static String youAre(Language lang) {
      return lang == Language.PT_BR ? "Você é " : "You are ";
    }

    public static String anAiAssistant(Language lang) {
      return lang == Language.PT_BR ? "um assistente de IA" : "an AI assistant";
    }

    public static String withExpertiseIn(Language lang) {
      return lang == Language.PT_BR ? " com expertise em " : " with expertise in ";
    }

    public static String and(Language lang) {
      return lang == Language.PT_BR ? " e " : " and ";
    }

    public static String useATone(Language lang, String tone) {
      return lang == Language.PT_BR ? " Use um tom " + tone + "." : " Use a " + tone + " tone.";
    }

    public static String respondIn(Language lang, String language) {
      return lang == Language.PT_BR
          ? " Responda em " + language + "."
          : " Respond in " + language + ".";
    }

    public static String requirements(Language lang) {
      return lang == Language.PT_BR ? "Requisitos:" : "Requirements:";
    }

    public static String constraints(Language lang) {
      return lang == Language.PT_BR ? "Restrições:" : "Constraints:";
    }

    public static String example(Language lang, int num) {
      return lang == Language.PT_BR ? "Exemplo " + num + ":" : "Example " + num + ":";
    }

    public static String input(Language lang) {
      return lang == Language.PT_BR ? "Entrada" : "Input";
    }

    public static String output(Language lang) {
      return lang == Language.PT_BR ? "Saída" : "Output";
    }

    public static String explanation(Language lang) {
      return lang == Language.PT_BR ? "Explicação" : "Explanation";
    }

    public static String badExample(Language lang, int num) {
      return lang == Language.PT_BR ? "Exemplo Ruim " + num + ":" : "Bad Example " + num + ":";
    }

    public static String badOutput(Language lang) {
      return lang == Language.PT_BR ? "Saída Ruim" : "Bad Output";
    }

    public static String whyItsWrong(Language lang) {
      return lang == Language.PT_BR ? "Por que está errado" : "Why it's wrong";
    }

    public static String avoidTheseExamples(Language lang) {
      return lang == Language.PT_BR
          ? "Os seguintes são exemplos do que NÃO fazer:"
          : "The following are examples of what NOT to do:";
    }

    public static String toolsAvailable(Language lang) {
      return lang == Language.PT_BR
          ? "Você tem acesso às seguintes ferramentas:"
          : "You have access to the following tools:";
    }

    public static String parameters(Language lang) {
      return lang == Language.PT_BR ? "Parâmetros" : "Parameters";
    }

    public static String toUseTool(Language lang) {
      return lang == Language.PT_BR
          ? "Para usar uma ferramenta, retorne um objeto JSON com os campos \"tool\" e"
                + " \"parameters\"."
          : "To use a tool, output a JSON object with \"tool\" and \"parameters\" fields.";
    }

    public static String reactPattern(Language lang) {
      return lang == Language.PT_BR
          ? "Siga este padrão para sua resposta:\n\n"
                + "Pensamento: [Seu raciocínio sobre o que fazer a seguir]\n"
                + "Ação: [A ferramenta a usar e seus parâmetros como JSON]\n"
                + "Observação: [Aguarde o resultado - será fornecido]\n\n"
                + "Repita o ciclo Pensamento/Ação/Observação conforme necessário.\n"
                + "Quando tiver informação suficiente, forneça:\n\n"
                + "Pensamento: [Raciocínio final]\n"
                + "Resposta Final: [Sua resposta completa ao usuário]"
          : "Follow this pattern for your response:\n\n"
                + "Thought: [Your reasoning about what to do next]\n"
                + "Action: [The tool to use and its parameters as JSON]\n"
                + "Observation: [Wait for the result - this will be provided]\n\n"
                + "Repeat the Thought/Action/Observation cycle as needed.\n"
                + "When you have enough information, provide:\n\n"
                + "Thought: [Final reasoning]\n"
                + "Final Answer: [Your complete response to the user]";
    }

    public static String targetAudience(Language lang) {
      return lang == Language.PT_BR ? "Público-alvo" : "Target audience";
    }

    public static String expertiseLevel(Language lang) {
      return lang == Language.PT_BR ? "Nível de expertise" : "Expertise level";
    }

    public static String tailorForAudience(Language lang) {
      return lang == Language.PT_BR
          ? "Adapte sua resposta apropriadamente para este público."
          : "Tailor your response appropriately for this audience.";
    }

    public static String outputRequirements(Language lang) {
      return lang == Language.PT_BR ? "Requisitos de saída:" : "Output requirements:";
    }

    public static String keepResponseUnder(Language lang, int tokens) {
      return lang == Language.PT_BR
          ? "Mantenha sua resposta em aproximadamente " + tokens + " tokens."
          : "Keep your response under approximately " + tokens + " tokens.";
    }

    public static String followThisSchema(Language lang) {
      return lang == Language.PT_BR ? "Siga este esquema:" : "Follow this schema:";
    }

    public static String outputDescription(Language lang) {
      return lang == Language.PT_BR ? "Descrição da saída" : "Output description";
    }
  }

  // ==================== COMPILE BUILDER ====================

  public static final class CompileBuilder {
    private final Prompt prompt;
    private final Map<String, Object> context;

    private CompileBuilder(Prompt prompt) {
      this.prompt = prompt;
      this.context = new HashMap<>();
    }

    public CompileBuilder with(String key, Object value) {
      context.put(key, value);
      return this;
    }

    public CompileBuilder withAll(Map<String, Object> values) {
      context.putAll(values);
      return this;
    }

    public Prompt build() {
      return prompt.compile(context);
    }

    public Prompt buildIf() {
      return prompt.compileIf(context);
    }
  }

  public static class TemplateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TemplateException(String message) {
      super(message);
    }

    public TemplateException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  // ==================== MAIN BUILDER ====================

  /**
   * Comprehensive builder for constructing high-quality prompts following best practices.
   *
   * <h2>Design Principles</h2>
   *
   * <ul>
   *   <li><b>Order Independent:</b> All methods can be called in any order
   *   <li><b>No Dependencies:</b> No method requires another method to be called first
   *   <li><b>Additive:</b> Methods that accept multiple values add to existing values
   *   <li><b>Multi-language:</b> Supports EN_US and PT_BR
   * </ul>
   */
  public static final class Builder {

    private Language language = Language.EN_US;
    private String systemInstruction;
    private String role;
    private final List<String> expertise = new ArrayList<>();
    private final List<String> personality = new ArrayList<>();
    private String task;
    private String goal;
    private final List<String> instructions = new ArrayList<>();
    private final List<String> requirements = new ArrayList<>();
    private final List<String> constraints = new ArrayList<>();
    private final List<ContextSection> contextSections = new ArrayList<>();
    private final List<Message> conversationHistory = new ArrayList<>();
    private String inputData;
    private String inputLabel;
    private List<Example> examples = new ArrayList<>();
    private final List<NegativeExample> negativeExamples = new ArrayList<>();
    private boolean shuffleExamples = false;
    private final Set<ReasoningStrategy> reasoningStrategies = new LinkedHashSet<>();
    private String customReasoningPrompt;
    private OutputFormat outputFormat;
    private String outputSchema;
    private String outputDescription;
    private final List<String> outputConstraints = new ArrayList<>();
    private Integer maxTokens;
    private final List<Tool> tools = new ArrayList<>();
    private boolean reActPattern = false;
    private String targetAudience;
    private String audienceExpertiseLevel;
    private DelimiterStyle delimiterStyle = DelimiterStyle.XML_TAGS;
    private String tone;
    private String responseLanguage;
    private final List<Section> customSections = new ArrayList<>();
    private String prefillText;

    public Builder() {}

    // ==================== LANGUAGE ====================

    public Builder language(Language language) {
      this.language = Objects.requireNonNull(language);
      return this;
    }

    public Builder portugues() {
      return language(Language.PT_BR);
    }

    public Builder english() {
      return language(Language.EN_US);
    }

    // ==================== ROLE & IDENTITY ====================

    public Builder role(String role) {
      this.role = Objects.requireNonNull(role);
      return this;
    }

    public Builder addRole(String role) {
      return role(role);
    }

    public Builder expertise(String... areas) {
      Collections.addAll(this.expertise, areas);
      return this;
    }

    public Builder addExpertise(String... areas) {
      return expertise(areas);
    }

    public Builder personality(String... traits) {
      Collections.addAll(this.personality, traits);
      return this;
    }

    public Builder addPersonality(String... traits) {
      return personality(traits);
    }

    // ==================== SYSTEM & CONTEXT ====================

    public Builder systemInstruction(String instruction) {
      this.systemInstruction = Objects.requireNonNull(instruction);
      return this;
    }

    public ContextBuilder context() {
      return new ContextBuilder(this);
    }

    public Builder addDocument(String name, String content) {
      this.contextSections.add(new ContextSection(ContextType.DOCUMENT, name, content));
      return this;
    }

    public Builder addBackground(String background) {
      this.contextSections.add(
          new ContextSection(ContextType.BACKGROUND, "background", background));
      return this;
    }

    public Builder conversationHistory(List<Message> messages) {
      this.conversationHistory.addAll(messages);
      return this;
    }

    public Builder addMessage(Message message) {
      this.conversationHistory.add(message);
      return this;
    }

    // ==================== TASK & INSTRUCTIONS ====================

    public Builder task(String task) {
      this.task = Objects.requireNonNull(task);
      return this;
    }

    public Builder goal(String goal) {
      this.goal = Objects.requireNonNull(goal);
      return this;
    }

    public Builder instruct(String instruction) {
      this.instructions.add(Objects.requireNonNull(instruction));
      return this;
    }

    public Builder instructions(String... instructions) {
      Collections.addAll(this.instructions, instructions);
      return this;
    }

    public Builder require(String requirement) {
      this.requirements.add(Objects.requireNonNull(requirement));
      return this;
    }

    public Builder requirements(String... requirements) {
      Collections.addAll(this.requirements, requirements);
      return this;
    }

    public Builder constraint(String constraint) {
      this.constraints.add(Objects.requireNonNull(constraint));
      return this;
    }

    public Builder constraints(String... constraints) {
      Collections.addAll(this.constraints, constraints);
      return this;
    }

    // ==================== INPUT ====================

    public Builder input(String input) {
      this.inputData = Objects.requireNonNull(input);
      return this;
    }

    public Builder input(String label, String input) {
      this.inputLabel = Objects.requireNonNull(label);
      this.inputData = Objects.requireNonNull(input);
      return this;
    }

    // ==================== EXAMPLES ====================

    public ExampleBuilder fewShot() {
      return new ExampleBuilder(this);
    }

    public Builder example(String input, String output) {
      this.examples.add(Example.of(input, output));
      return this;
    }

    public Builder negativeExample(String input, String badOutput, String reason) {
      this.negativeExamples.add(NegativeExample.of(input, badOutput, reason));
      return this;
    }

    // ==================== REASONING ====================

    public Builder withChainOfThought() {
      this.reasoningStrategies.add(ReasoningStrategy.CHAIN_OF_THOUGHT);
      return this;
    }

    public Builder withStepBack() {
      this.reasoningStrategies.add(ReasoningStrategy.STEP_BACK);
      return this;
    }

    public Builder withSelfVerification() {
      this.reasoningStrategies.add(ReasoningStrategy.SELF_VERIFY);
      return this;
    }

    public Builder withDecomposition() {
      this.reasoningStrategies.add(ReasoningStrategy.DECOMPOSE);
      return this;
    }

    public Builder withMultiplePerspectives() {
      this.reasoningStrategies.add(ReasoningStrategy.MULTI_PERSPECTIVE);
      return this;
    }

    public Builder withProsAndCons() {
      this.reasoningStrategies.add(ReasoningStrategy.PROS_CONS);
      return this;
    }

    public Builder withTreeOfThoughts() {
      this.reasoningStrategies.add(ReasoningStrategy.TREE_OF_THOUGHTS);
      return this;
    }

    public Builder withSelfConsistency() {
      this.reasoningStrategies.add(ReasoningStrategy.SELF_CONSISTENCY);
      return this;
    }

    public Builder withReasoning(String reasoningPrompt) {
      this.customReasoningPrompt = reasoningPrompt;
      return this;
    }

    public Builder withStrategy(ReasoningStrategy strategy) {
      this.reasoningStrategies.add(strategy);
      return this;
    }

    // ==================== OUTPUT ====================

    public Builder outputAs(OutputFormat format) {
      this.outputFormat = Objects.requireNonNull(format);
      return this;
    }

    public Builder outputSchema(String schema) {
      this.outputSchema = Objects.requireNonNull(schema);
      if (this.outputFormat == null) this.outputFormat = OutputFormat.JSON;
      return this;
    }

    public Builder outputDescription(String description) {
      this.outputDescription = description;
      return this;
    }

    public Builder outputConstraints(String... constraints) {
      Collections.addAll(this.outputConstraints, constraints);
      return this;
    }

    public Builder maxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public Builder concise() {
      this.instructions.add(
          language == Language.PT_BR
              ? "Seja conciso e direto. Evite elaboração desnecessária."
              : "Be concise and direct. Avoid unnecessary elaboration.");
      return this;
    }

    public Builder detailed() {
      this.instructions.add(
          language == Language.PT_BR
              ? "Forneça uma resposta abrangente e detalhada com explicações completas."
              : "Provide a comprehensive and detailed response with thorough explanations.");
      return this;
    }

    // ==================== TOOLS ====================

    public ToolBuilder tools() {
      return new ToolBuilder(this);
    }

    public Builder tool(String name, String description) {
      this.tools.add(Tool.of(name, description));
      return this;
    }

    public Builder withReActPattern() {
      this.reActPattern = true;
      return this;
    }

    // ==================== AUDIENCE ====================

    public Builder forAudience(String audience) {
      this.targetAudience = Objects.requireNonNull(audience);
      return this;
    }

    public Builder audienceLevel(String level) {
      this.audienceExpertiseLevel = level;
      return this;
    }

    // ==================== STYLE ====================

    public Builder delimiterStyle(DelimiterStyle style) {
      this.delimiterStyle = Objects.requireNonNull(style);
      return this;
    }

    public Builder tone(String tone) {
      this.tone = tone;
      return this;
    }

    public Builder respondIn(String language) {
      this.responseLanguage = language;
      return this;
    }

    // ==================== CUSTOM SECTIONS ====================

    public Builder section(String name, String content) {
      this.customSections.add(new Section(name, content));
      return this;
    }

    public Builder raw(String text) {
      this.customSections.add(new Section(null, text));
      return this;
    }

    // ==================== CODE-SPECIFIC ====================

    public Builder forCodeExplanation() {
      if (role == null)
        role = language == Language.PT_BR ? "instrutor de código" : "code instructor";
      if (task == null)
        task =
            language == Language.PT_BR
                ? "Explique o código a seguir"
                : "Explain the following code";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Explique o código seção por seção",
                "Esclareça o propósito e a lógica de cada parte",
                "Destaque padrões ou técnicas importantes",
                "Note possíveis problemas ou melhorias"
              }
              : new String[] {
                "Explain the code section by section",
                "Clarify the purpose and logic of each part",
                "Highlight important patterns or techniques",
                "Note potential issues or improvements"
              });
      return this;
    }

    public Builder forCodeDebugging(String errorMessage) {
      if (role == null)
        role = language == Language.PT_BR ? "especialista em debugging" : "debugging expert";
      if (task == null)
        task = language == Language.PT_BR ? "Depure o código a seguir" : "Debug the following code";
      addBackground((language == Language.PT_BR ? "Erro: " : "Error: ") + errorMessage);
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Identifique a causa raiz do erro",
                "Explique por que este erro ocorre",
                "Forneça o código corrigido",
                "Sugira como prevenir problemas similares"
              }
              : new String[] {
                "Identify the root cause of the error",
                "Explain why this error occurs",
                "Provide the corrected code",
                "Suggest how to prevent similar issues"
              });
      withChainOfThought();
      return this;
    }

    public Builder forCodeReview() {
      if (role == null)
        role = language == Language.PT_BR ? "revisor de código sênior" : "senior code reviewer";
      if (task == null)
        task =
            language == Language.PT_BR ? "Revise o código a seguir" : "Review the following code";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Verifique bugs, problemas de segurança e performance",
                "Avalie qualidade e manutenibilidade do código",
                "Sugira melhorias específicas com exemplos",
                "Note o que foi feito bem"
              }
              : new String[] {
                "Check for bugs, security issues, and performance problems",
                "Evaluate code quality and maintainability",
                "Suggest specific improvements with examples",
                "Note what's done well"
              });
      outputAs(OutputFormat.JSON);
      return this;
    }

    public Builder forCodeTranslation(String fromLanguage, String toLanguage) {
      if (role == null)
        role =
            (language == Language.PT_BR
                    ? "programador poliglota especialista em "
                    : "polyglot programmer expert in ")
                + fromLanguage
                + (language == Language.PT_BR ? " e " : " and ")
                + toLanguage;
      if (task == null)
        task =
            (language == Language.PT_BR
                ? "Traduza o código " + fromLanguage + " a seguir para " + toLanguage
                : "Translate the following " + fromLanguage + " code to " + toLanguage);
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Mantenha a mesma funcionalidade e comportamento",
                "Use padrões idiomáticos de " + toLanguage,
                "Adicione comentários onde a tradução difere significativamente"
              }
              : new String[] {
                "Maintain the same functionality and behavior",
                "Use idiomatic " + toLanguage + " patterns",
                "Add comments where the translation differs significantly"
              });
      return this;
    }

    public Builder forCodeRefactoring(String refactoringGoal) {
      if (role == null)
        role = language == Language.PT_BR ? "especialista em refatoração" : "refactoring expert";
      if (task == null)
        task =
            (language == Language.PT_BR
                ? "Refatore o código a seguir para melhorar " + refactoringGoal
                : "Refactor the following code to improve " + refactoringGoal);
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Preserve a funcionalidade original",
                "Aplique design patterns apropriados se benéfico",
                "Explique cada mudança de refatoração"
              }
              : new String[] {
                "Preserve the original functionality",
                "Apply appropriate design patterns if beneficial",
                "Explain each refactoring change made"
              });
      return this;
    }

    // ==================== ADVANCED PATTERNS ====================

    public Builder withRefinement(String previousAttempt, String feedback) {
      contextSections.add(
          new ContextSection(
              ContextType.CUSTOM,
              language == Language.PT_BR ? "tentativa_anterior" : "previous_attempt",
              previousAttempt));
      contextSections.add(new ContextSection(ContextType.CUSTOM, "feedback", feedback));
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Melhore a tentativa anterior abordando o feedback",
                "Mantenha o que estava bom enquanto corrige os problemas identificados"
              }
              : new String[] {
                "Improve upon the previous attempt by addressing the feedback",
                "Maintain what was good while fixing the identified issues"
              });
      return this;
    }

    public Builder withAnalogy(String analogy) {
      this.instructions.add(
          (language == Language.PT_BR ? "Pense nesta tarefa como: " : "Think of this task like: ")
              + analogy);
      return this;
    }

    public Builder asSelfImproving(String promptToImprove) {
      if (role == null)
        role =
            language == Language.PT_BR
                ? "especialista em engenharia de prompts"
                : "prompt engineering expert";
      if (task == null)
        task =
            language == Language.PT_BR
                ? "Analise e melhore o prompt a seguir"
                : "Analyze and improve the following prompt";
      contextSections.add(
          new ContextSection(ContextType.CUSTOM, "original_prompt", promptToImprove));
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Identifique fraquezas em clareza, especificidade e estrutura",
                "Aplique melhores práticas de engenharia de prompts",
                "Forneça o prompt melhorado",
                "Explique o que você mudou e por quê"
              }
              : new String[] {
                "Identify weaknesses in clarity, specificity, and structure",
                "Apply prompt engineering best practices",
                "Provide the improved prompt",
                "Explain what you changed and why"
              });
      outputAs(OutputFormat.JSON);
      return this;
    }

    public Builder withGuardRails(String... rails) {
      Collections.addAll(this.constraints, rails);
      return this;
    }

    public Builder withLength(String length) {
      return instruct(
          (language == Language.PT_BR ? "Forneça uma resposta de " : "Provide a ")
              + length
              + (language == Language.PT_BR ? "" : " response"));
    }

    public Builder withSubtasks(String... subtasks) {
      StringBuilder sb = new StringBuilder();
      sb.append(
          language == Language.PT_BR
              ? "Complete as seguintes subtarefas em ordem:\n"
              : "Complete the following subtasks in order:\n");
      for (int i = 0; i < subtasks.length; i++) {
        sb.append(i + 1).append(". ").append(subtasks[i]).append("\n");
      }
      this.instructions.add(sb.toString().trim());
      return instruct(
          language == Language.PT_BR
              ? "Aborde cada subtarefa sistematicamente antes de passar para a próxima"
              : "Address each subtask systematically before moving to the next");
    }

    public Builder prefill(String prefill) {
      this.prefillText = prefill;
      return instruct(
          language == Language.PT_BR
              ? "Continue a partir do ponto de partida fornecido"
              : "Continue from the provided starting point");
    }

    public Builder withConfidenceScore() {
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Avalie sua confiança nesta resposta de 0-100%",
                "Explique quais fatores afetam sua confiança"
              }
              : new String[] {
                "Rate your confidence in this answer from 0-100%",
                "Explain what factors affect your confidence"
              });
      return this;
    }

    public Builder asSocraticTutor() {
      if (role == null) role = language == Language.PT_BR ? "tutor socrático" : "Socratic tutor";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Guie o entendimento através de perguntas em vez de respostas diretas",
                "Faça perguntas investigativas que levem a insights",
                "Encoraje o estudante a descobrir as respostas por si mesmo",
                "Forneça dicas quando travado, não soluções"
              }
              : new String[] {
                "Guide understanding through questions rather than direct answers",
                "Ask probing questions that lead to insight",
                "Encourage the student to discover answers themselves",
                "Provide hints when stuck, not solutions"
              });
      return this;
    }

    public Builder asDevilsAdvocate() {
      if (role == null) role = language == Language.PT_BR ? "analista crítico" : "critical analyst";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Desafie as suposições na posição apresentada",
                "Apresente contra-argumentos convincentes",
                "Identifique potenciais fraquezas e pontos cegos",
                "Seja construtivo, não apenas contrário"
              }
              : new String[] {
                "Challenge the assumptions in the given position",
                "Present compelling counter-arguments",
                "Identify potential weaknesses and blind spots",
                "Be constructive, not just contrarian"
              });
      return this;
    }

    public Builder forBrainstorming() {
      if (role == null)
        role =
            language == Language.PT_BR
                ? "parceiro criativo de brainstorming"
                : "creative brainstorming partner";
      if (task == null) task = language == Language.PT_BR ? "Gere ideias" : "Generate ideas";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Gere ideias diversas e criativas",
                "Não julgue ideias inicialmente - quantidade sobre qualidade",
                "Construa e combine ideias",
                "Inclua sugestões práticas e não convencionais"
              }
              : new String[] {
                "Generate diverse and creative ideas",
                "Don't judge ideas initially - quantity over quality",
                "Build on and combine ideas",
                "Include both practical and unconventional suggestions"
              });
      outputAs(OutputFormat.NUMBERED_LIST);
      return this;
    }

    public Builder forComparison() {
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Compare sistematicamente os itens",
                "Identifique similaridades e diferenças",
                "Forneça uma análise equilibrada",
                "Faça uma recomendação se apropriado"
              }
              : new String[] {
                "Systematically compare the items",
                "Identify similarities and differences",
                "Provide balanced analysis",
                "Make a recommendation if appropriate"
              });
      return this;
    }

    public Builder forFactChecking() {
      if (role == null) role = language == Language.PT_BR ? "verificador de fatos" : "fact checker";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Verifique a precisão das afirmações",
                "Identifique informações incorretas ou enganosas",
                "Forneça correções com fontes quando possível",
                "Avalie o nível de confiança em suas verificações"
              }
              : new String[] {
                "Verify the accuracy of the claims",
                "Identify incorrect or misleading information",
                "Provide corrections with sources when possible",
                "Assess confidence level in your verifications"
              });
      withChainOfThought();
      withConfidenceScore();
      return this;
    }

    public Builder forTutorial() {
      if (role == null)
        role = language == Language.PT_BR ? "criador de tutoriais" : "tutorial creator";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Divida o processo em passos claros e numerados",
                "Explique cada passo em detalhes",
                "Inclua dicas e avisos importantes",
                "Forneça exemplos quando útil"
              }
              : new String[] {
                "Break down the process into clear, numbered steps",
                "Explain each step in detail",
                "Include tips and important warnings",
                "Provide examples when helpful"
              });
      return this;
    }

    // ==================== INTERVIEW & DIALOGUE MODES ====================

    /** Configures for conducting an interview. */
    public Builder forInterview() {
      if (role == null)
        role =
            language == Language.PT_BR ? "entrevistador profissional" : "professional interviewer";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Faça perguntas abertas e investigativas",
                "Ouça ativamente e faça perguntas de acompanhamento",
                "Mantenha um tom profissional mas acolhedor",
                "Explore os tópicos em profundidade"
              }
              : new String[] {
                "Ask open-ended and probing questions",
                "Listen actively and ask follow-up questions",
                "Maintain a professional but welcoming tone",
                "Explore topics in depth"
              });
      return this;
    }

    /** Configures for a debate format. */
    public Builder forDebate(String position) {
      if (role == null) role = language == Language.PT_BR ? "debatedor" : "debater";
      if (task == null)
        task =
            (language == Language.PT_BR ? "Argumente a favor de: " : "Argue in favor of: ")
                + position;
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Apresente argumentos lógicos e bem estruturados",
                "Antecipe e refute contra-argumentos",
                "Use evidências e exemplos para apoiar seus pontos",
                "Mantenha um tom respeitoso mesmo ao discordar"
              }
              : new String[] {
                "Present logical and well-structured arguments",
                "Anticipate and refute counter-arguments",
                "Use evidence and examples to support your points",
                "Maintain a respectful tone even when disagreeing"
              });
      return this;
    }

    /** Configures for role-playing scenarios. */
    public Builder forRolePlay(String character, String scenario) {
      if (role == null) role = character;
      addBackground((language == Language.PT_BR ? "Cenário: " : "Scenario: ") + scenario);
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Mantenha o personagem de forma consistente",
                "Responda como o personagem responderia",
                "Use linguagem e maneirismos apropriados ao personagem"
              }
              : new String[] {
                "Stay in character consistently",
                "Respond as the character would respond",
                "Use language and mannerisms appropriate to the character"
              });
      return this;
    }

    // ==================== ANALYSIS FRAMEWORKS ====================

    /** Configures for SWOT analysis. */
    public Builder forSWOTAnalysis() {
      if (role == null)
        role = language == Language.PT_BR ? "analista estratégico" : "strategic analyst";
      if (task == null)
        task = language == Language.PT_BR ? "Realize uma análise SWOT" : "Perform a SWOT analysis";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Identifique Forças (Strengths) internas",
                "Identifique Fraquezas (Weaknesses) internas",
                "Identifique Oportunidades (Opportunities) externas",
                "Identifique Ameaças (Threats) externas",
                "Forneça insights acionáveis baseados na análise"
              }
              : new String[] {
                "Identify internal Strengths",
                "Identify internal Weaknesses",
                "Identify external Opportunities",
                "Identify external Threats",
                "Provide actionable insights based on the analysis"
              });
      outputAs(OutputFormat.JSON);
      outputSchema(
          "{\"strengths\": [...], \"weaknesses\": [...], \"opportunities\": [...], \"threats\":"
              + " [...], \"recommendations\": [...]}");
      return this;
    }

    /** Configures for pros and cons analysis. */
    public Builder forProsConsAnalysis() {
      if (role == null) role = language == Language.PT_BR ? "analista" : "analyst";
      if (task == null)
        task =
            language == Language.PT_BR ? "Analise os prós e contras" : "Analyze the pros and cons";
      withProsAndCons();
      outputAs(OutputFormat.JSON);
      outputSchema(
          "{\"pros\": [...], \"cons\": [...], \"recommendation\": \"...\", \"confidence\":"
              + " \"high|medium|low\"}");
      return this;
    }

    /** Configures for decision-making framework. */
    public Builder forDecisionMaking() {
      if (role == null)
        role = language == Language.PT_BR ? "consultor de decisões" : "decision consultant";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Identifique claramente a decisão a ser tomada",
                "Liste todas as opções disponíveis",
                "Avalie cada opção com critérios objetivos",
                "Considere riscos e benefícios de cada opção",
                "Forneça uma recomendação clara com justificativa"
              }
              : new String[] {
                "Clearly identify the decision to be made",
                "List all available options",
                "Evaluate each option with objective criteria",
                "Consider risks and benefits of each option",
                "Provide a clear recommendation with justification"
              });
      withChainOfThought();
      return this;
    }

    /** Configures for root cause analysis (5 Whys). */
    public Builder forRootCauseAnalysis() {
      if (role == null)
        role = language == Language.PT_BR ? "analista de causa raiz" : "root cause analyst";
      if (task == null)
        task =
            language == Language.PT_BR
                ? "Realize análise de causa raiz usando a técnica dos 5 Porquês"
                : "Perform root cause analysis using the 5 Whys technique";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Pergunte 'Por quê?' repetidamente até chegar à causa raiz",
                "Documente cada nível de análise",
                "Identifique a causa raiz fundamental",
                "Sugira ações corretivas"
              }
              : new String[] {
                "Ask 'Why?' repeatedly until reaching the root cause",
                "Document each level of analysis",
                "Identify the fundamental root cause",
                "Suggest corrective actions"
              });
      withChainOfThought();
      return this;
    }

    // ==================== WRITING MODES ====================

    /** Configures for email drafting. */
    public Builder forEmailDrafting(EmailTone emailTone) {
      if (role == null)
        role =
            language == Language.PT_BR
                ? "redator de e-mails profissional"
                : "professional email writer";
      if (task == null) task = language == Language.PT_BR ? "Redija um e-mail" : "Draft an email";
      String toneDesc = emailTone.getDescription(language);
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Use um tom " + toneDesc,
                "Seja claro e direto",
                "Inclua uma linha de assunto apropriada",
                "Estruture o e-mail com saudação, corpo e encerramento"
              }
              : new String[] {
                "Use a " + toneDesc + " tone",
                "Be clear and direct",
                "Include an appropriate subject line",
                "Structure the email with greeting, body, and closing"
              });
      return this;
    }

    /** Configures for technical writing. */
    public Builder forTechnicalWriting() {
      if (role == null) role = language == Language.PT_BR ? "redator técnico" : "technical writer";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Use linguagem clara e precisa",
                "Defina termos técnicos quando usados pela primeira vez",
                "Use exemplos concretos para ilustrar conceitos",
                "Organize o conteúdo de forma lógica",
                "Inclua diagramas ou pseudocódigo quando útil"
              }
              : new String[] {
                "Use clear and precise language",
                "Define technical terms when first used",
                "Use concrete examples to illustrate concepts",
                "Organize content logically",
                "Include diagrams or pseudocode when helpful"
              });
      return this;
    }

    /** Configures for scientific/academic writing. */
    public Builder forAcademicWriting(CitationStyle citationStyle) {
      if (role == null)
        role = language == Language.PT_BR ? "escritor acadêmico" : "academic writer";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Use linguagem formal e objetiva",
                "Suporte afirmações com evidências",
                "Use o estilo de citação " + citationStyle.name(),
                "Mantenha tom imparcial e acadêmico",
                "Estruture com introdução, desenvolvimento e conclusão"
              }
              : new String[] {
                "Use formal and objective language",
                "Support claims with evidence",
                "Use " + citationStyle.name() + " citation style",
                "Maintain impartial and academic tone",
                "Structure with introduction, body, and conclusion"
              });
      return this;
    }

    /** Configures for persuasive/marketing copy. */
    public Builder forMarketingCopy() {
      if (role == null)
        role = language == Language.PT_BR ? "copywriter de marketing" : "marketing copywriter";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Use linguagem persuasiva e envolvente",
                "Foque nos benefícios para o cliente",
                "Crie senso de urgência quando apropriado",
                "Use gatilhos emocionais efetivos",
                "Inclua uma chamada para ação clara"
              }
              : new String[] {
                "Use persuasive and engaging language",
                "Focus on benefits to the customer",
                "Create a sense of urgency when appropriate",
                "Use effective emotional triggers",
                "Include a clear call to action"
              });
      return this;
    }

    /** Configures for storytelling/narrative. */
    public Builder forStorytelling() {
      if (role == null) role = language == Language.PT_BR ? "contador de histórias" : "storyteller";
      addPersonality(
          language == Language.PT_BR
              ? new String[] {"criativo", "envolvente", "descritivo"}
              : new String[] {"creative", "engaging", "descriptive"});
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Crie personagens interessantes e memoráveis",
                "Use descrições vívidas e sensoriais",
                "Construa tensão e interesse",
                "Mostre, não apenas conte"
              }
              : new String[] {
                "Create interesting and memorable characters",
                "Use vivid sensory descriptions",
                "Build tension and interest",
                "Show, don't just tell"
              });
      return this;
    }

    /** Configures for legal document analysis (with disclaimer). */
    public Builder forLegalAnalysis() {
      if (role == null) role = language == Language.PT_BR ? "analista jurídico" : "legal analyst";
      constraints(
          language == Language.PT_BR
              ? new String[] {
                "AVISO: Esta análise é apenas para fins informativos e não constitui aconselhamento"
                    + " jurídico",
                "Recomende consultar um advogado qualificado para decisões legais"
              }
              : new String[] {
                "DISCLAIMER: This analysis is for informational purposes only and does not"
                    + " constitute legal advice",
                "Recommend consulting a qualified attorney for legal decisions"
              });
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Identifique cláusulas e termos importantes",
                "Explique implicações em linguagem simples",
                "Destaque riscos potenciais",
                "Note ambiguidades ou áreas de preocupação"
              }
              : new String[] {
                "Identify important clauses and terms",
                "Explain implications in plain language",
                "Highlight potential risks",
                "Note ambiguities or areas of concern"
              });
      return this;
    }

    /** Configures for medical information (with strong disclaimer). */
    public Builder forMedicalInformation() {
      if (role == null) role = language == Language.PT_BR ? "educador de saúde" : "health educator";
      constraints(
          language == Language.PT_BR
              ? new String[] {
                "IMPORTANTE: Esta informação é apenas educacional e NÃO substitui aconselhamento"
                    + " médico profissional",
                "Sempre recomende consultar um profissional de saúde qualificado",
                "Não faça diagnósticos ou recomende tratamentos específicos"
              }
              : new String[] {
                "IMPORTANT: This information is educational only and does NOT substitute"
                    + " professional medical advice",
                "Always recommend consulting a qualified healthcare professional",
                "Do not diagnose or recommend specific treatments"
              });
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Forneça informações gerais de saúde baseadas em evidências",
                "Use linguagem clara e acessível",
                "Cite fontes confiáveis quando possível"
              }
              : new String[] {
                "Provide general evidence-based health information",
                "Use clear and accessible language",
                "Cite reliable sources when possible"
              });
      return this;
    }

    // ==================== EDUCATIONAL MODES ====================

    /** Configures for language learning assistance. */
    public Builder forLanguageLearning(String targetLang, String nativeLang) {
      if (role == null)
        role =
            language == Language.PT_BR
                ? "professor de " + targetLang
                : targetLang + " language teacher";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Explique conceitos em " + nativeLang + " quando necessário",
                "Forneça exemplos de uso em contexto",
                "Corrija erros de forma gentil e educativa",
                "Ensine aspectos culturais relevantes",
                "Use repetição espaçada para vocabulário"
              }
              : new String[] {
                "Explain concepts in " + nativeLang + " when needed",
                "Provide usage examples in context",
                "Correct errors gently and educationally",
                "Teach relevant cultural aspects",
                "Use spaced repetition for vocabulary"
              });
      return this;
    }

    /** Configures for ELI5 (Explain Like I'm 5) style explanations. */
    public Builder forELI5() {
      if (role == null)
        role = language == Language.PT_BR ? "explicador paciente" : "patient explainer";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Use linguagem muito simples",
                "Use analogias do dia-a-dia",
                "Evite jargão técnico completamente",
                "Use exemplos concretos e familiares",
                "Mantenha explicações curtas e claras"
              }
              : new String[] {
                "Use very simple language",
                "Use everyday analogies",
                "Avoid technical jargon completely",
                "Use concrete and familiar examples",
                "Keep explanations short and clear"
              });
      forAudience(language == Language.PT_BR ? "crianças de 5 anos" : "5-year-old children");
      return this;
    }

    /** Configures for expert-level explanation. */
    public Builder forExpertExplanation() {
      if (role == null) role = language == Language.PT_BR ? "especialista" : "expert";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Use terminologia técnica apropriada",
                "Aprofunde-se em nuances e complexidades",
                "Referencie pesquisas e desenvolvimentos recentes",
                "Discuta trade-offs e considerações avançadas"
              }
              : new String[] {
                "Use appropriate technical terminology",
                "Delve into nuances and complexities",
                "Reference recent research and developments",
                "Discuss trade-offs and advanced considerations"
              });
      audienceLevel(language == Language.PT_BR ? "especialista" : "expert");
      detailed();
      return this;
    }

    /** Configures for quiz/test generation. */
    public Builder forQuizGeneration(int numQuestions, QuestionType questionType) {
      if (role == null) role = language == Language.PT_BR ? "criador de quizzes" : "quiz creator";
      if (task == null)
        task =
            language == Language.PT_BR
                ? "Crie " + numQuestions + " perguntas de " + questionType.getDescription(language)
                : "Create "
                    + numQuestions
                    + " "
                    + questionType.getDescription(language)
                    + " questions";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Varie a dificuldade das perguntas",
                "Cubra diferentes aspectos do tópico",
                "Inclua respostas corretas e explicações"
              }
              : new String[] {
                "Vary the difficulty of questions",
                "Cover different aspects of the topic",
                "Include correct answers and explanations"
              });
      outputAs(OutputFormat.JSON);
      return this;
    }

    // ==================== SPECIALIZED MODES ====================

    /** Configures for API documentation generation. */
    public Builder forAPIDocumentation() {
      if (role == null)
        role = language == Language.PT_BR ? "documentador de API" : "API documentor";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Documente endpoints, parâmetros e respostas",
                "Inclua exemplos de requisição e resposta",
                "Liste códigos de erro e seus significados",
                "Especifique tipos de dados e validações"
              }
              : new String[] {
                "Document endpoints, parameters, and responses",
                "Include request and response examples",
                "List error codes and their meanings",
                "Specify data types and validations"
              });
      outputAs(OutputFormat.MARKDOWN);
      return this;
    }

    /** Configures for changelog generation. */
    public Builder forChangelog() {
      if (role == null)
        role = language == Language.PT_BR ? "gerente de releases" : "release manager";
      if (task == null)
        task = language == Language.PT_BR ? "Gere um changelog" : "Generate a changelog";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Agrupe mudanças por tipo (Added, Changed, Fixed, Removed)",
                "Seja claro e conciso",
                "Use verbos no passado",
                "Referencie issues/PRs quando relevante"
              }
              : new String[] {
                "Group changes by type (Added, Changed, Fixed, Removed)",
                "Be clear and concise",
                "Use past tense verbs",
                "Reference issues/PRs when relevant"
              });
      return this;
    }

    /** Configures for user story generation. */
    public Builder forUserStories() {
      if (role == null)
        role = language == Language.PT_BR ? "analista de produto" : "product analyst";
      if (task == null)
        task = language == Language.PT_BR ? "Crie user stories" : "Create user stories";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Use o formato: Como [persona], eu quero [funcionalidade], para que [benefício]",
                "Inclua critérios de aceitação",
                "Defina a prioridade (must have, should have, could have)",
                "Estime complexidade quando possível"
              }
              : new String[] {
                "Use the format: As a [persona], I want [feature], so that [benefit]",
                "Include acceptance criteria",
                "Define priority (must have, should have, could have)",
                "Estimate complexity when possible"
              });
      outputAs(OutputFormat.JSON);
      return this;
    }

    /** Configures for data visualization suggestions. */
    public Builder forDataVisualization() {
      if (role == null)
        role =
            language == Language.PT_BR
                ? "especialista em visualização de dados"
                : "data visualization specialist";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Recomende o tipo de gráfico mais apropriado",
                "Explique por que essa visualização é eficaz",
                "Sugira cores e formatação apropriadas",
                "Considere acessibilidade na visualização"
              }
              : new String[] {
                "Recommend the most appropriate chart type",
                "Explain why this visualization is effective",
                "Suggest appropriate colors and formatting",
                "Consider accessibility in visualization"
              });
      return this;
    }

    /** Configures for accessibility review. */
    public Builder forAccessibilityReview() {
      if (role == null)
        role =
            language == Language.PT_BR
                ? "especialista em acessibilidade"
                : "accessibility specialist";
      if (task == null)
        task = language == Language.PT_BR ? "Revise a acessibilidade" : "Review accessibility";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Avalie conformidade com WCAG",
                "Identifique barreiras para usuários com deficiências",
                "Sugira melhorias específicas",
                "Priorize problemas por severidade"
              }
              : new String[] {
                "Evaluate WCAG compliance",
                "Identify barriers for users with disabilities",
                "Suggest specific improvements",
                "Prioritize issues by severity"
              });
      outputAs(OutputFormat.JSON);
      outputSchema(
          "{\"issues\": [{\"severity\": \"critical|major|minor\", \"wcag\": \"criterion\","
              + " \"description\": \"...\", \"suggestion\": \"...\"}], \"score\": \"0-100\"}");
      return this;
    }

    /** Configures for security review. */
    public Builder forSecurityReview() {
      if (role == null)
        role = language == Language.PT_BR ? "especialista em segurança" : "security specialist";
      if (task == null)
        task = language == Language.PT_BR ? "Revise a segurança" : "Review security";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Identifique vulnerabilidades potenciais",
                "Classifique riscos por severidade",
                "Referencie CWE/OWASP quando aplicável",
                "Forneça recomendações de remediação"
              }
              : new String[] {
                "Identify potential vulnerabilities",
                "Classify risks by severity",
                "Reference CWE/OWASP when applicable",
                "Provide remediation recommendations"
              });
      withChainOfThought();
      outputAs(OutputFormat.JSON);
      return this;
    }

    /** Configures for performance optimization suggestions. */
    public Builder forPerformanceOptimization() {
      if (role == null)
        role =
            language == Language.PT_BR ? "especialista em performance" : "performance specialist";
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Identifique gargalos de performance",
                "Quantifique impacto quando possível",
                "Sugira otimizações específicas",
                "Considere trade-offs de cada otimização"
              }
              : new String[] {
                "Identify performance bottlenecks",
                "Quantify impact when possible",
                "Suggest specific optimizations",
                "Consider trade-offs of each optimization"
              });
      withChainOfThought();
      return this;
    }

    // ==================== RESPONSE CONTROL ====================

    /** Requests a response in bullet points only. */
    public Builder bulletPointsOnly() {
      outputAs(OutputFormat.BULLET_LIST);
      instruct(
          language == Language.PT_BR
              ? "Responda apenas com bullet points, sem parágrafos"
              : "Respond only with bullet points, no paragraphs");
      return this;
    }

    /** Requests an executive summary style response. */
    public Builder executiveSummary() {
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Comece com a conclusão principal",
                "Destaque apenas os pontos mais importantes",
                "Limite a 3-5 pontos-chave",
                "Use linguagem de negócios"
              }
              : new String[] {
                "Start with the main conclusion",
                "Highlight only the most important points",
                "Limit to 3-5 key points",
                "Use business language"
              });
      concise();
      return this;
    }

    /** Requests step-by-step format. */
    public Builder stepByStep() {
      outputAs(OutputFormat.NUMBERED_LIST);
      instruct(
          language == Language.PT_BR
              ? "Forneça instruções passo a passo claras e numeradas"
              : "Provide clear numbered step-by-step instructions");
      return this;
    }

    /** Requests response with specific word count. */
    public Builder wordCount(int min, int max) {
      instruct(
          language == Language.PT_BR
              ? "Mantenha a resposta entre " + min + " e " + max + " palavras"
              : "Keep the response between " + min + " and " + max + " words");
      return this;
    }

    /** Requests response with specific sentence count. */
    public Builder sentenceCount(int count) {
      instruct(
          language == Language.PT_BR
              ? "Responda em exatamente " + count + " frases"
              : "Respond in exactly " + count + " sentences");
      return this;
    }

    /** Configures to avoid certain topics or words. */
    public Builder avoid(String... items) {
      String avoidList = String.join(", ", items);
      constraint(
          language == Language.PT_BR
              ? "Evite mencionar ou usar: " + avoidList
              : "Avoid mentioning or using: " + avoidList);
      return this;
    }

    /** Configures to focus on specific topics. */
    public Builder focusOn(String... topics) {
      String topicList = String.join(", ", topics);
      instruct(
          language == Language.PT_BR
              ? "Foque especificamente em: " + topicList
              : "Focus specifically on: " + topicList);
      return this;
    }

    /** Adds a fallback instruction for when the AI cannot complete the task. */
    public Builder withFallback(String fallbackBehavior) {
      instruct(
          language == Language.PT_BR
              ? "Se não for possível completar a tarefa, " + fallbackBehavior
              : "If unable to complete the task, " + fallbackBehavior);
      return this;
    }

    /** Requests self-assessment of the response quality. */
    public Builder withSelfAssessment() {
      instruct(
          language == Language.PT_BR
              ? "Ao final, avalie a qualidade da sua resposta e indique áreas que poderiam ser"
                    + " melhoradas"
              : "At the end, assess the quality of your response and indicate areas that could be"
                    + " improved");
      return this;
    }

    /** Configures for handling ambiguous inputs. */
    public Builder handleAmbiguity(AmbiguityStrategy strategy) {
      String instruction =
          switch (strategy) {
            case ASK_CLARIFICATION ->
                language == Language.PT_BR
                    ? "Se a entrada for ambígua, peça esclarecimentos antes de responder"
                    : "If the input is ambiguous, ask for clarification before responding";
            case ASSUME_MOST_LIKELY ->
                language == Language.PT_BR
                    ? "Se a entrada for ambígua, assuma a interpretação mais provável e indique sua"
                          + " suposição"
                    : "If the input is ambiguous, assume the most likely interpretation and state"
                          + " your assumption";
            case PROVIDE_ALL_OPTIONS ->
                language == Language.PT_BR
                    ? "Se a entrada for ambígua, forneça respostas para todas as interpretações"
                          + " possíveis"
                    : "If the input is ambiguous, provide responses for all possible"
                          + " interpretations";
            case STATE_ASSUMPTIONS ->
                language == Language.PT_BR
                    ? "Declare claramente quaisquer suposições feitas na sua resposta"
                    : "Clearly state any assumptions made in your response";
          };
      instruct(instruction);
      return this;
    }

    /** Sets the difficulty level for explanations or content. */
    public Builder difficultyLevel(DifficultyLevel level) {
      String desc = level.getDescription(language);
      instruct(
          language == Language.PT_BR
              ? "Ajuste a complexidade para nível " + desc
              : "Adjust complexity for " + desc + " level");
      audienceLevel(desc);
      return this;
    }

    /** Sets the thinking style to use. */
    public Builder thinkingStyle(ThinkingStyle style) {
      String desc = style.getDescription(language);
      instruct(
          language == Language.PT_BR
              ? "Use uma abordagem de pensamento " + desc
              : "Use a " + desc + " thinking approach");
      return this;
    }

    /** Combines multiple thinking styles. */
    public Builder thinkingStyles(ThinkingStyle... styles) {
      for (ThinkingStyle style : styles) {
        thinkingStyle(style);
      }
      return this;
    }

    /** Requests numbered references/citations in the response. */
    public Builder withNumberedReferences() {
      instruct(
          language == Language.PT_BR
              ? "Use referências numeradas [1], [2], etc. e liste as fontes ao final"
              : "Use numbered references [1], [2], etc. and list sources at the end");
      return this;
    }

    /** Requests inline citations. */
    public Builder withInlineCitations() {
      instruct(
          language == Language.PT_BR
              ? "Inclua citações inline (Autor, Ano) quando referenciar informações"
              : "Include inline citations (Author, Year) when referencing information");
      return this;
    }

    /** Configures for a specific domain/industry. */
    public Builder forDomain(String domain) {
      addExpertise(domain);
      instruct(
          language == Language.PT_BR
              ? "Use terminologia e melhores práticas específicas de " + domain
              : "Use terminology and best practices specific to " + domain);
      return this;
    }

    /** Adds time constraints context. */
    public Builder withTimeContext(String timeContext) {
      addBackground(
          (language == Language.PT_BR ? "Contexto temporal: " : "Time context: ") + timeContext);
      return this;
    }

    /** Adds geographical/regional context. */
    public Builder withRegionalContext(String region) {
      addBackground(
          (language == Language.PT_BR ? "Contexto regional: " : "Regional context: ") + region);
      instruct(
          language == Language.PT_BR
              ? "Considere aspectos culturais e regulatórios de " + region
              : "Consider cultural and regulatory aspects of " + region);
      return this;
    }

    /** Requests a summary at the end of the response. */
    public Builder withSummaryAtEnd() {
      instruct(
          language == Language.PT_BR
              ? "Termine com um breve resumo dos pontos principais"
              : "End with a brief summary of the main points");
      return this;
    }

    /** Requests a TL;DR at the beginning of the response. */
    public Builder withTLDR() {
      instruct(
          language == Language.PT_BR
              ? "Comece com um TL;DR de 1-2 frases"
              : "Start with a 1-2 sentence TL;DR");
      return this;
    }

    /** Configures for structured thinking with explicit sections. */
    public Builder withStructuredThinking() {
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Organize sua resposta em seções claras",
                "Use cabeçalhos para cada seção",
                "Mantenha uma progressão lógica entre seções"
              }
              : new String[] {
                "Organize your response in clear sections",
                "Use headers for each section",
                "Maintain logical progression between sections"
              });
      return this;
    }

    /** Requests examples to be included in the response. */
    public Builder includeExamples(int count) {
      instruct(
          language == Language.PT_BR
              ? "Inclua pelo menos " + count + " exemplos práticos"
              : "Include at least " + count + " practical examples");
      return this;
    }

    /** Requests analogies to be used in explanations. */
    public Builder useAnalogies() {
      instruct(
          language == Language.PT_BR
              ? "Use analogias para tornar conceitos mais compreensíveis"
              : "Use analogies to make concepts more understandable");
      return this;
    }

    /** Configures for balanced/neutral perspective. */
    public Builder neutral() {
      instructions(
          language == Language.PT_BR
              ? new String[] {
                "Mantenha uma perspectiva equilibrada e imparcial",
                "Apresente múltiplos pontos de vista",
                "Evite linguagem tendenciosa"
              }
              : new String[] {
                "Maintain a balanced and impartial perspective",
                "Present multiple viewpoints",
                "Avoid biased language"
              });
      return this;
    }

    /** Configures for enthusiastic/positive tone. */
    public Builder enthusiastic() {
      tone(language == Language.PT_BR ? "entusiasmado e positivo" : "enthusiastic and positive");
      return this;
    }

    /** Configures for empathetic response style. */
    public Builder empathetic() {
      addPersonality(
          language == Language.PT_BR ? "empático" : "empathetic",
          language == Language.PT_BR ? "compreensivo" : "understanding");
      instruct(
          language == Language.PT_BR
              ? "Demonstre empatia e compreensão na resposta"
              : "Show empathy and understanding in the response");
      return this;
    }

    /** Configures response to be actionable. */
    public Builder actionable() {
      instruct(
          language == Language.PT_BR
              ? "Forneça recomendações práticas e acionáveis"
              : "Provide practical and actionable recommendations");
      return this;
    }

    /** Requests specific metrics or quantification. */
    public Builder withMetrics() {
      instruct(
          language == Language.PT_BR
              ? "Inclua métricas, números e quantificações quando possível"
              : "Include metrics, numbers, and quantifications when possible");
      return this;
    }

    /** Requests timeline or schedule format. */
    public Builder asTimeline() {
      instruct(
          language == Language.PT_BR
              ? "Organize a resposta em formato de linha do tempo cronológica"
              : "Organize the response in chronological timeline format");
      return this;
    }

    /** Requests prioritized list format. */
    public Builder asPrioritizedList() {
      instruct(
          language == Language.PT_BR
              ? "Organize itens em ordem de prioridade (mais importante primeiro)"
              : "Organize items in priority order (most important first)");
      outputAs(OutputFormat.NUMBERED_LIST);
      return this;
    }

    /** Configures for quick/immediate response needs. */
    public Builder quickResponse() {
      concise();
      instruct(
          language == Language.PT_BR
              ? "Vá direto ao ponto mais importante imediatamente"
              : "Get to the most important point immediately");
      return this;
    }

    /** Configures for comprehensive/thorough response. */
    public Builder comprehensive() {
      detailed();
      instruct(
          language == Language.PT_BR
              ? "Cubra todos os aspectos relevantes do tópico"
              : "Cover all relevant aspects of the topic");
      return this;
    }

    /** Adds version/compatibility context. */
    public Builder forVersion(String version) {
      addBackground(
          (language == Language.PT_BR ? "Versão/Compatibilidade: " : "Version/Compatibility: ")
              + version);
      return this;
    }

    /** Specifies the intended use case for the response. */
    public Builder forUseCase(String useCase) {
      addBackground((language == Language.PT_BR ? "Caso de uso: " : "Use case: ") + useCase);
      instruct(
          language == Language.PT_BR
              ? "Adapte a resposta para este caso de uso específico"
              : "Tailor the response for this specific use case");
      return this;
    }

    /** Specifies budget constraints context. */
    public Builder withBudgetContext(String budget) {
      addBackground((language == Language.PT_BR ? "Orçamento: " : "Budget: ") + budget);
      instruct(
          language == Language.PT_BR
              ? "Considere as restrições de orçamento nas recomendações"
              : "Consider budget constraints in recommendations");
      return this;
    }

    /** Configures for compliance with specific standards. */
    public Builder compliantWith(String... standards) {
      String standardsList = String.join(", ", standards);
      addBackground(
          (language == Language.PT_BR ? "Padrões de conformidade: " : "Compliance standards: ")
              + standardsList);
      instruct(
          language == Language.PT_BR
              ? "Garanta conformidade com: " + standardsList
              : "Ensure compliance with: " + standardsList);
      return this;
    }

    // ==================== TEMPLATES & CONFIG ====================

    public Builder apply(PromptTemplate template) {
      template.configure(this);
      return this;
    }

    public Builder configure(Consumer<Builder> configurator) {
      configurator.accept(this);
      return this;
    }

    public Builder when(boolean condition, Consumer<Builder> configurator) {
      if (condition) configurator.accept(this);
      return this;
    }

    public Builder when(Supplier<Boolean> condition, Consumer<Builder> configurator) {
      if (condition.get()) configurator.accept(this);
      return this;
    }

    // ==================== BUILD ====================

    public Prompt build() {
      StringBuilder sb = new StringBuilder();

      if (systemInstruction != null && !systemInstruction.isBlank()) {
        sb.append(delimiterStyle.wrap("system", systemInstruction)).append("\n");
      }

      if (role != null || !expertise.isEmpty() || !personality.isEmpty()) {
        sb.append(buildRoleSection()).append("\n");
      }

      if (!contextSections.isEmpty()) {
        sb.append(buildContextSection()).append("\n");
      }

      if (!conversationHistory.isEmpty()) {
        sb.append(buildConversationHistory()).append("\n");
      }

      if (goal != null) {
        sb.append(delimiterStyle.wrap("goal", goal)).append("\n");
      }
      if (task != null) {
        sb.append(delimiterStyle.wrap("task", task)).append("\n");
      }

      if (!instructions.isEmpty()) {
        sb.append(buildInstructionsSection()).append("\n");
      }

      if (!requirements.isEmpty() || !constraints.isEmpty()) {
        sb.append(buildRequirementsSection()).append("\n");
      }

      if (!examples.isEmpty()) {
        sb.append(buildExamplesSection()).append("\n");
      }

      if (!negativeExamples.isEmpty()) {
        sb.append(buildNegativeExamplesSection()).append("\n");
      }

      if (!tools.isEmpty()) {
        sb.append(buildToolsSection()).append("\n");
      }

      if (reActPattern) {
        sb.append(delimiterStyle.wrap("react_pattern", L10n.reactPattern(language))).append("\n");
      }

      if (targetAudience != null || audienceExpertiseLevel != null) {
        sb.append(buildAudienceSection()).append("\n");
      }

      if (outputFormat != null || outputSchema != null || outputDescription != null) {
        sb.append(buildOutputSection()).append("\n");
      }

      if (!reasoningStrategies.isEmpty() || customReasoningPrompt != null) {
        sb.append(buildReasoningSection()).append("\n");
      }

      for (Section section : customSections) {
        if (section.name() != null) {
          sb.append(delimiterStyle.wrap(section.name(), section.content())).append("\n");
        } else {
          sb.append(section.content()).append("\n");
        }
      }

      if (prefillText != null) {
        sb.append(delimiterStyle.wrap("assistant_start", prefillText)).append("\n");
      }

      if (inputData != null) {
        String label =
            inputLabel != null
                ? inputLabel.toLowerCase()
                : (language == Language.PT_BR ? "entrada" : "input");
        sb.append(delimiterStyle.wrap(label, inputData)).append("\n");
      }

      return Prompt.of(sb.toString().replaceAll("\n{3,}", "\n\n").trim());
    }

    public String buildAsString() {
      return build().content();
    }

    // ==================== PRIVATE HELPERS ====================

    private String buildRoleSection() {
      StringBuilder sb = new StringBuilder();
      sb.append(L10n.youAre(language));
      sb.append(role != null ? addArticle(role) : L10n.anAiAssistant(language));
      if (!expertise.isEmpty()) {
        sb.append(L10n.withExpertiseIn(language)).append(formatList(expertise));
      }
      if (!personality.isEmpty()) {
        sb.append(". ")
            .append(L10n.youAre(language).trim())
            .append(" ")
            .append(formatList(personality));
      }
      sb.append(".");
      if (tone != null) {
        sb.append(L10n.useATone(language, tone));
      }
      if (responseLanguage != null) {
        sb.append(L10n.respondIn(language, responseLanguage));
      }
      return sb.toString();
    }

    private String buildContextSection() {
      StringBuilder sb = new StringBuilder();
      for (ContextSection section : contextSections) {
        String name =
            section.type() == ContextType.DOCUMENT
                ? "document name=\"" + section.name() + "\""
                : section.type() == ContextType.BACKGROUND ? "background" : section.name();
        sb.append(delimiterStyle.wrap(name, section.content())).append("\n");
      }
      return sb.toString().trim();
    }

    private String buildConversationHistory() {
      return delimiterStyle.wrap(
          "conversation_history",
          conversationHistory.stream()
              .map(m -> m.role().toUpperCase() + ": " + m.content())
              .collect(Collectors.joining("\n")));
    }

    private String buildInstructionsSection() {
      if (instructions.size() == 1) return delimiterStyle.wrap("instructions", instructions.get(0));
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < instructions.size(); i++) {
        sb.append(i + 1).append(". ").append(instructions.get(i)).append("\n");
      }
      return delimiterStyle.wrap("instructions", sb.toString().trim());
    }

    private String buildRequirementsSection() {
      StringBuilder sb = new StringBuilder();
      if (!requirements.isEmpty()) {
        sb.append(L10n.requirements(language)).append("\n");
        for (String r : requirements) sb.append("- ").append(r).append("\n");
      }
      if (!constraints.isEmpty()) {
        if (!requirements.isEmpty()) sb.append("\n");
        sb.append(L10n.constraints(language)).append("\n");
        for (String c : constraints) sb.append("- ").append(c).append("\n");
      }
      return delimiterStyle.wrap("requirements", sb.toString().trim());
    }

    private String buildExamplesSection() {
      List<Example> list = new ArrayList<>(examples);
      if (shuffleExamples) Collections.shuffle(list);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < list.size(); i++) {
        Example ex = list.get(i);
        sb.append(L10n.example(language, i + 1))
            .append("\n")
            .append(L10n.input(language))
            .append(": ")
            .append(ex.input())
            .append("\n")
            .append(L10n.output(language))
            .append(": ")
            .append(ex.output());
        if (ex.explanation() != null)
          sb.append("\n").append(L10n.explanation(language)).append(": ").append(ex.explanation());
        if (i < list.size() - 1) sb.append("\n\n");
      }
      return delimiterStyle.wrap("examples", sb.toString());
    }

    private String buildNegativeExamplesSection() {
      StringBuilder sb = new StringBuilder();
      sb.append(L10n.avoidTheseExamples(language)).append("\n\n");
      for (int i = 0; i < negativeExamples.size(); i++) {
        NegativeExample ex = negativeExamples.get(i);
        sb.append(L10n.badExample(language, i + 1))
            .append("\n")
            .append(L10n.input(language))
            .append(": ")
            .append(ex.input())
            .append("\n")
            .append(L10n.badOutput(language))
            .append(": ")
            .append(ex.badOutput())
            .append("\n")
            .append(L10n.whyItsWrong(language))
            .append(": ")
            .append(ex.reason());
        if (i < negativeExamples.size() - 1) sb.append("\n\n");
      }
      return delimiterStyle.wrap("avoid", sb.toString());
    }

    private String buildToolsSection() {
      StringBuilder sb = new StringBuilder();
      sb.append(L10n.toolsAvailable(language)).append("\n\n");
      for (Tool tool : tools) {
        sb.append("- **").append(tool.name()).append("**: ").append(tool.description());
        if (!tool.parameters().isEmpty()) {
          sb.append("\n  ").append(L10n.parameters(language)).append(":\n");
          for (Map.Entry<String, ToolParameter> p : tool.parameters().entrySet()) {
            ToolParameter tp = p.getValue();
            sb.append("    - ")
                .append(p.getKey())
                .append(" (")
                .append(tp.type())
                .append(tp.required() ? ", required" : "")
                .append("): ")
                .append(tp.description());
            if (tp.enumValues() != null && !tp.enumValues().isEmpty())
              sb.append(" [").append(String.join(", ", tp.enumValues())).append("]");
            sb.append("\n");
          }
        }
        sb.append("\n");
      }
      sb.append(L10n.toUseTool(language));
      return delimiterStyle.wrap("tools", sb.toString().trim());
    }

    private String buildAudienceSection() {
      StringBuilder sb = new StringBuilder();
      if (targetAudience != null)
        sb.append(L10n.targetAudience(language)).append(": ").append(targetAudience);
      if (audienceExpertiseLevel != null) {
        if (sb.length() > 0) sb.append("\n");
        sb.append(L10n.expertiseLevel(language)).append(": ").append(audienceExpertiseLevel);
      }
      sb.append("\n").append(L10n.tailorForAudience(language));
      return delimiterStyle.wrap("audience", sb.toString());
    }

    private String buildOutputSection() {
      StringBuilder sb = new StringBuilder();
      if (outputFormat != null) sb.append(outputFormat.getInstruction(language));
      if (outputSchema != null)
        sb.append("\n\n").append(L10n.followThisSchema(language)).append("\n").append(outputSchema);
      if (outputDescription != null) {
        if (sb.length() > 0) sb.append("\n\n");
        sb.append(L10n.outputDescription(language)).append(": ").append(outputDescription);
      }
      if (!outputConstraints.isEmpty()) {
        sb.append("\n\n").append(L10n.outputRequirements(language)).append("\n");
        for (String c : outputConstraints) sb.append("- ").append(c).append("\n");
      }
      if (maxTokens != null) sb.append("\n").append(L10n.keepResponseUnder(language, maxTokens));
      return delimiterStyle.wrap("output_format", sb.toString().trim());
    }

    private String buildReasoningSection() {
      StringBuilder sb = new StringBuilder();
      if (customReasoningPrompt != null) {
        sb.append(customReasoningPrompt);
      } else {
        for (ReasoningStrategy s : reasoningStrategies) {
          if (sb.length() > 0) sb.append("\n");
          sb.append(s.getInstruction(language));
        }
      }
      return delimiterStyle.wrap("reasoning", sb.toString());
    }

    private String addArticle(String noun) {
      if (noun == null || noun.isEmpty()) return noun;
      char first = Character.toLowerCase(noun.charAt(0));
      if (language == Language.PT_BR) return "um " + noun;
      return (first == 'a' || first == 'e' || first == 'i' || first == 'o' || first == 'u'
              ? "an "
              : "a ")
          + noun;
    }

    private String formatList(List<String> items) {
      if (items.isEmpty()) return "";
      if (items.size() == 1) return items.get(0);
      if (items.size() == 2) return items.get(0) + L10n.and(language) + items.get(1);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < items.size() - 1; i++) sb.append(items.get(i)).append(", ");
      sb.append(language == Language.PT_BR ? "e " : "and ").append(items.get(items.size() - 1));
      return sb.toString();
    }

    private record Section(String name, String content) {}

    private record ContextSection(ContextType type, String name, String content) {}

    private enum ContextType {
      DOCUMENT,
      BACKGROUND,
      CUSTOM
    }
  }

  // ==================== SUB-BUILDERS ====================

  public static final class ContextBuilder {
    private final Builder parent;

    private ContextBuilder(Builder parent) {
      this.parent = parent;
    }

    public ContextBuilder document(String name, String content) {
      parent.contextSections.add(
          new Builder.ContextSection(Builder.ContextType.DOCUMENT, name, content));
      return this;
    }

    public ContextBuilder background(String info) {
      parent.contextSections.add(
          new Builder.ContextSection(Builder.ContextType.BACKGROUND, "background", info));
      return this;
    }

    public ContextBuilder section(String name, String content) {
      parent.contextSections.add(
          new Builder.ContextSection(Builder.ContextType.CUSTOM, name, content));
      return this;
    }

    public ContextBuilder data(String name, String data, String format) {
      parent.contextSections.add(
          new Builder.ContextSection(
              Builder.ContextType.CUSTOM, name, "Format: " + format + "\n\n" + data));
      return this;
    }

    public Builder done() {
      return parent;
    }
  }

  public static final class ExampleBuilder {
    private final Builder parent;

    private ExampleBuilder(Builder parent) {
      this.parent = parent;
    }

    public ExampleBuilder example(String input, String output) {
      parent.examples.add(Example.of(input, output));
      return this;
    }

    public ExampleBuilder exampleWithExplanation(String input, String output, String explanation) {
      parent.examples.add(Example.withExplanation(input, output, explanation));
      return this;
    }

    public ExampleBuilder add(Example example) {
      parent.examples.add(example);
      return this;
    }

    public ExampleBuilder addAll(List<Example> examples) {
      parent.examples.addAll(examples);
      return this;
    }

    public ExampleBuilder addAll(Map<String, String> examples) {
      examples.forEach((i, o) -> parent.examples.add(Example.of(i, o)));
      return this;
    }

    public ExampleBuilder reasoningExample(String input, String reasoning, String output) {
      parent.examples.add(
          new Example(
              input,
              reasoning
                  + "\n\n"
                  + (parent.language == Language.PT_BR ? "Resposta Final: " : "Final Answer: ")
                  + output,
              null));
      return this;
    }

    public ExampleBuilder shuffled() {
      parent.shuffleExamples = true;
      return this;
    }

    public ExampleBuilder validate(Predicate<Example> validator) {
      parent.examples = parent.examples.stream().filter(validator).collect(Collectors.toList());
      return this;
    }

    public ExampleBuilder limit(int max) {
      if (parent.examples.size() > max)
        parent.examples = new ArrayList<>(parent.examples.subList(0, max));
      return this;
    }

    public Builder done() {
      return parent;
    }
  }

  public static final class ToolBuilder {
    private final Builder parent;

    private ToolBuilder(Builder parent) {
      this.parent = parent;
    }

    public ToolDefinitionBuilder tool(String name, String description) {
      return new ToolDefinitionBuilder(this, name, description);
    }

    public ToolBuilder simpleTool(String name, String description) {
      parent.tools.add(Tool.of(name, description));
      return this;
    }

    public Builder done() {
      return parent;
    }
  }

  public static final class ToolDefinitionBuilder {
    private final ToolBuilder parent;
    private final String name;
    private final String description;
    private final Map<String, ToolParameter> parameters = new LinkedHashMap<>();

    private ToolDefinitionBuilder(ToolBuilder parent, String name, String description) {
      this.parent = parent;
      this.name = name;
      this.description = description;
    }

    public ToolDefinitionBuilder param(String name, ToolParameter param) {
      this.parameters.put(name, param);
      return this;
    }

    public ToolDefinitionBuilder stringParam(String name, String description) {
      return param(name, ToolParameter.string(description));
    }

    public ToolDefinitionBuilder intParam(String name, String description) {
      return param(name, ToolParameter.integer(description));
    }

    public ToolDefinitionBuilder boolParam(String name, String description) {
      return param(name, ToolParameter.bool(description));
    }

    public ToolDefinitionBuilder enumParam(String name, String description, String... values) {
      return param(name, ToolParameter.enumeration(description, values));
    }

    public ToolBuilder add() {
      parent.parent.tools.add(new Tool(name, description, parameters));
      return parent;
    }
  }

  // ==================== TEMPLATES ====================

  public static final class Templates {
    private Templates() {}

    public static PromptTemplate classification() {
      return b ->
          b.task("Classify the following text")
              .instruct("Respond only with the category name")
              .outputAs(OutputFormat.PLAIN_TEXT);
    }

    public static PromptTemplate sentimentAnalysis() {
      return b ->
          b.addRole("sentiment analysis expert")
              .task("Analyze the sentiment")
              .outputAs(OutputFormat.JSON)
              .outputSchema(
                  "{\"sentiment\": \"POSITIVE|NEUTRAL|NEGATIVE\", \"confidence\": \"0-1\","
                      + " \"reasoning\": \"...\"}");
    }

    public static PromptTemplate summarization() {
      return b ->
          b.addRole("professional summarizer")
              .task("Summarize the following content")
              .instructions(
                  "Capture main points", "Use clear language", "Maintain original meaning")
              .concise();
    }

    public static PromptTemplate dataExtraction() {
      return b ->
          b.addRole("data extraction specialist")
              .task("Extract structured data")
              .outputAs(OutputFormat.JSON)
              .instruct("Only extract explicitly mentioned information")
              .instruct("Use null for missing fields");
    }

    public static PromptTemplate codeGeneration(String lang) {
      return b ->
          b.addRole("expert " + lang + " developer")
              .task("Generate code")
              .instructions(
                  "Write clean code",
                  "Follow best practices",
                  "Include error handling",
                  "Add comments");
    }

    public static PromptTemplate codeReview() {
      return b -> b.forCodeReview();
    }

    public static PromptTemplate translation(String src, String tgt) {
      return b ->
          b.addRole("translator fluent in " + src + " and " + tgt)
              .task("Translate from " + src + " to " + tgt)
              .instructions(
                  "Maintain meaning and tone", "Use idiomatic expressions", "Preserve formatting");
    }

    public static PromptTemplate questionAnswering() {
      return b ->
          b.addRole("research assistant")
              .task("Answer based on context")
              .instructions(
                  "Base answer on provided context", "Say if not found", "Cite relevant parts")
              .withChainOfThought();
    }

    public static PromptTemplate creativeWriting() {
      return b ->
          b.addRole("creative writer")
              .addPersonality("imaginative", "engaging")
              .instruct("Use vivid descriptions")
              .detailed();
    }

    public static PromptTemplate mathReasoning() {
      return b ->
          b.addRole("mathematics expert")
              .task("Solve the problem")
              .withChainOfThought()
              .withSelfVerification()
              .instruct("Show all steps")
              .instruct("Verify answer");
    }

    public static PromptTemplate conversational() {
      return b ->
          b.addRole("helpful assistant")
              .addPersonality("friendly", "helpful")
              .instruct("Engage naturally")
              .instruct("Ask clarifying questions when needed");
    }

    public static PromptTemplate reactAgent() {
      return b ->
          b.addRole("AI agent")
              .withReActPattern()
              .withChainOfThought()
              .instruct("Think before acting")
              .instruct("Use appropriate tools");
    }

    public static PromptTemplate debugging() {
      return b -> b.forCodeDebugging("(error not specified)");
    }

    public static PromptTemplate contentModeration() {
      return b ->
          b.addRole("content moderator")
              .task("Analyze for policy violations")
              .outputAs(OutputFormat.JSON)
              .outputSchema(
                  "{\"is_safe\": true/false, \"violations\": [...], \"recommendation\":"
                      + " \"approve|flag|remove\"}");
    }

    public static PromptTemplate meetingSummary() {
      return b ->
          b.addRole("meeting summarizer")
              .task("Summarize transcript")
              .outputAs(OutputFormat.JSON)
              .outputSchema(
                  "{\"summary\": \"...\", \"key_decisions\": [...], \"action_items\": [...],"
                      + " \"open_questions\": [...]}");
    }

    public static PromptTemplate brainstorming() {
      return b -> b.forBrainstorming();
    }

    public static PromptTemplate socraticTutor() {
      return b -> b.asSocraticTutor();
    }

    public static PromptTemplate factChecking() {
      return b -> b.forFactChecking();
    }

    public static PromptTemplate tutorial() {
      return b -> b.forTutorial();
    }

    public static PromptTemplate comparison() {
      return b -> b.forComparison();
    }

    // New templates
    public static PromptTemplate interview() {
      return b -> b.forInterview();
    }

    public static PromptTemplate swotAnalysis() {
      return b -> b.forSWOTAnalysis();
    }

    public static PromptTemplate prosConsAnalysis() {
      return b -> b.forProsConsAnalysis();
    }

    public static PromptTemplate decisionMaking() {
      return b -> b.forDecisionMaking();
    }

    public static PromptTemplate rootCauseAnalysis() {
      return b -> b.forRootCauseAnalysis();
    }

    public static PromptTemplate emailDraft(EmailTone tone) {
      return b -> b.forEmailDrafting(tone);
    }

    public static PromptTemplate technicalWriting() {
      return b -> b.forTechnicalWriting();
    }

    public static PromptTemplate academicWriting(CitationStyle style) {
      return b -> b.forAcademicWriting(style);
    }

    public static PromptTemplate marketingCopy() {
      return b -> b.forMarketingCopy();
    }

    public static PromptTemplate storytelling() {
      return b -> b.forStorytelling();
    }

    public static PromptTemplate legalAnalysis() {
      return b -> b.forLegalAnalysis();
    }

    public static PromptTemplate medicalInfo() {
      return b -> b.forMedicalInformation();
    }

    public static PromptTemplate languageLearning(String target, String native_) {
      return b -> b.forLanguageLearning(target, native_);
    }

    public static PromptTemplate eli5() {
      return b -> b.forELI5();
    }

    public static PromptTemplate expertExplanation() {
      return b -> b.forExpertExplanation();
    }

    public static PromptTemplate quizGeneration(int n, QuestionType type) {
      return b -> b.forQuizGeneration(n, type);
    }

    public static PromptTemplate apiDocumentation() {
      return b -> b.forAPIDocumentation();
    }

    public static PromptTemplate changelog() {
      return b -> b.forChangelog();
    }

    public static PromptTemplate userStories() {
      return b -> b.forUserStories();
    }

    public static PromptTemplate dataVisualization() {
      return b -> b.forDataVisualization();
    }

    public static PromptTemplate accessibilityReview() {
      return b -> b.forAccessibilityReview();
    }

    public static PromptTemplate securityReview() {
      return b -> b.forSecurityReview();
    }

    public static PromptTemplate performanceOptimization() {
      return b -> b.forPerformanceOptimization();
    }

    public static PromptTemplate executiveSummary() {
      return b -> b.executiveSummary();
    }

    public static PromptTemplate stepByStep() {
      return b -> b.stepByStep();
    }

    // Persona-based templates
    public static PromptTemplate customerSupport() {
      return b ->
          b.addRole("customer support specialist")
              .addPersonality("helpful", "patient", "empathetic")
              .instructions(
                  "Address the customer's concern directly",
                  "Provide clear solutions",
                  "Maintain a positive tone",
                  "Offer follow-up assistance if needed");
    }

    public static PromptTemplate projectManager() {
      return b ->
          b.addRole("project manager")
              .addExpertise("project planning", "risk management", "team coordination")
              .instructions(
                  "Consider timeline and resources",
                  "Identify dependencies and risks",
                  "Provide actionable recommendations");
    }

    public static PromptTemplate uxDesigner() {
      return b ->
          b.addRole("UX designer")
              .addExpertise("user research", "interaction design", "usability")
              .instructions(
                  "Consider user needs and behaviors",
                  "Apply UX best practices",
                  "Provide specific design recommendations");
    }

    public static PromptTemplate dataScientist() {
      return b ->
          b.addRole("data scientist")
              .addExpertise("statistical analysis", "machine learning", "data visualization")
              .withChainOfThought()
              .instructions(
                  "Use appropriate statistical methods",
                  "Consider data quality and biases",
                  "Explain findings clearly");
    }

    public static PromptTemplate businessAnalyst() {
      return b ->
          b.addRole("business analyst")
              .addExpertise(
                  "requirements analysis", "process optimization", "stakeholder management")
              .instructions(
                  "Identify business needs",
                  "Analyze current state and gaps",
                  "Propose measurable solutions");
    }

    public static PromptTemplate technicalArchitect() {
      return b ->
          b.addRole("technical architect")
              .addExpertise("system design", "scalability", "integration patterns")
              .withChainOfThought()
              .instructions(
                  "Consider scalability and maintainability",
                  "Address non-functional requirements",
                  "Provide architecture diagrams when helpful");
    }

    public static PromptTemplate contentStrategist() {
      return b ->
          b.addRole("content strategist")
              .addExpertise("content planning", "SEO", "audience engagement")
              .instructions(
                  "Consider target audience",
                  "Optimize for discoverability",
                  "Maintain brand voice");
    }

    public static PromptTemplate financialAnalyst() {
      return b ->
          b.addRole("financial analyst")
              .addExpertise("financial modeling", "valuation", "risk analysis")
              .withChainOfThought()
              .instructions(
                  "Use appropriate financial metrics",
                  "Consider market conditions",
                  "Provide sensitivity analysis when relevant")
              .constraint("This is for informational purposes only and not financial advice");
    }
  }
}
