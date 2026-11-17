# Phase 5 · Monitoring engine and watches (+ demo kit)

## Goal
A maintainer creates a watch and gets a synchronous baseline. Sources are then checked per source on schedule (internal loop and/or tick), manually, with leases, backoff and pacing, and every attempt is recorded. In parallel, the Lead ships the deterministic demo kit (fixture pages, receiver, seeder) that the golden flow runs on.

## People
- **Owner (engine and watches):** Ajoy. Reviewers alternate Lead / Imrul.
- **Parallel (demo kit):** Lead. Reviewer: Ajoy.

## Prerequisites
- Phase 4 exit criteria met (`SafeFetchProxy`, adapters, preview).

## PRD references
FR-WCH-2..7 · FR-MON-1..10 · FR-EXT-5 (versioning and hashing) · FR-EXT-9 · FR-DEMO-1..6 · FR-ADM-4 · §9.1 tables 7–10, 19, `app_settings` · §9.3 watch status and run outcome · §11.3 execution model (DEC-07, DEC-08) · NFR-SEC-7 (quotas, manual-check limit, 200 active sources cap) · §13 defaults.

## Ordered task checklist — engine and watches (Ajoy)

### A. Schema
- [ ] `monitoring/V4__sources_and_watches.sql`: `sources`, `watches` (jsonb `section_keys`, unique `(project_id, source_id)`), `source_versions`, `source_sections`, `monitor_runs`; indexes `sources(next_check_at) WHERE status='ACTIVE'`, `source_versions(source_id, fetched_at DESC)`, `source_sections(version_id, stable_key)`, `monitor_runs(source_id, started_at DESC)` (§9.1).
- [ ] Entities: `source/domain/Source.java`, `Watch.java` (association entity projects ↔ sources), enums `WatchMode`, `WatchStatus`, `SourceStatus`, `SourceKind`; `diff/domain/SourceVersion.java`, `SourceSection.java`; `monitor/domain/MonitorRun.java`, `RunOutcome`, `RunTrigger`. Repositories in the same packages.

