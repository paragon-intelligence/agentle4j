# :material-code-braces: Prompt

> This docs was updated at: 2026-02-23

`com.paragon.prompts.Prompt` &nbsp;·&nbsp; **Class**

---

Represents an immutable text prompt that can contain template expressions.

A Prompt instance manages text content that can include various template features like
variable placeholders (`{{variable_name`}}), conditional blocks (`{{#if
condition`}...{{/if}}}), and iteration blocks (`{{#each items`}...{{/each}}}).

This class is immutable and thread-safe. All template processing methods return new Prompt
instances.

### Builder Features

  
- **Order Independent:** All builder methods can be called in any order
- **No Dependencies:** No method requires another method to be called first
- **Additive:** Methods add to existing configuration rather than replacing
- **Multi-language:** Supports English (US) and Portuguese (BR)

### Builder Usage Examples

```java
// Methods can be called in any order - these produce equivalent results:
Prompt prompt1 = Prompt.builder()
    .task("Analyze the data")
    .role("data analyst")
    .withChainOfThought()
    .build();
Prompt prompt2 = Prompt.builder()
    .withChainOfThought()
    .role("data analyst")
    .task("Analyze the data")
    .build();
// Multi-language support
Prompt ptBrPrompt = Prompt.builder()
    .language(Language.PT_BR)
    .role("analista de dados")
    .task("Analise os dados de vendas")
    .build();
```

*Since: 1.0*

## Methods

### `of`

```java
public static Prompt of(String text)
```

Creates a new Prompt from text content.

---

### `fromText`

```java
public static Prompt fromText(String text)
```

Creates a new Prompt from text content (alias for `.of(String)`).

---

### `empty`

```java
public static Prompt empty()
```

Creates a new empty Prompt.

---

### `builder`

```java
public static Builder builder()
```

Creates a new Builder for constructing prompts with best practices.

**Order Independence:** All builder methods can be called in any order. The final prompt
is assembled when `Builder.build()` is called.

**No Dependencies:** No method requires another method to be called. Each method is
independent and optional.

**Returns**

a new Builder instance

---

### `builder`

```java
public static Builder builder(PromptTemplate template)
```

Creates a Builder pre-configured for a specific use case.

---

### `builder`

```java
public static Builder builder(Language language)
```

Creates a Builder with a specific language.

**Parameters**

| Name | Description |
|------|-------------|
| `language` | the language for the prompt |

**Returns**

a new Builder with the language set

---

### `forTask`

```java
public static Builder forTask(String taskInstruction)
```

Creates a Builder starting with a specific task instruction.

---

### `forExtraction`

```java
public static Builder forExtraction(String whatToExtract)
```

Creates a Builder configured for extracting structured data.

---

### `forClassification`

```java
public static Builder forClassification(String... categories)
```

Creates a Builder configured for classification tasks.

---

### `forReasoning`

```java
public static Builder forReasoning(String problem)
```

Creates a Builder configured for multi-step reasoning problems.

---

### `forCode`

```java
public static Builder forCode(String language)
```

Creates a Builder configured for code generation.

---

### `forRAG`

```java
public static Builder forRAG()
```

Creates a Builder configured for RAG (Retrieval Augmented Generation).

---

### `forConversation`

```java
public static Builder forConversation()
```

Creates a Builder configured for conversational AI.

---

### `forTranslation`

```java
public static Builder forTranslation(String sourceLanguage, String targetLanguage)
```

Creates a Builder configured for translation tasks.

---

### `forSummarization`

```java
public static Builder forSummarization()
```

Creates a Builder configured for summarization tasks.

---

### `forQA`

```java
public static Builder forQA()
```

Creates a Builder configured for question answering.

---

### `forCreativeWriting`

```java
public static Builder forCreativeWriting()
```

Creates a Builder configured for creative writing.

---

### `forDataAnalysis`

```java
public static Builder forDataAnalysis()
```

Creates a Builder configured for data analysis.

---

### `forInterview`

```java
public Builder forInterview()
```

Configures for conducting an interview.

---

### `forDebate`

```java
public Builder forDebate(String position)
```

Configures for a debate format.

---

### `forRolePlay`

```java
public Builder forRolePlay(String character, String scenario)
```

Configures for role-playing scenarios.

---

### `forSWOTAnalysis`

```java
public Builder forSWOTAnalysis()
```

Configures for SWOT analysis.

---

### `forProsConsAnalysis`

```java
public Builder forProsConsAnalysis()
```

Configures for pros and cons analysis.

---

### `forDecisionMaking`

```java
public Builder forDecisionMaking()
```

Configures for decision-making framework.

---

### `forRootCauseAnalysis`

```java
public Builder forRootCauseAnalysis()
```

Configures for root cause analysis (5 Whys).

---

### `forEmailDrafting`

```java
public Builder forEmailDrafting(EmailTone emailTone)
```

Configures for email drafting.

---

### `forTechnicalWriting`

```java
public Builder forTechnicalWriting()
```

Configures for technical writing.

---

### `forAcademicWriting`

```java
public Builder forAcademicWriting(CitationStyle citationStyle)
```

Configures for scientific/academic writing.

---

### `forMarketingCopy`

```java
public Builder forMarketingCopy()
```

Configures for persuasive/marketing copy.

---

### `forStorytelling`

```java
public Builder forStorytelling()
```

Configures for storytelling/narrative.

---

### `forLegalAnalysis`

```java
public Builder forLegalAnalysis()
```

Configures for legal document analysis (with disclaimer).

---

