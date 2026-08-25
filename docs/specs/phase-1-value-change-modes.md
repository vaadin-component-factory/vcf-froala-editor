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
- **VCM-3** There is **no `TIMEOUT` mode**, and deliberately so. A debounce on
  `contentChanged` is what `ON_CHANGE` already is — Froala debounces typing itself
  (VCM-15) — so our own timer on top only stacked a second wait onto the first and made
  the effective idle time 500 ms + `valueChangeTimeout`. It was dropped on 2026-08-25
  in favour of configuring Froala's debounce directly (VCM-7). What the mode offered
  over `ON_CHANGE` was one number, and that number is now `setValueChangeTimeout`.
- **VCM-4** `INTERVAL` (client: `interval`) — syncs every `intervalPeriod` ms
  regardless of user activity, as long as there is something to sync (VT-2). Started
  from Froala's `initialized` event as well as on a mode switch, so it also runs for
  an editor that is created in this mode.
  It is the only mode that sends anything at all during an uninterrupted typing burst:
  Froala reports nothing until the user pauses, so `ON_CHANGE` and `ON_BLUR` have
  nothing to send. That is the reason it stayed when `TIMEOUT` went.
  *Verified:* `FroalaEditorIT.intervalMode_syncsOnEveryTick_whileTheUserKeepsTyping` —
  two ticks, with the editor never losing focus and the user never pausing.
- **VCM-5** Switching *away* from `INTERVAL` flushes the pending value first, so a mode
  change never swallows an edit.
  *Verified:* unverified.
- **VCM-6** Setting the mode to `null` server-side resets it to `ON_CHANGE` rather
  than throwing.
  *Verified:* `FroalaEditorKaribuTest.valueChangeMode_roundTripsAndDefaults`.

## Timeout and interval

- **VCM-7** `setValueChangeTimeout(int)` **is Froala's `typingTimer` option**, not a
  timer of ours: the idle time after the last keystroke before Froala reports the
  change at all. It therefore governs `ON_CHANGE`'s latency end to end. Default
  **500 ms**, Froala's own default. It is passed in the init options and written to
  `editor.opts` on a later change, which takes effect at once because Froala reads the
  option on every keystroke.
  *Verified:*
  `FroalaEditorIT.valueChangeTimeout_setsFroalasTypingTimer_andGovernsWhenAChangeIsReported`
  — all three: the value reaches `editor.opts.typingTimer` on a running editor, a
  rebuilt editor gets it through the init options, and the sync actually moves with it.
- **VCM-8** The timeout must be at least **250 ms**, the bound Froala enforces on
  `typingTimer` itself. Below that the option would be silently ignored and the getter
  would claim a value the editor does not use, so both the server and the client reject
  it with `IllegalArgumentException` and the same message.
  *Verified:* `FroalaEditorKaribuTest.valueChangeTimeout_rejectsAnythingFroalaWouldIgnore`.
- **VCM-8a** The option is **not exclusive to the value sync**. Froala uses the same
  timespan for its selection-change flush, which drives the active state of the toolbar
  buttons, and for the reveal delay of the inline toolbar. A long timeout slows those
  down too, and the javadoc says so.
- **VCM-19** `setIntervalPeriod(int)` is the time between two syncs in `INTERVAL`,
  and before the first one. Default **2000 ms**, must be greater than zero, rejected on
  both sides with the same message. Separate from `setValueChangeTimeout` because the
  two now mean different things: one is Froala's debounce, the other our tick.
  *Verified:*
  `FroalaEditorKaribuTest.intervalPeriod_roundTripsAndRejectsAnythingButPositiveValues`.

## Flush on blur

- **VCM-9** **A blur flushes in every mode**, not only in `ON_BLUR`. Losing focus
  usually means a click elsewhere, and that click can detach the component — which
  clears the pending interval and the pending throttle and would take the last edit
  with it. An empty delta dispatches nothing (VT-2), so a mode that synced already pays
  nothing.
  *Verified:* `FroalaEditorIT.typingThenDetachingImmediately_keepsTheLastChange` and its
  `_inIntervalMode` variant — type, then straight to the detach toggle with no pause and
  no click elsewhere first. The interval variant fails without this flush.
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
  `onValueChange()` itself does no rate limiting and always sends. `INTERVAL` limits its
  own rate already, and a flush — from a blur, a mode switch or an elapsed tick — must
  never be held back. Rate policy sits at the call site, the
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
- **VCM-17** This debounce is the whole latency of `ON_CHANGE`: it syncs
  `valueChangeTimeout` ms after the user pauses, nothing else waits. Nothing stacks on
  top of it any more — that stacking is what removed `TIMEOUT` (VCM-3). `INTERVAL` and
  `ON_BLUR` are unaffected; neither listens to `contentChanged`.
- **VCM-18** The e2e tests around it need Playwright's `clock().runFor()`, not
  `clock().fastForward()`: `fastForward` fires each due timer at most once and never the
  ones scheduled while it jumps, and Froala's debounce scheduling our sync is exactly
  that chain. "Nothing has been sent yet" is asserted by counting `_value-delta`
  dispatches on the client, because a delta only reaches the viewer through a server
  round trip in real time.

## Known gaps

- VCM-5, the flush on a mode switch, is still without a test.
- Froala's `save` plugin listens to `contentChanged` too and schedules a POST to
  `saveURL` 10 s later, which then fails on the missing URL. Nobody listens to that
  failure, so it is dead work rather than a defect — `saveInterval: 0` turns it off once
  the phase 2 option channel exists.
