package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonValue;

/** Supported PDF page formats. */
public enum PdfFormat {
  A0("A0"),
  A1("A1"),
  A2("A2"),
  A3("A3"),
  A4("A4"),
  A5("A5"),
  A6("A6"),
  LETTER("Letter"),
  LEGAL("Legal"),
  TABLOID("Tabloid"),
  LEDGER("Ledger");

  private final String value;

  PdfFormat(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
