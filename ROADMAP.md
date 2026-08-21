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

The `GreetingComponent` / `GreetingView` / `GreetingService` placeholders are gone
as of `b5627df`; `GreetingViewIT` is the last one left (Phase 1).

---

## Phase 1 — Minimal working Froala wrapper ✅

Goal: a `FroalaEditor` component that renders, round-trips HTML, and is covered by
one browserless and one e2e test.

**State as of 2026-08-21** — done. The wrapper renders, round-trips through the
delta channel, takes a license key, and `mvn clean verify -Pproduction` is green
with 11 browserless and 11 e2e tests. Two things landed that were not planned here at
all: the `ValueChangeMode` enum and `FroalaViewer` (see below).

Everything still marked open below is either explicitly deferred or belongs to a
later phase — nothing blocks Phase 2.

Decided and done:

- [x] **Integration shape: a project-owned Lit element**, not Flow-side Element API
      calls. `vcf-froala-editor.js` is a `LitElement` composed with Vaadin's own
      field mixins (`FieldMixin`, `ThemableMixin`, `ElementMixin`, `FocusMixin`,
      `PolylitMixin`, `SlotStylesMixin`) and instantiates Froala on a plain `<div>`
      in its default slot. **Why:** the field parts (label / helper / error-message
      / required-indicator) and the Lumo `inputFieldShared` styles come for free, so
      the editor looks and validates like a native Vaadin field; and every
      Froala-specific call stays in one file, which is what Phase 5 needs.
- [x] `@NpmPackage` + `@JsModule` + `@CssImport` on `FroalaEditor` —
      **`froala-editor` 5.4.0**, not the 4.6.2 the customer named (see Phase 5)
- [x] `FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String>`,
      plus `HasValidator`, `HasValidationProperties`, `HasLabel`, `HasHelper`,
      `HasSize`, `HasStyle`, `Focusable`, `InputNotifier`
- [x] Demo view: `BasicView` replaces the Greeting placeholder, wired into
      `MainLayout`'s side nav
- [x] `ValueChangeMode` (`ON_CHANGE` / `ON_BLUR` / `TIMEOUT` / `INTERVAL`) +
      `ClientSideReference` — **not originally planned.** Flow's own
      `com.vaadin.flow.data.value.ValueChangeMode` does not fit: the trigger is a
      Froala event (`contentChanged`, `blur`), not DOM input, so the modes and their
      client-side string representations are our own. Naming clash with the Flow
      enum is accepted and documented in the javadoc.
- [x] `FroalaViewer` — **not originally planned.** A read-only `fr-view` host so an
      app can render Froala HTML outside the editor with Froala's own CSS.

Open:

- [x] **Delta-based value transfer** (NST requirement: large HTML must not be sent
      whole on every change). Client → server works: the connector keeps
      `_lastSyncedValue`, builds a `patch_make`/`patch_toText` delta on change with
      a 50 ms throttle, and fires `_value-delta`; the server applies it in
      `FroalaEditor.applyDelta` and calls `setModelValue(…, true)` so the change
      event reads as client-originated. Ported from `parttio/hugerte-for-flow`
      (Apache-2.0, same license — reuse is clean with attribution).
      `org.bitbucket.cowwoc:diff-match-patch:1.2` + npm `diff-match-patch@1.0.5`.
      Still to do, of the three improvements over the reference:
      - [x] **Patch-apply flags are checked.** `results[1]` is a `boolean[]`, one
            entry per patch; verified empirically against diff-match-patch 1.2 that a
            failing patch returns the *unchanged* input, so the reference
            implementation loses the edit without a trace. `applyDelta` now throws
            `FroalaEditor.DeltaMismatchException`, and the delta listener answers it
            by calling the client's `resyncValue()`, which sends the full value back
            over a `_value-resync` event. The resync direction is deliberate: the
            client holds what the user typed, so pushing the server's stale value
            would discard it.
      - [x] **server → client stays full HTML — deliberate.** The delta handler sets
            only the *model* value, never the presentation value, so a client-side
            edit sends nothing back. The full value is pushed once, on detach
            (`setPresentationValue(getValue())`), so the next attach starts in sync.
            No server→client delta channel needed unless NST reports a case where a
            programmatic `setValue()` of a huge document is on a hot path.
      - [x] the reference's Jackson-3 config layer is not portable to Vaadin 24 —
            not ported, only the delta code was.
- [x] **`setLicenseKey(String)` + a static default** (`setDefaultLicenseKey`), the
      customer's named requirement. Maps onto Froala's **`key`** option — *not*
      `apiKey`, which is the Google Drive key; see `docs/customer-request.md`. Passed
      as an element property and read once, when the client side editor initializes,
      which is why the connector now initializes from `firstUpdated` instead of
      `connectedCallback`. Spring binding stays Phase 3 (**no Spring in
      `component/`**).
