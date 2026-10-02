# Froala Editor for Vaadin Flow

A Vaadin Component Factory add-on that wraps the Froala WYSIWYG Editor as a Java
Flow component, targeting Froala **5.4.0**. The goal is to
expose Froala's full feature set — rich-text formatting, media/content insertion,
inline/document/full-screen modes, productivity plugins (paste-from-Word, markdown, find-and-replace, counts,
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
- **README:** a change that users of the add-on notice (new or changed API, changed
  behaviour, a new limitation) updates `README.md` in the same commit. #20 stays open
  as the release reminder, but the content lives in the README, not in the ticket.
- **No customer names, anywhere** — files, commit messages, issues. And no customer
  information in the README, the code or the issues: the add-on is published in the
  Vaadin Component Factory. Write requirements and decisions as the add-on's own, e.g.
  "decided (maintainer, date)". Only `docs/customer-request.md` keeps the request, with
  names redacted.
- **Never push.** Pushing, opening pull requests and anything else that leaves this
  machine is the maintainer's step, always — not something to offer or do, even when
  the commits are ready and a remote exists. The agent's GitHub token enforces this
  rather than relying on good behaviour: issues only, scoped to this repository alone.
- **Tests & long-running ops:** run new/changed tests first; only run the full
  suite once those pass. Don't wrap waits in `until … done` sleep loops (they can
  stall) — poll periodically and check whether a background job has died.
  A change that only touches `demo/` needs `mvn -pl demo verify`, not the full gate: the
  demo has no tests and `e2e/` does not depend on it (maintainer, 2026-09-28).
- **Never self-dispatch after a question:** if you ask the user something, wait
  for the answer before acting.
- **Subagent models:** pick the cheapest model that fits a subagent. Always pass `model`
  explicitly — the inherited default is Opus or better, which is expensive for mechanical
  work. Restate the critical rules in each subagent's prompt. When unsure, start cheaper
  and escalate only if the output is shallow.

  | Subagent role | Model |
  |---|---|
  | Mechanical implementer (plan specifies the exact code) | Haiku |
  | Explore / search ("where is X defined") | Haiku |
  | Multi-file integration / pattern matching | Sonnet |
  | Per-phase code-quality or spec-compliance review | Sonnet |
  | Final whole-branch / holistic / deep design review | Opus or better |

## Agent skills

### Issue tracker

