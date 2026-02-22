#!/usr/bin/env python3
"""
javadoc2mkdocs.py — Parses Java source files and generates MkDocs Markdown pages.

Usage:
    python tools/javadoc2mkdocs.py [OPTIONS]

Options:
    --src       Path to Java source root (default: src/main/java)
    --out       Path to MkDocs output dir  (default: docs/api)
    --base-pkg  Root package to document   (default: all packages)
    --nav       Print the MkDocs nav YAML block for mkdocs.yml
    --clean     Delete all files in --out before generating
    --verbose   Show per-file progress
"""

from __future__ import annotations

import argparse
import html
import re
import shutil
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

# ──────────────────────────────────────────────────────────────────────────────
# Data model
# ──────────────────────────────────────────────────────────────────────────────

@dataclass
class Tag:
    name: str       # e.g. "param", "return", "throws"
    arg: str        # first token after the tag (parameter name, exception type …)
    text: str       # rest of the description


@dataclass
class MemberDoc:
    kind: str            # "method" | "field" | "constant" | "element"
    name: str
    signature: str       # raw signature line
    description: str
    tags: list[Tag] = field(default_factory=list)
    modifiers: list[str] = field(default_factory=list)
    deprecated: bool = False


@dataclass
class ClassDoc:
    package: str
    name: str
    kind: str            # "class" | "interface" | "enum" | "record" | "annotation" | "@interface"
    description: str
    tags: list[Tag] = field(default_factory=list)
    members: list[MemberDoc] = field(default_factory=list)
    modifiers: list[str] = field(default_factory=list)
    source_file: Optional[Path] = None
    deprecated: bool = False
    extends: Optional[str] = None
    implements: list[str] = field(default_factory=list)


# ──────────────────────────────────────────────────────────────────────────────
# Javadoc comment parser
# ──────────────────────────────────────────────────────────────────────────────

# Tags that take a leading "argument" (name / type) before the description
_ARG_TAGS = {"param", "throws", "exception", "see", "uses", "provides"}

_INLINE_TAG_RE = re.compile(r"\{@(\w+)\s+([^}]*)\}")
_HTML_TAG_RE = re.compile(r"<[^>]+>")
_LEADING_STAR_RE = re.compile(r"^\s*\*\s?", re.MULTILINE)


def _strip_javadoc_stars(raw: str) -> str:
    """Remove /** … */ delimiters and leading * characters."""
    raw = raw.strip()
    if raw.startswith("/**"):
        raw = raw[3:]
    if raw.endswith("*/"):
        raw = raw[:-2]
    return _LEADING_STAR_RE.sub("", raw).strip()


def _inline_tags_to_md(text: str) -> str:
    """Convert {@code …}, {@link …}, {@linkplain …}, {@literal …} to Markdown."""
    def replace(m: re.Match) -> str:
        tag, content = m.group(1), m.group(2).strip()
        if tag == "code":
            return f"`{content}`"
        if tag in ("link", "linkplain"):
            # e.g. {@link SomeClass#method()} → `SomeClass.method()`
            label = content.replace("#", ".").split(" ", 1)
            ref = label[0]
            display = label[1] if len(label) > 1 else ref
            return f"`{display}`"
        if tag == "literal":
            return content
        if tag == "value":
            return f"`{content}`"
        return content
    return _INLINE_TAG_RE.sub(replace, text)


def _html_to_md(text: str) -> str:
    """Convert common HTML fragments that appear in Javadoc to Markdown.

    Note: <pre> blocks are handled upstream in _convert_description before this
    function is called, so we do not process them here.
    """
    # Headings
    for i in range(6, 0, -1):
        text = re.sub(rf"<h{i}[^>]*>(.*?)</h{i}>", rf"\n{'#' * (i + 1)} \1\n", text, flags=re.DOTALL | re.IGNORECASE)
    # Lists
    text = re.sub(r"<ul[^>]*>|</ul>|<ol[^>]*>|</ol>", "", text, flags=re.IGNORECASE)
    text = re.sub(r"<li[^>]*>(.*?)(?=<li|</[uo]l>|$)", lambda m: f"\n- {m.group(1).strip()}", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"</li>", "", text, flags=re.IGNORECASE)
    # Inline formatting
    text = re.sub(r"<b>(.*?)</b>", r"**\1**", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<strong>(.*?)</strong>", r"**\1**", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<i>(.*?)</i>", r"*\1*", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<em>(.*?)</em>", r"*\1*", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<code>(.*?)</code>", r"`\1`", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<tt>(.*?)</tt>", r"`\1`", text, flags=re.DOTALL | re.IGNORECASE)
    # Paragraphs / line breaks
    text = re.sub(r"<p\s*/?>", "\n\n", text, flags=re.IGNORECASE)
    text = re.sub(r"</p>", "", text, flags=re.IGNORECASE)
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.IGNORECASE)
    # Strip remaining tags
    text = _HTML_TAG_RE.sub("", text)
    # Decode HTML entities
    text = html.unescape(text)
    return text


