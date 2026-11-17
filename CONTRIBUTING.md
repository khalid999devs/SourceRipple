# Contributing to SourceRipple

This is the team's collaboration rulebook. Every member follows it for every change; other
documents link here instead of repeating it. The product requirements live in
[`docs/product/MASTER_PRD.md`](docs/product/MASTER_PRD.md) and the task breakdown in
[`docs/development/`](docs/development/).

## 1. Ground rules

- **Nobody pushes to `main` or `develop`.** All changes arrive through a pull request (PR).
- **Every PR needs one approving review from another member.** Nobody approves their own work.
- **CI must be green** (`backend`, `frontend`, `docker`) before merging.
- `git push --no-verify` is **forbidden**. The pre-push hook exists to protect the branches.
- No secrets, credentials or personal data in code, config, tests, commits or logs.
- Merged Flyway migrations are never edited; add a new version instead.

One-time setup after cloning:

```sh
sh scripts/setup-git-hooks.sh   # enables scripts/hooks/pre-push (blocks pushes to main and develop)
cp .env.example .env            # local, ignored by Git
```

## 2. Branch model

| Branch | Purpose | Created from | Merges into |
|---|---|---|---|
| `main` | Production; every merge deploys | — | — |
| `develop` | Integration | `main` | `main` (release PR) |
| `feature/p<N>-<slug>` | Planned work for phase N, e.g. `feature/p4-safe-fetch-proxy` | `develop` | `develop` |
| `fix/<slug>` | Bug fix | `develop` | `develop` |
| `chore/<slug>` | Tooling, dependencies, housekeeping | `develop` | `develop` |
| `docs/<slug>` | Documentation only | `develop` | `develop` |
| `hotfix/<slug>` | Urgent production fix | `main` | `main`, then `develop` |

Branch names are lowercase, words separated by `-`. One branch covers one issue.

## 3. Local workflow

1. Sync `develop`:
   ```sh
   git switch develop
   git pull --ff-only origin develop
   ```
2. Create a branch for the issue:
   ```sh
   git switch -c feature/p4-safe-fetch-proxy
   ```
3. Work in small, focused commits (Section 5). Each commit builds and keeps tests passing.
4. Rebase on the latest `develop` before pushing:
   ```sh
   git fetch origin
   git rebase origin/develop
   ```
5. Run the same checks as CI:
   ```sh
   (cd backend && ./mvnw verify)                                   # needs Docker for Testcontainers
   (cd frontend && pnpm lint && pnpm typecheck && pnpm build)
   ```
   For Docker, Compose or configuration changes, also run `docker compose up --build` and check
   `http://localhost:8080/actuator/health`.
6. Push the branch (after a rebase, use `--force-with-lease`, never `--force`):
   ```sh
   git push -u origin feature/p4-safe-fetch-proxy
   ```
7. Open a PR into `develop` using the template (Section 6).

## 4. Remote workflow

**Feature, fix, chore and docs PRs**

1. Open the PR into `develop`, link the issue and request a reviewer.
2. CI runs `backend`, `frontend` and `docker`. All three must pass.
3. At least one approving review from another member. New commits dismiss earlier approvals.
4. Resolve every conversation.
5. **Squash-merge.** The squash commit title is the PR title. The branch is deleted automatically.

**Release PR (`develop` → `main`)**

1. Open a PR from `develop` into `main` titled `chore(release): <version or date>`.
2. The branch must be up to date with `main`, CI green, one approval.
3. Merge with a **merge commit** (no squash) so `main` keeps the release history.
4. The merge to `main` triggers the deployment jobs.

**Hotfix**

1. Branch `hotfix/<slug>` from `main`, fix, open a PR into `main` (same rules, squash-merge).
2. Immediately open a second PR that brings the fix into `develop` (or merge `main` into
   `develop` through a PR) so the branches do not drift.

## 5. Commit rules

