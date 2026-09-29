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
editor starts in the browser. On an editor that is already attached, a new key takes
effect only after the editor has been detached and attached again.

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
Froala option has a `with…` method, but any option can be passed as JSON. `getOptionsJson()`
returns the options the editor is configured with, whichever way they were set.

Limits of options:

- Each `setOptions` call replaces the previous options. They are not merged.
- Froala cannot change options on a running editor. Calling `setOptions` on an attached
  editor destroys it and builds a new one. The value is kept, but caret, selection,
  scroll position and undo history are lost.
- `key`, `height` and `width` have no `with…` method. Use `setLicenseKey` and the Vaadin
  size methods.
- The add-on sets Froala's `saveInterval` to 0 unless your options set it, so the save
  plugin is off. The value reaches the server through the value change listener.
- `setLicenseKey` wins over the `key` option while it holds a key. `setValueChangeTimeout`
  wins over `typingTimer` once it has been called.
- `events` and `aiAssistRequest` cannot be set from Java, because they take JavaScript
  functions and options are sent as JSON. `setOptions` throws on `events`. `aiAssistRequest` is
  accepted, but has no effect. Froala events reach the server only through the
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

Froala draws the button that opens a group's overflow panel only for its own group names,
the `MORE_…` constants in `FroalaToolbarGroup`. A group with a name of your own therefore
has to show all its buttons. If it doesn't, `ofGroups` throws. Set `withButtonsVisible` to
the number of buttons, or use one of the `MORE_…` names:

```java
// throws: four buttons, three shown by default, and no button to open the rest
FroalaToolbar.ofGroups(FroalaToolbarGroup.named("tools", "undo", "redo", "print", "fullscreen"));

// works: all four are shown
FroalaToolbar.ofGroups(FroalaToolbarGroup.named("tools", "undo", "redo", "print", "fullscreen").withButtonsVisible(4));
```

### Theme

The editor follows the Vaadin theme, including Lumo's dark variant. This is Froala's `theme`
option set to `vaadin`, and it is on by default. Its look comes from custom properties of the
add-on, which are set from Lumo. Override them to change the editor without touching Lumo:

```css
html {
  --vcf-froala-accent-color: #7b1fa2;
  --vcf-froala-field-border-radius: 0;
}
```

| Property | What it is | Set from |
|---|---|---|
| `--vcf-froala-background-color` | Background of the editor, toolbar and popups | `--lumo-base-color` |
| `--vcf-froala-neutral-color` | The tone all greys are mixed from | `--lumo-contrast` |
| `--vcf-froala-accent-color` | Active buttons, links, selections | `--lumo-primary-color` |
| `--vcf-froala-accent-contrast-color` | Text on the accent colour | `--lumo-primary-contrast-color` |
| `--vcf-froala-error-color` | Errors and deleted text | `--lumo-error-color` |
| `--vcf-froala-success-color` | Inserted text in track changes | `--lumo-success-color` |
| `--vcf-froala-warning-color` | Warnings and changed text | `--lumo-warning-color` |
| `--vcf-froala-border-color` | Grey borders | `--lumo-contrast-20pct` |
| `--vcf-froala-text-color` | Text in the toolbar and popups | `--lumo-body-text-color` |
| `--vcf-froala-text-color-secondary` | Lighter labels | `--lumo-secondary-text-color` |
| `--vcf-froala-text-color-tertiary` | Fainter labels | `--lumo-tertiary-text-color` |
| `--vcf-froala-text-color-disabled` | Disabled buttons | `--lumo-disabled-text-color` |
| `--vcf-froala-value-color` | Text in the editing area | `--vaadin-input-field-value-color`, else `--lumo-body-text-color` |
| `--vcf-froala-disabled-value-color` | The same, when disabled | `--vaadin-input-field-disabled-value-color`, else `--lumo-disabled-text-color` |
| `--vcf-froala-placeholder-color` | Placeholder | `--vaadin-input-field-placeholder-color`, else `--lumo-secondary-text-color` |
| `--vcf-froala-font-family` | Font | `--lumo-font-family` |
| `--vcf-froala-content-font-size` | Text size in the editing area | `--vaadin-input-field-value-font-size`, else `--lumo-font-size-m` |
| `--vcf-froala-content-font-weight` | Text weight in the editing area | `--vaadin-input-field-value-font-weight`, else `400` |
| `--vcf-froala-content-line-height` | Line height in the editing area | `--lumo-line-height-m` |
| `--vcf-froala-font-size-xxs` … `-m` | Text sizes in the toolbar and popups | `--lumo-font-size-xxs` … `-m` |
| `--vcf-froala-field-border-radius` | Corners of the editor | `--vaadin-input-field-border-radius`, else `--lumo-border-radius-m` |
| `--vcf-froala-radius-s` … `-l` | Corners of buttons, inputs, popups and dialogs | `--lumo-border-radius-s` … `-l` |
| `--vcf-froala-shadow-xs` … `-l` | Shadows of dropdowns, popups and dialogs | `--lumo-box-shadow-xs` … `-l` |
| `--vcf-froala-focus-ring-color` | Colour of focus rings | `--vaadin-focus-ring-color`, else `--lumo-primary-color-50pct` |
| `--vcf-froala-focus-ring-width` | Width of focus rings | `--vaadin-focus-ring-width`, else `2px` |
| `--vcf-froala-clickable-cursor` | Cursor over buttons | `--lumo-clickable-cursor` |
| `--vcf-froala-field-background` | Fill of the editor | `--vaadin-input-field-background`, else `--lumo-contrast-10pct` |
| `--vcf-froala-hover-highlight` | Highlight while the mouse is over the editor | `--vaadin-input-field-hover-highlight`, else `--lumo-contrast-50pct` |
| `--vcf-froala-hover-highlight-opacity` | Its strength | `--vaadin-input-field-hover-highlight-opacity`, else `0.1` |
| `--vcf-froala-invalid-border-color` | Border of an invalid editor, with `OUTLINED` | `--lumo-error-color` |
| `--vcf-froala-invalid-background` | Tint of an invalid editor | `--vaadin-input-field-invalid-background`, else `--lumo-error-color-10pct` |
| `--vcf-froala-invalid-hover-highlight` | Its hover highlight | `--vaadin-input-field-invalid-hover-highlight`, else `--lumo-error-color-50pct` |
| `--vcf-froala-readonly-border` | Border of a read-only editor | `--vaadin-input-field-readonly-border`, else `1px dashed var(--lumo-contrast-30pct)` |
| `--vcf-froala-disabled-background` | Tint of a disabled editor | `--vaadin-input-field-disabled-background`, else `--lumo-contrast-5pct` |

