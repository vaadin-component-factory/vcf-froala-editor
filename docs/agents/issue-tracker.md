# Issue tracker: GitHub

Issues and specs for this repo live as GitHub issues in `vaadin-component-factory/vcf-froala-editor`.
Use the `gh` CLI for all operations.

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --comments`, filtering comments by `jq` and also fetching labels.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`
- **Mark what the agent wrote**: every issue body and every comment the agent writes ends
  with a line of its own, `_Written by Claude Code_`. The token is the maintainer's, so
  GitHub shows the maintainer as the author. No session link.

Infer the repo from `git remote -v`; `gh` does this automatically when run inside a clone.

GitHub shares one number space across issues and PRs, so a bare `#42` may be either: resolve with `gh pr view 42` and fall back to `gh issue view 42`.

## If `gh` is old again after a container rebuild

The base image ships Debian bookworm ships `gh` **2.23.0 (February 2023)**, which predates fine-grained
personal access tokens: it checks permissions through the `X-OAuth-Scopes` header, which
those tokens never send, so `gh issue create` and `gh issue edit` refuse a perfectly valid
token. The devcontainer's `Dockerfile` therefore installs `gh` not from apt but from the
upstream release `.deb`, pinned through `ARG GH_VERSION` (**2.101.0**), and porcelain
commands work with it. `.devcontainer/` is gitignored, so that pin lives only in the local
file; if it is ever replaced by one that installs `gh` from apt, 2.23.0 is back.

If `gh --version` shows 2.23.0 again, either restore the pinned install and rebuild, or fall
back to `gh api`, which sends only the bearer and does no scope check. The token is not the
problem in either case.

The token itself does not survive a rebuild: `gh auth login` stores it under
`~/.config/gh`, which is not a mounted volume, so log in again after every rebuild.

## Agent write access is deliberately narrow

The agent authenticates with a **fine-grained personal access token scoped to this one
repository**, holding **Issues: read and write** plus **Metadata: read**. It can create,
read, label, comment on and close issues, and nothing else: no push, no pull request, no
access to any other repository. This is not a limitation to work around. It is the
`CLAUDE.md` rule "Never push" enforced by the server rather than by good behaviour. Anything
that leaves this machine is the maintainer's step.

Two consequences worth knowing before you diagnose a failure as your own mistake:

- `gh pr ...` and the `/pulls` API answer **403** (`Resource not accessible by personal
  access token`). That is the scope working, not a fault.
- The legacy issue-import API (`POST /repos/.../import/issues`) also answers 403, which is
  why every issue below number 30 carries the import date rather than its original one.

## Issues 1-29 were imported from GitLab

This project tracked work in GitLab (`stefan/froala` on `gitlab.vaadin.com`) until
2026-09-15, when all 29 issues were copied here. The numbers were preserved exactly, so a
`#n` written before the move still resolves. Their **creation dates are the import date**;
each body ends with a line naming the original GitLab number and date, and each imported
comment opens with the date it was originally written. GitLab is gone; do not try to reach it.

## Pull requests as a triage surface

**PRs as a request surface: no.** _(Set to `yes` if this repo treats external PRs as feature requests; `/triage` reads this flag.)_

When set to `yes`, PRs run through the same labels and states as issues, using the `gh pr` equivalents:

- **Read a PR**: `gh pr view <number> --comments` and `gh pr diff <number>` for the diff.
- **List external PRs for triage**: `gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments` then keep only `authorAssociation` of `CONTRIBUTOR`, `FIRST_TIME_CONTRIBUTOR`, or `NONE` (drop `OWNER`/`MEMBER`/`COLLABORATOR`).
- **Comment / label / close**: `gh pr comment`, `gh pr edit --add-label`/`--remove-label`, `gh pr close`.

Note that the agent's token has no pull-request permission, so turning this flag on also
means widening the token.

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.

## Blocking links are native here

Verified on this repo: `GET repos/<owner>/<repo>/issues/<n>/dependencies/blocked_by` answers,
and every issue reports an `issue_dependencies_summary`. Use the native dependency, **not** a
`Blocked by:` line in the body -- the native edge is visible in the UI and queryable.

    gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by \
      -F issue_id=<blocker-db-id>

`<blocker-db-id>` is the blocker's numeric **database id** (`gh api repos/<owner>/<repo>/issues/<n> --jq .id`),
not its `#number` and not its `node_id`. A ticket is unblocked when
`issue_dependencies_summary.blocked_by` reaches 0.

Sub-issues are available too (`sub_issues_summary` is reported), so a parent/child relation
can be native as well rather than a `## Parent` line.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a single issue with **child** issues as tickets.

- **Map**: a single issue labelled `wayfinder:map`, holding the Notes / Decisions-so-far / Fog body. `gh issue create --label wayfinder:map`.
- **Child ticket**: an issue linked to the map as a GitHub sub-issue (`gh api` on the sub-issues endpoint). Labels: `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`). Once claimed, the ticket is assigned to the driving dev.
- **Blocking**: the native dependency described above.
- **Frontier query**: list the map's open children (`gh issue list --state open`, scoped to the map's sub-issues), drop any with `issue_dependencies_summary.blocked_by > 0` or an assignee; first in map order wins.
- **Claim**: `gh issue edit <n> --add-assignee @me`, the session's first write.
- **Resolve**: `gh issue comment <n> --body "<answer>"`, then `gh issue close <n>`, then append a context pointer (gist + link) to the map's Decisions-so-far.
