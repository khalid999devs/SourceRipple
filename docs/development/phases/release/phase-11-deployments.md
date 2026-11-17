# Phase 11 · Deployments and release

## Goal
The release on `main` is deployed automatically and verifiably to Render (required) and Google Cloud Run (bonus), Render with Render Postgres and Cloud Run with Neon. Scheduled ticks are running, the README explains both deployments, and the demo has been rehearsed twice with a backup video.

## People
- **Owner:** Lead (Render, deploy-render job, tick workflow, release, runbook, rehearsals, video).
- **Imrul:** Cloud Run + Neon + Artifact Registry + WIF + Cloud Scheduler, `deploy-cloudrun` job, README deployment section.
- **Ajoy:** production smoke tests on both URLs.
- **Reviews:** Lead ↔ Imrul review each other; Ajoy approves the release PR.

## Prerequisites
- Phase 10 exit criteria met (all P0/P1 except deployment, CI green).
- **Starts at least 5 days before submission** (§21.2 deployment timing).
- Accounts from Phase 1 exist; Q2 and Q3 answered. Re-verify §17.1 free-tier terms today and note the date in the PR.

## PRD references
§15.3 `deploy-render`, `deploy-cloudrun`, `scheduler-tick.yml`, secrets and variables · §16.1 `PORT` · §16.3 env vars per platform · §17.1–§17.5 · §11.3 execution model per environment (DEC-08) · §20.1 item 10 · §20.2 · §19.3 "App not deployed" · DEC-09, DEC-10, DEC-11.

## Ordered task checklist
Step-by-step commands and click paths are in [deployment-runbook.md](../../guides/operations/deployment-runbook.md).

### A. Render web service + Render Postgres (Khalid) (§17.3)
- [ ] Render Postgres Free: PostgreSQL 17, Singapore; **internal** host; `jdbc:postgresql://<internal-host>:5432/<db>`; record the creation date (expires after 30 days).
- [ ] Render Web Service: Docker, root `Dockerfile`, branch `main`, Free, Singapore, health check `/actuator/health`, **Auto-Deploy Off**, env vars per §16.3 Render column (`SPRING_PROFILES_ACTIVE=prod`, `SOURCERIPPLE_SCHEDULER_MODE=both`, `SOURCERIPPLE_MIN_INTERVAL_HOURS=6`, `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS=false`, demo vars, secrets, `JAVA_TOOL_OPTIONS` per NFR-RES-1).
- [ ] GitHub: secret `RENDER_DEPLOY_HOOK_URL`, variable `RENDER_APP_URL`; secret `TICK_TOKEN` = Render's `SOURCERIPPLE_TICK_TOKEN`.
- [ ] `ci.yml` job `deploy-render` (push to `main` only, `needs: [backend, frontend, docker]`, `concurrency: deploy-${{ github.ref }}`): POST `"$RENDER_DEPLOY_HOOK_URL&ref=$GITHUB_SHA"`, then poll `$RENDER_APP_URL/actuator/info` until `git.commit == $GITHUB_SHA` (15 min timeout, retries through cold start); fail otherwise (§15.3).
- [ ] `.github/workflows/scheduler-tick.yml`: `cron: "17 */3 * * *"` + `workflow_dispatch`; POST `$RENDER_APP_URL/internal/scheduler/tick` with `X-SR-Tick-Token`, retrying through cold start (§15.3).

### B. Cloud Run + Neon (Imrul) (§17.4)
- [ ] Neon project (Postgres 17, region per §17.2 **(verify)**, **direct** host, `?sslmode=require`).
- [ ] GCP: enable Run, Artifact Registry, IAM Credentials, Cloud Scheduler APIs; Artifact Registry Docker repo with a cleanup policy keeping the last 2 images (< 0.5 GB).
- [ ] Deployer service account (`run.admin`, `artifactregistry.writer`, `iam.serviceAccountUser`); Workload Identity Federation pool + provider bound to this repository; secrets `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT`; variables `GCP_PROJECT_ID`, `GCP_REGION`, `CLOUD_RUN_SERVICE`, `CLOUD_RUN_URL`.
- [ ] Service `sourceripple`, asia-southeast1 **(verify free tier applies)**: request-based billing, min 0, **max 1**, 1 vCPU, 1 GiB, startup CPU boost, concurrency 20, timeout 300 s, `--allow-unauthenticated`; env per §16.3 Cloud Run column with `SOURCERIPPLE_SCHEDULER_MODE=external`; secrets via Secret Manager **(verify free allowance)** or env vars from CI secrets.
- [ ] `ci.yml` job `deploy-cloudrun` (push to `main` only, `needs: deploy-render`): `google-github-actions/auth` (WIF, no JSON keys) → build and push image tagged with the SHA → `google-github-actions/deploy-cloudrun` with `GIT_COMMIT=${{ github.sha }}` → poll `$CLOUD_RUN_URL/actuator/info` for the SHA.
- [ ] Cloud Scheduler job: `POST <CLOUD_RUN_URL>/internal/scheduler/tick`, header `X-SR-Tick-Token`, schedule `0 * * * *` **(verify the free job allowance)**.
- [ ] README §20.1 item 10: both platforms step by step, the architecture difference (sleeping instance with internal loop + cron tick vs request-only CPU with external tick), every env var (bonus documentation, §17.4).

