# Phase 8 · Integrations and admin

## Goal
Changes reach people and systems: in-app notifications, signed webhooks through a transactional outbox with retries and a delivery log, project API keys and read-only MCP tools. The admin console runs the instance. Delivers the Bridge pattern.

## People
- **Imrul:** FR-NTF + S12, FR-WHK (backend and S11 webhooks UI), S13 admin UI. Reviewer: Lead (Ajoy second).
- **Lead:** FR-KEY, FR-MCP, FR-ADM-2..4 backend, extending the Phase 6 change transaction with notification and outbox rows, S11 API keys and MCP UI, demo seeder webhook. Reviewer: Imrul (Ajoy second).
- **Ajoy:** reviews at least one PR from each; fixes core bugs found here.

## Prerequisites
- Phase 6 exit criteria met (change events and impacts in one transaction; golden flow green).
- Phase 7 in progress or done (screens to attach to); Phase 8 may overlap it (§21.3).

## PRD references
FR-NTF-1..2 · FR-WHK-1..7 · FR-KEY-1..2 · FR-MCP-1..3 · FR-ADM-2..4 · R7 · §6.2 API-key column · §9.1 tables 15–18 · §9.3 webhook delivery · §10.3 Notification, Webhook, ApiKey, Admin controllers · §10.5 payload · §11.5 Bridge · NFR-SEC-1 (webhooks), NFR-SEC-4, NFR-SEC-5 · §8.5 S11–S13.

## Ordered task checklist — notifications and webhooks (Imrul)
- [ ] `integrations/V7__notifications_and_webhooks.sql`: `notifications`, `webhook_endpoints`, `webhook_deliveries` (unique `event_id`, unique `(endpoint_id, change_event_id, event_type)`); indexes `notifications(recipient_user_id, read_at)`, `webhook_deliveries(state, next_attempt_at)` (§9.1).
- [ ] Bridge (§11.5): `notify/domain/ChangeNotification.java` (abstraction holding a `NotificationChannel`) with refined `ImpactNotification`, `SourceChangeNotification`; `notify/domain/NotificationChannel.java` (implementor) with `notify/application/InAppChannel.java` (inserts `notifications` rows, one per org member) and `notify/application/WebhookChannel.java` (inserts outbox rows for subscribed endpoints).
- [ ] `notify/application/NotificationDispatcher.java`: builds both notification kinds for a change event and sends each through both channels. The Lead wires it into `CheckOutcomeService` (see the Lead checklist). SECTIONS-mode watches notify only when a selected section changed (FR-IMP-5).
- [ ] `notify/application/NotificationService.java` + `notify/api/NotificationController.java`: list mine (unread first), mark one read, read-all (FR-NTF-2). Kinds `SOURCE_FAILING` (watch → ERROR) and `WEBHOOK_FAILING` (endpoint auto-disabled) (FR-NTF-1).
- [ ] `notify/infrastructure/SecretCipher.java`: AES-256-GCM with `SOURCERIPPLE_ENCRYPTION_KEY`, random 12-byte IV stored with the ciphertext (NFR-SEC-4).
- [ ] `notify/application/WebhookEndpointService.java` + `notify/api/WebhookController.java`: create (HTTPS in prod, SSRF-valid URL, generated 32-byte base64url secret unless ≥ 32 chars supplied, **shown once**), list, get, patch (url, eventTypes, enabled), delete, `POST /webhooks/{id}/test` (ping), `GET /webhooks/{id}/deliveries`; REVIEWER view only (FR-WHK-1, FR-WHK-6).
- [ ] `notify/application/WebhookSigner.java`: `X-SR-Signature: v1=<hex HMAC-SHA256(secret, timestamp + "." + rawBody)>` (FR-WHK-4).
- [ ] `notify/application/DeliveryService.java`: `dispatchQueued(changeEventId)` (first attempt right after commit, same request or tick, 5 s timeout) and `dispatchDue(budget)`; classify 2xx → DELIVERED, 408/429/5xx/network → RETRYING with 1 m, 5 m, 30 m, 2 h, 6 h, then FAILED after attempt 6; other 4xx and any 3xx (redirects not followed) → FAILED; auto-disable after 10 consecutive failed events (FR-WHK-5). HTTP goes through the guarded OkHttp client from `source` (NFR-SEC-1), never a second unguarded client.
- [ ] Wire delivery into `MonitorService`: after the recording transaction returns, call `dispatchQueued`; `runDueWork` and the tick also process due deliveries and report `deliveriesAttempted` (FR-MON-9).
- [ ] Payload per §10.5, absolute `evidenceUrl` from `SOURCERIPPLE_PUBLIC_BASE_URL`; no page text or secrets (FR-WHK-4). Receiver guidance in OpenAPI descriptions (FR-WHK-7).
- [ ] UI S12 `src/features/notifications/components/NotificationBell.tsx` (popover, unread count, poll 60 s, mark all read, deep links).
- [ ] UI S11 `src/features/integrations/pages/IntegrationsPage.tsx` webhooks: list, create dialog with reveal-once secret (confirmation checkbox), delivery log with state badges and attempts, payload viewer, **Send test ping**.
- [ ] UI S13 `src/features/admin/pages/AdminPage.tsx`: users (search, roles editor, enable/disable), monitor runs table, failing sources, stats tiles, demo controls (fixture v1/v2, receiver events) — guarded by `RequireRole role="ADMIN"`.

