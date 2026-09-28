# Froala Editor for Vaadin Flow

A Vaadin Flow add-on that wraps the [Froala WYSIWYG Editor](https://froala.com/wysiwyg-editor/)
as a Java component.

> [!IMPORTANT]
> **Froala is commercial software.** This add-on is licensed under Apache-2.0, but the
> Froala editor it wraps is not. To use it in your application you need your own license
> from [Froala](https://froala.com/wysiwyg-editor/pricing/). The add-on ships no license
> key. Without one the editor works, but shows Froala's unlicensed watermark.

## Compatibility

| | Version |
|---|---|
| Java | 17 or newer |
| Vaadin | 24.10, built and tested against 24.10.9 |
| Froala | 5.4.0, pulled in as the `froala-editor` npm package |

## Usage

```java
FroalaEditor editor = new FroalaEditor("Description");
editor.setValue("<p>Hello <b>World</b></p>");
editor.addValueChangeListener(event -> save(event.getValue()));
```

The value is the editor's HTML as a `String`.

### License key

```java
editor.setLicenseKey(key);
```

The key is set on each editor. There is no global default, so an application that reads
its key from configuration passes it to every editor it creates. The key is read when the
editor starts in the browser: setting it on an editor that is already attached takes
effect only after it has been detached and attached again.

### When the value is sent

`setValueChangeMode` decides when a change made in the browser reaches the server:

| Mode | The value is sent |
|---|---|
| `ON_CHANGE` (default) | on Froala's `contentChanged` |
| `ON_BLUR` | when the editor loses focus |
| `INTERVAL` | every `setIntervalPeriod` milliseconds (default 2000) while there are changes |

`setValueChangeTimeout` sets Froala's `typingTimer` for `ON_CHANGE` (default 500, minimum
250). Toolbar commands, paste, cut and undo are sent at once. `INTERVAL` is the only mode
that sends anything while the user types without pausing.

This is the add-on's own `ValueChangeMode`, not Vaadin's
`com.vaadin.flow.data.value.ValueChangeMode`.

### Froala options

Froala options are set with `setOptions`, or passed to the constructor. There are three
ways to write them:

```java
// typed
FroalaOptions options = FroalaOptions.defaults()
        .withPlaceholderText("Write something")
        .withCharCounterMax(2000);
editor.setOptions(options);

// raw JSON, for options without a with… method
editor.setOptions("{\"pastePlain\": true}");

// an elemental.json.JsonObject you already have
editor.setOptions(jsonObject);
```

`FroalaOptions` is immutable, so one instance can be shared between editors. Not every
Froala option has a `with…` method; any option can be passed as JSON. `getOptionsJson()`
returns the options the editor is configured with, whichever way they were set.

Limits of options:

- Each `setOptions` call replaces the previous options. They are not merged.
- Froala cannot change options on a running editor. Calling `setOptions` on an attached
  editor destroys it and builds a new one. The value is kept, but caret, selection,
  scroll position and undo history are lost.
- `key`, `height` and `width` have no `with…` method. Use `setLicenseKey` and the Vaadin
  size methods.
- The setters win over the options: `setLicenseKey` over `key` while it holds a key, and
  `setValueChangeTimeout` over `typingTimer` once it has been called.
- `events` and `aiAssistRequest` cannot be set from Java: they take JavaScript functions,
  and options are sent as JSON. `setOptions` throws on `events`; `aiAssistRequest` is
  accepted and has no effect. Froala events reach the server only through the
  listeners `FroalaEditor` offers.

### Toolbar

`withToolbarButtons` takes a `FroalaToolbar`, either flat or grouped.
`withToolbarButtonsMd`, `…Sm` and `…Xs` do the same for narrower screens.


```java
FroalaToolbar flat = FroalaToolbar.of("bold", "italic", "|", "undo", "redo");

FroalaToolbar grouped = FroalaToolbar.ofGroups(
        FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold", "italic", "underline", "strikeThrough"),
        FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_MISC, "undo", "redo").withAlign(FroalaToolbarAlign.RIGHT));
```

Froala draws the button that opens a group's overflow panel only for its own four group
names, available as `MORE_TEXT`, `MORE_PARAGRAPH`, `MORE_RICH` and `MORE_MISC`. A group with
any other name that holds more buttons than it shows would leave those buttons unreachable,
so `ofGroups` throws. Raise `withButtonsVisible` or use one of the four names.

### Working with the selection

`replaceSelectionContent` inserts HTML at the caret, replacing the selection if there is
one. `addSelectionChangeListener` reports whether there is a selection, for example to
enable an action only while there is one:

```java
Button redact = new Button("Redact", event -> editor.replaceSelectionContent("<strong>[redacted]</strong>"));
redact.setEnabled(false);
editor.addSelectionChangeListener(event -> redact.setEnabled(event.hasSelection()));
```

Clicking a button outside the editor keeps the selection, so the action still finds it.
The call is asynchronous: the new value arrives in the value change listener, whatever the
value change mode, and is not in `getValue()` right after the call.

### Showing the content outside the editor

`FroalaViewer` displays HTML with Froala's styles, so it looks as it did in the editor:

```java
FroalaViewer viewer = new FroalaViewer();
viewer.setContent(editor.getValue());
```

### Sanitizing

Nothing is sanitized on the server: neither the value `FroalaEditor` receives nor what
`FroalaViewer.setContent` displays. Froala's own cleaning runs in the browser and does not
protect a value that reaches the server any other way. Treat the value
as untrusted input, and sanitize it before storing or displaying it.

## Known issues

### The quick-insert button can be covered

Froala's quick-insert button (the `+` on an empty line) goes to the left of the editor box
whenever there is at least its own width of room between the editor and the **page** edge.
Froala does not check whether an ancestor of the editor clips that spot. So inside an
`AppLayout` with the drawer open, a `Dialog`, a `Popover`, or any scroll container with
less padding than that, the button ends up under the drawer or cut off at the container's edge.

There is no Froala option for this. The workaround is to override the inline `left` Froala
sets, in a global stylesheet (the editor's content lives in the light DOM):

```css
.fr-quick-insert {
  left: 8px !important;
}
```

This puts the button inside the editor box. The row of insert buttons that opens from it
follows along, because Froala positions it from the button's computed `left`. In an empty
editor the button covers the start of the placeholder text. The add-on cannot offer a
custom property for this: the right value depends on the layout around each editor. If
you need it to vary, use a property of your own:

```css
.fr-quick-insert {
  left: var(--my-quick-insert-left, 8px) !important;
}
```

Alternatively, leave enough room to the left of the editor inside its container, or switch
the `quickInsert` plugin off.
