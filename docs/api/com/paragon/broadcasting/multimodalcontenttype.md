# :material-format-list-bulleted-type: MultimodalContentType

> This docs was updated at: 2026-02-23

`com.paragon.broadcasting.MultimodalContentType` &nbsp;·&nbsp; **Enum**

---

## Methods

### `content`

```java
TEXT,

  /** Image content (screenshots, generated images, diagrams). */
  IMAGE,

  /** Audio content (recordings, synthesized speech). */
  AUDIO,

  /** Video content (short clips, screencasts). */
  VIDEO,

  /** Document content (PDFs, HTML pages, text files, etc.). */
  DOCUMENT,

  /**
   * Arbitrary structured data / JSON payloads that are still treated as content rather than just
   * metadata (e.g. tool responses, search results).
   */
  STRUCTURED
}
```

Plain text or markdown content (prompts, completions, error messages).

---

### `content`

```java
IMAGE,

  /** Audio content (recordings, synthesized speech). */
  AUDIO,

  /** Video content (short clips, screencasts). */
  VIDEO,

  /** Document content (PDFs, HTML pages, text files, etc.). */
  DOCUMENT,

  /**
   * Arbitrary structured data / JSON payloads that are still treated as content rather than just
   * metadata (e.g. tool responses, search results).
   */
  STRUCTURED
}
```

Image content (screenshots, generated images, diagrams).

---

### `content`

```java
AUDIO,

  /** Video content (short clips, screencasts). */
  VIDEO,

  /** Document content (PDFs, HTML pages, text files, etc.). */
  DOCUMENT,

  /**
   * Arbitrary structured data / JSON payloads that are still treated as content rather than just
   * metadata (e.g. tool responses, search results).
   */
  STRUCTURED
}
```

Audio content (recordings, synthesized speech).

---

### `content`

```java
VIDEO,

  /** Document content (PDFs, HTML pages, text files, etc.). */
  DOCUMENT,

  /**
   * Arbitrary structured data / JSON payloads that are still treated as content rather than just
   * metadata (e.g. tool responses, search results).
   */
  STRUCTURED
}
```

Video content (short clips, screencasts).

---

### `metadata`

```java
DOCUMENT,

  /**
   * Arbitrary structured data / JSON payloads that are still treated as content rather than just
   * metadata (e.g. tool responses, search results).
   */
  STRUCTURED
}
```

Document content (PDFs, HTML pages, text files, etc.).
