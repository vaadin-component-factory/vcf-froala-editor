# The component module stays Spring-free

`component/` is the artifact published to the Vaadin Directory, and it depends on
`vaadin-core` alone, per the official add-on guide. A Spring dependency there would be
forced on every consumer of the add-on, including those who do not use Spring at all.
Everything Spring-shaped lives in `demo/` instead.

## Consequences

The rule extends to the module's tests: plain `karibu-testing-v10`, never the `-spring`
artifact, because `MockSpringServlet` would pull Spring back in through the test scope.
The customer's request to read the license key from `application.properties` cannot be
satisfied inside the component either; see ADR-0004.