def _convert_description(raw: str) -> str:
    """Full pipeline: strip stars → pre-blocks → inline tags → HTML → clean up."""
    text = _strip_javadoc_stars(raw) if raw.startswith("/**") else raw
    # Handle <pre>{@code ...}</pre> BEFORE inline-tag conversion so the {@code
    # is not double-processed into backticks.
    text = re.sub(
        r"<pre>\{@code\s*(.*?)\s*\}</pre>",
        lambda m: "\n```java\n" + m.group(1).strip() + "\n```\n",
        text, flags=re.DOTALL,
    )
    # Plain <pre>...</pre> (no {@code})
    text = re.sub(
        r"<pre>(.*?)</pre>",
        lambda m: "\n```\n" + m.group(1).strip() + "\n```\n",
        text, flags=re.DOTALL,
    )
    text = _inline_tags_to_md(text)
    text = _html_to_md(text)
    # Collapse more than two consecutive blank lines
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def _parse_javadoc_block(raw_comment: str) -> tuple[str, list[Tag]]:
    """
    Split a raw Javadoc block into (description, [Tag, …]).
    Returns plain text (not yet HTML-converted) for the description.
    """
    body = _strip_javadoc_stars(raw_comment)

    # Split on block tags (@param, @return …) — must be at start of a line
    tag_split = re.split(r"\n\s*@", body)
    description_raw = tag_split[0].strip()

    tags: list[Tag] = []
    for chunk in tag_split[1:]:
        # chunk looks like "param foo the foo parameter\n   more text"
        lines = chunk.splitlines()
        first = lines[0].strip()
        rest = "\n".join(lines[1:]).strip()
        full_text = (first + " " + rest).strip()

        # Identify the tag name
        parts = first.split(None, 1)
        tag_name = parts[0].lower()
        remainder = parts[1] if len(parts) > 1 else ""
        # Append continuation lines
        if rest:
            remainder = (remainder + " " + rest).strip()

        if tag_name in _ARG_TAGS:
            sub = remainder.split(None, 1)
            arg = sub[0] if sub else ""
            desc = sub[1] if len(sub) > 1 else ""
        else:
            arg = ""
            desc = remainder

        tags.append(Tag(name=tag_name, arg=arg, text=desc.strip()))

    return description_raw, tags


# ──────────────────────────────────────────────────────────────────────────────
# Java source parser
# ──────────────────────────────────────────────────────────────────────────────

# Regex patterns for Java constructs
_PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
_JAVADOC_RE = re.compile(r"/\*\*.*?\*/", re.DOTALL)

# Modifiers we care about
_MODIFIERS = {"public", "protected", "private", "static", "abstract", "final",
              "default", "synchronized", "native", "strictfp", "sealed", "non-sealed"}

_CLASS_DECL_RE = re.compile(
    r"(?:(?:public|protected|private|static|abstract|final|sealed|non-sealed)\s+)*"
    r"(?P<kind>class|interface|enum|record|@interface)\s+"
    r"(?P<name>\w+)"
    r"(?P<generics><[^{;]*?>)?"
    r"(?:\s+extends\s+(?P<extends>[\w.<>, ]+?))?"
    r"(?:\s+implements\s+(?P<implements>[\w.<>, ]+?))?"
    r"(?:\s+permits\s+[\w.<>, ]+?)?"
    r"\s*[{(]"
)

_METHOD_RE = re.compile(
    r"(?:(?:public|protected|private|static|abstract|final|default|synchronized|native|override)\s+)*"
    r"(?:@\w+(?:\([^)]*\))?\s+)*"  # annotations before return type
    r"(?:[\w.<>\[\]@?]+\s+)+"      # return type (may have generics / annotations)
    r"(\w+)\s*\("                   # method name
    r"([^)]*)\)"                    # parameters
    r"(?:\s+throws\s+[\w.,\s<>]+)?" # throws
    r"\s*[{;]"
)

