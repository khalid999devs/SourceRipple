# Phase 6 · Change detection, dependencies and impacts

## Goal
A content change produces exactly one change event with durable section evidence. Registered dependencies on the changed sections (or whole page) produce impacts in the same transaction, and impacts can be reviewed. The golden flow of Appendix A passes in CI. This is the core-loop milestone.

## People
- **Owner:** Ajoy. Reviewers alternate Imrul / Lead.
- **Pairing:** Lead pairs with Ajoy on I9 (`GoldenFlowIT`) and adds the Appendix A dependencies to the demo seeder.

## Prerequisites
- Phase 5 exit criteria met (watches, baseline, checks, demo kit with fixture switch).

## PRD references
FR-EXT-6..9 (builds on Phase 5's FR-EXT-5) · FR-DEP-1..3 · FR-IMP-1..5 · §9.1 tables 11–14, transaction rule · §9.3 impact review · §10.3 Change, Dependency, Impact controllers · NFR-REL-1 · Appendix A · §3 scenarios A and B.

## Ordered task checklist

### A. Schema
- [ ] `monitoring/V6__changes_dependencies_impacts.sql`: `change_events` (unique `fingerprint`; `from_version_id`/`to_version_id` nullable `ON DELETE SET NULL`), `change_event_sections` (jsonb `diff_ops`), `dependencies` (jsonb `metadata`; unique index on `(watch_id, coalesce(section_key,''), external_id)`), `impacts` (unique `(change_event_id, dependency_id)`); indexes `change_events(source_id, occurred_at DESC)`, `dependencies(watch_id, section_key)`, `impacts(project_id, review_status, created_at DESC)` (§9.1).
- [ ] Entities: `diff/domain/ChangeEvent.java`, `ChangeEventSection.java`, enums `ChangeType` (ADDED, REMOVED, MODIFIED), `MatchState` (MATCHED, UNCERTAIN, MISSING); `impact/domain/Dependency.java`, `ObjectType`, `Impact.java`, `MatchConfidence`, `ReviewStatus`.

### B. Section diff and change events (FR-EXT-5..7)
- [ ] `diff/application/SectionMatcher.java`: match by key; fallback heading similarity ≥ 0.8 **and** content-fingerprint check → `UNCERTAIN`; still unmatched → `ADDED` / `REMOVED` (`MISSING`) (FR-EXT-6).
- [ ] `diff/application/WordDiffer.java`: java-diff-utils word-level diff → list of `{op, text}` stored as `diff_ops`.
- [ ] `diff/application/SectionDiffService.java`: compares two section lists → changed sections with change type and match state.
- [ ] Extend `ChangeDetectionService`: new hash → new version + sections → diff → one `ChangeEvent` with `fingerprint = SHA-256(sourceId|fromHash|toHash)` and `change_event_sections` (old text, new text, ops). A duplicate fingerprint is a no-op, never a second event (FR-EXT-7, NFR-REL-1).
- [ ] FR-EXT-9: < 200 chars of text and a script-mounted root → watch status `UNSUPPORTED`, reason `LIKELY_JS_RENDERED`.

### C. Dependencies (FR-DEP)
- [ ] `impact/application/DependencyService.java`: create/get/list/update/delete (MAINTAINER for writes); `sectionKey=null` = whole page, else must exist in the latest version → 422 `SECTION_NOT_FOUND`; duplicate → 409; `metadata` ≤ 4 KB.
- [ ] `impact/api/DependencyController.java`: `POST /projects/{projectId}/dependencies` (body has `watchId`), list, get, `PUT`, delete (§10.3). The watch must belong to that project.
- [ ] Watch delete (Phase 5) now cascades to dependencies (FR-WCH-4); the UI shows the count in Phase 7.

### D. Impacts (FR-IMP)
- [ ] `impact/application/ImpactService.java` `createImpacts(changeEvent)`: for every active dependency on any active watch of that source in non-archived projects, apply the FR-IMP-1 table (whole page + any change → MATCHED; section MODIFIED or confirmed REMOVED → MATCHED; UNCERTAIN/MISSING → UNCERTAIN; else none); one impact per (event, dependency), status `NEW` (FR-IMP-2).
- [ ] Call `ImpactService.createImpacts` from `monitor/application/CheckOutcomeService` **inside the same transaction** as the version and event (§9.1 transactions). Phase 8 (Lead) extends this transaction with notification and outbox rows.
- [ ] `impact/application/ImpactReviewService.java`: `@PreAuthorize("hasAnyRole('MAINTAINER','REVIEWER')")`; transitions per §9.3; dismiss requires a note; records reviewer and timestamp; invalid → 409 `INVALID_TRANSITION` (FR-IMP-3).
- [ ] `impact/application/ImpactQueryService.java` + `impact/api/ImpactController.java`: `GET /projects/{id}/impacts` filtered by status, confidence, source, date range, newest first, paginated; `GET /impacts/{id}`; `PATCH /impacts/{id}` (FR-IMP-4).
- [ ] `diff/api/ChangeController.java`: `GET /projects/{projectId}/changes`, `GET /changes/{changeId}` (evidence: sections, ops, metadata, affected dependencies) (§10.3, S8 data).
- [ ] Resolvers for `AccessPolicy`: `ChangeOrganizationResolver`, `DependencyOrganizationResolver`, `ImpactOrganizationResolver`; summary contributor for impacts by status (FR-PRJ-2).

### E. Retention (FR-EXT-8)
- [ ] `monitor/application/RetentionService.java`: keep the last 10 versions per source (configurable); pruning relies on `ON DELETE SET NULL` and never deletes `change_event_sections`. Run it inside the loop/tick budget, never after a response.

### F. Demo seeder and golden flow (Lead with Ajoy)
- [ ] `DemoSeeder`: add Appendix A dependencies: E-commerce `SERVICE payment-service @ payments/parameters`; Mobile Checkout `CODE_COMPONENT checkout-module @ payments/parameters` on a WHOLE_PAGE watch; Fitness App `SERVICE workout-planner` (whole page on nutrition).
- [ ] Confirm the section key the HTML adapter produces for the fixture's Parameters heading is exactly `payments/parameters` (give the heading an `id` in the fixture if needed).
- [ ] `GoldenFlowIT` (I9) per [testing.md](../../guides/engineering/testing.md#golden-flow-it).

## Suggested PR breakdown
| # | Branch | Author | Scope | Reviewer |
|---|---|---|---|---|
| 1 | `feature/p6-changes-schema` | Ajoy | A | Lead |
| 2 | `feature/p6-section-diff` | Ajoy | B, E, U19, U20 | Imrul |
| 3 | `feature/p6-dependencies` | Ajoy | C, I10 | Lead |
| 4 | `feature/p6-impacts` | Ajoy | D, U21–U24, I6, I11 | Imrul |
| 5 | `feature/p6-golden-flow` | Lead (pairing with Ajoy; each commits their own work) | F, I9 | Imrul |

## Tests to write
| ID | Class | Intent |
|---|---|---|
| U19 | `diff/application/SectionDiffServiceTest` | v1 → v2 payments fixture: only `payments/parameters` is MODIFIED |
| U20 | `SectionDiffServiceTest` | Renamed heading with similar body → `UNCERTAIN`, never MATCHED |
| U21 | `impact/application/ImpactServiceTest` | Two projects depend on the changed section → 2 impacts; unrelated section dependency → none |
| U22 | `ImpactServiceTest` | Whole-page dependency flagged by any section change |
| U23 | `ImpactServiceTest` | UNCERTAIN or MISSING section → confidence `UNCERTAIN` |
| U24 | `impact/application/ImpactReviewServiceTest` | Valid transitions pass; `RESOLVED → NEW` → `INVALID_TRANSITION`; dismiss without note rejected |
| I9 | `monitor/api/GoldenFlowIT` | Fixture v2 → manual check → 1 change event, 2 impacts (MATCHED, NEW), 0 for Fitness; re-check → no new version |
| I10 | `impact/api/DependencyCrudIT` | Create/get/put/delete codes; unknown section → 422 `SECTION_NOT_FOUND` |
| I11 | `impact/api/ImpactReviewIT` | REVIEWER resolves with a note → 200; invalid transition → 409 |
| I6 | `identity/api/TenantIsolationIT` | A user from another organization requesting a project, watch or impact → 404 |

## Exit criteria
- [ ] **I9 golden flow green in CI** for the Appendix A version, change and impact results (webhook and MCP results are verified in Phase 8), plus U19–U24, I6, I10, I11.
- [ ] Footer-only change creates no section change (PG4); identical re-fetch creates no event (NFR-REL-1).
- [ ] Evidence survives pruning: after more than 10 versions, `GET /changes/{id}` still returns old and new text.

## Evidence to capture
- CI run link showing `GoldenFlowIT` passing (core loop proof, §19.2 "Integration tests").
- Keep this branch's PR as the planned spot for the failing-then-fixed demo in Phase 10 (TST-R7: "a broken impact assertion").

## Risks and gotchas
- **One transaction**: version + sections → event + evidence → impacts (→ Phase 8 notifications and outbox). If any step fails, nothing is stored and the run is recorded as failed.
- **The fixture is fetched over HTTP**, so I9 needs a real listening port. MockMvc alone has none. Use the `DEFINED_PORT` recipe in [testing.md](../../guides/engineering/testing.md#golden-flow-it) and set `sourceripple.public-base-url` to it.
- **Shared source, fetched once** (FR-MON-1). Both payments watches point to one source; assert a single version and a single run.
- **Sections-mode filter** decides notification (FR-IMP-5, Phase 8), not impact creation; impacts follow dependencies only.
- **Appendix A's webhook and MCP results** are verified in Phase 8 (§21.2); Phase 8 extends I9 with the DELIVERED assertion.
- **Unique index with `coalesce`** must be an expression index; JPA can't express it, so it lives only in the migration.
- **Similarity threshold 0.8** is a ratio on normalized heading text; pick one metric (e.g. normalized Levenshtein) and state it in the PR description.
