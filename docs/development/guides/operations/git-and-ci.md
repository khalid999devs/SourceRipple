# Git workflow and CI

Strict rules for every member. Derived from PRD §15.2–§15.3, §19.3 and §21.4. "Direct push to `main`" is an automatic course failure; these rules exist to make it impossible.

## Repository bootstrap (Lead, Phase 1, once)
1. Create the **public** repository with `README.md` and `LICENSE` (Apache-2.0).
2. Push the scaffold directly to `main` as **one** commit: `chore: initial project scaffold`. This is the only direct push that will ever happen, and it happens before protection exists.
3. Create `develop` from `main` and push it.
4. Apply the protection settings below to `main` and `develop`.
5. Settings → General → Pull Requests: allow **squash merging** and **merge commits**; disable rebase merging; enable **Automatically delete head branches**.
6. Settings → Code security: secret scanning + push protection, Dependabot alerts and security updates (NFR-SEC-9).
7. Every member clones the repo, runs `scripts/setup-git-hooks.sh` (installs `scripts/hooks/pre-push`, which rejects pushes to `main` and `develop`) and starts the stack locally (Phase 1 exit). `git push --no-verify` is forbidden (§15.2).

From step 4 on, every change by anyone (Ajoy, Imrul, Lead) goes through a PR with ≥ 1 approving review from **another** member. No exceptions, admins included.

## Branch protection settings
Settings → Branches → Add branch ruleset (or classic rule) for each branch:

