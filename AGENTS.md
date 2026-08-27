# IMPORTANT: General rules and conventions for you (agent)

- Code and document for humans. They have to understand and maintain the
  application.
- Don't guess, confirm with docs / sources / research results / MCP. Being
  uncertain is fine and saying so is fine; stating an assumption as fact is not.
- If there is no solution or answer, say it. Acknowledging failure is better than
  trying to hide it.
- Review your own output with subagents — see *Verifying your output* for when and
  how.
- **Answer the question you were asked before you edit anything.** A question about
  finished work wants an answer, not a rewrite.
- The code is not your history / changelog. 
- Never silently revert or tidy away something in the workspace you cannot
  explain. Ask, or leave it.
- Follow clean code and yagni principles.
- Don't invent or work around something that already has a solution. Use the
  existing libs before adding new ones.
- Use plain and clear language in your answers, don't try to sound creative, keep it simple.


## Verifying your outcome / work

- **Agent review is part of every phase, before the commit.** Not optional and not
  something to wait to be asked for: once a phase's code is written and the tests
  are green, dispatch parallel review agents (see the model table under *Working conventions*) *before*
  offering the commit, and report what they found. Use several agents on separate
  axes rather than one general one — for a component wrapper that means at minimum
  the framework/API axis, the wrapped-library axis, and the test-quality axis
  (do the tests prove the behaviour, or do they pass around it?). Findings that
  belong to a later phase get a `TODO` in the code pointing at that phase, not a
  silent fix.

## Working conventions

- **Commits:** one commit per logical phase or feature; run the tests before
  committing, and don't commit on the user's behalf unless asked.
- **Never push.** Pushing, opening PRs and anything else that leaves this machine
  is the maintainer's step, always — not something to offer or do, even when the
  commits are ready and a remote exists.
- **Tests & long-running ops:** run new/changed tests first; only run the full
  suite once those pass. Don't wrap waits in `until … done` sleep loops (they can
  stall) — poll periodically and check whether a background job has died.
- **Never self-dispatch after a question:** if you ask the user something, wait
  for the answer before acting.
- **Pick the cheapest model that fits a subagent.** Always pass `model` explicitly
  — the inherited default is Opus or better, which is expensive for mechanical work.
  Restate the critical rules in each subagent's prompt. When unsure, start cheaper
  and escalate only if the output is shallow.

  | Subagent role | Model |
    |---|---|
  | Mechanical implementer (plan specifies the exact code) | Haiku |
  | Explore / search ("where is X defined") | Haiku |
  | Multi-file integration / pattern matching | Sonnet |
  | Per-phase code-quality or spec-compliance review | Sonnet |
  | Final whole-branch / holistic / deep design review | Opus or better |

