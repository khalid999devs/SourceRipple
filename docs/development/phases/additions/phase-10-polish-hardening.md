# Phase 10 · Polish and hardening

## Goal
Every P0 and P1 item except deployment is done and polished. The UI reaches the §8 quality bar, test counts and coverage are audited, a security review is complete, the failing-then-fixed CI evidence exists, the README is written (minus deployment) and the six pattern diagrams are drawn.

## People
- **Lead:** app polish, test audit, security review, failing-then-fixed CI evidence, README (§20.1 except item 10), demo script. Reviewer: Imrul.
- **Ajoy:** core bug fixes, pattern class diagrams and call paths. Reviewers: Lead / Imrul.

## Prerequisites
- Phases 8 and 9 exit criteria met.

## PRD references
§8.2–§8.5 (polish items, keyboard, responsive, accessibility) · §8.5 S4 charts, S9 graph · NFR-A11Y-1, NFR-COMP-1, NFR-MAINT-1, NFR-PERF-1/2 · §11.5 (diagrams, call paths, tests) · §12 (review checklist) · §14 TST-R1..R7 · §19 · §20.1, §20.2 · §21.5.

## Ordered task checklist — app polish (Lead)
- [ ] Command palette (cmdk) `src/components/shared/CommandPalette.tsx`, ⌘K / Ctrl-K: jump to project, watch, change; "New watch", "Open Impact Inbox", "Check now on …"; switch organization (§8.4).
- [ ] Keyboard shortcuts `src/hooks/useShortcuts.ts`: `g o`, `g p`, `g i`, `n`, `j`/`k`, `e`, `r`, `d` (asks for note), `?` shortcut sheet; disabled while typing in inputs (§8.4).
- [ ] Diff viewer split and unified modes with word highlights; mode saved in `preferences-store`; unified forced on mobile (S8, §8.5).
- [ ] S9 Dependency Map graph: `@xyflow/react` + dagre, Source → Section → Dependency → Project, nodes colored by open-impact status, click opens the sheet, toggle to table (P1; third in the cut order).
- [ ] S4 charts (Recharts via shadcn chart wrappers): sparklines, impacts-by-status, check activity (P1; fourth in the cut order).
- [ ] Motion: 150 ms hover/press, 200–250 ms sheets, 4 px row entrance, skeleton shimmer, pulse on running/retrying, number tickers on overview tiles; all disabled under `prefers-reduced-motion` (§8.3).
- [ ] Magic UI accents only on auth pages, empty states and overview tiles; never in data tables (§8.2).
- [ ] Responsive: tablet collapses the sidebar; mobile tables become cards and the sheet a bottom drawer (§8.5).
- [ ] Accessibility pass on S2, S6, S8, S10: keyboard-only flow, visible focus ring, labels on icon buttons, `aria-live` toasts, contrast AA for every token pair (NFR-A11Y-1, §8.3).
- [ ] State coverage audit: every data view has loading, empty, error (with `requestId`), forbidden, blocked, uncertain and unsupported states where applicable (§8.4).
- [ ] Payload viewer in S11 uses Shiki (lazy) (§8.2).

