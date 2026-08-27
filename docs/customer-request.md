# Customer request — NST (verbatim)

Source: customer request forwarded to Vaadin, received before 2026-08-13.
This is the **original wording**; do not edit it. Interpretations, scope cuts and
decisions belong in `ROADMAP.md`.

---

> As part of NST application development we would like to integrate the Froala
> WYSIWYG Editor (currently v4.6.2, https://froala.com/wysiwyg-editor) as one of
> our rich‑text editing component. To keep our application code clean and to speed
> up development, we would like to request an official Vaadin connector / add‑on
> that wraps Froala as java flow component.
>
> - Recommend to make the license key configurable, not hard‑coded (ideally read
>   from Spring config (application.properties))
> - We would like the connector to expose Froala's full/maximum possible feature
>   set — including rich‑text formatting (fonts, colors, styles, lists, tables,
>   quotes, code view), media and content insertion (images, files, links, emoji,
>   special characters), advanced editing modes (inline, document, full‑screen),
>   productivity features (paste‑from‑Word, markdown, find‑and‑replace,
>   word/character count, track changes, mentions, templates), localization/RTL and
>   accessibility, plus HTML sanitization and configurable image/file upload —
>   through a clean Java Flow API.
> - Appreciate if it can be forward-compatible with the upcoming 5.x series

---

## Measured surface of what "full feature set" means

Taken from the published npm packages, not from memory. The 4.6.2 column was
measured on 2026-08-13; the 5.4.0 column was re-measured on **2026-08-21** against
the package actually installed in `demo/node_modules`, which is the version this
connector targets.

| | froala-editor 4.6.2 | froala-editor 5.4.0 |
|---|---|---|
| Released | 2025-09-03 | 2026-08-19 |
| Plugins (`js/plugins/*.min.js`) | 42 | 49 |
| Options (`FroalaOptions` in `index.d.ts`) | — | **322** |
| Language files | 39 | 39 |
| Theme/plugin CSS files (non-minified) | — | 37 |

The option count is top-level members of the `FroalaOptions` interface, counted by
brace-matching the interface body — nested option objects are not counted twice.
It grew from 302 in 5.3.1 to 322 in 5.4.0, i.e. the surface is still moving; treat
any single number as a snapshot, not a constant.

New in 5.x, nothing removed: `ai_assist`, `code_snippet`, `collaborative` (yjs),
`export_to_word`, `import_from_word`, `link_to_anchor`, `page_break`.

**The request is ~11 months stale on the version:** 5.0.0 shipped 2026-01-15 and
5.4.0 is current. "Forward-compatible with the upcoming 5.x" is no longer a
forward-looking requirement — it was a *target-version decision*, and it has been
made: **this connector targets 5.4.0** (decided 2026-08-21, see Phase 5 in
`ROADMAP.md`). NST's sign-off is still open, since their request names 4.6.2.

**"mentions" is not a standalone Froala plugin — but it is not entirely absent
either.** Corrected 2026-08-21 after reading `index.d.ts`:

- There is no `mention` plugin file in either version's plugin folder, and Froala
  documents in-document mentions as a third-party integration (Tribute.js example).
- The `collaborative` plugin (465 KB, new in 5.x) *does* carry
  `mentionableUsers: Array<{id, name}>`, documented as "Users available for
  @mention inside comments", and there is a `mention` entry in the UI-label map.

So: **@mention inside comments ships; @mention in the document body does not.**
Which one NST means is an open question — and it matters, because the shipping one
comes bundled with a whole collaboration stack (Yjs, `docId`, `commentsUrl`,
`suggestionsUrl`, roles), not as an isolated feature.

**"templates" is not a Froala feature at all.** There is no document-template
plugin. `FroalaEditor.RegisterTemplate`, `ICON_TEMPLATES` and `POPUP_TEMPLATES` are
icon and popup markup templates — a different thing entirely. Document templates
would be custom work on our side, like in-document mentions.

**The license key option is `key`, not `apiKey`** — corrected 2026-08-21. Both are
typed in `index.d.ts` (`apiKey` line 1144, `key` line 1256), which is what caused
the earlier mix-up, but the runtime is unambiguous: the license module reads
`editor.opts.key`, while the only three uses of `apiKey` in
`froala_editor.pkgd.min.js` pass it to `gapi.client.init` for the Google Drive file
manager. `setLicenseKey(String)` maps onto `key`.

`key` also accepts an array of keys, not just a string (the runtime wraps a plain
string into one). Not exposed for now — a single key covers the customer's case.

---

## Open questions back to NST

Six questions. Each one changes what gets built, so they are written out with
Froala's actual situation next to them and can be asked without knowing Froala's
plugin list. The measurements behind them are in the section above.

### 1. What does "mentions" mean?

Froala offers two unrelated things under that word:

| Reading | What the user sees | What Froala provides |
|---|---|---|
| **In-document mentions** | Typing `@` while writing opens a name picker and inserts a reference into the text | Nothing. No plugin exists. Trigger detection, the dropdown and a server-side feed of candidate users would be built from scratch |
| **Mentions in review comments** | `@name` inside a comment attached to the document | Ships in the `collaborative` plugin (`mentionableUsers`) — which is a collaboration stack: Yjs, a `docId`, `commentsUrl` and `suggestionsUrl` endpoints, a role model |

The second reading brings infrastructure with it, not just work. **Which one is
meant?**

### 2. What does "templates" mean?

Froala has no document-template feature. The `RegisterTemplate` and
`ICON_TEMPLATES` names in its API are markup for Froala's own icons and popups, not
prepared documents. So this is custom work either way, and the two readings differ
in size:

- *Insert a stored HTML snippet at the caret* — a list of snippets and an insert call.
- *A document skeleton with placeholders that get filled in* — a variable
  substitution system.

**Which one is meant?**

### 3. Full feature set, or a first cut?

The request says "maximum feature set". Building all of it takes noticeably longer
than a first release covering rich-text formatting, media, upload and localization.
**Is a staged delivery acceptable, and if so, what has to be in the first stage?**

### 4. Which Vaadin version does the NST application run?

This add-on targets Vaadin 24, the platform floor, so it excludes as few consumers
as possible. If NST is on 25, several versions move together — Vaadin, the
browserless test library, the Java baseline. **Which version is NST on?**

### 5. Are track changes and mentions needed on day one?

They are the two largest items on the list. **Can they be in a later stage?**

### 6. Which of the service-backed plugins are needed?

Froala ships 49 plugins. 41 need nothing but a browser, or only the image and file
upload endpoint this add-on provides. The other eight need a server or a paid
service that is not part of the add-on:

| Plugin | What it gives the user | What it requires |
|---|---|---|
| `collaborative` | Several people editing one document at once, review comments, version history | Froala's collaboration server, plus `docId`, `commentsUrl` and `suggestionsUrl` endpoints and a role model. Also where comment mentions live — see question 1 |
| `ai_assist` | Rewrite, change tone, translate, chat about the text | An AI endpoint |
| `filestack` | File upload through Filestack | A Filestack account |
| `spell_checker` | Spell checking as you type | A SCAYT subscription |
| `import_from_word` | Open a .docx in the editor | Froala's conversion service |
| `export_to_word` | Save the document as .docx | Froala's conversion service |
| `save` | The editor posts its content to a URL on a timer | A save endpoint. Overlaps with what a Flow application already does through the value change listener |

All eight ship either way, so switching one on later costs nothing. The question is
what is on by default: a visible button with no service behind it does nothing when
clicked. **Which of these does NST need, and is the infrastructure for them in
place?**

---

Not open any more, recorded here because the request touches them: the add-on builds
against **Froala 5.4.0**, not the 4.6.2 the request names, and a consuming application
supplies its own license key through `setLicenseKey(String)` — the add-on ships none.
