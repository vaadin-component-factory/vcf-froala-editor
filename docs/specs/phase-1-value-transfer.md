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
  through `editor.html.set()`. One case cannot use the property and writes it from a JS
  call instead — see VT-11.
  *Verified:* `FroalaEditorKaribuTest.setValue_reachesTheClientProperty` for the
  property, `FroalaEditorIT.setValue_producesNoDeltaNobodyTyped` for `html.set` — it
  pushes deliberately messy markup, asserts the text arrives, and asserts that Froala's
  rewrite of it does **not** come back as a delta describing a change nobody made.
  `html.set` fires no `contentChanged` of its own (VCM-16), and nothing else may either.
- **VT-6** A client-originated change updates the **model** value only, never the
  presentation value. Without this, every keystroke would echo the full document
  back to the browser and defeat VT-1.
  *Verified:* `FroalaEditorIT.typing_neverPushesTheFullValueBackToTheClient` counts
  writes to the element's `value` property from inside the browser and asserts zero
  across a sentence's worth of typing, while still asserting the round trip happened.
  This is the guard for the requirement the whole delta design exists for.
- **VT-7** The `value` property lags behind the editor for as long as the component
  is attached (that is VT-6). It is brought in step exactly once, in
  `addDetachListener` → `setPresentationValue`, which is enough: Flow replays a node's
  properties when it is attached again, and that is what seeds the rebuilt editor
  (LC-2).
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
- **VT-10** A resync **clears the pending throttle timer**, so the flushed full value
  is not followed by a delta nobody asked for. It also sends what the editor holds
  *now*, not the last synced value — a sync still sitting in the throttle has no other
  way to arrive, because the resync just cancelled it.
  *Verified:* `FroalaEditorIT.resyncWhileASyncIsPending_carriesWhatTheEditorHoldsNow`,
  which synthesizes the whole state down to the `resyncValue()` call: the window is
  50 ms wide and a server round trip is slower than that, so the real path never reaches
  the client while a sync is still pending. Both halves fail on their own — dropping the
  `html.get()` resyncs the drifted base, and dropping the `clearTimeout` lets the orphan
  timer answer a following server-side `setValue` with a delta describing the server's
  own push. That last one is only visible because `html.set` fires no `contentChanged`
  of its own (VCM-16), so nothing else could have sent it.
- **VT-11** `setValue()` reaches the editor **even when it repeats a value the server
  set before**. That single case cannot travel through the property: VT-6 leaves the
  property on the value the server set last, Flow drops a write whose value the
  property already holds, and the browser — whose own copy of the state tree is stale
  for the same reason — would ignore the update even if it were sent.
  `setPresentationValue` detects exactly this case and writes the property from a JS
  call instead, which carries no such comparison:

  ```java
  boolean clientNeedsExplicitPush = liveOnClient
          && Objects.equals(newPresentationValue, getElement().getProperty("value"));
  ...
  getElement().executeJs("this.value = $0", newPresentationValue);
  ```

  The push deliberately goes through the connector's own `value` setter rather than a
  method of its own, so a reader of the connector sees one entry point for a server
  value, not two.

  The comparison is exhaustive because the server-side property and the browser's copy
  of the state tree move in lockstep — both change only on a server push, neither is
  touched by a client edit. So "equals the property" is the same statement as "the
  browser would drop it".

  `liveOnClient` is an own flag rather than `isAttached()`, which cannot express this:
  **measured — `isAttached()` answers `true` inside a detach listener**, because Flow
  fires those from `StateNode.setParent` *before* it clears the node's parent, so the
  node is still reachable from the tree. And the push VT-7's detach-time call would then
  ask for is **not** dropped: Flow defers it to the next attach, where it arrives after
  the property replay and overwrites whatever the server set while the component was
  away. Measured with `isAttached()` in place — the deferred call carries the value from
  the detach moment, not the current one.
  *Verified:* `FroalaEditorKaribuTest.detach_doesNotQueueAValuePushForTheNextAttach`,
  which fails with `isAttached()` and passes with the flag, and
  `setValueRepeatingTheLastServerValue_queuesAnExplicitClientPush`, so the guard is not
  vacuously satisfied.

  The flag flips in `beforeClientResponse`, not directly on attach, so that other attach
  listeners still see a component the browser does not know about yet. That is the
  reason `hugerte` states in its own code — *"we do this in before client response to
  allow other attach listeners to do their configs as well"* — where it protects the
  `checkAlreadyInitialized()` config guard.

  For the push guard itself the timing is unobservable, in both directions: the guard
  needs the property to lag behind the model, which only a client-originated
  `setModelValue` produces, and VT-7 restores equality on every detach — so right after
  an attach the two are equal and the first `setValue` cannot satisfy it either way.
  Measured: `add` followed by `setValue` queues no push. The timing is adopted anyway,
  because phase 2's configuration API is expected to need exactly the guard `hugerte`
  built it for, and a flag that flips too early is not something to discover then.
  *Verified:*
  `FroalaEditorIT.setValue_reachesTheClientEvenWhenItRepeatsTheLastServerValue`,
  which types on top of the seeded value and then re-sets exactly that seeded value.
  Reported as maintainer finding 6; same defect and same fix as
  parttio/hugerte-for-flow#30 (closed).

## Known gaps

- **Froala normalizes HTML.** After a programmatic `setValue`, `editor.html.get()`
  may not return byte-identical HTML to what was set, which would make the following
  change event carry a delta the user never typed. Whether this actually happens in
  5.4.0 has **not** been measured. If it does, VT-6 needs a suppression window
  around `html.set`.
- No test covers a `setValue()` of a *large* document. VT-6 now guards the direction
  that matters (no full value per keystroke), but nothing measures actual payload size,
  so a size regression in the delta itself would go unnoticed.
