# Froala Editor for Vaadin Flow

The domain glossary for this project: the ubiquitous language. Class names, method
names, test names and conversation all use these words for these things.

## Language

**Editor**:
The Froala instance bound into Flow as a Vaadin field, with a value, validation and
the field parts around it.

**Viewer**:
A read-only rendering of editor content. It is a component, not a field: it has no
value binding and nothing can be typed into it.
_Avoid_: read-only editor, preview

**Delta**:
A change to a value, sent instead of the whole value. The client describes what it
changed and the server applies it, so a large document does not cross the wire on
every keystroke.
_Avoid_: patch, diff, fragment

**Test view**:
A view that a browser test owns and is free to change. Distinct from a demo view,
which exists to show the add-on off and whose author must stay free to change it.
_Avoid_: fixture view

**Browserless**:
The test tier that exercises the server-side component with no browser and no
JavaScript, between unit tests and browser tests.
_Avoid_: integration test, UI unit test

**Plugin**:
A Froala plugin: one unit of the editor's own feature set.
_Avoid_: bare "plugin" for a Maven or Claude Code plugin, which are always written out

## Relationships

- An **Editor** holds a value; the client reports changes to it as **Deltas**
- A **Viewer** renders what an **Editor** produces, without editing it
- A **Plugin** adds behaviour to an **Editor**, never to a **Viewer**
- Browser tests drive **Test views**; **Browserless** tests build the component directly

## Flagged ambiguities

- "plugin" meant three different things (Froala plugin, Maven plugin, Claude Code
  plugin) and was answered wrongly three times. Resolved: bare **Plugin** is always
  Froala's; the other two are always written out in full.
- "fixture view" and "test view" both named the same thing, the docs preferring the
  first and the classes the second. Resolved: **Test view**, matching
  `FroalaTestView` and its siblings.