### B. Watches (FR-WCH)
- [ ] `source/application/WatchService.java` `create`: canonicalize URL, reuse or create the org-level source, enforce 20/project, 100/org (`QUOTA_EXCEEDED`), 200 active sources per instance, interval ≥ `SOURCERIPPLE_MIN_INTERVAL_HOURS` (`INTERVAL_TOO_SHORT`), duplicate → 409 `DUPLICATE_WATCH`; commit; then call `BaselineRunner.runBaseline(sourceId)` within 15 s if the source has no version; return 201 with `baselineStatus` `READY|FAILED|ALREADY_AVAILABLE`; a retryable baseline failure keeps the watch `ACTIVE` and scheduled with backoff, a permanent one sets `ERROR` (FR-WCH-2, FR-WCH-3).
- [ ] `source/application/BaselineRunner.java` port, implemented by `monitor/application/MonitorService` (see [architecture.md](../../guides/engineering/architecture.md#cross-module-calls)).
- [ ] `WatchService` list/get/update/delete: update name, mode, sectionKeys, interval, notify, status `ACTIVE`/`PAUSED` (cannot override `PAUSED_BY_ADMIN`); delete cascades dependencies (Phase 6 adds the table); recompute `sources.check_interval_seconds` = min active interval; source with no active watches → `INACTIVE` (FR-WCH-4, FR-MON-1).
- [ ] `source/application/WatchArchiveHandler.java` implements `ProjectArchiveListener` (pauses the project's watches) (FR-PRJ-1); `WatchOrganizationResolver` for `AccessPolicy`; summary contributor for watch count, failing sources and last check (FR-PRJ-2).
- [ ] `source/api/WatchController.java`: create, list, get, patch, delete, `POST /watches/{id}/check`, `GET /watches/{id}/sections` (FR-WCH-5, FR-WCH-6, §10.3).
- [ ] `diff/api/ChangeController.java` (first part): `GET /watches/{id}/versions` (via `diff/application/VersionQueryService`) and `GET /watches/{id}/runs` (via `monitor/application/MonitorRunQueryService`), paginated (§10.3). Change endpoints come in Phase 6.

### C. Monitoring engine (FR-MON, DEC-07, DEC-08)
- [ ] `monitor/application/MonitorScheduler.java`: `claimDue(batch)` in one short transaction (`FOR UPDATE SKIP LOCKED`, `claimed_until = now()+5 min`, batch 10) (FR-MON-2).
- [ ] `monitor/application/MonitorService.java`: `runDueWork(Duration budget)` (claim → per source `check` until budget spent), `checkWatch(watchId)` (manual, claims the same lease), `runBaseline(sourceId)`. Each check: robots/pacing → fetch (conditional GET) **outside** any transaction → adapter → record.
- [ ] `diff/application/ChangeDetectionService.java` (initial): first success → baseline version, no event (FR-MON-4); equal document hash or 304 → no version (FR-EXT-5). Phase 6 adds the diff/event path.
- [ ] `monitor/application/CheckOutcomeService.java` (`@Transactional`): writes version + sections, `monitor_runs` row (trigger, outcome, HTTP status, error code, duration, bytes) and source scheduling fields (FR-MON-6).
- [ ] `monitor/domain/BackoffPolicy.java`: retryable (timeout, network, 408, 429, 5xx) 15 m → 1 h → 4 h → 12 h → 24 h cap with jitter, honoring `Retry-After`; permanent (other 4xx, unsupported, robots, blocked) → watch `ERROR`; 5 consecutive retryable → `ERROR`; 3×404 over ≥ 24 h → potential-removal warning; next check = last attempt + interval ± 10% (FR-MON-1, FR-MON-5).
- [ ] `monitor/application/HostPacer.java`: scheduled requests to one host ≥ 10 s apart; user-initiated fetches skip it (FR-MON-10).
- [ ] `monitor/infrastructure/InternalSchedulerLoop.java`: `@Scheduled(fixedDelay = 60 s)`, active only when `sourceripple.scheduler.mode` is `internal` or `both` (FR-MON-8a).
- [ ] `monitor/api/SchedulerController.java` `POST /internal/scheduler/tick` and `monitor/infrastructure/TickSecurityConfig.java` (stateless chain, `@Order` before the session chain, `X-SR-Tick-Token` compared with `MessageDigest.isEqual`, 401 otherwise); one tick at a time → 409 `TICK_IN_PROGRESS`; 60 s budget; returns `{sourcesProcessed, deliveriesAttempted, remainingDue}` (`deliveriesAttempted` is 0 until Phase 8) (FR-MON-8b, FR-MON-9).
- [ ] Manual check rate limit 1/min/source → 429 `CHECK_RATE_LIMITED` with `Retry-After` (FR-WCH-5).

## Ordered task checklist — demo kit (Lead)
- [ ] `core/V5__app_settings.sql`: `app_settings(key, value jsonb)` (§9.1).
- [ ] `demo/infrastructure/DemoConfig.java` with `@ConditionalOnProperty("sourceripple.demo.enabled")`; every `/demo/**` route 404 when off (FR-DEMO-1).
- [ ] `demo/api/DemoController.java`: `GET /demo/vendor/payments|nutrition|feed.xml` from `src/main/resources/demo/{v1,v2}/` templates; v2 changes `customer_id` → `customer_reference` and the footer year; feed gains one item (FR-DEMO-2, Appendix A).
- [ ] `demo/application/DemoFixtureService.java`: fixture version in `app_settings`; `PUT /api/v1/admin/demo/fixture` in `admin/api/AdminController.java` (FR-DEMO-3, FR-ADM-4); 404 when the demo is off.
- [ ] `demo/api/DemoController` `POST /demo/receiver`: verify `X-SR-Signature` (HMAC-SHA256 over `timestamp.rawBody`, constant time, 5-minute window) with `SOURCERIPPLE_DEMO_WEBHOOK_SECRET`, dedupe by `X-SR-Event-Id`, keep the last 50 events with the verification result in `app_settings` key `demo.receiver.events`; `GET /api/v1/admin/demo/receiver/events` (FR-DEMO-4, NFR-SEC-5). This is an independent reference implementation; Phase 8's `WebhookSigner` must match it.
- [ ] `demo/application/DemoSeeder.java`: idempotent, on `ApplicationReadyEvent`, running as `maintainer@demo.local`; creates Demo Org, E-commerce, Mobile Checkout, Fitness App, both demo users (`SOURCERIPPLE_DEMO_USER_PASSWORD`) and the Appendix A watches, all URLs built from `SOURCERIPPLE_PUBLIC_BASE_URL` (FR-DEMO-5, FR-DEMO-6). Dependencies are added in Phase 6, the webhook endpoint in Phase 8.

## Suggested PR breakdown
| # | Branch | Author | Scope | Reviewer |
|---|---|---|---|---|
| 1 | `feature/p5-sources-watches-schema` | Ajoy | A | Lead |
| 2 | `feature/p5-watch-service` | Ajoy | B, U7, U9, U10, I4, I8, I13 | Imrul |
| 3 | `feature/p5-monitor-engine` | Ajoy | C (claims, checks, backoff, pacing, loop), U17, U18, U30 | Lead |
| 4 | `feature/p5-scheduler-tick` | Ajoy | tick endpoint and chain, I16 | Imrul |
| 5 | `feature/p5-demo-fixtures` | Lead | app_settings, fixtures, fixture switch | Ajoy |
| 6 | `feature/p5-demo-receiver-seeder` | Lead | receiver, seeder | Ajoy |

## Tests to write
| ID | Class | Intent |
|---|---|---|
| U9 | `source/application/WatchServiceTest` | 21st watch in a project and 101st in an org → `QUOTA_EXCEEDED` |
| U10 | `WatchServiceTest` | Interval below minimum → `INTERVAL_TOO_SHORT`; same (project, source) → `DUPLICATE_WATCH` |
| U17 | `diff/application/ChangeDetectionServiceTest` | Same document hash → no version, no event |
| U18 | `ChangeDetectionServiceTest` | First success → baseline version, no event |
| U30 | `monitor/application/MonitorSchedulerTest` | Only due, unleased sources claimed; backoff steps and ±10% jitter (fixed `Clock`, seeded random) |
| I8 | `source/api/WatchCreateIT` | Watch on a MockWebServer page → 201, `baselineStatus=READY`, one version stored |
| I16 | `monitor/api/SchedulerTickIT` | No token / wrong token → 401; valid token → due sources processed, response counts |
| U7 | `workspace/application/ProjectServiceTest` | Archive sets `archivedAt` and calls `ProjectArchiveListener`; `WatchArchiveHandler` pauses the project's watches |
| I4 | `identity/api/RoleAccessIT` | REVIEWER `POST /projects/{id}/watches` → 403; MAINTAINER → 201 |
| I13 | `source/api/SsrfBlockIT` | With `allow-private-targets=false`, creating a watch on a MockWebServer URL → 422 `SOURCE_FETCH_BLOCKED` and `getRequestCount()==0` |

## Exit criteria
- [ ] With Compose and the demo enabled: seeded watches on the fixture reach `baselineStatus=READY` (or baseline on the first loop).
- [ ] **Check now** on an unchanged source → run outcome `UNCHANGED` or `NOT_MODIFIED`, no new `source_versions` row.
- [ ] Fixture switch v1 ↔ v2 persists across restart; receiver accepts a correctly signed test POST and rejects a bad signature.
- [ ] U7, U9, U10, U17, U18, U30, I4, I8, I13, I16 green.

## Evidence to capture
- Monitor-run table screenshot (via SQL or Swagger) showing BASELINE then UNCHANGED.

## Risks and gotchas
- **No work after the HTTP response** (FR-MON-8, DEC-08). No `@Async`, no fire-and-forget threads. Watch create and manual check finish inside the request.
- **Never hold a DB transaction across a fetch.** Hikari has 5 connections; claim in a short transaction, fetch, then record in another.
- **The lease is the lock.** Manual checks must claim the same lease so the loop and a tick never process the same source (§11.3.4).
- **Baseline failure semantics (FR-WCH-2, FR-MON-5).** Retryable → watch stays `ACTIVE`, `baselineStatus=FAILED`, retried with backoff; permanent → `ERROR`, no automatic retry until edited or re-checked.
- **Seeder and security.** Services are `@PreAuthorize`d; the seeder must run with a temporary `SecurityContext` for the demo maintainer and clear it afterwards.
- **Seeder fetching its own URL.** On `ApplicationReadyEvent` the server is listening locally; on Render the public URL may not route yet, so the baseline may fail once and be retried.
- **Constant-time comparison** for the tick token and receiver signature (`MessageDigest.isEqual`).
- **Inject `java.time.Clock`** everywhere time matters, so U30 and later I12 can control time.
