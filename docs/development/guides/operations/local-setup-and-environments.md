# Local setup and environments

Derived from PRD §16 (container and configuration), §13 (defaults), FR-DEMO and §11.3 (scheduler modes).

## Prerequisites
| Tool | Why |
|---|---|
| Git | Clone, hooks (`scripts/setup-git-hooks.sh`) |
| Docker with Compose v2 | `docker compose up --build`; Testcontainers for ITs |
| Java 21 (Temurin) | Running/testing the backend outside Docker (Maven comes from `./mvnw`) |
| Node 22 + corepack (pnpm) | Frontend dev server and build (`corepack enable`) |
| `gh` CLI (optional) | Opening PRs from the terminal |

## Run everything with Compose (the graded path)
```bash
cp .env.example .env            # dev-only placeholder values
docker compose up --build       # db, app, prometheus, grafana
```
| URL | What |
|---|---|
| http://localhost:8080 | App (marketing site, `/app`, `/swagger-ui.html`) |
| http://localhost:3000 | Grafana (admin from `.env`, dashboard "SourceRipple Overview") |
| http://localhost:9090 | Prometheus (target `app:8080` should be UP) |

Compose holds no credential values; each secret is `${VAR:?set VAR in .env}`, so a missing `.env` fails with a clear message (§16.2). Reset the database with `docker compose down -v`.

## Run backend and frontend separately (day-to-day development)
```bash
# 1. a database for local runs
docker run -d --name sr-db -p 5432:5432 \
  -e POSTGRES_DB=sourceripple -e POSTGRES_USER=sourceripple -e POSTGRES_PASSWORD=sourceripple postgres:17-alpine

# 2. backend on :8080 with the local profile
cd backend
set -a; source ../.env; set +a
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sourceripple
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 3. frontend on :5173 (Vite proxies /api, /v3, /actuator, /demo, /mcp to :8080)
cd frontend && pnpm install && pnpm dev
```
Open http://localhost:5173. The browser sees one origin, so session cookies and the `XSRF-TOKEN` cookie work without CORS. After changing backend DTOs, run `pnpm gen:api` (backend must be running) and commit `src/lib/api/schema.d.ts`.

With `SOURCERIPPLE_PUBLIC_BASE_URL=http://localhost:8080`, demo fixture URLs point at the backend directly, which works from both :8080 and :5173.

## Profiles
| Profile | Used by | Key differences |
|---|---|---|
| `local` | Compose and `spring-boot:run` | `allow-private-targets=true`, min interval 1 h, scheduler `internal`, readable console logs, demo enabled, cookie not `Secure` |
| `test` | `./mvnw verify` (`src/test/resources/application-test.yml`) | `allow-private-targets=true`, Testcontainers datasource, demo off unless a test enables it, scheduler loop off (tests call services directly) |
| `prod` | Render, Cloud Run | `allow-private-targets` must be false (startup fails otherwise), min interval 6 h, `Secure` cookie, JSON logs, forwarded headers trusted |

## Environment variables
From PRD §16.3. "dev" = placeholder in `.env.example`; "secret" = platform secret store.

