# :material-approximately-equal: EmbeddingProvider

> This docs was updated at: 2026-02-23

`com.paragon.embeddings.EmbeddingProvider` &nbsp;·&nbsp; **Interface**

---

Interface for embedding providers.

Uses synchronous API optimized for Java 21+ virtual threads.

## Methods

### `createEmbeddings`

```java
List<Embedding> createEmbeddings(@NonNull List<String> input, @NonNull String model)
```

Creates embeddings for the given inputs.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input texts to embed |
| `model` | the embedding model to use |

**Returns**

the list of embeddings

