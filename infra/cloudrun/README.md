# Cloud Run deployment notes

Second deployment target (PRD §17.4, DEC-11, P1). It is set up in **Phase 11**; nothing here is
deployed yet. The step-by-step guide lives in the README "Deployment" section once Phase 11 lands.

## Shape

- Service `sourceripple`, region `asia-southeast1` (PRD §17.2; fall back to a US region with the
  database in the same region if Singapore is unavailable).
- The same root `Dockerfile` image, pushed to Artifact Registry (tag = commit SHA, keep the last
  2 images with a cleanup policy).
- Request-based billing, min instances 0, **max instances 1**, 1 vCPU, 1 GiB, startup CPU boost on,
  concurrency 20, request timeout 300 s, `--allow-unauthenticated`.
- Database: Neon Free, **project B** (separate from Render's project A), direct host, JDBC URL
  with `sslmode=require`.
- `SOURCERIPPLE_SCHEDULER_MODE=external`: Cloud Run has no CPU between requests, so no background
  loop runs. Cloud Scheduler calls `POST <CLOUD_RUN_URL>/internal/scheduler/tick` with the
  `X-SR-Tick-Token` header on schedule `0 * * * *`.
- Environment variables follow PRD §16.3; secrets come from Secret Manager or Cloud Run
  environment variables set from CI secrets, never from the repository.

## One-time setup checklist (Phase 11)

1. GCP project linked to a billing account (open question Q3).
2. Enable the Cloud Run, Artifact Registry, IAM Credentials and Cloud Scheduler APIs.
3. Artifact Registry Docker repository with a cleanup policy.
4. Deployer service account with `run.admin`, `artifactregistry.writer` and
   `iam.serviceAccountUser`.
5. Workload Identity Federation pool bound to this GitHub repository (no JSON keys).
6. GitHub secrets `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT`; variables `GCP_PROJECT_ID`,
   `GCP_REGION`, `CLOUD_RUN_SERVICE`, `CLOUD_RUN_URL`.
7. Cloud Scheduler job for the hourly tick.

## Pitfalls

- Never rely on `@Scheduled` or `@Async` work on Cloud Run (DEC-08).
- Warm the service before a demo; cold starts take a few seconds even with CPU boost.
- Keep Artifact Registry storage under the 0.5 GB free allowance.
