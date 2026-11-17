# Phase 1 · Foundation and scaffolding

## Goal
A public, protected repository where an empty Spring Boot + React app builds, tests, packages into one Docker image and starts with PostgreSQL through `docker compose up --build`, gated by CI. No product features yet.

## People
- **Owner:** Lead
- **Support:** Ajoy and Imrul verify locally (clone, hooks, stack); Ajoy answers Q5 (Spring Boot line for the Spring AI MCP starter, FR-MCP-3)
- **Reviewers:** after the initial scaffold commit, Ajoy and Imrul review every Lead PR.

## Prerequisites
- None. Answer PRD §24 Q1–Q6 during this phase (owners as listed there).

## PRD references
§15.1 repository · §15.2 branching and protection · §15.3 CI (`backend`, `frontend`, `docker` only, **no deploy jobs**) · §16.1 Dockerfile · §16.2 Compose · §16.3 environment variables · §17.1 free tiers · §19.3 automatic-failure guards · §21.2 deployment-timing note · NFR-SEC-9 supply chain · DEC-01, DEC-04, DEC-05.

## Ordered task checklist

### A. Decisions and accounts (Lead)
- [ ] Q4: get submission and demo dates; set the Phase 11 start ≥ 5 days before submission on the board (§21.2).
- [ ] Q1: ask the instructor for a one-line written OK on ADMIN/MAINTAINER/REVIEWER (§6). Q2: ask whether Render + Neon counts as "deployed on Render".
- [ ] Q3: decide who attaches the GCP billing account. If nobody can, raise it now (§17.4 pitfalls).
- [ ] Q6: confirm Apache-2.0; the Lead's name goes into PRD §21.
- [ ] Create accounts (no deploy): Render workspace, Neon projects **A** and **B** (Singapore, verify region per §17.2), GCP project with billing linked (§21.2 note).
- [ ] Q5 (Ajoy): find the newest stable Spring Boot line the Spring AI MCP server starter (WebMVC, Streamable HTTP) supports; the Lead pins it in `backend/pom.xml` (§11.4, FR-MCP-3). Record the finding in the Q5 issue.

