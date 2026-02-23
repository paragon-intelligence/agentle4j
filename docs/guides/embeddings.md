# Embeddings Guide

> This docs was updated at: 2026-02-23

Text embeddings convert words and sentences into numerical vectors that capture semantic meaning. Agentle4j provides embedding support through OpenRouter with built-in retry handling.

---

## Quick Start

```java
import com.paragon.embeddings.EmbeddingProvider;
import com.paragon.embeddings.OpenRouterEmbeddingProvider;
import com.paragon.embeddings.Embedding;

EmbeddingProvider embeddings = OpenRouterEmbeddingProvider.builder()
    .apiKey(System.getenv("OPENROUTER_API_KEY"))
    .build();

List<Embedding> results = embeddings.createEmbeddings(
    List.of("Hello world", "AI is amazing"),
    "openai/text-embedding-3-small"
);

// Access the vector
float[] vector = results.get(0).embedding();
System.out.println("Dimensions: " + vector.length);  // 1536 for text-embedding-3-small
```

---

## Use Cases

| Use Case | Description |
|----------|-------------|
| **Semantic Search** | Find documents by meaning, not just keywords |
| **Similarity Matching** | Compare how related two texts are |
| **Clustering** | Group similar documents together |
| **RAG** | Retrieval-Augmented Generation for AI assistants |
| **Recommendations** | Find similar items based on descriptions |

---

## Creating Embeddings

### Single Text

```java
List<Embedding> results = embeddings.createEmbeddings(
    List.of("Hello world"),
    "openai/text-embedding-3-small"
);

Embedding embedding = results.get(0);
float[] vector = embedding.embedding();
int index = embedding.index();  // Position in the input list
```

### Batch Processing

```java
List<String> documents = List.of(
    "Machine learning is a subset of AI",
    "Deep learning uses neural networks",
    "Natural language processing handles text",
    "Computer vision analyzes images"
);

List<Embedding> embeddings = provider.createEmbeddings(
    documents,
    "openai/text-embedding-3-small"
);

// Each embedding corresponds to the input at the same index
for (int i = 0; i < embeddings.size(); i++) {
    System.out.println(documents.get(i) + " → " + embeddings.get(i).embedding().length + " dims");
}
```

---

## Configuration

### Builder Options

```java
OpenRouterEmbeddingProvider provider = OpenRouterEmbeddingProvider.builder()
    .apiKey("your-api-key")           // Required (or set OPENROUTER_API_KEY env var)
    .retryPolicy(RetryPolicy.builder()
        .maxRetries(5)
        .initialDelay(Duration.ofMillis(500))
        .build())
    .allowFallbacks(true)             // Use backup providers on overload
    .objectMapper(customMapper)       // Custom JSON mapper
    .build();
```

### Environment Variable

If no API key is provided, it's loaded from `OPENROUTER_API_KEY`:

```java
// Loads from environment automatically
OpenRouterEmbeddingProvider provider = OpenRouterEmbeddingProvider.builder()
    .build();
```

---

## Retry Behavior

The provider automatically retries on transient failures:

| Status Code | Meaning | Retry Behavior |
|-------------|---------|----------------|
| **429** | Rate limit exceeded | ✅ Retry with exponential backoff |
| **529** | Provider overloaded | ✅ Retry + use fallback providers |
| **5xx** | Server errors | ✅ Retry with exponential backoff |
| **4xx** | Client errors | ❌ Fail immediately |

### Fallback Providers

When `allowFallbacks(true)` is set (default), OpenRouter automatically routes to backup embedding providers if the primary is overloaded:

```java
OpenRouterEmbeddingProvider provider = OpenRouterEmbeddingProvider.builder()
    .apiKey(apiKey)
    .allowFallbacks(true)  // Default: enabled
    .build();
```

---

## Available Models

Popular embedding models on OpenRouter:

| Model | Dimensions | Best For |
|-------|-----------|----------|
| `openai/text-embedding-3-small` | 1536 | General purpose, good balance |
| `openai/text-embedding-3-large` | 3072 | Higher quality, more expensive |
| `openai/text-embedding-ada-002` | 1536 | Legacy, widely compatible |

---

## Calculating Similarity

Use cosine similarity to compare embeddings:

```java
public static double cosineSimilarity(float[] a, float[] b) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    
    for (int i = 0; i < a.length; i++) {
        dotProduct += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}

// Usage
List<Embedding> embeddings = provider.createEmbeddings(
    List.of("cats are great pets", "dogs are loyal companions", "java programming"),
    "openai/text-embedding-3-small"
);

double catsDogs = cosineSimilarity(
    embeddings.get(0).embedding(),
    embeddings.get(1).embedding()
);
double catsJava = cosineSimilarity(
    embeddings.get(0).embedding(),
    embeddings.get(2).embedding()
);

System.out.println("cats vs dogs: " + catsDogs);   // ~0.8 (similar)
System.out.println("cats vs java: " + catsJava);   // ~0.3 (different)
```

---

## Resource Management

Close the provider when done to release HTTP connections:

```java
OpenRouterEmbeddingProvider provider = OpenRouterEmbeddingProvider.builder()
    .apiKey(apiKey)
    .build();

try {
    // Use provider...
} finally {
    provider.close();
}
```

---

## Best Practices

### ✅ Do

```java
// Batch embeddings for efficiency
List<Embedding> batch = provider.createEmbeddings(
    largeDocumentList,  // Process many at once
    model
);

// Use appropriate model for your use case
.model("openai/text-embedding-3-small")  // Good balance

// Enable fallbacks for reliability
.allowFallbacks(true)
```

### ❌ Don't

```java
// Don't create embeddings one at a time in a loop
for (String doc : documents) {
    provider.createEmbeddings(List.of(doc), model);  // Slow!
}

// Don't ignore cleanup
provider.close();  // Always close when done
```

---

## Next Steps

- [Responder Guide](responder.md) - Core API client
- [Agents Guide](agents.md) - Build AI agents with tools