## Ordered task checklist — quality and docs (Lead)
- [ ] Test audit: every U1–U31 and I1–I17 present, named per [testing.md](../../guides/engineering/testing.md), mapped in [PHASE_PLAN.md](../../PHASE_PLAN.md#test-id-ownership-tracker-prd-14); CI summary shows ≥ 31 unit IDs and 17 IT IDs (§21.2); I4 and I6 have every case; JaCoCo ≥ 70% lines for `monitor`, `diff`, `impact`, `notify`, `identity` services (TST-R6, soft target).
- [ ] Every endpoint in §10.3 has a role or membership test (NFR-SEC-3); add cases to `RoleAccessIT` / `TenantIsolationIT` where missing.
- [ ] Security review against §12 (record results in the PR):
  - [ ] NFR-SEC-1: IP literals, redirects re-validated, DNS guard, interceptor, caps, prod refuses `allow-private-targets`.
  - [ ] NFR-SEC-2: BCrypt strength, session rotation, cookie flags in prod, CSRF on every cookie write.
  - [ ] NFR-SEC-4: no secrets in code, Compose, fixtures, CI logs; log masking works (grep logs after a golden flow).
  - [ ] NFR-SEC-5/6: signatures constant-time; no `dangerouslySetInnerHTML` on fetched text (`grep -r dangerouslySetInnerHTML frontend/src`).
  - [ ] NFR-SEC-7: quotas, 200-source cap, every rate limit (login 10/15 min per IP and email → `RATE_LIMITED`), 4 KB public bodies.
  - [ ] NFR-SEC-8: CSP exactly `default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' https://api.github.com; frame-ancestors 'none'`, plus `X-Content-Type-Options` and `Referrer-Policy`; no inline scripts; CORS off.
  - [ ] NFR-SEC-9: secret scanning, Dependabot, dependency review enabled.
- [ ] Failing-then-fixed evidence (TST-R7) per [testing.md](../../guides/engineering/testing.md#failing-then-fixed-ci-evidence).
- [ ] `README.md` sections 1–9, 11, 12 of §20.1 (architecture and ER Mermaid from §11.1/§9.2, endpoint table from §10.3, Swagger link, patterns table linking `docs/diagrams/`, run steps, testing counts and report locations, CI/CD pipeline diagram, observability, team, license, notices). Section 10 (deployment) is Phase 11.
- [ ] Demo script rehearsal notes for §20.2 (owner per segment) in `docs/diagrams/demo-script.md`.
- [ ] `THIRD_PARTY_NOTICES.md` complete for every copied UI source and font (§8.1).

## Ordered task checklist — core fixes and diagrams (Ajoy)
- [ ] Triage and fix core bugs reported in Phases 8–9, each with a regression test.
- [ ] Six pattern diagrams in `docs/diagrams/` (Mermaid `classDiagram`), each with a call path and the test that proves it (§11.5):
  `pattern-adapter.md` (U15, U16), `pattern-factory.md` (U14), `pattern-proxy.md` (U12, U13), `pattern-bridge.md` (U27), `pattern-flyweight.md` (U28), `pattern-singleton.md` (U12/U13; explains Spring singleton beans vs GoF Singleton).
- [ ] Clean-code pass (NFR-MAINT-1): no dead code or commented-out blocks, no tutorial comments or PRD/phase notes in code, only short *why* comments, layering rules hold (§11.2).

## Suggested PR breakdown
| # | Branch | Author | Reviewer |
|---|---|---|---|
| 1 | `feature/p10-command-palette-shortcuts` | Lead | Imrul |
| 2 | `feature/p10-diff-modes-responsive` | Lead | Ajoy |
| 3 | `feature/p10-dependency-graph-charts` | Lead | Imrul |
| 4 | `feature/p10-motion-a11y` | Imrul | Lead |
| 5 | `feature/p10-test-audit` | Lead | Ajoy |
| 6 | `fix/p10-security-review` | Lead | Imrul |
| 7 | `docs/p10-readme` | Lead | Imrul, Ajoy |
| 8 | `docs/p10-pattern-diagrams` | Ajoy | Lead |
| 9 | `fix/p10-<bug>` (one per bug) | Ajoy | Lead / Imrul |

## Tests to write
- Fill any audit gaps; no new IDs. Each bug fix carries a regression test in an existing class.

## Exit criteria
- [ ] All P0 and P1 items in §5 done except deployment (Render, Cloud Run), or cut per §21.5 with the cut recorded.
- [ ] CI green on `develop`; summary shows ≥ 31 unit and 17 integration test IDs.
- [ ] Failing-then-fixed run links captured.
- [ ] Six diagrams merged; README complete except deployment.
- [ ] Lighthouse ≥ 90 still holds after polish (PG7).

## Evidence to capture
- CI summary screenshot with unit and IT counts (§19.2 "Tests run in CI").
- The red run and the green fixing run links (TST-R7, §20.2 9:00–11:00).
- Pattern diagram slide images (§20.2 8:00–9:00).

## Risks and gotchas
- **Polish is cuttable, the core is not** (§21.5). Cut graph and charts before touching tests or RBAC.
- **Shortcuts must not fire in text fields** or while a dialog is open.
- **Motion and Magic UI** inflate bundles; keep them lazy and out of the marketing initial chunk (NFR-PERF-3).
- **The failing run must happen on a PR**, never by pushing to a protected branch.
- **Diagrams must match code.** Generate class names from the actual source; reviewers check each name exists.