### `forMedicalInformation`

```java
public Builder forMedicalInformation()
```

Configures for medical information (with strong disclaimer).

---

### `forLanguageLearning`

```java
public Builder forLanguageLearning(String targetLang, String nativeLang)
```

Configures for language learning assistance.

---

### `forELI5`

```java
public Builder forELI5()
```

Configures for ELI5 (Explain Like I'm 5) style explanations.

---

### `forExpertExplanation`

```java
public Builder forExpertExplanation()
```

Configures for expert-level explanation.

---

### `forQuizGeneration`

```java
public Builder forQuizGeneration(int numQuestions, QuestionType questionType)
```

Configures for quiz/test generation.

---

### `forAPIDocumentation`

```java
public Builder forAPIDocumentation()
```

Configures for API documentation generation.

---

### `forChangelog`

```java
public Builder forChangelog()
```

Configures for changelog generation.

---

### `forUserStories`

```java
public Builder forUserStories()
```

Configures for user story generation.

---

### `forDataVisualization`

```java
public Builder forDataVisualization()
```

Configures for data visualization suggestions.

---

### `forAccessibilityReview`

```java
public Builder forAccessibilityReview()
```

Configures for accessibility review.

---

### `forSecurityReview`

```java
public Builder forSecurityReview()
```

Configures for security review.

---

### `forPerformanceOptimization`

```java
public Builder forPerformanceOptimization()
```

Configures for performance optimization suggestions.

---

### `bulletPointsOnly`

```java
public Builder bulletPointsOnly()
```

Requests a response in bullet points only.

---

### `executiveSummary`

```java
public Builder executiveSummary()
```

Requests an executive summary style response.

---

### `stepByStep`

```java
public Builder stepByStep()
```

Requests step-by-step format.

---

### `wordCount`

```java
public Builder wordCount(int min, int max)
```

Requests response with specific word count.

---

### `sentenceCount`

```java
public Builder sentenceCount(int count)
```

Requests response with specific sentence count.

---

### `avoid`

```java
public Builder avoid(String... items)
```

Configures to avoid certain topics or words.

---

### `focusOn`

```java
public Builder focusOn(String... topics)
```

Configures to focus on specific topics.

---

### `withFallback`

```java
public Builder withFallback(String fallbackBehavior)
```

Adds a fallback instruction for when the AI cannot complete the task.

---

### `withSelfAssessment`

```java
public Builder withSelfAssessment()
```

Requests self-assessment of the response quality.

---

### `handleAmbiguity`

```java
public Builder handleAmbiguity(AmbiguityStrategy strategy)
```

Configures for handling ambiguous inputs.

---

### `difficultyLevel`

```java
public Builder difficultyLevel(DifficultyLevel level)
```

Sets the difficulty level for explanations or content.

---

### `thinkingStyle`

```java
public Builder thinkingStyle(ThinkingStyle style)
```

Sets the thinking style to use.

---

### `thinkingStyles`

```java
public Builder thinkingStyles(ThinkingStyle... styles)
```

Combines multiple thinking styles.

---

### `withNumberedReferences`

```java
public Builder withNumberedReferences()
```

Requests numbered references/citations in the response.

---

### `withInlineCitations`

```java
public Builder withInlineCitations()
```

Requests inline citations.

---

### `forDomain`

```java
public Builder forDomain(String domain)
```

Configures for a specific domain/industry.

---

### `withTimeContext`

```java
public Builder withTimeContext(String timeContext)
```

Adds time constraints context.

---

### `withRegionalContext`

```java
public Builder withRegionalContext(String region)
```

Adds geographical/regional context.

---

### `withSummaryAtEnd`

```java
public Builder withSummaryAtEnd()
```

Requests a summary at the end of the response.

---

### `withTLDR`

```java
public Builder withTLDR()
```

Requests a TL;DR at the beginning of the response.

---

### `withStructuredThinking`

```java
public Builder withStructuredThinking()
```

Configures for structured thinking with explicit sections.

---

### `includeExamples`

```java
public Builder includeExamples(int count)
```

Requests examples to be included in the response.

---

### `useAnalogies`

```java
public Builder useAnalogies()
```

Requests analogies to be used in explanations.

---

### `neutral`

```java
public Builder neutral()
```

Configures for balanced/neutral perspective.

---

### `enthusiastic`

```java
public Builder enthusiastic()
```

Configures for enthusiastic/positive tone.

---

### `empathetic`

```java
public Builder empathetic()
```

Configures for empathetic response style.

---

### `actionable`

```java
public Builder actionable()
```

Configures response to be actionable.

---

### `withMetrics`

```java
public Builder withMetrics()
```

Requests specific metrics or quantification.

---

### `asTimeline`

```java
public Builder asTimeline()
```

Requests timeline or schedule format.

---

### `asPrioritizedList`

```java
public Builder asPrioritizedList()
```

Requests prioritized list format.

---

### `quickResponse`

```java
public Builder quickResponse()
```

Configures for quick/immediate response needs.

---

### `comprehensive`

```java
public Builder comprehensive()
```

Configures for comprehensive/thorough response.

---

### `forVersion`

```java
public Builder forVersion(String version)
```

Adds version/compatibility context.

---

### `forUseCase`

```java
public Builder forUseCase(String useCase)
```

Specifies the intended use case for the response.

---

### `withBudgetContext`

```java
public Builder withBudgetContext(String budget)
```

Specifies budget constraints context.

---

### `compliantWith`

```java
public Builder compliantWith(String... standards)
```

Configures for compliance with specific standards.
