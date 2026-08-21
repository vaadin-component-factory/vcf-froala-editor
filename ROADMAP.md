# Roadmap — Froala Editor for Vaadin Flow

Progress tracker for `vcf-froala-editor`. Update the status boxes as work lands;
record decisions in `memory/addon-constraints.md`, not here. The customer's
original wording lives in `docs/customer-request.md`.

**Status legend:** `[ ]` open · `[~]` in progress · `[x]` done

---

## Phase 0 — Scaffolding ✅

- [x] Reactor root (Spring Boot as BOM, not parent), `component/` (standalone,
      Spring-free), `demo/`, `e2e/`
- [x] Vaadin 24.10.9 Core / Spring Boot 3.5.15 / JDK 17
- [x] Karibu 2.4.4 browserless layer in `demo/`
- [x] Plain Playwright 1.62.0 e2e layer in `e2e/`
- [x] Spotless + Checkstyle (root **and** `component/`, which inherits nothing)
- [x] Green `mvn clean verify -Pproduction`
- [x] `cd component && mvn clean verify` green standalone (the Directory path)

**State as of 2026-08-13:** scaffolding complete, `mvn clean verify -Pproduction`
green, and committed on `main` (three commits: scaffolding, project docs,
customer request + estimate). No remote is configured yet.

The `GreetingComponent` / `GreetingView` / `GreetingService` classes are
placeholders that prove the wiring. Phase 1 replaces them.

---

## Phase 1 — Minimal working Froala wrapper

Goal: a `FroalaEditor` component that renders, round-trips HTML, and is covered by
one browserless and one e2e test.

- [ ] Decide the integration shape: a project-owned LitElement/TS connector that
      instantiates Froala, vs. driving Froala from Flow via the Element API.
      Record the decision and why.
- [ ] `@NpmPackage("froala-editor", version = "4.6.2")` + `@JsModule` for the
      connector; import Froala's CSS via `@CssImport`/`@StyleSheet`
- [ ] `FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String>` (or
      `CustomField<String>`) so it behaves like a normal Vaadin field: `getValue`,
      `setValue`, `addValueChangeListener`, Binder support
- [ ] **Delta-based value transfer** (NST requirement: large HTML must not be sent
      whole on every change). Prior art to port: `parttio/hugerte-for-flow`,
      **Apache-2.0**, same license as this add-on — reuse is clean with attribution.
      Its mechanism, verified 2026-08-13:
      - client keeps `_lastSyncedValue`; on change `dmp.patch_make(last, current)`
        → `patch_toText` → `_value-delta` CustomEvent, plus a 50 ms throttle
      - server `applyDelta(old, delta)` = `patchFromText` + `patchApply`, ~10 lines
      - `org.bitbucket.cowwoc:diff-match-patch:1.2` (Java) + npm
        `diff-match-patch@1.0.5`. Both resolve; both are old but the algorithm is
        stable — note them as a small supply-chain item, not a blocker.
      Three things to do better than the reference:
      - it **discards the patch-apply result flags** (`results[1]`), so a patch
        that does not apply cleanly corrupts the value silently. Check them and
        fall back to a **full resync** on failure.
      - **server → client is still full HTML** there; `setValue()` of a large
        document ships everything. Decide whether NST needs that direction too.
      - its config layer uses Jackson 3 (`tools.jackson`, Vaadin 25) — **not**
        portable to this Vaadin 24 stack. Only the delta code ports.
- [ ] `setLicenseKey(String)` + a static default (see `CLAUDE.md` — **no Spring in
      `component/`**)
- [ ] Replace the Greeting placeholders in `demo/` with a `FroalaEditorView`
- [ ] Karibu test: value round-trip / server-side state only (no JS runs there)
- [ ] Playwright IT: type into the real editor, assert the value reaches the server
- [ ] Delete `GreetingComponent`, `GreetingView`, `GreetingService`, and their tests

Removing the last placeholder is the definition of done for this phase.

---

## Phase 2 — Configuration API

Goal: expose Froala's options through Java instead of leaking raw JSON.

- [ ] A `FroalaConfig` builder mapping Froala options to typed Java setters,
      serialized to the connector as JSON
