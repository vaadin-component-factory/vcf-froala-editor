# spring-boot-dependencies as a BOM, not as a parent

The reactor root imports `spring-boot-dependencies` as a BOM rather than inheriting
from `spring-boot-starter-parent`, so the project can adopt a corporate parent later
without unpicking Spring Boot's.

## Consequences

A BOM manages dependency versions only. The root therefore pins plugin versions itself
(`spring-boot-maven-plugin` and its `repackage` execution, surefire, failsafe) and sets
its own compiler configuration, none of which would need stating under the parent.
