---
name: vaadin-usage-review
description: Use when asked to review/audit whether code uses official Vaadin API or custom workarounds — invoked as /vaadin-usage-review. Flags executeJs hacks, inline getStyle CSS, raw getElement DOM manipulation, manual paging instead of DataProvider, hand-rolled dialogs, custom auth, deprecated API, workarounds in project-owned JS/TS connectors (web-component monkey-patching, private-member access), and raw HTML elements or custom-styled markup used where a Vaadin component fits (Div/Span/native input/button instead of TextField/Button/layouts; hand-styled badges/cards instead of Badge/Card). Reports only, never edits. Vaadin MCP is the single source of truth.
---

# Vaadin Usage Review

## Overview

Audits Vaadin code for places where a **custom workaround or hack** was used even though an **official Vaadin API exists** — typically because whoever wrote it (often an AI agent) didn't look it up.

This includes **component choice**: raw HTML elements (`Div`, `Span`, native `<input>`/`<button>`, flexbox `Div`s) or custom-styled markup used where an equivalent **Vaadin component** already exists (`TextField`, `Button`, `VerticalLayout`/`HorizontalLayout`, `Badge`, `Card`, …). Vaadin components are the default; raw HTML is the exception that must earn its place — see [Component choice](#component-choice-prefer-vaadin-components-over-html).

**This skill is report-only. It NEVER edits, fixes, or stages files.** It produces a findings report.

**The Vaadin MCP is the Single Source of Truth (SSOT).** Every claim that "an official API exists" MUST be verified against the Vaadin MCP and cited. Do not assert official APIs from memory.

## The Two Iron Rules

1. **MCP is SSOT.** If the Vaadin MCP is unavailable, STOP and report that — do not fall back to memory, training knowledge, or the web. A guess is worse than an honest "cannot verify".
2. **Report only.** No `Edit`, no `Write`, no fixes applied, no commits. Findings describe the official alternative; they do not change code.

## Component choice: prefer Vaadin components over HTML

Vaadin components are the default. A raw HTML element (`Div`, `Span`, `NativeLabel`, native `<input>`/`<button>`, `Anchor`-as-button, `new Element(...)`, `Html`, or raw `<div>/<span>/<input>/<button>` in `.tsx`) or custom-styled markup is a candidate finding when it fills a role an existing Vaadin component already covers — **and** when custom styling is written for something a Vaadin component already provides (a hand-styled badge, card, panel, pill, spinner …), the Vaadin component wins.

But this axis is a **false-positive magnet** — most `Div`/`Span` usage is legitimate structure. The user must **not** be flooded with speculative "a Div is theoretically better/worse" notes. Apply this gate to every candidate, in order, and only report what survives:

1. **Role test.** Does the element (a) play the role of a specific Vaadin component — input, button, layout, badge, card, dialog, tab, menu, progress … — **or** (b) carry custom styling / `theme` / CSS classes that reproduce one?
   - **No** (a bare structural wrapper, a plain text holder, a slot target with no component-equivalent role) → **not a finding.** Do not report bare `Div`/`Span`.
   - **Yes** → continue.
2. **MCP confirmation.** Verify against the MCP that the Vaadin component exists **for this version and edition** and actually covers the role. Note if it is a **commercial/Pro** component (the user should know the equivalent isn't free). Not confirmed → drop or mark `unverified`.
3. **Concrete-advantage check (the anti-noise gate).** Before recommending the switch, ask: does the HTML element give a **concrete, specific, MCP-verifiable** advantage the Vaadin component cannot — a documented missing feature, a real constraint, a measured cost?
   - **No** → finding: recommend the Vaadin component (normal severity group).
   - **Yes** → do **not** file a plain "switch" finding. Instead raise it as a *component-choice tradeoff for the user's decision*, naming the specific advantage and its MCP basis.

**"Theoretically lighter", "more flexible in principle", "a Div could do this too", "might be simpler" are NOT concrete advantages.** If that is all you have, recommend the Vaadin component (or stay silent) — never emit a speculative "HTML is arguably better here" note. Better to miss one debatable case than to bury the user in theoretical bikeshedding.

## Workflow

### Step 1 — Preflight: confirm the MCP (SSOT) is reachable

Call `get_latest_vaadin_version`.

- **Succeeds** → MCP is available, continue.
- **Fails / tool not present / errors** → STOP immediately. Output exactly:

  > ⚠️ Vaadin MCP not available. The Vaadin MCP is the single source of truth for this review, so I cannot verify findings against official API. No review performed. (Enable the Vaadin MCP server and re-run `/vaadin-usage-review`.)

  Do not proceed. Do not guess from training knowledge.

### Step 2 — Establish project context (do not guess)

Determine, from the repo:
- **Vaadin version** — read `pom.xml` (`vaadin.version`) or `build.gradle`. Map to the MCP enum (`"24"`, `"25.0"`, `"25.1"`, `"25.2"`). If none found, call `get_latest_vaadin_version` and state the assumption in the report.
- **Dev model** — Flow (Java views), Hilla/React (`@BrowserCallable`, `.tsx` views under `views/`), or both. This sets `ui_language` for MCP queries (`java` / `react` / `common`).
- **Theme** — Lumo or Aura (check `@Theme` annotation and `frontend/themes/`). Affects which variant/utility-class advice is correct.

State all three at the top of the report. Never recommend a `LUMO_*` variant without confirming the theme.

### Step 3 — Ask which scope to review (always)

**Always ask the user what to review. Never assume a scope and never start dispatching on a guess.**

First run `git status` and `git diff --name-only` to ground concrete suggestions, then ask the user (use `AskUserQuestion`) and **wait for the answer before dispatching anything** — never self-dispatch after asking a question.

Propose options derived from the current git state, and recommend the one that best fits it (e.g. if there is an active change set, suggest "changed files" first):
- Changed files vs the base branch (`git diff --name-only main...HEAD` + unstaged) — "did the work just done introduce hacks?".
- Staged files only.
- A specific file / package / module the user names.
- The whole project — warn that this can be large and slow.

The user's answer wins; the suggestions are only a convenience. Do not proceed until they have chosen.

Once the scope is chosen, filter to Vaadin-relevant files:
- Java in view/component packages.
- `.tsx`/`.ts` Hilla views and endpoints.
- Theme `.css`.
- **Project-owned JavaScript/TypeScript** — frontend scripts, client-side connectors, `@JsModule`/`@JavaScript`-referenced files that live in *this repo*. Typical locations: `frontend/` (especially `*-connector.js`, `*.connector.ts`, `frontend/generated/jar-resources/` only if authored here), and `src/main/resources/META-INF/resources/frontend/` (or `META-INF/frontend/`).

**Never review dependency or generated JS.** Exclude `node_modules/`, `target/`, `build/`, `frontend/generated/flow/` and other Flow-generated bundles, and any minified/vendored `*.min.js`. Only the project's own authored JS/TS is in scope. If a `@JsModule` points at an npm package (not a repo path), it is out of scope.

If the resolved scope has no Vaadin-relevant files, report that and stop.

### Step 4 — Dispatch parallel review agents

**REQUIRED:** Split the in-scope files into batches (group by feature/package, ~3–6 files each) and dispatch one reviewer per batch concurrently.

Give each reviewer this contract (adapt the file list):

> You are reviewing Vaadin files for custom workarounds that have an official API. **Report only — do not edit any file.** The Vaadin MCP is the single source of truth.
>
> Project: Vaadin {version}, {dev model}, {theme} theme.
> Files: {batch}
>
> For each suspicious pattern (see the smell catalog), you MUST verify against the MCP before reporting it — use `search_vaadin_docs`, `get_component_java_api`, `get_component_react_api`, `get_component_web_component_api`, `get_component_styling`, or `get_full_document` with the matching `vaadin_version` and `ui_language`. For project-owned `.js`/`.ts` connectors, check against the web-component / element API; never review `node_modules` or generated bundles. Only report a finding if the MCP confirms an official API exists. If the MCP does not confirm an alternative, either drop the finding or mark it `unverified` — never invent an API from memory.
>
> Also review **component choice**: raw HTML elements or custom-styled markup used where a Vaadin component fits. Apply the three-step component-choice gate: (1) do NOT flag bare structural `Div`/`Span` — only markup that plays a specific Vaadin component's role or is styled to imitate one; (2) confirm via the MCP that the component exists for this version/edition (note if it is Pro); (3) recommend the Vaadin component unless the HTML gives a **concrete, MCP-verifiable** advantage, in which case report it as a tradeoff for the user's decision. Never emit a speculative "HTML is theoretically better" note.
>
> **Do NOT report items that are verified correct** — patterns checked and found to use the official API are silently discarded. Only return actual violations.
>
> Return findings as: file:line · severity · the workaround · the official API (with the exact MCP source: tool + doc file_path/component) · 1-line why it matters. Do NOT propose a diff or apply changes.

### Step 5 — Aggregate and report

Merge reviewer findings into one report. Group by severity. Every official-API claim must carry its MCP source. End with the report-only reminder.

**Omit verified-correct items entirely.** Items that reviewers checked and found to be using the official API are not included in the report — only violations appear. If the user explicitly asks "what did you verify as correct?" you may list them, but never include them by default.

## Smell Catalog (candidates to verify — not auto-findings)

A smell is only a finding **after the MCP confirms an official alternative**.

### Server-side (Java / Flow & Hilla endpoints)

| Smell | Why it's suspect | Verify in MCP |
|-------|------------------|---------------|
| `getElement().executeJs(...)` / `callJsFunction(...)` | Imperative JS where a typed Java/React API often exists | component java/react API |
| `UI.getCurrent().getPage().executeJs(...)` | Global JS escape hatch instead of an API | `search_vaadin_docs` |
| `@JavaScript` / `@StyleSheet` on a component | Loading raw assets where a typed API / theme exists | component API, theming docs |
| `getElement().setProperty/setAttribute/getProperty` | Raw property poking instead of typed setters | component API |
| `getStyle().set("...")` for color/spacing/radius | Inline CSS instead of theme variants / utility classes | `get_component_styling`, theming docs |
| `new Element("...")` / `Html(...)` building UI | Hand-built DOM instead of components | `search_vaadin_docs` |
| Reflection on Vaadin internals (`getDeclaredField`, accessing `_`/`internal` API) | Reaching past the public API | component API |
| Manual `subList` / page math before `setItems` | Reinventing lazy loading | DataProvider / `setItemsPageable` docs |
| Hand-rolled filtering/sorting before `setItems` | Instead of `DataProvider` filter/sort | DataProvider docs |
| Custom `Thread`/polling to push updates | Instead of `@Push` / Signals | push / signals docs |
| Manual `UI.access` loops to refresh UI | Instead of Signals / data binding | signals docs |
| `fetch`/REST in a React view | Instead of `@BrowserCallable` endpoint | Hilla endpoint docs (`react`) |
| Hand-rolled dialog/notification/upload/menu via DOM | Instead of `Dialog`/`Notification`/`Upload`/`ContextMenu` | component API |
| Manual `Binder`-less field wiring & validation | Instead of `Binder` / `BeanValidationBinder` | forms/validation docs |
| Custom routing via `getUI().navigate(string)` string-building | Instead of typed `@Route` navigation / route params | views/navigation docs |
| Custom role/auth checks in views | Instead of `@RolesAllowed` / `VaadinSecurityConfigurer` | security docs |
| `@Deprecated` Vaadin API in use | Newer API exists | component API for target version |

### Component choice — HTML where a Vaadin component fits

**Apply the [component-choice gate](#component-choice-prefer-vaadin-components-over-html) to every row: skip bare structural `Div`/`Span`, confirm the component in the MCP for this version/edition, and only recommend the switch when the HTML has no concrete verified advantage.** Applies to Flow (`Div`, `Span`, `NativeButton`, `Input`, `Anchor`, `new Element`, `Html`) and to raw JSX in `.tsx` views alike.

| Smell | Prefer | Verify in MCP |
|-------|--------|---------------|
| Native `<input>` / `Input` / `Div` styled as a field | `TextField` / `NumberField` / `TextArea` / `DatePicker` / … | component API |
| `NativeButton` / `<button>` / `Anchor` styled as a button | `Button` (+ variants) | component API |
| `Div`/`Span` with `display:flex` styling used to lay out children | `HorizontalLayout` / `VerticalLayout` / `FlexLayout` | layout docs |
| `Div`/`Span` custom-styled as a badge / pill / status chip | `Badge` | component styling / theming docs |
| `Div` custom-styled as a card / panel | `Card` | component API |
| Hand-built `Div` list/table with manual rows | `Grid` / `VirtualList` | component API |
| Raw `<dialog>` / overlay `Div` | `Dialog` | component API |
| `Div`/`Span` custom-styled as a progress bar / spinner | `ProgressBar` | component API |

### Project-owned JavaScript / TypeScript (connectors, frontend scripts)

Same principle as `executeJs`/`callJsFunction`: client-side code that reaches into Vaadin internals instead of using the supported element/connector contract. **Only authored project files — never `node_modules` or generated bundles.**

| Smell | Why it's suspect | Verify in MCP |
|-------|------------------|---------------|
| `customElements.get('vaadin-...')` then patching `.prototype` / overriding methods | Monkey-patching a Vaadin web component | component web-component API |
| Reading/writing private members (`el._something`, `el.__data`, `$`/`shadowRoot` internals) of a `vaadin-*` element | Depending on undocumented internals | `get_component_web_component_api` |
| `setTimeout`/`setInterval`/`MutationObserver` polling to wait for a component to be "ready" | Instead of lifecycle / `whenDefined` / Element API readiness | `search_vaadin_docs` |
| Manually dispatching synthetic events (`dispatchEvent(new CustomEvent(...))`) to drive a component | Instead of the component's public methods/properties | component web-component API |
| `querySelector('vaadin-...')` to grab and mutate a server-managed component | Bypasses Flow's element ↔ component sync | `search_vaadin_docs` |
| Connector reimplementing server↔client messaging | Instead of the Element API / `@ClientCallable` / `callJsFunction` contract | `search_vaadin_docs` |
| Injecting global CSS/`<style>` from JS | Instead of theme files / `@CssImport` / style properties | theming docs |
| Hand-rolled fetch to a server endpoint from a connector | Instead of `@BrowserCallable` / element RPC | Hilla / element-API docs |

The catalog is a starting set — flag anything that looks like fighting the framework, then verify it.

## Red Flags — you are about to break the skill

- About to call `Edit`/`Write`/`git commit` → STOP. This skill reports only.
- About to write "Vaadin has API X" without an MCP tool call backing it → STOP. That's memory, not the SSOT.
- MCP preflight failed and you're "just checking from what I know" → STOP. Report MCP-unavailable and end.
- Recommending a `LUMO_*` variant before confirming the project's theme → STOP. Check the theme first.
- Couldn't find a project Vaadin version and silently used your own → STOP. State the assumption.
- About to dispatch reviewers without having asked the user for the scope → STOP. Always ask first (Step 3) and wait for the answer.
- About to flag a bare `Div`/`Span` that isn't playing or imitating any Vaadin component's role → STOP. Structural wrappers and plain text holders are fine.
- About to write "a Div is theoretically lighter / more flexible / could also work" → STOP. That's not a concrete advantage. Recommend the Vaadin component or say nothing.
- About to raise "HTML is arguably better here" without a concrete, MCP-verified advantage → STOP. The user must not be flooded with speculative tradeoff notes.

## Report Format

Only violations are shown. Patterns verified as correct are silently omitted (the user can ask for them explicitly if needed).

```
# Vaadin Usage Review

Context: Vaadin {version} · {Flow/Hilla} · {Lumo/Aura} theme
Scope: {N files reviewed} ({changed files | named scope})
MCP: available (SSOT)

## High — functional bug or broken behavior
- {file:line} — {the custom workaround / hack}.
  Official: {the Vaadin API}. Source: {MCP tool + component/doc @ version}.
  Why: {1-line impact}.

## Medium — non-idiomatic, fights theme/framework
- ...

## Low — minor / style
- ...

## Component choice — for your decision (HTML kept for a concrete, verified reason)
- {file:line} — {HTML element} used where a Vaadin component fits.
  Concrete advantage of the HTML here: {specific, MCP-verified reason}. Raised as a tradeoff, not a required change.

## Unverified (MCP had no confirmed alternative — listed for human judgment)
- ...

_Report only. No files were changed._
```

If no violations were found, report: `No violations found. All reviewed patterns use official Vaadin API.`

## Common Mistakes

- **Reporting from memory.** The baseline failure mode: confidently citing "official docs" without a single MCP call. Every API claim needs a tool-backed source.
- **Drifting into fixes.** Writing "Fix:" with code to apply, or actually editing. State the alternative as information, not an action.
- **Theme/version blindness.** Recommending Lumo variants in an Aura project, or current API for an older Vaadin version.
- **Silent MCP fallback.** When the MCP is down, guessing instead of reporting unavailability.
- **Div/Span noise.** Flagging every raw element. Only markup that plays or imitates a Vaadin component's role is in scope; bare structure and plain text holders are not.
- **Theoretical bikeshedding.** Raising "HTML might be lighter/more flexible" without a concrete, MCP-verified advantage. Default to the Vaadin component; the reverse note is the rare exception, not the norm.