_FIELD_RE = re.compile(
    r"(?:(?:public|protected|private|static|final|volatile|transient)\s+)+"
    r"(?:[\w.<>\[\]@?]+\s+)+"       # type
    r"(\w+)"                         # field name
    r"\s*(?:=|;)"
)


def _extract_modifiers(text_before: str) -> list[str]:
    return [w for w in text_before.split() if w in _MODIFIERS]


def _guess_member_kind(text: str) -> str:
    """Classify a member declaration snippet."""
    stripped = text.strip()
    if re.search(r"\w+\s*\(", stripped):
        return "method"
    return "field"


def _parse_member_signature(sig_line: str) -> tuple[str, str]:
    """Return (name, cleaned_signature)."""
    # Try to find a method name
    m = re.search(r"(\w+)\s*\(", sig_line)
    if m:
        return m.group(1), sig_line.strip()
    # Field: last identifier before = or ;
    m = re.search(r"(\w+)\s*(?:[=;]|$)", sig_line)
    if m:
        return m.group(1), sig_line.strip()
    return "unknown", sig_line.strip()


def _find_class_body_pairs(source: str) -> list[tuple[str, int, int]]:
    """
    Scan source for class/interface/enum/record/@interface declarations.
    Returns list of (declaration_header, start_of_header, start_of_body).
    start_of_body points to the '{' or '(' that opens the body.
    """
    results = []
    for m in _CLASS_DECL_RE.finditer(source):
        results.append((m.group(0), m.start(), m.end()))
    return results


def parse_java_file(path: Path) -> Optional[ClassDoc]:
    """Parse a single .java file and return a ClassDoc (or None on failure)."""
    try:
        source = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return None

    # Package
    pkg_m = _PACKAGE_RE.search(source)
    package = pkg_m.group(1) if pkg_m else ""

    # Find all Javadoc blocks with their positions
    javadoc_blocks: list[tuple[int, int, str]] = [
        (m.start(), m.end(), m.group(0)) for m in _JAVADOC_RE.finditer(source)
    ]

    # Find the primary class declaration (first top-level)
    class_decls = _find_class_body_pairs(source)
    if not class_decls:
        return None

    primary_decl_text, primary_start, primary_body_start = class_decls[0]

    # Determine kind and name from the primary declaration
    kind_m = re.search(
        r"(?P<kind>@interface|class|interface|enum|record)\s+(?P<name>\w+)",
        primary_decl_text
    )
    if not kind_m:
        return None

    raw_kind = kind_m.group("kind")
    class_name = kind_m.group("name")
    # Normalize kind label
    kind_map = {"@interface": "annotation", "interface": "interface",
                "class": "class", "enum": "enum", "record": "record"}
    kind = kind_map.get(raw_kind, raw_kind)

    # Modifiers for the class
    before_kind = primary_decl_text[:kind_m.start()]
    class_modifiers = _extract_modifiers(before_kind)

    # extends / implements
    extends_m = re.search(r"\bextends\b\s+([\w.<>, ]+?)(?:\s+(?:implements|permits)\b|\s*[{(])", primary_decl_text)
    implements_m = re.search(r"\bimplements\b\s+([\w.<>, ]+?)(?:\s+permits\b|\s*[{(])", primary_decl_text)
    extends = extends_m.group(1).strip() if extends_m else None
    implements = [i.strip() for i in implements_m.group(1).split(",")] if implements_m else []

    # Find the Javadoc block immediately preceding the primary class declaration
    class_javadoc = ""
    for jd_start, jd_end, jd_text in reversed(javadoc_blocks):
        if jd_end <= primary_start:
            # Check nothing but whitespace/annotations between Javadoc end and class start
            between = source[jd_end:primary_start]
            if re.fullmatch(r"[\s\S]*", between):  # always true; just use the closest one
                class_javadoc = jd_text
                break

    desc_raw, class_tags = _parse_javadoc_block(class_javadoc) if class_javadoc else ("", [])
    description = _convert_description(desc_raw) if desc_raw else ""
    deprecated = any(t.name == "deprecated" for t in class_tags)

    doc = ClassDoc(
        package=package,
        name=class_name,
        kind=kind,
        description=description,
        tags=class_tags,
        modifiers=class_modifiers,
        source_file=path,
        deprecated=deprecated,
        extends=extends,
        implements=implements,
    )

    # ── Parse members ────────────────────────────────────────────────────────
    # We look for Javadoc blocks inside the class body and pair each with the
    # next code declaration.  We operate on the source *after* the class opening.
    body_source = source[primary_body_start:]

    # Re-scan for Javadoc blocks within the body
    member_javadocs: list[tuple[int, int, str]] = [
        (m.start(), m.end(), m.group(0))
        for m in _JAVADOC_RE.finditer(body_source)
    ]

    for jd_start, jd_end, jd_text in member_javadocs:
        # Grab text between end of javadoc and next '{', ';', or end-of-block
        after = body_source[jd_end:jd_end + 600]

        # Strip annotations between Javadoc and declaration
        after_stripped = re.sub(r"^\s*(?:@\w+(?:\([^)]*\))?\s*)+", "", after).strip()

        # Find the signature line: up to first '{' or ';'
        sig_end = min(
            (after_stripped.find("{") if after_stripped.find("{") != -1 else len(after_stripped)),
            (after_stripped.find(";") if after_stripped.find(";") != -1 else len(after_stripped)),
        )
        sig_line = after_stripped[:sig_end].strip()
        if not sig_line:
            continue

        # Skip if this looks like another class declaration
        if re.search(r"\b(?:class|interface|enum|record|@interface)\b", sig_line):
            continue

        member_kind = _guess_member_kind(sig_line)
        name, signature = _parse_member_signature(sig_line)

        if name in ("", "unknown"):
            continue

        # Parse the member's Javadoc
        mdesc_raw, member_tags = _parse_javadoc_block(jd_text)
        mdesc = _convert_description(mdesc_raw) if mdesc_raw else ""
        member_deprecated = any(t.name == "deprecated" for t in member_tags)

        # For enums/annotations treat each element as "constant"/"element"
        if kind == "enum" and member_kind == "field":
            member_kind = "constant"
        elif kind == "annotation" and member_kind == "method":
            member_kind = "element"

        # Determine modifiers from the signature
        mods = _extract_modifiers(sig_line.split("(")[0] if "(" in sig_line else sig_line)

        # Skip purely private members unless they have Javadoc (rare but valid)
        if "private" in mods and not mdesc and not member_tags:
            continue

        doc.members.append(MemberDoc(
            kind=member_kind,
            name=name,
            signature=signature,
            description=mdesc,
            tags=member_tags,
            modifiers=mods,
            deprecated=member_deprecated,
        ))

    return doc