GitHub Issues in `vaadin-component-factory/vcf-froala-editor`, driven through the `gh`
CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Two category roles and five state roles, each label string equal to its name. See
`docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and one `docs/adr/` at the repo root. See
`docs/agents/domain.md`.

## Stack

- **Vaadin** 24.10.7 (Core) on **Spring Boot** 3.5.14, **JDK** 17+
- Base package: `com.vaadin.componentfactory.froala`
- Build/verify gate: `mvn clean verify -Pe2e`. Without `-Pe2e` the reactor leaves `e2e/` out, so a
  plain `mvn clean install` runs no browser tests. `-Pproduction` additionally builds the demo's
  production bundle.
- The frontend build uses Vaadin's own Node.js in `~/.vaadin` (`require.home.node=true` in the
  root pom). In the devcontainer `~/.vaadin` is a read-only mount, so every build there adds
  `-Drequire.home.node=false`, e.g. `mvn clean verify -Pe2e -Drequire.home.node=false`.

Spring Boot 3.5.14 is not "the latest 3.5.x" — it is the version
`com.vaadin:vaadin-spring:24.10.7` is built against. Derive it from the Vaadin
release when bumping instead of taking the newest 3.5.x.

Vaadin stays on 24.10.7 on purpose (maintainer, 2026-09-30). From 24.10.8 on, a production
frontend build asks for a Vaadin license key even with core components only, and the e2e
build, CI and the demo server then fail without one. 24.10.7 builds without a key. Before
raising the 24.10.x version, build `e2e` with an empty `user.home` and check that it still
passes.

Java 17 and Vaadin 24 are the deliberate floor (ADR-0005). Do not raise them
without writing the reason down as an ADR first.

A Froala update reruns the theme generator, because the vaadin theme's rules are generated from
Froala's stylesheet (ADR-0007):
`node component/src/theme-generator/generate-vaadin-theme.js demo/node_modules/froala-editor`.

## Module structure

- **`component/`** — the add-on itself, published to the Vaadin Directory. Standalone:
  no `<parent>`, its own `vaadin-bom` import and properties, so it builds and releases
  on its own. **Spring-free, tests included** (ADR-0003). Holds the unit and browserless
  tests, so a standalone build verifies the published artifact.
  Spotless and Checkstyle hang off the reactor root and are **not** inherited here, so
  this pom repeats them — resolving their config through `${codechecks.config.dir}` =
  `${project.basedir}/..`. Never `${maven.multiModuleProjectDirectory}`: during a
  standalone build it points at `component/` itself and breaks the build.
  `mvn clean install -Pdirectory` in `component/` also builds the Vaadin Directory package,
  `target/vcf-froala-editor-<version>.zip`. It holds the jar, the sources jar, `LICENSE`,
  `README.md` and the Directory manifest from `component/assembly/`.
- **`demo/`** — runnable Vaadin + Spring Boot app that depends on the add-on and shows
  it off. Holds everything Spring-shaped (`Application`, `@Service` beans) and **no tests
  at all**. It shows how to use the add-on, not how to configure Spring, so it binds no
  license key (maintainer, 2026-10-01).
- **`e2e/`** — Playwright tests in a real browser, run by failsafe on every `verify` with `-Pe2e`,
  always against a production bundle. **Completely independent of `demo/`**
  (maintainer, 2026-09-28). It has no dependency on the demo, and its own app
  (`E2eApplication`), views, frontend bundle and test data. It depends on `component/`
  only. The app and views sit in `e2e/src/main/java` although they are test code,
  because `vaadin-maven-plugin` builds the bundle from the compile/runtime classpath and
  never sees `target/test-classes`.

The reactor root imports `spring-boot-dependencies` as a **BOM, not a parent**
(ADR-0002), and therefore pins plugin versions and compiler settings itself.

### The license key

`component/` exposes a plain `setLicenseKey(String)` per instance, with no global
default. Reading the key from configuration is the application's job (ADR-0003, ADR-0004).

**This project itself never has a commercial key** — not locally, not in CI. Froala
runs unlicensed here and shows its watermark, deliberately and permanently. Don't
propose acquiring one, don't write anything that needs a valid key, and don't treat
the watermark as a defect. Where a key has to be exercised, a dummy string is enough:
`FroalaTestView.LICENSE_KEY` proves it reaches Froala's own `opts.key`.

## State management

This project uses **classic state (component fields / Spring beans)**.

Use plain component fields and Spring beans for state; wire UI updates explicitly.
Do not introduce Signals (ADR-0005).

## Testing

License-free stack, all run by `mvn clean verify -Pe2e`.

- **JUnit 5** — unit tests, in `component/`, pinned to the version
  `spring-boot-dependencies` manages in the reactor root, so a standalone build of the
  add-on and a reactor build run the same one.
- **Browserless UI-unit — Karibu Testing** (`MockVaadin`), in `component/`, test scope.
  Fast, browser-free, runs in the normal test phase. Plain `MockVaadin.setup()`; the
  `-spring` artifact and `MockSpringServlet` are forbidden (ADR-0003). Pinned to the
  **2.4.x** line, which has to move together with the Vaadin version (ADR-0005). Karibu
  initialises no servlet filters (no Spring Security) and **executes no JavaScript** —
  it can assert server-side state and element attributes, never the Froala editor
  itself.
- **Browser e2e — plain Playwright** (`*IT` extending `SpringPlaywrightIT`), real
  headless Chromium, run by failsafe on every `verify` with `-Pe2e`. No DramaFinder
  (ADR-0005). **Every Froala behaviour that needs the JS to run must be tested here.**

**Test our wiring, not Froala.** The add-on's job is the connection between Flow and
Froala, so that is what the tests cover: does the value we set arrive, does the change
we make come back, does the option we pass reach `editor.opts`. Whether Froala itself
behaves correctly — its licensing, its toolbar rendering, its own HTML handling — is
Froala's scope and not ours to assert.

**No test lives in `demo/`, ever.** Unit and browserless tests belong in `component/`,
browser tests in `e2e/`. And no test asserts against the demo's views: browser tests run
against test views the tests own, under `e2e/src/main/java/.../it/views/`, browserless
tests build the component they assert on. The demo exists to show the add-on off and its
author has to stay free to change it — a test that reads it breaks on a label change.

Driving Vaadin + Froala from Playwright has three traps that each let a test pass with
the bug deliberately put back. They are written out under *Testing standards* in
`STYLEGUIDE.md`; read them before writing a browser test.

Mirror existing tests when adding new ones. When a module's Java or frontend changes,
rebuild that module before running the demo or e2e tests so they don't run against a
stale jar. The e2e module always builds with `forceProductionBuild`. It needs `-Pe2e` to
be in the reactor, but no `production` profile, and the e2e run exercises the true
optimized bundle rather than a precompiled one.

## Release

The demo server deploys from the branch `v-herd-demo` with the root `Dockerfile`, and it
shows the released version, not the work that follows it. The build would run with a
snapshot too, because the `Dockerfile` builds the add-on from `component/` itself. The
order of the steps keeps the branch on the release all the same. Until the first release
the branch carries the snapshot.

1. Remove `-SNAPSHOT` from every version in the four poms (root, `component`, `demo`,
   `e2e`), the parent references and the add-on dependencies included. In `README.md`, set
   the version in the Installation snippet and drop the note that the add-on is not
   released yet.
2. Commit as `Release <version>` and tag it with
   `git tag -a <version> -m "Release <version>"`.
3. Bring `v-herd-demo` to the tagged state by merging main into it with
   `git merge main -X theirs`. `git diff <version> HEAD` must come back empty.
4. The maintainer pushes main, the tag and `v-herd-demo` together, and creates the GitHub
   release.
5. Only then bump main to the next snapshot, usually the next patch (`1.0.0` becomes
   `1.0.1-SNAPSHOT`), and commit as `Bump to <next>-SNAPSHOT`. Bumping earlier would carry
   the snapshot into `v-herd-demo` with the merge.

The agent prepares steps 1 to 3 and 5 only when the maintainer asks, and never pushes.

## Who owns which file

Getting this wrong wastes the maintainer's time, so it is worth stating.

| File | Owner | Rule |
|---|---|---|
| `docs/customer-request.md` | maintainer | The quoted request is verbatim — never edit it. Questions *back to the customer* belong here, below the quote. |
| `docs/adr/` | Claude | Decisions with their reasons. Sparingly: hard to reverse, surprising, a real trade-off. |
| `CLAUDE.md`, `STYLEGUIDE.md`, `CONTEXT.md` | shared | Standing rules. Add here only what outlives the piece of work that raised it. |

## Conventions

- Vaadin views: `@Route`. The project has no Spring Security and the demo shows none, so a
  demo view needs no access annotation (maintainer, 2026-10-01).
- The MCP `vaadin` server is the source of truth for Vaadin API — prefer it over
  memory when reaching for component APIs.
- Don't pin dependency versions to a guessed "latest" — resolve the current
  release first.
- Code style: read and follow `STYLEGUIDE.md`. Spotless + Checkstyle run in the
  build; `mvn spotless:apply` fixes formatting. Spotless's `<pom><sortPom>` block is
  deliberately left out of all poms: they are hand-authored with block comments
  explaining the BOM-not-parent setup and the module layout, and sortPom would
  reorder elements away from what those comments document.
- Domain language: use the terms in `CONTEXT.md`.

