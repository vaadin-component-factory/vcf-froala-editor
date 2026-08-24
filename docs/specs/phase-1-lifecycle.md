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
  *Verified:* `FroalaEditorIT.initialValue_isInTheEditorOnLoad` — `BasicView` sets its
  value before the first attach, and the assertion is scoped to Froala's own editing
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
  *Verified:* unverified as a direct assertion. Reproducing it needs a detach inside
  the same frame as the attach.
- **LC-5** Creating the editor is idempotent: `_initEditor` only builds when
  `this.editor` is unset, so `firstUpdated` and `connectedCallback` cannot produce
  two instances.
  *Verified:* unverified as a direct assertion; a second instance would break
  `detachAndReattach_keepsTheValueAndKeepsWorking`.

## Detach and re-attach

- **LC-6** On detach the editor is destroyed (`editor.destroy()`), its host `<div>`
  removed, and all three timer handles cleared — interval, timeout and throttle.
  Every one of them outlives the element otherwise: in `INTERVAL` mode the interval
  keeps firing against a destroyed editor, and each re-attach starts another on top.
  *Verified:* the clean-up is verified only indirectly, through
  `detachAndReattach_keepsTheValueAndKeepsWorking`. A leaked interval is hard to
  observe from outside, because its events come from an element no longer in the tree.
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
  editor is built by `firstUpdated` on a fresh element, and its value comes from the
  presentation-value push of LC-6 / VT-7 — nothing survives on the client.
  *Verified:* by the measurement above; the probe was temporary and is not in the
  suite. LC-8's test covers the observable outcome.
- **LC-10** `connectedCallback`'s `hasUpdated` branch therefore covers a different
  case: a **client-side DOM move** of an element that has already rendered, where Lit
  does not run `firstUpdated` again and nothing else would rebuild the editor.
  *Verified:* **unverified** — whether any Vaadin 24 component moves a field's
  element that way has not been established. The branch is kept because it is two
  lines and the failure mode without it is a permanently empty editor.

## Decided, with a cost

- **LC-6 stands: destroy on detach.** Decided 2026-08-24 on the strength of LC-9.
  The alternative below cannot pay off, because a Flow detach discards the element a
  surviving instance would have to live on. Re-parenting into a `Dialog` or `Popover`
  is such a detach plus attach, so it rebuilds the editor too.
  **Accepted cost:** every detach loses Froala's undo/redo stack, caret, selection and
  scroll position. The value survives (VT-7). Nothing preserves the rest and no
  requirement asks for it — named here so it is a known cost, not a surprise.

  The original reasoning, kept because it is the argument to revisit if a concrete
  case appears:
- **Whether to destroy on detach at all.** LC-6 is one of two defensible answers.
  The maintainer's own `stefanuebe/vaadin-fullcalendar` (`v6_master`) does the
  opposite deliberately: the widget survives the detach and `connectedCallback` falls
  through a guard.
  **LC-9 narrows what that can buy here.** Since a Flow detach discards the element
  itself, a surviving widget instance survives with it — keeping it alive helps only
  the client-side move of LC-10, never a Flow remove/add. Weigh the decision on that
  basis, not on the assumption that Flow reconnects the same element. Destroy-and-rebuild is the expensive path under Flow/Lit attach
  churn and it throws away undo stack and caret; keeping the instance holds DOM
  references when the detach turns out to be final. Open in `ROADMAP.md`, phase 1
  deferred list. LC-4 and LC-6 already handle the *correctness* half either way.
