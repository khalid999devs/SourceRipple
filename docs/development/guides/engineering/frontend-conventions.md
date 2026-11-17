# Frontend conventions

Applies to `frontend/`. Stack and design come from PRD §8 (DEC-12, DEC-13); this guide fixes structure and rules.

## Folder structure
```
frontend/src/
├─ main.tsx                 mounts <Providers><RouterProvider/></Providers>
├─ app/                     router.tsx, providers.tsx, guards/ (RequireAuth, RequireRole), pages/ (NotFound, Forbidden, ServerError)
├─ styles/globals.css       Tailwind v4 @theme Midnight tokens
├─ marketing/               pages/ (LandingPage, PricingPage, PrivacyPage, TermsPage), components/, plans.ts
├─ features/<feature>/      api.ts (query keys + hooks), components/, pages/
│   auth, onboarding, overview, projects, watches, changes, dependencies, impacts, integrations, notifications, admin, settings
├─ components/              ui/ (shadcn copies), layout/ (AppShell, MarketingShell, Sidebar, TopBar), shared/ (StatusBadge, DataState, CopyButton, RelativeTime, DataTable, CommandPalette…)
├─ lib/                     api/ (client.ts, schema.d.ts generated), utils.ts (cn), format.ts
├─ stores/                  ui-store.ts, preferences-store.ts (Zustand)
└─ hooks/                   useShortcuts.ts, useMediaQuery.ts …
```
Structure rules:
- **At most 6 files directly in any directory.** Beyond that, group related files into sub-directories (sub-packages in Java). Exempt: the repository root and the `backend/` and `frontend/` tool-config roots.
- **No source file over 800 lines.** Split by responsibility instead. Exempt: generated files (e.g. `schema.d.ts`) and lockfiles.
- Reviewers reject PRs that break either rule.
- `components/ui/` therefore groups shadcn copies by kind: `ui/form/` (button, input, select, checkbox, label, textarea), `ui/overlay/` (dialog, sheet, popover, tooltip, dropdown-menu, command), `ui/display/` (badge, skeleton, table, tabs, card, separator), `ui/feedback/` (sonner, alert). After `pnpm dlx shadcn add <c>`, move the file into its group and fix the import. Feature `components/` and `pages/` folders split the same way when they grow.

Rules: features don't import from each other's `components/` (move shared pieces to `components/shared/`); `marketing/` never imports from `features/` or `components/layout/AppShell` (bundle split, NFR-PERF-3). File names: components `PascalCase.tsx`, hooks `useX.ts`, others `kebab-case.ts`.

## Two shells (PRD §8.4, FR-WEB-1)
| Shell | Routes | Loads |
|---|---|---|
| `MarketingShell` | `/`, `/pricing`, `/privacy`, `/terms` | Marketing chunk only |
| Auth split layout | `/login`, `/register` | Small auth chunk |
| `AppShell` | `/app/**` | Dashboard chunk (sidebar, top bar, features) |

Each shell is a `lazy` route in `src/app/router.tsx` (React Router v7, `createBrowserRouter`). Every feature page is also lazy. Unknown routes render `NotFoundPage`.

## Routing and role guards
- `RequireAuth` wraps `/app`: loads `useMe()`; 401 → `/login?next=<path>`; no organizations → `/app/onboarding` (S3).
- `RequireRole role="ADMIN"` wraps `/app/admin/**`; otherwise renders `ForbiddenPage` naming the role (S15).
- In-page actions use `RoleGate` (hide or disable with "Requires MAINTAINER") (§8.4). The server is always the authority (§6.3).
- Detail sheets keep state in the URL: `?selected=<id>` (§8.4).

## Server state vs UI state (PRD §8.2)
| Data | Where |
|---|---|
| Anything from the API (me, orgs, projects, watches, changes, impacts, notifications, deliveries) | **TanStack Query v5** |
| Theme, sidebar collapsed, active org/project IDs, inbox filters, diff view mode, command palette open | **Zustand** (`ui-store`; `preferences-store` with `persist`) |

Never copy server data into Zustand. Each feature exports a key factory from its `api.ts`:

```ts
export const impactKeys = {
  all: ['impacts'] as const,
  list: (projectId: string, filters: ImpactFilters) => [...impactKeys.all, 'list', projectId, filters] as const,
  detail: (id: string) => [...impactKeys.all, 'detail', id] as const,
};
```
Mutations invalidate the narrowest keys that changed. Optimistic updates only for reversible actions (notification read, impact status with rollback); never for checks, deliveries or monitoring (§8.4). Notifications poll every 60 s (`refetchInterval`) (FR-NTF-2).

