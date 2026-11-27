# Phase 3 · Workspace and API foundation (+ frontend foundation)

## Goal
Organizations, members and projects have full CRUD behind the access rules. Every error leaves the API in one envelope with a request ID, and OpenAPI is served. In parallel, the React SPA has its real foundation: tokens, both shells, routing with guards, the generated API client, and working login, register and logout.

## People
- **Owner (backend):** Ajoy. Reviewers alternate Imrul / Lead.
- **Parallel (frontend foundation):** Lead. Reviewer: Imrul.

## Prerequisites
- Phase 2 exit criteria met (auth, sessions, CSRF, `AccessPolicy`, I1–I3 and I5 green).

## PRD references
FR-ORG-1..3 · FR-PRJ-1..2 · R5, R6 · FR-API-1..4 · §10.1–§10.3 (Organization, Member, Project controllers) · §8.1–§8.4 · §8.5 S2, S15 · DEC-12, DEC-13 · NFR-SEC-6, NFR-SEC-8 · FR-WEB-1 (route split only).

## Ordered task checklist — backend (Ajoy)

### A. API foundation (FR-API)
- [ ] `shared/infrastructure/RequestIdFilter.java`: read `X-Request-Id` or generate `req_<random>`, put it in MDC as `requestId`, echo it in the response header (FR-API-3).
- [ ] Complete `shared/api/GlobalExceptionHandler.java`: every exception → §10.2 envelope with the status from §10.1; `requestId` from MDC; `retryable` flag; unknown errors → 500 `INTERNAL_ERROR`, logged with stack trace, never returned (FR-API-1).
- [ ] `shared/api/PageResponse.java` `{items, page, size, totalItems, totalPages}`; `size` max 100 (§10.1).
- [ ] springdoc: `/v3/api-docs` and `/swagger-ui.html` public; group `/api/v1/**`; security schemes for session cookie and (later) bearer key (FR-API-4).
- [ ] `shared/infrastructure/SpaForwardingConfig.java`: forward every non-API, non-file path to `/index.html` (§10.3 "outside the REST API"). Exclude `/api`, `/v3`, `/actuator`, `/internal`, `/mcp`, `/demo`, `/swagger-ui`.

### B. Organizations and members (FR-ORG)
- [ ] `workspace/application/OrganizationService.java`: create (`@PreAuthorize("hasRole('MAINTAINER')")`, creator = owner + member, quota 5 → `QUOTA_EXCEEDED` 409), list mine, get, rename (owner), delete (owner, only if every project is archived, else 409 `ORG_NOT_EMPTY`) (FR-ORG-1, FR-ORG-3, R6).
- [ ] `workspace/application/MembershipService.java`: list (member), add by email (owner; 404 if no account; 201), remove (owner; 204; owner cannot be removed → 409) (FR-ORG-2, R5).
- [ ] `workspace/api/OrganizationController.java`, `workspace/api/MemberController.java` with DTOs in `workspace/api/dto/` (§10.3).
- [ ] `AuthController` `/me` now returns organizations (FR-AUTH-4).

