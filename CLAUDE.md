# Froala Editor for Vaadin Flow

A Vaadin Component Factory add-on that wraps the Froala WYSIWYG Editor as a Java
Flow component, requested for the NST application. The customer named v4.6.2; the
add-on targets **5.4.0** and NST's sign-off on that is still open. The goal is to
expose Froala's full feature set — rich-text formatting, media/content
insertion, inline/document/full-screen
modes, productivity plugins (paste-from-Word, markdown, find-and-replace, counts,
track changes, mentions, templates), localization/RTL and accessibility, HTML
sanitization and configurable image/file upload — through a clean Java API, with a
**configurable** (never hard-coded) license key.

The customer's original request is quoted verbatim in `docs/customer-request.md`,
together with the measured size of Froala's surface (322 options in 5.4.0, 49
plugins, 39 locales) — read it before estimating or scoping anything.

Current state: the first cut is done — `FroalaEditor` renders, round-trips HTML
through a delta channel and takes a license key. What is built, what is next and
what the component guarantees all live in the issue tracker; see
`docs/agents/issue-tracker.md` for how to reach it. Decisions with their reasons
are in `docs/adr/`.

## General agent rules

- Code and document for humans. They have to understand and maintain the
  application.
- Don't guess, confirm with docs / sources / research results / MCP. Being
  uncertain is fine and saying so is fine; stating an assumption as fact is not.
- If there is no solution or answer, say it. Acknowledging failure is better than
  trying to hide it.
- **Answer the question you were asked before you edit anything.** A question about
  finished work wants an answer, not a rewrite.
- Never silently revert or tidy away something in the workspace you cannot
  explain. Ask, or leave it.
- Use plain and clear language in your answers, don't try to sound creative, keep it simple.

## Working conventions

- **Commits:** one commit per logical phase or feature; run the tests before
  committing, and don't commit on the user's behalf unless asked. `/implement`
  closes out with `/code-review` before the commit is offered.
- **Never push.** Pushing, opening merge requests and anything else that leaves this
  machine is the maintainer's step, always — not something to offer or do, even when
  the commits are ready and a remote exists. The agent's GitLab token enforces this
  rather than relying on good behaviour: Reporter, scoped to this project alone.
- **Tests & long-running ops:** run new/changed tests first; only run the full
  suite once those pass. Don't wrap waits in `until … done` sleep loops (they can
  stall) — poll periodically and check whether a background job has died.
- **Never self-dispatch after a question:** if you ask the user something, wait
  for the answer before acting.

## Agent skills

### Issue tracker