# ──────────────────────────────────────────────────────────────────────────────
# Markdown renderer
# ──────────────────────────────────────────────────────────────────────────────

_KIND_BADGE = {
    "class":      "Class",
    "interface":  "Interface",
    "enum":       "Enum",
    "record":     "Record",
    "annotation": "Annotation",
    "@interface": "Annotation",
}

_KIND_ICON = {
    "class":      ":material-code-braces:",
    "interface":  ":material-approximately-equal:",
    "enum":       ":material-format-list-bulleted-type:",
    "record":     ":material-database:",
    "annotation": ":material-at:",
}


def _render_tags(tags: list[Tag], skip: set[str] | None = None) -> str:
    """Render @param, @return, @throws, @see, @since, @deprecated sections."""
    skip = skip or set()
    out: list[str] = []

    params = [t for t in tags if t.name == "param" and t.name not in skip]
    returns = [t for t in tags if t.name == "return" and t.name not in skip]
    throws = [t for t in tags if t.name in ("throws", "exception") and t.name not in skip]
    sees = [t for t in tags if t.name == "see" and t.name not in skip]
    since = [t for t in tags if t.name == "since" and t.name not in skip]
    deprecated = [t for t in tags if t.name == "deprecated" and t.name not in skip]

    if deprecated:
        desc = _convert_description(deprecated[0].text) if deprecated[0].text else "This element is deprecated."
        out.append(f'!!! warning "Deprecated"\n    {desc}\n')

    if params:
        out.append("**Parameters**\n")
        out.append("| Name | Description |")
        out.append("|------|-------------|")
        for t in params:
            desc = _convert_description(t.text).replace("\n", " ").replace("|", "\\|")
            out.append(f"| `{t.arg}` | {desc} |")
        out.append("")

    if returns:
        out.append("**Returns**\n")
        for t in returns:
            out.append(_convert_description(t.text))
        out.append("")

    if throws:
        out.append("**Throws**\n")
        out.append("| Type | Condition |")
        out.append("|------|-----------|")
        for t in throws:
            desc = _convert_description(t.text).replace("\n", " ").replace("|", "\\|")
            out.append(f"| `{t.arg}` | {desc} |")
        out.append("")

    if sees:
        out.append("**See Also**\n")
        for t in sees:
            ref = (t.arg + " " + t.text).strip()
            out.append(f"- `{ref}`")
        out.append("")

    if since:
        out.append(f"*Since: {since[0].text or since[0].arg}*\n")

    return "\n".join(out)