- [x] Browserless tests, 11 of them: `FroalaEditorKaribuTest` (8 — value round
      trip, value change listener, license key incl. the static default, value change
      mode, timeout validation) and `FroalaEditorDeltaTest` (3 — `applyDelta` happy
      path, empty base, and the drift case that must throw rather than lose the
      edit). The delta test is plain JUnit, no Vaadin: `applyDelta` is a pure
      function, so Karibu would only add setup cost.
- [x] `FroalaEditorIT`, 11 tests — the only layer that runs Froala's JavaScript:
      typing reaches the server through the delta channel (asserted via the demo's
      `FroalaViewer` echo, which only the server-side value change listener ever
      writes), editor and toolbar render, the Focus button really moves focus into
      `.fr-element`, fast typing followed by a blur loses nothing, a sync inside the
      50 ms throttle window is deferred rather than dropped, `ON_BLUR` syncs only on
      focus loss, detach/re-attach keeps the value *and* leaves a working editor, a
      drifted client recovers through the resync path, and `readonly` / `disabled`
      really remove `contenteditable`. The label/helper test is marked in code as a
      Vaadin `FieldMixin` smoke check, not Froala coverage — it would pass with the
      editor deleted.

      The demo grew the controls these tests need (`readonly-toggle`,
      `enabled-toggle`, `attach-toggle`, plus ids on the existing ones): a browser
      test cannot reach the server-side API on its own. Writing them also turned up a
      demo bug — `WordUtils.capitalizeFully("ON_BLUR")` renders `On_blur`, because it
      only splits on whitespace.
- [x] All placeholders deleted, `GreetingViewIT` included.
- [x] **Green build gate.** `mvn clean verify -Pproduction` passes: 11 browserless
      + 11 e2e tests, Spotless and Checkstyle clean in all four modules.

Carried into the connector as marked TODOs, tracked here so they are not lost:

- [x] `ValueChangeMode.TIMEOUT` implemented as a debounce
      (`restartValueChangeTimeoutIfMode`), restarted on every `contentChanged`.
      `INTERVAL` now also starts from Froala's `initialized` event rather than only
      on a mode switch.
- [x] The 50 ms throttle in `onValueChange` **defers instead of drops.** It used to
      return without syncing, which is safe for a keystroke (a later one carries the
      delta) but loses the last edit for a blur or timeout flush, since no later
      change follows.
- [x] Initial value assignment at editor init — the connector seeds
      `editorElement.innerHTML` before constructing Froala, which adopts it; there is
      no init option for the content
- [x] `readonly` / `disabled` → **`edit.off()` / `edit.on()`**. Froala has no
      `mode.set` — that was a TinyMCE API carried over from the `hugerte` reference;
      the real one is the `Edit` module (`off`/`on`/`isDisabled`). Applied from
      Froala's `initialized` event, because the editor builds asynchronously.
- [x] **Adopted Vaadin's own mixins instead of hand-rolled properties**:
      `disabled` now comes from `DisabledMixin` (which also maintains `aria-disabled`
      and is declared `sync: true`), and the locally declared `focused` property is
      gone — `FocusMixin` toggles that attribute itself, so a reflected property
      fought it on every update. `readonly` stays local (Vaadin's lives in
      `InputControlMixin`, which assumes a slotted `<input>`) but now matches
      Vaadin's shape.
- [ ] Server-side editor configuration (`initialConfig` / `rawInitialConfig` are
      declared but never read; the `setConfig` the comment names does not exist).
      Overlaps Phase 2 — decide there whether Phase 1 gets a raw-JSON escape hatch
      or waits for the typed API.
Deferred out of Phase 1 — none is needed for "renders, round-trips, is tested".
Each has a `TODO Phase 2` at its place in the connector:

- [ ] Tooltip support. Decide between one Vaadin tooltip on the host and delegating
      to Froala's own `Tooltip`/`Popups` modules. The old comment argued this in
      HugeRTE terms, which does not describe Froala — re-justify against those two
      modules.
- [ ] `replaceSelectionContent` — the Froala equivalent is
      **`editor.html.insert(html, clean, doSplit)`** (`index.d.ts:2256`). Deferred
      because `clean`/`doSplit` are configuration decisions.
- [ ] `isInDialog()` (commented out) — an iframe-era TinyMCE workaround for toolbar
      positioning inside a dialog. Froala positions DOM-relative through
      `Position`/`Popups`, so verify there is a problem at all before implementing;
      delete the block if the toolbar behaves inside `vaadin-dialog`.
