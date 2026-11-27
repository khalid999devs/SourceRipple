# Phase 9 · Observability and marketing site

## Goal
Operators see the system move in a provisioned Grafana dashboard fed by Prometheus and custom Micrometer metrics (bonus +10). Visitors get a fast, honest, public landing and pricing site with a waitlist, legal pages and a one-click live demo.

## People
- **Lead:** §18 observability, FR-WEB-5..7 (waitlist API, admin waitlist incl. S18, demo login and reset). Reviewer: Imrul (Ajoy second).
- **Imrul:** §8.6 landing, pricing, legal and SEO (FR-WEB-1..4, 8..11). Reviewer: Lead (Ajoy for copy honesty).
- **Ajoy:** reviews; checks every marketing claim against the shipped product (FR-WEB-2).

## Prerequisites
- **Start gate:** Phase 6 exit met (core loop protected, §21.3, §22).
- Phase 3 shells merged. Phase 8 webhook metrics need Phase 8 delivery code; add those meters when it lands.

## PRD references
§18 OBS-1..4 (OBS-5 is P2) · §16.2 prometheus/grafana services · §6.3 actuator rules · FR-WEB-1..11 · §8.6 · §8.5 S1, S16, S17, S18 · NFR-SEC-7 (public endpoint limits), NFR-SEC-8, NFR-SEC-10 · NFR-PERF-3 · PG7, PG8 · DEC-14, DEC-15.

## Ordered task checklist — observability (Lead)
- [ ] Actuator: expose `health` (liveness/readiness groups; details `when-authorized`), `info` (version, `git.commit`, build time) and `prometheus` only (OBS-1). Add `micrometer-registry-prometheus`.
- [ ] `shared/infrastructure/MetricsSecurityConfig.java`: dedicated stateless chain for `/actuator/prometheus`, HTTP Basic with `SOURCERIPPLE_METRICS_USERNAME` / `SOURCERIPPLE_METRICS_PASSWORD`, `@Order` before the session chain (OBS-1, §6.3).
- [ ] Custom meters (OBS-2) registered where the work happens: `sourceripple_monitor_checks_total{outcome,trigger}` and `sourceripple_fetch_duration_seconds` (monitor/source), `sourceripple_changes_detected_total` (diff), `sourceripple_impacts_created_total{confidence}` and `sourceripple_impact_reviews_total{status}` (impact), `sourceripple_webhook_deliveries_total{result}` and `sourceripple_webhook_delivery_duration_seconds` (notify), gauges `sourceripple_sources_due`, `sourceripple_sources_failing`, `sourceripple_watches_active`, timer `sourceripple_scheduler_tick_duration_seconds`.
- [ ] `infra/prometheus/prometheus.yml`: scrape `app:8080/actuator/prometheus` every 15 s with `basic_auth` and `password_file` from a Compose secret (OBS-3, §16.2).
- [ ] `infra/grafana/provisioning/datasources/` (Prometheus) and `provisioning/dashboards/` provider pointing at `infra/grafana/dashboards/sourceripple.json` "SourceRipple Overview" with the 10 panels of OBS-4. Anonymous access off; admin from `.env`.
- [ ] Complete the `prometheus` and `grafana` services in `docker-compose.yml` (ports 9090 and 3000).

