# Implementation brief: Phases 2–7 (Ajoy)

Paste everything below the line as the working brief for whoever implements Ajoy's phases. It is written to "you", the implementer.

---

You are implementing Phases 2–7 of SourceRipple, an open-source Spring Boot + React project, on behalf of Ajoy (GitHub `ajoycodes`). Work in the repository on disk. Follow these instructions exactly.

## Sources of truth
1. `docs/product/MASTER_PRD.md` is the ground truth. Every behavior you build must trace to a PRD ID (`FR-*`, `NFR-*`, `DEC-*`, `U#`/`I#`, `§n`). If the PRD does not ask for it, do not build it.
2. `docs/development/phases/core/phase-02-identity-access.md` … `phase-07-baseline-screens.md` are your task lists. Work each checklist top to bottom.
3. Read these guides before writing code and follow them: `docs/development/guides/engineering/` (`architecture.md`, `backend-conventions.md`, `frontend-conventions.md`, `testing.md`) and `docs/development/guides/operations/` (`git-and-ci.md`, `local-setup-and-environments.md`).
4. If the PRD and a dev doc disagree, the PRD wins. If the PRD is missing something or contradicts itself, stop that item, note it, and continue with the next item. Never edit anything under `docs/product/` or `docs/idea/`.

## Before you start
1. Read `docs/development/README.md` and `docs/development/PHASE_PLAN.md` (dependencies, review rotation, test-ID tracker).
2. Set up the machine per `local-setup-and-environments.md`: Docker, Java 21, Node 22 with corepack; `cp .env.example .env`; `docker compose up --build` must start; `cd backend && ./mvnw verify` must pass on a clean `develop`.
3. Confirm the Phase 1 exit criteria are met (protected `main`/`develop`, CI green). If not, stop and tell the Lead.
4. For the phase you start, create one GitHub issue per row of its "Suggested PR breakdown", labeled `P0` + `backend`/`frontend`, and put them on the Projects board.