def _member_section_title(kind: str) -> str:
    return {
        "method":   "Methods",
        "field":    "Fields",
        "constant": "Constants",
        "element":  "Annotation Elements",
    }.get(kind, "Members")


def render_class_page(doc: ClassDoc) -> str:
    """Render a ClassDoc as a full MkDocs Markdown page."""
    lines: list[str] = []

    icon = _KIND_ICON.get(doc.kind, "")
    badge = _KIND_BADGE.get(doc.kind, doc.kind.capitalize())
    deprecation_note = " *(deprecated)*" if doc.deprecated else ""

    # Title
    lines.append(f"# {icon} {doc.name}{deprecation_note}\n")

    # Metadata strip
    lines.append(f"`{doc.package}.{doc.name}` &nbsp;·&nbsp; **{badge}**\n")

    if doc.extends or doc.implements:
        hierarchy = []
        if doc.extends:
            hierarchy.append(f"Extends `{doc.extends}`")
        if doc.implements:
            impl_list = ", ".join(f"`{i}`" for i in doc.implements)
            hierarchy.append(f"Implements {impl_list}")
        lines.append(" &nbsp;·&nbsp; ".join(hierarchy) + "\n")

    lines.append("---\n")

    # Class-level description
    if doc.description:
        lines.append(doc.description)
        lines.append("")

    # Class-level tags (@author, @since, @deprecated, @see …)
    class_tag_md = _render_tags(doc.tags, skip={"param"})
    if class_tag_md.strip():
        lines.append(class_tag_md)

    if not doc.members:
        return "\n".join(lines)

    # Group members by kind
    from collections import defaultdict
    by_kind: dict[str, list[MemberDoc]] = defaultdict(list)
    for m in doc.members:
        by_kind[m.kind].append(m)

    # Render each group
    order = ["constant", "element", "field", "method"]
    for kind in order:
        members = by_kind.get(kind)
        if not members:
            continue

        section_title = _member_section_title(kind)
        lines.append(f"## {section_title}\n")

        for m in members:
            dep_note = " *(deprecated)*" if m.deprecated else ""
            lines.append(f"### `{m.name}`{dep_note}\n")

            # Signature block
            clean_sig = m.signature.strip()
            lines.append(f"```java\n{clean_sig}\n```\n")

            if m.description:
                lines.append(m.description)
                lines.append("")

            tag_md = _render_tags(m.tags)
            if tag_md.strip():
                lines.append(tag_md)

            lines.append("---\n")

        # Remove trailing hr for cleanliness
        while lines and lines[-1].strip() in ("---", "---\n", ""):
            lines.pop()
        lines.append("")

    return "\n".join(lines)


def render_package_index(package: str, classes: list[ClassDoc]) -> str:
    """Render the index page for a package."""
    lines: list[str] = []
    lines.append(f"# Package `{package}`\n")
    lines.append("---\n")

    by_kind: dict[str, list[ClassDoc]] = {}
    for doc in sorted(classes, key=lambda d: d.name):
        by_kind.setdefault(doc.kind, []).append(doc)

    order = ["class", "interface", "record", "enum", "annotation", "@interface"]
    for kind in order:
        docs = by_kind.get(kind)
        if not docs:
            continue
        badge = _KIND_BADGE.get(kind, kind.capitalize())
        icon = _KIND_ICON.get(kind, "")
        lines.append(f"## {icon} {badge}s\n")
        lines.append("| Name | Description |")
        lines.append("|------|-------------|")
        for doc in docs:
            slug = doc.name.lower()
            # First sentence of description
            first_sentence = (doc.description or "").split(".")[0].replace("\n", " ").strip()
            if len(first_sentence) > 100:
                first_sentence = first_sentence[:97] + "…"
            dep_marker = " *(deprecated)*" if doc.deprecated else ""
            lines.append(f"| [`{doc.name}`]({slug}.md){dep_marker} | {first_sentence} |")
        lines.append("")

    return "\n".join(lines)


