# Lifecycle

When the Froala instance is created and destroyed, and what that means for every
setter. This is the area where Flow and Lit interact badly if left alone, so the
guarantees here are mostly about *not* breaking.

## Initialization

- **LC-1** The Froala instance is created from Lit's `firstUpdated`, **not** from
  `connectedCallback`. By `firstUpdated`, properties the server set in the same
  response — `licenseKey`, `value` — are already applied, which matters because
  Froala reads its options exactly once, at construction.
  *Verified:* `FroalaEditorIT.editorRendersWithToolbar` (the editor exists at all),
  `labelAndHelperText_areRendered`.
- **LC-2** The initial value is applied by seeding the host element's `innerHTML`
  before constructing Froala, which adopts the content of the element it initializes
  on. Froala has no init option for content.
  *Verified:* `FroalaEditorIT.initialValue_isInTheEditorOnLoad` — `FroalaTestView` sets
  its value before the first attach, and the assertion is scoped to Froala's own editing
  surface, so the value can only have arrived through the seeding.
  `detachAndReattach_keepsTheValueAndKeepsWorking` covers the re-attach path through
  the same code.
- **LC-3** Nothing may touch a Froala module (`edit`, `html`, …) before Froala's
  `initialized` event. `this.editor` being assigned is not sufficient — Froala builds
  asynchronously. The connector tracks this in `_editorInitialized` and re-applies
  the pending state from the event handler.
  *Verified:* `FroalaDisabledAtInitIT` — both tests, against a fixture view whose
  editors are disabled resp. read-only before they are ever attached, so the state can
  only have been applied from Froala's `initialized` handler. `disabled_stopsEditing`
  was cited here before and does not prove it: it toggles long after `initialized` has
  fired and therefore exercises the ordinary `updated()` path.
- **LC-4** `_initEditor` returns without doing anything when the host is no longer
  connected. Lit does **not** check `isConnected` before running `firstUpdated`, and
  Flow can attach and detach an element before that first update flushes — without
  the guard a live Froala instance would be left on a host that already had its one
  and only `disconnectedCallback`, with nothing to destroy it.
  *Verified:* `FroalaEditorIT.initEditorOnAHostThatIsAlreadyDetached_buildsNothing`,
  against an element that was attached and removed inside one task, so it is that host
  and not merely a loose element. One measurement came out of writing it: Lit does
  complete an update on such an element — `hasUpdated` turns true — but **no editor is
  built through that path even with the guard removed**, so something upstream already
  covers the Lit route. The guard is the second line of defence and is asserted where it
  does bite, on the call itself: without it, `_initEditor` on that host builds a Froala
  instance nobody will ever destroy.
- **LC-5** Creating the editor is idempotent: `_initEditor` only builds when
  `this.editor` is unset, so `firstUpdated` and `connectedCallback` cannot produce
  two instances.
  *Verified:* `FroalaEditorIT.buildingTheEditorASecondTime_changesNothing` — the same
  instance and a single editable surface after a second `_initEditor()`. Without the
  guard the whole view comes apart, which is the point: the first instance stays live
  and unreachable, still registered and never destroyed.

## Detach and re-attach

- **LC-6** On detach the editor is destroyed (`editor.destroy()`), its host `<div>`
  removed, and all three timer handles cleared. `destroy()` is **not** optional:
  Froala registers every instance in a global `FroalaEditor.INSTANCES` array from
  `_init` and removes it only from `destroy()`, and other live editors iterate that
  array from global mouse, resize and drag handlers. Verified against the installed
  5.4.0 bundle; there is no `MutationObserver` on the editor's own element, so nothing
  self-cleans when the element is simply removed — interval, timeout and throttle.
  Every one of them outlives the element otherwise: in `INTERVAL` mode the interval
  keeps firing against a destroyed editor, and each re-attach starts another on top.
  *Verified:* `FroalaEditorIT.detachingInIntervalMode_leavesNoTimerBehind` for the
  interval, plus `detachAndReattach_keepsTheValueAndKeepsWorking` for the rest. The
  leak is invisible on its own — a tick on a detached element finds no editor, computes
  an empty delta and dispatches nothing — so the test keeps a reference to the element
  and gives the timer something to report before running the clock forward. Without the
  `clearInterval` it reports.
- **LC-7** `super.disconnectedCallback()` runs **before** the teardown, matching
  `FocusMixin`, `ControllerMixin` and Vaadin's `ResizeMixin`.
  *Verified:* by inspection.
- **LC-8** A detached and re-attached editor holds the value it had and is fully
  usable — the user can keep typing and the server keeps receiving changes.
  *Verified:* `FroalaEditorIT.detachAndReattach_keepsTheValueAndKeepsWorking`
  asserts both.
- **LC-9** A **Flow-driven** detach and re-attach produces a **new element**, not a
  reconnected one. Measured 2026-08-24: marking the element from the browser,
  toggling attachment and reading the marker back finds it gone. So the re-attached
  editor is built by `firstUpdated` on a fresh element, seeded from the `value`
  property the detach listener brought in step (VT-7) — nothing survives on the client.
  *Verified:* by the measurement above; the probe was temporary and is not in the
  suite. LC-8's test covers the observable outcome.
- **LC-10** `connectedCallback`'s `hasUpdated` branch therefore covers a different
  case: a **client-side DOM move** of an element that has already rendered, where Lit
  does not run `firstUpdated` again and nothing else would rebuild the editor.
  *Verified:* **parked 2026-08-25 — not relevant for now.** No Vaadin 24 component has
  been shown to move a field's element that way, so there is nothing to reproduce. The
  branch stays: two lines, and without it the failure mode is a permanently empty
  editor. Revisit only if such a move actually turns up.

## Decided

- **LC-6 stands: destroy on detach.** Decided 2026-08-24, and closed 2026-08-25 by the
  maintainer. Froala does not clean up after itself when its element is detached, and
  leaving an instance in its global registry means other editors keep reaching into it
  (see LC-6). The alternative could not pay off regardless, because a Flow detach
  discards the element a surviving instance would have to live on — re-parenting into a
  `Dialog` or `Popover` is such a detach plus attach.
  The purely client-side DOM move of LC-10 is handled, not broken: the element instance
  survives it, so `_lastSyncedValue` does too and the editor is re-seeded from it.

  **A detach therefore restores the value, and only the value** (VT-7). Undo/redo
  stack, caret, selection and scroll position start fresh. That is not a trade we made
  and could unmake: Flow owns the disconnect, the element is gone by the time we hear
  about it, and nothing tells us whether the component will ever come back — so there
  is no state we could responsibly hold on to. Moving an editor between a dialog and
  the page is the case where a user would notice, and it is an edge case; no
  requirement asks for more.

  The prior art, kept because it is the argument to revisit if a concrete case appears:
  the maintainer's own `stefanuebe/vaadin-fullcalendar` (`v6_master`) does the opposite
  deliberately — the widget survives the detach and `connectedCallback` falls through a
  guard. **LC-9 narrows what that can buy here.** Since a Flow detach discards the
  element itself, a surviving widget instance would have nothing to live on; keeping it
  alive helps only the client-side move of LC-10, never a Flow remove/add. Weigh any
  revisit on that basis, not on the assumption that Flow reconnects the same element.
