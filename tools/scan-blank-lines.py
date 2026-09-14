#!/usr/bin/env python3
"""Deterministic pre-scan for the mechanical subset of the STYLEGUIDE.md readability rules.

This is a CANDIDATE GENERATOR, not a gate. It enumerates every site that
matches a machine-checkable readability pattern so the /code-readability reviewers no
longer have to *discover* them — they only adjudicate (real / false positive).
Recall is the job of this script; precision is the job of the reviewer.

Covered patterns (the mechanical subset only):
  2.1-i   guard / early-return block NOT followed by a blank line (guard->work)
  2.1-ii  terminating `return` NOT preceded by a blank line (work->result)
  3       multiline comment NOT preceded by a blank line (and not first-in-block)

NOT covered here (already enforced by Checkstyle, or pure judgment):
  Rule 1 (NeedBraces) and Rule 4 (FQN) -> Checkstyle.
  Rule 2.2 (multi-line init / loop-with-initializers / stream chains) -> judgment.

To tell a method-terminating `return` from a guard `return` inside an `if`, the
script tracks brace depth with a small stack: each `{` is classified from its
opening line (type / method / block), so the `}` that follows a return reveals
exactly what it closes. No peephole guessing.

Usage:
  python3 scan-rule2.py <file-or-dir> [<file-or-dir> ...]

Exit code is always 0 (it reports candidates; it does not fail a build).
Output lines: <path>:<line> · <sub-rule> · <snippet>
"""

import os
import re
import sys

GUARD_BODY = re.compile(r"^(return\b.*;|throw\b.*;|continue;|break;)$")
RETURN_STMT = re.compile(r"^return\b.*;$|^return;$")
TYPE_DECL = re.compile(r"\b(class|interface|enum|record|@interface)\b")
CONTROL_OPENER = re.compile(
    r"^(\}\s*)?(else\b|try\b|finally\b|do\b"
    r"|if\s*\(|for\s*\(|while\s*\(|switch\s*\(|catch\s*\(|synchronized\s*\()"
)
SKIP_DIRS = ("/generated/", "/target/", "/build/")


def java_files(paths):
    """Expand args into a sorted list of .java files, skipping generated sources."""
    out = []
    for p in paths:
        if os.path.isdir(p):
            for root, _, names in os.walk(p):
                for name in names:
                    if name.endswith(".java"):
                        out.append(os.path.join(root, name))
        elif p.endswith(".java"):
            out.append(p)

    out = [f for f in out if not any(s in f for s in SKIP_DIRS)]
    return sorted(set(out))


def prev_nonblank(lines, i):
    """Index of the nearest non-blank line strictly above i, or -1."""
    j = i - 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    return j


def next_nonblank(lines, i):
    """Index of the nearest non-blank line strictly below i, or len(lines)."""
    j = i + 1
    while j < len(lines) and lines[j].strip() == "":
        j += 1
    return j


def sanitize(line, in_block):
    """Strip comments and string/char literals so brace counting sees code only.

    Returns (clean_line, in_block_after) where in_block tracks an open `/* */`.
    """
    out = []
    i, n = 0, len(line)
    in_str = in_char = False
    while i < n:
        two = line[i : i + 2]
        if in_block:
            if two == "*/":
                in_block = False
                i += 2
                continue
            i += 1
            continue

        if in_str:
            if line[i] == "\\":
                i += 2
                continue
            if line[i] == '"':
                in_str = False
            i += 1
            continue

        if in_char:
            if line[i] == "\\":
                i += 2
                continue
            if line[i] == "'":
                in_char = False
            i += 1
            continue

        if two == "//":
            break
        if two == "/*":
            in_block = True
            i += 2
            continue
        if line[i] == '"':
            in_str = True
            i += 1
            continue
        if line[i] == "'":
            in_char = True
            i += 1
            continue

        out.append(line[i])
        i += 1

    return "".join(out), in_block


