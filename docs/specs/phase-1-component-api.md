# Component API

The Java-facing surface of phase 1: what `FroalaEditor` is as a Vaadin field, plus
the license key and `FroalaViewer`. Editor **configuration** is phase 2 and is not
specified here.

## Field contract

- **API-1** `FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String>`
  with the empty string as its empty value, so it works as a `HasValue` in a
  `Binder` like any other Vaadin field, and `isEmpty()` / required validation behave
  normally.
  *Verified:* `FroalaEditorKaribuTest.setValue_reachesTheClientProperty`,
  `valueChangeListener_firesOnServerSideChange`. Binder integration itself is
  unverified — no test binds the editor to a bean.
- **API-2** The field parts a Vaadin field is expected to have — label, helper text,
  error message, required indicator — come from Vaadin's own `FieldMixin` on the
  client element and are rendered by the connector's template, so they look and
  behave like every other field.
  *Verified:* `FroalaEditorIT.labelAndHelperText_areRendered`. Marked in the test's
  javadoc as a `FieldMixin` smoke check: it would still pass with the editor removed.
- **API-3** Implemented mixins: `HasLabel`, `HasHelper`, `HasSize`, `HasStyle`,
  `HasValidator<String>`, `HasValidationProperties`, `InputNotifier`,
  `Focusable<FroalaEditor>`.
- **API-4** HTML is **not** sanitized or parsed on the server. Whatever the client
  reports becomes the value. Consumers must treat the value as untrusted before
  storing or re-displaying it. Stated in the class javadoc.
  *Verified:* n/a — this is an explicit non-guarantee.

## Read-only, disabled, focus

- **API-5** `setReadOnly(true)` and `setEnabled(false)` both stop editing: the
  editing area loses `contenteditable` and the toolbar stops responding. Froala has
  no mode API — the connector uses `edit.off()` / `edit.on()`.
  *Verified:* `FroalaEditorIT.readOnly_stopsEditing`, `disabled_stopsEditing`.
- **API-6** `disabled` is owned by Vaadin's `DisabledMixin`, which also maintains
  `aria-disabled` and marks the property `sync: true`. `readonly` is declared locally,
  in the same shape Vaadin uses, because Vaadin's own lives in `InputControlMixin`,
  which assumes a slotted `<input>`.
  *Verified:* by inspection.
- **API-7** `focus()` moves the caret into the editing area, not just to the host
  element.
  *Verified:* `FroalaEditorIT.focusButton_movesFocusIntoTheEditor`.
- **API-8** The `focused` attribute is toggled by Vaadin's `FocusMixin`, and Froala's
  `focus` / `blur` events are re-dispatched from the host so Flow-side focus
  listeners work.
  *Verified:* unverified as a direct assertion.

## License key

Froala is commercial. The add-on ships **no** key — without one the editor works but
shows Froala's unlicensed watermark.

- **API-9** `setLicenseKey(String)` maps onto Froala's **`key`** option. Not
  `apiKey`, which is Froala's Google Drive key (`index.d.ts:1144`, used only for
  `gapi.client.init`); the licensing code reads `opts.key`.
  *Verified:* `FroalaEditorKaribuTest.licenseKey_isHandedToTheClient` — which
  asserts the **element property**, not that Froala received it. Reading
  `editor.opts.key` off the live instance needs a key input in the demo and is open.
  The test name overclaims and should read `…_isSetAsElementProperty`.
- **API-10** `setLicenseKey(null)` removes the property instead of sending a null, so
  Froala sees no `key` option at all rather than an explicit empty one.
  *Verified:* `FroalaEditorKaribuTest.licenseKey_nullRemovesTheProperty`.
- **API-11** The key is read once, when the client-side editor initializes. Setting
  it on an attached instance has no effect until the next detach/attach. Documented
  on the setter.
  *Verified:* unverified.
- **API-12** ~~`setDefaultLicenseKey(String)` applies a key to every instance created
  afterwards.~~ **To be removed.** One key per application set at construction is what
  other add-ons do; global mutable static state is not this add-on's scope. Currently
  still present, covered by
  `FroalaEditorKaribuTest.defaultLicenseKey_appliesToNewInstancesOnly`.
- **API-13** Spring `@ConfigurationProperties` binding of the key is **not** part of
  the component. `component/` stays Spring-free; the binding belongs in `demo/` or a
  later optional `vcf-froala-editor-spring` module (phase 3).

## FroalaViewer

- **API-14** `FroalaViewer` renders Froala-produced HTML **outside** an editor, with
  Froala's own stylesheet, by hosting a `fr-view` element. `setContent(String)` writes
  the HTML; there is no value binding and no editing.
  *Verified:* used as the assertion channel in `FroalaEditorIT` — the demo echoes the
  server-side value into it, so several tests fail if it stops rendering. It has no
  test of its own.
- **API-15** `setContent` writes `innerHTML` verbatim. Same non-guarantee as API-4:
  unsanitized input renders as markup.

## Known gaps

- No test binds `FroalaEditor` in a `Binder`, although API-1 is the reason for
  extending `AbstractSinglePropertyField` at all.
- The three-arg `super("value", "", true)` constructor makes Flow register its own
  listener for a `value-changed` DOM event. Nothing dispatches it, so it is inert —
  but turning `value` into a notifying Lit property would silently open a second
  update path beside `_value-delta`. Noted at the constructor.
