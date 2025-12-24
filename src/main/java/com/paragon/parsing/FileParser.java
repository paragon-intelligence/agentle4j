package com.paragon.parsing;

public interface FileParser {
  default ParsedFile parse(File file) {
    return parse(file, ParsingOptions.withDefaultOptions());
  }

  ParsedFile parse(File file, ParsingOptions options);
}