| Setting | `main` | `develop` |
|---|---|---|
| Require a pull request before merging | on | on |
| Required approvals | 1 | 1 |
| Dismiss stale approvals when new commits are pushed | on | on |
| Require approval of the most recent push (author can't self-approve) | on | on |
| Require status checks: `backend`, `frontend`, `docker` | on | on |
| Require branches to be up to date before merging | on | off (on is allowed) |
| Require conversation resolution | on | on |
| Require linear history | off (release PRs use merge commits) | **on** |
| Allow force pushes | off | off |
| Allow deletions | off | off |
| Do not allow bypassing the above settings (include administrators) | **on** | **on** |

Take screenshots of both pages for grading (§19.1).

## Branch model and naming
| Branch | Cut from | Merges into | Merge method |
|---|---|---|---|
| `main` | — | — | production; every merge deploys (Phase 11) |
| `develop` | `main` | `main` via release PR | merge commit |
| `feature/p<N>-<slug>` | `develop` | `develop` | squash |
| `fix/p<N>-<slug>` | `develop` | `develop` | squash |
| `chore/p<N>-<slug>`, `docs/p<N>-<slug>` | `develop` | `develop` | squash |
| `hotfix/<slug>` | `main` | `main`, then `develop` | merge commit into `main`, squash into `develop` |

`<N>` is the phase number, `<slug>` is lowercase kebab-case, ≤ 5 words: `feature/p4-safe-fetch-proxy`. One branch = one vertical slice = one PR.

## Local workflow
```bash
git switch develop && git pull --ff-only          # 1. sync
git switch -c feature/p4-safe-fetch-proxy         # 2. branch
# 3. small commits, each building and passing tests
git fetch origin && git rebase origin/develop     # 4. rebase before opening/updating the PR
(cd backend && ./mvnw -B verify)                  # 5. backend gate
(cd frontend && pnpm lint && pnpm typecheck && pnpm build)  #    frontend gate
git push -u origin feature/p4-safe-fetch-proxy    # 6. push the branch (after a rebase: --force-with-lease)
gh pr create --base develop --fill                # 7. open the PR, fill the template
```
Force-pushing is allowed **only** to your own work branch, only with `--force-with-lease`.

## Remote workflow
1. PR targets `develop`. CI (`backend`, `frontend`, `docker`) must be green.
2. Request the reviewer named in the phase file (rotation in [PHASE_PLAN.md](../../PHASE_PLAN.md#review-rotation-prd-214)).
3. ≥ 1 approval from another member; all conversations resolved.
4. **Squash-merge**; the squash title is the PR title. The branch is deleted automatically.
5. **Release:** PR `develop → main`, title `chore(release): <summary>`, merge commit, approved by a member other than the author. From Phase 11 it deploys.

## Commit messages
Format: `type(scope): subject`
- **type:** `feat` `fix` `refactor` `test` `docs` `chore` `ci` `build` `perf` `style`.
- **scope:** `identity` `workspace` `source` `content` `diff` `monitor` `impact` `notify` `integration` `admin` `demo` `marketing` `web` `infra` `ci` `docs` (use `shared` for the shared backend module).
- **subject:** imperative, lowercase, ≤ 72 characters for the whole first line, no trailing period: `feat(source): block private ip literals in canonicalizer`.
- **body (optional):** why the change is needed, wrapped at 72.
- **footer:** `Refs #<issue>` (or `Closes #<issue>` on the last commit of the slice).
- No tool or assistant attribution trailers, and don't add `Co-Authored-By` lines by hand. Each person commits their own work under their own GitHub account.

## Pull requests
**Title:** same format as a commit (`feat(impact): add review workflow`).
**Description** (the template in `.github/pull_request_template.md`):

```markdown
## Summary
One or two sentences: what and why.
## Phase · PRD IDs
Phase 6 · FR-IMP-3 · U24 · I11
## Changes
- Bullet list of notable changes (files, endpoints, migrations)
## Tests
- New/changed tests by ID and class; local `./mvnw verify` result
## Evidence
CI run link, screenshots for UI changes
## Checklist
- [ ] Linked issue  - [ ] Tests added  - [ ] No secrets  - [ ] Migrations are new files only
- [ ] Generated API client updated (if DTOs changed)  - [ ] THIRD_PARTY_NOTICES updated (if UI code copied)
```
Keep PRs reviewable: aim for < 400 changed lines excluding generated files and fixtures.

## Review rules
Reviewers check, in order:
1. Traceability: phase and PRD IDs present; behavior matches the cited PRD text; nothing out of scope.
2. Correctness and tests: the listed U#/I# exist, assert real behavior, and pass in CI.
3. Security: role and membership checks on every new endpoint (§6.3), no secrets, SSRF path used for outbound HTTP, no work after the response.
4. Conventions: layering, naming, error codes ([backend-conventions.md](../engineering/backend-conventions.md), [frontend-conventions.md](../engineering/frontend-conventions.md)).
5. Code structure: no directory holds more than 6 files directly (repo root and the `backend/`/`frontend/` tool-config roots exempt); no source file over 800 lines (generated files and lockfiles exempt); no unnecessary comments (only short *why* comments; no PRD/phase/test IDs, tutorial text or commented-out code in source). **Reject** a PR that breaks any of these.

**Approve** when the PR is correct and complete; minor nits can be left as non-blocking comments. **Request changes** for anything wrong, missing tests, scope creep or a convention breach. Respond to a review request within **24 hours** (12 hours during Phase 11). The author resolves a conversation only after addressing it.

## When things go wrong
- **CI fails:** read the job log, reproduce locally with the same command, fix on the same branch, push. Never disable or skip a test to go green. Flaky test → open an issue labeled `bug` and fix it in this or the next PR.
- **Merge conflict:** `git fetch && git rebase origin/develop`, resolve, rerun the local gates, `git push --force-with-lease`. Migration number clash: renumber your unmerged migration to the next free number.
- **Hotfix:** production broken on `main` → `hotfix/<slug>` from `main`, PR into `main` (review + CI as usual, merge commit), then PR the same fix into `develop`.
- **Accidental local commit on `develop`/`main`:** `git branch feature/p<N>-<slug>` then `git reset --hard origin/develop` (or `origin/main`); the hook blocks the push anyway.

## CI jobs
`.github/workflows/ci.yml` triggers on `pull_request` into `develop`/`main` and `push` to `develop`/`main`. Third-party actions pinned to a major version (§15.3).

| Job | Since | When | Steps |
|---|---|---|---|
| `backend` | Phase 1 | always | `actions/setup-java` (Temurin 21, Maven cache) → `./mvnw -B verify` → upload Surefire, Failsafe and JaCoCo reports → test summary with unit and IT counts |
| `frontend` | Phase 1 | always | corepack/pnpm → `pnpm install --frozen-lockfile` → `lint` → `typecheck` → `build` |
| `docker` | Phase 1 | needs backend, frontend | write a CI `.env` from `.env.example` → `docker build` → `docker compose up -d --build` → wait for `/actuator/health` UP → curl smoke (register + login from Phase 2) → `docker compose down -v` |
| `dependency-review` | Phase 1 | PRs only | `actions/dependency-review-action` (NFR-SEC-9) |
| `deploy-render` | Phase 11 | push to `main` only; needs the three | `curl -fsS -X POST "$RENDER_DEPLOY_HOOK_URL&ref=$GITHUB_SHA"` → poll `$RENDER_APP_URL/actuator/info` until `git.commit == $GITHUB_SHA` (15 min, retries for cold start) → fail if never matched |
| `deploy-cloudrun` (P1) | Phase 11 | push to `main` only; needs `deploy-render` | `google-github-actions/auth` (WIF) → build/push image tagged SHA to Artifact Registry → `google-github-actions/deploy-cloudrun` (`GIT_COMMIT=$GITHUB_SHA`) → poll `$CLOUD_RUN_URL/actuator/info` |

Deploy jobs use `concurrency: deploy-${{ github.ref }}`. Phase 11 also adds `.github/workflows/scheduler-tick.yml`: `cron: "17 */3 * * *"` + `workflow_dispatch`, POST `$RENDER_APP_URL/internal/scheduler/tick` with `X-SR-Tick-Token: ${{ secrets.TICK_TOKEN }}`, retrying through cold start. GitHub disables scheduled workflows after 60 days without repository activity; re-enable before the demo.

## Secrets and variables (Phase 11)
| Kind | Name | Value from |
|---|---|---|
| Secret | `RENDER_DEPLOY_HOOK_URL` | Render service → Settings → Deploy Hook |
| Secret | `TICK_TOKEN` | Same value as `SOURCERIPPLE_TICK_TOKEN` on the platforms |
| Secret | `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT` | WIF provider resource name, deployer SA email |
| Variable | `RENDER_APP_URL`, `CLOUD_RUN_URL` | Public service URLs |
| Variable | `GCP_PROJECT_ID`, `GCP_REGION`, `CLOUD_RUN_SERVICE` | GCP setup |

Runtime application secrets live only in Render's environment and in Cloud Run (Secret Manager or env vars), never in GitHub logs (§15.3, NFR-SEC-4). Render Auto-Deploy stays **Off**; CI is the only deployer.
