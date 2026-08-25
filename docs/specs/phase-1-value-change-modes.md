# Value change modes

*When* a client-side edit is reported to the server. The transport itself is
[value transfer](phase-1-value-transfer.md).

`com.vaadin.componentfactory.froala.ValueChangeMode` is the add-on's **own** enum,
deliberately not Flow's `com.vaadin.flow.data.value.ValueChangeMode`: the trigger is
a Froala event, not a DOM `input` event, so the set of modes and their client-side
names are ours. The name clash is accepted and documented in the javadoc.

## Modes

- **VCM-1** `ON_CHANGE` (client: `change`) — syncs on Froala's `contentChanged`
  event, which is **not** per keystroke: Froala coalesces typing itself (see
  *Froala's own debounce* below) and only reports once the user pauses.
  **This is the default.**
  *Verified:* `FroalaEditorIT.typedText_reachesTheServer`,
  `FroalaEditorKaribuTest.valueChangeMode_roundTripsAndDefaults` (default).
- **VCM-2** `ON_BLUR` (client: `blur`) — syncs only when the editor loses focus.
  While the editor has focus, no edit reaches the server.
  *Verified:* `FroalaEditorIT.onBlurMode_syncsOnlyWhenFocusLeaves`, which asserts
  both halves: nothing before the blur, the full text after it.
- **VCM-3** `TIMEOUT` (client: `timeout`) — a **debounce**: every `contentChanged`
  restarts a timer of `valueChangeTimeout` ms, and the sync happens when the user
  pauses. Equivalent to Flow's `LAZY`. Because `contentChanged` is itself debounced,
  the idle time a user actually experiences is Froala's ~500 ms **plus**
  `valueChangeTimeout`.
  *Verified:* `FroalaEditorIT.timeoutMode_syncsOnceTheUserPauses_andEveryEditRestartsTheWait`,
  under Playwright's fake clock. Asserts both halves — nothing sent while the timer
  runs, and an edit inside the window restarting it rather than letting the running
  timer through.
- **VCM-4** `INTERVAL` (client: `interval`) — syncs every `valueChangeTimeout` ms
  regardless of user activity, as long as there is something to sync (VT-2). Started
  from Froala's `initialized` event as well as on a mode switch, so it also runs for
  an editor that is created in this mode.
  *Verified:* `FroalaEditorIT.intervalMode_syncsOnEveryTick_whileTheUserKeepsTyping` —
  two ticks, with the editor never losing focus and the user never pausing, which is
  what separates it from `TIMEOUT`.
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

## Flush on blur

- **VCM-9** **A blur flushes in every mode**, not only in `ON_BLUR`. Losing focus
  usually means a click elsewhere, and that click can detach the component — which
  clears the mode's pending timer and would take the last edit with it. An empty delta
  dispatches nothing (VT-2), so the modes that synced already pay nothing.
  *Verified:* `FroalaEditorIT.typingThenDetachingImmediately_keepsTheLastChange`
  and its `_inTimeoutMode` / `_inIntervalMode` variants — type, then straight to the
  detach toggle with no pause and no click elsewhere first. The two mode variants fail
  without this flush.
- **VCM-10** What a blur cannot save is a detach with no blur before it — another view
  removing the editor, a timer, a closing browser. By the time `disconnectedCallback`
  runs, the element is out of the DOM and its Flow node is detached server-side, so
  nothing dispatched there would arrive.

  This is out of reach rather than unfinished, and it matters less than it reads: an
  editor's content is working data until the application saves it, so a reload or a
  closed tab is the application's problem to cover and it has better means than a
  keystroke sync. What is left after that is a browser crash, which takes more with it
  than this ever could. No requirement asks for more.

## Throttle

- **VCM-11** `ON_CHANGE` never syncs more often than every **50 ms**. Typing rarely
  reaches that rate — Froala's own debounce already coalesces it — so what this really
  catches are the paths that bypass that debounce: a toolbar command fires
  `contentChanged` twice in a row, and paste, cut and undo/redo fire it at once.
- **VCM-12** The throttle belongs to `ON_CHANGE` alone, in `onValueChangeThrottled()`.
  `onValueChange()` itself does no rate limiting and always sends. `TIMEOUT` and
  `INTERVAL` limit their own rate already, and a flush — from a blur, a mode switch or
  an elapsed timer — must never be held back. Rate policy sits at the call site, the
  dispatcher only dispatches.
  *Verified:* `FroalaEditorIT.syncInsideTheThrottleWindow_isDeferredNotDropped` calls
  the throttled path twice inside the window and asserts both arrive.
- **VCM-13** A sync that falls inside the throttle window is **deferred, not dropped**
  — it is rescheduled for the end of the window. Dropping is safe for a keystroke,
  because a later one carries the delta, but the last keystroke of a burst has no
  successor. This is a deliberate deviation from the reference implementation, which
  drops (see `docs/upstream/hugerte-value-change-losses.md`).
  *Verified:* `syncInsideTheThrottleWindow_isDeferredNotDropped`,
  `fastTyping_thenBlur_losesNothing`.
- **VCM-14** The 50 ms window is a constant, not configurable. No requirement asks
  for it to be.

## Froala's own debounce

Measured in froala-editor 5.4.0, because it changes what every mode above actually
feels like.

- **VCM-15** `contentChanged` comes from Froala's **undo stack**, not from a DOM
  event: its `keydown` handler restarts a `max(250, opts.typingTimer)` timer — 500 ms
  by default — and only the elapsed timer pushes an undo step and fires
  `contentChanged`. It is a trailing-edge debounce, so sustained typing produces
  *nothing* until the user pauses, and Froala also drops a step whose HTML equals the
  last one.
- **VCM-16** Everything that is not typing bypasses that timer and fires
  synchronously: toolbar commands (twice — once before and once after the command),
  paste, cut, tab, undo/redo, Ctrl-combinations and blur. `html.set` fires no
  `contentChanged` at all, which is why a server-side `setValue` never echoes back as
  a delta.
- **VCM-17** The delays therefore **stack**: `ON_CHANGE` syncs ~500 ms after the user
  pauses, and `TIMEOUT` after ~500 ms + `valueChangeTimeout`. `INTERVAL` and `ON_BLUR`
  are unaffected — neither listens to `contentChanged`. Both e2e tests above are
  written around this and say so at the assertion.
- **VCM-18** `typingTimer` is a normal Froala init option, so the 500 ms becomes
  configurable as soon as the phase 2 option channel exists. Today `_initEditor`
  passes only `key` and `events`, so it is fixed.

## Known gaps

- VCM-5, the flush on a mode switch, is still without a test.
