---
name: compact-ready
description: Use right before compacting the context — when the user asks "compact ready?", "bereit für compact?", "ready to compact?", "können wir compacten?", is about to run /compact, or when the conversation is getting long and should be safely compacted. Invoked as /compact-ready or by those natural-language questions. Captures the volatile session state — current goal, decisions and their rationale, next steps, open todos, blockers, gotchas learned — and writes it into the project's existing durable artifacts (its issue tracker or progress file, CONTEXT.md, docs/adr/, CLAUDE.md) so a fresh agent can resume after a compact with no information loss. This skill DOES edit those files, and it MAY commit the progress artifact it just updated before the compact. It never touches source code.
---

# Compact-Ready

## Overview

A compaction discards the live conversation. Anything that matters but lives
**only** in this chat — the current goal and *why*, decisions made and the options
rejected, the next concrete step, open todos, blockers, gotchas learned — is lost
unless written down first. `/compact-ready` harvests that state into the project's
**existing** durable artifacts so the user can immediately compact.

**The test — the fresh-agent test:**

> A new agent, with **zero memory of this conversation**, must be able to read the
> project's durable files and resume the exact next step. If resuming would need
> anything that lives only in this chat, capture it before declaring ready.

This is an action skill: the user expects the files **updated**, not a list of gaps
to fix themselves.

## Not `/handoff`

`/handoff` (from `mattpocock-skills`) fires at the same moment — its own
description is "compact the current conversation into a handoff document" — so the
difference is not the occasion but what survives. `/handoff` writes one document to
the OS temp directory, outside the repo, and never commits; it also has
`disable-model-invocation: true`, so it only runs when the user types it. This skill
writes the state into **the project's own files**, may commit that capture, and
activates on its own when the user asks whether they can compact. Ephemeral note for
whoever picks the thread up next → `/handoff`. State the repo should still hold
tomorrow → this.

## What to capture, and where it goes

Route each piece by **lifespan** — this routing is the whole job:

| State (lives only in the chat) | Goes to |
|---|---|
| Current goal + why; next step(s); open todos; blockers / open questions | The project's **progress artifact**. In a project set up by `/setup-matt-pocock-skills` that is the issue tracker named in `docs/agents/issue-tracker.md` — a real tracker, or the local ticket files — which is where `/to-spec` and `/to-tickets` already put in-flight work. Otherwise whatever status/plan file the project keeps. |
| Decisions + rationale + **rejected alternatives** | The progress artifact while the choice is still in flight. Once it **holds**: an ADR under `docs/adr/` (or whatever file the project keeps decisions in). Rejected alternatives stay with the ticket. |
| Gotchas / constraints learned that will always hold | `CONTEXT.md` for domain knowledge, `CLAUDE.md` for a standing instruction. Neither is pre-seeded — create it only when there is something real to put in it. |

Already obvious from the code, the git diff, or existing docs? **Don't re-record it.**

**A progress artifact with its own rules overrides this table.** If a file states
how it may be edited — a contract section, a ticket template — follow that instead:
put the statuses and open questions there in its own format, and route the rest
(rationale, rejected routes, learnings) to an ADR or a note. Do not append a session
report to a file that says it is not a log.

## Workflow

1. **Inventory the live state.** List, for yourself, what a fresh agent would be
   missing: goal + why, decisions + rationale + rejected options, next step(s),
   open todos (read the live todo list), blockers, gotchas learned this session.
2. **Inventory the project's artifacts.** `ls`; read `docs/agents/issue-tracker.md`
   if it exists (it names where tickets live); check `docs/`, `docs/adr/`,
   `CONTEXT.md`, and grep for any STATUS/PROGRESS/JOURNAL/TODO file. Decide which
   one is the project's home for in-flight progress. Run `git status`/`git diff
   --name-only` to ground "what changed".
3. **Write each piece into the right artifact.** Edit in place, tightly. Mark done
   tickets/plan steps; record decisions with their *why*; reconcile any spec that
   drifted from reality. Dates absolute — today's date, never "today".
4. **Verify and report.** Re-read what you wrote; apply the fresh-agent test
   literally. Fix any gap. Then report (format below). If you *can't* make it pass
   (e.g. the user must answer an open question first), say so and name what's still
   only in the chat — don't claim ready.

## Keep it clean

- **Main agent only** — never delegate the capture to a subagent; the state lives
  in *this* conversation, which a subagent doesn't have.
- **Route transient progress to the progress artifact, not `CLAUDE.md`.** CLAUDE.md
  is standing instructions; "currently doing X" there is noise. (This is the
  skill's most common failure.)
- **The harness's own memory** (`~/.claude/…/memory/`) is fair game for a durable
  fact, but it is per-user and outside the repo: it never substitutes for writing
  the state into the project's files, which is the whole point here.
- **Capture the *why*, not just the *what*** — a decision without its rationale
  gets re-litigated after the compact.
- **Distill, don't transcribe** — the conclusion and next step, not the chat log.
- **Adapt to what exists** — only create a new note when the project has no
  progress home, and keep it minimal. Don't impose a `STATUS.md`.
- **A decisions file is one line per decision.** What holds, dated, newest first —
  the story of how it was found goes in the progress artifact or nowhere. If such a
  file is drifting past ~60 lines, say so in the report instead of adding to it.
- **Never edit source code.**
- **Committing the capture is allowed, and preferred.** The progress artifact you
  just updated may be committed before the compact, because an uncommitted capture
  is the one thing a compact cannot protect. Scope it tightly: only the files this
  skill wrote, one commit, a message naming what was captured. Nothing else gets
  swept in. `CLAUDE.md`'s *Review before the gate, review before the commit* does
  not apply to a pure status capture — there is no code in it to review. Pushing
  stays forbidden.

## Report Format

```
=== COMPACT-READY ===

Captured the session state into:
  - <file>          — <what was added/updated>

Resume point for a fresh agent: <the very next action>
Open blockers/questions: <list, or "none">

Fresh-agent test: PASS — safe to /compact.
=====================
```
