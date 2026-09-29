# The vaadin theme's rules are generated from Froala's stylesheet

Froala's stylesheet hard-codes its colours, more than a thousand colour declarations in 5.4.0's
`froala_editor.pkgd.css`, and its radii, font sizes, shadows and cursors as well. It has almost
no custom properties to hook into. Froala's own `dark.css` does not cover all of them, so it is
no template either. Overrides written by hand would drift with every Froala release.

So `component/src/theme-generator/generate-vaadin-theme.js` repeats every rule that sets one of
these under `.vaadin-theme`, with the value expressed through the add-on's own `--vcf-froala-*`
properties. Every value Lumo has a property for gets one of ours, unless a technical reason
speaks against it (maintainer, 2026-09-28). Each colour is read as a full tone mixed with white.
A grey is the neutral colour mixed with white, a light blue is the accent colour mixed with
white, and the white becomes the background colour. Since Lumo's dark variant swaps these colours,
every mix turns dark with it, and dark mode needs no rules of its own.

## Consequences

- The output, `vcf-froala-theme-vaadin-rules.css`, is committed and never edited by hand. A
  Froala update reruns the generator (see `CLAUDE.md`).
- The properties are set in a small hand-written file, `vcf-froala-theme-vaadin.css`. That is
  the one place a Vaadin 25 mapping (`--vaadin-*`, then `--lumo-*` or `--aura-*`) goes.
- Only longhands are written, such as `border-color` for Froala's `border: 1px solid
  #ccc`. A shorthand would also reset the width, style and `background-clip`, and so break
  Froala's own later rules.
- The repeated rules are one class more specific than Froala's. A later Froala rule that sets the
  same property to a value the theme leaves alone, such as `background: transparent` or
  `cursor: default`, is therefore repeated too, wherever the theme set that property on the same
  class or tag.
- Radii and shadows go to the closest Lumo size, font sizes to Lumo's where Froala's is exactly
  one of them. Grey borders are mixed from their own border colour, and grey text goes to the
  closest Lumo text colour.
- Corners are themed only on surfaces such as the box, popups, buttons and inputs. Other corners,
  such as those of a slider thumb or a pill-shaped toggle, give a widget its shape.
- The heights, spacing and line heights of Froala's toolbar and popups are not themed. Froala
  places dropdown arrows, badges and tooltip text at fixed offsets from them, and those would no
  longer line up. Styles inside the editable content keep their sizes, corners and shadows, because
  they shape the document.
- The editable area, the field states and Vaadin's focus ring are set by hand in
  `vcf-froala-theme-vaadin.css`, not generated, because Froala has no rules for them.
- The theme fills the box like a Vaadin text field and hides Froala's border, because the editor
  sits among Vaadin fields (maintainer, 2026-09-28). Froala's look, the background colour with a border,
  is the theme variant `OUTLINED`. The border keeps its width and only turns transparent, so
  nothing moves between the two. The fill lies over the background colour as an image, because Lumo's
  fill is see-through and a sticky toolbar has to hide the content that scrolls below it. Froala's
  button greys, second toolbar row and row divider are mixed with the background colour and would vanish
  on the fill, so on the filled toolbar they are mixed with transparent instead.
- Froala's darker hover shades of a colour collapse onto the colour itself. A shade mixed with
  black would disappear on a dark background.
- Hues Lumo has no colour for, such as the purple and teal of the track changes and collaboration
  plugins, keep their tone but mix it against the background colour.