def render_api_index(all_docs: list[ClassDoc]) -> str:
    """Render the top-level API reference index."""
    lines: list[str] = []
    lines.append("# API Reference\n")
    lines.append(
        "Auto-generated from Javadoc source comments. "
        "Run `make docs-gen` to regenerate.\n"
    )
    lines.append("---\n")

    packages: dict[str, list[ClassDoc]] = {}
    for doc in all_docs:
        packages.setdefault(doc.package, []).append(doc)

    lines.append("## Packages\n")
    lines.append("| Package | Classes |")
    lines.append("|---------|---------|")
    for pkg in sorted(packages):
        pkg_slug = pkg.replace(".", "/")
        count = len(packages[pkg])
        lines.append(f"| [`{pkg}`]({pkg_slug}/index.md) | {count} |")
    lines.append("")

    return "\n".join(lines)


# ──────────────────────────────────────────────────────────────────────────────
# File system walker
# ──────────────────────────────────────────────────────────────────────────────

def collect_java_files(src_root: Path, base_pkg: str = "") -> list[Path]:
    """Find all .java files under src_root, optionally filtered by base_pkg."""
    if base_pkg:
        pkg_path = src_root / base_pkg.replace(".", "/")
        if not pkg_path.exists():
            print(f"[warn] base-pkg path does not exist: {pkg_path}", file=sys.stderr)
            return []
        return sorted(pkg_path.rglob("*.java"))
    return sorted(src_root.rglob("*.java"))


def _write_dir_pages(directory: Path) -> None:
    """
    Recursively write .pages files for the awesome-pages plugin.
    Each .pages file lists only the direct children of its directory:
      - index.md first (if present)
    Then sub-directories and .md files in sorted order.
    This mirrors the actual filesystem structure, so awesome-pages never
    references a path that doesn't exist.
    """
    for dirpath in sorted(directory.rglob("*")):
        if not dirpath.is_dir():
            continue
        children_dirs = sorted(
            [c.name for c in dirpath.iterdir() if c.is_dir() and not c.name.startswith(".")]
        )
        children_md = sorted(
            [c.name for c in dirpath.iterdir() if c.suffix == ".md"]
        )
        # index.md always comes first
        ordered_md: list[str] = []
        if "index.md" in children_md:
            ordered_md.append("index.md")
        ordered_md += [f for f in children_md if f != "index.md"]

        nav_entries: list[str] = []
        for md in ordered_md:
            nav_entries.append(f"  - {md}")
        for d in children_dirs:
            nav_entries.append(f"  - {d}")

        if nav_entries:
            pages_content = "nav:\n" + "\n".join(nav_entries) + "\n"
            (dirpath / ".pages").write_text(pages_content, encoding="utf-8")


def generate_docs(
    src_root: Path,
    out_dir: Path,
    base_pkg: str = "",
    verbose: bool = False,
    clean: bool = False,
) -> list[ClassDoc]:
    """
    Parse all Java files and write Markdown pages under out_dir.
    Returns the list of all parsed ClassDocs.
    """
    if clean and out_dir.exists():
        shutil.rmtree(out_dir)
        if verbose:
            print(f"[clean] Deleted {out_dir}")

    out_dir.mkdir(parents=True, exist_ok=True)

    java_files = collect_java_files(src_root, base_pkg)
    if not java_files:
        print("[warn] No Java files found.", file=sys.stderr)
        return []

    all_docs: list[ClassDoc] = []
    skipped = 0

    for jf in java_files:
        if verbose:
            print(f"  parsing {jf.relative_to(src_root)}")
        doc = parse_java_file(jf)
        if doc is None:
            skipped += 1
            continue
        # Only include files with at least a class-level Javadoc OR members with docs
        has_content = bool(doc.description or doc.tags or doc.members)
        if not has_content:
            skipped += 1
            continue
        all_docs.append(doc)

    print(f"[info] Parsed {len(all_docs)} classes ({skipped} skipped — no Javadoc)")

    # Build package → docs mapping
    packages: dict[str, list[ClassDoc]] = {}
    for doc in all_docs:
        packages.setdefault(doc.package, []).append(doc)

    # Write per-class pages
    for doc in all_docs:
        pkg_dir = out_dir / doc.package.replace(".", "/")
        pkg_dir.mkdir(parents=True, exist_ok=True)
        page = render_class_page(doc)
        out_path = pkg_dir / f"{doc.name.lower()}.md"
        out_path.write_text(page, encoding="utf-8")

    # Write package index pages + awesome-pages .pages files
    for pkg, docs in packages.items():
        pkg_dir = out_dir / pkg.replace(".", "/")
        pkg_dir.mkdir(parents=True, exist_ok=True)
        index_page = render_package_index(pkg, docs)
        (pkg_dir / "index.md").write_text(index_page, encoding="utf-8")

    # Write top-level API index
    api_index = render_api_index(all_docs)
    (out_dir / "index.md").write_text(api_index, encoding="utf-8")

    # Write .pages files for every directory under out_dir.
    # Each .pages file only references *direct* children of that directory,
    # which is what mkdocs-awesome-pages expects.
    _write_dir_pages(out_dir)

    print(f"[info] Written to {out_dir}/")
    return all_docs