## Scope boundaries
- **In scope:** Phases 2, 3 (backend part), 4, 5 (engine and watches), 6 and 7 as written in the phase files.
- **Not yours:** the Lead's Phase 3 frontend foundation and the Lead's Phase 5 demo kit (you review them; don't build them). Do not start Phases 8–11 (notifications, webhooks, API keys, MCP, admin features beyond FR-ADM-1 (users and roles API), observability, marketing site, deployments).
- Where your code must connect to later work, leave only the port or call site the phase file describes (for example `BaselineRunner`, `ProjectArchiveListener`). No stub implementations of later features, no fake data, no placeholder endpoints.

## Repository workflow (strict)
- Never push to `main` or `develop`; protection and the `pre-push` hook block it. Run `scripts/setup-git-hooks.sh` once.
- One vertical slice = one branch = one PR into `develop`. Use the branch names from each phase file's "Suggested PR breakdown", format `feature/p<N>-<slug>` (or `fix/p<N>-<slug>`).
- Local loop for every slice:
  1. `git switch develop && git pull --ff-only`, then `git switch -c feature/p<N>-<slug>`.
  2. Small commits, each compiling and passing tests.
  3. `git fetch origin && git rebase origin/develop`.
  4. `cd backend && ./mvnw -B verify` and, if frontend changed, `cd frontend && pnpm lint && pnpm typecheck && pnpm build`. Both must pass.
  5. `git push -u origin <branch>` (after a rebase: `--force-with-lease`, own branch only).
  6. Open the PR into `develop` using the template.
- Merge only when CI (`backend`, `frontend`, `docker`) is green, one other member has approved, and all conversations are resolved. Squash-merge. Never approve or merge your own PR without that approval.
- Reviewers alternate: odd-numbered PRs in a phase → Lead (`khalid999devs`), even-numbered → Imrul (`kayes2207054`). Every Phase 7 PR also requests Imrul. Follow any reviewer named in the phase file.

## Commit and PR rules
- Commit format `type(scope): subject`: imperative, lowercase, first line ≤ 72 characters, no trailing period.
  - Types: `feat fix refactor test docs chore ci build perf style`.
  - Scopes: `identity workspace source content diff monitor impact notify integration admin demo marketing shared web infra ci docs`.
  - Optional body: why. Footer: `Refs #<issue>`.
  - No tool or assistant attribution and no `Co-Authored-By` trailers.
- PR title: same format. PR description sections: **Summary**, **Phase · PRD IDs** (e.g. `Phase 4 · NFR-SEC-1 · U12`), **Changes**, **Tests** (IDs and classes), **Evidence** (CI link, screenshots for UI), **Checklist**.
- If CI fails, fix it on the same branch. Never skip, disable or weaken a test to go green.

## Code rules
- Follow the module map, layering and port rules in `architecture.md`; error codes and exceptions in `backend-conventions.md`; Flyway plan V1–V6 for your phases (new files only, never edit a merged migration).
- **No unnecessary comments.** The course teacher reviews the code. Only short comments explaining a non-obvious *why*. No tutorial or explanatory comments, no PRD IDs, phase numbers or test IDs in source, no commented-out code, no TODO essays. Traceability lives in PR descriptions and `docs/development/`.
- **Structure limits.** At most 6 files directly in any directory (repo root and the `backend/`/`frontend/` tool-config roots exempt): group related files into sub-directories or concept sub-packages. No source file over 800 lines (generated files and lockfiles exempt): split by responsibility. Reviewers reject PRs that break either rule.
- No business work after the HTTP response (DEC-08): no `@Async`, no fire-and-forget threads. Never hold a DB transaction across an outbound HTTP call.
- Tests: unit tests `*Test` without a Spring context (JUnit 5, Mockito, AssertJ); integration tests `*IT` with `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers `@ServiceConnection`; `@MockitoBean`, never `@MockBean`; never call the real internet (MockWebServer or the demo fixture). Name test methods after behavior.

## Definition of done for each slice
- [ ] Every checklist item of the slice is implemented exactly as the phase file and the cited PRD sections say.
- [ ] The slice's tests exist, assert real behavior and pass locally (`./mvnw verify`) and in CI.
- [ ] New endpoints have role and membership checks and appear correctly in Swagger UI.
- [ ] Error responses use the envelope and the PRD error codes; no stack traces.
- [ ] If DTOs changed and the frontend exists, `pnpm gen:api` was run and `schema.d.ts` committed.
- [ ] No secrets, no new dependencies outside the PRD stack, no edits to merged migrations.
- [ ] Structure and comment rules hold; the PR description is complete; the reviewer approved.

## Phases in order
Do not start a phase until the previous phase's exit criteria are met and merged.

| Phase | Exit criteria (all checkable) | Tests you own |
|---|---|---|
| 2 · Identity and access | Auth, sessions in Postgres, CSRF, three enforcement layers; RBAC and auth tests green in CI; BCrypt hashes in DB; bootstrap admin from env | U1, U2, U3, U8; I1, I2, I3, I5 (also FR-ADM-1 users and roles API and the login rate limit) |
| 3 · Workspace and API (backend) | Org, member, project CRUD with correct codes; error envelope with `requestId` on every error; OpenAPI at `/swagger-ui.html`; login/logout works in the SPA once the Lead's frontend foundation lands | U4, U5, U6; I7, I15 |
| 4 · Safe fetching and extraction | Preview returns sections for HTML and RSS fixtures; private, loopback, metadata and IP-literal targets blocked; prod refuses `allow-private-targets=true` | U11–U16, U28 |
| 5 · Monitoring and watches | Fixture watched, baseline READY, re-check creates no duplicate version; loop, tick, leases, backoff, pacing, run log working | U7, U9, U10, U17, U18, U30; I4, I8, I13, I16 (FR-EXT-5 versioning is yours here) |
| 6 · Changes, dependencies, impacts | **Golden flow green in CI for the Appendix A version, change and impact results** (pair with the Lead on I9; webhook and MCP results are Phase 8); footer-only change ignored; evidence survives pruning | U19–U24; I6, I9 (with Lead), I10, I11 |
| 7 · Baseline screens (incl. S9 table view) | The whole core loop is usable in the browser end to end as the demo maintainer and reviewer; frontend gate green | No new IDs; regression tests for any backend bug you fix |

## Stop and hand back
When the Phase 7 exit criteria are met and every Phase 2–7 PR is merged, **stop**. Do not continue into Phase 8. Report back with:
- [ ] List of merged PR links per phase (title + reviewer).
- [ ] Test counts from the latest green CI run on `develop` (link): your unit IDs U1–U24, U28, U30 (26) and integration IDs I1–I11, I13, I15, I16 (14), each marked done or missing; total unit and IT test-method counts from the CI summary.
- [ ] Open issues, known bugs, and anything deferred, each with a GitHub issue link.
- [ ] Every PRD gap or contradiction you hit, with the PRD section, and how you handled it.
- [ ] Anything Phases 8–9 owners must know (ports to implement, call sites in `CheckOutcomeService`, migration numbers used).
