# Value change modes

*When* a client-side edit is reported to the server. The transport itself is
[value transfer](phase-1-value-transfer.md).

`com.vaadin.componentfactory.froala.ValueChangeMode` is the add-on's **own** enum,
deliberately not Flow's `com.vaadin.flow.data.value.ValueChangeMode`: the trigger is
a Froala event, not a DOM `input` event, so the set of modes and their client-side
names are ours. The name clash is accepted and documented in the javadoc.

## Modes

- **VCM-1** `ON_CHANGE` (client: `change`) — syncs on Froala's `contentChanged`
  event. Froala fires it per word, line or structural edit, not per keystroke.
  **This is the default.**
  *Verified:* `FroalaEditorIT.typedText_reachesTheServer`,
  `FroalaEditorKaribuTest.valueChangeMode_roundTripsAndDefaults` (default).
- **VCM-2** `ON_BLUR` (client: `blur`) — syncs only when the editor loses focus.
  While the editor has focus, no edit reaches the server.
  *Verified:* `FroalaEditorIT.onBlurMode_syncsOnlyWhenFocusLeaves`, which asserts
  both halves: nothing before the blur, the full text after it.
- **VCM-3** `TIMEOUT` (client: `timeout`) — a **debounce**: every `contentChanged`
  restarts a timer of `valueChangeTimeout` ms, and the sync happens when the user
  pauses. Equivalent to Flow's `LAZY`.
  *Verified:* **unverified** — needs Playwright's `page.clock()` so the test does not
  wait on the wall clock.
- **VCM-4** `INTERVAL` (client: `interval`) — syncs every `valueChangeTimeout` ms
  regardless of user activity, as long as there is something to sync (VT-2). Started
  from Froala's `initialized` event as well as on a mode switch, so it also runs for
  an editor that is created in this mode.
  *Verified:* **unverified** — same reason as VCM-3.
- **VCM-5** Switching *away* from `TIMEOUT` or `INTERVAL` flushes the pending value
  first, so a mode change never swallows an edit.
  *Verified:* unverified.
- **VCM-6** Setting the mode to `null` server-side resets it to `ON_CHANGE` rather
  than throwing.
  *Verified:* `FroalaEditorKaribuTest.valueChangeMode_roundTripsAndDefaults`.

## Timeout

- **VCM-7** `setValueChangeTimeout(int)` is the timespan for both `TIMEOUT` (idle
  time before the sync) and `INTERVAL` (time between syncs, and before the first
  one). Default **2000 ms**.
  *Verified:* `FroalaEditorKaribuTest.valueChangeMode_roundTripsAndDefaults`.
- **VCM-8** The timeout must be greater than zero. Zero and negative values are
  rejected with `IllegalArgumentException`, on the server and in the client's own
  setter, with the same message.
  *Verified:* `FroalaEditorKaribuTest.valueChangeTimeout_rejectsAnythingButPositiveValues`.

## Throttle

- **VCM-9** Independently of the mode, the connector never syncs more often than
  every **50 ms**.
  *Verified:* `FroalaEditorIT.syncInsideTheThrottleWindow_isDeferredNotDropped`.
- **VCM-10** A sync that falls inside the throttle window is **deferred, not
  dropped** — it is rescheduled for the end of the window. Dropping is safe for a
  keystroke, because a later one carries the delta, but a blur or a timeout flush has
  no successor, so dropping loses the user's last edit.
  *Verified:* `FroalaEditorIT.syncInsideTheThrottleWindow_isDeferredNotDropped` (calls
  the real `onValueChange` twice inside the window and asserts both arrive) and
  `fastTyping_thenBlur_losesNothing`.
- **VCM-11** The 50 ms window is a constant, not configurable. No requirement asks
  for it to be.

## Known gaps

- VCM-3 and VCM-4, the two modes with timing behaviour, are the two without a test.
