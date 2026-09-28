# Style Guide

The documented coding standards for this project. Extend it with project-specific
conventions as they emerge. `/code-review` reads this file as the repo's standards
source for its Standards axis, so keep every rule concrete and quotable.

> **Relationship to other files**
> - This file is the **single source of truth** for code standards. Keep the
>   normative rules in this one place.
> - Machine-enforced standards live in their own config: Spotless and Checkstyle in
>   the poms, `checkstyle/checkstyle.xml`, `eclipse/`, `.prettierrc`. Don't
>   re-document here what tooling already enforces — point at the config instead.
> - Decisions about *how the system is built*, rather than how code is written, are
>   ADRs under `docs/adr/`. Cite them here where they constrain the code.

---

## 1. Audience

Code is written for **human Java developers**, not for the machine and not for the
AI that wrote it. It must read top-to-bottom like prose: easy to scan, easy to
change. Optimise for the next reader.

## 2. Formatting & whitespace

These are mechanical and non-negotiable (machine-checkable):

- **Always use `{}` blocks** for one-line `if`/`else`/`for`/`while` — never the
  braceless form.
- A **guard clause / early-return `if`** is followed by exactly one blank line
  before the main logic — *unless* the next line closes the method (`}`).
- The **terminating `return`** that produces the method's result is preceded by
  exactly one blank line, separating it from the code that built the value —
  *unless* it is the method's only statement, or it sits immediately after the
  opening `{`.
- **Multiline comments** are always preceded by one blank line.

The three blank-line rules are mechanical but very easy to miss by reading: working
through a 752-line file by eye typically turns up 3 of 13 real misses. Run
`tools/scan-blank-lines.py <paths>` instead of eyeballing. It favours recall over
precision, so it reports false positives and its output is adjudicated, not applied.
Checkstyle already enforces the brace rule and the fully-qualified-name rule; the
script covers what it does not.

## 3. Method shape (judgment)

Read a method as a sequence of phases — **guard → setup → work → result** — and
separate those phases with a single blank line. Do not clutch everything into one
dense block.

Put a blank line *between* phases such as:

- an object initialised and configured over multiple lines,
- a larger block (e.g. a loop) preceded by its initialisers,
- a long call chain (e.g. a stream) spanning multiple lines.

**Do not over-separate.** A blank line goes *between* phases, not between every
statement. Tightly related one-liners stay together.

## 4. Naming & imports

- **No fully-qualified names** in code unless genuinely unavoidable (name clash).
  Use `import` instead.
- Names state intent. A reader should understand a variable/method from its name
  without reading the body.

## 5. Comments

- Less is more. Keep comments **short and focused on the WHY**.
- The WHAT must be obvious from the code itself — if it isn't, rewrite the code
  rather than explaining it in a comment.
- No commented-out code without a stated reason.
- **The code is not your logbook.** A comment records why the code is the way it is,
  never what it used to be, what was tried first, or what changed in this round. That
  belongs in the commit message.

## 6. Structure & visibility

- **Nested types** (inner classes) belong at the **bottom** of the owning class.
- **Never widen production-API visibility** (package-private → public) just to
  make something testable. Test through the real public interface or restructure.
- Prefer **deep modules**: a small, stable interface over a substantial
  implementation. A class whose interface is nearly as complex as its body is a
  smell — see `/improve-codebase-architecture`.
- **Don't invent what already exists.** Before writing a helper or working around a
  limitation, look for the solution already in this codebase or in a dependency it
  already has. A new dependency is the last resort, not the first.

---

## Project-specific standards *(extend here)*

The sections below are placeholders. Fill them in for the concrete project; delete
the ones that don't apply. Anything written here becomes a rule a reviewer can cite.

### Architecture & package layout

> _Describe the architectural style (package-by-feature, layered, hexagonal, …),
> the package/module layout, and the boundary rules (what may import what).
> Architectural decisions worth preserving belong in `docs/adr/` — reference them
> here rather than restating them._

### Vaadin / UI conventions

Prefer official Vaadin API over custom workarounds or DOM manipulation — verify against the Vaadin MCP before rolling your own.

> _e.g. component composition patterns, where views live, styling approach (reuse
> existing CSS over inline styles, explicit CSS classes over positional selectors),
> responsive-layout expectations. Use the Vaadin MCP as the authority for the API._

### Testing standards

Which layer a test belongs in, and what may not be asserted against the demo, is in
`CLAUDE.md`. What follows are the craft rules for writing them.

**Three traps when driving Vaadin + Froala from Playwright.** Each was learned the
expensive way: every one of them let a test pass with the bug deliberately put back.

- **`page.clock().runFor()`, never `fastForward()`.** `fastForward` fires each due
  timer at most once and never the ones scheduled while it jumps. Froala's own typing
  debounce scheduling our sync is exactly such a chain, so the jump silently breaks it.
- **`locator.click()` returns when the click is dispatched, not when the server has
  answered.** Reading client state right after it is a race. Give the test view's
  control a visible effect to wait for — the buttons that change something invisible
  disable themselves, and the test asserts `isDisabled()` first.
- **"Nothing has been sent yet" cannot be asserted on the viewer.** A value reaches it
  through a round trip in real time, so an empty viewer only means *not yet*. Count the
  client's own `_value-delta` dispatches instead; they happen synchronously in the timer
  callback.

**A test that passes with the bug put back is not a test.** Before claiming a test
guards something, reinstate the defect and watch it fail. If it stays green either way,
fix it or delete it, and say which.

### Writing

Applies to everything a person reads: the README, Javadoc, comments, commit messages and
issues.

- **No semicolons, colons or dashes joining two sentences**, and no comma splices either.
  Write two sentences, or join them with a word such as "because", "so" or "but". A colon
  is fine before an example, a list or a table. Chained clauses are hard to read and mark
  a text as machine-written.
- **The README is written for developers who know Vaadin and Froala.** It documents what
  the add-on adds or changes, not how Vaadin or Froala work. Where it states a rule or a
  failure case, a short example follows.

### Error handling & logging

> _e.g. when to throw vs. return, exception types, logging levels and format._
