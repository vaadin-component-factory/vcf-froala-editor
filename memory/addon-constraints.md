---
name: addon-constraints
description: Non-obvious rules shaping the Froala add-on — Spring-free component module, license-key placement, version-line couplings
metadata:
  type: project
---

Decisions made when the project was scaffolded (2026-08-13) that are not derivable
from the code:

- **`component/` stays Spring-free and parentless.** `vaadin-core` only, its own
  BOM import, no `<parent>` — so it builds and publishes to the Vaadin Directory
  standalone. Anything wired on the reactor root (Spotless, Checkstyle, Lombok if
  ever added) is **not** inherited and must be duplicated into `component/pom.xml`.
- **The Froala license key must be configurable, not hard-coded** (explicit
  customer requirement). That conflicts with the Spring-free rule, so the resolution
  is: plain `setLicenseKey(String)` on the component, Spring `@ConfigurationProperties`
  binding in `demo/` — or later a separate optional `vcf-froala-editor-spring`
  module. Never add Spring to `component/`.
- **Java 17 / Vaadin 24 are deliberate**, chosen as the platform floor to maximize
  the pool of consumers who can use the published add-on. Raising them is a
  decision to record, not a default.
- **Karibu's version line is coupled to the Vaadin line**: 2.4.x is Vaadin 24.8+
  only, 2.6.x+ is Vaadin 25 only. Bumping Vaadin means bumping Karibu.
- **DramaFinder was rejected on purpose** for the e2e module: 1.x is built against
  Vaadin 25 / JUnit 6 / Java 21 and cannot run on this Vaadin 24 / Spring Boot 3 /
  JUnit 5 stack. The e2e module uses plain Playwright with a small hand-written
  base class instead. Revisit only if the project moves to Vaadin 25.
- **Karibu executes no JavaScript.** Any Froala behaviour that depends on the
  editor's JS running can only be covered in `e2e/`, never browserless.
- **Spring Boot 3.5.15 is not "latest 3.5.x"** — it is the version
  `com.vaadin:vaadin-spring:24.10.9` is built against. Derive it from the Vaadin
  release when bumping, don't just take the newest 3.5.x.
- **Spotless's `<pom><sortPom>` block was deliberately left out** of both poms,
  unlike the `setup-code-checks` template. The poms are hand-authored with block
  comments explaining the BOM-not-parent setup and the `exec` classifier; sortPom
  reorders elements and would detach those comments from what they document. Don't
  re-add it while syncing with the code-checks template without deciding that the
  comment churn is acceptable.
- **`component/` resolves the code-checks config via
  `${codechecks.config.dir}` = `${project.basedir}/..`**, not
  `${maven.multiModuleProjectDirectory}` like the root pom. That property points at
  `component/` itself during a standalone build, which would break it. Verified:
  `cd component && mvn clean verify` is green including Spotless + Checkstyle.

See [[project-overview]].