def brace_kinds(lines):
    """For each line, record the kind ('type'|'method'|'block') of the frame that
    the FIRST `}` on that line pops. google-java-format puts a closing brace on
    its own line, so that first pop is the one sitting after a return.
    """
    popped = [None] * len(lines)
    stack = []
    in_block = False
    for idx, raw in enumerate(lines):
        clean, in_block = sanitize(raw, in_block)
        stripped = raw.strip()

        # Classify a frame opened on this line from the line's own text.
        if TYPE_DECL.search(clean):
            kind = "type"
        elif CONTROL_OPENER.match(stripped):
            kind = "block"
        else:
            kind = "method"

        first_pop_recorded = False
        for ch in clean:
            if ch == "{":
                stack.append(kind)
            elif ch == "}":
                top = stack.pop() if stack else "block"
                if not first_pop_recorded:
                    popped[idx] = top
                    first_pop_recorded = True

    return popped


def scan_guard_to_work(lines, findings):
    """2.1-i: a guard block's closing `}` glued to the next statement."""
    n = len(lines)
    for i in range(n):
        if lines[i].strip() != "}":
            continue

        pj = prev_nonblank(lines, i)
        prev = lines[pj].strip() if pj >= 0 else ""
        if not GUARD_BODY.match(prev):
            continue

        # The very next physical line decides: a blank line satisfies the rule.
        if i + 1 < n:
            nxt = lines[i + 1].strip()
            if nxt and nxt[0] != "}" and not nxt.startswith(("else", "catch", "finally")):
                findings.append((i + 2, "2.1-i guard->work no blank", nxt[:80]))


def scan_return_to_result(lines, popped, findings):
    """2.1-ii: a terminating `return` glued to the work that built the value."""
    n = len(lines)
    for i in range(n):
        s = lines[i].strip()
        if not RETURN_STMT.match(s):
            continue

        # Terminating: the next non-blank line is the `}` that closes the METHOD
        # (or type) — not a nested guard `if`, which pops a 'block' frame.
        nb = next_nonblank(lines, i)
        if nb >= n or lines[nb].strip()[:1] != "}" or popped[nb] == "block":
            continue

        # A blank line directly above satisfies the rule.
        if i > 0 and lines[i - 1].strip() == "":
            continue

        pj = prev_nonblank(lines, i)
        if pj < 0:
            continue

        prev = lines[pj].strip()

        # Exempt: sole statement (right after the opening `{`) or a comment above.
        # Guard-internal returns are already excluded via the brace-kind check.
        if prev.endswith("{") or prev.startswith(("//", "*", "/*")):
            continue

        findings.append((i + 1, "2.1-ii return no leading blank", s[:80]))


def scan_comment_leading_blank(lines, findings):
    """Rule 3: a multiline comment glued under a code statement."""
    n = len(lines)
    for i in range(n):
        s = lines[i].strip()

        starts_line_run = s.startswith("//") and i + 1 < n and lines[i + 1].strip().startswith("//")
        starts_block = s.startswith("/*") and "*/" not in s
        if not (starts_line_run or starts_block):
            continue

        if i == 0:
            continue

        prev = lines[i - 1].strip()

        # Exempt: first line in a block, after a blank line, or a comment run.
        if prev == "" or prev.endswith("{") or prev.startswith(("//", "*", "/*")):
            continue

        findings.append((i + 1, "3 multiline comment no leading blank", s[:80]))


def scan_file(path):
    with open(path, encoding="utf-8") as fh:
        lines = fh.read().split("\n")

    popped = brace_kinds(lines)

    findings = []
    scan_guard_to_work(lines, findings)
    scan_return_to_result(lines, popped, findings)
    scan_comment_leading_blank(lines, findings)

    findings.sort(key=lambda f: f[0])
    return findings


def main(argv):
    files = java_files(argv[1:])
    if not files:
        print("no .java files in scope", file=sys.stderr)
        return 0

    total = 0
    for path in files:
        findings = scan_file(path)
        if not findings:
            continue

        print(f"=== {path} ({len(findings)}) ===")
        for line, rule, snippet in findings:
            print(f"{path}:{line} · {rule} · {snippet}")
            total += 1

    print(f"--- {total} candidate(s) across {len(files)} file(s) ---")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