- [ ] Toolbar composition: enum/constant per Froala button, ordered groups,
      responsive breakpoints
- [ ] Editing modes: inline, document, full-screen
- [ ] Escape hatch: `setOption(String, Object)` for anything not yet typed —
      cheaper than chasing Froala's full option surface up front
- [ ] Demo view exercising each mode

---

## Phase 3 — Spring integration (license key + upload)

- [ ] `@ConfigurationProperties("vaadin.froala")` binding — `license-key` at
      minimum — in `demo/`, or a new optional `vcf-froala-editor-spring` module
      (decide; a `-spring` module is the reusable answer, the demo the cheap one)
- [ ] Server-side image/file upload endpoint + `setImageUploadURL` wiring
- [ ] Document how a consuming app supplies the key via `application.properties`

---

## Phase 4 — Feature coverage

Ordered by customer priority. Each item = Java API + demo + at least one e2e
assertion.

- [ ] Rich-text formatting: fonts, colors, styles, lists, tables, quotes, code view
- [ ] Media & content: images, files, links, emoji, special characters
- [ ] Productivity: paste-from-Word, markdown, find-and-replace, word/char count,
      track changes, mentions, templates
- [ ] Localization / RTL — wire Vaadin's `I18NProvider` locale into Froala's
      `language` option. Verified against froala-editor 5.3.1:
      - 39 language files, each a **UMD module** (~26 KB) that self-registers into
        `FroalaEditor.LANGUAGE` and must be loaded *before* editor init. Statically
        bundling all 39 is ~1 MB in every consuming app — use a dynamic import Vite
        can resolve statically. This is the whole cost of the item.
      - Codes are not `java.util.Locale`: `pt_br`, `zh_cn`, `en_gb`, `me`, `ku` —
        and there is **no `en` file** (English is built in). Mapping + fallback
        rules need a decision, the table itself is generated.
      - **RTL ships inside the language file** (`direction: 'rtl'` in `ar.js`), so
        it is not a separate axis; `direction` also exists as a top-level option to
        override. Flow side must mirror `dir` onto the host element.
      - Test 3 locales (de / ar / zh_cn), not 39, plus one cheap test asserting
        every enum constant resolves to an existing file.
- [ ] Accessibility: keyboard navigation, ARIA, focus handling
- [ ] HTML sanitization — decide client-side (Froala) vs. server-side
      (jsoup/OWASP) vs. both. **Server-side is the trust boundary**; client-only
      sanitization is not enough.

---

## Phase 5 — Froala 5.x

**5.x is already GA** (5.0.0 on 2026-01-15, 5.3.1 current as of 2026-08-13) — the
customer request's "upcoming 5.x" is out of date. The plugin surface is purely
additive (42 → 49, nothing removed), so this is a target-version decision, not a
migration project.

- [ ] Decide the target: build against 5.x from day 1 (recommended) vs. 4.6.2
      because NST is pinned there
- [ ] Keep the connector's Froala-specific surface behind one TS file so a major
      swap stays localized
- [ ] If both majors must be supported: one artifact with a version switch, or
      separate branches — decide before Phase 2 hardens the option API

---

## Phase 6 — Release

- [ ] `README.md` with usage, license-key setup, and a compatibility table
- [ ] Licensing statement: the add-on is Apache-2.0 but **Froala itself is
      commercial** — consumers need their own Froala license. Make this
      unmissable.
- [ ] `assembly` / Directory metadata, `mvn install` works standalone in
      `component/`
- [ ] Publish to the Vaadin Directory + Maven Central

---

## Effort estimate (2026-08-13)

Answer to the customer's "how much effort is this?". Grounded in the measured
surface in `docs/customer-request.md` — 302 options, 49 plugins, 39 locales — not
in a gut feeling. Person-days = one experienced Vaadin/Flow developer who has done
a JS-component integration before, 8 h days, including tests and demo.

The two columns are estimated by **different methods on purpose** — dividing the
human number by a productivity factor produces a wrong answer, because the two
have almost disjoint bottlenecks.

