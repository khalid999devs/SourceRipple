# Deployment runbook

Phase 11 procedure for PRD §17.3 (Render + Neon, required), §17.4 (Cloud Run + Neon, bonus) and §17.5 (demo day). Items the PRD marks **(verify)** must be checked on the day and the result noted in the Phase 11 PR. Items marked **(check)** are implementation details not stated in the PRD that should be confirmed against current provider docs.

Before starting: re-read §17.1 free-tier terms and confirm they still hold (PRD §22). Region decision (§17.2): Singapore everywhere. **(verify)** that Neon offers AWS ap-southeast-1 and that Cloud Run's free tier applies in asia-southeast1; if not, put each app and its database in the same US region.

## 1. Neon → JDBC (both deployments)
1. Neon console → New project (Postgres 17, region per §17.2). Project **A** for Render, project **B** for Cloud Run (separate quotas).
2. Connection details → **disable "Connection pooling"** to get the **direct** host (no `-pooler` in the hostname).
3. Neon shows `postgresql://USER:PASSWORD@HOST/DBNAME?sslmode=require`. Convert:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST/DBNAME?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `USER` |
| `SPRING_DATASOURCE_PASSWORD` | `PASSWORD` |

User and password never go inside the JDBC URL. If Neon's string includes extra libpq-only parameters (e.g. `channel_binding`), drop them unless the PostgreSQL JDBC driver supports them **(check)**.

## 2. Render + Neon A (Lead) — §17.3
1. Render → New → **Web Service** → connect the GitHub repository; branch `main`; runtime **Docker**; Dockerfile path `./Dockerfile`; instance **Free**; region **Singapore**.
2. Health check path: `/actuator/health`.
3. Settings → Build & Deploy → **Auto-Deploy: Off**.
4. Environment (§16.3 Render column):
   `SPRING_PROFILES_ACTIVE=prod`, the three `SPRING_DATASOURCE_*` values, `SOURCERIPPLE_PUBLIC_BASE_URL=https://<service>.onrender.com`, `SOURCERIPPLE_ENCRYPTION_KEY`, `SOURCERIPPLE_ADMIN_EMAIL`/`_PASSWORD`, `SOURCERIPPLE_METRICS_USERNAME`/`_PASSWORD`, `SOURCERIPPLE_TICK_TOKEN`, `SOURCERIPPLE_SCHEDULER_MODE=both`, `SOURCERIPPLE_MIN_INTERVAL_HOURS=6`, `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS=false`, `SOURCERIPPLE_DEMO_ENABLED=true`, `SOURCERIPPLE_DEMO_WEBHOOK_SECRET`, `SOURCERIPPLE_DEMO_USER_PASSWORD`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError`. `PORT` and `RENDER_GIT_COMMIT` are provided by Render.
5. Settings → **Deploy Hook** → copy into GitHub secret `RENDER_DEPLOY_HOOK_URL`. Add variable `RENDER_APP_URL=https://<service>.onrender.com`.
6. GitHub secret `TICK_TOKEN` = the Render `SOURCERIPPLE_TICK_TOKEN`. Merge `scheduler-tick.yml` and run it once with **Run workflow**.
7. Merge the `deploy-render` job, then open the release PR `develop → main`. On merge, CI calls the hook with `&ref=$GITHUB_SHA` and polls `/actuator/info` until `git.commit` matches.
8. The demo seeder runs on startup (demo enabled). Verify the golden flow on the public URL (section 5).

Render pitfalls (§17.3): spin-down after 15 min idle and ~1 min cold start; restarts lose memory (sessions are in Postgres); ephemeral disk; 512 MB RAM; 500 build minutes (only `main` deploys, image skips tests); **no 24/7 keep-alive pinger** (burns Neon CU-hours). If Q2 rules out Neon, use Render Postgres Free and create it within 30 days of grading (§17.1).

