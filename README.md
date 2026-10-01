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
| Vaadin | 24.10, built and tested against 24.10.7 |
| Froala | 5.4.0, pulled in as the `froala-editor` npm package |

## Installation

```xml
<dependency>
    <groupId>com.vaadin.componentfactory</groupId>
    <artifactId>vcf-froala-editor</artifactId>
    <version>1.0.0</version>
</dependency>
```

Vaadin's frontend build pulls in the `froala-editor` npm package by itself.

The add-on is not released yet. Until it is, build it with `mvn install` in the `component`
directory and use the version from its `pom.xml`, currently `1.0.0-SNAPSHOT`.

## Usage

```java
@Route("")
public class EditorView extends VerticalLayout {

    public EditorView() {
        FroalaEditor editor = new FroalaEditor("Description");
        editor.setValue("<p>Hello <b>World</b></p>");
        editor.addValueChangeListener(event -> Notification.show(event.getValue()));
        add(editor);
    }
}
```

The value is the editor's HTML as a `String`. It is not sanitized, see [Sanitizing](#sanitizing).

Label, helper text and error message work as in Vaadin's other fields. They are linked to
Froala's editable area with `aria-labelledby` and `aria-describedby`.

### License key

```java
editor.setLicenseKey(key);
```

The key is set on each editor. There is no global default, so an application that reads its key
from configuration passes it to every editor it creates.

The key is read when the editor is built in the browser. On an editor that is already attached,
a new key takes effect only with the next build. That is a detach and attach, or a `setOptions`
call with other options:

```java
editor.setLicenseKey(key);                          // not yet used by the running editor
editor.setOptions(options.withSpellcheck(false));   // rebuilds the editor, now with the key
```

### Value changes

`setValueChangeMode` decides when a change made in the browser reaches the server:

| Mode | The value is sent |
|---|---|
| `ON_CHANGE` (default) | on Froala's `contentChanged` |
| `ON_BLUR` | when the editor loses focus |
| `INTERVAL` | every `setIntervalPeriod` milliseconds (default 2000) while there are changes |

`setValueChangeTimeout` sets Froala's `typingTimer` for `ON_CHANGE` (default 500). Below 250,
Froala's own minimum, it throws. For example `setValueChangeTimeout(100)` throws an
`IllegalArgumentException`.

Toolbar commands, paste, cut and undo are sent at once. `INTERVAL` is the only mode that sends
anything while the user types without pausing.

The modes are the add-on's own `FroalaValueChangeMode`, not Vaadin's `ValueChangeMode`:

```java
editor.setValueChangeMode(FroalaValueChangeMode.ON_BLUR);
```

### Froala options

Froala options are set with `setOptions`, or passed to the constructor. There are two
ways to write them:

```java
// typed
FroalaOptions options = FroalaOptions.defaults()
        .withPlaceholderText("Write something")
        .withCharCounterMax(2000);
editor.setOptions(options);

// raw JSON, for options without a with… method
editor.setOptions("{\"tabSpaces\": 4}");
```

`FroalaOptions` is immutable, so one instance can be shared between editors. Not every Froala
option has a `with…` method, but any option can be passed as JSON.

`getOptionsJson()` returns the options set with `setOptions`, whichever way they were set. The
add-on's own defaults, such as the `vaadin` theme, are not part of it.

Limits of options:

- Each `setOptions` call replaces the previous options. They are not merged. To change one
  option, keep your `FroalaOptions` and pass a changed copy:

  ```java
  editor.setOptions(options.withPlaceholderText("Write something"));
  editor.setOptions(FroalaOptions.defaults().withCharCounterMax(2000)); // the placeholder is gone
  editor.setOptions(options.withCharCounterMax(2000));                 // both are set
  ```
- Froala cannot change options on a running editor. Calling `setOptions` with other options
  on an attached editor destroys it and builds a new one. The value is kept, but caret,
  selection, scroll position and undo history are lost. The same options again change
  nothing.
- `key`, `height` and `width` have no `with…` method. Use `setLicenseKey` and the Vaadin
  size methods.