- [ ] Server-driven editor configuration. `initialConfig` / `rawInitialConfig` carry
      over from **both** references — the same split exists in
      `stefanuebe/vaadin-fullcalendar` as `initialOptions` / `initialJsonOptions`,
      spread into the init options. Per the maintainer it has broadly proven itself
      there, because of how Flow handles JSON server-side. The fields are in place,
      the channel is not. **Do not decide this in isolation:** whether the split
      earns its place here is a Phase 2 question, to be answered together with the
      option API as a whole, not in advance.

### Hugerte / TinyMCE artefact sweep (2026-08-21)

The connector was ported from `parttio/hugerte-for-flow`, which wraps a **TinyMCE
fork**, so TinyMCE APIs that do not exist in Froala came along with it. Swept
against the installed froala-editor 5.4.0. Found and dealt with:

- `mode.set('readonly'|'design')` → **`edit.off()` / `edit.on()`** (fixed)
- `selection.setContent(html)` → **`html.insert(...)`**. `setContent` appears zero
  times in the whole of `index.d.ts`; `FroalaSelection` has 19 methods and none is
  it. (TODO, Phase 2)
- per-instance `resizeObserver` teardown that nothing ever created — TinyMCE is
  iframe-based and needs a reflow observer, Froala is a plain `contenteditable`
  div. Also not Vaadin's `ResizeMixin` pattern, which uses a module-level singleton.
  (deleted)
- `isInDialog()` and the tooltip rationale (TODOs above)

Verified as genuinely Froala, so nobody "fixes" them: `html.get/set`, `destroy()`,
`edit.off/on`, the events `initialized` / `blur` / `focus` / `contentChanged`,
`events.focus()`, the `key` option, and all eleven plugin names quoted in Phase 4b.
The Java layer carries no artefacts — it only ever talks element properties and
events. The CSS was spot-checked for `tox-`/`mce-` prefixes; a full CSS sweep has
not been done.

### Review findings fixed after the first draft (2026-08-21)

Three parallel Sonnet reviews (Vaadin/Flow API + lifecycle, Froala/hugerte
artefacts, test quality). What they caught, beyond the artefacts above:

- **Orphaned editor.** Lit does not check `isConnected` before running
  `firstUpdated`, and Flow can attach then detach before that update flushes — the
  result was a live Froala instance on a host that had already had its only
  `disconnectedCallback`, with nothing left to destroy it. `_initEditor` now returns
  early when `!this.isConnected`.
- **Timers outlived the element.** `disconnectedCallback` cleared the editor but not
  `_valueChangeHandleForInterval` / `_valueChangeHandleForTimeout` /
  `_throttleHandle`. In `INTERVAL` mode the interval kept firing against a destroyed
  editor, and every re-attach started another one on top. Found independently by two
  of the three reviews.
- **`super.disconnectedCallback()` was called last.** `FocusMixin`,
  `ControllerMixin` and `ResizeMixin` all call super *first* and tear down after.
  Aligned.
- **`updateReadonlyMode()` broke the connector's own stated invariant** — it touched
  Froala's `edit` module from `updated()`, which runs in the same cycle as
  `firstUpdated`, i.e. before Froala's async `initialized` event. Now gated on an
  `_editorInitialized` flag and re-applied from that event, so a component that
  starts out `disabled` is not lost.
- **Mixin order** was `DisabledMixin(FocusMixin(...))`; Vaadin's own components nest
  `FieldMixin(FocusMixin(DisabledMixin(...)))`. No behavioural difference found in
  either mixin's source, aligned for consistency.
- **Dead code removed:** the write-only `initialized` field, the `addAttachListener`
  that existed only to set it, and the now-unused `runBeforeClientResponse` helper;
  the duplicate `@NpmPackage`/`@JsModule`/`@CssImport` on `BasicView` (a second
  source of truth for the Froala version pin).
- **Landmine documented, not changed:** the three-arg `super("value", "", true)`
  constructor makes Flow register its own listener for a `value-changed` DOM event.
  Nothing dispatches it, so it is inert — but making `value` a notifying Lit
  property would quietly open a second update path beside `_value-delta`. Noted at
  the constructor.
- **`setDefaultLicenseKey` is global mutable state.** Javadoc now warns test authors
  to reset it; the only current guard is one `@AfterEach`.

Still open from the reviews, deliberately:

- [ ] e2e for `TIMEOUT` and `INTERVAL` modes — needs Playwright's `page.clock()` so
      the test does not wait on the wall clock. `ON_CHANGE` and `ON_BLUR` are covered.
- [ ] e2e reading the license key off the live Froala instance
      (`editor.opts.key`). Needs the demo to expose a key input, since a browser test
      cannot reach the server API. Server side is covered browserless.