# ──────────────────────────────────────────────────────────────────────────────
# Nav generator
# ──────────────────────────────────────────────────────────────────────────────

def generate_nav_yaml(all_docs: list[ClassDoc], out_dir_name: str = "api") -> str:
    """
    Print a MkDocs nav YAML block that can be pasted into mkdocs.yml.
    """
    packages: dict[str, list[ClassDoc]] = {}
    for doc in all_docs:
        packages.setdefault(doc.package, []).append(doc)

    lines: list[str] = []
    lines.append("  - API Reference:")
    lines.append(f"    - Overview: {out_dir_name}/index.md")

    for pkg in sorted(packages):
        pkg_slug = pkg.replace(".", "/")
        short = pkg.split(".")[-1]
        lines.append(f"    - {short}:")
        lines.append(f"      - Overview: {out_dir_name}/{pkg_slug}/index.md")
        for doc in sorted(packages[pkg], key=lambda d: d.name):
            lines.append(
                f"      - {doc.name}: {out_dir_name}/{pkg_slug}/{doc.name.lower()}.md"
            )

    return "\n".join(lines)


# ──────────────────────────────────────────────────────────────────────────────
# CLI
# ──────────────────────────────────────────────────────────────────────────────

def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Generate MkDocs API reference from Java Javadoc comments.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    p.add_argument("--src", default="src/main/java",
                   help="Java source root (default: src/main/java)")
    p.add_argument("--out", default="docs/api",
                   help="Output directory for generated Markdown (default: docs/api)")
    p.add_argument("--base-pkg", default="",
                   help="Root package to document (default: all packages)")
    p.add_argument("--nav", action="store_true",
                   help="Print the MkDocs nav YAML block and exit")
    p.add_argument("--clean", action="store_true",
                   help="Delete --out before generating")
    p.add_argument("--verbose", "-v", action="store_true",
                   help="Show per-file progress")
    return p


def main() -> None:
    args = _build_arg_parser().parse_args()

    # Resolve paths relative to the directory the script is invoked from
    cwd = Path.cwd()
    src_root = (cwd / args.src).resolve()
    out_dir = (cwd / args.out).resolve()

    if not src_root.exists():
        print(f"[error] Source root not found: {src_root}", file=sys.stderr)
        sys.exit(1)

    # Determine the relative name of out_dir from docs/
    docs_root = (cwd / "docs").resolve()
    try:
        out_dir_name = str(out_dir.relative_to(docs_root))
    except ValueError:
        out_dir_name = args.out

    print(f"[info] Source: {src_root}")
    print(f"[info] Output: {out_dir}")
    if args.base_pkg:
        print(f"[info] Package filter: {args.base_pkg}")

    all_docs = generate_docs(
        src_root=src_root,
        out_dir=out_dir,
        base_pkg=args.base_pkg,
        verbose=args.verbose,
        clean=args.clean,
    )

    if args.nav and all_docs:
        print("\n# ── Paste this into your mkdocs.yml nav: ──────────────────────────")
        print(generate_nav_yaml(all_docs, out_dir_name))

    print("[done]")


if __name__ == "__main__":
    main()
