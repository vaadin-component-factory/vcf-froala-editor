# Theming

How the editor is made to look like a Vaadin component. Written **before** the
implementation, so every requirement is `unverified` until its test exists.

## The mechanism

- **THM-1** The add-on ships a **Froala theme named `vaadin`**, set through Froala's
  own `theme` option. It is **not** a Vaadin theme variant.
- **THM-2** Vaadin's `HasThemeVariant` was considered and dropped on 2026-08-27.
  `addThemeVariants` can be called at runtime, and Froala cannot change a theme at
  runtime — measured: writing `editor.opts.theme` on a live editor leaves the box
  class untouched, and Froala has no API with `theme` in its name. A variant that
  can be set but only half applies breaks the interface's contract. Using Froala's
  mechanism also means an incompatible pairing like `gray` plus `vaadin` cannot be
  built by accident: there is one slot.
- **THM-3** Froala stamps its theme as a class on the box **and on the popups,
  tooltip, modal and overlay it appends to `<body>`**. That is why the theme has to
  be Froala's: those elements are outside the component, so a `theme~="vaadin"`
  attribute on the host could never reach them.
- **THM-4** `theme` is an init option (CFG-1). Switching the theme on a live editor
  is **not supported** in phase 2.
- **THM-5** The `vaadin` theme is **active by default**. An application that wants
  plain Froala sets `theme` to one of Froala's own values or to none.
- **THM-6** `theme` is not a free string in `FroalaOptions`. It is an enum with the
  known values (`VAADIN`, `DARK`, `GRAY`, `ROYAL`) plus an escape for a custom one,
  so a typo cannot silently produce an unstyled editor.

## The stylesheet

- **THM-7** Two layers, following `stefanuebe/vaadin-fullcalendar`'s
  `full-calendar-theme-vaadin.css`:
  1. our own custom properties on `html`, mapped from Lumo (`--lumo-*`) with
     `color-mix` derivations for the tints Froala needs;
  2. those properties written into Froala's `.fr-*` rules.

  An application can retheme the editor by overriding our properties, without
  touching Lumo. FullCalendar has a third layer — feeding the library's own
  `--fc-*` variables — which does not exist here: Froala's stylesheet has
  effectively no custom properties (one `var()` in 10,446 lines) and hard-codes
  164 distinct colours.
- **THM-8** Dark mode is not implemented separately. Lumo's properties change under
  Vaadin's `theme="dark"`, so the mapping in THM-7 follows.
- **THM-9** Specificity is won the way Froala's own theme files win it — the theme
  class is prepended to Froala's selector (`.vaadin-theme.fr-box.fr-basic
  .fr-element`), not fought with `!important`.
- **THM-10** Coverage matters because of THM-5. A rule that is not overridden keeps
  its hard-coded light colour, which under Lumo dark means dark text on a dark
  background — worse than not theming at all. Measured surface: **791 selectors**
  carry a colour, in 1065 declarations (424 background, 304 text, 236 border, 57
  shadow, 34 SVG fill). Excluding the plugins for AI, version history, files manager
  and track changes leaves **404**. The set shrinks with whatever phase 2 decides to
  ship as plugins.

## Known gaps

- **THM-11** Froala keeps the tooltip, modal and overlay in a per-document `shared`
  object and builds them once, with the theme class baked in. Two editors with
  different themes on the same page therefore share those elements and the first one
  built wins. Measured: even a newly opened popup kept the first editor's theme
  class.