- The add-on sets Froala's `saveInterval` to 0 unless your options set it, so the save
  plugin is off. The value reaches the server through the value change listener.
- `setLicenseKey` wins over the `key` option while it holds a key. `setValueChangeTimeout`
  wins over `typingTimer` once it has been called. For example `setLicenseKey("a")` together
  with `setOptions("{\"key\": \"b\"}")` builds the editor with `a`.
- Froala's `toolbarButtonsEnabledOnEditorOff` has no `with…` method, because it does not work
  in 5.4.0. It should keep the buttons it names usable while the editor is read-only or
  disabled, but Froala marks the whole toolbar disabled with or without it. With
  `{"toolbarButtonsEnabledOnEditorOff": ["selectAll"]}` the select-all button is disabled as
  well.
- `events` and `aiAssistRequest` cannot be set from Java, because they take JavaScript
  functions and options are sent as JSON. `setOptions("{\"events\": {}}")` throws an
  `IllegalArgumentException`. `aiAssistRequest` is accepted, but does nothing. Froala events
  reach the server only through the listeners `FroalaEditor` offers.

### Plugins and languages

The browser downloads a Froala plugin or language file only when an editor needs it.
`withPluginsEnabled` names the plugins an editor gets:

```java
FroalaOptions options = FroalaOptions.defaults()
        .withPluginsEnabled(FroalaPlugin.ALIGN, FroalaPlugin.LISTS, FroalaPlugin.TRACK_CHANGES)
        .withLanguage("de");
```

- Without `withPluginsEnabled` an editor gets `FroalaPlugin.basics()`, a basic rich-text
  editor. These are `ALIGN`, `COLORS`, `FIND_AND_REPLACE`, `FONT_FAMILY`, `FONT_SIZE`, `HELP`,
  `LINE_HEIGHT`, `LINK`, `LINK_TO_ANCHOR`, `LISTS`, `PARAGRAPH_FORMAT`, `QUOTE`, `URL` and
  `WORD_PASTE`. Anything that inserts other content is off, and so are the style menus that
  only offer Froala's samples.
  For example an editor without options has no table button. `basics()` returns a new set,
  so adding one plugin takes one line:

  ```java
  Set<String> plugins = FroalaPlugin.basics();
  plugins.add(FroalaPlugin.TABLE);
  options = options.withPluginsEnabled(plugins);
  ```
- `FroalaPlugin.all()` enables every plugin, as in
  `options.withPluginsEnabled(FroalaPlugin.all())`. Some plugins need a server, a second
  library or a paid service, such as `IMAGE_MANAGER` or `SPELL_CHECKER`. The Javadoc of each
  constant says which.
- `FroalaPlugin` covers every plugin file of the npm package. Its constants hold the name
  each plugin registers itself under, which is what Froala expects. For example the file
  `find_and_replace.min.js` registers `findReplace`, and that is what
  `FroalaPlugin.FIND_AND_REPLACE` holds.
- A plugin of your own goes into the same list by its registered name. The add-on only loads
  Froala's plugin files, so your application loads the file that registers yours, for example
  with `@JsModule`:

  ```js
  // frontend/my-plugin.js
  import FroalaEditor from 'froala-editor';

  FroalaEditor.PLUGINS.myPlugin = function (editor) {
    return { _init() { /* ... */ } };
  };
  ```

  ```java
  @JsModule("./my-plugin.js")
  public class MyView extends Div {
      public MyView() {
          add(new FroalaEditor(FroalaOptions.defaults().withPluginsEnabled(FroalaPlugin.LISTS, "myPlugin")));
      }
  }
  ```
- `withLanguage` takes the name of a file in Froala's `js/languages/`, such as `de` or `pt_br`.
  A name without a file leaves the editor in English. For example `withLanguage("de_DE")` shows
  English tooltips, because the file is called `de`.
- Without `withLanguage` the editor takes the language of the UI's locale, `UI.getLocale()`.
  It tries language and country first and the language alone second. For example `zh_CN`
  gives `zh_cn`, and `de_AT` gives `de` because there is no `de_at` file. A locale without a
  file leaves the editor in English. For example `en` has no file, while `en_GB` and `en_CA`
  get Froala's British and Canadian files.
