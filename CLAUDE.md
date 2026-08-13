# Froala Editor for Vaadin Flow

A Vaadin Component Factory add-on that wraps the Froala WYSIWYG Editor (currently
v4.6.2, forward-compatible with the upcoming 5.x) as a Java Flow component,
requested for the NST application. The goal is to expose Froala's full feature set
— rich-text formatting, media/content insertion, inline/document/full-screen
modes, productivity plugins (paste-from-Word, markdown, find-and-replace, counts,
track changes, mentions, templates), localization/RTL and accessibility, HTML
sanitization and configurable image/file upload — through a clean Java API, with a
**configurable** (never hard-coded) license key.

The customer's original request is quoted verbatim in `docs/customer-request.md`,
together with the measured size of Froala's surface (302 options, 49 plugins, 39
locales) — read it before estimating or scoping anything.

Current state: scaffolding only. See `ROADMAP.md` for what is built, what is next,
and the effort estimate. The `GreetingComponent` / `GreetingView` classes are placeholders that prove
the build wiring; phase 1 replaces them.

## Stack

- **Vaadin** 24.10.9 (Core) on **Spring Boot** 3.5.15, **JDK** 17+
- Base package: `com.vaadin.componentfactory.froala`
- Build/verify gate: `mvn clean verify -Pproduction`

Java 17 and Vaadin 24 are deliberate: they are the platform's floor, so the
published add-on excludes as few consumers as possible. Do not raise them without
a reason that is written down in `ROADMAP.md`.

## Module structure

- **`component/`** — the add-on itself, published to the Vaadin Directory.
  **Standalone: no `<parent>`**, its own `vaadin-bom` import and properties, so it
  builds and releases on its own. **Spring-free** — `vaadin-core` only, per the
  official add-on guide, so it forces nothing on consumers. Anything wired to
  plugins on the reactor root (Spotless, Checkstyle) is **not** inherited here and
  must be duplicated into this pom.
- **`demo/`** — runnable Vaadin + Spring Boot app that depends on the add-on and
  showcases it. Holds everything Spring-shaped (`Application`, `@Service` beans,
  the future `@ConfigurationProperties` license-key binding) and the browserless
  tests. Its `spring-boot-maven-plugin` uses `<classifier>exec</classifier>` so the
  main jar stays a plain library jar the `e2e` module can depend on — don't remove
  that.
- **`e2e/`** — Playwright tests that boot the demo and drive it in a real browser,
  run by failsafe in the `production` profile.

The reactor root imports `spring-boot-dependencies` as a **BOM, not a parent**, so
the project can adopt a corporate parent later. Consequence: the BOM manages
dependency versions only, so the root pins plugin versions (`spring-boot-maven-plugin`
+ its `repackage` execution, surefire, failsafe) and compiler settings itself.

### Where the license key goes

The customer asked for the Froala license key to come from Spring config
(`application.properties`). That collides with the Spring-free add-on rule, so:
the component exposes a plain `setLicenseKey(String)` / static default, and the
Spring binding lives in the demo — or later in a separate optional
`vcf-froala-editor-spring` module. **Do not add a Spring dependency to
`component/`.**

## State management

This project uses **classic state (component fields / Spring beans)**.

Use plain component fields and Spring beans for state; wire UI updates explicitly.
Do not introduce Signals — they are a Vaadin 25 feature and this project targets 24.

## Testing

License-free stack, all run by `mvn clean verify -Pproduction`. Browserless layer:
**Karibu 2.4.x**. Browser e2e: **yes**.

- **JUnit 5** — unit tests (Spring Boot 3.x line).
- **Browserless UI-unit — Karibu Testing** (`com.github.mvysny.kaributesting.v10.LocatorJ`
  + `MockVaadin`, Spring-aware via `MockSpringServlet`). Fast, browser-free, runs in
  the normal test phase. Karibu is pinned to the **2.4.x** line — 2.4.x is Vaadin
  24.8+ only and 2.6.x+ is Vaadin 25 only, so the line must move together with the
  Vaadin version. Karibu does not initialise servlet filters (no Spring Security)
  and **executes no JavaScript** — it can assert server-side state and element
  attributes, never the Froala editor itself.
- **Browser e2e — plain Playwright** (`*IT` extending `SpringPlaywrightIT`) — real
  headless Chromium, run by failsafe in the `production` profile. DramaFinder is
  deliberately *not* used: 1.x is built against Vaadin 25 / JUnit 6 / Java 21 and
  does not fit this stack. **Every Froala behaviour that needs the JS to run must
  be tested here.**

Mirror existing tests when adding new ones.

When a module's Java or frontend changes, rebuild that module before running the
demo or e2e tests so they don't run against a stale jar. The demo's `production`
profile already sets `forceProductionBuild`, so the e2e run exercises the true
optimized bundle rather than a precompiled one.

## Conventions

- Vaadin views: `@Route` + access annotation (`@AnonymousAllowed` / `@PermitAll`).
- The MCP `vaadin` server is the source of truth for Vaadin API — prefer it over
  memory when reaching for component APIs.
- Don't pin dependency versions to a guessed "latest" — resolve the current
  release first.
- Code style: read and follow `STYLEGUIDE.md`. Spotless + Checkstyle run in the
  build; `mvn spotless:apply` fixes formatting.
- Domain language: use the terms in `CONTEXT.md`.

## Working conventions

- **Don't present an assumption as fact.** Verify it (Vaadin MCP, docs, grep, run
  it) or say you don't know. Being uncertain is fine; guessing confidently is not.
- **Commits:** one commit per logical phase or feature; run the tests before
  committing, and don't commit on the user's behalf unless asked.
- **Never push.** Pushing, opening PRs and anything else that leaves this machine
  is the maintainer's step, always — not something to offer or do, even when the
  commits are ready and a remote exists.
- **Tests & long-running ops:** run new/changed tests first; only run the full
  suite once those pass. Don't wrap waits in `until … done` sleep loops (they can
  stall) — poll periodically and check whether a background job has died.
- **Never self-dispatch after a question:** if you ask the user something, wait
  for the answer before acting.
- **Pick the cheapest model that fits a subagent.** Always pass `model` explicitly
  — the inherited default is Opus or better, which is expensive for mechanical work.
  Restate the critical rules in each subagent's prompt. When unsure, start cheaper
  and escalate only if the output is shallow.

  | Subagent role | Model |
  |---|---|
  | Mechanical implementer (plan specifies the exact code) | Haiku |
  | Explore / search ("where is X defined") | Haiku |
  | Multi-file integration / pattern matching | Sonnet |
  | Per-phase code-quality or spec-compliance review | Sonnet |
  | Final whole-branch / holistic / deep design review | Opus or better |
