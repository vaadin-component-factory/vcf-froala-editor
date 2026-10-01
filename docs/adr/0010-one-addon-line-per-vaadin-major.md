# One add-on line per Vaadin major

The add-on for Vaadin 24 does not run on Vaadin 25, and an add-on for 25 does not run on 24.
Vaadin 25 needs Java 21 and Spring Boot 4, and it dropped Flow API that 24 deprecated. So the
add-on keeps one line per Vaadin major (maintainer, 2026-10-01):

- `v1` for Vaadin 24, add-on 1.x, branched off main after the 1.0.0 release.
- main for Vaadin 25, add-on 2.x, with Vaadin 25.3, Java 21 and Spring Boot 4.

Each new Vaadin major gets the next add-on major, starting at x.0.0. When Vaadin 26 arrives, main
moves to add-on 3.x and the 25 line continues in a branch `v2`.

`v1` is not deprecated. It is a separate line only for the technical reason above. Fixes are made
on main and ported back to `v1` where they apply. Whether a new feature goes to `v1` as well is
decided per feature.

The lines differ as little as necessary (maintainer, 2026-10-01). A change that is not forced by
the newer Vaadin version stays out of main, so a fix or a feature can be taken over between the
lines with little rework.

Raising the floor to Vaadin 25 and dropping 24 was rejected. It drops every consumer still on
Vaadin 24 and Java 17, which ADR-0005 set out to keep.

## Consequences

- ADR-0005 applies to `v1` only.
- The test stack stays the same on main: Karibu, plain Playwright, no DramaFinder and no switch to
  Vaadin's own browserless testing, although Vaadin 25 would allow both. Only their versions move
  with Vaadin. Tests can then be taken over between the lines unchanged.
- A release from `v1` leaves the demo server alone. The live demo shows the newest major only.
- CI runs on `v1` as well as on main.
