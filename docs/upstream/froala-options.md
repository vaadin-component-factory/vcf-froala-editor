# Froala options in 5.4.0, the measured facts

Measured on 2026-09-29 against `demo/node_modules/froala-editor/` 5.4.0, for #7. The
browser numbers come from a static harness that loads `js/froala_editor.pkgd.min.js`,
`js/plugins/track_changes.min.js`, `js/plugins/trim_video.min.js` and all seven files under
`js/third_party/`, which registers all 53 plugins. `docs/upstream/froala-plugins.md` describes
how to build such a harness.

## How many options there are

| Source | Count |
|---|---|
| `FroalaOptions` interface in `index.d.ts`, top-level members | **322** |
| `Object.keys(FroalaEditor.DEFAULTS)` with all 53 plugins loaded | **325** |
| Both together | **341** |

The d.ts count was 302 in 5.3.1, so the surface moves between minor releases. Neither
source is complete on its own.

19 options exist at runtime but are missing from `index.d.ts`:
`DOMPurify`, `draggableElement`, `toolbarResponsiveToEditor`, `aiChatMaxFiles`,
`aiChatRequestTimeoutMs`, `fileUploadToAzure`, `filesInsertButtons`, `filesInsertButtons2`,
`filesManagerUploadParam`, `googleOptions`, `filesManagerUploadToAzure`,
`filesManagerUploadMethod`, `imageUploadToAzure`, `html2pdf`, `keepTextFormatOnTable`,
`videoUploadToAzure`, `fontAwesomeTemplate5`, `fontAwesome5Sets`, `tui`.

16 options are typed in `index.d.ts` but have no runtime default:
`apiKey`, `app`, `codoxOptions`, `docId`, `editor`, `username`, `aviaryKey`, `aviaryOptions`,
`events`, `emoticonsStep`, `key`, `autoStart`, `autofocus`, `update`, `popupButtons`,
`collabConfig`. Some are read without a default (`key`, `events`). Others belong to
integrations that no longer ship (`aviaryKey`).

## Commands and button names

- **200 commands** (`FroalaEditor.COMMANDS`) with all 53 plugins loaded. Of those, 17 come
  from the core, 1 from the `edit_in_popup` module and the rest from plugin files.
- **136** of them appear in at least one of Froala's own default button lists, meaning
  `TOOLBAR_BUTTONS` with its `_MD`/`_SM`/`_XS` variants and every `…Buttons` option.
  `FroalaButton` holds those 136 plus `align`, `save`, `aiChangeTone` and `aiTranslateTo`,
  which work as buttons but appear in no default list.
- The other 60 run inside popups (`imageSetAlt`, `tableInsert`, `applytextColor`, …) or are
  the overflow buttons of the four groups (`moreText`, …).
- `quickInsertButtons` does not take command names. The quick insert plugin keeps its own
  registry (`FroalaEditor.QUICK_INSERT_BUTTONS`) of seven names, `image`, `video`, `embedly`,
  `table`, `ol`, `ul` and `hr`. Each declares a `requiredPlugin`, except `hr`.

## Options found not to work

`toolbarButtonsEnabledOnEditorOff` should keep the buttons it names usable after
`editor.edit.off()`, which is how the add-on makes an editor read-only or disabled. Built with
`toolbarButtons: ['bold', 'selectAll', 'alignLeft']`, `edit.off()` leaves `.fr-toolbar`
with `fr-disabled` and no button with a class of its own, both with `[]` and with
`['selectAll']`. The code path that should mark single buttons calls `.filter(function(e){var
e=S(e), …})`, which looks like it receives an index rather than an element. That is a reading of
minified code, not a confirmed cause. The add-on offers no typed method for the option.

## What the typed API covers

After #7, `FroalaOptions` has a `with…` method for the HTML cleaning options (`html…`,
`useClasses`), the paste options (`paste…`, `word…` apart from `wordExportFileName`) and 27 of
the 28 `…Buttons` options of popups and plugins. The one left out is
`toolbarButtonsEnabledOnEditorOff`, see above. Everything else goes through
`FroalaEditor.setOptions(String)`.
