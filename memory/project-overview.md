---
name: project-overview
description: What the Froala Editor Flow add-on is, its stack, and its state-management approach
metadata:
  type: project
---

**Froala Editor for Vaadin Flow** (`vcf-froala-editor`) — a Vaadin Component
Factory add-on wrapping the Froala WYSIWYG Editor (v4.6.2, to stay
forward-compatible with 5.x) as a Java Flow component. Requested for the NST
application: the customer wants Froala's full feature set (formatting, media,
inline/document/fullscreen modes, productivity plugins, l10n/RTL/a11y, HTML
sanitization, configurable upload) behind a clean Java API, with a configurable —
not hard-coded — license key.

As of 2026-08-13 only the scaffolding exists; `GreetingComponent`/`GreetingView`
are placeholders. Progress is tracked in `ROADMAP.md`.

Stack: Vaadin 24.10.9 (Core), Spring Boot 3.5.15, JDK 17+. Base package
`com.vaadin.componentfactory.froala`. Verify gate: `mvn clean verify -Pproduction`.

State management: **classic state (component fields / Spring beans)**. Signals are
a Vaadin 25 feature and are out of scope here.

See [[addon-constraints]] for the rules that shape changes, and `CLAUDE.md` for
module structure, testing, and conventions.
