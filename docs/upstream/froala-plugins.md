# Froala plugins in 5.4.0 — the measured facts

Everything here was measured on 2026-08-27 against
`demo/node_modules/froala-editor/` 5.4.0, partly by reading the sources and
partly in a real browser against the same bundle the add-on imports
(`js/froala_editor.pkgd.min.js`, loaded from a static harness). This file exists
because the plugin question was answered wrongly three times; check here before
answering it again, and correct this file rather than a javadoc if a number turns
out to be off.

## What the add-on loads

`vcf-froala-editor.js` imports `froala-editor/js/froala_editor.pkgd.min.js` and
nothing else. Everything below follows from that single import.

Froala ships three bundles and two plugin directories:

| Path | Registers |
|---|---|
| `js/froala_editor.min.js` | core only, **0** plugins |
| `js/plugins.pkgd.min.js` | 46 plugins, no core |
| `js/froala_editor.pkgd.min.js` | core + the same **46** plugins — this is ours |
| `js/plugins/` | 49 files: **48** register a plugin, 1 registers a module |
| `js/third_party/` | 7 files: **5** register a plugin, 2 are libraries |

## The numbers

Measured in the browser, `Object.keys(FroalaEditor.PLUGINS)` after loading our
bundle:

- **46 plugins loaded.**
- **37 modules** (`FroalaEditor.MODULES`) — not plugins, `pluginsEnabled` has no
  say over them.
- **183 commands** (`FroalaEditor.COMMANDS`).
- **107 of those 183 commands declare no plugin at all.**
- **32 of the 46 plugins are named by at least one command.** The other 14 have
  no toolbar button of their own.
- `FroalaEditor.REQUIRED_PLUGINS` is `["image", "video", "file", "filesManager"]`
  — Filestack refuses to work without those four.

## Two plugins ship as files but are in no bundle

`js/plugins/track_changes.min.js` and `js/plugins/trim_video.min.js` are **not
part of `froala_editor.pkgd.min.js`, and not part of `plugins.pkgd.min.js`
either**. They register `PLUGINS.track_changes` and `PLUGINS.trimVideoPlugin`
only when the standalone file is loaded, which the add-on never does.

Consequences, all confirmed in the browser:

- `FroalaEditor.COMMANDS["trackChanges"]` does not exist for us. The core still
  carries toolbar code that special-cases the name — it checks
  `pluginsEnabled.indexOf("track_changes")` and builds a `trackChanges` group —
  but the command that would fill it is not there.
- `FroalaPlugin.TRACK_CHANGES` and `FroalaPlugin.TRIM_VIDEO` are therefore
  **dead constants today**. Passing them to `withPluginsEnabled` adds a string
  that matches no loaded plugin. Nothing throws and nothing happens.
- Track changes is on the feature list. Turning it on means importing
  `froala-editor/js/plugins/track_changes.min.js` in addition to the bundle —
  an open decision, not something the current build supports.

`trim_video` is only ever called from `files_manager`
(`y.trimVideoPlugin.trimVideo(...)`), so it has no toolbar button and no
standalone use.

## How `pluginsEnabled` actually works

Two lines of the core decide everything:

1. `this.opts.pluginsEnabled || (this.opts.pluginsEnabled = Object.keys(PLUGINS))`
   — the default is `null`, which at init becomes **every plugin currently
   loaded**. For us that is the 46. Verified: the effective array equals the
   registry exactly.
2. The instantiation loop skips a plugin when `PLUGINS[name]` exists and
   `pluginsEnabled` does not contain `name`.

So **`pluginsEnabled` holds the registered name** (`PLUGINS.x`), not the file
name. `track_changes` is the one plugin whose registered name keeps an
underscore, which is why the core's own checks read
`pluginsEnabled.indexOf("track_changes")`.

`pluginsDisabled` is a second option, applied as a filter over `pluginsEnabled`.
The add-on does not expose it.

## Disabling a plugin does not reliably remove its buttons

The toolbar filter is `COMMANDS[name].plugin`, compared against
`pluginsEnabled`. A command that declares no `plugin` is never filtered — and
107 of the 183 do not declare one.

Measured with `pluginsEnabled: ['align', 'lists']` and buttons
`['bold', 'textColor', 'print', 'insertImage']`:

| Button | Declares | Rendered | Plugin instantiated |
|---|---|---|---|
| `bold` | nothing | yes | — (core `commands` module) |
| `textColor` | nothing | **yes** | **no** (`colors` was skipped) |
| `print` | `print` | no | no |
| `insertImage` | `image` | no | no |

`textColor` is the trap: the `colors` plugin registers `textColor`,
`backgroundColor` and five more commands, but declares no `plugin` on any of
them. Restricting `pluginsEnabled` leaves those buttons drawn with nothing
behind them. The same holds for every one of the 14 plugins no command names.

This also explains an earlier wrong measurement: `textColor` was used to test
whether disabled-plugin buttons disappear, and it never could have.