The greys and tints are mixed from these colours, so `--vcf-froala-neutral-color` changes every
grey in the editor. Hues Lumo has no colour for keep their tone, such as the purple of track
changes. The field states look as they do on a Vaadin text field. The heights and spacing of the
toolbar and popups stay Froala's, because Froala places parts of them at fixed offsets.

The editor provides a set of variants for the Vaadin theme:

| Variant | What it does |
|---|---|
| `OUTLINED` | Outlines the editor with a border, no field background. Let the editor appear like Froala's native look. |
| `NO_HOVER_HIGHLIGHT` | No highlight while the mouse is over the editor. |

These can be set like theme variants on other fields:

```java
editor.addThemeVariants(FroalaEditorVariant.OUTLINED, FroalaEditorVariant.NO_HOVER_HIGHLIGHT);
```

Please note, that these theme variants are NOT the built-in Froala themes, nor do they affect them. To change the whole editor theme to a Froala native theme, you have to set that via the options:

```java
// Froala's own look, no theme
FroalaOptions.defaults().withTheme(FroalaTheme.NONE);

// Froala's dark theme
FroalaOptions.defaults().withTheme(FroalaTheme.DARK);

// a theme of your own, styled through the class brand-theme
FroalaOptions.defaults().withCustomTheme("brand");
```

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
The call is asynchronous. The new value arrives in the value change listener, whatever the
value change mode, and is not in `getValue()` right after the call.

### Showing the content outside the editor

`FroalaViewer` displays HTML with Froala's styles, so it looks as it did in the editor:

```java
FroalaViewer viewer = new FroalaViewer();
viewer.setContent(editor.getValue());
```

### Sanitizing

Nothing is sanitized on the server, neither the value `FroalaEditor` receives nor what
`FroalaViewer.setContent` displays. Froala's own cleaning runs in the browser and does not
protect a value that reaches the server any other way. Treat the value as untrusted input,
and sanitize it before storing or displaying it.

## Known issues

### The quick-insert button is hidden by the AppLayout drawer or cut off in a Dialog

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
custom property for this, because the right value depends on the layout around each editor. If
you need it to vary, use a property of your own:

```css
.fr-quick-insert {
  left: var(--my-quick-insert-left, 8px) !important;
}
```

Alternatively, leave enough room to the left of the editor inside its container, or switch
the `quickInsert` plugin off.
