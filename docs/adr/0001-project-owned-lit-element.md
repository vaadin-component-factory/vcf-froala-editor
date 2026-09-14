# A project-owned Lit element, not Flow-side Element API calls

Froala could have been driven entirely from Java through Flow's Element API. Instead
the add-on ships its own `vcf-froala-editor` Lit element, composed with Vaadin's field
mixins (`FieldMixin`, `ThemableMixin`, `ElementMixin`, `FocusMixin`, `PolylitMixin`,
`SlotStylesMixin`), which instantiates Froala on a plain `<div>` in its default slot.
Two reasons: the field parts (label, helper, error message, required indicator) and the
Lumo `inputFieldShared` styles then come for free, so the editor looks and validates
like a native Vaadin field; and every Froala-specific call is confined to one file,
which is what a future Froala major version upgrade has to touch.
