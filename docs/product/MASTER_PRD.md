# SourceRipple — Master Product Requirements Document (PRD)

> **This document is the single source of truth for SourceRipple.**
> Development docs (`docs/development/`) are generated from it and must cite its IDs (`FR-*`, `NFR-*`, `DEC-*`, `TST-*`).
> Where this PRD and the submitted blueprint disagree, **this PRD wins**. The blueprint remains the background and rationale document.

| Field | Value |
|---|---|
| Product | SourceRipple — Web Change and Project Dependency Monitoring System |
| Document | Master PRD (source of truth) |
| Date | 17 Nov 2026 |
| Status | Core idea **locked** (as submitted). Implementation decisions below are binding unless changed through this document. |
| Course | CSE 3220 Software Engineering Lab, mandatory 3-person team |
| Inputs | `docs/idea/SourceRipple_Full_Product_Blueprint (1).pdf` (31 Oct 2026, the submitted proposal) and `docs/idea/CSE 3220 CSE 2K22 Software Engineering Lab Project Guidelines.docx` (the grading guideline) |
| Distribution | Open source from day one (public GitHub repository, Apache-2.0, see DEC-01) |

### How to use and change this document
- **Keywords.** MUST means required for submission. SHOULD means expected unless there is a written reason not to. MAY means optional.
- **Stable IDs.** Requirement IDs never change meaning. A removed requirement keeps its ID and is marked *Withdrawn*.
- **Changing it.** Changes go through a pull request to this file, reviewed like code. This is a single living document with no version numbers; the file's Git history is the change record.
- **Verify before building.** Any figure marked **(verify)** was not confirmed at the time of writing. Check it before relying on it.

---

## Table of contents
1. Product summary
2. Problem and users
3. Usage scenarios
4. Goals, non-goals and success metrics
5. Scope and priorities
6. Roles and access control
7. Functional requirements
8. UX, screens and design language
9. Domain model and database
10. REST API
11. Architecture and technology
12. Security requirements
13. Other non-functional requirements
14. Testing requirements
15. Engineering workflow (Git, PRs, CI/CD)
16. Containerization and configuration
17. Hosting and deployment (open-source free tiers)
18. Observability (bonus)
19. Course compliance matrix
20. Deliverables: README and 15-minute demo
21. Delivery plan, team and scope-cut order
22. Risks
23. Changes from the submitted blueprint
24. Open questions
- Appendix A: Golden fixture and expected results
- Appendix B: Glossary

---

## 1. Product summary

**One-line promise.** When a webpage changes, SourceRipple shows what changed, which of your projects may depend on it, and how connected systems can respond.

A traditional website monitor answers *"What changed?"* SourceRipple answers: *"What changed, which of my systems depend on it, and what should be investigated?"*

**The locked core loop.** Every release must keep this loop working end to end:

```
Monitor a URL ──► Version + section diff ──► Match registered dependencies ──► Impact
     ▲                                                                        │
     └──── scheduled / manual / tick ◄──── Review in dashboard ◄──────────────┤
                                            Signed webhook ◄──────────────────┤
                                            REST / MCP (pull) ◄───────────────┘
```

**Product invariants.** These come from blueprint §28 and must not be broken:
1. The product is web-native first. The React dashboard and monitoring engine work with no webhook, API key, MCP client or AI agent.
2. An organization owns many projects. A project watches many unrelated URLs, and one URL (a *source*) may be watched by several projects in the same organization.
3. Only changes in a dependency's registered section, or any change for a whole-page dependency, create a potential impact. SourceRipple never declares a change "breaking".
4. A watch starts with a successful baseline. After that, it is checked on schedule whether or not anyone has the dashboard open.
5. The lab MVP fetches public HTTP(S) HTML and RSS only. There is no headless browser, no paid AI or LLM, no CAPTCHA or login bypass, and no site-wide crawl.
6. Every outgoing request, whether a source fetch or a webhook, passes SSRF and resource-limit controls.
7. Every sensitive read or write enforces role and organization/project membership, including API-key and MCP access.
8. Change events, impacts, notifications and webhook attempts are durable and auditable. Webhook delivery is at-least-once, with stable event IDs.
9. SourceRipple never edits customer code, data or chatbots. It reports evidence; the receiving system decides what to do.

### 1.1 Long-term vision and business model (DEC-14)
SourceRipple is built as an open-source product with a commercial path. It follows the **open-core + managed-cloud** model common to developer-infrastructure companies (Hatchet is one example).

- **Community edition (now).** This repository, under Apache-2.0. Self-hostable with one `docker compose up`, with the full core loop and no paid AI.
- **SourceRipple Cloud (later).** A managed subscription service. We run the fetching infrastructure and offer longer history, more frequent checks, team features and premium capabilities: JS rendering, SSO, audit exports and priority support.
- **Positioning.** *Agent-ready change intelligence for the web dependencies of software teams.*
  - "Agent-ready" means webhooks and MCP hand AI agents verified evidence.
  - SourceRipple itself runs no paid AI model, and marketing copy must never claim AI-powered analysis (FR-WEB-2).
- **In the lab version:**
  - The marketing site and pricing page are real and public.
  - Cloud plans appear as *coming soon*, with a waitlist.
  - There are **no payments, subscriptions or plan enforcement**.
  - The public deployment is labeled **Public preview**, with no uptime promise.
- **Before charging money:**
  1. Validate demand (blueprint §22: at least 3 committed pilot users).
  2. Clear the name's trademark and domain.
  3. Decide the licensing split.
  4. Move off free hosting.

  See §24, Q7–Q9.
- **Course fit.** Marketing pages are extra frontend routes in the same SPA. They don't change the mandated stack or any graded requirement.

---

## 2. Problem and users

**Problem.** Software depends on information published outside its control: API docs, provider policies, pricing pages, supplier specs, public notices. These pages change without sending a machine-readable event to the people downstream. Teams rely on manual checks or generic "page changed" alerts, and still have to work out which product is affected.

**Primary users.** Individual developers and small teams who maintain several apps, integrations, knowledge bases or automations. The developer/team use case is targeted first, because individual page-change alerts are already well served by existing tools.

| Persona | Typical job | Maps to role (§6) |
|---|---|---|
| Developer, agency engineer or SaaS engineer | Watches third-party docs across several client or product projects and declares which services depend on which sections | MAINTAINER |
| Support or documentation owner, QA, or a client stakeholder | Reviews flagged impacts, marks them investigating, resolved or dismissed, and reads the evidence | REVIEWER |
| Instance operator (self-hoster, or the course demo operator) | Manages users and roles, watches system health, stops abusive or failing watches | ADMIN |
| Downstream system or AI agent (non-human) | Receives signed webhooks, or pulls evidence through REST/MCP with a project API key | API key principal |

**Positioning (honest).** Strong competitors already exist: Firecrawl /monitor, changedetection.io, Fluxpect and Intello (blueprint §21). SourceRipple's niche is a **simple, self-hostable, platform-independent link from web changes to explicitly declared project dependencies**, with no paid AI and no lock-in to one workflow tool.

---

## 3. Usage scenarios

**A. External API docs affect two projects.** An organization has an *E-commerce* project and a *Mobile Checkout* project, and both depend on a vendor's "Payment Parameters" section. A *Fitness App* project watches unrelated docs.
1. The vendor renames `customer_id` to `customer_reference`.
2. The next check stores a new version and records a section diff.
3. Two impacts are created with review status **NEW** (one per dependent record). The Fitness project gets none.
4. A maintainer sees both in the Impact Inbox. The project webhook notifies the team's backend, and an agent can pull the exact diff over MCP.

**B. Own website, no integrations.** A small store watches its own refund-policy page and declares one *whole-page* dependency on its FAQ record. The page changes from 30 to 14 days.
1. SourceRipple records the diff and flags the FAQ.
2. A REVIEWER opens the evidence, updates the FAQ elsewhere, and marks the impact **RESOLVED** with a note.

No webhook, API key or MCP is needed.

**C. Supplier data triggers a workflow.** An operator watches a supplier spec page, with a dependency registered on a catalog-import service.
1. A spec section changes, and SourceRipple sends a signed `impact.detected` webhook.
2. The receiver verifies the signature, deduplicates by event ID, and fetches the evidence with a READ-scoped project API key.
3. The receiver decides whether to update its catalog.

**Boundary.** Webhooks announce evidence. The receiving system owns review and any mutation. MCP is pull-only and never wakes an agent.

---

## 4. Goals, non-goals and success metrics

### 4.1 Course goals (MUST)
- G1. Meet every mandatory requirement of the CSE 3220 guideline and avoid every automatic-failure condition (§19).
- G2. Score fully on the 100-mark rubric with evidence that can be shown live and in GitHub.
- G3. Earn both bonuses (+20): Monitoring & Observability (Actuator, Prometheus, Grafana) and Additional Deployment (Google Cloud Run).
- G4. Launch a public, production-quality marketing site (landing and pricing) that opens the demo and sets up the commercial path (§1.1).

### 4.2 Product goals
| ID | Metric | Target |
|---|---|---|
| PG1 | Time from sign-up to first baseline | Under 5 minutes, using one URL |
| PG2 | Time to register a section dependency after the first watch | Under 2 minutes |
| PG3 | Impact recall on the golden fixture (Appendix A) | 100% of intended links flagged |
| PG4 | Spurious impacts on footer/nav-only or unrelated-section edits | 0 |
| PG5 | Webhook reliability in the fixture (fail once, then succeed) | Same event ID, ends in DELIVERED |
| PG6 | Running cost | $0 in subscriptions and paid APIs (free tiers only, §17) |
| PG7 | Landing page quality | Lighthouse ≥ 90 for performance, accessibility, best practices and SEO; LCP < 2.5 s on a mid-range mobile connection |
| PG8 | Commercial signal | Waitlist sign-ups and pilot conversations tracked from launch; no revenue target in the lab |

### 4.3 Non-goals (lab version)
- AI, LLM or semantic change judging.
- JavaScript rendering or a headless browser.
- Site-wide crawling or sitemap discovery.
- Automatic dependency discovery from code or manifests.
- Visual or screenshot diffs.
- SSO, per-project roles or enterprise audit exports.
- Payments, subscriptions or plan enforcement. The pricing page shows planned plans with a waitlist, and no card data is ever collected (§1.1).
- Editing customer systems.
- Guaranteed 24/7 monitoring on free hosting.

---

## 5. Scope and priorities

| Priority | Capability | Requirement refs |
|---|---|---|
| **P0 (must ship)** | Register, login, logout, BCrypt, persistent sessions | FR-AUTH |
| P0 | Three roles (ADMIN, MAINTAINER, REVIEWER) and organization membership | §6, FR-ORG |
| P0 | Full CRUD for organizations, projects, watches, dependencies and webhook endpoints | FR-ORG, FR-PRJ, FR-WCH, FR-DEP, FR-WHK |
| P0 | URL preview, synchronous baseline, manual check, scheduled checks (internal loop and external tick) | FR-WCH, FR-MON |
| P0 | Safe fetching (SSRF guard, limits, robots.txt); HTML and RSS adapters | FR-MON, NFR-SEC |
| P0 | Section extraction, hashing, section diff, change events with stored evidence, retention | FR-EXT |
| P0 | Impact engine, Impact Inbox, review workflow, in-app notifications | FR-IMP, FR-NTF |
| P0 | Signed webhooks with outbox, retries and a delivery log | FR-WHK |
| P0 | Admin console (users and roles, monitor runs, pause watches) | FR-ADM |
| P0 | Demo kit (fixture pages and a verifying webhook receiver) | FR-DEMO |
| P0 | Global exception handling and OpenAPI docs | FR-API |
| P0 | ≥20 unit and ≥10 integration tests in CI; Docker/Compose; Git flow with protection; CI/CD auto-deploy to Render; README | §14–§17, §20 |
| P0 | **Public marketing site**: landing, pricing (Cloud plans on a waitlist), privacy and terms, demo login, SEO basics | FR-WEB, §8.6 |
| **P1 (bonus / should)** | Actuator, Prometheus and Grafana with a provisioned dashboard | §18 |
| P1 | Second deployment on Google Cloud Run (with a Cloud Scheduler tick) | §17 |
| P1 | Project API keys; MCP read tools (`list_projects`, `list_impacts`, `get_change`) | FR-KEY, FR-MCP |
| **P2 (if time)** | MCP write tools; webhook redelivery button; simple include/exclude text filters; frontend unit tests; Grafana Cloud scraping of a live deployment; login lockout; prerendering marketing pages; optional self-hosted cookieless analytics | various |
| **Out of lab scope** | Sitemap/path discovery, JS rendering, visual diffs, AI judging, repository/manifest import, SSO, multi-region, payments and subscriptions, plan enforcement | §4.3 |

---

## 6. Roles and access control

The guideline's role triple (ADMIN, SELLER, BUYER) is applied as **domain-equivalent roles**. The team was told the exact names are not required.

| Role | Guideline equivalent | Who | Summary |
|---|---|---|---|
| **ADMIN** | ADMIN | Instance operator | Manages users and roles, sees operational data (users, sources, monitor runs), pauses abusive or failing watches, runs demo controls. Does **not** automatically see project content such as dependencies or impacts. |
| **MAINTAINER** | SELLER (produces the monitoring configuration) | Developer or owner | Creates organizations and projects, and configures watches, dependencies, webhooks and API keys. Also has every REVIEWER capability. |
| **REVIEWER** | BUYER (consumes the results) | Stakeholder or triager | Reads projects, changes and evidence in the organizations they belong to, and triages impacts. Cannot change configuration. |

### 6.1 Rules
- **R1. Roles are global and stored many-to-many.** Roles live in `user_roles` (users ↔ roles), so a user may hold several, for example ADMIN + MAINTAINER for a demo operator.
- **R2. Registration.** `POST /auth/register` takes `accountType` ∈ {`MAINTAINER`, `REVIEWER`}. ADMIN can never be self-assigned (the request is rejected with 400 `ROLE_NOT_ALLOWED`).
- **R3. Bootstrap admin.** On startup, if no ADMIN exists, one is created from `SOURCERIPPLE_ADMIN_EMAIL` / `SOURCERIPPLE_ADMIN_PASSWORD`. If either variable is missing, startup logs a warning and creates no admin. Credentials are never hard-coded.
- **R4. Role changes.** An ADMIN can grant or revoke roles and enable or disable users. An ADMIN cannot remove their own ADMIN role, and at least one enabled ADMIN must always remain.
- **R5. Organization membership.** The creator of an organization is its **owner** and a member (`organization_members`, users ↔ organizations, many-to-many). The owner adds and removes members by email (existing accounts only). Every organization-scoped resource requires membership. Within an organization, what a member can do comes from their global role.
- **R6. Owner-only actions.** Renaming or deleting the organization, and managing members.
- **R7. API keys.** API keys are project-scoped principals with scopes `READ` and/or `WRITE`. They never reach `/admin/**`, organization management, members, or other projects.

### 6.2 Permission matrix
"Member" means the user belongs to the organization that owns the resource. ✓ = allowed, ✗ = 403, — = not applicable.

| Capability | ADMIN (non-member) | MAINTAINER (member) | REVIEWER (member) | API key (own project) |
|---|---|---|---|---|
| Register, login, logout, view own profile | ✓ | ✓ | ✓ | — |
| Create an organization | ✗ (unless also MAINTAINER) | ✓ | ✗ | ✗ |
| Rename or delete an organization; manage members | ✗ | owner only | ✗ | ✗ |
| View organizations, projects, watches, versions, changes, evidence | ✗ (404) | ✓ | ✓ | READ |
| Create, update or archive a project | ✗ | ✓ | ✗ | ✗ |
| Preview a URL; create, update or delete a watch; manual check | ✗ | ✓ | ✗ | WRITE (watch create/check only, P2) |
| Create, update or delete a dependency | ✗ | ✓ | ✗ | WRITE (P2) |
| View impacts; change review status with a note | ✗ | ✓ | ✓ | READ / WRITE (status, P2) |
| Manage webhooks; view delivery log; send test ping | ✗ | ✓ | view only | ✗ |
| Create, list or revoke API keys | ✗ | ✓ | ✗ | ✗ |
| Read and clear own notifications | ✓ | ✓ | ✓ | — |
| `/admin/**`: users, roles, enable/disable, monitor runs, sources, pause/resume a watch, stats, demo controls | ✓ | ✗ | ✗ | ✗ |

### 6.3 Enforcement: three layers, all MUST
1. **URL rules** in the Spring Security `SecurityFilterChain`:
   - `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/demo-login` (demo only) and `/api/v1/public/**` (waitlist) are public and rate-limited.
   - `/api/v1/admin/**` requires `hasRole('ADMIN')`.
   - `/internal/scheduler/tick` requires the tick token (FR-MON-9).
   - `/mcp/**` requires an API key.
   - `/actuator/health` and `/actuator/info` are public; `/actuator/prometheus` requires the metrics credential.
   - The SPA's static assets, the marketing routes and `/demo/vendor/**` are public (the last only when the demo is enabled).
   - Everything else requires authentication.
