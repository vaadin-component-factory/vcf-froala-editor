# Upstream issue draft — `parttio/hugerte-for-flow`

Three ways an edit can be lost on its way to the server. Found while porting the delta
value transfer into `vcf-froala-editor`, which is built on the same design, so all
three apply there too and are fixed in that add-on. Not filed yet — this is the draft
to file.

Read against `src/main/resources/META-INF/resources/frontend/vaadin-huge-rte.js` at
HEAD, 2026-08-24.

---

## 1. The 50 ms throttle drops a change instead of deferring it

`onValueChange()` wraps everything it does in the throttle check, with no `else`:

```js
onValueChange() {
    let now = Date.now();
    if (this._lastSyncedValueTimestamp < now - 50) {
        this._lastSyncedValueTimestamp = now;
        // ... build the delta and dispatch _value-delta
    }
    // nothing here: a call inside the window is simply gone
}
```

A call that lands inside the window does nothing at all, and nothing reschedules it. If
another change follows, that later one carries the delta and the loss is invisible —
which is why this survives casual testing. If none follows, the edit never reaches the
server.

It also swallows explicit flushes. `stopValueChangeTimeout()` and
`stopValueChangeInterval()` both call `onValueChange()` with the comment *"flush value
to server"*, and that flush is silently dropped whenever it happens within 50 ms of the
previous sync — for instance when the user switches the value change mode right after
typing.

**Fix:** defer instead of dropping — reschedule the call for the end of the window.

## 2. A blur only flushes in `ON_BLUR` mode

```js
editor.on('blur', e => {
    ...
    this.onValueChangeIfMode("blur");
```

In `TIMEOUT` and `INTERVAL` the change waits for a timer. Losing focus usually means the
user clicked something else, and that click can detach the component or navigate away
before the timer fires. The blur is the last moment at which anything can still be sent,
regardless of which mode is active.

Reproduction (this is how it was found, against the Froala port, in `INTERVAL` or
`TIMEOUT` mode):

1. Type something into the editor.
2. Without pausing, click a button that removes the editor from the layout.
3. Re-add it — the last thing typed is gone.

**Fix:** flush on every blur, in every mode. A delta that is empty dispatches nothing,
so the modes that have already synced pay nothing for it.

## 3. The mode timers are never cleared on detach

`disconnectedCallback()` removes the editor and the light-DOM container, but
`_valueChangeHandleForInterval` and `_valueChangeHandleForTimeout` are left running —
`clearInterval` / `clearTimeout` appear only in `stopValueChangeInterval()`,
`stopValueChangeTimeout()` and the debounce restart, none of which the teardown calls.

Two consequences:

- In `INTERVAL` mode the interval keeps firing against a removed editor, forever, and
  every re-attach starts another one on top.
- A pending `TIMEOUT` flush still fires after the editor is gone. `onValueChange()` then
  reads `this.editor?.getContent() ?? this._lastSyncedValue`, which is the *old* value,
  produces an empty delta and sends nothing — so this is a third route to the same lost
  edit as in 2.

**Fix:** clear both handles in `disconnectedCallback()`.

---

## Suggested shape

The three fixes fit together, and separating the two jobs `onValueChange()` currently
does makes them fall out on their own:

- `onValueChange()` only builds the delta, updates the last synced value and dispatches.
  No rate limiting, always immediate.
- The throttle moves into the `ON_CHANGE` path, which is the only mode that needs one —
  the other two limit their own rate already, and a flush must never be held back.
- `blur` calls `onValueChange()` unconditionally.
- `disconnectedCallback()` clears the interval, the timeout and the throttle handle.

That is what `vcf-froala-editor` does now; the connector there is close enough in
structure that the diff should read almost directly.