A button that *is* filtered is neither drawn nor counted towards a group's
`buttonsVisible` — Froala's `buildGroup` skips the whole iteration, including
the counter.

## Plugins whose commands are named differently than the plugin

Worth knowing when looking for a button:

- `export_to_word` registers the command `export`, not `exportToWord`.
- `import_from_word` registers `import`, not `importFromWord`.
- `find_and_replace` registers as plugin `findReplace`; its command is
  `findReplace` too.
- `trim_video` registers no command.

## The 14 plugins with no toolbar command

`charCounter`, `codeBeautifier`, `colors`, `cryptoJSPlugin`, `draggable`,
`entities`, `exportToWord`, `forms`, `importFromWord`, `lineBreaker`,
`quickInsert`, `url`, `wordCounter`, `wordPaste`.

Four of them do register commands, just not ones that name them (`colors`,
`forms`, `exportToWord`, `importFromWord`). The rest are pure behaviour.

## What needs something the browser alone cannot give

Read off each plugin's own option defaults.

| Plugin | Needs | Evidence |
|---|---|---|
| `aiAssist` | an endpoint | `aiAssistEndpoint: null`, no provider of its own |
| `collaborative` | a relay server | Yjs is bundled; the transport is not |
| `filestack` | account + API key | `filestackOptions: {}`, plus `REQUIRED_PLUGINS` |
| `imageManager` | an endpoint that lists images | `imageManagerLoadURL: "https://i.froala.com/load-files"` — Froala's own demo server — and `imageManagerDeleteURL: ""` |
| `save` | an endpoint | `saveURL: null`, `saveInterval: 10000` ms |
| `file`, `image`, `video` | an upload endpoint **for uploading only** | `fileUploadURL`/`imageUploadURL`/`videoUploadURL` all `null`; inserting by URL works without one |
| `filesManager` | an upload endpoint **for uploading only** | `filesManagerUploadURL: null`; its "By URL" and "Embedded Code" tabs work without one. Its `googleOptions: {}` adds Google Drive |
| `importFromWord` | the third-party mammoth.js script in the page | reads `window.mammoth`; `importFromWordUrlToUpload: null` is a server-side alternative |
| `exportToWord` | **nothing** | builds the file in the browser; its only option is `wordExportFileName` |
| `cryptoJSPlugin` | nothing itself | signs direct S3/Azure uploads for `file`, `image`, `video`, `filesManager` |

Every other loaded plugin needs nothing.

## The five plugins under `js/third_party/`

Not in any bundle, so not loaded, so not in `FroalaPlugin`. Each needs a library
or a service the add-on does not ship:

| File | Registers | Needs |
|---|---|---|
| `spell_checker.min.js` | `spellChecker` | WebSpellChecker SCAYT, a paid subscription |
| `embedly.min.js` | `embedly` | an Embedly account |
| `font_awesome.min.js` | `fontAwesome` | the Font Awesome stylesheet |
| `imageFileRobot.min.js` | `imageFilerobot` | the Filerobot library |
| `image_tui.min.js` | `imageTUI` | the Toast UI image editor library |

`showdown.min.js` and `yjs.min.js` register nothing — they are bundled copies of
the libraries `markdown` and `collaborative` use.

**The browser's own spell checker is a different thing entirely**: it is Froala's
`spellcheck` option, `true` by default, written onto the editable element as the
`spellcheck` attribute at init and again on every `html.set`. That is the spell
checker the add-on relies on, and it is already running. SCAYT is unrelated and stays
unwired.

## `edit_in_popup`

Ships as `js/plugins/edit_in_popup.min.js` but registers
`MODULES.editInPopup`, not a plugin. `pluginsEnabled` has no say over it, which
is why `FroalaPlugin` has 48 constants for 49 files.

## How to redo any of this

Reading the sources needs nothing: they are under
`demo/node_modules/froala-editor/`, and the registration of every plugin is one
grep (`PLUGINS\.[a-zA-Z_]+ *=`).

The browser measurements need Froala loaded and nothing else — no demo, no Vaadin,
no build, so they cost nothing and cannot disturb a running dev server:

1. `python3 -m http.server 8899` from `/`, so the harness can reference
   `node_modules` by absolute path.
2. A static HTML page that pulls in
   `demo/node_modules/froala-editor/css/froala_editor.pkgd.min.css` and
   `.../js/froala_editor.pkgd.min.js`, and exposes one function that builds an
   editor over a seeded `<div>` with a dummy `key` and a given options object,
   resolving on Froala's `initialized` event, plus one that destroys it again.
3. Drive it with `playwright-core` and `page.evaluate`. The registry
   (`FroalaEditor.PLUGINS`), the command table (`FroalaEditor.COMMANDS`), the
   defaults (`FroalaEditor.DEFAULTS`) and an editor's effective options
   (`editor.opts`) are all readable from the page.

Read a number off the running registry rather than off a file listing — that
distinction is what this whole file is about.