Format ([Conventional Commits](https://www.conventionalcommits.org/)):

```
type(scope): subject

Optional body explaining why the change is needed.

Refs #<issue>
```

- **subject:** imperative mood, lowercase, at most 72 characters for the whole first line, no
  trailing period. Example: `feat(monitor): claim due sources with a lease`.
- **type:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `build`, `perf`, `style`.
- **scope:** the module or area: `identity`, `workspace`, `source`, `content`, `diff`, `monitor`,
  `impact`, `notify`, `integration`, `admin`, `demo`, `marketing`, `web`, `infra`, `ci`, `docs`.
- **body (optional):** why, not what. Wrap at 72 characters.
- **footer:** `Refs #<issue>` (or `Closes #<issue>`).
- **No `Co-Authored-By` lines and no tool trailers.** The author is the person who wrote and
  understands the change.
- Breaking changes use `!` after the scope and a `BREAKING CHANGE:` footer.

The **PR title** uses the same format, because it becomes the squash commit on `develop`.

## 6. Pull requests

The template ([`.github/pull_request_template.md`](.github/pull_request_template.md)) has these
sections, all required:

| Section | Content |
|---|---|
| Summary | What changes and why, in one short paragraph |
| Phase · PRD IDs | e.g. `Phase 4 · NFR-SEC-1 · U12, U13`, plus `Closes #<issue>` |
| Changes | Bullet list of the notable changes |
| Tests | Tests added or changed and how they were run |
| Evidence | CI link, screenshots, curl output or logs |
| Checklist | Conventions, rebase, scope, secrets, migrations, docs, reviewer |

Keep PRs small: one issue, ideally under 400 changed lines excluding generated files and
lockfiles. Draft PRs are welcome for early feedback.

## 7. Review rules

- Review within one working day. If you cannot, say so and reassign.
- Reviewers alternate so every member reviews regularly (target: at least 5 reviews each).
- Check correctness against the PRD IDs, tests (the right layer, meaningful assertions), security
  (authorization on every endpoint, no secrets, no SSRF gaps), naming and layering
  (controllers → services → repositories), and that the evidence matches the claim.
- Use **Request changes** for blocking issues and plain comments for suggestions. Prefix optional
  remarks with `nit:`.
- The author resolves a conversation only after addressing it; the reviewer may reopen it.
- Approve only what you have read and understood.

## 8. Branch protection (GitHub settings)

Applied to both `main` and `develop` under **Settings → Branches** (or an equivalent ruleset):

| Setting | `main` | `develop` |
|---|---|---|
| Require a pull request before merging | on | on |
| Required approvals | 1 | 1 |
| Dismiss stale approvals when new commits are pushed | on | on |
| Require status checks: `backend`, `frontend`, `docker` | on | on |
| Require branches to be up to date before merging | on | off |
| Require conversation resolution | on | on |
| Require linear history | off (release merge commits) | on |
| Allow force pushes | off | off |
| Allow deletions | off | off |
| Do not allow bypassing the above settings (admins included) | on | on |

Repository settings: allow squash merging and merge commits, disable rebase merging, and enable
**Automatically delete head branches**.

## 9. When something goes wrong

**CI fails**

1. Open the failing job and read the first error, not the last one.
2. Reproduce locally with the same command (`./mvnw verify`, `pnpm lint`, `pnpm typecheck`,
   `pnpm build`, or `docker compose up --build`).
3. Push a fix commit to the same branch. Never merge with a red check, and never disable or skip
   a test to make CI pass.
4. If `develop` itself is red, fixing it takes priority over new work; tell the team.

**Merge conflicts**

1. `git fetch origin && git rebase origin/develop`.
2. Resolve each file, keeping both sides' intent; re-run the checks from Section 3.
3. `git rebase --continue`, then `git push --force-with-lease`.
4. If a conflict touches someone else's in-flight work, agree on the resolution with them first.

**A mistake reached `develop` or `main`**

Open a `fix:` PR (for example a `git revert` of the commit) through the normal process. History on protected branches is never
rewritten.
