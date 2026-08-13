---
name: hugerte-prior-art
description: parttio/hugerte-for-flow is the reference implementation for delta value transfer — Apache-2.0, portable, with three known weaknesses
metadata:
  type: reference
---

<https://github.com/parttio/hugerte-for-flow> — a Vaadin Flow wrapper around
HugeRTE (TinyMCE fork), **Apache-2.0**, i.e. the same license as this add-on, so
code can be ported with attribution.

Relevant because it already solves the problem NST raised: **large HTML values
must not be sent whole on every change.** Its delta mechanism (verified
2026-08-13):

- client `vaadin-huge-rte.js` keeps `_lastSyncedValue`, does
  `dmp.patch_make(last, current)` → `patch_toText`, fires a `_value-delta`
  CustomEvent, throttled at 50 ms
- server `HugeRte.applyDelta(old, delta)` = `patchFromText` + `patchApply`
- `org.bitbucket.cowwoc:diff-match-patch:1.2` + npm `diff-match-patch@1.0.5`

Port it rather than inventing it, but fix three things:

1. It **ignores the patch-apply result flags** (`results[1]`) — a patch that does
   not apply cleanly corrupts the value silently. Check them, resync fully on
   failure.
2. **Server → client remains full HTML.** Only client → server is compressed.
3. Its config layer is Jackson 3 (`tools.jackson`) on **Vaadin 25 / JDK 21** —
   not portable to this Vaadin 24 / JDK 17 stack. Only the delta code ports.

It is also a useful shape reference generally (enums for `Plugin`/`Toolbar`/
`Language`, `AbstractSinglePropertyField<_, String>`, engine checked into
`META-INF/resources/assets/` because Vaadin's Spring Security whitelists
`/assets/**` but not `/frontend/**`). See [[addon-constraints]].
