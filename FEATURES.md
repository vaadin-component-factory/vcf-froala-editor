# Froala Editor for Vaadin Flow: features

A Vaadin Flow add-on that wraps the Froala WYSIWYG Editor 5.4.0 as a Java component. It needs
Java 17 or newer and Vaadin 24.10, and it is licensed under Apache-2.0. Froala itself needs a
license of your own. The license key is set per editor and is never hard-coded. Without a key
the editor runs with Froala's watermark.

## A proper Vaadin field

- The value is the editor's HTML as a `String`. The field works with Binder, validation,
  read-only and disabled.
- Label, helper text and error message behave like in Vaadin's other fields. They are linked
  for screen readers with `aria-labelledby` and `aria-describedby`.
- Three modes decide when a change reaches the server: on change (with a configurable typing
  pause), on blur, or at a fixed interval.
- Changes go to the server as a delta, not as the full HTML each time.
- Focus and blur events reach the server.

## Configuration

- Froala's options are set typed through `FroalaOptions`, in a builder style. Anything without
  a method yet can be passed as JSON.
- The toolbar is typed, flat or grouped, with separate variants for narrower screens. Every
  Froala button is available as a constant.
- The popups' button lists, e.g. for images, links and tables, can each be configured.
- Froala's plugins are chosen from an enum. The default is a sensible basic rich-text set.
- The browser loads plugins and language files only when an editor needs them.
- All 39 Froala languages are available. The language follows the UI's locale, and the text
  direction (left-to-right or right-to-left) is applied to the label and helper text too.

## Look

- The Vaadin theme is the default. The editor follows Lumo, including its dark variant.
- Every colour, font and radius can be overridden through `--vcf-froala-*` properties, without
  touching Lumo.
- Froala's own themes can still be chosen.

## Custom toolbar commands

- Custom buttons with a title and any Vaadin icon run a listener on the server.
- Buttons can go in the toolbar and in the popups, and in several places at once.
- Keyboard shortcuts are Ctrl or Cmd plus a letter, a digit or F1 to F12, with Shift and Alt
  optional. Any other key works through its key code.
- A plain Vaadin Popover can be attached to a button. It opens on click and on the shortcut.
- Buttons can work as toggles, with the pressed state held and controlled by the server.

## Content and selection

- The server can insert HTML at the caret or replace the selection with it.
- The server can select the whole content.
- Images, files and videos are uploaded through Flow to a handler of the application, which
  stores the file and returns its link. No upload endpoint of its own is needed.
- A listener reports whether text is selected, e.g. to enable an action only then.
- `FroalaViewer` shows stored HTML outside the editor, looking as it did while editing. It
  needs no editor on the page.

## Security

- The handling of unsanitized HTML is documented, with an example of sanitizing on the server
  with jsoup.
- Upload is off until an upload handler or URL is set. So no `blob:` links end up in the stored
  HTML that would be dead after a reload.

## Quality

- Covered by unit tests, browserless tests (Karibu) and end-to-end tests in a real Chromium
  (Playwright), about 190 tests in all.
- The component has no dependency on Spring.
