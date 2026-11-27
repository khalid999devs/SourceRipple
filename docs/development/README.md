# SourceRipple development docs

These docs turn the product requirements into work that any team member or new contributor can pick up and finish.

> **Ground rule.** [`docs/product/MASTER_PRD.md`](../product/MASTER_PRD.md) is the single source of truth. These dev docs only add implementation detail: file paths, order of work, class names, commands and checklists. They cite PRD IDs (`FR-*`, `NFR-*`, `DEC-*`, `U#`/`I#`, `§n`) instead of restating requirements. If a dev doc and the PRD disagree, the PRD wins. Fix the dev doc, or open a PR against the PRD (§ "How to use and change this document"). Never change behavior here that the PRD does not describe.

This is a single living set of documents. Git history is the change record, so there are no version numbers in the docs.

## Index

| File | What it is for |
|---|---|
| [PHASE_PLAN.md](PHASE_PLAN.md) | All 11 phases: owners, dependencies, calendar, reviews, scope cuts, test-ID ownership |
| [phases/phase-01-foundation.md](phases/foundation/phase-01-foundation.md) | Repo, protection, CI, Docker/Compose, accounts (Lead) |
| [phases/phase-02-identity-access.md](phases/core/phase-02-identity-access.md) | Data model base, auth, roles, `AccessPolicy`, filter chains (Ajoy) |
| [phases/phase-03-workspace-api-frontend.md](phases/core/phase-03-workspace-api-frontend.md) | Organizations, projects, error envelope, OpenAPI (Ajoy) and frontend foundation (Lead) |
| [phases/phase-04-safe-fetch-extraction.md](phases/core/phase-04-safe-fetch-extraction.md) | SSRF guard, adapters, factory, Flyweight, preview (Ajoy) |
| [phases/phase-05-monitoring-watches.md](phases/core/phase-05-monitoring-watches.md) | Watches, monitoring engine, tick (Ajoy) and demo kit (Lead) |
| [phases/phase-06-changes-impacts.md](phases/core/phase-06-changes-impacts.md) | Versioning, section diff, dependencies, impacts, golden flow (Ajoy, Lead pairs on I9) |
| [phases/phase-07-baseline-screens.md](phases/core/phase-07-baseline-screens.md) | Functional core-loop screens (Ajoy) |
| [phases/phase-08-integrations-admin.md](phases/additions/phase-08-integrations-admin.md) | Notifications, webhooks, API keys, MCP, admin (Imrul and Lead) |
| [phases/phase-09-observability-marketing.md](phases/additions/phase-09-observability-marketing.md) | Actuator, Prometheus, Grafana, waitlist (Lead) and marketing site (Imrul) |
| [phases/phase-10-polish-hardening.md](phases/additions/phase-10-polish-hardening.md) | UI polish, test audit, security review, README (Lead), diagrams (Ajoy) |
| [phases/phase-11-deployments.md](phases/release/phase-11-deployments.md) | Render, Cloud Run, deploy jobs, runbook, release (Lead, smoke tests by Ajoy) |
| [guides/architecture.md](guides/engineering/architecture.md) | Module map, internal layout, layering, execution model, pattern placement |
| [guides/backend-conventions.md](guides/engineering/backend-conventions.md) | Naming, DTOs, validation, errors, transactions, security annotations, config, Flyway plan |
| [guides/frontend-conventions.md](guides/engineering/frontend-conventions.md) | Folder layout, shells, routing, Query vs Zustand, API client, tokens, a11y and performance |
| [guides/testing.md](guides/engineering/testing.md) | Unit and integration test recipes, MockWebServer, Surefire/Failsafe, JaCoCo, failing-then-fixed evidence |
| [guides/git-and-ci.md](guides/operations/git-and-ci.md) | Repo bootstrap, protection click-path, PRs, release flow, CI jobs, secrets |
| [guides/local-setup-and-environments.md](guides/operations/local-setup-and-environments.md) | Prerequisites, Compose, running apps separately, profiles, env vars, demo accounts |
| [guides/deployment-runbook.md](guides/operations/deployment-runbook.md) | Render + Render Postgres, Cloud Run + Neon, verification, demo-day runbook |
| [handoffs/ajoy-phases-2-7.md](handoffs/ajoy-phases-2-7.md) | Paste-ready implementation brief for Phases 2–7 |

