# Roadmap — Froala Editor for Vaadin Flow

Progress tracker for `vcf-froala-editor`. Update the status boxes as work lands and
record decisions here, in the phase they belong to; standing rules that outlive a
phase go to `CLAUDE.md`. What a finished phase actually guarantees is specified in
`docs/specs/`; the customer's original wording lives in `docs/customer-request.md`.

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

Specified in `docs/specs/phase-1-value-transfer.md`,
`phase-1-value-change-modes.md`, `phase-1-lifecycle.md` and
`phase-1-component-api.md` — written after the fact, and the place where the
remaining test gaps are recorded per requirement.

**State as of 2026-08-24** — done. The wrapper renders, round-trips through the
delta channel, takes a license key, and `mvn clean verify -Pproduction` is green
with 11 browserless and 17 e2e tests. Two things landed that were not planned here at
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
- [x] **`setLicenseKey(String)`** per instance, the
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
      + 17 e2e tests, Spotless and Checkstyle clean in all four modules.

Carried into the connector as marked TODOs, tracked here so they are not lost:

- [x] ~~`ValueChangeMode.TIMEOUT`~~ — **removed on 2026-08-25**. It debounced
      `contentChanged`, which is itself a debounce, so it only stacked a second wait
      onto Froala's own (VCM-3, VCM-15). `setValueChangeTimeout` now configures Froala's
      `typingTimer` instead, and `INTERVAL` got its own `setIntervalPeriod`.
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
- [x] `isInDialog()` — **deleted 2026-08-24** (maintainer's finding 8). An iframe-era
      TinyMCE workaround for toolbar positioning; Froala positions DOM-relative through
      its `Position`/`Popups` modules and no problem is observable under `/overlay`.
- [x] **Decided 2026-08-24: keep destroying Froala on detach.** Two independent
      reasons, both checked rather than assumed.

      **1. Froala does not clean up after itself.** Verified against the installed
      5.4.0 bundle: `FroalaEditor.INSTANCES` is a global static array, `_init` does
      `INSTANCES.push(this)`, and the only place that removes an entry is `destroy()`
      (`INSTANCES.splice(INSTANCES.indexOf(this), 1)`, together with `$oel.off(...)`,
      `removeData('froala.editor')` and `core.destroy()`). No `MutationObserver`
      watches the editor's own element — the two in the bundle belong to the Yjs sync
      and the AI ghost plugin. So skipping `destroy()` does not just leak the instance
      and its detached DOM: **other live editors keep reaching into it**, because
      global handlers iterate `INSTANCES` — a `mousedown` handler calls
      `INSTANCES[i].popups.areVisible()` and `$el.find('.fr-marker')`, image and video
      resizing triggers `image.hideResizer` on every other instance, and drag handling
      scans all instances for `.fr-dragging`.

      **2. The alternative cannot pay off anyway.** A Flow detach discards the HTML
      element, so there is no element left for a surviving instance to live on — and
      re-parenting a component into a `Dialog` or `Popover`, which the demo's
      `OverlayView` does, is exactly such a detach plus attach.

      **A detach restores the value, and only the value.** Undo stack, caret and
      scroll position start fresh after a re-parent. That is not a trade we made and
      could unmake: Flow owns the disconnect, the element is gone by the time the
      connector hears about it, and nothing says whether the component will come back
      — so there is no state we could responsibly hold on to. The case where a user
      would notice is moving an editor between a dialog and the page, which is an edge
      case, and the maintainer closed it on that basis (2026-08-25). The purely
      client-side DOM move (dragging an element to a new parent without the server
      involved) is handled rather than broken — the element instance survives it, so
      `_lastSyncedValue` does too and `_initEditor` re-seeds from it. Prior art below.

      **Prior art, for a revisit if a concrete case turns up.** The connector calls
      `editor.destroy()` in `disconnectedCallback`. The maintainer's own
      `stefanuebe/vaadin-fullcalendar` (`v6_master`, `full-calendar.ts`) does the
      opposite on purpose: `disconnectedCallback` only releases observers and
      draggables, the widget instance survives, and `connectedCallback` falls through
      an `if (!this._calendar)` guard on re-attach. The reason is the churn Flow and
      Lit produce together — a component can be connected, get its first update and
      be disconnected again — which makes destroy-and-rebuild the expensive path and
      also throws away undo stack and caret. Keeping the instance trades that against
      holding DOM references when the detach is final. Both are defensible; pick one
      deliberately rather than by inheritance, and note that the `isConnected` guard
      in `_initEditor` and the timer clean-up already handle the *correctness* half.
      **Measured 2026-08-24, and it narrows the question:** a Flow detach/re-attach
      discards the HTML element and builds a new one, so a surviving Froala instance
      would be discarded with it. Keeping the instance can only pay off for a
      client-side DOM move of the same element, not for a Flow remove/add. See
      `docs/specs/phase-1-lifecycle.md`, LC-9 and LC-10.
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
- **`setDefaultLicenseKey` was global mutable state** and is **removed** (2026-08-24,
  maintainer's call): one key per instance set by the application is what other
  add-ons do, and storing it globally is not this add-on's scope. The javadoc warning
  about resetting it in tests, and the `@AfterEach` guard, went with it.

Still open from the reviews, deliberately:

- [ ] e2e for `TIMEOUT` and `INTERVAL` modes — needs Playwright's `page.clock()` so
      the test does not wait on the wall clock. `ON_CHANGE` and `ON_BLUR` are covered.
- [x] e2e reading the license key off the live Froala instance (`editor.opts.key`).
      Done 2026-08-24 — `FroalaEditorIT.licenseKey_arrivesInFroalasOwnOptions`. It
      needed no real key: the fixture view sets a dummy string, and whether a key is
      *valid* is Froala's scope, not ours.
- [ ] A direct assertion that no interval leaks across detach. The clean-up is in
      place and the value/edit contract is covered, but the leak itself is hard to
      observe from outside: the events would be dispatched from an element that is no
      longer in the tree.
- [x] `focus()` null check (`this.editor?.events.focus()`)

### Test debt — the work queue for phase 1

Everything phase 1 promises is built and the maintainer's findings are worked; what is
left before the phase can be called done is the list below, **in this order** (agreed
2026-08-25). Each item is a spec requirement that currently says *unverified*.

1. ~~**The timing of `TIMEOUT` and `INTERVAL`.**~~ **Done (2026-08-25).** e2e tests
   under Playwright's fake clock, each re-run with the bug put back in to prove the
   assertions bite. `TIMEOUT` did not survive the exercise: measuring it is what showed
   it was a second debounce on top of Froala's own, so it is gone and
   `setValueChangeTimeout` configures Froala's `typingTimer` instead (VCM-3, VCM-7).
   What is verified now is VCM-4, VCM-7, VCM-8 and VCM-19. Two things worth keeping:
   - It has to be `clock().runFor()`, not `clock().fastForward()`. `fastForward` fires
     each due timer at most once and never the ones scheduled while it jumps — and that
     chain is exactly the case here, because Froala's own debounce is what starts ours.
   - "Nothing has been sent yet" cannot be asserted on the viewer: the delta reaches it
     through a server round trip in real time, so looking right after a time jump only
     proves it is early. The tests count `_value-delta` dispatches on the client, which
     happen synchronously inside the timer callback.
2. ~~**VT-10 — a resync clears the pending throttle.**~~ **Done (2026-08-25).** Took
   three attempts to get a test with teeth, and the two failures are the lesson: the
   real server path never reaches the client while a sync is still pending, because the
   round trip is slower than the 50 ms window, and the pending throttle firing a moment
   later repairs the value on its own — so every assertion on the viewer passed with the
   bug in place. It is asserted on the client's own events now, with `resyncValue()`
   called at the moment the state exists.
3. ~~**LC-4 / LC-5 — `_initEditor`'s `isConnected` guard and its idempotence.**~~
   **Done (2026-08-25).** Both asserted in the browser, both counter-checked. LC-4 came
   with a measurement worth keeping: Lit does complete an update on an element that was
   attached and removed inside one task, but no editor is built through that path even
   with the guard removed — something upstream already covers the Lit route, so the
   guard is the second line of defence and is asserted on the call itself.
4. ~~**No interval leaks across detach.**~~ **Done (2026-08-25).** The leak is
   invisible on its own — a tick on a detached element finds no editor, computes an
   empty delta and dispatches nothing — so the test holds a reference to the element and
   gives the timer something to report before running the clock forward (LC-6).
5. ~~**After `setValue(X)` no delta arrives that nobody typed.**~~ **Done
   (2026-08-25).** With deliberately messy markup, because already-normalized HTML
   produces an empty delta and would prove nothing: the first version of this test
   passed with the bug reinstated for exactly that reason (VT-5).
6. ~~**The unverified API points.**~~ **Done (2026-08-25).** Three, not two: `Binder`
   both ways with `asRequired` firing on the empty value (API-1), Froala's focus and
   blur reaching Flow-side listeners (API-8), and the license key being read once at
   build time (API-11) — that last one pins a documented limitation, not a behaviour we
   would want.

**Phase 1 is closed with this queue.** Two requirements stay without a test and are
**parked as not relevant** — settled, not open work, and not to be raised again unless
something asks for them: VCM-5 (a mode switch flushes the pending value — rare on a live
editor, and the blur flush covers the ordinary exit) and LC-10 (the `connectedCallback`
branch for a purely client-side DOM move — no Vaadin 24 component has been shown to do
one, so there is nothing to reproduce).

### Acceptance — with the maintainer (2026-08-25) — passed

Walked by the maintainer on 2026-08-25; **every point passed**, nothing left open.

- **A1 / A2** — the blur flush broke nothing: `ON_CHANGE` shows no double sync, `ON_BLUR`
  behaves as before.
- **B1 — `/overlay`, dialog and popover** — the one place a Flow detach and a client-side
  DOM move meet. No defect.
- **B2** — no interval left running after a detach in `INTERVAL` mode.
- **B3** — no typing latency in a multi-page document.
- **B4** — layout: heights, toolbar wrapping, viewer frame.

The demo runs in dark mode (`@Theme(variant = "dark")`), which makes the phase 4 Lumo item
plainly visible: Froala's own chrome stays light. Known, and not a phase 1 defect.

### The maintainer's findings (2026-08-24) — worked

`docs/issues/findings.md` is the maintainer's queue. It is **gitignored and maintainer
owned**: read it, do not edit it, report back in chat. All eleven items are answered;
what follows is the part worth keeping.

- **4 / 5 — `hasUpdated` and `isConnected` are never assigned because neither is
  ours.** `hasUpdated` is Lit's own reactive-element flag, `isConnected` is the
  standard DOM `Node` property. Both are read-only from our side. What each one is
  *for* is documented at its call site in the connector.
- **6 — `setValue` did not reach the client after a client-side edit.** Fixed; see
  VT-11 and the decision below.
- **7 — documented, not changed**, as the maintainer asked. Their reading is the one
  in the code now: the listener is what would open a second update path if `value` ever
  became a notifying Lit property, and it is why we do not need our own value-change
  listener management.
- **8 — `isInDialog` removed.** No positioning problem is observable under `/overlay`;
  it was an iframe-era TinyMCE workaround and Froala positions through its own
  Position/Popups modules.
- **9 / 10 / 11 — done.** Markdown doc comments (`///`) converted back to `/** */`,
  changelog-voiced comments cut down to the reason they exist, both FIXMEs resolved.

#### Decision: how a server value reaches the browser (finding 6)

The bug had **two** layers of the same de-duplication, one on each side:

1. The server-side `value` property still held the value the *server* last set,
   because a client edit deliberately never writes it (VT-6). Flow drops a property
   write whose value the property already holds, so `setValue(x)` after the user typed
   on top of `x` produced no write at all.
2. Even with a write, the **browser** keeps its own copy of the state tree and ignores
   a property update whose value that copy already holds — stale for the same reason.

Fix: `setPresentationValue` detects exactly that case and pushes over a JS call
instead, which has no such comparison (VT-11). Everything else stays as it was —
`setModelValue` in the delta handler, the detach listener, the property as the normal
transport.

One deviation from the upstream shape: the guard's "is the browser there" half is an
own `liveOnClient` flag, not `isAttached()`. Measured — `isAttached()` answers `true`
inside a detach listener (Flow fires those before clearing the node's parent), so it
does not mean what it reads like, and the push it lets through is deferred to the next
attach and overwrites the value set in the meantime. `hugerte` reuses its
`isInitialized` field here, which already existed for `checkAlreadyInitialized()`; we
have no such field yet — but its `beforeClientResponse` timing is adopted regardless.
That timing exists for their config guard, not for the push, and makes no measurable
difference here; phase 2's configuration API is the point at which it would start to
matter, and a flag that flips too early is not worth rediscovering then. Both cases are
tested in `FroalaEditorKaribuTest`. See VT-11.

**This is otherwise the fix `parttio/hugerte-for-flow` already ships** (issue 30, closed;
`HugeRte.setPresentationValue`), down to the guard. Deliberately adopted rather than
invented: the two add-ons share the delta design, so they should share the answer and
there is one shape to re-check when Vaadin moves.

Rejected on the way, worth writing down so it is not tried again:

- **`clear()` then `setValue(x)`.** Suggested in the issue thread and reported there
  as not working. Both writes collapse into one round trip, so the browser only ever
  sees the final value — which its copy of the tree already holds.
- **Keeping the server-side property in step with every client edit** via
  `ElementPropertyMap.setProperty(name, value, false)`, Flow's own path for a
  client-originated property. It works and removes the need for the detach listener,
  but it costs a dependency on a Flow internal *and* breaks the invariant the guard
  above rests on: once the property tracks the editor, the drop case can no longer be
  detected from the server, so every `setValue` has to push unconditionally. The
  invariant is worth more than the tidier mirror.
- **`@Synchronize` on `value`.** The public way to keep both sides in step, and the
  reason Vaadin's own fields never see this bug — but it asks the browser to send the
  full value on every change, which defeats the delta channel outright.

---

## Phase 2 — Configuration API

Goal: expose Froala's options through Java instead of leaking raw JSON.
Specified in `docs/specs/phase-2-configuration.md` and `phase-2-theming.md`,
written before the code as agreed in `docs/specs/README.md`.

**The three checks are measured — 2026-08-27.** Each has a demo view, so the
result can be looked at and not only read. The views were built against the
connector's dead `initialConfig` property and moved onto `setOptions(String)` once
that existed.

| | Question | Answer | View |
|---|---|---|---|
| 1 | Are toolbar group names free? | **Free**, but a name that is not a registered command loses its overflow button | `/check-toolbar` |
| 2 | Where does an upload go with no URL? | **Nowhere** — the file stays in the browser as a `blob:` URL. Two other defaults do leave the network | `/check-upload`, with a real upload endpoint as the counter-check |
| 3 | Can plugins and languages load on demand? | **Yes**, all three verification points pass | `/check-on-demand`, which reports the files the browser fetched and their sizes |

Details are on the items below. Check 3's view is a spike element in `demo/`
(`froala-ondemand-spike.js`) and not the add-on: it imports Froala's core only and
pulls each plugin with a dynamic import.

The measurement harness from the planning session is still in the session
scratchpad (`harness2.html` plus `playwright-core` and small `*.mjs` drivers, served
over a local `python3 -m http.server`). It drives raw Froala in headless Chromium
without Vaadin — that is what answered checks 1 and 2; check 3 needed the real
production build.

**Done 2026-08-27, in the order agreed with the maintainer:** the grouped toolbar
form is typed (`FroalaToolbar`, `FroalaToolbarGroup`, `FroalaToolbarAlign`, plus the
three narrower-screen options), and the two demo views are in — `/options` and
`/toolbar`, both a form plus a "Reload editor" button that detaches the editor, sets
the options and attaches it again. The toolbar work went first because a demo built
on the flat form would have had to be rebuilt once groups landed.

Building the demos turned up a **connector bug they are the first thing to hit**:
Froala finishes building an editor that was destroyed mid-build and fires its events
anyway, and the handlers are bound to the element rather than to their instance, so
a discarded editor's `initialized` ran against its half-built successor and threw.
Each built editor now carries a generation and a stale handler stays quiet
(CFG-21, `FroalaOptionsIT.detachSetOptionsAttach_rebuildsWithoutBreakingTheEditor`).

Still open from the same round, deliberately not fixed: a detach that is followed by
`setOptions` in the same round trip **builds two editors** — one on re-attach with
the old options, one when the new options arrive an update later. The guard makes
the wasted one harmless, but it is still a full Froala init nobody sees.

Design decided 2026-08-27 with the maintainer, reasons in the specs:

- [x] `FroalaOptions` — immutable, typed, serialized to the raw JSON Froala expects
      (CFG-6, CFG-7). **Not a record**, decided 2026-08-27: a record's canonical
      constructor is public API and would change signature with every option we type,
      and the option surface moves between Froala minors. It holds an
      `elemental.json.JsonObject`; each `with…` is a one-liner naming its option.
      Lombok is not needed and was not added.
- [x] `FroalaEditor(FroalaOptions)` plus `setOptions` in three overloads —
      `FroalaOptions`, `JsonObject`, `String` (CFG-8). **No `setOption(String,
      Object)`** — the raw overloads are the escape hatch (CFG-9), and they are also
      the answer to the `initialConfig` / `rawInitialConfig` split carried over from
      `hugerte` and `vaadin-fullcalendar` (CFG-10). The dead `rawInitialConfig` field
      is gone, `initialConfig` became the `options` property
- [x] Options are **init-only**; `setOptions` on an attached editor rebuilds
      (CFG-1..CFG-4). No batching machinery was needed after all: Flow already sends
      one property update per round trip however often it was set, and the client
      compares the options it built with against the ones it was given, so a
      re-attach re-sending the same property does not rebuild for nothing
- [x] Our setters are applied after the options and win (CFG-11).
      `setValueChangeTimeout` stays a setter (CFG-12) — and its client-side default
      became null, so a `typingTimer` from the options is not overwritten by a
      default nobody asked for
- [ ] **Options still to type.** 322 exist; the first cut types 21 of them plus the
      `FroalaPlugin` enum. What the raw overload is still needed for: the grouped
      toolbar form, the `MD`/`SM`/`XS` breakpoints, HTML sanitization
      (`htmlAllowedTags` and its eight neighbours), paste handling, and the
      per-plugin button lists
- [x] Constructor no longer writes the three value-change defaults — removed
      2026-08-27, they restated the client's own defaults (CFG-13)
- [ ] A `vaadin` Froala theme, **active by default**, Lumo-mapped through our own
      custom properties (THM-1, THM-5, THM-7). Vaadin's `HasThemeVariant` was
      considered and dropped — Froala cannot switch a theme at runtime (THM-2)
- [x] **Toolbar composition — plain strings, not an enum, for now.**
      Decided 2026-08-27. A toolbar entry is only a command name, and a custom button
      (`RegisterCommand`) is a name we do not know in advance, so a string is what
      the API has to carry either way. Narrowing to an enum later is a restriction
      of the accepted values, not a change of shape.

      **Check 1, measured 2026-08-27: group names are free strings.** Froala's own
      default already uses `versionControl`, `exportImport` and `collab` next to the
      four `more…` names. Any key with a `buttons` array becomes a button group.

      One catch, and it decides how the API has to document this: **the group name
      is also the command name of the group's overflow button.** When a group holds
      more buttons than its `buttonsVisible`, Froala builds a collapsed `more`
      panel for the rest and then looks for `FroalaEditor.COMMANDS[groupName]` to
      render the button that opens it. With `moreText` it finds one; with
      `myTextGroup` it finds nothing, and the overflow buttons are in the DOM with
      no way to reach them. Registering a command under the group's name brings the
      toggle back — checked both ways.

      Also measured: overflow only exists in the **object** form. Both array forms
      set `showMoreButtons` to false internally and always render every button,
      naming the groups `group1`, `group2`, … An unknown button name is dropped
      silently, and it still counts towards `buttonsVisible`.

      **Typed 2026-08-27** as `FroalaToolbar` — `of(String…)` for the flat form,
      `ofGroups(FroalaToolbarGroup…)` for the grouped one — with
      `withToolbarButtonsMd/Sm/Xs` next to `withToolbarButtons`. Two more things
      measured while typing it: in the **flat** form `|` and `-` are consumed as
      group breaks and never drawn, and the three narrower-screen options do **not**
      cascade — each falls back to `toolbarButtons` alone, never to the next one up.

      Froala accepts three forms, all of which the API has to reach
      ([docs](https://froala.com/wysiwyg-editor/docs/options/)): a flat list of
      buttons; a flat list with `|` and `-` separators; or named groups, each with
      `buttons` plus optional `align` (`left`/`right`) and `buttonsVisible` (how many
      show before the "more" arrow). `align` and `buttonsVisible` belong to the
      **group**, not to a button. Plus `toolbarButtonsMD` / `SM` / `XS` — the same
      structure again for ≥992px, ≥768px and below. 183 commands are registered in
      5.4.0, which is the set a button name can come from.

      A button whose plugin is disabled is dropped silently by Froala. Not our
      problem to catch — bad input, bad output.
- [x] Editing modes: **inline** (`toolbarInline`) and **document**
      (`documentReady`) are plain options and need nothing beyond the options API.
      **Full-screen is a method, not an option** — and gets **no Java API**, decided
      2026-08-27. The toolbar button is the way to it.
- [x] Demo view exercising each mode — both modes are switches in `/options`
- [ ] Expose `pluginsEnabled`. **On by default: the 41 plugins that need nothing
      but a browser, or only the upload endpoint of phase 3. Off by default: the
      eight that need a server or a paid service** — `collaborative`, `ai_assist`,
      `filestack`, `spell_checker`, `import_from_word`, `export_to_word`, `save`.
      Decided 2026-08-27; a visible button with no service behind it is worse than
      no button. Which of the eight the customer actually needs is the plugin
      question in `docs/customer-request.md`, still open.

      Note the two name spaces measured under check 3: `pluginsEnabled` takes the
      name a plugin registers itself under (`fontFamily`), the file is called
      something else (`font_family.min.js`). The five service plugins above are
      named by file here.
- [ ] **Where does an upload go when no URL is set?** **Check 2, measured
      2026-08-27: nowhere.** `imageUploadURL`, `fileUploadURL` and `videoUploadURL`
      all default to `null`, and Froala treats null — and its own demo endpoint
      `https://i.froala.com/upload` — as "do not upload": it reads the file with a
      `FileReader` and inserts a `blob:` URL instead. No request leaves the browser.
      Counter-checked by setting a URL, which does produce the POST.

      The counter-check is in the demo as case 3 of `/check-upload`: a
      `FroalaUploadController` (`demo/.../rest/`) takes the multipart POST under the
      parameter name `file` — the default for image, file and video alike — stores
      the bytes and answers `{"link": "/froala-upload/<id>"}`, which it then serves
      the file under. Image and file both round-trip, and the link that reaches the
      server as part of the value survives a reload. That is also the shape phase 3
      has to turn into something the add-on offers.

      One rule that carries over to phase 3: the content type and file name the
      browser sees on the way back are decided **server side**, from the bytes, and
      never taken from the upload. The endpoint answers on the application's own
      origin, so handing an uploaded file back inline under a type the uploader
      chose is stored cross-site scripting. The demo controller serves the image
      types it recognises inline and everything else as a download, with
      `X-Content-Type-Options: nosniff`.

      That closes the data-protection question for uploads, and opens a different
      one. A `blob:` URL lives only in the tab that created it. The editor sends
      that URL to the server as part of its value, the server stores it, and after
      a reload the image is gone. So uploads staying on by default is fine for the
      network but produces broken documents; the honest default is to keep the
      upload buttons off until a URL is configured, and to say so.

      Two defaults **do** reach other people's servers, neither of them an upload:
      - `imageManagerLoadURL` defaults to `https://i.froala.com/load-files`. The
        image manager sends a GET there as soon as it opens. It is not reachable out
        of the box — `imageManager` is not in the default `imageInsertButtons` — but
        adding that one button starts talking to Froala's server.
      - `emoticonsUseImage` defaults to `true`, so the emoji picker loads its icons
        from `cdnjs.cloudflare.com`, and every inserted emoji keeps a cdnjs URL in
        the stored HTML. The `emoticons` button **is** in the default toolbar.
        `emoticonsUseImage: false` inserts the plain character instead.
- [x] `typingTimer` is exposed in `FroalaOptions` as **deprecated on arrival**,
      pointing at `setValueChangeTimeout`. Possible since the constructor stopped
      writing the defaults (CFG-13). Anyone who prefers the option can use it and
      leave the setter alone.
- [x] **Callback options are out of reach from Java** (CFG-18). Of Froala's 322
      typed options exactly one takes a function, `aiAssistRequest`; the other
      callback surface is `events`, itself an option, holding 159 handlers. All
      three `setOptions` overloads end as JSON and JSON has no functions, so
      `setOptions` rejects `events` rather than letting it arrive as data Froala
      would then try to call. Parked for phase 4, with `vaadin-fullcalendar`'s
      `JsCallback` as the shape to copy.
- [ ] `saveInterval: 0` turns off Froala's `save` plugin, which today schedules a
      POST to `saveURL` 10 s after every `contentChanged` and then fails on the
      missing URL. Dead work per edit rather than a defect. Settable now
      (`withSaveInterval`); what is still open is whether the add-on should default
      it to 0 rather than leave Froala's 10
- [ ] Decide which plugins ship at all — it sets the size of the theme's coverage
      (THM-10) and of the option surface
- [ ] **Load plugins and language files on demand instead of shipping one bundle.**
      **Check 3, measured 2026-08-27: it works.** Maintainer's idea. Today the
      connector imports `froala_editor.pkgd.min.js` — core plus all 49 plugins,
      1956 KB — and no language file at all.

      Instead: import the core statically and pull each plugin and the one needed
      language file with a dynamic `import()`. Everything stays shipped, only what a
      configuration asks for is downloaded. Measured in `demo/` with the spike
      element and a real `-Pproduction` build, all three verification points pass:

      - **The plugin files register themselves into the core we import.** They are
        UMD, and their CommonJS branch does `require('froala-editor')`, which
        resolves to `js/froala_editor.min.js` — the same module. No
        `window.FroalaEditor` global is needed. Measured: `FroalaEditor.PLUGINS`
        went from 0 to 8 entries, `FroalaEditor.LANGUAGE` from empty to `de`.
      - **Awaiting the imports before constructing the editor is early enough.**
        Froala reads its plugin registry once, in the constructor, and the loaded
        plugins' buttons were in the toolbar afterwards. 64 ms for eight plugins
        plus a language file, from cache-cold chunks.
      - **The production build emits one chunk per file.** 49 plugin chunks and 39
        language chunks under `VAADIN/build/`, none folded into the main bundle, and
        the browser fetched exactly the eight selected plus `de`.

      Two things this turned up that the API has to handle:
      - **A plugin's file name is not the name `pluginsEnabled` wants.**
        `font_family.min.js` registers as `fontFamily`, `find_and_replace` as
        `findReplace`, `cryptojs` as `cryptoJSPlugin`, `trim_video` as
        `trimVideoPlugin` — and `track_changes` keeps its underscore, alone among
        the 49. `edit_in_popup` registers no plugin at all. So the Java side needs
        both names per plugin, not a string that is converted.
      - **Two Froala copies on one page do not share a registry.** While the spike
        ran next to the add-on's own editor, the spike's core reported zero plugins
        although the packaged bundle had them all. That is expected — different
        modules — but it means the switch has to be complete: core plus dynamic
        imports, or the packaged bundle, never both.

      `Page.addJavaScript(url)` is **not** the mechanism. It needs a served URL, and
      `node_modules` is build input, not a served directory — using it would mean
      copying the 39 language files (1.4 MB) into `META-INF/resources/` and keeping
      that copy in step with every Froala upgrade. It is also not needed: a static
      map of `() => import('froala-editor/js/plugins/<name>.min.js')` literals is
      what makes Rollup emit the chunks. A variable inside the specifier does not
      work — Rollup cannot follow one through a bare package id.

      Sizes, for the decision: core 545 KB against the packaged 2002 KB, all 49
      plugins 1549 KB, all 39 language files 1345 KB, core CSS 54 KB against the
      packaged 301 KB. The demo's main bundle is 2.67 MB today, most of it the
      packaged Froala.

- [ ] **Language files are not loaded at all today.** Froala's `language` option
      only takes effect when the matching file from `js/languages/` has been loaded;
      39 files, 1.4 MB in total. Setting the language today changes nothing and says
      nothing — the editor stays English. Covered by the item above: the spike loaded
      `de` on demand and Froala picked it up.

Re-measure the option count before the typed API freezes: 322 in 5.4.0, 302 in
5.3.1, so the surface moves between minors. 17 options that exist in the bundle are
**missing from `index.d.ts`** (`toolbarResponsiveToEditor`, `imageUploadToAzure`,
`filesInsertButtons`, `keepTextFormatOnTable`, …), so the d.ts is not a complete
inventory on its own.

---

## Phase 3 — Spring integration (license key + upload)

- [ ] `@ConfigurationProperties("vaadin.froala")` binding — `license-key` at
      minimum — in `demo/`, or a new optional `vcf-froala-editor-spring` module
      (decide; a `-spring` module is the reusable answer, the demo the cheap one)
- [ ] Server-side image/file upload endpoint + `setImageUploadURL` wiring
- [ ] Document how a consuming app supplies the key via `application.properties`

---

## Phase 4 — Feature coverage

- [ ] **Server-set callbacks.** Not phase 2, parked 2026-08-27. Two of Froala's
      options take functions, and neither can travel as JSON: `events`, a map of 159
      handlers, and `aiAssistRequest`. `setOptions` rejects `events` today (CFG-18).

      When one of them is actually needed — `aiAssistRequest` is the realistic case,
      not `events` — **take the shape from `vaadin-fullcalendar`, which already has
      it as `JsCallback`** (maintainer's pointer). Related prior art for the
      connector is already in the notes for its lifecycle handling.

      `events` stays a separate question: Froala's events want server-side listeners
      next to `addValueChangeListener`, not an option carrying a function.
- [ ] **Custom toolbar buttons.** Not phase 2, decided 2026-08-27. Froala's
      `RegisterCommand(name, {...})` takes a title, an icon, and a `callback` that
      would have to reach a server-side listener — plus `undo`, `focus`, `toggle`,
      `showOnMobile`, `refreshAfterCallback`. It is also **static**: a registered
      command exists for every editor on the page, not per instance.
      Phase 2's only obligation is not to make this harder: the toolbar API carries
      strings, so a name Froala does not know is already expressible.

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
- [ ] Lumo integration and dark mode. The host already pulls Lumo's
      `inputFieldShared` and renders Vaadin's field parts, but **Froala's own chrome
      is unstyled by us** — toolbar, popups, dropdowns and the editing surface come
      with Froala's stock CSS and ignore Lumo's tokens, so they do not follow the
      Vaadin theme and stay light when the app is dark. Needs: Froala's colors,
      radii, spacing and fonts mapped onto `--lumo-*`, and the theme in effect
      passed to Froala (it has its own `theme` option and a `dark` variant). Note
      the commented `ThemeDetectionMixin` import in the connector — that is the
      Vaadin side of detecting the active theme, kept as the marker for this item.
      Froala's chrome lives in the light DOM, which the add-on's own `@CssImport`
      stylesheet already reaches, so this is CSS work in a file that exists rather
      than a styling-boundary problem.
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
- [x] **4.6.2 is not a target.** The customer request names it, but the decision is
      taken and not up for re-litigation: this add-on builds against 5.4.0 only.
      Supporting both majors would mean a version switch in the connector or separate
      branches; neither is planned.
- [ ] Keep the connector's Froala-specific surface behind one file so a major swap
      stays localized

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

## Phase 7 — Vaadin 25

Not a port, a **decision**: does the add-on move to 25, or serve both lines? Java 17
and Vaadin 24 are the current floor on purpose — they are the platform's own floor, so
the published add-on excludes as few consumers as possible (see `CLAUDE.md`). Vaadin 25
needs Java 21 and Spring Boot 4, so moving means dropping consumers, and serving both
means two maintained lines (e.g. 1.x for 24, 2.x for 25). Whether NST needs 25 at all
is one of the open questions to them in `docs/customer-request.md` — that answer comes
first.

- [ ] Decide: raise the floor, or two lines. Everything below follows from it.
- [ ] Test stack moves with the version, and not one piece at a time:
      - **Karibu is version-locked** — 2.4.x is Vaadin 24.8+, 2.6.x is Vaadin 25 only,
        no line spans both. A Vaadin bump is a Karibu bump.
      - **Vaadin's own browserless layer is free on 25.1+** (it needs a commercial
        TestBench license on 24), so on 25 it becomes the natural replacement for
        Karibu rather than an extra cost.
      - **DramaFinder becomes usable.** It was ruled out for phase 1 only because 1.x
        is built against Vaadin 25 / JUnit 6 / Java 21 (see the Testing section of
        `CLAUDE.md`); on 25 that objection is gone and plain Playwright can be
        reconsidered.
      - JUnit 5 → 6 comes with Spring Boot 4.
- [ ] Re-check the value transfer against Flow 25. It rests on Flow behaviour, not on
      Flow's public contract: property de-duplication on both sides, detach listeners
      firing before the node's parent is cleared, and a `beforeClientResponse` task of
      a detached node being deferred rather than dropped. All three are covered by
      `FroalaEditorKaribuTest` and VT-11 — run those first, they are the canary.
- [ ] Signals. `CLAUDE.md` bans them because they are a Vaadin 25 feature; on 25 that
      ban should be re-read rather than silently dropped. Classic state is not wrong,
      so this is a deliberate choice, not an automatic migration.
- [ ] Vaadin 25 dropped some Flow API that 24 deprecated — walk the deprecation
      warnings of a 25 build before assuming the wrapper compiles unchanged.

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

### Froala's quick-insert popup is hidden under `AppLayout` (2026-08-24)

Froala's inline action popup — the `+` that appears at the start of an empty line —
does not work when the editor sits inside a Vaadin `AppLayout`. The **drawer appears
to hide it**. With the drawer hidden, Froala computes the position correctly, so this
is a positioning/stacking problem, not a broken popup.

Reported by the maintainer against the demo, whose `MainLayout` is an `AppLayout` with
a `DrawerToggle`.

What is known about the element: it carries the CSS class **`fr-quick-insert`** and,
once created, sits **below `.fr-box`**. Reaching it is not the problem — the add-on
already ships a light-DOM stylesheet (`vcf-froala-editor.css`, `@CssImport` without
`themeFor`, which lands in the global scope) and that file already styles `.fr-box`
and `.fr-wrapper` the same way.

**Not fixable in Froala** — it is a dependency, not our code, so whatever we do has to
work from the outside. Two outcomes, decision open:

- [ ] Find our own answer — most likely CSS in the stylesheet we already ship
      (stacking context, `z-index`, `position`), otherwise repositioning or
      re-parenting `.fr-quick-insert`. Whatever it is, it has to keep working when the
      drawer opens and closes and when the editor is inside an overlay (see the
      `/overlay` demo view).
- [ ] Or declare it a **known limitation** and document it in the README, with the
      workaround of not using the quick-insert plugin under `AppLayout`.

Either way this needs an e2e test against a fixture view that puts the editor in an
`AppLayout` — there is none today, every fixture view is a plain `VerticalLayout`.

---

## Open questions

Questions **for the customer** live in `docs/customer-request.md`, next to the request
that raised them. Do not duplicate them here, and do not enumerate them here either —
the maintainer edits that list.

What is left below is ours to answer.

- Nothing at the moment. The license question is closed: the add-on is Apache-2.0
  under "Vaadin Ltd.", confirmed by the maintainer 2026-08-27, and `LICENSE` plus a
  `<licenses>` block in both poms were added then. Most VCF add-ons carry no owner
  in the per-file header, so `eclipse/license-header.txt` stays as it is.
