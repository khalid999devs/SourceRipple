# Phase 7 · Baseline product screens

## Goal
The whole core loop is usable in the browser on the Phase 3 shells: onboard, create a project, add a watch with preview, see the baseline, check now, read the change and its diff, register dependencies, and triage impacts. Functional and correct first; polish comes in Phase 10.

## People
- **Owner:** Ajoy
- **Reviewers:** Imrul on every PR (UI conventions), Lead alternating as the second reviewer when available

## Prerequisites
- Phase 6 exit criteria met (golden flow green; change, dependency and impact APIs exist).
- Phase 3 frontend foundation merged (tokens, shells, router, guards, generated client).

## PRD references
§8.4 app shell and interaction patterns (state coverage, role-aware UI, copy and IDs, time) · §8.5 S3, S4 (tiles), S5, S6, S7, S8 (basic diff), S9 (table), S10 (basic triage), S14 · §8.3 status colors · NFR-SEC-6 (text-only rendering) · PG1, PG2 · FR-WCH, FR-DEP, FR-IMP, FR-ORG-2.

## Ordered task checklist
Conventions: [frontend-conventions.md](../../guides/engineering/frontend-conventions.md). Regenerate `src/lib/api/schema.d.ts` first (`pnpm gen:api` against a running backend) and commit it.

### A. Shared building blocks
- [ ] `src/components/shared/StatusBadge.tsx`: one component for review status, confidence (UNCERTAIN dashed), change type, webhook state, source/watch status, with the §8.3 colors, tinted background, dot **and** text (color never the only signal).
- [ ] `src/components/shared/DataState.tsx`: loading skeleton, empty (CTA), error (message + `requestId` + Retry), forbidden (names the role), blocked (`SOURCE_FETCH_BLOCKED` reason), unsupported (JS-rendered hint) (§8.4 state coverage).
- [ ] `src/components/shared/CopyButton.tsx`, `RelativeTime.tsx` (date-fns, exact UTC tooltip), `RoleGate.tsx` (hide or disable with "Requires MAINTAINER" tooltip) (§8.4).
- [ ] `src/components/shared/DataTable.tsx`: TanStack Table v8 with server pagination (`page`, `size`, `totalItems`) and a sticky header (§8.2).

### B. Onboarding and settings
- [ ] S3 `src/features/onboarding/pages/OnboardingPage.tsx`: MAINTAINER with no org → stepper Create organization → Create project → Add first watch; REVIEWER with no org → "Ask a maintainer to add you (your email: …)". `RequireAuth` redirects here when `me.organizations` is empty.
- [ ] S14 `src/features/settings/pages/SettingsPage.tsx`: profile; organization rename/delete (owner; show 409 `ORG_NOT_EMPTY`); members list, add by email, remove (owner only) (FR-ORG-1, FR-ORG-2).
- [ ] Active organization and project held in `ui-store` (IDs only); the sidebar org switcher sets it.

### C. Overview and projects
- [ ] S4 `src/features/overview/pages/OverviewPage.tsx`: stat tiles (active watches, changes in 7 days, open impacts, failing sources) from project summaries (FR-PRJ-2) and the recent changes list. Charts are Phase 10.
- [ ] S5 `src/features/projects/pages/ProjectsPage.tsx` (cards or table: watch count, open impacts, last check) and `ProjectDetailPage.tsx` with tabs Watches · Dependencies · Changes · Impacts · Webhooks · API Keys · Settings. The Webhooks and API Keys tabs are added in Phase 8; don't render them or stub fake data now.
- [ ] Create/edit project dialog; archive with confirmation (FR-PRJ-1).

### D. Watches
- [ ] S6 `src/features/watches/components/NewWatchWizard.tsx`: step 1 URL with live preview (skeleton → kind, title, warnings); step 2 WHOLE_PAGE or section checkboxes (key, title, excerpt); step 3 interval (min from server error `INTERVAL_TOO_SHORT`) and notify; result shows `baselineStatus` inline (FR-WCH-1, FR-WCH-2). Opened from the top-bar **New watch** button and `n` later in Phase 10.
- [ ] S7 `src/features/watches/pages/WatchDetailPage.tsx`: status pill, schedule, next check, **Check now** (inline progress, then outcome; 429 shows retry time), monitor-run timeline (`/watches/{id}/runs`), versions list, current sections (`/watches/{id}/sections`) (FR-WCH-5, FR-WCH-6).
- [ ] Edit watch (mode, sections, interval, notify, pause/resume) and delete with confirmation showing the dependency count (FR-WCH-4).

