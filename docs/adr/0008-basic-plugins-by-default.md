# An editor without plugin options gets a basic rich-text editor

Froala enables every plugin registered on the page when `pluginsEnabled` is not set. With the
packaged bundle that meant 46 plugins, among them some that need a server, a second library or a
paid service, and whose buttons then did nothing. A first rule (2026-08-27) switched off only
those. It still left 41 plugins on, including menus that offer nothing but Froala's sample styles
("Big Red", "Small Blue"), counters below the editor, and track changes, which adds its five buttons
to every toolbar, including one the application set itself.

So an editor without `pluginsEnabled` gets `FroalaPlugin.basics()`, a basic rich-text editor of
14 plugins: text and paragraph formats, lists, quotes, links, find and replace, the shortcut
dialog, links typed as URLs and cleaning of text pasted from Word (maintainer, 2026-09-29).
Nothing that inserts other content, not even tables. The application adds what it needs to the
set `basics()` returns, or passes `FroalaPlugin.all()`.

## Consequences

- An application that relied on the packaged bundle's plugins without naming them loses buttons,
  such as those for images and tables. Adding them back is one `plugins.add(...)` each.
- The list lives in Java only. `FroalaEditor` sends it with every editor, in an element property
  of its own, so the options on the element stay what the application set.
- Which plugins count as basic is taste, and was decided as such. A plugin moves into or out of
  the set only by a decision recorded here.