- [ ] A direct assertion that no interval leaks across detach. The clean-up is in
      place and the value/edit contract is covered, but the leak itself is hard to
      observe from outside: the events would be dispatched from an element that is no
      longer in the tree.
- [x] `focus()` null check (`this.editor?.events.focus()`)

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
      track changes, mentions, templates. Plugin-backed in 5.4.0 and therefore
      cheap: `word_paste`, `import_from_word`, `export_to_word`, `markdown`,
      `find_and_replace`, `word_counter`, `char_counter`, `track_changes`,
      `code_view`, `code_snippet`, `code_beautifier`. **Not** plugin-backed and
      therefore custom work: **templates** (Froala's `RegisterTemplate` /
      `ICON_TEMPLATES` / `POPUP_TEMPLATES` are icon and popup markup, not document
      templates) and **in-document mentions** (see Phase 4d in the estimate).
- [ ] Localization / RTL — wire Vaadin's `I18NProvider` locale into Froala's
      `language` option. Verified against froala-editor 5.4.0 (re-checked 2026-08-21):
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

**5.x is already GA** (5.0.0 on 2026-01-15, **5.4.0** current, released
2026-08-19) — the customer request's "upcoming 5.x" is out of date. The plugin
surface is purely additive (42 → 49, nothing removed), so this was a target-version
decision, not a migration project.

- [x] **Decided 2026-08-21: stay on 5.4.0.** `docs/customer-request.md` has been
      re-baselined onto that version and re-measured against the package installed
      in `demo/node_modules`: **322 options** (up from 302 in 5.3.1), 49 plugins, 39
      locales, 37 CSS files. The option surface is still moving between minors —
      treat the count as a snapshot, and re-measure once more before Phase 2 freezes
      the typed API.
- [ ] Confirm with NST that 5.x is acceptable, or budget the +2–4 d to also support
      4.6.2. The customer request names 4.6.2 explicitly, so this is *our* decision
      until they sign off on it.
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
surface in `docs/customer-request.md` — 322 options, 49 plugins, 39 locales, as
measured against froala-editor 5.4.0 — not in a gut feeling. Person-days = one experienced Vaadin/Flow developer who has done
a JS-component integration before, 8 h days, including tests and demo.

The two columns are estimated by **different methods on purpose** — dividing the
human number by a productivity factor produces a wrong answer, because the two
have almost disjoint bottlenecks.

| Phase | Scope driver | Human dev | Claude + copilot |
|---|---|---|---|
| 1 Core wrapper | connector, value round-trip, field/Binder semantics, license key, detach | 8–12 d | 1–2 d |
| 1b Delta transfer | diff-match-patch both halves + resync-on-drift; ported from `hugerte-for-flow` (Apache-2.0) instead of built fresh | 1–2 d *(3–5 d without the prior art)* | 0.25–0.5 d |
| 2 Config API | 322 options (~130 typed + escape hatch), toolbar model, 4 breakpoints, 3 modes | 15–20 d | 1.5–2.5 d |
| 3 Spring + upload | `-spring` module, image/file upload endpoint, image manager, limits | 10–14 d | 1.5–2.5 d |
| 4a Formatting & media | ~15 plugins, config + demo + e2e each | 8–10 d | 0.5–1 d |
| 4b Productivity | Word paste/import/export, markdown, find&replace, counters, code view/snippet, templates | 8–12 d | 1–1.5 d |
| 4c Track changes | own accept/reject API, server-side representation | 5–8 d | 1–2 d |
| 4d Mentions | no in-document mention plugin — custom trigger + async server data feed. **Unconfirmed:** the `collaborative` plugin ships @mention *in comments* (`mentionableUsers`); if that is what NST wants, this row shrinks — but it drags in the whole Yjs collaboration stack | 6–10 d | 1–2 d |
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

Deliberately *not* in the Claude column: the 322 typed setters, the toolbar enum
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

- **Froala 4.6.2 or 5.x?** Answered on our side — the connector targets **5.4.0**
  (Phase 5). Still needs NST's sign-off: are they pinned to 4.6.2, and if so, do
  they want one artifact supporting both majors?
- **Which "mentions" does NST mean?** In-document `@name` autocomplete (no Froala
  plugin — custom trigger plus server data feed), or @mention inside review
  comments (ships in the `collaborative` plugin, but only together with Yjs,
  `docId`, `commentsUrl`, `suggestionsUrl` and a role model)? The second is a much
  bigger surface than "mentions" suggests, and it changes both the estimate and the
  architecture. Ask before Phase 4d is scoped.
- **Are document templates in scope?** Froala has no template plugin, so this is
  ours to build. Cheap if "template" means "insert a stored HTML snippet", not
  cheap if it means a variable/placeholder system.
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
