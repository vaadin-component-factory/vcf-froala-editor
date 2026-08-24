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
  *Verified:* `FroalaEditorIT.detachAndReattach_keepsTheValueAndKeepsWorking` covers
  the re-attach path, which uses the same code. A `setValue()` **before** the first
  attach is **unverified** — the demo starts with an empty editor and Karibu runs no
  JavaScript.
- **LC-3** Nothing may touch a Froala module (`edit`, `html`, …) before Froala's
  `initialized` event. `this.editor` being assigned is not sufficient — Froala builds
  asynchronously. The connector tracks this in `_editorInitialized` and re-applies
  the pending state from the event handler.
  *Verified:* `FroalaEditorIT.disabled_stopsEditing` exercises the re-apply path for
  a value set before init.
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
- **LC-9** On re-attach the editor is re-created from `connectedCallback`, because
  Lit runs `firstUpdated` only once per element.
  *Verified:* same test as LC-8.

## Undecided

- **Whether to destroy on detach at all.** LC-6 is one of two defensible answers.
  The maintainer's own `stefanuebe/vaadin-fullcalendar` (`v6_master`) does the
  opposite deliberately: the widget survives the detach and `connectedCallback` falls
  through a guard. Destroy-and-rebuild is the expensive path under Flow/Lit attach
  churn and it throws away undo stack and caret; keeping the instance holds DOM
  references when the detach turns out to be final. Open in `ROADMAP.md`, phase 1
  deferred list. LC-4 and LC-6 already handle the *correctness* half either way.
- **Lost on every detach, whichever way that goes:** Froala's undo/redo stack,
  caret and selection, scroll position. Nothing preserves them today and no
  requirement asks for it — worth naming so it is a decision, not a surprise.