## Traceability conventions

Every change must trace back to the PRD. Use these formats everywhere.

| Item | Format | Example |
|---|---|---|
| Branch | `feature/p<N>-<slug>`, also `fix/…`, `chore/…`, `docs/…`, `hotfix/…` | `feature/p4-safe-fetch-proxy` |
| PR title (PRD §15.2 + §21.4) | `type(scope): subject` (Conventional Commits); the description's first line is `Phase N · <PRD IDs>` | Title `feat(source): block private targets in safe fetch proxy`; description `Phase 4 · NFR-SEC-1 · U12` |
| Commit (PRD §15.2) | `type(scope): subject`, imperative, lowercase, ≤ 72 chars, no period; types `feat fix refactor test docs chore ci build perf style`; footer `Refs #<issue>` | `test(diff): cover renamed heading as uncertain` |
| Commit scope | Backend module (`identity`, `workspace`, `source`, `content`, `diff`, `monitor`, `impact`, `notify`, `integration`, `admin`, `demo`, `marketing`, `shared`) or area `web`, `infra`, `ci`, `docs` | `feat(web): add impact inbox filters` |
| Issue | One per PR-sized slice, labeled `P0`/`P1`/`P2` plus `backend`/`frontend`/`devops`/`bonus` | "Phase 4 · U11 UrlCanonicalizer" |
| Test names | Class names from the phase file; the ID → class mapping lives in [PHASE_PLAN.md](PHASE_PLAN.md#test-id-ownership-tracker-prd-14) and the PR description, never in source | `SafeFetchProxyTest.blocksMetadataAddressWithoutCallingDelegate` |

Feature PRs are squash-merged, so the PR title becomes the commit on `develop`. Release PRs (`develop` → `main`) use a merge commit. The only commit ever pushed straight to `main` is the Lead's initial scaffold, before protection exists (Phase 1). The full rules are in [guides/git-and-ci.md](guides/operations/git-and-ci.md).

## How to pick up a phase

1. Check the previous phase's exit criteria in [PHASE_PLAN.md](PHASE_PLAN.md). If a prerequisite isn't met, say so in the team channel before starting.
2. Open the phase file. Read its **PRD references** in the PRD itself, not only the summary here.
3. Create one GitHub issue for each suggested PR in the phase file, put them on the Projects board (Backlog → In progress → Review → Done) and assign the reviewer named there.
4. Work the ordered checklist from the top. Each `- [ ]` item names its PRD IDs, the files and the classes. Tick items in the issue, not in this file.
5. Write the tests listed for the phase in the same PR as the code they cover. A PR without its tests is not ready for review.
6. Open the PR with the template, run `./mvnw verify` and `pnpm lint && pnpm typecheck && pnpm build` first, and request the named reviewer.
7. When every exit criterion is checked, capture the **evidence for grading** listed in the phase file and store it under `docs/diagrams/` or link it from the PR.

## Rules for every contributor

- Read the phase file, the guides it links and the cited PRD sections before writing code.
- Use the exact paths, class names and error codes given. Don't add endpoints, tables, settings or dependencies the PRD doesn't call for.
- If something is missing or contradictory in the PRD, stop and raise it with the Lead. Don't guess, and don't edit `docs/product/` or `docs/idea/` outside a PRD change PR.
- Never push to `main` or `develop`. Open a PR from a work branch.
- Code structure: at most 6 files directly in any directory (repo root and the `backend/`/`frontend/` tool-config roots exempt), no source file over 800 lines (generated files and lockfiles exempt), and only short *why* comments with no PRD, phase or test IDs in source. Reviewers reject PRs that break these.
