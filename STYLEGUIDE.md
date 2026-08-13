# Style Guide

The documented coding standards for this project. This is the **starting point** —
extend it with project-specific conventions as they emerge. Tools and reviewers
(e.g. the Matt Pocock `review` skill, `/code-readability`) treat this file as a
citable *standards source*, so keep every rule concrete and quotable.

> **Relationship to other files**
> - This file is the **single source of truth** for code standards. `/code-readability`
>   and the `review` skill scan it — keep the normative rules in this one place.
> - Machine-enforced standards (formatting via Spotless, Checkstyle, `.editorconfig`,
>   Prettier) are configured by `setup-code-checks`. Don't re-document here what
>   tooling already enforces — point to the config instead.

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

## 6. Structure & visibility

- **Nested types** (inner classes) belong at the **bottom** of the owning class.
- **Never widen production-API visibility** (package-private → public) just to
  make something testable. Test through the real public interface or restructure.
- Prefer **deep modules**: a small, stable interface over a substantial
  implementation. A class whose interface is nearly as complex as its body is a
  smell — see `improve-codebase-architecture`.

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

> _e.g. the test-type decision rule (unit vs browserless vs e2e), the rule that
> tests drive real user behaviour (keyboard/mouse APIs) instead of programmatic
> shortcuts, naming conventions for tests, and the build/verify gate
> (`mvn clean verify -Pproduction`)._

### Error handling & logging

> _e.g. when to throw vs. return, exception types, logging levels and format._