2. **Method security** (`@EnableMethodSecurity`) on application services. Every write method declares its role, for example `@PreAuthorize("hasRole('MAINTAINER')")`, and impact review declares `hasAnyRole('MAINTAINER','REVIEWER')`.
3. **Resource access policy.** An `AccessPolicy` service resolves the organization from the resource ID and checks membership. It is called inside services or through `@PreAuthorize("@access.canReadProject(#projectId)")`.
   - A non-member gets **404** (the resource's existence is not revealed).
   - A member without the right role gets **403**.
   - The server never trusts an `organizationId` sent by the client.

---

## 7. Functional requirements

### FR-AUTH: Authentication
- **FR-AUTH-1 Register.** `POST /api/v1/auth/register` takes `{email, password, displayName, accountType}`.
  - Email is case-insensitive and unique (409 `EMAIL_TAKEN`).
  - Passwords are at least 10 characters, are stored only as a BCrypt hash, and are never logged.
- **FR-AUTH-2 Login.** `POST /api/v1/auth/login` takes `{email, password}`.
  - On success it creates a server session and returns `{user, roles}`.
  - Invalid credentials return 401 with a generic message. Disabled users get 401 `ACCOUNT_DISABLED`.
- **FR-AUTH-3 Logout.** `POST /api/v1/auth/logout` invalidates the server session, clears the cookie and returns 204.
- **FR-AUTH-4 Current user.** `GET /api/v1/auth/me` returns the profile, roles and organizations.
- **FR-AUTH-5 Session storage.** Sessions are stored in PostgreSQL (Spring Session JDBC), so they survive free-tier restarts and spin-downs.
  - Cookie: HttpOnly, `Secure` in production, `SameSite=Lax`.
  - Idle timeout: 8 hours.
- **FR-AUTH-6 CSRF.** Cookie-authenticated state-changing requests require a CSRF token, using Spring Security's documented SPA cookie-to-header setup (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header). API-key and tick requests are stateless and exempt.
- **FR-AUTH-7 Disabling users.** Disabling a user takes effect on their next request; their sessions are deleted.
- *P2:* lock login after 5 failures in 15 minutes.

### FR-ORG: Organizations and membership
- **FR-ORG-1 CRUD.** Create (MAINTAINER), list mine, get, rename (owner) and delete (owner).
  - Delete is allowed only when every project is archived. Otherwise it returns 409 `ORG_NOT_EMPTY`.
- **FR-ORG-2 Membership.** List members (any member), add a member by email (owner, 201; 404 if the account doesn't exist) and remove a member (owner, 204). The owner cannot be removed.
- **FR-ORG-3 Quota.** At most 5 organizations per user (configurable).

### FR-PRJ: Projects
- **FR-PRJ-1 CRUD.** Create, list, get, update (name and description) and delete.
  - Delete is a soft archive: it sets `archived_at`, pauses the project's watches and returns 204.
  - The slug is unique within the organization.
- **FR-PRJ-2 Summary.** `GET /projects/{id}/summary` returns the watch count, impacts by review status, failing sources and the last check time.

### FR-WCH: Watches (a project ↔ source link)
- **FR-WCH-1 Preview.** `POST /projects/{id}/watches/preview` takes `{url}`.
  - It fetches synchronously through the safe fetcher and returns `{canonicalUrl, kind: HTML|RSS, title, sections:[{key,title,excerpt}], warnings[]}`.
  - Warnings include `LIKELY_JS_RENDERED`, `ROBOTS_DISALLOWED` and `LARGE_PAGE` (decoded size over 1 MB).
  - Nothing is saved. Rate limit: 10 previews per minute per user.
- **FR-WCH-2 Create.** `POST /projects/{id}/watches` takes `{url, name?, mode: WHOLE_PAGE|SECTIONS, sectionKeys[], intervalHours, notifyOnChange}`. It:
  1. canonicalizes the URL;
  2. reuses or creates the organization-level source;
  3. creates the watch;
  4. runs the **baseline synchronously** within a total budget of 15 s if the source has no version yet;
  5. returns **201** with `baselineStatus: READY|FAILED|ALREADY_AVAILABLE`.
  - If the baseline fails with a retryable error, the watch stays `ACTIVE` and is retried on schedule (FR-MON-5). A permanent failure puts it in `ERROR`.
- **FR-WCH-3 Limits.** At most 20 watches per project and 100 per organization; one watch per (project, source), otherwise 409.
  - `intervalHours`: default 24, minimum `SOURCERIPPLE_MIN_INTERVAL_HOURS` (6 in production, 1 locally).
- **FR-WCH-4 Read, update, delete.** List, get, update (name, mode, sectionKeys, interval, notify, status `ACTIVE`/`PAUSED`) and delete (204). Deleting cascades to the watch's dependencies; the UI asks for confirmation and shows the count. A source left with no active watches becomes `INACTIVE` and is no longer fetched.
- **FR-WCH-5 Manual check.** `POST /watches/{id}/check` runs a check **synchronously** and returns **200** with the monitor run `{outcome, changeEventId?, httpStatus, durationMs}`. Rate limit: 1 per minute per source (429 `CHECK_RATE_LIMITED`).
- **FR-WCH-6 Sections.** `GET /watches/{id}/sections` lists the sections of the latest version; the dependency picker uses it.
- **FR-WCH-7 Statuses.** `ACTIVE`, `PAUSED` (by a user), `PAUSED_BY_ADMIN`, `ERROR` (permanent failure, or 5 consecutive retryable failures), `UNSUPPORTED` (content type, or JS-only content).

### FR-MON: Monitoring engine
- **FR-MON-1 Per-source scheduling (DEC-07).** The engine fetches each source once, no matter how many watches it has.
  - `sources.check_interval` = the minimum `intervalHours` of its active watches.
  - `next_check_at` = last attempt + interval, with ±10% jitter.
- **FR-MON-2 Claiming due work.** A worker claims due sources in one transaction using `SELECT … FOR UPDATE SKIP LOCKED`, sets a lease (`claimed_until = now() + 5 min`) and processes at most `N` sources (default 10) per run. A crashed run's lease simply expires.
- **FR-MON-3 Fetching.** Fetches go through the safe fetcher (NFR-SEC-1) as conditional GETs (`If-None-Match` / `If-Modified-Since`).
  - HTTP 304 counts as a success with no new version.
  - The User-Agent is `SourceRippleBot/<version> (+<repo URL>)`.
- **FR-MON-4 Baseline.** The first successful fetch becomes the baseline version. It never produces a change event.
- **FR-MON-5 Failure handling.**
  - *Retryable* (timeout, network error, 408, 429, 5xx): exponential backoff (15 m, 1 h, 4 h, 12 h, then 24 h cap) with jitter, honoring `Retry-After`.
  - *Permanent* (other 4xx, unsupported content type, robots disallow, blocked destination): watch status becomes `ERROR`, with no automatic retry until a user edits or re-checks it.
  - A failed fetch is **never** treated as deleted content. A "potential removal" warning appears only after three consecutive 404s spanning at least 24 hours.
- **FR-MON-6 Monitor runs.** Every attempt writes a `monitor_runs` row: trigger, outcome, HTTP status, error code, duration and bytes.
- **FR-MON-7 robots.txt.** Each host's robots.txt is fetched (through the same safe fetcher), parsed with crawler-commons and cached for 24 hours. A disallow produces permanent failure `ROBOTS_DISALLOWED`.
- **FR-MON-8 Triggers (DEC-08).** The same `MonitorService.runDueWork(budget)` is invoked by:
  - (a) an internal scheduler loop every 60 s, when `SOURCERIPPLE_SCHEDULER_MODE` is `internal` or `both`;
  - (b) `POST /internal/scheduler/tick`, when the mode is `external` or `both`.
  - **No work may run after an HTTP response has been sent.** There is no fire-and-forget `@Async` for business work, because Cloud Run gives no CPU outside requests (§17.4).
- **FR-MON-9 Tick endpoint.** Requires the header `X-SR-Tick-Token`, compared in constant time to `SOURCERIPPLE_TICK_TOKEN`; otherwise 401.
  - Processes due sources, then due webhook deliveries, within a 60 s budget.
  - Returns `{sourcesProcessed, deliveriesAttempted, remainingDue}`.
  - Only one tick runs at a time; a concurrent tick returns 409.
- **FR-MON-10 Per-host pacing.** Scheduled requests to the same host are at least 10 s apart. At most 2 fetches run concurrently per instance. User-initiated fetches (preview, create, manual check) are governed by the per-user rate limits instead.

### FR-EXT: Extraction, versions and diff
- **FR-EXT-1 Adapters.** Each `SourceAdapter` turns a fetched document into a `NormalizedDocument{title, sections[{key,title,text,sha256}], contentSha256}`.
  - `HtmlPageAdapter` uses Jsoup.
  - `RssFeedAdapter` uses Jsoup's XML parser; each item is a section with key = GUID or link.
  - `SourceAdapterFactory` picks the adapter from the response content type and a sniff of the body.
- **FR-EXT-2 HTML cleaning.**
  - Remove `script`, `style`, `noscript`, `template`, `iframe`, `form`, `nav`, `header`, `footer` and `aside`, plus elements with ARIA roles `navigation`, `banner` or `contentinfo`.
  - Main content is `main`, else `article`, else `[role=main]`, else `body`.
  - Keep headings, paragraphs, lists, tables (serialized as `cell | cell` rows) and `pre`/`code`.
- **FR-EXT-3 Sections.**
  - Split at `h1`–`h4`.
  - Key: the heading's `id` or nearest anchor if present, else the slugified heading path (for example `payments/parameters`). Duplicate keys get `-2`, `-3`.
  - Text before the first heading is `_intro`.
- **FR-EXT-4 Normalization.** Unicode NFC, collapse whitespace, strip zero-width characters, trim. Hashes are SHA-256.
- **FR-EXT-5 Versioning.** If the document hash equals the latest version's hash, no version is stored. Otherwise a new `source_versions` row and its `source_sections` rows are stored.
- **FR-EXT-6 Section matching.**
  - Match by key first.
  - Fallback for unmatched sections: heading similarity ≥ 0.8 plus a content-fingerprint check, recorded as `UNCERTAIN`.
  - Still unmatched: `ADDED` or `REMOVED`, with state `MISSING` for removed sections.
  - Changed matched sections: `MODIFIED`, with a word-level diff (java-diff-utils).
- **FR-EXT-7 Change event.** Exactly one change event per (source, from-version, to-version), protected by a unique `fingerprint` = SHA-256(sourceId | fromHash | toHash).
  - Each changed section's evidence (old text, new text, diff operations) is stored in `change_event_sections`, so the evidence survives version pruning.
- **FR-EXT-8 Retention.** Keep the last 10 versions per source (configurable). Pruning sets `from_version_id`/`to_version_id` to null on old events and never deletes evidence.
- **FR-EXT-9 Unsupported pages.** If the extracted text is under 200 characters and the page has a script-mounted root, status becomes `UNSUPPORTED` with reason `LIKELY_JS_RENDERED`.

### FR-DEP: Dependencies
- **FR-DEP-1 Belongs to a watch.** A dependency belongs to a **watch**, and therefore to that project and source.
  - Fields: `objectType` ∈ {SERVICE, CODE_COMPONENT, WORKFLOW, FAQ, KNOWLEDGE_RECORD, DATASET, OTHER}, `externalId`, `displayName`, `description?`, `sectionKey?`, `metadata?` (JSON, ≤ 4 KB), `active`.
- **FR-DEP-2 Whole-page dependencies.** `sectionKey = null` means the whole page. Otherwise the key must exist in the latest version (422 `SECTION_NOT_FOUND`).
- **FR-DEP-3 CRUD and uniqueness.** Full CRUD; (watch, sectionKey, externalId) is unique (409).

### FR-IMP: Impacts and review
- **FR-IMP-1 Impact creation.** In the same transaction as the change event, for every active dependency on any active watch of that source (non-archived projects):

  | Situation | Result |
  |---|---|
  | Whole-page dependency, and any section changed | Impact, confidence `MATCHED` |
  | Dependency's section is `MODIFIED`, or `REMOVED` and confirmed | Impact, confidence `MATCHED` |
  | Dependency's section is `UNCERTAIN` or `MISSING` | Impact, confidence `UNCERTAIN` |
  | Otherwise | No impact |

- **FR-IMP-2 Uniqueness and starting state.** One impact per (change event, dependency). It starts with review status **NEW**.
- **FR-IMP-3 Review workflow.**
  - Transitions: `NEW → INVESTIGATING → RESOLVED | DISMISSED`, with `NEW → RESOLVED | DISMISSED` allowed, and reopening `RESOLVED|DISMISSED → INVESTIGATING`.
  - Dismissing requires a note.
  - Each change records reviewer and timestamp. Invalid transitions return 409 `INVALID_TRANSITION`.
- **FR-IMP-4 Impact Inbox.** Filter by project, review status, confidence, source and date range; paginate; newest first. Each row links to its evidence.
- **FR-IMP-5 Watches without dependencies.** Their changes still appear in Change History and trigger `SOURCE_CHANGED` notifications. A `SECTIONS`-mode watch is notified only when one of its selected sections changed.

### FR-NTF: In-app notifications (InAppChannel, part of the Bridge pattern)
- **FR-NTF-1 Kinds.** `IMPACT_DETECTED`, `SOURCE_CHANGED`, `SOURCE_FAILING`, `WEBHOOK_FAILING`. One row per recipient (each member of the organization).
- **FR-NTF-2 API and UI.** List mine (unread first), mark one as read, mark all as read. The UI shows a bell with an unread count and polls every 60 s.

### FR-WHK: Webhooks (WebhookChannel, part of the Bridge pattern)
- **FR-WHK-1 Endpoint CRUD.**
  - Create: `{url, eventTypes[], secret?}`. The URL must be HTTPS in production and pass the SSRF rules.
  - The secret is generated (32 random bytes, base64url) unless one of at least 32 characters is supplied. It is **shown once** and stored with AES-GCM encryption under `SOURCERIPPLE_ENCRYPTION_KEY`.
  - List, get, update (url, eventTypes, enabled) and delete.
- **FR-WHK-2 Event types.**
  - `impact.detected`: per project, per change event, listing that project's new impacts.
  - `source.changed`: a change on a watched source that passes the watch's section filter.
  - `ping`: a test event.
- **FR-WHK-3 Outbox.** Delivery rows are inserted in **the same transaction** as the change event and impacts. The `webhook_deliveries` table *is* the outbox. (endpoint, change event, event type) is unique, and `event_id` is stable across retries.
- **FR-WHK-4 Request format.** `POST` a JSON body with headers `X-SR-Event-Id`, `X-SR-Event-Type`, `X-SR-Timestamp` (unix seconds) and `X-SR-Signature: v1=<hex HMAC-SHA256(secret, timestamp + "." + rawBody)>`.
  - The body carries IDs, statuses, section keys and an absolute `evidenceUrl`. It never includes full page text or secrets.
- **FR-WHK-5 Dispatch and retry.**
  - The first attempt happens right after commit, inside the same request or tick, with a 5 s timeout.
  - 2xx → `DELIVERED`.
  - 408, 429, 5xx or a network error → `RETRYING`, with backoff of 1 m, 5 m, 30 m, 2 h and 6 h (6 attempts in total), then `FAILED`.
  - Any other 4xx, or any 3xx (webhook redirects are not followed) → `FAILED` immediately.
  - States: `QUEUED → DELIVERING → DELIVERED | RETRYING → FAILED`.
  - An endpoint is auto-disabled after 10 consecutive failed events, which also triggers a `WEBHOOK_FAILING` notification.
- **FR-WHK-6 Delivery log and testing.** View the delivery log per endpoint (attempts, response code, error) and send a test ping. *P2:* manual redelivery.
- **FR-WHK-7 Receiver guidance.** The documentation (README and OpenAPI) gives receiver guidance: verify the signature in constant time, reject timestamps older than 5 minutes, and deduplicate by `X-SR-Event-Id`.

### FR-KEY: Project API keys (P1)
- **FR-KEY-1 Lifecycle.** Create `{name, scopes}`, which returns `sr_<prefix>_<secret>` **once**. Only the SHA-256 hash is stored, along with the prefix for display. List (prefix, scopes, last used) and revoke (204).
- **FR-KEY-2 Usage.** Send `Authorization: Bearer sr_…`. A key may only access its own project's resources: other projects return 404 and revoked keys return 401. `last_used_at` is updated at most once per minute.

### FR-MCP: MCP interface (P1 read tools, P2 write tools)
- **FR-MCP-1 Endpoint.** A Streamable HTTP endpoint at `/mcp`, authenticated by a project API key.
  - P1 tools: `sourceripple_list_projects`, `sourceripple_list_impacts(projectId, since?, status?)`, `sourceripple_get_change(changeId)`.
  - P2 tools: `track_source`, `register_dependency`, `acknowledge_impact` (WRITE scope).
- **FR-MCP-2 Shared services.** Tools call the same application services as REST, so access rules and outputs are identical. Every web-derived text field is labeled `untrusted_web_content`.
- **FR-MCP-3 Fallback.** If the Spring AI MCP server starter does not support the chosen Spring Boot version, choose the Boot version the starter supports (decided in Phase 1). If that is impossible, MCP moves to P2. REST and webhooks are unaffected.

### FR-ADM: Admin console (ADMIN only)
- **FR-ADM-1 Users.** List and search; enable or disable; replace roles under rule R4.
- **FR-ADM-2 Monitoring.** All monitor runs (filterable), failing sources, and pause/resume of any watch (`PAUSED_BY_ADMIN`, which the maintainer cannot override).
- **FR-ADM-3 Stats.** Counts of users, organizations, active watches, checks in the last 24 h, changes, impacts, and webhook success rate.
- **FR-ADM-4 Demo controls** (only when the demo is enabled): switch the fixture version and view the demo receiver's events (FR-DEMO).

### FR-DEMO: Demo kit (deterministic, P0 for the presentation)
- **FR-DEMO-1 Enablement.** Enabled only when `SOURCERIPPLE_DEMO_ENABLED=true`. Otherwise every `/demo/**` route returns 404.
- **FR-DEMO-2 Fixture pages.** Public pages served by the app:
  - `GET /demo/vendor/payments`: v1 has `customer_id, amount`; v2 has `customer_reference, amount` and a changed footer year.
  - `GET /demo/vendor/nutrition`: never changes.
  - `GET /demo/vendor/feed.xml`: RSS, which gains one item in v2.
- **FR-DEMO-3 Fixture version.** `PUT /api/v1/admin/demo/fixture {version: "v1"|"v2"}`. The version is stored in the database so it survives restarts.
- **FR-DEMO-4 Demo receiver.** `POST /demo/receiver` (public) verifies the HMAC with `SOURCERIPPLE_DEMO_WEBHOOK_SECRET`, deduplicates by event ID and stores the last 50 events (in `app_settings`, key `demo.receiver.events`) with the verification result. ADMIN views them at `GET /api/v1/admin/demo/receiver/events`.
- **FR-DEMO-5 Public URLs.** Fixture and receiver URLs are built from `SOURCERIPPLE_PUBLIC_BASE_URL`, so in production they travel over the public internet and **pass the SSRF guard legitimately**. They need no exemption.
- **FR-DEMO-6 Seeding.** An idempotent demo seeder (enabled only with the demo) creates:
  - the organization "Demo Org" with projects E-commerce, Mobile Checkout and Fitness App;
  - the watches and dependencies from Appendix A;
  - demo users `maintainer@demo.local` and `reviewer@demo.local`, with passwords taken from environment variables.

### FR-API: Cross-cutting API behavior
- **FR-API-1 Global exceptions.** A `@RestControllerAdvice` maps every exception to the error envelope (§10.2) with the right status. Stack traces are never returned.
- **FR-API-2 Validation.** Bean Validation on all request DTOs. Field errors come back as 400 `VALIDATION_FAILED` with `fieldErrors[]`.
- **FR-API-3 Request IDs.** Every response carries `X-Request-Id`, either propagated or generated. It is also in the MDC and in error bodies.
- **FR-API-4 OpenAPI.** springdoc-openapi serves `/v3/api-docs` and `/swagger-ui.html`, both public and read-only.

### FR-WEB: Public marketing site, pricing and waitlist
- **FR-WEB-1 Routes.**
  - Public: `/` (landing), `/pricing`, `/privacy`, `/terms`, `/login`, `/register`.
  - The authenticated app lives under `/app/**`.
  - A signed-in visitor sees **Open dashboard** instead of **Log in** in the marketing nav.
  - Unknown routes show the 404 page.
- **FR-WEB-2 Honest copy.** This is a hard rule.
  - Never claim AI-powered analysis; the product is *agent-ready* (webhooks + MCP).
  - **No made-up customer logos, testimonials, user counts, uptime figures or "most popular" badges.**
  - Social proof comes only from real sources, such as the live GitHub star count (FR-WEB-8).
  - Planned features are labeled *Coming soon*.
- **FR-WEB-3 Landing page.** Sections as specified in §8.6. Every call to action works: Start free, Try the live demo, Star on GitHub, View pricing.
- **FR-WEB-4 Pricing page.**
  - Plans come from one typed config file (`frontend/src/marketing/plans.ts`), so names, limits, prices and call-to-action states change in one place.
  - Monthly/yearly toggle, plan cards, feature comparison table and FAQ.
  - Each plan's call to action has a state: `available` (Community, Cloud Hobby), `waitlist` (Pro, Business) or `contact` (Enterprise).
  - Prices are marked *illustrative* until validation.
- **FR-WEB-5 Waitlist.** `POST /api/v1/public/waitlist` takes `{email, plan, company?, message?, consent: true, website: ""}`.
  - `website` is a hidden honeypot. If it is filled, the request is silently dropped.
  - Missing consent returns 400.
  - Emails are stored normalized and unique, and the plan of interest is updated on repeat sign-ups.
  - The response is **always 202 with the same body**, whether the email is new or not, so it never reveals which emails are already registered.
  - Rate limit: 5 per hour per IP.
- **FR-WEB-6 Waitlist admin.** ADMIN can list and search sign-ups, export them as CSV and delete one on request (privacy).
- **FR-WEB-7 Live demo access.** `POST /api/v1/auth/demo-login` exists only when the demo is enabled.
  - It starts a session as the demo REVIEWER without exposing any password. Rate limit: 10 per hour per IP.
  - *P1:* ADMIN can **reset demo data** (`POST /api/v1/admin/demo/reset`), which re-runs the seeder and sets the fixture back to v1.
- **FR-WEB-8 GitHub stars.** The public GitHub API is called from the browser, cached for one hour in `sessionStorage`. If the call fails, the button simply shows "Star on GitHub" with no number.
- **FR-WEB-9 SEO and sharing.**
  - A title and meta description for each route, a canonical URL, Open Graph and Twitter card tags with a static `og.png`, a favicon set and `theme-color`.
  - `robots.txt` allows marketing pages and disallows `/app` and `/api`. `sitemap.xml` lists the marketing routes.
  - *P2:* prerender the marketing routes to static HTML at build time.
- **FR-WEB-10 Legal pages.**
  - **Privacy** explains what is stored: account email, password hash, monitored URLs and page text, waitlist emails, and no ads or tracking.
  - **Terms** cover acceptable use (no monitoring that breaks a site's terms) and the public preview (no SLA).
  - Both are marked as drafts to be reviewed before any commercial launch.
- **FR-WEB-11 Analytics.** None by default (NFR-SEC-10). *P2:* optional self-hosted, cookieless analytics (for example Umami or Plausible Community Edition), enabled only through an environment variable.

---

## 8. UX, screens and design language

### 8.1 Design direction (DEC-12)
- **Feel.** Dark-first, modern developer-tool aesthetic, in the style of current AI/infra SaaS consoles: calm, dense, precise and fast.
- **Reference.** The **Hatchet** dashboard and site (hatchet.run, MIT-licensed repo `hatchet-dev/hatchet`, dashboard in `frontend/app`).
  - Its dashboard stack is very close to ours: React, Vite, Tailwind with class-based dark mode, shadcn/ui on Radix, TanStack Query/Table/Router, cmdk, React Flow with dagre, Recharts, React Hook Form with Zod, and lucide icons.
  - Its dark theme uses a deep navy base, `--background: 226 82% 6%`, with slate borders and muted surfaces (`217.2 32.6% 17.5%`).
- **What we take from Hatchet.** The *patterns*:
  - sidebar shell with a workspace switcher;
  - dense run/event tables with status badges;
  - a right-hand detail sheet;
  - a DAG view;
  - a command palette;
  - relative timestamps with exact-time tooltips;
  - copy buttons on IDs;
  - calm dark surfaces.
- **What we must not copy.** Hatchet's name, logo, wordmark, screenshots, illustrations or marketing copy. SourceRipple has its own name, a "ripple" mark and its own accent palette.
  - If any MIT-licensed Hatchet source file is copied (not just used as inspiration), keep its copyright and MIT notice and list it in `THIRD_PARTY_NOTICES.md`.
- **Rule for every UI library.** It must be open source under a license compatible with Apache-2.0 (MIT, ISC, Apache-2.0, BSD, or SIL OFL for fonts). No paid UI kits. Each source is recorded in `THIRD_PARTY_NOTICES.md`.

### 8.2 Frontend stack (DEC-13)
| Concern | Choice | Notes |
|---|---|---|
| Framework | **React 19 + TypeScript (strict) + Vite** | SPA built into the Spring Boot image (DEC-04) |
| Package manager | pnpm (via corepack) | Lockfile committed |
| Routing | React Router v7 | Route-level code splitting; role-guarded routes |
| Server state | **TanStack Query v5** | All API data lives here; query-key factory per feature; no server data in Zustand |
| Client/UI state | **Zustand** (with `persist` for preferences) | Theme, sidebar collapsed, active org/project, inbox filters, diff view mode, command-palette open |
| API client | `openapi-typescript` + `openapi-fetch`, generated from `/v3/api-docs` | Typed contract shared with the backend; sends cookies and the CSRF header automatically |
| Styling | **Tailwind CSS v4** (CSS-first `@theme` tokens) | Dark default; tokens in §8.3 |
| Components | **shadcn/ui** (copied into the repo) on **Radix UI** primitives | Accessible dialogs, sheets, menus, tabs, tooltips, popovers |
| Motion | **Motion** (motion.dev) | Honors `prefers-reduced-motion` |
| Accent effects | Selected **Magic UI** components (MIT): animated beam, border beam, dot/grid pattern, shimmer button, bento grid, number ticker | Mostly on the marketing site (§8.6); in the app only on auth pages, empty states and overview tiles. Never inside data tables. |
| Code highlighting | **Shiki** (MIT), lazy-loaded | Marketing code tabs and the webhook payload viewer |
| Tables | TanStack Table v8 (shadcn data-table pattern) | Server-side pagination, column visibility, sticky header |
| Forms | React Hook Form + Zod | Schemas mirror the backend validation |
| Command palette | cmdk | ⌘K / Ctrl-K |
| Toasts | Sonner | Shows `requestId` on errors |
| Charts | Recharts (shadcn chart wrappers) | Overview sparklines and bars |
| Graph | React Flow (`@xyflow/react`) + dagre layout | Dependency Map |
| Diff viewer | Custom component rendering the backend's word-diff operations | Split and unified modes; monospace |
| Icons / dates | lucide-react; date-fns | Relative time with an exact UTC tooltip |
| Fonts | **Geist Sans / Geist Mono** (SIL OFL, self-hosted) | No external font CDN |
| Quality | ESLint (typescript-eslint), Prettier, `tsc --noEmit`; *P2:* Vitest + Testing Library, Playwright smoke test | Lint, typecheck and build gate CI |

### 8.3 Design tokens ("Midnight" theme, dark default)
These are starting values. The frontend owner may tune them, but every text pair must pass WCAG 2.1 AA contrast.

| Token | Value | Use |
|---|---|---|
| `--background` | `hsl(226 82% 6%)` | App background (deep navy, in Hatchet's family) |
| `--surface-1` / `--card` | `hsl(224 60% 9%)` | Cards, sidebar |
| `--surface-2` | `hsl(222 45% 12%)` | Hovered rows, inputs, popovers |
| `--border` | `hsl(217 33% 17%)` | 1 px hairlines; cards also get a `white/5%` top inner highlight |
| `--foreground` | `hsl(210 40% 98%)` | Primary text |
| `--muted-foreground` | `hsl(215 20% 65%)` | Secondary text |
| `--primary` (Ripple Cyan) | `hsl(188 86% 53%)` | Primary actions, focus ring, links |
| `--accent` (Ripple Violet) | `hsl(258 90% 66%)` | Gradient partner: `linear-gradient(135deg, cyan → violet)`, used sparingly (logo, hero, primary CTA border beam) |
| `--radius` | `0.5rem` (cards `0.75rem`) | |

**Semantic status colors.** Each has a tinted background (15% alpha), colored text and a dot.

| Status | Color |
|---|---|
| NEW | amber |
| INVESTIGATING | sky |
| RESOLVED | emerald |
| DISMISSED | zinc |
| Confidence UNCERTAIN | orange with a dashed outline |
| Change ADDED | emerald |
| Change REMOVED | rose |
| Change MODIFIED | amber |
| Webhook DELIVERED | emerald |
| Webhook RETRYING | amber, pulsing |
| Webhook FAILED | rose |
| Check running | cyan, pulsing |
| Source ERROR | rose |
| Source UNSUPPORTED | zinc |

**Typography.**
- Geist Sans: 14 px base, 13 px in dense tables, `tabular-nums` for numbers.
- Geist Mono: URLs, IDs, section keys, diffs and JSON.
- Headings are semibold with a tight tracking scale (`text-2xl / xl / lg / base`).

**Elevation and texture.**
- Borders and surface steps instead of heavy shadows.
- A glassy top bar (`backdrop-blur`, `background/70%`).
- A faint radial cyan-violet glow and dot grid only on the landing, auth and empty-state screens.

**Motion.**
- 150 ms ease-out for hover and press.
- 200–250 ms for sheets and dialogs.
- List rows enter with a 4 px slide and fade.
- Skeleton shimmer for loading.
- A pulse on running and retrying states.
- Number tickers on overview tiles.
- All of it is disabled under reduced motion.

**Light theme.** P2: the same tokens remapped. Dark is the only theme required for submission.

### 8.4 App shell and interaction patterns
- **Two shells, one SPA.** The **marketing shell** (top nav and footer) serves `/`, `/pricing`, `/privacy` and `/terms`. The **app shell** (below) serves `/app/**`. The auth pages use a split layout. Each shell is code-split, so marketing visitors never download the dashboard bundle.
- **Sidebar** (240 px, collapsible to 64 px):
  - Top: organization switcher.
  - Main navigation: Overview · Projects · Sources · Changes · Dependency Map · Impact Inbox (with an unread badge) · Integrations · Admin (ADMIN only).
  - Bottom: notifications bell, user menu (profile, logout) and a GitHub link.
- **Top bar.** Breadcrumbs, project switcher, ⌘K search field and a primary **New watch** button (MAINTAINER only).
- **Detail sheet.** Selecting a row in Changes, Inbox or the delivery log opens a right-hand sheet (`Sheet`, 560–720 px) without leaving the list. The URL updates (`?selected=<id>`), so links can be shared.
- **Command palette (⌘K).** Jump to a project, watch or change; run "New watch", "Open Impact Inbox" and "Check now on …"; switch organization.
- **Keyboard.**

  | Keys | Action |
  |---|---|
  | `g o` | Go to Overview |
  | `g p` | Go to Projects |
  | `g i` | Go to Impact Inbox |
  | `n` | New watch |
  | `j` / `k` | Move between Inbox rows |
  | `e` | Mark investigating |
  | `r` | Resolve |
  | `d` | Dismiss (asks for a note) |
  | `?` | Show the shortcut sheet |

- **Optimistic updates.** Only for reversible local actions (mark a notification read, impact status change with rollback on error). **Never** for check results, webhook delivery or monitoring claims.
- **State coverage.** Every data view designs these states: loading (skeleton), empty (illustration plus a call to action), error (message, `requestId`, Retry), forbidden (explains the required role), blocked (`SOURCE_FETCH_BLOCKED`, with the reason), uncertain match (dashed badge plus an explanation), and unsupported page (JS-rendered hint).
- **Role-aware UI.** Actions the user's role cannot perform are hidden or disabled with a tooltip ("Requires MAINTAINER"). The server is always the authority (§6.3).
- **Copy and IDs.** IDs, section keys, URLs, webhook secrets and API keys have copy buttons that show a toast. Secrets and keys are shown once, in a "reveal once" dialog with a confirmation checkbox.
- **Time.** Relative times ("3m ago") with an exact UTC tooltip. Server timestamps are always UTC.

### 8.5 Screens
| ID | Screen | Content and key interactions | Priority |
|---|---|---|---|
| S1 | Landing (`/`, public) | Full marketing page as specified in §8.6 | **P0** (rich motion P1) |
| S16 | Pricing (`/pricing`, public) | Plan cards with a monthly/yearly toggle, comparison table, FAQ, waitlist dialog for Cloud plans (§8.6) | **P0** |
| S17 | Privacy and Terms (public) | Plain-language draft legal pages (FR-WEB-10) | P0 |
| S18 | Admin → Waitlist | Search, CSV export, delete on request (FR-WEB-6) | P1 |
| S2 | Login / Register | Split layout (form + glow panel). Register asks for the role through two choice cards: "I maintain projects" (MAINTAINER) and "I review impacts" (REVIEWER) | P0 |
| S3 | Onboarding | First login with no organization: stepper Create organization → Create project → Add first watch. A REVIEWER with no organization sees "Ask a maintainer to add you (your email: …)" | P0 |
| S4 | Overview | Stat tiles (active watches, changes in 7 days, open impacts, failing sources) with sparklines; recent changes feed; impacts by status chart; check activity | P0 (basic), P1 (charts) |
| S5 | Projects list → Project detail | Cards or table with watch count, open impacts and last check. Detail tabs: Watches · Dependencies · Changes · Impacts · Webhooks · API Keys · Settings | P0 |
| S6 | New watch wizard | Step 1: URL with live preview (skeleton, then detected kind, title and warnings). Step 2: choose WHOLE_PAGE or tick sections (key, title, excerpt). Step 3: interval and notify, confirm. Result: inline baseline status | P0 |
| S7 | Watch / source detail | Status pill, schedule, next check, **Check now** (inline run progress, then outcome), monitor-run timeline, versions list, current sections | P0 |
| S8 | Change history → Change detail | Timeline of change events. Detail view: changed-section list on the left (ADDED, REMOVED and MODIFIED badges plus match state) and the diff viewer on the right (split or unified, word highlights); metadata (original URL, from/to version, detected at); **affected dependencies** panel | P0 |
| S9 | Dependency Map | React Flow graph of Source → Section → Dependency → Project. Nodes are colored by open-impact status and clicking one opens its sheet. Toggles to a table view | P0 (table), P1 (graph) |
| S10 | Impact Inbox | Filter chips (project, status, confidence, source, date; saved in Zustand), dense list, keyboard triage. Sheet: evidence diff excerpt, why the impact was flagged (dependency → section → change), review actions and a note, history | P0 |
| S11 | Integrations | Webhooks: endpoint list, create dialog (reveal secret once), delivery log with status and attempts, payload viewer, **Send test ping**. API keys: create (reveal once), scopes, last used, revoke. MCP: connection snippet with copy | P0 webhooks, P1 keys/MCP |
| S12 | Notifications | Bell popover with unread-first list; mark all read; deep links | P0 |
| S13 | Admin | Users (search, roles editor, enable/disable), monitor runs table, failing sources, stats tiles, demo controls (fixture v1/v2 switch, receiver events) | P0 |
| S14 | Settings | Profile; organization settings and members (owner adds and removes by email) | P0 |
| S15 | System pages | 404, 403 (names the role needed), 500 (shows `requestId`), offline/retry banner | P0 |

**Responsive behavior.**
- Desktop-first (≥ 1280 px shows everything).
- Tablet: the sidebar collapses.
- Mobile: tables become cards, the sheet becomes a bottom drawer, and the diff uses unified mode.

**Accessibility.**
- Radix primitives for focus management.
- A visible focus ring (`--primary`).
- `aria-live` for toasts.
- Every icon-only button has a label.
- Color is never the only signal: badges also carry text or an icon.

### 8.6 Marketing site: landing and pricing (DEC-15)
**Goal.** It should look and feel like a top-tier developer-infrastructure SaaS, in the class of hatchet.run, while staying **honest** (FR-WEB-2) and entirely our own brand.

**Visual language.** The Midnight tokens (§8.3), scaled up for marketing:
- **Hero headline:** Geist 56–64 px, semibold, tracking −0.03em, gradient text (white → cyan → violet) on the key phrase.
- **Section titles and body:** section titles 36–40 px; body 16–18 px in `--muted-foreground`.
- **Layout:** content up to 1200 px wide; sections spaced 96–128 px apart.
- **Navigation:** a sticky glass nav (`backdrop-blur`, hairline border).
- **Texture:** a faint dot grid, a radial cyan-violet glow behind the hero, and hairline-bordered cards with a top highlight.
- **Motion:** sections fade and slide in on scroll, 200–300 ms with Motion, once per section, respecting reduced motion.
- **Animated beams:** these draw the core idea. One *source* node sends ripples to several *project* nodes, and only the dependent ones light up. The visual itself explains the product.

**Landing page (`/`) sections, in order:**
1. **Nav:** logo (ripple mark), Product, How it works, Pricing, Docs (GitHub), live GitHub stars, Log in, and a primary **Start free** button.
2. **Hero:**
   - Eyebrow: "Open source · Agent-ready".
   - Headline, for example *"When the web changes, know which projects it touches."*
   - Subhead: watches the docs, policies and pages your products rely on, shows exactly what changed, and flags the projects that depend on it, in your dashboard, by signed webhook or to your AI agents over MCP. No paid AI required.
   - Buttons: **Start free**, **Try the live demo** (FR-WEB-7), plus a secondary "Star on GitHub".
   - Visual: a **real screenshot of our Impact Inbox** in a browser frame with a border beam, next to the animated-beam ripple diagram.
3. **Problem → answer:** "Generic monitors say a page changed. SourceRipple says which of your projects to check." A side-by-side "page changed" alert versus a SourceRipple impact card.
4. **How it works:** four steps, Monitor → Compare → Map → Deliver, each with a small looping illustration (URL pulse → section diff → dependency link → webhook/MCP).
5. **Feature bento grid:**
   - Section-level diffs
   - Dependency mapping across projects
   - Impact inbox and triage
   - Signed webhooks with retries
   - MCP for AI agents
   - Roles and teams
   - Safe-by-design fetching (SSRF-protected)
   - Self-hostable, no paid AI
6. **Developer section:** tabbed code panels highlighted with Shiki:
   - webhook payload;
   - signature verification in Node, Python and Java;
   - a REST `curl` example;
   - MCP client configuration.
7. **Open source:** Apache-2.0, `docker compose up` in one copyable command, links to GitHub and the contribution guide.
8. **Pricing teaser:** the three main plans in compact form, linking to `/pricing`.
9. **FAQ:** an accordion of 6–8 questions (self-host vs Cloud, what counts as a watch, how often checks run, JS-heavy pages, data and privacy, when Cloud launches).
10. **Final call to action** and a **footer** with columns Product, Developers, Company and Legal, plus the license and GitHub link.

**Pricing page (`/pricing`).** Prices are **illustrative** until validation (§24 Q9), are configured in `plans.ts` and are shown in USD.

| Plan | Price | For | Includes | Button (lab) |
|---|---|---|---|---|
| **Community** (self-hosted) | Free forever | Developers who run it themselves | Every core feature, Apache-2.0; limits set by your own infrastructure; community support on GitHub | "Self-host on GitHub" |
| **Cloud Hobby** | $0 | Trying it hosted | 1 organization, 3 projects, 10 watches, checks at least 24 h apart, 30-day history, dashboard and webhooks | "Start free" (public preview) |
| **Cloud Pro** | $19 / month (illustrative) | Small teams and agencies | 5 members, 100 watches, checks at least 1 h apart, 1-year history, API keys and MCP, email support | "Join waitlist" |
| **Cloud Business** | $99 / month (illustrative) | Growing teams | 25 members, 1,000 watches, checks at least 15 min apart; JS rendering, SSO and audit export marked *Coming soon*; priority support | "Join waitlist" |
| **Enterprise** | Custom | Large or regulated organizations | Support contract for self-hosting or a dedicated cloud, SLA, custom limits | "Contact us" (waitlist form with `plan=ENTERPRISE`) |

**Pricing page details:**
- A monthly/yearly toggle (yearly shows "2 months free").
- The **Recommended** tag on Pro is a choice we make, not a popularity claim.
- A full feature comparison table with sticky plan headers.
- A pricing FAQ.
- A note that SourceRipple Cloud is not yet available and the public instance is a free preview.
- The waitlist dialog (FR-WEB-5) has email, plan, optional company and message, a consent checkbox and the hidden honeypot.

**Quality bar.**
- Meets PG7: Lighthouse ≥ 90, LCP < 2.5 s.
- Self-hosted fonts and WebP/AVIF screenshots.
- Animations are lazy-loaded below the fold.
- No layout shift.
- Fully keyboard-accessible.
- Every marketing claim must be true of the shipped product on the day it is published.

---

## 9. Domain model and database

**Engine and access.**
- PostgreSQL 17 (16+ supported).
- Schema is owned by **Flyway** migrations under `backend/src/main/resources/db/migration`.
- Data access is **Spring Data JPA** (Hibernate) with `spring.jpa.hibernate.ddl-auto=validate`.
- Merged migrations are never edited.

**Conventions.**
- UUID primary keys.
- `timestamptz` timestamps in UTC.
- Enumerations stored as `varchar` with check constraints.
- JSON columns are `jsonb`, mapped with `@JdbcTypeCode(SqlTypes.JSON)`.

### 9.1 Tables (20 domain tables plus framework tables)
| # | Table | Key columns | Relationships |
|---|---|---|---|
| 1 | `users` | id, email (unique, lower-cased), password_hash, display_name, enabled, created_at, last_login_at | |
| 2 | `roles` | id, name (unique: ADMIN, MAINTAINER, REVIEWER) | Seeded by migration |
| 3 | `user_roles` | user_id, role_id (composite PK) | **M:M** users ↔ roles (`@ManyToMany`) |
| 4 | `organizations` | id, owner_user_id, name, created_at | M:1 → users (owner) |
| 5 | `organization_members` | organization_id, user_id, added_at (composite PK) | **M:M** organizations ↔ users (`@ManyToMany`) |
| 6 | `projects` | id, organization_id, name, slug, description, archived_at, created_at; unique (organization_id, slug) | M:1 → organizations; 1:M → watches |
| 7 | `sources` | id, organization_id, canonical_url, kind (HTML/RSS), status, etag, last_modified, check_interval_seconds, next_check_at, claimed_until, last_checked_at, last_success_at, consecutive_failures, last_error_code; unique (organization_id, canonical_url) | M:1 → organizations |
| 8 | `watches` | id, project_id, source_id, name, mode, section_keys (jsonb), interval_hours, notify_on_change, status, created_by, created_at; unique (project_id, source_id) | **Association entity** making projects ↔ sources M:M; M:1 → projects, sources, users |
| 9 | `source_versions` | id, source_id, fetched_at, title, content_sha256, normalized_text, byte_count, http_status | M:1 → sources; 1:M → source_sections |
| 10 | `source_sections` | id, version_id, position, stable_key, title, body_text, section_sha256 | M:1 → source_versions |
| 11 | `change_events` | id, source_id, from_version_id (nullable, ON DELETE SET NULL), to_version_id (nullable, ON DELETE SET NULL), fingerprint (unique), sections_changed, occurred_at | M:1 → sources |
| 12 | `change_event_sections` | id, change_event_id, section_key, title, change_type, match_state, old_text, new_text, diff_ops (jsonb) | M:1 → change_events |
| 13 | `dependencies` | id, watch_id, section_key (nullable = whole page), object_type, external_id, display_name, description, metadata (jsonb), active, created_at; unique (watch_id, coalesce(section_key,''), external_id) | M:1 → watches |
| 14 | `impacts` | id, change_event_id, dependency_id, project_id, match_confidence, review_status, reviewer_note, reviewed_by, reviewed_at, created_at; unique (change_event_id, dependency_id) | M:1 → change_events, dependencies, projects, users |
| 15 | `notifications` | id, recipient_user_id, project_id, kind, change_event_id, impact_id, message, created_at, read_at | M:1 → users, projects |
| 16 | `webhook_endpoints` | id, project_id, url, encrypted_secret, event_types (jsonb), enabled, consecutive_failures, created_at | M:1 → projects |
| 17 | `webhook_deliveries` (the **outbox**) | id, endpoint_id, change_event_id, event_id (unique), event_type, payload (jsonb), state, attempt_count, next_attempt_at, last_response_code, last_error, delivered_at, created_at; unique (endpoint_id, change_event_id, event_type) | M:1 → webhook_endpoints, change_events |
| 18 | `api_keys` | id, project_id, name, key_prefix, key_hash (unique), scopes, created_by, created_at, last_used_at, revoked_at | M:1 → projects, users |
| 19 | `monitor_runs` | id, source_id, trigger (BASELINE/SCHEDULED/MANUAL/TICK), started_at, finished_at, outcome, http_status, error_code, duration_ms, bytes | M:1 → sources |
| 20 | `waitlist_signups` | id, email (unique, normalized), plan (CLOUD_PRO/CLOUD_BUSINESS/ENTERPRISE), company, message, consent_at, source_page, created_at, updated_at | Standalone (public marketing) |
| – | `app_settings` | key, value (jsonb), used for the demo fixture version and similar | |
| – | `spring_session`, `spring_session_attributes` | Spring Session JDBC (schema added as a Flyway migration; `spring.session.jdbc.initialize-schema=never`) | |

**Relationship coverage (guideline: 1:M, M:1, M:M with JPA).**
- 1:M and M:1 throughout, for example organization → projects and impact → dependency.
- True `@ManyToMany`: users ↔ roles and organizations ↔ users.
- Association-entity M:M: projects ↔ sources through `watches`.

**Required indexes.**
- `sources(next_check_at) WHERE status='ACTIVE'`
- `source_versions(source_id, fetched_at DESC)`
- `source_sections(version_id, stable_key)`
- `change_events(source_id, occurred_at DESC)`
- `dependencies(watch_id, section_key)`
- `impacts(project_id, review_status, created_at DESC)`
- `webhook_deliveries(state, next_attempt_at)`
- `notifications(recipient_user_id, read_at)`
- `monitor_runs(source_id, started_at DESC)`

**Transactions.** One transaction covers: new version + sections → change event + section evidence → impacts → notifications → webhook outbox rows. Dispatch happens after commit.

### 9.2 ER diagram
```mermaid
erDiagram
  USERS }o--o{ ROLES : "user_roles"
  USERS }o--o{ ORGANIZATIONS : "organization_members"
  USERS ||--o{ ORGANIZATIONS : owns
  ORGANIZATIONS ||--o{ PROJECTS : has
  ORGANIZATIONS ||--o{ SOURCES : "deduplicated URLs"
  PROJECTS ||--o{ WATCHES : has
  SOURCES ||--o{ WATCHES : "watched by"
  SOURCES ||--o{ SOURCE_VERSIONS : versions
  SOURCE_VERSIONS ||--o{ SOURCE_SECTIONS : sections
  SOURCES ||--o{ CHANGE_EVENTS : changes
  CHANGE_EVENTS ||--o{ CHANGE_EVENT_SECTIONS : evidence
  WATCHES ||--o{ DEPENDENCIES : declares
  CHANGE_EVENTS ||--o{ IMPACTS : causes
  DEPENDENCIES ||--o{ IMPACTS : "flagged by"
  PROJECTS ||--o{ IMPACTS : scopes
  PROJECTS ||--o{ WEBHOOK_ENDPOINTS : has
  WEBHOOK_ENDPOINTS ||--o{ WEBHOOK_DELIVERIES : attempts
  CHANGE_EVENTS ||--o{ WEBHOOK_DELIVERIES : "delivered as"
  PROJECTS ||--o{ API_KEYS : has
  SOURCES ||--o{ MONITOR_RUNS : runs
  USERS ||--o{ NOTIFICATIONS : receives
```

### 9.3 State machines
- **Watch status.** `ACTIVE ⇄ PAUSED`, `ACTIVE → ERROR → ACTIVE` (after an edit or a successful check), `ACTIVE → UNSUPPORTED`. Admin: any state → `PAUSED_BY_ADMIN → ACTIVE`.
- **Monitor run outcome.** `BASELINE | UNCHANGED | NOT_MODIFIED | CHANGED | FAILED_RETRYABLE | FAILED_PERMANENT | BLOCKED`.
- **Impact review.** `NEW → INVESTIGATING → RESOLVED | DISMISSED`; `NEW → RESOLVED | DISMISSED`; `RESOLVED | DISMISSED → INVESTIGATING`.
- **Webhook delivery.** `QUEUED → DELIVERING → DELIVERED`. Failures go `DELIVERING → RETRYING → DELIVERING …` until attempt 6, then `FAILED`. A non-retryable 4xx goes `DELIVERING → FAILED` directly.

---

## 10. REST API

### 10.1 Conventions
- Base path `/api/v1`, JSON, camelCase, UUID string IDs, ISO-8601 UTC timestamps.
- Pagination is `?page=0&size=20` (maximum 100) and returns `{items, page, size, totalItems, totalPages}`.
- Creates return **201** with a `Location` header. Deletes return **204**. Validation errors return **400**. Missing authentication returns **401**. An authenticated user without the role gets **403**. Missing or not-visible resources (including other tenants') get **404**. Uniqueness conflicts and invalid state transitions get **409**. Semantic failures, such as a blocked URL or unknown section, get **422**. Rate limits return **429** with `Retry-After`.

### 10.2 Error envelope
```json
{ "error": "SOURCE_FETCH_BLOCKED", "message": "This URL resolves to a disallowed destination.",
  "requestId": "req_80b2…", "retryable": false, "fieldErrors": [] }
```

**Error codes (initial set):**
`VALIDATION_FAILED`, `UNAUTHENTICATED`, `ACCOUNT_DISABLED`, `FORBIDDEN`, `NOT_FOUND`, `EMAIL_TAKEN`, `ROLE_NOT_ALLOWED`, `DUPLICATE_WATCH`, `QUOTA_EXCEEDED`, `INTERVAL_TOO_SHORT`, `SOURCE_FETCH_BLOCKED`, `SOURCE_FETCH_FAILED`, `ROBOTS_DISALLOWED`, `UNSUPPORTED_CONTENT`, `SECTION_NOT_FOUND`, `INVALID_TRANSITION`, `ORG_NOT_EMPTY`, `CHECK_RATE_LIMITED`, `RATE_LIMITED`, `TICK_IN_PROGRESS`, `INTERNAL_ERROR`.

### 10.3 Endpoint catalog (15 controllers)
CRUD-complete entities: **Organization, Project, Watch, Dependency, WebhookEndpoint**. The guideline requires 3; we provide 5.

| Controller | Method and path | Who | Success |
|---|---|---|---|
| **AuthController** | `POST /auth/register` | public | 201 |
| | `POST /auth/login` | public | 200 |
| | `POST /auth/logout` | authenticated | 204 |
| | `GET /auth/me` | authenticated | 200 |
| | `POST /auth/demo-login` (only when the demo is enabled) | public, rate-limited | 200 |
| **OrganizationController** | `POST /organizations` | MAINTAINER | 201 |
| | `GET /organizations` | member | 200 |
| | `GET /organizations/{orgId}` | member | 200 |
| | `PATCH /organizations/{orgId}` | owner | 200 |
| | `DELETE /organizations/{orgId}` | owner | 204 / 409 |
| **MemberController** | `GET /organizations/{orgId}/members` | member | 200 |
| | `POST /organizations/{orgId}/members` | owner | 201 |
| | `DELETE /organizations/{orgId}/members/{userId}` | owner | 204 |
| **ProjectController** | `POST /organizations/{orgId}/projects` | MAINTAINER | 201 |
| | `GET /organizations/{orgId}/projects` | member | 200 |
| | `GET /projects/{projectId}` | member | 200 |
| | `PATCH /projects/{projectId}` | MAINTAINER | 200 |
| | `DELETE /projects/{projectId}` (archive) | MAINTAINER | 204 |
| | `GET /projects/{projectId}/summary` | member | 200 |
| **WatchController** | `POST /projects/{projectId}/watches/preview` | MAINTAINER | 200 |
| | `POST /projects/{projectId}/watches` | MAINTAINER | 201 |
| | `GET /projects/{projectId}/watches` | member | 200 |
| | `GET /watches/{watchId}` | member | 200 |
| | `PATCH /watches/{watchId}` | MAINTAINER | 200 |
| | `DELETE /watches/{watchId}` | MAINTAINER | 204 |
| | `POST /watches/{watchId}/check` | MAINTAINER | 200 |
| | `GET /watches/{watchId}/sections` | member | 200 |
| **ChangeController** | `GET /watches/{watchId}/versions` | member / READ key | 200 |
| | `GET /watches/{watchId}/runs` | member | 200 |
| | `GET /projects/{projectId}/changes` | member / READ key | 200 |
| | `GET /changes/{changeId}` (evidence) | member / READ key | 200 |
| **DependencyController** | `POST /projects/{projectId}/dependencies` (body includes `watchId`) | MAINTAINER | 201 |
| | `GET /projects/{projectId}/dependencies` | member | 200 |
| | `GET /dependencies/{id}` | member | 200 |
| | `PUT /dependencies/{id}` | MAINTAINER | 200 |
| | `DELETE /dependencies/{id}` | MAINTAINER | 204 |
| **ImpactController** | `GET /projects/{projectId}/impacts` | member / READ key | 200 |
| | `GET /impacts/{impactId}` | member | 200 |
| | `PATCH /impacts/{impactId}` (status + note) | MAINTAINER, REVIEWER | 200 / 409 |
| **NotificationController** | `GET /notifications` | authenticated | 200 |
| | `POST /notifications/{id}/read` | authenticated | 204 |
| | `POST /notifications/read-all` | authenticated | 204 |
| **WebhookController** | `POST /projects/{projectId}/webhooks` | MAINTAINER | 201 (secret shown once) |
| | `GET /projects/{projectId}/webhooks` | member | 200 |
| | `GET /webhooks/{id}` | member | 200 |
| | `PATCH /webhooks/{id}` | MAINTAINER | 200 |
| | `DELETE /webhooks/{id}` | MAINTAINER | 204 |
| | `POST /webhooks/{id}/test` | MAINTAINER | 200 |
| | `GET /webhooks/{id}/deliveries` | member | 200 |
| **ApiKeyController** (P1) | `POST /projects/{projectId}/api-keys` | MAINTAINER | 201 (key shown once) |
| | `GET /projects/{projectId}/api-keys` | MAINTAINER | 200 |
| | `DELETE /api-keys/{id}` (revoke) | MAINTAINER | 204 |
| **AdminController** | `GET /admin/users` | ADMIN | 200 |
| | `PATCH /admin/users/{id}` (enabled) | ADMIN | 200 |
| | `PUT /admin/users/{id}/roles` | ADMIN | 200 |
| | `GET /admin/monitor-runs` | ADMIN | 200 |
| | `GET /admin/sources?status=` | ADMIN | 200 |
| | `POST /admin/watches/{id}/pause`, `/resume` | ADMIN | 200 |
| | `GET /admin/stats` | ADMIN | 200 |
| | `PUT /admin/demo/fixture` | ADMIN | 200 |
| | `GET /admin/demo/receiver/events` | ADMIN | 200 |
| | `POST /admin/demo/reset` (P1) | ADMIN | 200 |
| | `GET /admin/waitlist`, `GET /admin/waitlist/export.csv` | ADMIN | 200 |
| | `DELETE /admin/waitlist/{id}` | ADMIN | 204 |
| **PublicController** | `POST /public/waitlist` | public, rate-limited, honeypot | 202 (always) |
| **SchedulerController** | `POST /internal/scheduler/tick` (not under `/api/v1`) | tick token | 200 / 401 / 409 |
| **DemoController** | `GET /demo/vendor/{page}`, `POST /demo/receiver` (only when the demo is enabled) | public | 200 |

**Outside the REST API:**
- `/actuator/health` and `/actuator/info` are public.
- `/actuator/prometheus` requires the metrics credential.
- `/v3/api-docs` and `/swagger-ui.html` are public.
- `/mcp` requires an API key (P1).
- The SPA fallback forwards every non-API, non-file path to `index.html`.

### 10.4 Example: create a watch
```http
POST /api/v1/projects/7c1e…/watches
Cookie: SESSION=…            X-XSRF-TOKEN: …
{ "url": "https://docs.vendor.example/payments", "mode": "SECTIONS",
  "sectionKeys": ["payments/parameters"], "intervalHours": 24, "notifyOnChange": true }

201 Created   Location: /api/v1/watches/9a2f…
{ "watchId": "9a2f…", "sourceId": "41d0…", "status": "ACTIVE",
  "baselineStatus": "READY", "nextCheckAt": "2026-11-18T09:12:00Z" }
```

### 10.5 Example: webhook `impact.detected`
```http
POST https://customer.example/hooks/sourceripple
X-SR-Event-Id: evt_01JD…   X-SR-Event-Type: impact.detected
X-SR-Timestamp: 1794900000  X-SR-Signature: v1=5f2c…
{ "eventId": "evt_01JD…", "type": "impact.detected", "projectId": "7c1e…", "sourceId": "41d0…",
  "changeId": "c55b…", "occurredAt": "2026-11-18T09:12:04Z",
  "impacts": [ { "impactId": "…", "externalId": "payment-service", "sectionKey": "payments/parameters",
                 "confidence": "MATCHED", "reviewStatus": "NEW" } ],
  "evidenceUrl": "https://<host>/api/v1/changes/c55b…" }
```

---

## 11. Architecture and technology

### 11.1 Shape (DEC-03, DEC-04)
**One Spring Boot modular monolith, deployed as one container.** It serves:
- the REST API;
- the built React SPA (as static resources), which includes the public marketing site;
- the MCP endpoint;
- the demo kit;
- the monitoring engine (scheduler loop and tick).

PostgreSQL is the only stateful dependency. There are no microservices, no message broker and no Redis.

```mermaid
flowchart LR
  subgraph Clients
    B[Browser SPA]
    R[Customer webhook receiver]
    A[AI agent / MCP client]
    T[Tick: GitHub Actions cron / Cloud Scheduler]
  end
  subgraph App[SourceRipple container - Spring Boot]
    SEC[Spring Security: session + CSRF / API key / tick token]
    API[REST controllers + DTOs]
    MCP[MCP tools]
    SVC[Application services]
    MON[Monitor engine: claim → fetch → extract → diff → impact]
    FETCH[SafeFetchProxy → OkHttp + DNS guard]
    OUT[Outbox dispatcher: webhooks]
    SPA[Static SPA]
  end
  DB[(PostgreSQL: Neon / compose)]
  WEB[(Public web pages)]
  B --> SEC --> API --> SVC
  A --> SEC --> MCP --> SVC
  T --> SEC --> MON
  SVC --> MON --> FETCH --> WEB
  SVC --> DB
  MON --> DB
  OUT --> R
  SVC --> OUT
  B --> SPA
  PROM[Prometheus] -->|/actuator/prometheus| App
  GRAF[Grafana] --> PROM
```

### 11.2 Backend packages (`io.sourceripple.*`)
| Package | Responsibility |
|---|---|
| `identity` | Users, roles, registration and login, sessions, bootstrap admin, `AccessPolicy` |
| `workspace` | Organizations, members, projects |
| `source` | URL canonicalization, sources, watches, safe fetching (`WebFetcher`, `SafeFetchProxy`, `OkHttpWebFetcher`, DNS guard), robots |
| `content` | `SourceAdapter`s, `SourceAdapterFactory`, `ExtractionRuleSet` (Flyweight), normalization |
| `diff` | Section matching, word diff, change events and evidence |
| `monitor` | Claiming, `MonitorService`, scheduler loop, tick, monitor runs, backoff, retention |
| `impact` | Dependencies, impact engine, review workflow |
| `notify` | `ChangeNotification` abstractions and `NotificationChannel`s (InApp, Webhook) (Bridge); notifications; webhook endpoints; outbox dispatcher; signer |
| `integration` | API keys, MCP tools |
| `admin` | Admin services and controller |
| `demo` | Fixture pages, demo receiver, seeder, demo login and reset (`@ConditionalOnProperty`) |
| `marketing` | Waitlist sign-ups (public endpoint, admin list and export) |
| `shared` | Error envelope, global exception handler, request-ID filter, config properties, `GlobalFetchLimits` (Singleton), time and ID utilities |

**Layering rules.**
- Controllers hold no business logic: they validate, call one service and map to a DTO.
- Services own transactions and authorization.
- Repositories are used only inside their own module. Other modules go through services.
- Entities never leave the service layer; DTOs cross every boundary.

### 11.3 Execution model (DEC-08)
1. **No business work runs after a response is sent.** Work runs inside a request (manual check, preview, create, tick) or on the internal scheduler thread.
2. **Triggers per environment:**

   | Environment | `SOURCERIPPLE_SCHEDULER_MODE` | How checks run |
   |---|---|---|
   | Local / compose | `internal` | Loop every 60 s |
   | Render (free, sleeps when idle) | `both` | Loop while awake, plus a GitHub Actions cron tick every 3 h that also wakes the service |
   | Cloud Run (request-based billing) | `external` | Cloud Scheduler tick every hour; no background loop, because there is no CPU between requests |

3. **Catch-up.** Overdue sources (`next_check_at <= now`) are processed on the next loop or tick, so a sleeping instance catches up when it wakes.
4. **Concurrency.** Claims use `FOR UPDATE SKIP LOCKED` with a lease, so the loop, a tick and a manual check never process the same source at the same time.

### 11.4 Backend technology (DEC-05)
| Concern | Choice |
|---|---|
| Language / build | **Java 21 LTS**, **Maven** (wrapper committed) |
| Framework | **Spring Boot**: the latest stable line supported by the Spring AI MCP server starter, decided and pinned in Phase 1. Spring Web MVC, Validation, Security, Data JPA, Actuator |
| Database | PostgreSQL JDBC, **Flyway**, Hibernate (`ddl-auto=validate`), HikariCP (max pool 5) |
| Sessions | **Spring Session JDBC** |
| HTTP out | **OkHttp** for fetching and webhooks (custom `Dns` for SSRF pinning, network interceptor, byte limits); `MockWebServer` in tests |
| Parsing | **Jsoup** (HTML and RSS via `Parser.xmlParser()`), **crawler-commons** (robots.txt), **java-diff-utils** (word diff) |
| API docs | springdoc-openapi (version matched to Boot) |
| Metrics / logs | Micrometer + Prometheus registry; Spring Boot structured JSON logging; request-ID MDC |
| MCP (P1) | Spring AI MCP server starter (WebMVC, Streamable HTTP) behind Spring Security |
| Tests | JUnit 5, Mockito, AssertJ, Spring Boot Test, **MockMvc**, **Testcontainers** (PostgreSQL), MockWebServer, JaCoCo |

### 11.5 Design patterns (rubric: "appropriate use", 10 marks)
Each pattern needs a class diagram, a call path and a focused test (§14). All six patterns taught in the course are implemented where they solve a real problem.

| Pattern | Where | Why it is genuine | Test |
|---|---|---|---|
| **Adapter** | `SourceAdapter` ← `HtmlPageAdapter`, `RssFeedAdapter` | Two incompatible formats become one `NormalizedDocument` contract. **The RSS adapter stays in P0**, otherwise the pattern is hollow. | TST-U15, U16 |
| **Factory** | `SourceAdapterFactory` | Picks the adapter from content type and body sniffing; callers never branch on format | TST-U14 |
| **Proxy** | `SafeFetchProxy implements WebFetcher`, wrapping `OkHttpWebFetcher` | Enforces scheme, port, IP and robots policy, size and rate limits *before* delegating | TST-U12, U13 |
| **Bridge** | `ChangeNotification` (`ImpactNotification`, `SourceChangeNotification`) × `NotificationChannel` (`InAppChannel`, `WebhookChannel`) | Message content and delivery mechanism vary independently: 2 × 2 combinations with no subclass explosion | TST-U27 |
| **Flyweight** | `ExtractionRuleSetPool` sharing immutable `ExtractionRuleSet`s by normalized signature | Many watches share identical extraction rules (default HTML or RSS rules plus per-mode variants) | TST-U28 |
| **Singleton** | `GlobalFetchLimits`: one immutable JVM-wide policy (byte caps, timeouts, concurrency) built once from configuration | One authoritative policy object read by the proxy, the fetcher and the scheduler. The report explains how this relates to Spring's singleton beans. | covered by U12/U13 |

Supporting architectural patterns (not counted, but documented): Repository, DTO, Transactional Outbox, Strategy-like backoff policy, and a layered modular monolith.

---

## 12. Security requirements

### NFR-SEC-1 Safe fetching (SSRF), applied to every source fetch, robots.txt fetch and webhook delivery
1. **URL validation.**
   - Allow only `http` and `https`; production webhooks must be `https`.
   - Reject userinfo, fragments-only URLs, and ports other than 80, 443, 8080 and 8443 (configurable).
   - Reject hosts that are an IP literal in a forbidden range.
   - Canonicalize: lower-case the host, strip default ports, remove the fragment, keep the query.
2. **Forbidden destinations (IPv4 and IPv6).** Loopback, RFC 1918 private ranges, link-local (including `169.254.169.254` metadata), CGNAT `100.64/10`, multicast, broadcast, unspecified, ULA `fc00::/7`, `::ffff:` mapped private addresses, and documentation ranges.
3. **DNS pinning.** A custom OkHttp `Dns` resolves the host, filters out forbidden addresses (failing if none remain) and returns only validated addresses, so the socket connects to exactly what was checked.
   - Java's `java.net.http.HttpClient` has no resolver hook, which is why we use OkHttp (DEC-06).
   - OkHttp skips the `Dns` hook for IP-literal hosts, so step 1 must reject private IP literals.
   - A network interceptor re-checks the connected socket's remote address before any request bytes are sent (defense in depth).
4. **Redirects.** Followed manually, at most 3 hops, and every hop is re-validated.
5. **Limits** (from `GlobalFetchLimits`):
   - Connect timeout 5 s, read timeout 10 s, total timeout 15 s.
   - At most 2 MB compressed and 5 MB decoded, enforced while streaming.
   - Content types allowed: `text/html`, `application/xhtml+xml`, `application/rss+xml`, `application/atom+xml`, `application/xml`, `text/xml`.
   - No decompression bombs.
6. **Etiquette.** Identify as `SourceRippleBot`, honor robots.txt, and keep the per-host pacing (FR-MON-10). Never log in, submit forms or bypass CAPTCHAs.
7. **`allow-private-targets` flag.** `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS` defaults to false and is true only in the `local` and `test` profiles. **In the `prod` profile, the application refuses to start if it is true.**

### NFR-SEC-2 Authentication and sessions
- BCrypt (strength 10–12).
- Sessions stored server-side in PostgreSQL.
- Cookie: HttpOnly, `Secure` in production, `SameSite=Lax`.
- The session ID is rotated on login. With Spring Security 6+, the custom JSON login endpoint must save the `SecurityContext` explicitly through the `SecurityContextRepository`.
- CSRF uses the SPA cookie-to-header pattern.

### NFR-SEC-3 Authorization
Three-layer enforcement (§6.3). Every endpoint has a role or membership test (§14). Non-members receive 404, never a different error that would reveal the resource exists.

### NFR-SEC-4 Secrets
- No credentials in code, Compose files, test fixtures or CI logs.
- All secrets come from environment variables or platform secret stores (§16.3).
- Webhook secrets are encrypted with AES-256-GCM using `SOURCERIPPLE_ENCRYPTION_KEY` (32 bytes, base64).
- API keys and passwords are hashed.
- Secrets and keys are shown once.
- Logs mask `Authorization`, `Cookie`, `X-SR-Signature`, `X-SR-Tick-Token` and the `password` and `secret` fields.

### NFR-SEC-5 Webhook integrity
- HMAC-SHA256 signature over `timestamp.rawBody`.
- Timestamps in seconds; receivers reject anything older than 5 minutes.
- Stable event IDs for deduplication.
- The demo receiver implements this verification as the reference.

### NFR-SEC-6 Untrusted content
- Page text is data, never instructions.
- It is rendered as text only in React: no `dangerouslySetInnerHTML` for fetched content.
- MCP outputs label it `untrusted_web_content`.
- Scripts from fetched pages are never stored for rendering.

### NFR-SEC-7 Abuse controls on a public instance (open registration)
- Quotas (FR-ORG-3, FR-WCH-3).
- Minimum interval of 6 h in production.
- Rate limits (Bucket4j or an in-memory token bucket; one instance per deployment): login 10 attempts per 15 min per IP and per email; preview, manual check, waitlist and demo login as set in FR-WCH-1, FR-WCH-5, FR-WEB-5 and FR-WEB-7. Exceeding a limit returns 429 `RATE_LIMITED` (`CHECK_RATE_LIMITED` for manual checks).
- Admins can pause watches and disable users.
- Per-host pacing.
- A global cap of 200 active sources per instance (configurable).
- Public marketing endpoints: the waitlist is limited to 5 per hour per IP with a honeypot field, and demo login to 10 per hour per IP. Request bodies are capped at 4 KB.

### NFR-SEC-8 HTTP hardening
- Security headers: CSP `default-src 'self'`; `script-src 'self' 'wasm-unsafe-eval'` (Shiki); `style-src 'self' 'unsafe-inline'` (Radix, Sonner and Motion set inline styles); `img-src 'self' data:`; `connect-src 'self' https://api.github.com` (FR-WEB-8); `frame-ancestors 'none'`. Also `X-Content-Type-Options` and `Referrer-Policy`. No inline scripts.
- No CORS needed, because the SPA is same-origin. CORS stays disabled.

### NFR-SEC-9 Supply chain (free for public repositories)
- GitHub secret scanning with push protection.
- Dependabot (Maven, npm and GitHub Actions).
- The dependency-review action on pull requests.
- Third-party UI code sources listed in `THIRD_PARTY_NOTICES.md`.

### NFR-SEC-10 Privacy
- No analytics or telemetry sent to vendors (no PostHog or Sentry by default). The marketing site sets no tracking cookies.
- Waitlist data is the email, plan, optional company and message, and the consent timestamp. It is used only for launch contact and deleted on request (FR-WEB-6).
- Only the necessary page text is kept, under the retention policy (FR-EXT-8).
- No third-party passwords are stored.

---

## 13. Other non-functional requirements

| ID | Requirement |
|---|---|
| NFR-PERF-1 | Non-fetch API endpoints answer in p95 < 300 ms at lab scale (≤ 100 sources, ≤ 10k impacts) on the free tiers, excluding cold starts. |
| NFR-PERF-2 | List endpoints are paginated and backed by the indexes in §9.1. |
| NFR-PERF-3 | Marketing routes meet PG7 (Lighthouse ≥ 90 on all four categories, LCP < 2.5 s). The marketing bundle excludes dashboard code (route-level splitting). The initial JS for `/` is ≤ 200 KB gzip. |
| NFR-REL-1 | Change events are idempotent (unique fingerprint). Webhooks are at-least-once with stable event IDs. Claims use leases and are crash-safe. |
| NFR-REL-2 | Health is `UP` only after Flyway has migrated and the database is reachable. Liveness and readiness groups are enabled. |
| NFR-RES-1 | Fits a 512 MB container: `JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"`, Tomcat max threads 20, Hikari pool 5, fetch pool 2. |
| NFR-RES-2 | Startup stays light (no eager caches; MCP and demo beans are conditional). Target < 60 s on Render's free instance. |
| NFR-OBS-1 | Structured logs with `requestId`, user ID (when known) and source ID on monitor logs. No page content in logs. |
| NFR-A11Y-1 | WCAG 2.1 AA on primary flows (S2, S6, S8, S10): keyboard-only operation, contrast, labels, focus. |
| NFR-COMP-1 | Latest Chrome, Edge, Firefox and Safari; desktop-first, usable on mobile (§8.5). |
| NFR-MAINT-1 | Flyway migrations are immutable after merge. Java follows the standard naming conventions. Frontend passes ESLint and Prettier. No dead code or commented-out blocks. Comments explain *why*. |
| NFR-COST-1 | $0 recurring cost: only the free tiers in §17. No paid AI, scraping, search or vector services. |

**Configuration defaults.** All are environment-overridable through `@ConfigurationProperties` (`sourceripple.*`).

| Setting | Default |
|---|---|
| Interval, default / minimum | 24 h / 6 h in production (1 h locally) |
| Versions retained per source | 10 |
| Watches per project / per organization | 20 / 100 |
| Organizations per user | 5 |
| Fetch timeouts (connect / read / total) | 5 / 10 / 15 s |
| Fetch size cap (compressed / decoded) | 2 MB / 5 MB |
| Redirects | 3 |
| Fetch concurrency | 2 |
| Per-host gap | 10 s |
| Preview rate limit | 10 per minute per user |
| Manual check rate limit | 1 per minute per source |
| Webhook timeout | 5 s |
| Webhook attempts | 6 |
| Tick budget | 60 s |
| Claim batch | 10 |
| Lease | 5 min |

---

## 14. Testing requirements

**Rules.**
- **TST-R1.** At least **20 unit tests** at the service layer: JUnit 5 and Mockito, no Spring context, named `*Test`, run by Surefire. **Target 31.**
- **TST-R2.** At least **10 integration tests** at the controller layer: `@SpringBootTest` + `@AutoConfigureMockMvc` + **MockMvc**, real PostgreSQL through **Testcontainers** (`@ServiceConnection`), named `*IT`, run by Failsafe. **Target 17.**
- **TST-R3.** `./mvnw verify` runs both suites in CI on every pull request and every push to `develop` and `main`. A red build blocks the merge (§15.2).
- **TST-R4.** External HTTP is never touched. Source pages and webhook receivers use `MockWebServer` or the demo fixture controller. Tests run with `allow-private-targets=true` (test profile only).
- **TST-R5.** Use `@MockitoBean` (not the deprecated `@MockBean`) where a Spring test needs a mock.
- **TST-R6.** JaCoCo reports are uploaded in CI. Soft target: ≥ 70% line coverage for the `monitor`, `diff`, `impact`, `notify` and `identity` services.
- **TST-R7.** Keep evidence of one intentionally failing CI run (a broken impact assertion on a pull request) followed by the fixing commit, for the demo.

### 14.1 Planned unit tests (service layer)
| ID | Test |
|---|---|
| U1 | `AuthService` register hashes the password with BCrypt and assigns the chosen role |
| U2 | `AuthService` rejects `accountType=ADMIN` |
| U3 | `AuthService` rejects a duplicate email (case-insensitive) |
| U4 | `OrganizationService` create makes the creator owner and member |
| U5 | `MembershipService` only the owner can add or remove; the owner cannot be removed |
| U6 | `ProjectService` generates a unique slug within the organization |
| U7 | `ProjectService` archiving pauses the project's watches |
| U8 | `AccessPolicy` a non-member gets NotFound; a member without the role gets Forbidden |
| U9 | `WatchService` enforces project and organization quotas |
| U10 | `WatchService` rejects an interval below the minimum and a duplicate (project, source) |
| U11 | `UrlCanonicalizer` normalizes and rejects bad schemes, userinfo and private IP literals |
| U12 | `SafeFetchProxy` blocks loopback, private and metadata targets **without calling the delegate** (Proxy) |
| U13 | `SafeFetchProxy` / fetcher enforces the byte cap and the content-type allow-list |
| U14 | `SourceAdapterFactory` picks the HTML or RSS adapter from content type and sniffing (Factory) |
| U15 | `HtmlPageAdapter` produces stable sections and ignores nav and footer changes (Adapter) |
| U16 | `RssFeedAdapter` maps items to keyed sections (Adapter) |
| U17 | `ChangeDetectionService` an unchanged hash creates no version and no event |
| U18 | `ChangeDetectionService` the first success is a baseline with no event |
| U19 | `SectionDiffService` detects only the modified *Parameters* section |
| U20 | `SectionDiffService` a renamed heading yields `UNCERTAIN`, not a confident match |
| U21 | `ImpactService` flags only dependencies on the changed section, across two projects |
| U22 | `ImpactService` a whole-page dependency is flagged by any change |
| U23 | `ImpactService` an uncertain or missing section gives confidence `UNCERTAIN` |
| U24 | `ImpactReviewService` accepts valid transitions, rejects invalid ones, and requires a note to dismiss |
| U25 | `WebhookSigner` produces a deterministic `v1=` signature for a known vector |
| U26 | `DeliveryService` classifies 2xx, 4xx, 429 and 5xx correctly and schedules the right backoff |
| U27 | `NotificationDispatcher` sends both notification types to both channels (Bridge) |
| U28 | `ExtractionRuleSetPool` returns the same instance for equal rule signatures (Flyweight) |
| U29 | `ApiKeyService` stores only a hash, verifies a key, and rejects a revoked key |
| U30 | `MonitorScheduler` claims only due, unleased sources and applies backoff and jitter |
| U31 | `WaitlistService` normalizes and upserts emails, requires consent, and drops honeypot submissions |

### 14.2 Planned integration tests (MockMvc + Testcontainers)
| ID | Test |
|---|---|
| I1 | Register → login → `/auth/me` → logout → `/auth/me` returns 401 (session invalidated) |
| I2 | Bad credentials → 401; invalid register body → 400 with `fieldErrors` |
| I3 | Unauthenticated access to a protected endpoint → 401 |
| I4 | REVIEWER `POST /watches` → 403; MAINTAINER → 201 |
| I5 | Non-admin `/admin/users` → 403; ADMIN → 200 |
| I6 | A user from another organization requesting a project, watch or impact → 404 (no leak) |
| I7 | Project CRUD full cycle with correct codes (201 / 200 / 204 / 404) |
| I8 | Create a watch against a MockWebServer page → 201 with `baselineStatus=READY` |
| I9 | **Golden flow:** switch the fixture to v2 → manual check → 1 change event, 2 impacts, 0 for Fitness (Appendix A) |
| I10 | Dependency CRUD; an unknown section key → 422 |
| I11 | REVIEWER can resolve an impact; an invalid transition → 409 |
| I12 | Webhook receiver returns 500 then 200 → `RETRYING` then `DELIVERED`, with the same event ID and a valid signature |
| I13 | A watch URL resolving to a private IP (prod policy) → 422 `SOURCE_FETCH_BLOCKED`, and no request reaches the target |
| I14 | Project A's API key reading project B → 404; a revoked key → 401 |
| I15 | Error envelope shape (error, message, requestId, retryable) for 400, 404 and 409 |
| I16 | Tick without a token or with a wrong token → 401; with a valid token, due sources are processed |
| I17 | `POST /public/waitlist` works unauthenticated → 202 and the same body for new and duplicate emails; missing consent → 400; non-admin `/admin/waitlist` → 403 |

The frontend gate is lint, typecheck and build. Vitest component tests and a Playwright smoke test are P2.

---

## 15. Engineering workflow

### 15.1 Repository
**Repository.** A public GitHub repository under the Apache-2.0 license (DEC-01). It is public so branch protection, Actions minutes, secret scanning and dependency review are free (§17.1).

```
sourceripple/
├─ backend/                 # Spring Boot (Maven): src/main/java/io/sourceripple/<modules>, src/test/java
│  └─ src/main/resources/db/migration/   # Flyway V1__…sql
├─ frontend/                # Vite + React + TS (pnpm)
├─ infra/
│  ├─ prometheus/prometheus.yml
│  ├─ grafana/provisioning/{datasources,dashboards}/  +  grafana/dashboards/sourceripple.json
│  └─ cloudrun/             # notes / service config for Cloud Run
├─ docs/                    # idea/ (submitted), product/ (this PRD), development/ (generated from PRD), diagrams/
├─ .github/
│  ├─ workflows/ci.yml  workflows/scheduler-tick.yml
│  ├─ pull_request_template.md  ISSUE_TEMPLATE/  CODEOWNERS  dependabot.yml
├─ Dockerfile               # root, multi-stage (frontend + backend → one image)
├─ docker-compose.yml       # root: db, app, prometheus, grafana
├─ .env.example             # every variable, placeholder values only
├─ README.md  LICENSE  CONTRIBUTING.md  THIRD_PARTY_NOTICES.md
└─ .gitignore  .dockerignore  .editorconfig
```

### 15.2 Branching and protection (automatic-fail guard: no direct push to `main`)
- **Branches.**
  - `main`: production. Every merge deploys.
  - `develop`: integration.
  - Work branches: `feature/p<N>-<slug>` (N = phase number), `fix/…`, `chore/…`, `docs/…`, and `hotfix/…` (cut from `main` and merged back into both).
- **Flow.** `feature/*` → PR into `develop` → release PR `develop` → `main`.
  - Feature PRs are **squash-merged**.
  - Release PRs use a **merge commit**.
- **Protection rules for `main` and `develop`:**
  - pull request required;
  - **at least 1 approving review** (the author can't approve their own PR);
  - stale approvals dismissed on new commits;
  - status checks `backend`, `frontend` and `docker` must pass (on `main`, the branch must also be up to date);
  - conversations must be resolved;
  - force-pushes and deletion blocked;
  - **"Do not allow bypassing the above settings"** on, so admins are included.
- **Bootstrap (one-time exception, done by the Lead):**
  1. Push the complete Phase 1 scaffold directly to `main` as one commit, `chore: initial project scaffold`. No protection exists yet.
  2. Create `develop` from `main`.
  3. Immediately turn on both protection rules above.
  4. From then on, **every** change by every member, the Lead included, goes through a PR approved by at least one other member.

  Nobody pushes to `main` or `develop` again. The `pre-push` hook in `scripts/hooks/` blocks it locally, and `--no-verify` is forbidden.
- **Pull requests and commits.**
  - Every PR has a description, a linked issue, test evidence (CI link or screenshots) and a reviewer.
  - Commits and PR titles follow Conventional Commits, `type(scope): subject`: imperative, lowercase, ≤ 72 characters, no trailing period.
  - Commit messages and PR text never carry tool or assistant attribution trailers.
  - Full rules: `CONTRIBUTING.md`.
- **Repository settings.** Linear history on `develop`, head branches deleted automatically after merge, squash-merge for feature PRs, merge commits for release PRs.
- **Issue tracking.** Use a GitHub Projects board: Backlog → In progress → Review → Done, with labels `P0`, `P1`, `P2`, `backend`, `frontend`, `devops` and `bonus`.

### 15.3 CI/CD (GitHub Actions)
**`ci.yml` triggers:** `pull_request` (into `develop` or `main`) and `push` (to `develop` or `main`).

| Job | Runs | Steps |
|---|---|---|
| `backend` | always | `setup-java@v4` (Temurin 21, Maven cache) → `./mvnw -B verify` (unit and IT, Testcontainers) → upload Surefire/Failsafe and JaCoCo reports → publish a test summary showing unit and IT counts |
| `frontend` | always | `pnpm install --frozen-lockfile` → `lint` → `typecheck` → `build` |
| `docker` | needs backend + frontend | `docker build` → `docker compose up -d --build` with a CI `.env` → wait for `/actuator/health` = UP → smoke-test register and login with curl → `docker compose down -v` |
| `deploy-render` | **push to `main` only**, needs all three | `curl -fsS -X POST "$RENDER_DEPLOY_HOOK_URL&ref=$GITHUB_SHA"`, then poll `https://<render-app>/actuator/info` until `git.commit` equals `$GITHUB_SHA` (15 min timeout, retries for cold start). The job fails if it never matches, which proves the deployment really happened. |
| `deploy-cloudrun` (P1) | push to `main` only, needs `deploy-render` | `google-github-actions/auth` (Workload Identity Federation, no JSON keys) → build and push the image to Artifact Registry (tag = SHA) → `google-github-actions/deploy-cloudrun` → poll `/actuator/info` for the SHA |

**Workflow rules.**
- `concurrency: deploy-${{ github.ref }}` prevents overlapping deploys.
- Third-party actions are pinned to a major version.
- Dependency review runs on PRs.

**`scheduler-tick.yml`.**
- Schedule: `cron: "17 */3 * * *"`, plus manual `workflow_dispatch`.
- Action: POST `/internal/scheduler/tick` on the Render URL with `X-SR-Tick-Token`, retrying through the cold start.
- Note: GitHub disables scheduled workflows after 60 days without repository activity. Re-enable it before the demo period.

**Render settings.** Auto-Deploy is **Off**. Render deploys only through the hook from CI (passing `ref` also disables auto-deploy), so tests always gate production.

**GitHub secrets and variables.**
- Secrets: `RENDER_DEPLOY_HOOK_URL`, `TICK_TOKEN`, `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT`.
- Variables: `RENDER_APP_URL`, `GCP_PROJECT_ID`, `GCP_REGION`, `CLOUD_RUN_SERVICE`, `CLOUD_RUN_URL`.
- Runtime application secrets live in Render's environment settings and in Google Secret Manager or Cloud Run environment variables (§17), never in GitHub logs.

---

## 16. Containerization and configuration

### 16.1 `Dockerfile` (repository root, multi-stage)
1. **`web` stage:** `node:22-alpine` with corepack and pnpm; `pnpm install --frozen-lockfile`; `pnpm build`, producing `frontend/dist`.
2. **`build` stage:** `maven:3.9-eclipse-temurin-21`; resolve dependencies first (cache layer); copy `frontend/dist` into `src/main/resources/static`; `./mvnw -B package -DskipTests`. Tests run in CI's `backend` job, not in the image build, to save Render build minutes.
3. **`runtime` stage:** `eclipse-temurin:21-jre-alpine`, with a non-root `app` user, a copied jar, `EXPOSE 8080`, `JAVA_TOOL_OPTIONS` from NFR-RES-1, `HEALTHCHECK` using wget on `/actuator/health`, and `ENTRYPOINT ["java","-jar","/app/app.jar"]`.

The server binds to `${PORT:8080}`. Render injects `PORT` (default 10000) and Cloud Run injects `PORT=8080`.

### 16.2 `docker-compose.yml` (repository root; must work with `docker compose up --build`)
| Service | Image / build | Notes |
|---|---|---|
| `db` | `postgres:17-alpine` | Credentials from `.env`; named volume; `pg_isready` healthcheck |
| `app` | `build: .` | `env_file: .env`; `depends_on: db (service_healthy)`; port `8080:8080`; profile `local`; demo enabled; `SOURCERIPPLE_PUBLIC_BASE_URL=http://localhost:8080`; `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS=true` (local only) |
| `prometheus` | `prom/prometheus` | Mounts `infra/prometheus/prometheus.yml`; scrapes `app:8080/actuator/prometheus` every 15 s with basic auth, where `password_file` is a Compose secret sourced from the `SOURCERIPPLE_METRICS_PASSWORD` environment variable; port 9090 |
| `grafana` | `grafana/grafana` | Provisioned datasource and dashboard from `infra/grafana`; admin credentials from `.env`; anonymous access off; port 3000 |

**Credentials.** Compose holds **no credential values**. Every secret is referenced as `${VAR:?set VAR in .env}`, so a missing `.env` fails with a clear message.

**README run steps:**
1. `cp .env.example .env` (it holds dev-only placeholder values).
2. `docker compose up --build`.
3. Open the app at http://localhost:8080, Grafana at http://localhost:3000 and Prometheus at http://localhost:9090.

### 16.3 Environment variables
| Variable | Purpose | Local | Render | Cloud Run |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` / `prod` (`test` is used by the test suite) | local | prod | prod |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | JDBC connection. For Neon: `jdbc:postgresql://<host>/<db>?sslmode=require` with the **direct (non-pooled)** host. Render and Neon give `postgresql://user:pass@host/db`, which **must be converted to JDBC form**. | compose db | Neon project A | Neon project B |
| `PORT` | HTTP port | 8080 | injected | injected |
| `SOURCERIPPLE_PUBLIC_BASE_URL` | Absolute links (evidence URLs, demo fixture and receiver) | `http://localhost:8080` | Render URL | Cloud Run URL |
| `SOURCERIPPLE_ENCRYPTION_KEY` | AES-GCM key for webhook secrets (32 bytes, base64) | dev value | secret | secret |
| `SOURCERIPPLE_ADMIN_EMAIL` / `_PASSWORD` | Bootstrap admin | dev | secret | secret |
| `SOURCERIPPLE_METRICS_USERNAME` / `SOURCERIPPLE_METRICS_PASSWORD` | `/actuator/prometheus` basic auth | dev | secret | secret |
| `SOURCERIPPLE_TICK_TOKEN` | Tick endpoint token | dev | secret | secret |
| `SOURCERIPPLE_SCHEDULER_MODE` | `internal` / `external` / `both` | internal | both | external |
| `SOURCERIPPLE_MIN_INTERVAL_HOURS` | Minimum watch interval | 1 | 6 | 6 |
| `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS` | SSRF exemption (refused in prod) | true | false | false |
| `SOURCERIPPLE_DEMO_ENABLED` / `_DEMO_WEBHOOK_SECRET` / `_DEMO_USER_PASSWORD` | Demo kit | true | true | true |
| `GIT_COMMIT` | Shown in `/actuator/info` (Render provides `RENDER_GIT_COMMIT`, which is used as a fallback) | — | auto | set at deploy |
| `JAVA_TOOL_OPTIONS` | JVM memory flags | optional | NFR-RES-1 | NFR-RES-1 |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Compose database | dev | — | — |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | Compose Grafana | dev | — | — |

---

## 17. Hosting and deployment (open source, free tiers only)

### 17.1 Free-tier stack (checked 17 Nov 2026; re-check before each phase)
| Need | Service | Free terms relevant to us |
|---|---|---|
| Code, CI and security | **GitHub, public repository** | Branch protection, rulesets, Actions on standard runners, secret scanning with push protection, Dependabot and dependency review are all free for public repositories. |
| Primary app hosting (**required**) | **Render, free web service (Docker)** | 512 MB RAM; **spins down after 15 min without inbound traffic**; about 1 min cold start; 750 free instance hours per workspace per month; 5 GB bandwidth and 500 build minutes per month; no persistent disk; may be restarted at any time; deploy hooks support `ref=<sha>`; exposes `RENDER_GIT_COMMIT`. |
| Database for Render | **Neon Free** (project A) | Free permanently, no credit card. 1 GB storage per project; 100 CU-hours per project per month; **scales to zero after 5 min idle (cannot be disabled)** and resumes in a few hundred milliseconds; 5 GB transfer per project. |
| Database fallback | Render Postgres Free | 1 GB, **expires 30 days after creation** (14-day grace period), one per workspace. Use only if Neon is blocked, and create it within 30 days of grading. |
| Second deployment (**bonus**) | **Google Cloud Run** (request-based billing) | Free tier of 2M requests, 180,000 vCPU-seconds and 360,000 GiB-seconds per month. **CPU is allocated only while requests are processed.** Idle instances shut down. **Requires a billing account** (card on file). |
| Database for Cloud Run | **Neon Free** (project B) | Separate project, so each deployment gets its own quota. |
| Image registry for Cloud Run | Artifact Registry | 0.5 GB of free storage. Keep the last 2 images with a cleanup policy. |
| Cloud Run wake-up | Cloud Scheduler | One HTTP job every hour with the tick header. **(verify)** the free allowance has historically been 3 jobs per billing account. |
| Not used | Cloud SQL | No free tier (30-day trial only). |

### 17.2 Region (DEC-10)
Use **Singapore** everywhere, the nearest major region to the team (UTC+6): Render *Singapore*, Neon *AWS ap-southeast-1*, Cloud Run *asia-southeast1*.
- **(verify)** that Neon offers that region and that Cloud Run's free tier applies there.
- If not, put each app and its database in the *same* US region (Render Oregon or Ohio, Neon us-east or us-west, Cloud Run us-central1).

### 17.3 Primary: Render with Neon (DEC-09, P0)
1. Create Neon project A (Postgres 17). Copy the **direct** connection details and form the JDBC URL with `sslmode=require`.
2. Create a Render **Web Service**:
   - repository connected; branch `main`; runtime **Docker**; root `Dockerfile`; instance **Free**; region Singapore;
   - health check path `/actuator/health`;
   - **Auto-Deploy: Off**;
   - environment variables per §16.3.
3. Copy the service's **Deploy Hook** URL into the GitHub secret `RENDER_DEPLOY_HOOK_URL`. Add `RENDER_APP_URL` as a repository variable.
4. Generate `SOURCERIPPLE_TICK_TOKEN`, set it on Render and as `TICK_TOKEN` in GitHub. Enable `scheduler-tick.yml`.
5. Run a release PR into `main`. CI deploys and verifies the SHA via `/actuator/info`.
6. Seed the demo (automatic when the demo is enabled) and verify the golden flow on the public URL.

**Render pitfalls:**
- **Spin-down.** Scheduled checks happen only while the service is awake. Mitigations: catch-up on wake, a cron tick every 3 h, and **Check now** in the demo. No 24/7 monitoring is promised on the free tier.
- **Restarts.** In-memory state is lost. Sessions live in PostgreSQL, and nothing important is in memory.
- **Disk.** The filesystem is ephemeral. Nothing is written to disk.
- **Memory.** 512 MB, so the NFR-RES-1 JVM flags apply.
- **Build minutes** (500/month). Only `main` deploys, and the image build skips tests.
- **Neon CU-hours.** Render is awake only about 15 min after each request or tick, and Neon sleeps 5 min after that. Roughly 8 ticks a day plus demo use fits within 100 CU-hours. **Do not add a 24/7 keep-alive pinger**, as it would burn Neon's quota.

### 17.4 Second deployment: Google Cloud Run with Neon (DEC-11, P1, +10 bonus)
**Cloud Run vs Cloudflare (decision: Cloud Run).**

| Criterion | Google Cloud Run | Cloudflare (Workers / Containers) |
|---|---|---|
| Runs our Spring Boot Docker image unchanged | **Yes**: any container that listens on `$PORT` | Workers: **no**, because the V8 isolate runtime can't run a JVM. Containers: yes, but only behind a Worker/Durable Object wrapper. |
| Free for us | **Yes**: request-based free tier (billing account required) | **No**: Containers need **Workers Paid ($5/month)** |
| Managed Postgres | No free one, so Neon is used | None, so Neon or Hyperdrive would be needed anyway |
| Background work | No CPU outside requests, solved by our tick design (DEC-08) | Containers also sleep after idle |
| Maturity / CI | GA; official `auth` and `deploy-cloudrun` GitHub Actions | Containers are newer, with some features still in public beta |
| Verdict | **Chosen** | Not used for app hosting (it could front a custom domain later) |

**Cloud Run configuration:**
- Service `sourceripple`, region asia-southeast1.
- **Request-based billing**, min instances 0, **max instances 1** (keeps it in the free tier and leaves one scheduler).
- 1 vCPU, 1 GiB memory, **startup CPU boost on**, concurrency 20, request timeout 300 s, `--allow-unauthenticated` (the app must be public).
- Environment from §16.3 with `SOURCERIPPLE_SCHEDULER_MODE=external`. Secrets come from Secret Manager **(verify the free allowance)** or are set as Cloud Run environment variables from CI secrets.

**One-time setup:**
1. Create a GCP project and link the billing account.
2. Enable the Run, Artifact Registry, IAM Credentials and Cloud Scheduler APIs.
3. Create an Artifact Registry Docker repository with a cleanup policy.
4. Set up a deployer service account (`run.admin`, `artifactregistry.writer`, `iam.serviceAccountUser`).
5. Create a Workload Identity Federation pool bound to this GitHub repository.
6. Create a Cloud Scheduler job: `POST <CLOUD_RUN_URL>/internal/scheduler/tick`, header `X-SR-Tick-Token`, schedule `0 * * * *`.

**Cloud Run pitfalls:**
- Never rely on `@Scheduled` or `@Async` there (DEC-08).
- Cold start takes a few seconds even with CPU boost, so warm the service before the demo.
- Keep image storage under 0.5 GB.
- If nobody on the team can attach a billing account, raise it immediately (§24, Q3). The fallback must be another container host with a free tier, chosen and verified at that time.

**README deployment section (bonus rubric: 5 marks).** It must cover both platforms step by step (the setup above), the architecture difference (sleeping instance with an internal loop vs request-only CPU with an external tick), and every environment variable.

### 17.5 Demo-day runbook
- **T-24 h:**
  - Neon quota is below 70%.
  - Render and Cloud Run show the latest SHA in `/actuator/info`.
  - Fixture reset to v1.
  - Demo users can log in.
  - **Record a full backup video** of the golden flow.
- **T-15 min:** open both URLs to warm them up, start local Compose (Grafana), and log in as the maintainer, reviewer and admin in separate browser profiles.
- **Fallback:** if a free tier fails live, run the identical flow on local Compose, then show the backup video and the CI deploy logs.

---

## 18. Observability (bonus: Actuator 2 + Prometheus 4 + Grafana 4)

- **OBS-1 Actuator.**
  - Expose `health` (with liveness/readiness groups and DB details shown only when authorized), `info` (app version, `git.commit`, build time) and `prometheus`. Nothing else is web-exposed.
  - Security: `health` and `info` are public; `prometheus` uses HTTP Basic with the metrics credential, in a dedicated stateless filter chain.
- **OBS-2 Custom Micrometer metrics** (Prometheus names):

  | Metric | Tags / type |
  |---|---|
  | `sourceripple_monitor_checks_total` | `outcome`, `trigger` |
  | `sourceripple_fetch_duration_seconds` | histogram |
  | `sourceripple_changes_detected_total` | |
  | `sourceripple_impacts_created_total` | `confidence` |
  | `sourceripple_impact_reviews_total` | `status` |
  | `sourceripple_webhook_deliveries_total` | `result` |
  | `sourceripple_webhook_delivery_duration_seconds` | |
  | `sourceripple_sources_due` | gauge |
  | `sourceripple_sources_failing` | gauge |
  | `sourceripple_watches_active` | gauge |
  | `sourceripple_scheduler_tick_duration_seconds` | |

  Built-in metrics are also scraped: `http_server_requests_seconds`, `jvm_*`, `hikaricp_*`, `process_*`.
- **OBS-3 Prometheus** runs in Compose, scraping every 15 s (§16.2). Its config lives in `infra/prometheus/prometheus.yml`.
- **OBS-4 Grafana** runs in Compose. The datasource and dashboard are **provisioned from the repository**, so `docker compose up --build` shows a working dashboard with no manual clicks. Dashboard "SourceRipple Overview" panels:
  1. Checks per minute by outcome
  2. Fetch latency p50 and p95
  3. Changes detected per hour
  4. Impacts created by confidence
  5. Open impacts by review status
  6. Webhook success vs failure, and delivery latency
  7. Due and failing sources
  8. HTTP request rate and p95 by URI
  9. JVM heap, GC and threads
  10. Hikari connections
  - The dashboard's metrics are emitted by the demo flow, so every panel moves during the presentation.
- **OBS-5 (P2).** Grafana Cloud's free tier scraping the public Render deployment through basic auth. **(verify the current free terms)**. Optional; the bonus is earned through Compose.

---

## 19. Course compliance matrix

### 19.1 Mandatory requirements
| Guideline requirement | How SourceRipple meets it | PRD ref | Evidence at grading |
|---|---|---|---|
| Spring Boot, PostgreSQL, Spring Security, Docker, GitHub CI/CD, Render | Spring Boot monolith, PostgreSQL (Neon / Compose), Spring Security, Docker and Compose, GitHub Actions, Render | §11, §15–§17 | Repository, live URL |
| Exactly 3 members | Three members, each owning code phases | §21 | Commit and PR history |
| Registration, login/logout, BCrypt | FR-AUTH-1..5 | §7 | I1, I2; database shows BCrypt hashes |
| Role-based authorization (3 roles), URL and method-level | ADMIN / MAINTAINER / REVIEWER; `SecurityFilterChain` rules + `@PreAuthorize` + `AccessPolicy` | §6 | I4, I5, I6; live 403 demo |
| REST principles, proper methods and status codes, global exception handling | §10 conventions, error envelope, `@RestControllerAdvice` | §10, FR-API | Swagger UI, I15 |
| ≥ 5 controllers; CRUD for ≥ 3 entities | 15 controllers; CRUD for 5 entities | §10.3 | Swagger UI |
| PostgreSQL, ≥ 6 tables, 1:M / M:1 / M:M, JPA | 20 domain tables; `@ManyToMany` users↔roles and orgs↔users; association entity `watches` | §9 | ER diagram, entities |
| ≥ 20 unit (service) + ≥ 10 integration (controller) tests; JUnit, Mockito, SpringBootTest, MockMvc; run in CI | 31 planned + 17 planned | §14 | CI test summary |
| Dockerfile + `docker-compose.yml` (app + PostgreSQL); environment variables; `docker compose up --build` | Root multi-stage Dockerfile; Compose with db, app, prometheus, grafana; `.env` | §16 | Live run; CI `docker` job |
| Branches `main` (protected) / `develop` / feature; no direct push; PR with ≥ 1 approval | §15.2 rules, admins included | §15.2 | Branch settings screenshot, PR list |
| GitHub Actions: build, test, auto-deploy to Render from `main` | `ci.yml`: backend, frontend, docker, deploy-render (SHA-verified) | §15.3 | Actions run links |
| Deployed on Render, publicly accessible | Render free web service + Neon | §17.3 | Live URL |
| README: description, architecture diagram, ER diagram, API endpoints, run instructions, CI/CD explanation | README outline | §20.1 | README |
| 15-minute demo | Timed script | §20.2 | Presentation |

### 19.2 Rubric (100 marks) and how each is earned
| Rubric item | Marks | Earned by |
|---|---|---|
| Design patterns | 10 | Six genuine GoF patterns (§11.5), each with a diagram and a test |
| Clean code and naming | 5 | NFR-MAINT-1, layering rules (§11.2), lint gates |
| Exception handling | 5 | Error envelope, codes, global handler, request IDs (FR-API) |
| Spring Security implemented | 5 | Session auth, CSRF, four authentication mechanisms in dedicated filter chains (session, API key, tick token, metrics basic auth) |
| Password encryption | 3 | BCrypt (FR-AUTH-1) |
| Role-based access enforced | 7 | Three-layer enforcement (§6.3); I4–I6; live demo |
| Unit test quality | 7 | 31 focused Mockito tests on real logic (§14.1) |
| Integration tests | 5 | 17 MockMvc + Testcontainers tests (§14.2) |
| Tests run in CI | 3 | `backend` job, required check; failing-then-fixed run kept |
| Proper Dockerfile | 4 | Multi-stage, non-root, healthcheck, small JRE image |
| Compose works | 4 | `docker compose up --build`, verified in the CI `docker` job |
| Environment configuration | 2 | `.env.example`, fail-fast variables, no credentials (§16) |
| Branch protection | 5 | §15.2 (`main` and `develop`, admins included) |
| GitHub Actions workflow | 5 | §15.3 |
| Automatic deployment | 5 | Deploy hook with `ref` and SHA verification |
| Entity relationships | 10 | §9 |
| App runs without error | 5 | Health checks, smoke tests, demo runbook (§17.5) |
| Proper demonstration | 5 | §20.2 script, rehearsed twice, backup video |
| README | 5 | §20.1 |
| **Bonus: Actuator / Prometheus / Grafana** | **+10** | §18 |
| **Bonus: additional deployment + documentation** | **+10** | §17.4 (Cloud Run) and the README deployment section |

### 19.3 Automatic-failure guards
| Failure condition | Guard | Owner | Verified |
|---|---|---|---|
| No design patterns | §11.5 implemented with tests U12–U16, U27, U28 | Ajoy (Adapter, Factory, Proxy, Flyweight, Singleton), Imrul (Bridge) | Phases 4, 8 and 10 |
| No role-based access control | §6 plus I4–I6 required in CI | Ajoy | Phase 2 |
| Direct push to `main` | One-time scaffold commit, then protection with admins included turned on immediately; pre-push hook blocks `main` and `develop` | Lead | Phase 1, day 1 |
| No Dockerization | Dockerfile and Compose in the scaffold; CI `docker` job | Lead | Phase 1 |
| Tests not implemented | Required CI check; count audit | All; audit by Lead | Every phase; final audit in Phase 10 |
| App not deployed | Image built and smoke-tested in CI from Phase 1; hosting accounts created in Phase 1; SHA-verified deploys in **Phase 11** | Lead | Phase 1 (image), Phase 11 (live) |

---

## 20. Deliverables

### 20.1 README.md (required content)
1. Project description, one-line promise, and live URLs (Render; Cloud Run).
2. Feature list and roles (with the permission matrix summary).
3. **Architecture diagram** (Mermaid, §11.1) and the execution model.
4. **ER diagram** (Mermaid, §9.2).
5. **API endpoints** table (§10.3) plus a link to Swagger UI.
6. Design patterns: a table and links to the diagrams in `docs/diagrams/`.
7. **Run locally**: prerequisites, `cp .env.example .env`, `docker compose up --build`, URLs, demo accounts.
8. Testing: how to run, counts, and where the reports are.
9. **CI/CD explanation**: a pipeline diagram (Mermaid), branch strategy, protection rules, deploy verification.
10. **Deployment**: Render + Neon and Cloud Run + Neon + Cloud Scheduler step by step, with the environment variable reference (bonus documentation).
11. Observability: how to open Prometheus and Grafana, and what each panel shows.
12. Team and contributions; license; third-party notices.

### 20.2 15-minute demo script
| Time | Segment | What is shown |
|---|---|---|
| 0:00–1:00 | Problem and pitch | Presented **on the live landing page**: hero, animated ripple, "how it works", and a quick look at pricing and the open-core vision |
| 1:00–2:00 | Architecture | Diagram, execution model, stack |
| 2:00–6:30 | **Golden flow live on Render** | 1. Log in as the maintainer; Demo Org with 3 projects. 2. Watch detail with its baseline. 3. Admin switches the fixture to v2. 4. **Check now** shows the CHANGED run. 5. Change detail: diff `customer_id → customer_reference`, footer ignored. 6. Impact Inbox: **2 NEW impacts, 0 for Fitness**. 7. Webhook delivery log: DELIVERED, plus the demo receiver with "signature valid". |
| 6:30–8:00 | **Roles** | The reviewer resolves an impact with a note, sees no "New watch", and a forced API call returns 403 (DevTools). The admin console shows users and roles and monitor runs. A reviewer calling `/admin` gets 403. |
| 8:00–9:00 | Patterns and errors | Pattern class-diagram slide; the error envelope for a blocked private-IP URL (`SOURCE_FETCH_BLOCKED`) |
| 9:00–11:00 | **Git and CI/CD** | Branches and protection settings; a reviewed PR; an Actions run with unit and IT counts; the failing-then-fixed run; deploy job SHA matching `/actuator/info` |
| 11:00–12:00 | Docker | `docker compose ps` (db, app, prometheus, grafana); `.env.example` |
| 12:00–13:30 | **Bonus** | Grafana dashboard moving with the demo traffic; the same app running on Cloud Run |
| 13:30–15:00 | Q&A buffer | Optional MCP `get_change` returning the same evidence as the UI |

---

## 21. Delivery plan, team and scope-cut order

### 21.1 Team and working model
**Members:** **Lead** (repository owner; fill in name), **Ajoy** and **Imrul**. All three write production code, open PRs and review each other.

| Phases | Who leads | What happens |
|---|---|---|
| **1** | **Lead** | Sets up the repository, its structure, branch protection, the CI build/test pipeline, and Docker/Compose. |
| **2–7** | **Ajoy** | Builds the **baseline and primary product**: the full backend core loop plus functional screens. Lead and Imrul review every PR and take small parallel tasks (frontend foundation, demo kit), so all three show up in the Git history from the start. |
| **8–10** | **Lead + Imrul** | Build the **additions and polish**: integrations, admin, observability, marketing site, UI polish, test audit and README. Ajoy fixes core bugs, reviews, and draws the pattern diagrams. |
| **11** | **Lead** (Imrul and Ajoy supporting) | **Deployments and release**: Render, Cloud Run, deploy automation, runbook, rehearsal. |

### 21.2 Phase plan (development docs expand each phase into tasks)
| Phase | Name | Owner | Parallel / support | Scope (PRD refs) | Exit criteria |
|---|---|---|---|---|---|
| 1 | Foundation and scaffolding | Lead | Ajoy and Imrul verify locally | Repository structure (§15.1), protection (§15.2), `ci.yml` jobs `backend`, `frontend` and `docker` (§15.3, no deploy jobs yet), root Dockerfile and Compose (§16), `.env.example`, README skeleton, issue board. Render, Neon and GCP accounts created (no deploy). | Protected `main`/`develop`; CI green on the empty app; `docker compose up --build` starts app and db; every member has cloned the repository, run `scripts/setup-git-hooks.sh` and started the stack locally |
| 2 | Data model, identity and access | Ajoy | Lead reviews | Flyway V1 and roles seed, JPA entities (§9), FR-AUTH, roles, membership, `AccessPolicy` (§6), security filter chains; FR-ADM-1 (users and roles API); tests U1–U3, U8, I1–I3, I5 | RBAC and auth tests green in CI |
| 3 | Workspace and API foundation | Ajoy | **Imrul:** frontend foundation (§8.2–§8.4: Vite, Tailwind v4, shadcn, tokens, both shells, router guards, Zustand, TanStack Query and generated client, S2 auth pages, S15 system pages) | FR-ORG, FR-PRJ, FR-API (error envelope, request ID, OpenAPI); tests U4–U6, I7, I15 | CRUD and error envelope green; login and logout work in the SPA |
| 4 | Safe fetching and extraction | Ajoy | Lead reviews | NFR-SEC-1 (canonicalizer, `GlobalFetchLimits`, `SafeFetchProxy`, OkHttp DNS guard, robots), FR-EXT-1..4 (HTML and RSS adapters, factory, rule-set Flyweight), FR-WCH-1 preview; tests U11–U16, U28 | Preview returns sections for HTML and RSS; private targets blocked |
| 5 | Monitoring engine and watches | Ajoy | **Lead:** demo kit (FR-DEMO-1..6) | FR-WCH-2..7, FR-EXT-5 (versioning), FR-MON (claims, loop, tick, runs, backoff, pacing); tests U7, U9, U10, U17, U18, U30, I4, I8, I13, I16 | Fixture watched, baseline READY, a re-check creates no duplicate version |
| 6 | Change detection, dependencies and impacts | Ajoy | Lead pairs on I9 | FR-EXT-6..9, FR-DEP, FR-IMP; tests U19–U24, I6, I9–I11 | **Golden flow (Appendix A, version, change and impact results) green in CI**; the webhook and MCP results are verified in Phase 8 |
| 7 | Baseline product screens | Ajoy | Imrul reviews UI conventions | Functional S3, S4 (tiles), S5, S6, S7, S8 (basic diff), S9 (table view), S10 (basic triage), S14 on the Phase 3 shells | The whole core loop is usable in the browser end to end |
| 8 | Integrations and admin | Imrul + Lead | Ajoy reviews | **Imrul:** FR-NTF + S12, FR-WHK + S11 webhooks UI, S13 admin UI. **Lead:** FR-KEY, FR-MCP, FR-ADM-2..4 backend; extends the Phase 6 change transaction with notification and outbox rows (FR-NTF, FR-WHK-3). Tests U25–U27, U29, I12, I14 | Signed webhook DELIVERED to the demo receiver; admin console works |
| 9 | Observability and marketing site | Lead + Imrul | Ajoy reviews | **Lead:** §18 (Actuator, metrics, Prometheus, Grafana), FR-WEB-5..7 (waitlist API, admin waitlist, demo login and reset), U31, I17. **Imrul:** §8.6 landing, pricing, legal and SEO (FR-WEB-1..4, 8..11) | Grafana dashboard live through Compose; landing and pricing reach Lighthouse ≥ 90 locally |
| 10 | Polish and hardening | Imrul + Lead | **Ajoy:** core bug fixes, pattern diagrams | **Imrul:** app polish (Motion, ⌘K, keyboard triage, S9 graph, split/unified diff, S4 charts, responsive, accessibility). **Lead:** test audit (≥ 31 unit / 17 IT), security review, failing-then-fixed CI evidence, README (§20.1) except the deployment section, demo script | All P0 and P1 items done except deployment; CI green |
| 11 | Deployments and release | Lead | **Imrul:** Cloud Run and docs. **Ajoy:** production smoke tests | §17.3 Render + Neon A, `deploy-render` job with SHA check, `scheduler-tick.yml`; §17.4 Cloud Run + Neon B + Cloud Scheduler + `deploy-cloudrun`; README deployment section; §17.5 runbook; two rehearsals; backup video; release `develop → main` | Both public URLs serve the release SHA; the golden flow passes on Render; the demo has been rehearsed |

**Deployment timing.** Deploying in Phase 11 is the team's choice. To keep the "App not deployed" automatic fail from ever being at risk:
- the production Docker image is built and smoke-tested in CI from Phase 1;
- the hosting accounts are created in Phase 1;
- §17 already holds the exact steps;
- Phase 11 must start **at least 5 days before submission**.

An optional one-off smoke deploy is allowed at any earlier point.

### 21.3 Sequencing and calendar
- **Dependencies:**
  - Phases 2 → 3 → 4 → 5 → 6 → 7 run in order (Ajoy).
  - Imrul's Phase 3 frontend foundation runs alongside Ajoy's backend work.
  - Phase 8 needs Phase 6 (change events).
  - Phase 9's marketing pages need only the Phase 3 shells, but start after Phase 6 to protect the core schedule.
  - Phase 10 follows 8–9; Phase 11 comes last.
  - Phases 8–9 may overlap Phase 7, since different people own them.
- **Rough calendar** (blueprint's 5 weeks): Phases 1–3 in week 1–2, Phases 4–6 in week 2–3, Phases 7–9 in week 3–4, Phases 10–11 in week 5. Exact dates come once Q4 is answered.

### 21.4 Collaboration rules (rubric evidence)
- Each member has ≥ 5 merged PRs containing code and has given ≥ 5 reviews.
- Nobody approves their own work.
- Ajoy's PRs alternate between Lead and Imrul as reviewer; Lead and Imrul review each other in Phases 8–11.
- Every PR names its phase and PRD IDs (for example `Phase 4 · NFR-SEC-1 · U12`).

### 21.5 Scope-cut order (if behind schedule)
Cut in this order:
1. MCP write tools (P2), then MCP read tools (keep API keys).
2. Heavy marketing animations and in-app Magic UI accents. The landing and pricing pages stay, as static but polished pages.
3. Dependency Map graph (keep the table view).
4. Overview charts (keep the stat tiles).
5. Webhook test ping and redelivery.

**Never cut:** RBAC, Render auto-deploy, CI/CD, Docker and Compose, tests ≥ 20/10, the core loop (monitor → diff → dependency → impact → review → webhook), README, Prometheus/Grafana (+10 for little effort), and the RSS adapter (it keeps Adapter and Factory genuine).

---

## 22. Risks
| Risk | Impact | Mitigation |
|---|---|---|
| Render free spin-down and restarts | Missed schedules; slow first request | Catch-up on wake, cron tick, synchronous manual check, sessions in the database, warm-up before the demo, backup video |
| Neon CU-hour quota | Database suspended until month end | No 24/7 pinger; tick every 3 h; separate projects per deployment; check usage weekly |
| No GCP billing account | Second-deployment bonus at risk | Decide in Phase 1 (Q3); choose and verify a fallback host early |
| Spring AI MCP vs Spring Boot version | MCP blocked | Pin a compatible Boot version in Phase 1; MCP is P1 and cut first |
| JS-rendered pages | Empty extraction | `UNSUPPORTED` state with a clear hint; demo uses controlled fixtures |
| SSRF or abuse on a public instance | Security incident, host suspension | NFR-SEC-1 and NFR-SEC-7; prod refuses `allow-private-targets`; admin kill switches |
| Scope creep (the guideline values workflow over features) | Late, low-quality delivery | P0 list, scope-cut order, phase exit criteria |
| Uneven contribution | Rubric collaboration evidence | Phase ownership in §21, PR and review minimums |
| Deployment left to Phase 11 | "App not deployed" automatic fail if it slips | Image verified in CI from Phase 1; Render, Neon and GCP accounts created in Phase 1; deployment steps pre-written (§17); Phase 11 starts at least 5 days before submission; an optional one-off smoke deploy is allowed at any time |
| Live demo failure | Lost demo marks | Runbook (§17.5), local Compose fallback, recorded video |
| Free-tier terms change | Hosting breaks | Re-verify §17.1 at the start of each phase; this PRD is updated if anything changes |
| Marketing polish eats core time | Graded work slips | Phase 9 marketing work starts only after the Phase 6 core-loop exit; animations are first in the cut order |
| Overclaiming on the public site | Credibility or legal risk | FR-WEB-2 honesty rule; review every claim against the shipped product before release |
| Name or trademark conflict before selling | Forced rebrand | Clear the trademark and domain before any paid launch (Q7) |

---

## 23. Changes from the submitted blueprint
The core idea is unchanged. These are compliance fixes (C), feasibility fixes (F), gap fixes (G) and scope decisions (S).

| # | Blueprint § | Blueprint said | PRD decision | Type |
|---|---|---|---|---|
| 1 | §06, §16 | Single owner; roles not required; multi-user roles deferred | Three roles + membership + three-layer enforcement (§6) | C (automatic-fail) |
| 2 | §19, §20, §27 | Local Compose or "controlled server" counts as deployment | Render (required) via CI deploy hook; Cloud Run as the second platform (§17) | C (automatic-fail) + bonus |
| 3 | §19 | Feature branches → `main` via the lead | `main` / `develop` / feature with protection, admins included (§15.2) | C |
| 4 | §13 | Register and login only | Plus logout and `me` (FR-AUTH) | C |
| 5 | §13 | Partial endpoints; no deletes | 15 controllers; full CRUD for 5 entities (§10.3) | C |
| 6 | §07 | "JPA or JDBC" | JPA + Flyway, `ddl-auto=validate`; explicit `@ManyToMany` (§9) | C |
| 7 | §18 | Scenario tests only; no MockMvc | ≥ 20 Mockito service unit tests + ≥ 10 MockMvc ITs (§14) | C |
| 8 | §24 | `infra/` Compose; separate frontend and backend Dockerfiles | Root Dockerfile (SPA bundled) + root Compose (§16) | C |
| 9 | §23 | 12-minute presentation | 15-minute script (§20.2) | C |
| 10 | §23 | "3 members, 2 primary developers" | All three own code phases (§21) | C |
| 11 | §07 | Separate static frontend | SPA served by Spring Boot: one service, same-origin sessions, free-tier friendly (DEC-04) | F |
| 12 | §07, §09 | Java HttpClient with "connect to validated address" | OkHttp with a DNS guard and network interceptor; `java.net.http` cannot pin validated IPs (DEC-06) | F |
| 13 | §09 | robots.txt handling unspecified | crawler-commons parser, 24 h cache (FR-MON-7) | F |
| 14 | §08, §13 | Watch create returns 202 async; background jobs | 201 with synchronous baseline; synchronous manual check; no work after the response (DEC-08), because Cloud Run gives no CPU outside requests | F |
| 15 | §08 | Per-watch schedules and per-watch version cursors | Per-source scheduling (minimum interval of active watches); impacts fan out immediately (FR-MON-1) | F (simpler, fewer bugs) |
| 16 | §16 | Session vs token undecided | Server sessions in PostgreSQL + SPA CSRF (FR-AUTH-5/6) | F |
| 17 | §12 | No changed-section table; pruning could orphan evidence | `change_event_sections` with old and new text; version foreign keys `SET NULL` (FR-EXT-7/8) | G |
| 18 | §12 | Dependency → project + source; section required | Dependency → watch; nullable section = whole page (FR-DEP) | G |
| 19 | §11 | Two conflicting status sets (NEEDS_REVIEW / UNVERIFIED_SOURCE vs NEW…) | `match_confidence` + `review_status` (FR-IMP) | G |
| 20 | §15 | Outbox table undefined; in-app channel undefined | `webhook_deliveries` is the outbox; a `notifications` table backs `InAppChannel` | G |
| 21 | §13 | No preview, sections or run-status endpoints | Added: preview, sections, runs, summary (§10.3) | G |
| 22 | §16 | Webhook-secret encryption key unspecified | AES-256-GCM with `SOURCERIPPLE_ENCRYPTION_KEY`; shown once (FR-WHK-1) | G |
| 23 | — | Demo fixture and receiver location unspecified; SSRF guard would block localhost | Demo kit on public base URL; private targets allowed only in local and test, refused in prod (FR-DEMO, NFR-SEC-1.7) | G |
| 24 | — | Open registration on a public URL that fetches arbitrary URLs | Abuse controls (NFR-SEC-7) | G |
| 25 | §17, §23 | RSS adapter cut second | RSS adapter is P0, because the patterns need two adaptees | S |
| 26 | §06 | Sitemap/path discovery P1 | Out of lab scope | S |
| 27 | §06, §14 | MCP P1 including writes | P1 read tools only, built last; writes P2 | S |
| 28 | §19 | Basic observability | Actuator + Prometheus + Grafana (bonus) | S |
| 29 | §05 | UI aesthetics unspecified | Dark-first, Hatchet-inspired design language and frontend stack (§8) | S |
| 30 | §25 | Apache-2.0 as a candidate | Apache-2.0, public from day one (DEC-01) | S |
| 31 | §25 | "Optional hosted offering" as a vague future | Open-core vision: Community (self-hosted) plus SourceRipple Cloud (subscriptions later) (DEC-14) | S |
| 32 | §05 | No public site | Production-quality landing and pricing pages, waitlist, demo login, legal pages, SEO; no payments (DEC-15, FR-WEB) | S |

### Decision register
| ID | Decision |
|---|---|
| DEC-01 | Open source, Apache-2.0, public repository |
| DEC-02 | Roles ADMIN / MAINTAINER / REVIEWER |
| DEC-03 | Modular monolith |
| DEC-04 | SPA bundled into Spring Boot |
| DEC-05 | Java 21 + Maven + JPA/Flyway |
| DEC-06 | OkHttp with DNS guard |
| DEC-07 | Per-source scheduling |
| DEC-08 | No work after the response; internal loop and/or external tick |
| DEC-09 | Render + Neon primary |
| DEC-10 | Singapore region |
| DEC-11 | Cloud Run + Neon secondary; Cloudflare rejected for app hosting |
| DEC-12 | Hatchet-inspired dark design language |
| DEC-13 | Frontend stack (§8.2) |
| DEC-14 | Open-core business model; Cloud subscriptions later; no payments in the lab |
| DEC-15 | Public marketing site (landing, pricing with waitlist, legal pages) in the same SPA |

---

## 24. Open questions
| # | Question | Owner | Needed by |
|---|---|---|---|
| Q1 | Domain role names instead of ADMIN/SELLER/BUYER. The team understands they are allowed. Get a one-line written confirmation from the instructor if possible. | Lead | Phase 1 |
| Q2 | Is the Render app using an external free Postgres (Neon) acceptable as "deployed on Render"? If not, use Render Postgres Free and create it within 30 days of grading. | Lead | Phase 1 |
| Q3 | Who can attach a GCP billing account for Cloud Run? | Team | Phase 1 |
| Q4 | Exact submission and demo dates, to turn the phases into a calendar. | Lead | Phase 1 |
| Q5 | Spring Boot version compatible with the Spring AI MCP starter at implementation time. | Ajoy | Phase 1 |
| Q6 | Confirm Apache-2.0; add the lead's name to §21. | Lead | Phase 1 |
| Q7 | Trademark and domain clearance for "SourceRipple" before any paid launch, and the choice of a production domain for the public site. | Lead | Before public launch |
| Q8 | Licensing strategy for commercialization. Apache-2.0 lets anyone host the product commercially. Decide whether paid Cloud features live in a separate proprietary module or stay hosting-only. | Team | Before Cloud launch |
| Q9 | Final plan limits and prices, set after pilot interviews (blueprint §22); payment provider choice (post-lab). | Team | Before Cloud launch |

## Appendix A: Golden fixture and expected results
```
Demo Org
  Projects: E-commerce, Mobile Checkout, Fitness App

Sources (served by the demo kit at SOURCERIPPLE_PUBLIC_BASE_URL)
  /demo/vendor/payments    v1: Parameters → customer_id, amount | Footer → © 2025
                           v2: Parameters → customer_reference, amount | Footer → © 2026
  /demo/vendor/nutrition   unchanged in v1 and v2
  /demo/vendor/feed.xml    v2 adds one item

Watches and dependencies
  E-commerce      watches payments (SECTIONS: payments/parameters) → SERVICE payment-service @ payments/parameters
  Mobile Checkout watches payments (WHOLE_PAGE)                    → CODE_COMPONENT checkout-module @ payments/parameters
  Fitness App     watches nutrition (WHOLE_PAGE)                   → SERVICE workout-planner (whole page)
  Webhooks: E-commerce → /demo/receiver (impact.detected)

Action: admin switches the fixture v1 → v2; maintainer runs Check now on the payments watch

Required result
  1 new source version for /demo/vendor/payments (shared source fetched once)
  1 change event; changed sections = [payments/parameters] (footer stripped by extraction → no section)
  2 impacts, review_status NEW, confidence MATCHED (payment-service, checkout-module)
  0 impacts for Fitness App
  1 webhook delivery with a stable event ID → DELIVERED; demo receiver shows signature valid
  REST GET /changes/{id} and MCP sourceripple_get_change return identical evidence
  Re-running Check now without a change → outcome UNCHANGED / NOT_MODIFIED, no new version
```

## Appendix B: Glossary
| Term | Meaning |
|---|---|
| **Source** | A canonical URL within an organization, fetched once however many projects watch it. |
| **Watch** | A project's subscription to a source, with a mode, sections, interval and notify setting. |
| **Version** | A stored normalized snapshot, created only when content changes. |
| **Section** | A heading-delimited part of a version, with a stable key. |
| **Change event** | One detected difference between two versions, with section evidence. |
| **Dependency** | A customer-declared record (service, FAQ, workflow…) that relies on a watch's section or the whole page. |
| **Impact** | The inference "this dependency may be affected by this change", with a confidence and a review status. |
| **Delivery** | One webhook event to one endpoint (outbox row), retried until DELIVERED or FAILED. |
| **Tick** | An external HTTP trigger that runs due monitoring and delivery work, used on hosts that sleep or throttle CPU. |
| **Demo kit** | Built-in fixture pages and a verifying webhook receiver, used for deterministic demos and tests. |
| **Community edition** | The open-source, self-hosted SourceRipple (this repository, Apache-2.0). |
| **SourceRipple Cloud** | The planned managed, subscription-based service. Shown on the pricing page with a waitlist; not sold in the lab version. |
| **Public preview** | The free public deployment on Render and Cloud Run, with no SLA. |
