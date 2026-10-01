# Java 17 and Vaadin 24 are the floor

Applies to the `v1` line only since 2026-10-01. main targets Vaadin 25 (ADR-0010).

The add-on targets Java 17 and Vaadin 24 deliberately, not because newer versions are
unavailable. They are the platform's floor, so the published artifact excludes as few
consumers as possible. Raising either narrows the audience and needs a written reason.

## Consequences

Several things that look like separate choices are only this decision showing through:

- **No Signals.** They are a Vaadin 25 feature. State is held in component fields and
  Spring beans, and UI updates are wired explicitly.
- **No DramaFinder** for browser tests. Its 1.x line is built against Vaadin 25,
  JUnit 6 and Java 21. Plain Playwright is used instead.
- **Karibu pinned to the 2.4.x line.** 2.4.x is Vaadin 24.8+ only and 2.6.x+ is
  Vaadin 25 only, so the line has to move together with the Vaadin version.
