# Open value sets are String constants, closed ones stay enums

An application can register its own Froala plugin (`FroalaEditor.PLUGINS.myPlugin = …`), its own
theme (a stylesheet for the class `myTheme-theme`) and its own commands. Until 2026-09-29 each of
these had a different way in. `FroalaPlugin` was an enum and left no way for an application's own
plugin besides raw JSON. `FroalaTheme` was an enum with a second method, `withCustomTheme(String)`,
for everything else. Toolbar groups and buttons were already plain strings.

So a value set an application can extend is a final class of `String` constants, and the methods
that take such a value take a plain `String` (maintainer, 2026-09-29). An application's own value
is then just another string in the same call. This covers `FroalaPlugin`, `FroalaTheme`,
`FroalaButton`, `FroalaQuickInsertButton` and `FroalaToolbarGroup`'s group names. There is no
`custom(...)` factory and no second `withCustom…` method.

A closed set, Froala's or the add-on's own, stays an enum, because an application cannot add to
it and the compiler can then catch a typo. These are `FroalaTextDirection`, `FroalaToolbarAlign`,
`FroalaEditorVariant` and `ValueChangeMode` (maintainer, 2026-09-29).

## Consequences

- A typo in an open value is not caught by the compiler. Froala drops an unknown plugin or button
  silently, and an unknown theme just finds no stylesheet. The constants are the protection.
- `FroalaPlugin` holds only the registered name. The file name is known to the client-side
  loader alone, which maps registered names to files and passes any other name on untouched.
- `FroalaTheme.NONE` is the empty string. Froala adds the theme class only when `opts.theme` is
  truthy, and an empty string still overrides the add-on's default `vaadin`.
- `basics()` and `all()` return a new `Set<String>`. `all()` lists the constants by hand, and a
  unit test checks it against the class's fields.
