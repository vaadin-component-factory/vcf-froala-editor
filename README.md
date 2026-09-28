# Froala Editor for Vaadin Flow

A Vaadin Flow add-on that wraps the [Froala WYSIWYG Editor](https://froala.com/wysiwyg-editor/)
as a Java component.

## Known issues

### The quick-insert button can be covered

Froala's quick-insert button (the `+` on an empty line) goes to the left of the editor box
whenever there is at least its own width of room between the editor and the **page** edge.
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
editor the button covers the start of the placeholder text. The add-on
cannot offer a custom property for this: the right value depends on the layout around
each editor. If you need it to vary, use a property of your own:

```css
.fr-quick-insert {
  left: var(--my-quick-insert-left, 8px) !important;
}
```

Alternatively, leave enough room to the left of the editor inside its container, or switch
the `quickInsert` plugin off.