### B. Bootstrap the repository (Lead, in this order)
- [ ] Create the **public** GitHub repository with `README.md` and `LICENSE` (Apache-2.0) (DEC-01).
- [ ] Push the full scaffold (sections C–F) directly to `main` as one commit `chore: initial project scaffold`. This is the **only** direct push ever allowed, and it happens before protection exists.
- [ ] Create `develop` from `main`.
- [ ] Enable protection on `main` and `develop` exactly as in [git-and-ci.md](../../guides/operations/git-and-ci.md#branch-protection-settings) (§15.2), including "Do not allow bypassing" (admins included).
- [ ] Turn on secret scanning with push protection, Dependabot alerts, and "Automatically delete head branches" (NFR-SEC-9).
- [ ] Run `scripts/setup-git-hooks.sh` locally; every member does the same (installs `scripts/hooks/pre-push`, which refuses pushes to `main` and `develop`).
- [ ] From now on, every change goes through a PR into `develop` with ≥ 1 approval from another member.

### C. Backend skeleton (`backend/`)
- [ ] `pom.xml`: Java 21, pinned Boot line; starters web, validation, security, data-jpa, actuator; `flyway-core` + `flyway-database-postgresql`; `postgresql`; test deps (spring-boot-starter-test, spring-security-test, testcontainers postgresql + junit-jupiter, `spring-boot-testcontainers`); Surefire (`*Test`) + Failsafe (`*IT`) + JaCoCo plugins (§11.4, TST-R1..R6). Maven wrapper committed and executable.
- [ ] `src/main/java/io/sourceripple/SourceRippleApplication.java` and empty module packages per [architecture.md](../../guides/engineering/architecture.md).
- [ ] `application.yml`: `server.port: ${PORT:8080}`, `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, Hikari max pool 5, Tomcat max threads 20 (NFR-RES-1), actuator exposing only `health` and `info`, liveness/readiness groups (NFR-REL-2), `info.git.commit: ${GIT_COMMIT:${RENDER_GIT_COMMIT:unknown}}` (§16.3).
- [ ] `application-local.yml`, `application-prod.yml`, `src/test/resources/application-test.yml` (profiles per §16.3; see [local-setup-and-environments.md](../../guides/operations/local-setup-and-environments.md)).
- [ ] Temporary security config permitting `/actuator/health`, `/actuator/info` and static files only (replaced in Phase 2).
- [ ] One placeholder test of each kind so Surefire and Failsafe both run: `SourceRippleApplicationIT` (context loads against Testcontainers) and a trivial `*Test`. Delete the trivial unit test once U1 exists.

### D. Frontend skeleton (`frontend/`)
- [ ] Vite + React 19 + TypeScript strict, pnpm via corepack, lockfile committed (§8.2).
- [ ] Scripts: `dev`, `build`, `lint` (ESLint typescript-eslint), `typecheck` (`tsc --noEmit -p tsconfig.app.json`), `format` (Prettier). `eslint.config.js`, `.prettierrc`.
- [ ] `vite.config.ts` proxy for `/api`, `/v3`, `/actuator`, `/demo`, `/mcp` to `http://localhost:8080`.
- [ ] `public/favicon.svg`, `robots.txt` placeholder. Full foundation comes in Phase 3.

### E. Container and configuration (Lead)
- [ ] Root `Dockerfile`: stages `web` → `build` → `runtime` exactly as §16.1 (non-root `app` user, `HEALTHCHECK` with wget, NFR-RES-1 `JAVA_TOOL_OPTIONS`, tests skipped in image).
- [ ] `.dockerignore` (exclude `.git`, `node_modules`, `target`, `frontend/dist`, `.env`).
- [ ] `docker-compose.yml`: `db` (postgres:17-alpine, healthcheck, named volume) and `app` (`build: .`, `env_file: .env`, `depends_on: db: condition: service_healthy`, `8080:8080`). `prometheus` and `grafana` services may be stubbed now and are completed in Phase 9 (§16.2). Every secret is `${VAR:?set VAR in .env}`.
- [ ] `.env.example` with **every** §16.3 variable and dev-only placeholder values; `.gitignore` includes `.env`.
- [ ] `.gitattributes` (`* text=auto eol=lf`, `mvnw` and `*.sh` LF), `.editorconfig`.

### F. CI and repository hygiene (Lead)
- [ ] `.github/workflows/ci.yml` with jobs `backend`, `frontend`, `docker` per §15.3 and [git-and-ci.md](../../guides/operations/git-and-ci.md#ci-jobs). The `docker` job waits for health UP; the register/login curl smoke is added in Phase 2 when auth exists.
- [ ] Dependency-review job on PRs; `dependabot.yml` for Maven, npm (pnpm) and GitHub Actions (NFR-SEC-9).
- [ ] `pull_request_template.md`, `ISSUE_TEMPLATE/` (feature, bug), `CODEOWNERS` (backend → Ajoy + Lead, frontend → Imrul + Lead, `.github/` and `infra/` → Lead).
- [ ] `README.md` skeleton with the §20.1 headings; `CONTRIBUTING.md` (links to the dev docs), `SECURITY.md`, `THIRD_PARTY_NOTICES.md` (empty table).
- [ ] GitHub Projects board (Backlog → In progress → Review → Done) and labels `P0 P1 P2 backend frontend devops bonus` (§15.2). One issue per suggested PR of Phases 1–3.

## Suggested PR breakdown
| PR | Branch | Author | Reviewer |
|---|---|---|---|
| Initial scaffold (direct commit, before protection) | `main` | Lead | — (bootstrap exception) |
| Boot line change if Q5 requires it | `chore/p1-boot-mcp-pin` | Lead (Q5 finding from Ajoy) | Ajoy |
| CI hardening: dependency review, test summary, report upload | `chore/p1-ci-reports` | Lead | Ajoy |
| Repo hygiene fixes found during local verification | `docs/p1-repo-hygiene` | Lead | Imrul |

## Tests to write
- `SourceRippleApplicationIT`: Spring context starts against Testcontainers PostgreSQL; `/actuator/health` returns UP. It is scaffolding, not one of I1–I17.

## Exit criteria
- [ ] `main` and `develop` protected with the exact settings; a direct `git push origin develop` is rejected.
- [ ] CI `backend`, `frontend`, `docker` green on a PR into `develop` and required in protection.
- [ ] `cp .env.example .env && docker compose up --build` starts `db` and `app`; `curl localhost:8080/actuator/health` → `{"status":"UP"}`.
- [ ] Ajoy and Imrul have each cloned the repo, run `scripts/setup-git-hooks.sh` and started the stack locally (§21.2); the ≥ 5 merged PRs per member (§21.4) accrue from Phase 2 on.
- [ ] Render, Neon A/B and GCP accounts exist; Q1–Q6 answered or escalated.

## Evidence to capture (§19.1, §20.2)
- Screenshots of both branch protection pages (§19.1 "Branch settings screenshot").
- Link to the first green Actions run; screenshot of `docker compose ps`.
- The rejected direct push output (terminal screenshot), for the "no direct push" guard (§19.3).

## Risks and gotchas
- **Protection ordering (§15.2 bootstrap).** The initial scaffold commit is the only direct push; `--no-verify` is forbidden afterwards. Enable protection immediately after creating `develop`; any later direct push breaks an automatic-failure guard (§19.3).
- **Required checks only appear after they have run.** The initial push triggers `ci.yml` on `main`, so `backend`, `frontend` and `docker` become selectable. Keep job IDs exactly these names.
- **"Do not allow bypassing"** must be on, or the Lead (admin) can still push directly.
- **Executable bits and line endings.** `git update-index --chmod=+x backend/mvnw scripts/*.sh scripts/hooks/pre-push`; LF endings, or CI fails with `/bin/sh^M`.
- **Docker build needs the lockfile** (`pnpm install --frozen-lockfile`). Commit `pnpm-lock.yaml`.
- **Health needs the DB.** Health is UP only when the datasource is reachable (NFR-REL-2). In Compose, `depends_on` with `service_healthy` avoids startup races.
- **No credentials in Compose or CI logs** (NFR-SEC-4). CI writes a throwaway `.env` from placeholders, never secrets.
- **MCP compatibility drives the Boot version** (§22). If no compatible line exists, MCP moves to P2 per FR-MCP-3; record it in the Q5 PR.
- **Free tiers change** (§22). Note the date you checked §17.1 in the accounts issue.
