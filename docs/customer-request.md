# Customer request — NST (verbatim)

Source: customer request forwarded to Vaadin, received before 2026-08-13.
This is the **original wording**; do not edit it. Interpretations, scope cuts and
decisions belong in `ROADMAP.md` and `memory/addon-constraints.md`.

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

Taken from the published npm packages on 2026-08-13, not from memory:

| | froala-editor 4.6.2 | froala-editor 5.3.1 |
|---|---|---|
| Released | 2025-09-03 | 2026-07-15 |
| Plugins (`js/plugins/*.min.js`) | 42 | 49 |
| Options (`FroalaOptions` in `index.d.ts`) | — | **302** |
| Language files | 39 | 39 |
| Theme/plugin CSS files | — | 37 |

New in 5.x, nothing removed: `ai_assist`, `code_snippet`, `collaborative` (yjs),
`export_to_word`, `import_from_word`, `link_to_anchor`, `page_break`.

**The request is ~11 months stale on the version:** 5.0.0 shipped 2026-01-15 and
5.3.1 is current. "Forward-compatible with the upcoming 5.x" is no longer a
forward-looking requirement — it is a *target-version decision* (see the open
question in `ROADMAP.md`).

**"mentions" is not a Froala plugin.** There is no `mention` in either plugin
folder. Froala documents mentions as a third-party integration (Tribute.js
example). For this connector it is custom work — a client-side trigger plus a
server-side data feed — not a plugin toggle. Estimate accordingly.

The `apiKey` option is the license key; `setLicenseKey(String)` maps onto it.
