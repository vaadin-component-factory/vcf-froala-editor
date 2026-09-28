# e2e is independent of the demo

`e2e/` depends on `component/` alone and boots its own app, `E2eApplication`, with its
own test views and its own production bundle (maintainer, 2026-09-28). Until then it
booted the demo and ran in the demo's bundle. That coupling came from the first
scaffold and was never decided. It broke when the demo's `BasicView` stopped using
`Checkbox` and `Select`: Vaadin splits the bundle per route, and the test views had only
worked because the demo's start route happened to load the same components eagerly.
The demo's author has to stay free to change it without breaking a test.

## Consequences

The app and the test views sit in `e2e/src/main/java` although they are test code.
`vaadin-maven-plugin` builds the bundle from the project's compile/runtime classpath and
never scans `target/test-classes` (verified in `FlowModeAbstractMojo.getClasspathElements`,
flow-maven-plugin 24.10.10), so views under `src/test/java` would get no chunk. The module
sets `maven.deploy.skip`, so they never ship.

The demo's `spring-boot-maven-plugin` no longer needs the `exec` classifier, which only
existed so e2e could use the demo jar as a library.