| Phase | Scope driver | Human dev | Claude + copilot |
|---|---|---|---|
| 1 Core wrapper | connector, value round-trip, field/Binder semantics, license key, detach | 8–12 d | 1–2 d |
| 1b Delta transfer | diff-match-patch both halves + resync-on-drift; ported from `hugerte-for-flow` (Apache-2.0) instead of built fresh | 1–2 d *(3–5 d without the prior art)* | 0.25–0.5 d |
| 2 Config API | 302 options (~130 typed + escape hatch), toolbar model, 4 breakpoints, 3 modes | 15–20 d | 1.5–2.5 d |
| 3 Spring + upload | `-spring` module, image/file upload endpoint, image manager, limits | 10–14 d | 1.5–2.5 d |
| 4a Formatting & media | ~15 plugins, config + demo + e2e each | 8–10 d | 0.5–1 d |
| 4b Productivity | Word paste/import/export, markdown, find&replace, counters, code view/snippet, templates | 8–12 d | 1–1.5 d |
| 4c Track changes | own accept/reject API, server-side representation | 5–8 d | 1–2 d |
| 4d Mentions | **not a Froala plugin** — custom trigger + async server data feed | 6–10 d | 1–2 d |
| 4e Localization / RTL | 39 language files, lazy load, `I18NProvider` wiring, RTL | 4–6 d | 0.5–1 d |
| 4f Accessibility | keyboard, ARIA, focus, screen-reader pass | 4–6 d | 1–2 d |
| 4g Sanitization | server-side policy (jsoup/OWASP), allow-list API, XSS corpus | 5–7 d | 0.5–1 d |
| 6 Release | README, compat table, licensing statement, Directory + Central, CI | 5–7 d | 0.5–1 d |
| Cross-cutting | review cycles, rework, flaky e2e, customer feedback | 14–21 d | 1.5–3 d |
| **Total (full scope)** | | **93–135 d ≈ 4.5–6.5 person-months** | **12–22 d ≈ 3–4 weeks** |

Phase 5 is 0 d if 5.x is the target from day 1; +2–4 d to also support 4.6.2.

**MVP cut — the number worth negotiating for.** Phases 1 + 3 + 4a + 4g, plus
Phase 2 reduced to toolbar/modes/escape-hatch instead of ~130 typed setters, plus
a lean release: **32–45 d human / 4–7 d with Claude**. Covers everyday editing,
images, upload, a safe HTML boundary. Track changes, mentions, markdown, full
localization and the long option tail land later as increments.

**How the Claude column is derived — and what actually limits it.** Not writing
speed; code volume is effectively free. The real costs, in order:

0. **Agent review runs first** — per-phase code-quality/spec review and a final
   holistic pass, per the subagent table in `CLAUDE.md`. Defect *finding* happens
   before anything reaches the copilot: style, missing tests, cross-module
   violations, naming drift across 130 setters, obvious upload/injection
   mistakes. Its cost trades against rework, so the Claude column is unchanged by
   it. Its blind spots are real, though: product taste, accessibility with actual
   assistive technology, NST-specific context, and the correlated blindness of
   reviewing code from the same model that wrote it (mitigated by independent
   reviewer agents, not eliminated).
