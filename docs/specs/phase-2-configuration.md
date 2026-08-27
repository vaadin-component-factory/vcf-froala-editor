# Configuration

How Froala's options reach the editor. Written **before** the implementation, so
every requirement is `unverified` until its test exists.

## No runtime options

- **CFG-1** `FroalaOptions` is an **init object**. It is applied when Froala is
  built and is not changed on a live editor.

  This follows Froala, not a preference of ours. Froala's own answer to "can I
  change an option after initializing" is *destroy the instance and initialize it
  again; for a few options it also works on the fly* — with no list of which few.
  The `option()` method that v1 had was removed in v2 and never replaced
  (froala/wysiwyg-editor#879, open since 2015).
- **CFG-2** Reviewed 2026-08-27 against what a Flow application would realistically
  change on a **live, focused** editor. The answer is **nothing**:

  | Candidate | Why it is not one |
  |---|---|
  | `height`, `width`, `heightMin/Max` | The editor is wrapped as a Vaadin field; the field gives the height and our styling makes it flexible. A Froala height fights it. |
  | `placeholderText` | Does not change while a form is open. If someone needs it, that is a feature request with a case behind it. |
  | `language`, `direction` | Follow the user's locale, which does not change mid-session. If it does, a rebuild is acceptable. |
  | `theme` | See `phase-2-theming.md` — init only, and Froala cannot switch it at runtime anyway. |
  | `toolbarButtons` | Configured once. A read-only view is `setReadOnly`/`setEnabled` plus CSS, not a different toolbar. |
  | `charCounterMax`, `wordCounterMax` | A hard input limit, set with the field. |

- **CFG-3** Because of CFG-2, no option needs a per-option "is this live?"
  classification, no refresh dispatch, and no second attach-timing flag. Calling
  `setOptions` on an attached editor **rebuilds** the editor.
- **CFG-4** A rebuild keeps the value and nothing else — caret, selection, scroll
  and the undo stack start fresh. This is the same trade a detach/re-attach already
  makes (`phase-1-lifecycle.md`, LC-6 *Decided*), for the same reason.
- **CFG-5** Several `setOptions` calls in one server round trip cause **one**
  rebuild, batched in `beforeClientResponse`. Reuse `FroalaEditor.liveOnClient`,
  which already flips there (`phase-1-value-transfer.md`, VT-11) — do not add a
  second flag with attach timing.

## Shape

- **CFG-6** `FroalaOptions` is an immutable object with a `with…` style builder (a
  record plus Lombok `@With`), typed per option. It is **not** a `FroalaBuilder`:
  the object is the thing that is passed around and stored, the builder is only how
  it is written.
- **CFG-7** `FroalaOptions` serializes to the raw JSON Froala expects. That
  serialization is the single path — every other entry point below ends in the same
  string.
- **CFG-8** Entry points: a `FroalaEditor(FroalaOptions)` constructor, and
  `setOptions` in three overloads — `FroalaOptions`, `JsonObject`, `String`.
- **CFG-9** There is **no `setOption(String, Object)`**. The `JsonObject` and
  `String` overloads are the escape hatch for anything not yet typed, and a single
  option is a one-entry object. A per-key setter would be a fourth way to say the
  same thing and would need its own merge order against the other three.
- **CFG-10** This answers the `initialConfig` / `rawInitialConfig` split carried
  over from `hugerte` and `vaadin-fullcalendar` (`ROADMAP.md`, phase 1 deferrals):
  the split is the typed object versus the `JsonObject`/`String` overloads, on one
  channel, not two fields.

## Precedence

- **CFG-11** Options are applied first, **our own setters after them**. Where the
  add-on owns a setter for something Froala also has as an option, the setter wins.
  Documented on both.
- **CFG-12** The add-on owns `setValueChangeMode`, `setValueChangeTimeout` and
  `setIntervalPeriod`. `setValueChangeTimeout` **is** Froala's `typingTimer`
  (`phase-1-value-change-modes.md`, VCM-7) and stays a setter: the other two are
  setters, other Vaadin fields carry the same names, and the option belongs to the
  delta channel, which is ours.
- **CFG-13** The constructor no longer writes the three value-change defaults as
  element properties — removed 2026-08-27. They restated the client's own defaults
  (`"change"`, `500`, `2000`) and the getters fall back to the same constants, so
  nothing changed behaviourally. What it buys: a `typingTimer` coming in through
  CFG-8 is no longer overwritten unconditionally, so exposing it as a deprecated
  option is possible if we choose to.

## Toolbar, plugins and what is loaded

Measured 2026-08-27 against froala-editor 5.4.0; the demo views `/check-toolbar`,
`/check-upload` and `/check-on-demand` show the same results in a browser.

- **CFG-14** A toolbar group name is a **free string**, and it is also the command
  name of the group's overflow button. If a group holds more buttons than its
  `buttonsVisible` and no command is registered under the group's name, the
  overflow buttons are rendered into a collapsed panel that nothing can open. The
  API therefore takes strings and **says this**, rather than restricting the name to
  Froala's four `more…` groups. *unverified*
- **CFG-15** A plugin has **two names**: the file (`font_family.min.js`) and the
  name it registers itself under, which is what `pluginsEnabled` takes
  (`fontFamily`). They differ for 20 of the 49, `track_changes` keeps its underscore
  where every other multi-word plugin is camel-cased, and `edit_in_popup` registers
  no plugin at all. The Java side carries both per plugin; neither is derived from
  the other. *unverified*
- **CFG-16** Plugins and language files are loaded **on demand**, with a dynamic
  `import()` per file, on top of Froala's core. All 49 plugins and all 39 language
  files stay shipped; a given editor downloads what its configuration names. This
  replaces the packaged `froala_editor.pkgd.min.js`, and it replaces it completely —
  core and packaged bundle on one page are two module instances with two separate
  plugin registries. *unverified*
- **CFG-17** Upload is **off until a URL is configured**. With no URL Froala uploads
  nothing — it inserts a `blob:` URL, which is valid only in the tab that created it,
  so the value the server stores points at nothing after a reload. A silently broken
  document is worse than a missing button. *unverified*

## Known gaps

- `saveInterval: 0` — Froala's `save` plugin schedules a POST to `saveURL` 10 s
  after every `contentChanged`, which then fails on the missing URL. Nobody listens,
  so it is dead work per edit rather than a defect. Turn it off here.
- Two Froala defaults reach servers that are not the application's:
  `imageManagerLoadURL` (`https://i.froala.com/load-files`, reached only if
  `imageManager` is added to `imageInsertButtons`) and `emoticonsUseImage`
  (`cdnjs.cloudflare.com`, reached out of the box because `emoticons` is in the
  default toolbar, and it leaves a cdnjs URL in the stored HTML). Decide the
  defaults for both when the option surface is typed.
