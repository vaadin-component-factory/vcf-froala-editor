---
name: housekeeping
description: Use when cleaning up leftover resources after tests, UI exploration, server starts, Playwright sessions, or larger changes — invoked as /housekeeping. Runs the deterministic cleanup script (core dumps, JVM attach files, screenshots, Playwright temp dirs, hanging Chromium) and adds judgment checks the script can't do: stop only-idle servers, report Docker containers, run the formatter check, and flag git hygiene. Reports a summary; never modifies source code.
---

# Housekeeping

## Overview

Cleans up leftover development/testing resources and reports a summary. Two layers:

1. **Deterministic** — `housekeeper-cleanup.sh` (core dumps, JVM attach files, Playwright screenshots/temp dirs, stale server log, hanging Chromium/Playwright). Safe to run any time.
2. **Judgment** — checks that need context: don't kill a server in active use, report (not stop) Docker, formatter check, git hygiene.

**The same `housekeeper-cleanup.sh` also runs automatically via the `SessionEnd` hook** as a safety net. This skill is the on-demand, judgment-aware entry point; the hook is the guaranteed end-of-session sweep. A `SessionEnd` hook cannot invoke this skill (hooks run shell commands, not skills) — that is why both share the one script.

## Rules

- **Never modify source code or project files.** Cleanup touches only known temp/artifact locations and stray processes.
- **Never delete `*.sh` scripts in the workspace root** — user-maintained, not in Git, unrecoverable.
- **Never delete Dockerfiles / devcontainer files** outside known temp locations.
- **Don't stop servers that may be in active use** — check first.
- When unsure about an untracked file: **report it, do not delete it.**
- **Never scan/decompile JARs** for API lookups — use the Vaadin MCP tools instead.

## Workflow

### Step 1 — Run the deterministic cleanup script

```bash
/workspace/.claude/scripts/housekeeper-cleanup.sh
```

Capture its output for the report. This handles core dumps, JVM attach files, screenshots, Playwright temp dirs, stale server log, and hanging Chromium/Playwright processes.

### Step 2 — Judgment checks (not covered by the script)

- **Server processes** — look for dev-server PID files in `/tmp/` or the process list. Stop them only via the project scripts (`claude-server-stop.sh`) and only if not in active use.
- **Docker** — `docker ps`; **report** project-related running containers, do not stop them.
- **Formatter** — if a formatter is configured (e.g. `mvn spotless:check`), run the **check** only and report the result. Do **not** auto-fix source.
- **Script hygiene** — verify scripts referenced in `CLAUDE.md`/guidelines (`claude-server-start.sh`, `claude-server-stop.sh`, `claude-print-server-logs.sh`) exist and are executable; flag mismatches between documented and actual behavior (ports, log paths, PID files).
- **Git status** — `git status`; flag untracked files that should be staged or gitignored; warn if `.env`, credentials, or large binaries are staged. Report the current branch; warn if on `main`/`master`.

For larger cleanups you may dispatch the `housekeeper` subagent (`model: haiku`) to run Steps 1–2 and keep the main context lean.

### Step 3 — Report

```
=== HOUSEKEEPING REPORT ===

Cleanup:     [OK | server stopped / N processes terminated]
Formatting:  [OK | issues found | N/A]
Docker:      [OK | containers: ...]
Git:         [OK | notes] — branch: <name>
===========================
```

## Common Mistakes

- **Killing a server still in use.** Check PID/usage before stopping anything.
- **Auto-fixing formatting.** Run the *check*, report it; fixing source is out of scope here.
- **Deleting untracked files on sight.** Report unknowns; only known temp/artifact paths are auto-removed (and only by the script).