### E. Changes and dependencies
- [ ] S8 `src/features/changes/pages/ChangesPage.tsx` (timeline) and `ChangeDetailPage.tsx`: changed-section list with change-type and match-state badges, diff viewer, metadata (URL, from/to version, detected at), **affected dependencies** panel.
- [ ] `src/features/changes/components/DiffViewer.tsx`: renders the backend `diff_ops` as text spans (unified mode now; split mode in Phase 10). Never `dangerouslySetInnerHTML` (NFR-SEC-6).
- [ ] S9 table view `src/features/dependencies/pages/DependenciesPage.tsx` plus create/edit dialog: pick watch, then section from `/watches/{id}/sections` or "Whole page"; object type, external ID, display name, description, metadata JSON (FR-DEP-1..3). Target: register a dependency in under 2 minutes (PG2).

### F. Impact Inbox (basic triage)
- [ ] S10 `src/features/impacts/pages/ImpactInboxPage.tsx`: filter chips (project, status, confidence, source, date) kept in `ui-store`; dense list newest first; row opens a right-hand `Sheet` with `?selected=<id>` (evidence excerpt, why flagged: dependency → section → change, review actions with note, history) (FR-IMP-3, FR-IMP-4, §8.4 detail sheet).
- [ ] Status change uses an optimistic update with rollback on error (§8.4); dismiss requires a note.

## Suggested PR breakdown
| # | Branch | Scope | Reviewers |
|---|---|---|---|
| 1 | `feature/p7-shared-ui-blocks` | A | Imrul, Lead |
| 2 | `feature/p7-onboarding-settings` | B | Imrul |
| 3 | `feature/p7-overview-projects` | C | Imrul, Lead |
| 4 | `feature/p7-watch-wizard-detail` | D | Imrul |
| 5 | `feature/p7-changes-dependencies` | E | Imrul, Lead |
| 6 | `feature/p7-impact-inbox` | F | Imrul |

## Tests to write
- No new U/I IDs (frontend unit tests are P2, §14). The CI gate is `pnpm lint`, `typecheck`, `build`.
- If a screen exposes a backend bug, fix it with a regression test in the relevant existing `*Test`/`*IT` class.

## Exit criteria
- [ ] As `maintainer@demo.local` in Compose: create org → project → watch (preview, baseline READY) in under 5 minutes (PG1).
- [ ] Register a section dependency in under 2 minutes (PG2).
- [ ] Admin switches the fixture to v2 (Swagger or SQL until the Phase 8 admin UI) → **Check now** shows CHANGED → change detail shows `customer_id → customer_reference`, footer not listed → Impact Inbox shows 2 NEW, none for Fitness.
- [ ] As `reviewer@demo.local`: no **New watch** button; can resolve an impact with a note; a forced API write returns 403.
- [ ] Every data view handles loading, empty, error and forbidden states.
- [ ] Frontend CI gate green.

## Evidence to capture
- Short screen recording of the core loop in the browser (input for the Phase 11 backup video).
- Screenshot of the Impact Inbox with the sheet open (candidate for the landing-page hero, §8.6).

## Risks and gotchas
- **Hidden ≠ secure.** Role-aware UI is convenience; the server is the authority (§8.4, §6.3). Always handle 403 gracefully.
- **Optimistic updates only for reversible actions** (impact status, notification read). Never for Check now, deliveries or monitoring (§8.4).
- **Fetched page text is untrusted** (NFR-SEC-6). Render as text nodes only; no markdown or HTML rendering of evidence.
- **Query keys.** Use each feature's key factory and invalidate the right keys after mutations (e.g. impact patch → impacts list + project summary).
- **No server data in Zustand.** Filters and selected IDs only.
- **Long synchronous requests.** Watch create and Check now can take up to ~15 s; show progress and disable the button, don't retry automatically.
