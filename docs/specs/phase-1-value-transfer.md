# Value transfer

How the editor's HTML gets from the browser to the server and back. The driving
requirement is the customer's: an NST document can be large, so a keystroke must not
put the whole document on the wire.

Implemented by `FroalaEditor.java` and `vcf-froala-editor.js` together — neither
half is meaningful alone. Ported from `parttio/hugerte-for-flow` (Apache-2.0).

## Client → server

- **VT-1** A client-side change is reported as a **delta**, not as the full value.
  The connector keeps the last value it synced (`_lastSyncedValue`), builds a
  diff-match-patch patch text against the current one and dispatches it as
  `_value-delta`.
  *Verified:* `FroalaEditorIT.typedText_reachesTheServer`.
- **VT-2** An empty delta is not sent. A change event that produces no textual
  difference reaches the server as nothing at all.
  *Verified:* implicitly by `onBlurMode_syncsOnlyWhenFocusLeaves`, which would see a
  spurious sync otherwise. No dedicated test.
- **VT-3** The server applies the delta with `FroalaEditor.applyDelta(old, delta)`
  and publishes the result with `setModelValue(value, true)`, so the resulting
  `ValueChangeEvent` reports `isFromClient() == true`.
  *Verified:* `FroalaEditorKaribuTest.valueChangeListener_firesOnServerSideChange`
  (server side), `FroalaEditorIT.typedText_reachesTheServer` (end to end).
- **VT-4** `applyDelta` is a pure function of `(oldValue, delta)` and public, so the
  delta format can be tested without a UI.
  *Verified:* `FroalaEditorDeltaTest` (3 tests).

## Server → client

- **VT-5** Server → client transfer is **full HTML, not a delta.** `setValue()` sets
  the element's `value` property; the connector's setter replaces the editor content
  through `editor.html.set()`.
  *Verified:* `FroalaEditorKaribuTest.setValue_reachesTheClientProperty` (property
  only — Karibu runs no JavaScript, so `html.set` itself is unverified).
- **VT-6** A client-originated change updates the **model** value only, never the
  presentation value. Without this, every keystroke would echo the full document
  back to the browser and defeat VT-1.
  *Verified:* unverified as a direct assertion. Covered indirectly: VT-7's test would
  fail if the presentation value were being written on every change.
- **VT-7** The full value is pushed to the client exactly once per detach
  (`addDetachListener` → `setPresentationValue`), so a re-attached editor starts from
  the server's value.
  *Verified:* `FroalaEditorIT.detachAndReattach_keepsTheValueAndKeepsWorking`.

## Drift

Both sides must agree on the base a delta was built against. They can stop agreeing —
a lost update, a programmatic `setValue` racing a keystroke, a bug.

- **VT-8** A delta that does not apply is **never** applied partially. `applyDelta`
  checks the per-patch flags diff-match-patch returns and throws
  `FroalaEditor.DeltaMismatchException` if any patch was skipped.
  This is the central deviation from the reference implementation: diff-match-patch
  hands back the *unpatched* string on failure (verified empirically against 1.2), so
  ignoring the flags loses the edit with no trace.
  *Verified:* `FroalaEditorDeltaTest.applyDelta_driftedBase_throwsInsteadOfLosingTheEdit`.
- **VT-9** On drift the **client** resends, never the server. The server answers the
  exception by calling `resyncValue()` on the element, which dispatches
  `_value-resync` with the full current value; the server adopts it via
  `setModelValue(value, true)`.
  Direction is deliberate: the client holds what the user typed, so pushing the
  server's stale value would discard their work.
  *Verified:* `FroalaEditorIT.driftedClient_recoversThroughResync`, which corrupts
  `_lastSyncedValue` in the browser and asserts recovery.
- **VT-10** A resync clears the pending throttle timer, so the flushed full value is
  not followed by a stale delta.
  *Verified:* unverified.

## Known gaps

- **Froala normalizes HTML.** After a programmatic `setValue`, `editor.html.get()`
  may not return byte-identical HTML to what was set, which would make the following
  change event carry a delta the user never typed. Whether this actually happens in
  5.4.0 has **not** been measured. If it does, VT-6 needs a suppression window
  around `html.set`.
- No test covers a `setValue()` of a *large* document, which is the requirement's
  actual motivation. Nothing measures the payload size the design is meant to avoid.