### C. Projects (FR-PRJ)
- [ ] `workspace/application/ProjectService.java`: create (MAINTAINER, slug from name, unique within the org with `-2`, `-3`), list per org, get, update name/description, archive (sets `archived_at`, calls `ProjectArchiveListener.onArchived(projectId)`) (FR-PRJ-1).
- [ ] `workspace/application/ProjectArchiveListener.java` port; Phase 5 implements it in `source` to pause watches (see [architecture.md](../../guides/engineering/architecture.md#cross-module-calls)).
- [ ] `GET /projects/{id}/summary` (FR-PRJ-2) via `ProjectSummaryContributor` port; returns zeros until Phase 5 (watches, failing sources, last check) and Phase 6 (impacts by status) add contributors.
- [ ] `workspace/api/ProjectController.java` (§10.3); 201 with `Location`; archive → 204.

## Ordered task checklist — frontend foundation (Lead)
Follow [frontend-conventions.md](../../guides/engineering/frontend-conventions.md).

- [ ] Tailwind CSS v4 with `@theme` Midnight tokens in `src/styles/globals.css` (§8.3), dark by default (`class="dark"` on `<html>` in `index.html`, no inline script).
- [ ] Geist Sans and Geist Mono self-hosted (OFL) under `public/fonts/` or bundled; no font CDN (§8.2).
- [ ] shadcn/ui initialized (`components.json`), primitives copied into `src/components/ui/` (button, input, dialog, sheet, dropdown-menu, tabs, tooltip, popover, badge, skeleton, sonner).
- [ ] `src/components/layout/MarketingShell.tsx` (top nav + footer) and `AppShell.tsx` with `Sidebar.tsx` (240/64 px, nav from §8.4, Admin only for ADMIN) and `TopBar.tsx` (breadcrumbs, project switcher placeholder, ⌘K field placeholder, **New watch** for MAINTAINER only) (§8.4).
- [ ] `src/app/router.tsx`: marketing routes `/`, `/pricing`, `/privacy`, `/terms` (placeholders), `/login`, `/register`, app under `/app/**`, 404 catch-all; each shell lazy-loaded (FR-WEB-1, §8.4).
- [ ] `src/app/guards/RequireAuth.tsx` (redirect to `/login?next=`) and `RequireRole.tsx` (renders the 403 page naming the role) (§8.4 role-aware UI).
- [ ] `src/app/providers.tsx`: `QueryClientProvider`, Sonner `Toaster` (shows `requestId` on errors), Radix `TooltipProvider`.
- [ ] `src/lib/api/client.ts`: `openapi-fetch` client typed by generated `src/lib/api/schema.d.ts`; middleware adds `X-XSRF-TOKEN` from the cookie on non-GET; normalizes the §10.2 envelope into an `ApiError` (§8.2).
- [ ] `src/stores/ui-store.ts` and `preferences-store.ts` (Zustand, `persist` for preferences) (§8.2).
- [ ] `src/features/auth/`: `api.ts` (query keys + `useMe`, `useLogin`, `useRegister`, `useLogout`), `pages/LoginPage.tsx`, `pages/RegisterPage.tsx` with split layout and the two role choice cards (S2); RHF + Zod mirroring backend rules (password ≥ 10).
- [ ] System pages S15: `src/app/pages/NotFoundPage.tsx`, `ForbiddenPage.tsx`, `ServerErrorPage.tsx` (shows `requestId`), `src/components/shared/OfflineBanner.tsx`.
- [ ] `THIRD_PARTY_NOTICES.md`: shadcn/ui, Radix, Geist, lucide and each library added (§8.1 rule).

## Suggested PR breakdown
| # | Branch | Author | Scope | Reviewer |
|---|---|---|---|---|
| 1 | `feature/p3-api-foundation` | Ajoy | A, I15 | Imrul |
| 2 | `feature/p3-organizations` | Ajoy | B, U4, U5 | Lead |
| 3 | `feature/p3-projects` | Ajoy | C, U6, I7 | Imrul |
| 4 | `feature/p3-web-tokens-shells` | Lead | tokens, fonts, shadcn, shells, router, guards, S15 | Imrul |
| 5 | `feature/p3-web-auth` | Lead | generated client, CSRF middleware, providers, stores, S2 | Imrul |

## Tests to write
| ID | Class | Intent |
|---|---|---|
| U4 | `workspace/application/OrganizationServiceTest` | Create sets owner and adds creator to members |
| U5 | `workspace/application/MembershipServiceTest` | Only the owner adds/removes; owner removal rejected |
| U6 | `workspace/application/ProjectServiceTest` | Slug `shop`, then `shop-2` within the same org |
| I7 | `workspace/api/ProjectCrudIT` | 201 + Location → 200 get → 200 patch → 204 archive → 404 for a random ID |
| I15 | `shared/api/ErrorEnvelopeIT` | 400, 404 and 409 bodies have `error`, `message`, `requestId`, `retryable`; `X-Request-Id` header matches |

## Exit criteria
- [ ] U4–U6, I7, I15 green (U7 follows in Phase 5 once watches exist).
- [ ] Swagger UI lists Auth, Organization, Member and Project controllers.
- [ ] In the browser (Vite dev against the backend, and inside the Compose image): register → login → land in `/app` → logout → back on `/login`.
- [ ] Refreshing `/app/anything` serves the SPA (fallback works); unknown routes show the 404 page.
- [ ] `pnpm lint`, `pnpm typecheck`, `pnpm build` green; the marketing chunk does not include `AppShell` (check the build output).

## Evidence to capture
- Swagger UI screenshot (§19.1 controllers row).
- Screenshot of login and register pages (dark Midnight theme).

## Risks and gotchas
- **Upward dependencies.** `workspace` must not import `source` or `impact`. Use the `ProjectArchiveListener` and `ProjectSummaryContributor` ports.
- **Slug race.** The unique constraint is the real guard; catch the violation and retry with the next suffix.
- **Org delete 409** must check archived state in the same transaction as the delete.
- **Generated client drift.** Regenerate `schema.d.ts` whenever a DTO changes and commit it in the same PR; CI builds the frontend without a running backend.
- **CSP (NFR-SEC-8) forbids inline scripts** (`script-src 'self' 'wasm-unsafe-eval'`). No theme-bootstrap `<script>` in `index.html`; set `class="dark"` statically.
- **No server data in Zustand** (§8.2). `me`, orgs and projects live in TanStack Query.
- **Cookies through the Vite proxy** work because the browser sees one origin (`localhost:5173`). Don't enable CORS on the backend.