## 3. Cloud Run + Neon B (Imrul) — §17.4
One-time setup (replace `PROJECT`, `OWNER/REPO`):
```bash
gcloud config set project PROJECT
gcloud services enable run.googleapis.com artifactregistry.googleapis.com \
  iamcredentials.googleapis.com cloudscheduler.googleapis.com
gcloud artifacts repositories create sourceripple --repository-format=docker --location=asia-southeast1
# cleanup policy keeping the 2 most recent images (< 0.5 GB free storage)
gcloud artifacts repositories set-cleanup-policies sourceripple --location=asia-southeast1 --policy=infra/cloudrun/cleanup-policy.json

gcloud iam service-accounts create sr-deployer
for r in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser; do
  gcloud projects add-iam-policy-binding PROJECT --member=serviceAccount:sr-deployer@PROJECT.iam.gserviceaccount.com --role=$r; done

gcloud iam workload-identity-pools create github --location=global
gcloud iam workload-identity-pools providers create-oidc github --location=global --workload-identity-pool=github \
  --issuer-uri=https://token.actions.githubusercontent.com \
  --attribute-mapping=google.subject=assertion.sub,attribute.repository=assertion.repository \
  --attribute-condition="assertion.repository=='OWNER/REPO'"
gcloud iam service-accounts add-iam-policy-binding sr-deployer@PROJECT.iam.gserviceaccount.com \
  --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/OWNER/REPO"
```
Exact flag names **(check)** against current `gcloud` docs. Document the final commands in `infra/cloudrun/README.md`.

GitHub: secrets `GCP_WIF_PROVIDER` (`projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/github`), `GCP_SERVICE_ACCOUNT`; variables `GCP_PROJECT_ID`, `GCP_REGION=asia-southeast1`, `CLOUD_RUN_SERVICE=sourceripple`, `CLOUD_RUN_URL` (after first deploy).

Service configuration (set by `deploy-cloudrun` flags):

| Setting | Value |
|---|---|
| Billing | request-based (CPU only during requests) |
| Instances | min 0, **max 1** |
| Resources | 1 vCPU, 1 GiB, startup CPU boost on |
| Concurrency / timeout | 20 / 300 s |
| Access | `--allow-unauthenticated` |
| Env | §16.3 Cloud Run column, `SOURCERIPPLE_SCHEDULER_MODE=external`, `GIT_COMMIT=$GITHUB_SHA`, public base URL = Cloud Run URL |
| Secrets | Secret Manager **(verify the free allowance)** or env vars from CI secrets |

Cloud Scheduler **(verify: free allowance historically 3 jobs per billing account)**:
```bash
gcloud scheduler jobs create http sr-tick --location=asia-southeast1 --schedule="0 * * * *" \
  --uri="$CLOUD_RUN_URL/internal/scheduler/tick" --http-method=POST \
  --headers="X-SR-Tick-Token=$TICK_TOKEN"
```
Cloud Run pitfalls (§17.4): never rely on `@Scheduled`/`@Async`; cold start is a few seconds even with CPU boost, so warm before the demo; keep images < 0.5 GB; no billing account → raise Q3 immediately and pick a verified fallback host.

## 4. README deployment section (Imrul, §20.1 item 10)
Both platforms step by step (sections 1–3 condensed), the execution-model difference (Render: sleeping instance with internal loop + 3-hourly GitHub cron tick; Cloud Run: request-only CPU with hourly Cloud Scheduler tick), and the full environment-variable reference. Worth 5 bonus marks (§17.4).

## 5. Verification (after every release)
```bash
for U in "$RENDER_APP_URL" "$CLOUD_RUN_URL"; do
  curl -fsS "$U/actuator/health"                       # {"status":"UP"}
  curl -fsS "$U/actuator/info" | jq -r .git.commit      # == release SHA
  curl -s -o /dev/null -w '%{http_code}\n' "$U/actuator/prometheus"   # 401
done
curl -fsS -X POST -H "X-SR-Tick-Token: $TICK_TOKEN" "$RENDER_APP_URL/internal/scheduler/tick"  # 200 JSON
```
Then Ajoy's smoke list in [phase-11-deployments.md](../../phases/release/phase-11-deployments.md#d-production-smoke-tests-ajoy): golden flow on Render, roles and 403s, SSRF block, landing/pricing/waitlist/demo login.

## Demo-day runbook
**T-24 h** (§17.5):
- [ ] Neon A and B usage below 70% of CU-hours.
- [ ] Render and Cloud Run `/actuator/info` show the latest release SHA.
- [ ] Fixture reset to v1 on both deployments.
- [ ] Admin, maintainer and reviewer can log in on both.
- [ ] `scheduler-tick.yml` enabled and last run green; Cloud Scheduler last run 200.
- [ ] **Record the full backup video** of the golden flow.

**T-15 min:**
- [ ] Open both URLs to warm them up.
- [ ] Start local Compose for Grafana (`docker compose up -d`) and run the golden flow once locally so panels move.
- [ ] Log in as maintainer, reviewer and admin in three separate browser profiles.
- [ ] Open the Actions run with the failing-then-fixed evidence and the deploy log with the SHA match.

**Fallback:** if a free tier fails live, run the identical flow on local Compose, then show the backup video and the CI deploy logs.