## Ordered task checklist — keys, MCP and admin backend (Lead)
- [ ] `integrations/V8__api_keys.sql`: `api_keys` (unique `key_hash`) (§9.1 #18).
- [ ] `integration/application/ApiKeyService.java` + `integration/api/ApiKeyController.java`: create `{name, scopes}` → `sr_<prefix>_<secret>` once; store SHA-256 hash + prefix; list (prefix, scopes, last used); revoke 204 (FR-KEY-1).
- [ ] `integration/infrastructure/ApiKeySecurityConfig.java`: stateless chain for `Authorization: Bearer sr_…` and `/mcp/**`, CSRF exempt (FR-AUTH-6); `ApiKeyAuthenticationFilter` → `ApiKeyPrincipal(projectId, scopes)`; other project → 404, revoked → 401; `last_used_at` at most once per minute; never reaches `/admin/**` or org management (R7, FR-KEY-2). `AccessPolicy` learns the API-key principal.
- [ ] MCP (P1, FR-MCP-1..3): Spring AI MCP server (WebMVC, Streamable HTTP) at `/mcp`, conditional bean; tools `sourceripple_list_projects`, `sourceripple_list_impacts(projectId, since?, status?)`, `sourceripple_get_change(changeId)` in `integration/application/McpTools.java` calling the same services as REST; web-derived text labeled `untrusted_web_content` (NFR-SEC-6).
- [ ] Extend the Phase 6 transaction: `monitor/application/CheckOutcomeService` calls `NotificationDispatcher` **inside** the transaction, so notification and outbox rows commit with the event and impacts (FR-NTF, FR-WHK-3, §9.1).
- [ ] `admin/application/AdminService.java` + `AdminController`: monitor runs (filterable), `GET /admin/sources?status=`, `POST /admin/watches/{id}/pause|resume` (`PAUSED_BY_ADMIN`), `GET /admin/stats`, demo controls (FR-ADM-2..4). Users and roles (FR-ADM-1) exist since Phase 2.
- [ ] `DemoSeeder`: E-commerce webhook → `<public base>/demo/receiver`, event `impact.detected`, secret `SOURCERIPPLE_DEMO_WEBHOOK_SECRET` (Appendix A).
- [ ] UI S11 API keys tab (create reveal-once, scopes, last used, revoke) and MCP connection snippet with copy.

## Suggested PR breakdown
| # | Branch | Author | Scope | Reviewer |
|---|---|---|---|---|
| 1 | `feature/p8-notifications-bridge` | Imrul | schema, Bridge, in-app, U27 | Lead |
| 2 | `feature/p8-webhook-endpoints` | Imrul | cipher, endpoint CRUD, signer, U25 | Lead |
| 3 | `feature/p8-webhook-delivery` | Imrul | DeliveryService, post-commit dispatch, U26, I12, I9 extension | Lead + Ajoy |
| 4 | `feature/p8-web-integrations-bell` | Imrul | S11 webhooks, S12 | Lead |
| 5 | `feature/p8-web-admin` | Imrul | S13 | Lead |
| 6 | `feature/p8-api-keys` | Lead | keys + chain, U29, I14, keys UI | Imrul |
| 7 | `feature/p8-mcp-read-tools` | Lead | MCP tools, snippet | Imrul |
| 8 | `feature/p8-admin-backend` | Lead | transaction extension, FR-ADM-2..4, demo webhook seed | Ajoy |

## Tests to write
| ID | Class | Intent |
|---|---|---|
| U25 | `notify/application/WebhookSignerTest` | Known secret, timestamp, body → known `v1=` hex |
| U26 | `notify/application/DeliveryServiceTest` | 200 → DELIVERED; 404 → FAILED; 429/503 → RETRYING with the next backoff step (fixed `Clock`) |
| U27 | `notify/application/NotificationDispatcherTest` | Both notification types reach both channels (2 × 2) |
| U29 | `integration/application/ApiKeyServiceTest` | Only hash stored; valid key verifies; revoked rejected |
| I12 | `notify/api/WebhookDeliveryIT` | MockWebServer 500 then 200 → RETRYING then DELIVERED; same `X-SR-Event-Id`; signature verifies |
| I14 | `integration/api/ApiKeyAccessIT` | Project A key reading project B → 404; revoked key → 401 |
| I9 | `GoldenFlowIT` (extend) | One delivery DELIVERED; demo receiver shows signature valid |

## Exit criteria
- [ ] Golden flow in Compose ends with the E-commerce delivery **DELIVERED** and the receiver showing "signature valid"; I9 extension green.
- [ ] Admin console: change roles (R4 guard works), disable a user (their session ends), pause/resume a watch, view runs, stats, demo controls.
- [ ] U25–U27, U29, I12, I14 green.
- [ ] MCP `sourceripple_get_change` returns the same evidence as `GET /changes/{id}` (Appendix A), or MCP formally moved to P2 per FR-MCP-3.

## Evidence to capture
- Delivery log screenshot (DELIVERED, attempts) and receiver events with "signature valid" (§20.2 step 7).
- Bridge code links for the Phase 10 diagram.

## Risks and gotchas
- **Outbox rows in the same transaction** as event and impacts; the HTTP attempt only after commit, still inside the request/tick (FR-WHK-3, DEC-08). Never `@Async`.
- **Sign the exact bytes you send.** Serialize once to a byte array, sign it, send it.
- **Timestamps are unix seconds**, not millis (NFR-SEC-5).
- **AES-GCM IVs must never repeat** for a key. Generate per encryption.
- **SSRF applies to webhooks** (NFR-SEC-1). The demo receiver passes because it is reached via the public base URL (FR-DEMO-5); tests use `allow-private-targets=true`.
- **Retries in tests:** don't sleep. Advance the injected `Clock` and call `dispatchDue`.
- **3xx responses** are FAILED immediately; webhook redirects are never followed (FR-WHK-5).
- **API-key chain ordering:** it must match before the session chain and must not swallow cookie requests.
- **Secrets shown once** (§8.4): the create response is the only place the secret/key appears.