GitLab Issues in the project `stefan/froala` on the self-hosted instance
`gitlab.vaadin.com`, driven through the `glab` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, each label string equal to its name. See
`docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and one `docs/adr/` at the repo root. See
`docs/agents/domain.md`.

## Stack

- **Vaadin** 24.10.9 (Core) on **Spring Boot** 3.5.15, **JDK** 17+
- Base package: `com.vaadin.componentfactory.froala`
- Build/verify gate: `mvn clean verify -Pproduction`

Spring Boot 3.5.15 is not "the latest 3.5.x" — it is the version
`com.vaadin:vaadin-spring:24.10.9` is built against. Derive it from the Vaadin
release when bumping instead of taking the newest 3.5.x.

Java 17 and Vaadin 24 are deliberate: they are the platform's floor, so the
published add-on excludes as few consumers as possible. Do not raise them without
a reason written down as an ADR; see `docs/adr/0005-java-17-vaadin-24-floor.md`.

## Module structure

- **`component/`** — the add-on itself, published to the Vaadin Directory.
  **Standalone: no `<parent>`**, its own `vaadin-bom` import and properties, so it
  builds and releases on its own. **Spring-free** — `vaadin-core` only, per the
  official add-on guide, so it forces nothing on consumers. Anything wired to
  plugins on the reactor root (Spotless, Checkstyle) is **not** inherited here and
  must be duplicated into this pom — resolving their config through
  `${codechecks.config.dir}` = `${project.basedir}/..`, not the root pom's
  `${maven.multiModuleProjectDirectory}`, which points at `component/` itself during
  a standalone build and would break it. Holds the **unit and browserless tests**, so
  a standalone build verifies the published artifact. They stay Spring-free too:
  plain `karibu-testing-v10`, never `-spring`.
- **`demo/`** — runnable Vaadin + Spring Boot app that depends on the add-on and
  showcases it. Holds everything Spring-shaped (`Application`, `@Service` beans,
  the future `@ConfigurationProperties` license-key binding) and **no tests at all**
  — see Testing. Its `spring-boot-maven-plugin` uses `<classifier>exec</classifier>` so the
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
the component exposes a plain `setLicenseKey(String)` per instance, and the
Spring binding lives in the demo — or later in a separate optional
`vcf-froala-editor-spring` module. **Do not add a Spring dependency to
`component/`.**

**This project itself never has a commercial key** — not locally, not in CI. Froala
runs unlicensed here and shows its watermark, deliberately and permanently. Don't
propose acquiring one, don't write anything that needs a valid key, and don't treat
the watermark as a defect. Where a key has to be exercised, a dummy string is enough:
`FroalaTestView.LICENSE_KEY` proves it reaches Froala's own `opts.key`.

## State management

This project uses **classic state (component fields / Spring beans)**.

Use plain component fields and Spring beans for state; wire UI updates explicitly.
Do not introduce Signals — they are a Vaadin 25 feature and this project targets 24.

## Testing

License-free stack, all run by `mvn clean verify -Pproduction`. Browserless layer:
**Karibu 2.4.x**. Browser e2e: **yes**.

- **JUnit 5** — unit tests, in `component/`. Pinned there to the version
  `spring-boot-dependencies` manages in the reactor root, so a standalone build of the
  add-on and a reactor build run the same one.
- **Browserless UI-unit — Karibu Testing** (`MockVaadin`), in `component/`, test scope.
  Fast, browser-free, runs in the normal test phase. Plain `MockVaadin.setup()` — the
  `-spring` artifact and `MockSpringServlet` would pull Spring into the add-on, which
  is forbidden. Karibu is pinned to the **2.4.x** line — 2.4.x is Vaadin
  24.8+ only and 2.6.x+ is Vaadin 25 only, so the line must move together with the
  Vaadin version. Karibu does not initialise servlet filters (no Spring Security)
  and **executes no JavaScript** — it can assert server-side state and element
  attributes, never the Froala editor itself.
- **Browser e2e — plain Playwright** (`*IT` extending `SpringPlaywrightIT`) — real
  headless Chromium, run by failsafe in the `production` profile. DramaFinder is
  deliberately *not* used: 1.x is built against Vaadin 25 / JUnit 6 / Java 21 and
  does not fit this stack. **Every Froala behaviour that needs the JS to run must
  be tested here.**

**Test our wiring, not Froala.** The add-on's job is the connection between Flow and
Froala, so that is what the tests cover: does the value we set arrive, does the change
we make come back, does the option we pass reach `editor.opts`. Whether Froala itself
behaves correctly — its licensing, its toolbar rendering, its own HTML handling — is
Froala's scope and not ours to assert.

**No test lives in `demo/`, ever.** Unit and browserless tests belong in `component/`,
browser tests in `e2e/`. And no test asserts against the demo's views: browser tests run
against test views the tests own, under `e2e/src/test/java/.../it/views/`, browserless
tests build the component they assert on. The demo exists to show the add-on off and its
author has to stay free to change it — a test that reads it breaks on a label change.

**Three things about driving Vaadin + Froala from Playwright**, each learned the
expensive way — every one of them let a test pass with the bug deliberately reinstated:

- **`page.clock().runFor()`, never `fastForward()`.** `fastForward` fires each due timer
  at most once and never the ones scheduled while it jumps. Froala's own typing debounce
  scheduling our sync is exactly such a chain, so the jump silently breaks it.
- **`locator.click()` returns when the click is dispatched, not when the server has
  answered.** Reading client state right after it is a race. Give the test view's control a
  visible effect to wait for — the buttons that change something invisible disable
  themselves, and the test asserts `isDisabled()` first.
- **"Nothing has been sent yet" cannot be asserted on the viewer.** A value reaches it
  through a round trip in real time, so an empty viewer only means *not yet*. Count the
  client's own `_value-delta` dispatches instead; they happen synchronously in the timer
  callback.

Mirror existing tests when adding new ones.

When a module's Java or frontend changes, rebuild that module before running the
demo or e2e tests so they don't run against a stale jar. The demo's `production`
profile already sets `forceProductionBuild`, so the e2e run exercises the true
optimized bundle rather than a precompiled one.

## Who owns which file

Getting this wrong wastes the maintainer's time, so it is worth stating.

| File | Owner | Rule |
|---|---|---|
| `docs/customer-request.md` | maintainer | The quoted request is verbatim — never edit it. Questions *back to the customer* belong here, below the quote. |
| `docs/adr/` | Claude | Decisions with their reasons. Sparingly: hard to reverse, surprising, a real trade-off. |
| `CLAUDE.md`, `STYLEGUIDE.md`, `CONTEXT.md` | shared | Standing rules. Add here only what outlives the piece of work that raised it. |

## Conventions

- Vaadin views: `@Route` + access annotation (`@AnonymousAllowed` / `@PermitAll`).
- The MCP `vaadin` server is the source of truth for Vaadin API — prefer it over
  memory when reaching for component APIs.
- Don't pin dependency versions to a guessed "latest" — resolve the current
  release first.
- Code style: read and follow `STYLEGUIDE.md`. Spotless + Checkstyle run in the
  build; `mvn spotless:apply` fixes formatting. Spotless's `<pom><sortPom>` block is
  deliberately left out of both poms: they are hand-authored with block comments
  explaining the BOM-not-parent setup and the `exec` classifier, and sortPom would
  reorder elements away from what those comments document.
- Domain language: use the terms in `CONTEXT.md`.