1. **The copilot's review bandwidth — the binding constraint, but only on the
   parts that carry judgement.** With agent review upstream this is a *second*
   instance, not a first: budget **~12–20 h of the copilot's own time** for the
   full scope, ~6–10 h for the MVP. It concentrates on: the API shape
   (naming, what is typed vs. escape hatch — this is a published add-on, the
   signatures are semver-permanent), the upload endpoint, the sanitization
   allow-list, and actually *using* the demo by hand.
   Explicitly **not** on generated setter bodies. For generated code the review
   unit is the **signature list plus an exception list** ("not generated, and
   why / hand-typed / security-relevant") — one page, 30–60 min, not 130 method
   bodies. Correctness of the bodies is covered mechanically by a generated
   round-trip test over every setter, which is strictly better than reading them.
2. **Browser feedback cycles.** `mvn clean verify -Pproduction` runs in ~55 s on
   this project; a stubborn JS↔Flow bug costs 10–40 cycles. Hours, not days — but
   it is the one place where the work is genuinely serial.
3. **Things no amount of generation touches:** the Froala license key (without it
   e2e runs against a watermarked, partly gated editor), an accessibility pass
   with real assistive technology, Vaadin Directory / Maven Central / legal steps,
   and every decision that has to come back from NST. These are calendar time.

Deliberately *not* in the Claude column: the 302 typed setters, the toolbar enum
and the locale enum are generated from Froala's own `index.d.ts` and file listing.
That is a script plus one review pass — it does not scale with the option count,
which is exactly why the two columns diverge most in Phase 2.

Estimate history: a first version of this table put the Claude column at 40–61 d.
That number was human-days ÷ ~3, not a bottom-up estimate, and was corrected on
2026-08-13.

---

## Known issues

### Frontend formatting is not in the build gate (2026-08-20)

The `<typescript>` and `<css>` prettier steps were removed from both poms. **Cause,
reproduced:** Spotless starts the prettier node server with

    npm start --scripts-prepend-node-path=true -- --node-server-instance-id=<id>

`--scripts-prepend-node-path` was removed in **npm 7**. npm 11 only warns
("Unknown cli config ... will stop working in the next major version"), but **npm 12**
— `latest` since 2026-07-29 — aborts:

    npm error code EUNKNOWNCONFIG
    npm error Unknown cli flag: --scripts-prepend-node-path

`serve.js` then never runs, so the `server-<id>.port` file it is supposed to write
never appears, and Spotless gives up after a **2-minute timeout** with
`ServerStartException: Starting server failed. (...)` — a message that carries no
cause, and that neither `-e` nor `-X` expands. It looks like a hang; it is a timeout.

Ruled out along the way: proxy, `NODE_ENV`, node version (26.4.0 verified working),
`ignore-scripts`, node shims, and the npm install itself (`NpmInstall` passes only
`--no-audit --no-fund`, which is why that step always succeeded while only the
server start failed).

**Not fixable by upgrading:** `spotless-lib` 4.10.0 (2026-08-17, the newest) still
passes the flag.

**Upstream:** [diffplug/spotless#3024](https://github.com/diffplug/spotless/issues/3024)
— "prettier incompatibility with npm 12", opened 2026-08-19, still open. It already
identifies the same root cause and contains the npm 11 vs. npm 12 comparison, so
there is nothing to add; just watch it. Note it also reports that Spotless *retries
every minute* rather than failing once, which is why the symptom reads as a hang.

- [ ] Re-add both prettier blocks once it is fixed — `vcf-froala-editor.js`
      is going to be the most-edited file in the repo and deserves a gate. `.prettierrc`
      is deliberately kept at the project root for that return.
- [ ] Alternative if upstream stalls: pin an npm < 12 for Spotless only, via the
      step's `<npmExecutable>`. Note that Vaadin's bundled node
      (`~/.vaadin/node`, node 22 / npm 10) ships **no `npm` launcher** — only the
      `node` binary and `lib/node_modules/npm/bin/npm-cli.js` — so this needs a real
      npm installation, not that directory.

---

## Open questions

- **Froala 4.6.2 or 5.x?** 5.x has been GA since 2026-01-15 (5.3.1 current). Is NST
  pinned to 4.6.2, or can the connector target 5.x directly? This is the single
  biggest open decision — it shapes the option API.
- Does NST expect "full/maximum feature set" literally, or is the MVP cut above
  acceptable for the first release? The difference is ~3 months of work.
- Which Vaadin version does the NST application actually run? This project targets
  24 (the platform floor) — if NST is on 25, revisit Vaadin/Karibu/DramaFinder and
  the Java floor together.
- Does NST need track changes and mentions on day one, or are they phase 4 tail?
  They are the two most expensive items in the list.
- Who provides the Froala license key for CI? The e2e tests will show Froala's
  unlicensed watermark until one is available.
- Is `eclipse/license-header.txt` (Apache-2.0, "Copyright $YEAR Vaadin Ltd.") the
  wording Component Factory actually uses? It was authored during scaffolding to
  replace the template's `<YOUR NAME OR COMPANY>` placeholder. If it needs to
  change: edit that file, then `mvn spotless:apply` restamps every source file.