## Ordered task checklist — waitlist and demo access (Lead)
- [ ] `marketing/V9__waitlist.sql`: `waitlist_signups` (unique normalized email) (§9.1 #20).
- [ ] `marketing/application/WaitlistService.java`: normalize email, upsert plan on repeat, require `consent: true` (400), silently drop when honeypot `website` is non-empty (FR-WEB-5).
- [ ] `marketing/api/PublicController.java` `POST /api/v1/public/waitlist`: always **202 with the same body**; 5/hour/IP (429 `RATE_LIMITED`); body ≤ 4 KB (NFR-SEC-7).
- [ ] Admin waitlist: `GET /admin/waitlist` (search), `GET /admin/waitlist/export.csv`, `DELETE /admin/waitlist/{id}` (FR-WEB-6) and UI S18 `src/features/admin/components/WaitlistTab.tsx`.
- [ ] `POST /api/v1/auth/demo-login` (demo only): session as `reviewer@demo.local` without a password, same context-saving path as login, 10/hour/IP (429 `RATE_LIMITED`) (FR-WEB-7).
- [ ] P1 `POST /admin/demo/reset`: re-run `DemoSeeder` and set fixture to v1 (FR-WEB-7).
- [ ] `server.forward-headers-strategy: native` so per-IP limits use the real client IP behind Render/Cloud Run proxies. Tomcat only trusts `X-Forwarded-For` from private and link-local peers; `framework` would trust any client.

## Ordered task checklist — marketing site (Imrul)
Conventions: [frontend-conventions.md](../../guides/engineering/frontend-conventions.md#marketing-site).
- [ ] `src/marketing/plans.ts`: typed plans from the §8.6 table; CTA states `available | waitlist | contact`; prices marked *illustrative* (FR-WEB-4).
- [ ] `src/marketing/pages/LandingPage.tsx`: the 10 sections of §8.6 in order; hero with a **real** Impact Inbox screenshot (WebP/AVIF) and the animated-beam ripple; CTAs Start free (`/register`), Try the live demo (demo-login), Star on GitHub, View pricing (FR-WEB-3).
- [ ] `src/marketing/components/`: `GitHubStars.tsx` (public API from the browser, cached 1 h in `sessionStorage`, no number on failure) (FR-WEB-8); `WaitlistDialog.tsx` (email, plan, company, message, consent, hidden `website`); `CodeTabs.tsx` (Shiki, lazy) with payload, signature verification in Node/Python/Java, `curl`, MCP config; Magic UI pieces (animated beam, border beam, dot pattern, shimmer button, bento grid, number ticker) copied with MIT notices.
- [ ] `src/marketing/pages/PricingPage.tsx`: monthly/yearly toggle ("2 months free"), plan cards with **Recommended** on Pro, sticky-header comparison table, pricing FAQ, "Cloud not yet available; public preview" note (§8.6).
- [ ] `PrivacyPage.tsx`, `TermsPage.tsx`: content per FR-WEB-10, marked as drafts.
- [ ] Marketing nav shows **Open dashboard** when signed in (FR-WEB-1).
- [ ] SEO (FR-WEB-9): per-route title and meta description, canonical, Open Graph and Twitter tags with static `public/og.png`, favicon set, `theme-color`; `public/robots.txt` (allow marketing; disallow `/app`, `/api`); `public/sitemap.xml` (marketing routes).
- [ ] No analytics, no tracking cookies (FR-WEB-11, NFR-SEC-10).

## Suggested PR breakdown
| # | Branch | Author | Scope | Reviewer |
|---|---|---|---|---|
| 1 | `feature/p9-actuator-metrics` | Lead | actuator, metrics chain, meters | Imrul |
| 2 | `feature/p9-prometheus-grafana` | Lead | infra configs, dashboard JSON, Compose | Ajoy |
| 3 | `feature/p9-waitlist` | Lead | waitlist API + admin + S18, U31, I17 | Imrul |
| 4 | `feature/p9-demo-login-reset` | Lead | demo login, reset | Ajoy |
| 5 | `feature/p9-landing` | Imrul | landing + shared marketing components | Lead, Ajoy (claims) |
| 6 | `feature/p9-pricing-legal-seo` | Imrul | pricing, legal, SEO files | Lead |

## Tests to write
| ID | Class | Intent |
|---|---|---|
| U31 | `marketing/application/WaitlistServiceTest` | `Foo@X.io ` normalized; repeat updates plan; no consent rejected; honeypot dropped without saving |
| I17 | `marketing/api/WaitlistIT` | Unauthenticated → 202, identical body for new and duplicate; missing consent → 400; MAINTAINER `/admin/waitlist` → 403 |

## Exit criteria
- [ ] `docker compose up --build` → Grafana at :3000 shows "SourceRipple Overview" with no manual clicks; panels move when the golden flow runs.
- [ ] `/actuator/prometheus` → 401 without credentials, 200 with them; `/actuator/health` and `/info` public.
- [ ] Lighthouse (mobile) ≥ 90 on performance, accessibility, best practices and SEO for `/` and `/pricing` locally; LCP < 2.5 s; initial JS for `/` ≤ 200 KB gzip (PG7, NFR-PERF-3).
- [ ] Waitlist, demo login and every landing CTA work. U31, I17 green.

## Evidence to capture
- Grafana dashboard screenshot with live data; Prometheus targets page showing `UP` (§19.2 bonus).
- Lighthouse reports for `/` and `/pricing` (store under `docs/diagrams/evidence/`).

## Risks and gotchas
- **Marketing polish must not eat core time** (§22). Animations are first in the scope-cut order.
- **Honesty is a hard rule** (FR-WEB-2): no fake logos, testimonials, counts, uptime or "most popular"; planned items say *Coming soon*.
- **CSP (NFR-SEC-8)** allows exactly `connect-src 'self' https://api.github.com` for the stars call; no other third-party origins.
- **Shiki** runs under `script-src 'self' 'wasm-unsafe-eval'`; keep it lazy so it stays out of the initial bundle.
- **Bundle split.** Marketing must not import from `src/features/**` or `AppShell` (NFR-PERF-3).
- **Gauges** must read cheap queries or cached values; Prometheus scrapes every 15 s and Hikari has 5 connections.
- **Rate limits per IP** are wrong behind a proxy unless forwarded headers are trusted.