### C. Release (Lead)
- [ ] Release PR `develop → main` (merge commit), titled `chore(release): release to production`; description lists merged phases and links the CI run. Approved by Ajoy or Imrul.
- [ ] Watch `deploy-render` and `deploy-cloudrun` go green with matching SHAs.
- [ ] Re-enable `scheduler-tick.yml` if GitHub disabled it (60 days without activity).

### D. Production smoke tests (Ajoy)
- [ ] On both URLs: `/actuator/health` UP; `/actuator/info` SHA = release SHA; `/actuator/prometheus` 401 without credentials.
- [ ] Golden flow on Render (Appendix A): admin switches fixture to v2 → maintainer **Check now** → change detail → 2 NEW impacts, 0 Fitness → delivery DELIVERED, receiver "signature valid" → reset fixture to v1.
- [ ] Roles: reviewer has no New watch; forced write → 403; reviewer `/api/v1/admin/users` → 403.
- [ ] SSRF: preview `http://169.254.169.254/` → 422 `SOURCE_FETCH_BLOCKED`.
- [ ] Landing, pricing, waitlist (202), demo login work on the public URL.
- [ ] Record results as a checklist comment on the release PR.

### E. Demo readiness (Lead, all present) (§17.5, §20.2)
- [ ] Two full rehearsals of the 15-minute script with timings.
- [ ] Backup video of the golden flow recorded at T-24 h.
- [ ] Demo-day runbook executed per [deployment-runbook.md](../../guides/operations/deployment-runbook.md#demo-day-runbook).

## Suggested PR breakdown
| # | Branch | Author | Reviewer |
|---|---|---|---|
| 1 | `chore/p11-deploy-render` | Lead | Imrul |
| 2 | `chore/p11-scheduler-tick` | Lead | Ajoy |
| 3 | `chore/p11-deploy-cloudrun` | Imrul | Lead |
| 4 | `docs/p11-readme-deployment` | Imrul | Lead |
| 5 | Release PR `develop → main` | Lead | Ajoy |
| 6 | `hotfix/<slug>` only if production breaks (cut from `main`, merged back to both) | any | another member |

## Tests to write
- No new U/I IDs. Deploy jobs are themselves verification (SHA polling). Smoke results are recorded on the release PR.

## Exit criteria
- [ ] Both public URLs serve the release SHA in `/actuator/info` (deploy jobs green).
- [ ] Golden flow passes on Render; Cloud Run serves the same app.
- [ ] Tick runs: a `scheduler-tick.yml` run and a Cloud Scheduler execution both return 200.
- [ ] README deployment section merged; demo rehearsed twice; backup video stored.

## Evidence to capture (§19.1, §20.2)
- Live URLs (Render, Cloud Run) in README.
- `deploy-render` log showing the SHA match next to the `/actuator/info` response.
- Render dashboard (Auto-Deploy Off), Cloud Run service page, Cloud Scheduler job page.
- Render Postgres expiry date and Neon usage page at T-24 h (< 70% quota).

## Risks and gotchas
- **Hook URL already has `?key=`**, so the ref is appended with `&ref=`; passing `ref` also keeps auto-deploy off (§15.3).
- **`git.commit` source:** Render provides `RENDER_GIT_COMMIT`; Cloud Run needs `GIT_COMMIT` set at deploy. Without it the poll never matches and the job fails (by design).
- **JDBC URLs:** convert `postgresql://user:pass@host/db` to JDBC form, put user and password in their own variables, and use the **direct** host, not `-pooler` (§16.3).
- **Render spin-down:** first request ~1 min. Polling and ticks must retry; never add a 24/7 keep-alive (free instance hours, §17.3).
- **Cloud Run has no CPU between requests:** `SCHEDULER_MODE=external` only; never rely on `@Scheduled` or `@Async` there (DEC-08).
- **`allow-private-targets=true` in prod refuses startup** (NFR-SEC-1.7). Check env before deploying.
- **Billing account** missing → raise immediately (§24 Q3); fallback host must be chosen and verified then.
- **Max instances 1** on Cloud Run keeps one scheduler and the free tier.