## Generated API client (PRD §8.2)
- `pnpm gen:api` runs `openapi-typescript http://localhost:8080/v3/api-docs -o src/lib/api/schema.d.ts`. Commit the result in the same PR as the backend DTO change, so CI builds without a backend.
- `src/lib/api/client.ts` creates one `openapi-fetch` client on the same origin (spec paths already start with `/api/v1`) with middleware that: sends cookies (same-origin), adds `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie on non-GET requests, and turns non-2xx bodies into a typed `ApiError {error, message, requestId, retryable, fieldErrors}`.
- Errors surface as Sonner toasts showing `requestId` (§8.2); field errors map onto React Hook Form fields.
- Forms: React Hook Form + Zod schemas mirroring backend validation (password ≥ 10, URL, metadata ≤ 4 KB).

## Midnight tokens (PRD §8.3)
- Defined once in `src/styles/globals.css` with Tailwind v4 `@theme` (`--color-background`, `--color-surface-1`, `--color-surface-2`, `--color-border`, `--color-foreground`, `--color-muted-foreground`, `--color-primary` Ripple Cyan, `--color-accent` Ripple Violet, `--radius`), values from §8.3. Dark is the default and only required theme; light theme is P2.
- Status colors come from one map used by `StatusBadge` (tinted 15% background, colored text, dot, plus text label). UNCERTAIN uses a dashed outline.
- Typography: Geist Sans 14 px (13 px in dense tables, `tabular-nums`), Geist Mono for URLs, IDs, section keys, diffs, JSON. Fonts self-hosted.
- Elevation by borders and surface steps; glassy top bar; glow and dot grid only on landing, auth and empty states.
- Motion: 150 ms hover, 200–250 ms sheets, 4 px row entrance, pulse for running/retrying; all off under `prefers-reduced-motion`.
- Every text/background pair must pass WCAG 2.1 AA; check when tuning tokens.

## Hatchet-inspired patterns (PRD §8.1)
Take the patterns, not the brand: sidebar shell with workspace switcher, dense tables with status badges, right-hand detail sheet, DAG view (S9), command palette (⌘K), relative time with exact UTC tooltip, copy buttons on IDs, calm dark surfaces. Never copy Hatchet's name, logo, screenshots, illustrations or copy. If an MIT Hatchet source file is copied, keep its notice and list it in `THIRD_PARTY_NOTICES.md`.

## Component and library rules
- Only libraries in §8.2. Adding anything else needs a PRD change.
- Licenses compatible with Apache-2.0 only (MIT, ISC, Apache-2.0, BSD, SIL OFL for fonts). No paid kits.
- shadcn/ui components are copied into `components/ui/` and may be edited; Magic UI pieces are copied into `marketing/components/` (or `components/shared/` for app accents). Record each source in `THIRD_PARTY_NOTICES.md` in the same PR.
- Magic UI in the app only on auth pages, empty states and overview tiles; never in data tables.
- Fetched page text renders as text only. No `dangerouslySetInnerHTML` for fetched content (NFR-SEC-6).
- CSP (NFR-SEC-8): `script-src 'self' 'wasm-unsafe-eval'` (Shiki), `style-src 'self' 'unsafe-inline'` (Radix, Sonner, Motion), `img-src 'self' data:`, `connect-src 'self' https://api.github.com`. No inline `<script>` in `index.html`; no other external origins.
- Shiki is lazy-loaded.

## Marketing site
- Plans, prices and CTA states live only in `src/marketing/plans.ts` (FR-WEB-4).
- Honest copy (FR-WEB-2): no AI-powered claims, fake logos, testimonials, counts, uptime or "most popular". *Coming soon* for planned features; every claim true on the day it ships.
- GitHub stars fetched from the browser and cached 1 h in `sessionStorage` (FR-WEB-8).
- Per-route `<title>`, description, canonical, Open Graph and Twitter tags (React 19 hoists `<title>`/`<meta>` rendered in components); `public/og.png`, favicon set, `theme-color`, `robots.txt`, `sitemap.xml` (FR-WEB-9).
- No analytics, no tracking cookies (FR-WEB-11).

## Accessibility (NFR-A11Y-1, §8.5)
- Radix primitives for focus management; visible `--primary` focus ring; labels on icon-only buttons; `aria-live` toasts; color never the only signal.
- Keyboard-only operation of S2, S6, S8, S10. Shortcuts (§8.4) never fire inside inputs or open dialogs.
- Responsive: desktop-first; tablet collapses the sidebar; mobile shows cards, bottom drawer and unified diff.

## Performance budgets (PG7, NFR-PERF-3)
| Budget | Target | How checked |
|---|---|---|
| Lighthouse (mobile) on `/` and `/pricing` | ≥ 90 in all four categories | Chrome Lighthouse against `pnpm build && pnpm preview` or the Compose app |
| LCP | < 2.5 s | Lighthouse |
| Initial JS for `/` | ≤ 200 KB gzip | build output / `vite build --mode production` report |
| Layout shift | none | Lighthouse CLS |
Screenshots as WebP/AVIF with explicit width/height; animations below the fold lazy-loaded.

## Comments
The code is reviewed by the course teacher; keep it free of commentary noise.
- Only short comments explaining a non-obvious *why*. No tutorial or explanatory comments, no JSDoc restating types, no TODO essays.
- No phase numbers, PRD IDs or test IDs in source files. Traceability lives in PR descriptions and `docs/development/`.
- No commented-out code or JSX, no dead components.

## Quality gate
`pnpm lint` (ESLint typescript-eslint), `pnpm typecheck` (`tsc --noEmit`), `pnpm build`, Prettier formatting. All three run in CI (`frontend` job). Vitest and Playwright are P2.
