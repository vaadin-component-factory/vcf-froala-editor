# Specifications

One file per behaviour area, stating what the component **guarantees** — not how it
is built (that is the code) and not what is planned (that is `ROADMAP.md`).

These were written **after** phase 1 was implemented, so they describe delivered
behaviour rather than intent. From phase 2 on, the spec is written first.

**Conventions**

- Every requirement has a stable id (`VT-3`, `LC-2`, …). Ids are never reused; a
  dropped requirement is struck through, not deleted.
- Every requirement names the test that proves it, or says **unverified**. A
  requirement without a test is a claim, not a guarantee — the point of naming it is
  that the gap is visible.
- Only behaviour that is observable from outside belongs here. Internal fields and
  private helpers do not.
- Contradiction between spec and code is a bug in one of them. Say which.
- e2e tests run against **fixture views the tests own**, under
  `e2e/src/test/java/.../it/views/`, never against the demo. The demo exists to show
  the add-on off and its author has to stay free to change labels, values and layout;
  a test that asserts against it breaks for reasons that are not regressions.

**Files**

- [`phase-1-value-transfer.md`](phase-1-value-transfer.md) — how a value moves
  between client and server, and what happens when the two drift apart
- [`phase-1-value-change-modes.md`](phase-1-value-change-modes.md) — when a change
  is reported, the four modes, the throttle
- [`phase-1-lifecycle.md`](phase-1-lifecycle.md) — attach, detach, re-attach, and
  what Froala's asynchronous init means for every setter
- [`phase-1-component-api.md`](phase-1-component-api.md) — the Vaadin field
  contract, license key, read-only/disabled, focus, `FroalaViewer`