- The locale is read when the editor is built. A later `UI.setLocale` does not change a
  running editor, because Froala cannot change the language of one. To switch at runtime,
  give the editor new options with the language:

  ```java
  editor.setOptions(options.withLanguage("fr"));
  ```

  This builds the editor again, which keeps the value but loses caret, selection and undo
  history. A build without a language reads the locale again. Only options that differ from
  the current ones cause a build, though. For example `setOptions` with the same options after
  `UI.setLocale` leaves the editor in its old language.
- Track changes marks insertions and deletions in the HTML. That markup reaches the server with
  the value unless the changes are accepted or rejected first. For example a deletion stays in
  the value as a `<span data-tracking-deleted="true">`.
- Froala adds the five buttons of track changes to every toolbar, including one you set
  yourself. For example `FroalaToolbar.of("bold", "italic")` then shows `showChanges`,
  `applyAll`, `removeAll`, `applyLast` and `removeLast` as well.

### Text direction

The component's `dir` follows the direction Froala builds the editor with, so the label,
helper text and error message sit on the same side as the text. That direction comes from
the language file if there is one, and from `withDirection` otherwise. Every Froala language
file names a direction, and Froala uses it over the option. The UI's locale picks a language
file too (see [Plugins and languages](#plugins-and-languages)), so `withDirection` takes
effect only for a locale without a file, such as English:

```java
FroalaOptions.defaults().withLanguage("ar");                                        // dir="rtl"
FroalaOptions.defaults().withLanguage("de");                                        // dir="ltr", even on a right-to-left page
FroalaOptions.defaults().withLanguage("ar").withDirection(FroalaTextDirection.LTR); // still dir="rtl"
FroalaOptions.defaults().withDirection(FroalaTextDirection.RTL);                    // dir="rtl" only under a locale without a file
FroalaOptions.defaults().withLanguage("en").withDirection(FroalaTextDirection.RTL); // dir="rtl" and English under any locale
```

The direction is applied each time the editor is built, and it wins over a `dir` you set on the
component yourself.

With neither a language file nor a direction, Froala's default `AUTO` applies and sets no
`dir`. When a build with `AUTO` follows one with a direction, the component gets back the `dir`
it had before, your own or none. With none it follows the page again.

### Toolbar

`withToolbarButtons` takes a `FroalaToolbar`, either flat or grouped.
`withToolbarButtonsMd`, `…Sm` and `…Xs` do the same for narrower screens. A button is named
by its command. `FroalaButton` has a constant for each of Froala's buttons, and its Javadoc
names the plugin each one needs.

```java
FroalaToolbar flat = FroalaToolbar.of(FroalaButton.BOLD, FroalaButton.ITALIC, FroalaButton.VERTICAL_SEPARATOR,
        FroalaButton.UNDO, FroalaButton.REDO);

FroalaToolbar grouped = FroalaToolbar.ofGroups(
        FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, FroalaButton.BOLD, FroalaButton.ITALIC),
        FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_MISC, FroalaButton.UNDO, FroalaButton.REDO)
                .withAlign(FroalaToolbarAlign.RIGHT));
```

The constants are plain strings. A command your application registers with Froala goes into
the same list as a string:

```java
FroalaToolbar.of(FroalaButton.BOLD, FroalaButton.ITALIC, "myCommand");
```

Froala draws the button that opens a group's overflow panel only for its own group names,
the `MORE_…` constants in `FroalaToolbarGroup`. A group with a name of your own therefore
has to show all its buttons. If it doesn't, `ofGroups` throws. Set `withButtonsVisible` to
the number of buttons, or use one of the `MORE_…` names:

```java
// throws: four buttons, three shown by default, and no button to open the rest
FroalaToolbar.ofGroups(FroalaToolbarGroup.named("tools",
        FroalaButton.UNDO, FroalaButton.REDO, FroalaButton.PRINT, FroalaButton.FULLSCREEN));

// works: all four are shown
FroalaToolbar.ofGroups(FroalaToolbarGroup.named("tools",
        FroalaButton.UNDO, FroalaButton.REDO, FroalaButton.PRINT, FroalaButton.FULLSCREEN).withButtonsVisible(4));
```

Two ready-made toolbars cover the common cases. `FroalaToolbar.froalaDefault()` is Froala's
own default toolbar. `FroalaToolbar.basics()` has the same groups, reduced to the buttons of
`FroalaPlugin.basics()`. The Javadoc of each lists every group with its buttons. Both are a
starting point for a toolbar that differs in a detail:

```java
// every button of every group shown, no overflow panels
FroalaOptions options = FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.basics().withAllButtonsVisible());

// Froala's toolbar, with every insert button shown and five text buttons before the overflow
FroalaToolbar toolbar = FroalaToolbar.froalaDefault()
        .withAllButtonsVisible(FroalaToolbarGroup.MORE_RICH)
        .withButtonsVisible(FroalaToolbarGroup.MORE_TEXT, 5);
```

Changing a single group needs a grouped toolbar. A flat one shows every button anyway, so
changing one of its groups throws:

```java
FroalaToolbar.of(FroalaButton.BOLD).withAllButtonsVisible(FroalaToolbarGroup.MORE_RICH); // IllegalStateException
```

Setting a toolbar also replaces Froala's narrower variants, which show fewer buttons per group.
For example `froalaDefault()` on a phone still shows four insert buttons, where Froala's own
default shows none. Set the narrow ones yourself where they should differ. For example, to hide
the insert buttons on phones as Froala does:

```java
FroalaOptions options = FroalaOptions.defaults()
        .withToolbarButtons(FroalaToolbar.froalaDefault())
        .withToolbarButtonsXs(FroalaToolbar.froalaDefault().withButtonsVisible(FroalaToolbarGroup.MORE_RICH, 0));
```

### Popup buttons

The popups have button lists of their own, such as the one that opens on an image or on a
link. Each has a `with…Buttons` method on `FroalaOptions` named after Froala's option, and its
Javadoc lists Froala's default. The list replaces the default, so name every button the popup
should keep:

```java
// the image popup with replace, align, remove, alternative text and size only
FroalaOptions options = FroalaOptions.defaults().withImageEditButtons(List.of(
        FroalaButton.IMAGE_REPLACE, FroalaButton.IMAGE_ALIGN, FroalaButton.IMAGE_REMOVE,
        FroalaButton.HORIZONTAL_SEPARATOR, FroalaButton.IMAGE_ALT, FroalaButton.IMAGE_SIZE));
```

`withQuickInsertButtons` is the exception. The quick insert plugin names its buttons itself, so
they come from `FroalaQuickInsertButton` rather than `FroalaButton`:

```java
FroalaOptions.defaults().withQuickInsertButtons(List.of(FroalaQuickInsertButton.TABLE, FroalaQuickInsertButton.UL));
```

### Own commands

A `FroalaCommand` is an action of your own with a name, a title, an icon and optionally a
keyboard shortcut. `addCommand` adds it to an editor and runs the listener on the server
whenever the user triggers it in that editor. The command's name decides where its button
appears, in the toolbar or in a popup's button list, like any of Froala's commands:

```java
FroalaCommand insertTemplate = new FroalaCommand("insertTemplate", "Insert template", VaadinIcon.FILE_TEXT.create())
        .withShortcut(Key.KEY_T, KeyModifier.SHIFT); // Ctrl+Shift+T, or Cmd+Shift+T on a Mac

// the command's name places its button, here in the toolbar after bold
editor.setOptions(FroalaOptions.defaults()
        .withToolbarButtons(FroalaToolbar.of(FroalaButton.BOLD, insertTemplate.name())));
Registration registration = editor.addCommand(insertTemplate,
        event -> editor.replaceSelectionContent("<p>Dear customer,</p>"));

// you can also add the command at multiple places, here in the toolbar and in the popup that opens on a link
editor.setOptions(FroalaOptions.defaults()
        .withToolbarButtons(FroalaToolbar.of(FroalaButton.BOLD, insertTemplate.name()))
        .withLinkEditButtons(List.of(FroalaButton.LINK_OPEN, insertTemplate.name())));

registration.remove(); // the command and its buttons are gone from this editor
```

The icon is any Vaadin icon: `VaadinIcon`, `LumoIcon`, an icon of your own iconset, an
`SvgIcon` with a URL or a `FontIcon`. An `SvgIcon` with a `DownloadHandler` shows nothing,
because it has no URL until it is attached.

A shortcut is always Ctrl, or Cmd on a Mac, plus a key, because Froala requires it. Froala adds
Ctrl or Cmd itself, and a `KeyModifier.CONTROL` or `KeyModifier.META` given anyway is ignored.
Shift and Alt can be added.

`withShortcut(Key, ...)` takes a letter, a digit or a function key from F1 to F12. Any other key
is given by its key code. That is the keyboard event's `keyCode`, which is what Froala expects.
The label is what the button's tooltip shows for the key. Which code a key has on which keyboard
layout, and whether the browser takes the combination first, is up to Froala and the browser:

```java
insertTemplate.withShortcut(Key.F2);                           // Ctrl+F2
insertTemplate.withShortcut(Key.KEY_T, KeyModifier.CONTROL);   // Ctrl+T (CONTROL is ignored)
insertTemplate.withShortcut(191, "/");                         // Ctrl+/ on a US layout, the tooltip shows "Ctrl+/"
```

These throw an `IllegalArgumentException`:

- a `Key` that is no letter, digit or function key from F1 to F12
- Alt Graph as a modifier
- a key code below 1, or a missing label
- a second command of the same name on one editor

```java
insertTemplate.withShortcut(Key.SLASH);                        // IllegalArgumentException, use the key code
insertTemplate.withShortcut(Key.KEY_T, KeyModifier.ALT_GRAPH); // IllegalArgumentException, Froala cannot bind it
insertTemplate.withShortcut(0, "X");                           // IllegalArgumentException, below 1
editor.addCommand(insertTemplate, event -> {});
editor.addCommand(insertTemplate, event -> {});                // IllegalArgumentException, the name is taken
```

Adding or removing a command on an attached editor builds the editor again, as `setOptions`
does. The value is kept, but caret, selection and undo history are lost. Several calls within
one server round trip cause one rebuild.

Froala keeps a command's title, icon and shortcut for the whole page, not per editor. The
listener, the buttons and the shortcut still belong to the editors that added the command.
What is shared:

- Two editors that add the same name with a different title, icon, shortcut or toggle both
  show the definition of the editor built last.
- A name of one of Froala's own commands, such as `bold`, replaces Froala's command in every
  editor on the page. A shortcut with the keys of one of Froala's, such as Ctrl+B, replaces
  that one as well.

```java
first.addCommand(new FroalaCommand("sign", "Sign", VaadinIcon.PENCIL.create()), event -> sign(first));
second.addCommand(new FroalaCommand("sign", "Sign off", VaadinIcon.CHECK.create()), event -> sign(second));
// both editors show the one built last, say "Sign off" with the check mark, and each runs its own listener

editor.addCommand(new FroalaCommand("bold", "Bold", VaadinIcon.BOLD.create()), event -> {});
// Froala's bold is gone from every editor on the page
```

A shortcut only works while the command is in the `shortcutsEnabled` option. Froala's default
list takes it in by itself. Options that set `shortcutsEnabled` must name the command too:

```java
editor.setOptions("{\"shortcutsEnabled\": [\"bold\", \"italic\", \"insertTemplate\"]}");
```

#### A popover at the button

Instead of a listener, a command can take a plain Vaadin `Popover`. A click on the command's
toolbar button opens the popover next to the button, and the command's shortcut opens it too:

```java
Popover popover = new Popover(new Paragraph("My own popup"));
editor.addCommand(new FroalaCommand("myPopup", "My popup", VaadinIcon.INFO_CIRCLE.create()), popover);
```

The editor puts the popover into the UI and points it at the button, which Froala replaces on
every rebuild. So don't add the popover to a layout and don't set a target of your own:

```java
layout.add(popover);         // not needed, the editor has put it into the UI already
popover.setTarget(someButton); // takes the popover away from the command's button
```

The editor never closes the popover itself, so it closes as its own settings say. A popover
that should stay open until its button is clicked again turns the other ways off:

```java
popover.setCloseOnOutsideClick(false);
popover.setCloseOnEsc(false);
```

This works for toolbar buttons only. An editor whose toolbar does not list the command has no
button, so the popover never opens there, not even by the shortcut.

#### Toggle commands

`withToggle()` makes a command's button show a pressed state, like Froala's bold button. Your
application holds the state per editor and switches it with `setCommandActive`, typically in
the command's listener:

```java
FroalaCommand reviewMode = new FroalaCommand("reviewMode", "Review mode", VaadinIcon.EYE.create()).withToggle();
editor.addCommand(reviewMode, event -> editor.setCommandActive(reviewMode, !editor.isCommandActive(reviewMode)));
```

Switching the state does not build the editor again, and a rebuild keeps it. Two editors with
the same command show their own state. A command the editor does not have, or one that is no
toggle, throws:

```java
editor.setCommandActive(insertTemplate, true); // IllegalArgumentException, not a toggle
```

A toggle with a popover does not follow the popover by itself. One line connects the two, so
the button shows as pressed while its popover is open:

```java
editor.addCommand(reviewMode, popover);
popover.addOpenedChangeListener(event -> editor.setCommandActive(reviewMode, event.isOpened()));
```

### Uploads

Upload is off until the editor has somewhere to send the file. Froala would otherwise insert a
`blob:` URL that is valid only in the browser tab that created it, so the stored HTML would
point at nothing after a reload. Switched off means the upload button is gone from the insert
popup, and a dropped or pasted image is not inserted.

An upload handler switches it on. It stores the file and returns the link the editor puts into
the document:

```java
editor.setImageUploadHandler(event -> {
    String id = storage.save(event.getInputStream()); // an id of its own, never the client's file name
    return "/images/" + id;
});
editor.setFileUploadHandler(...);
editor.setVideoUploadHandler(...);
```

The upload goes through Flow, so it needs no endpoint of its own. It is refused while the
editor is disabled or read-only, and the handler runs outside the UI's lock like any Flow
upload.

The application serves the stored files itself, like `/images/<id>` above, under a link that
stays valid across sessions. A URL of a Flow `DownloadHandler` does not, because it is bound to
the UI. A `FroalaViewer` opens such a link only after `setRouterIgnorePaths`, see
[Viewer](#viewer).

A Spring controller serves them, or without Spring a Vaadin `RequestHandler`. A
`VaadinServiceInitListener` registers the handler. It runs before Flow's router and gets the
user's `VaadinSession`, so the application can check who asks:

```java
public class ImageServing implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler((session, request, response) -> {
            String path = request.getPathInfo();
            if (path == null || !path.startsWith("/images/")) {
                return false; // everything else goes to Flow
            }
            // check the user, then write the file with a content type the application decides
            return true;
        });
    }
}
```

Without Spring, the file `META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener`
names the listener. In a Spring application a bean is enough. The demo's Upload / Files view
shows both ways, including how the content type is decided.

Spring Boot limits a multipart upload to 1 MB by default, and that limit applies to the upload
handler as well. A larger file is refused before the handler runs, and the user sees Froala's
"Error during file upload." Raise it to what the editor should accept. The request carries
Froala's form fields next to the file, so give it a little more room than the file:

```properties
# images and files up to Froala's own limit of 10 MB
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=11MB
```

An upload handler can also set a limit of its own, with or without Spring. Froala's own limits,
such as `imageMaxSize`, are checked in the browser only:

```java
editor.setImageUploadHandler(new FroalaUploadHandler() {
    @Override
    public String upload(UploadEvent event) throws IOException {
        return "/images/" + storage.save(event.getInputStream());
    }

    @Override
    public long getFileSizeMax() {
        return 10 * 1024 * 1024; // a larger image is refused before upload runs
    }
});
```

Froala checks its own limits in the browser before it uploads. They are 10 MB for an image or
a file and 50 MB for a video. To accept larger files, raise `imageMaxSize`, `fileMaxSize` or
`videoMaxSize` as well.

An endpoint of your own works too, with a URL in the options. It receives a multipart POST
with the file as `file` and answers `{"link": "…"}` with the URL it serves the file under. A
handler takes precedence over the URL:

```java
FroalaOptions options = FroalaOptions.defaults()
        .withImageUploadUrl("/api/upload/image")
        .withFileUploadUrl("/api/upload/file")
        .withVideoUploadUrl("/api/upload/video");
```

### Theme

The editor follows the Vaadin theme, including Lumo's dark variant. This is Froala's `theme`
option set to `vaadin`, and it is on by default. Its look comes from custom properties of the
add-on, which are set from Lumo. [Appendix: Theme properties](#appendix-theme-properties) lists
them all. Override them to change the editor without touching Lumo:

```css
html {
  --vcf-froala-accent-color: #7b1fa2;
  --vcf-froala-field-border-radius: 0;
}
```

The greys and tints are mixed from the theme's colors, so `--vcf-froala-neutral-color` changes
every grey in the editor. Hues Lumo has no color for keep their tone, such as the purple of
track changes. The field states look as they do on a Vaadin text field.

The heights and spacing of the toolbar and popups stay Froala's, because Froala places parts of
them at fixed offsets.

The editor provides a set of variants for the Vaadin theme:

| Variant | What it does |
|---|---|
| `OUTLINED` | Outlines the editor with a border, no field background. Looks like Froala's native editor. |
| `NO_HOVER_HIGHLIGHT` | No highlight while the mouse is over the editor. |

These can be set like theme variants on other fields:

```java
editor.addThemeVariants(FroalaEditorVariant.OUTLINED, FroalaEditorVariant.NO_HOVER_HIGHLIGHT);
```

These theme variants are not Froala's built-in themes and do not affect them. To change the
whole editor theme to a Froala native theme, you have to set that via the options:

```java
// Froala's own look, no theme
FroalaOptions.defaults().withTheme(FroalaTheme.NONE);

// Froala's dark theme
FroalaOptions.defaults().withTheme(FroalaTheme.DARK);

// a theme of your own, styled through the class brand-theme, with a stylesheet you load yourself
FroalaOptions.defaults().withTheme("brand");
```

### Selection

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
value change mode, and is not in `getValue()` right after the call:

```java
editor.replaceSelectionContent("<strong>[redacted]</strong>");
editor.getValue();                                            // still the old value
editor.addValueChangeListener(event -> save(event.getValue())); // gets the new one
```

Nothing is inserted while the editor is read-only or disabled, because both lock the value
against changes from the client:

```java
editor.setReadOnly(true);
editor.replaceSelectionContent("<strong>[redacted]</strong>"); // inserts nothing
```

`selectAll` selects the whole content, and the selection change listener reports it:

```java
editor.selectAll();
```

### Viewer

`FroalaViewer` displays HTML with Froala's styles, so it looks as it did in the editor:

```java
FroalaViewer viewer = new FroalaViewer();
viewer.setContent(editor.getValue());
```

It loads the add-on's stylesheets itself, so it needs no editor on the same page. The base text
takes font, colour and size from the page, which under Lumo matches the editor. The
`--vcf-froala-*` properties change the editor only.

The viewer carries the class `vaadin-theme` for the editor's default theme. Remove it when your
editors use another Froala theme:

```java
viewer.removeClassName("vaadin-theme");
```

Vaadin's router takes a click on a link inside the application as navigation to a route. A link
to an uploaded file is none, so the click shows "Couldn't find route". Give the viewer the paths
whose links should open with a page load instead. Every other link stays with the router, so a
link to one of your views keeps working as before:

```java
viewer.setRouterIgnorePaths("/files", "/reports/*.pdf");
```

A path is relative to the application's root, so it holds under any context path. A `*` matches
any characters, and a path without one covers everything below it, so `/files` stands for
`/files/*`.

For HTML you display some other way, the static method does the same for any component:

```java
FroalaViewer.applyRouterIgnore(div, "/files");
```

### Sanitizing

Nothing is sanitized on the server, neither the value `FroalaEditor` receives nor what
`FroalaViewer.setContent` displays. Froala's own cleaning runs in the browser and does not
protect a value that reaches the server any other way. Treat the value as untrusted input,
and sanitize it before storing or displaying it, for example with jsoup:

```java
String safe = Jsoup.clean(editor.getValue(), Safelist.relaxed());
```

Froala's cleaning in the browser is still worth setting, because it decides what the user can
put into the editor at all. `FroalaOptions` has a `withHtml…` method for each of Froala's
`html…` options.

The lists hold regular expressions, matched against the whole name and ignoring case. For
example `"h[1-6]"` allows `H3`, and `"b"` allows `b` but not `br`. The two exceptions are
`withHtmlAllowedEmptyTags` and `withHtmlDoNotWrapTags`, which take plain tag names.

A tag that is not allowed is unwrapped and its text stays. A tag in `withHtmlRemoveTags` goes
together with its content.

```java
FroalaOptions options = FroalaOptions.defaults()
        .withHtmlAllowedTags(List.of("p", "br", "strong", "em", "u", "a", "ul", "ol", "li", "h[1-6]"))
        .withHtmlRemoveTags(List.of("script", "style", "iframe")) // gone with their content
        .withHtmlAllowedAttrs(List.of("href", "target", "rel"))
        .withHtmlAllowedStyleProps(List.of()); // an empty list removes every style attribute
```

Pasted text gets its own options on top of these. `withPastePlain(true)` keeps lists and tables
and turns everything else into plain paragraphs. `withPasteDeniedTags`, `withPasteDeniedAttrs`
and `withPasteAllowedStyleProps` narrow what a paste keeps. The two denied lists take exact
names, not patterns, so `"h[1-6]"` denies nothing.

Text from Word goes through `FroalaPlugin.WORD_PASTE`, which asks the user whether to keep the
formatting. The `withWord…` methods change that.

```java
FroalaOptions options = FroalaOptions.defaults()
        .withPasteDeniedAttrs(List.of("class", "id", "style"))
        .withWordPasteModal(false)
        .withWordPasteKeepFormatting(false); // Word text is cleaned without asking
```

## Known issues

### The quick-insert button is hidden by the AppLayout drawer or cut off in a Dialog

Froala's quick-insert button (the `+` on an empty line) goes to the left of the editor box
whenever there is at least its own width of room between the editor and the page edge.
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
editor the button covers the start of the placeholder text.

The add-on cannot offer a custom property for this, because the right value depends on the
layout around each editor. If you need it to vary, use a property of your own:

```css
.fr-quick-insert {
  left: var(--my-quick-insert-left, 8px) !important;
}
```

Alternatively, leave enough room to the left of the editor inside its container, or switch
the `quickInsert` plugin off.

## More

- The `demo` module is a runnable application that shows the editor with its options.
- Questions and bugs go to the
  [issue tracker](https://github.com/vaadin-component-factory/vcf-froala-editor/issues).

## Appendix: Theme properties

The custom properties of the `vaadin` theme. [Theme](#theme) explains how to use them.

| Property | What it is | Set from |
|---|---|---|
| `--vcf-froala-background-color` | Background of the editor, toolbar and popups | `--lumo-base-color` |
| `--vcf-froala-neutral-color` | The tone all greys are mixed from | `--lumo-contrast` |
| `--vcf-froala-accent-color` | Active buttons, links, selections | `--lumo-primary-color` |
| `--vcf-froala-accent-contrast-color` | Text on the accent color | `--lumo-primary-contrast-color` |
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
| `--vcf-froala-focus-ring-color` | Color of focus rings | `--vaadin-focus-ring-color`, else `--lumo-primary-color-50pct` |
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
