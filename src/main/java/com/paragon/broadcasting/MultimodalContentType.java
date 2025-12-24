package com.paragon.broadcasting;

public enum MultimodalContentType {
  /** Plain text or markdown content (prompts, completions, error messages). */
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