| Variable | Purpose | Local | Render | Cloud Run |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Profile | `local` | `prod` | `prod` |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | JDBC; Neon: `jdbc:postgresql://<direct host>/<db>?sslmode=require` | compose `db` | Neon A | Neon B |
| `PORT` | HTTP port | 8080 | injected | injected |
| `SOURCERIPPLE_PUBLIC_BASE_URL` | Absolute links: evidence URLs, demo fixture, receiver | `http://localhost:8080` | Render URL | Cloud Run URL |
| `SOURCERIPPLE_ENCRYPTION_KEY` | AES-256-GCM key for webhook secrets (32 bytes, base64) | dev | secret | secret |
| `SOURCERIPPLE_ADMIN_EMAIL` / `SOURCERIPPLE_ADMIN_PASSWORD` | Bootstrap admin (R3) | dev | secret | secret |
| `SOURCERIPPLE_METRICS_USERNAME` / `SOURCERIPPLE_METRICS_PASSWORD` | `/actuator/prometheus` basic auth; Compose's Prometheus `password_file` secret reads the same password | dev | secret | secret |
| `SOURCERIPPLE_TICK_TOKEN` | Tick token (`X-SR-Tick-Token`) | dev | secret | secret |
| `SOURCERIPPLE_SCHEDULER_MODE` | `internal` / `external` / `both` | `internal` | `both` | `external` |
| `SOURCERIPPLE_MIN_INTERVAL_HOURS` | Minimum watch interval | 1 | 6 | 6 |
| `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS` | SSRF exemption (refused in prod) | `true` | `false` | `false` |
| `SOURCERIPPLE_DEMO_ENABLED` | Demo kit | `true` | `true` | `true` |
| `SOURCERIPPLE_DEMO_WEBHOOK_SECRET` | Demo receiver HMAC secret | dev | secret | secret |
| `SOURCERIPPLE_DEMO_USER_PASSWORD` | Password for both demo users | dev | secret | secret |
| `GIT_COMMIT` | `/actuator/info` `git.commit` (fallback `RENDER_GIT_COMMIT`) | — | auto | set at deploy |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError` (NFR-RES-1) | optional | set | set |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Compose database | dev | — | — |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | Compose Grafana | dev | — | — |

Every other setting in PRD §13 (quotas, timeouts, caps, rate limits, retention, claim batch, lease) has a default in `application.yml` under `sourceripple.*` and can be overridden by env var through relaxed binding (see [backend-conventions.md](../engineering/backend-conventions.md#configuration-sourceripple-prd-13)).

Generate local secrets with:
```bash
openssl rand -base64 32      # SOURCERIPPLE_ENCRYPTION_KEY
openssl rand -hex 24         # tick token, demo webhook secret, metrics password
```

## Demo accounts and demo kit (FR-DEMO, FR-WEB-7)
| Account | Role | Password |
|---|---|---|
| `SOURCERIPPLE_ADMIN_EMAIL` | ADMIN | `SOURCERIPPLE_ADMIN_PASSWORD` |
| `maintainer@demo.local` | MAINTAINER (owner of Demo Org) | `SOURCERIPPLE_DEMO_USER_PASSWORD` |
| `reviewer@demo.local` | REVIEWER (member of Demo Org) | same; or **Try the live demo** (demo login, no password) |

Golden flow by hand (Appendix A):
1. Log in as the maintainer; open Demo Org → E-commerce → the payments watch (baseline present).
2. As admin (separate browser profile): Admin → Demo controls → fixture **v2** (`PUT /api/v1/admin/demo/fixture`).
3. As maintainer: **Check now** → CHANGED → change detail shows `customer_id → customer_reference`, footer ignored.
4. Impact Inbox: 2 NEW (payment-service, checkout-module), none for Fitness App.
5. Integrations → E-commerce webhook delivery log: DELIVERED; Admin → receiver events: signature valid.
6. Reset: fixture back to **v1** (or `POST /api/v1/admin/demo/reset`, P1).

Fixture pages: `/demo/vendor/payments`, `/demo/vendor/nutrition`, `/demo/vendor/feed.xml`. With `SOURCERIPPLE_DEMO_ENABLED=false`, every `/demo/**` route returns 404.

## Troubleshooting
- **App exits at startup in `prod`:** `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS=true` is refused (NFR-SEC-1.7).
- **Health DOWN:** the database is unreachable or Flyway failed (NFR-REL-2); check `docker compose logs app`.
- **Flyway checksum or "applied migration not resolved":** someone renumbered or edited a migration; reset your local DB (`docker compose down -v`).
- **ITs fail with "Could not find a valid Docker environment":** start Docker; Testcontainers needs it.
- **403 on every POST from the SPA:** the `XSRF-TOKEN` cookie wasn't issued; the SPA must call `GET /api/v1/auth/me` first.
