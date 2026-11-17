# Backend conventions

Applies to everything under `backend/`. Rules cite the PRD; module placement is in [architecture.md](architecture.md).

## Naming (NFR-MAINT-1)
| Kind | Pattern | Example |
|---|---|---|
| Controller | `<Resource>Controller` in `<module>/api` | `WatchController` |
| Request / response DTO | `<Action><Resource>Request`, `<Resource>Response` (Java `record`) in `<module>/api/dto` | `CreateWatchRequest`, `WatchResponse` |
| Service | `<Concept>Service` in `<module>/application` | `ImpactReviewService` |
| Port | Noun describing the capability, in the lower module's `application` | `BaselineRunner` |
| Entity | Singular noun in `<module>/domain` | `ChangeEvent` |
| Repository | `<Entity>Repository` (Spring Data) | `WatchRepository` |
| Enum | Singular, values in `UPPER_SNAKE` matching the PRD | `ReviewStatus.INVESTIGATING` |
| Exception | `<Meaning>Exception` extending `shared.api.ApiException` | `SourceFetchBlockedException` |
| Unit test / IT | `<ClassUnderTest>Test` / `<Feature>IT` | `SafeFetchProxyTest`, `GoldenFlowIT` |

Standard Java naming; no abbreviations except `Id`, `Url`, `Http`, `Api`.

## Code structure
- **At most 6 files directly in any directory.** Beyond that, group related files into sub-directories (sub-packages in Java). Exempt: the repository root and the `backend/` and `frontend/` tool-config roots.
- **No source file over 800 lines.** Split by responsibility instead. Exempt: generated files (e.g. `schema.d.ts`) and lockfiles.
- Reviewers reject PRs that break either rule.
- In Java, a layer package that would pass 6 files is split by concept, e.g. `source/application/fetch/` and `source/application/watch/`, `shared/api/error/` for the exception classes. Paths in the phase files name the module and layer; choose the sub-package when the sixth file would be exceeded.

## Comments (NFR-MAINT-1)
The code is reviewed by the course teacher; it must read cleanly without commentary.
- Only short comments that explain a non-obvious *why* (e.g. why OkHttp redirects are disabled). Names and structure explain the *what*.
- No tutorial or explanatory comments, no Javadoc that restates the signature, no TODO essays.
- No phase numbers, PRD IDs or test IDs in source files or comments. Traceability lives in PR descriptions and `docs/development/`.
- No commented-out code and no dead code.

## DTOs and entities
- Entities never leave the service layer (§11.2). Map with small static `from(...)` methods on the response record or a module-local mapper class; no mapping libraries.
- JSON is camelCase, IDs are UUID strings, timestamps ISO-8601 UTC (`Instant`) (§10.1).
- UUID primary keys generated in Java (`UUID.randomUUID()` or `@UuidGenerator`); `timestamptz` columns map to `Instant` (§9).
- Enums stored as `varchar` with `@Enumerated(EnumType.STRING)` and a DB check constraint.
- `jsonb` columns use `@JdbcTypeCode(SqlTypes.JSON)` (§9).
- `@ManyToMany` only for users ↔ roles and organizations ↔ users (§9.1); everything else is `@ManyToOne` with lazy fetch. Watches are an association entity, not a join table.
- Paginated responses use `shared/api/PageResponse<T>`; `size` ≤ 100 (§10.1).

## Validation (FR-API-2)
- Bean Validation on every request DTO (`@NotBlank`, `@Email`, `@Size`, `@Pattern`); controllers take `@Valid @RequestBody`.
- Semantic checks (quota, section exists, URL allowed) live in services and throw the mapped exception.
- Field errors return 400 `VALIDATION_FAILED` with `fieldErrors[{field, message}]`.

## Errors (PRD §10.1, §10.2, FR-API-1)
All errors leave through `shared/api/GlobalExceptionHandler` as `{error, message, requestId, retryable, fieldErrors}`. Never return stack traces.

| Error code | Status | Exception (in `shared.api` unless noted) | Raised by |
|---|---|---|---|
| `VALIDATION_FAILED` | 400 | `MethodArgumentNotValidException`, `ConstraintViolationException` | framework |
| `ROLE_NOT_ALLOWED` | 400 | `BadRequestException(ROLE_NOT_ALLOWED)` | `AuthService` (R2) |
| `UNAUTHENTICATED` | 401 | written by `JsonAuthenticationEntryPoint` | security chain |
| `ACCOUNT_DISABLED` | 401 | `DisabledException` → handler | login (FR-AUTH-2) |
| `FORBIDDEN` | 403 | `ForbiddenException`, `AccessDeniedException`; `JsonAccessDeniedHandler` in chain | `AccessPolicy`, `@PreAuthorize` |
| `NOT_FOUND` | 404 | `NotFoundException` | `AccessPolicy` (non-member), services |
| `EMAIL_TAKEN` | 409 | `ConflictException(EMAIL_TAKEN)` | `AuthService` |
| `DUPLICATE_WATCH` | 409 | `ConflictException(DUPLICATE_WATCH)` | `WatchService` |
| `QUOTA_EXCEEDED` | 409 | `ConflictException(QUOTA_EXCEEDED)` | org/watch/source quotas |
| `INVALID_TRANSITION` | 409 | `ConflictException(INVALID_TRANSITION)` | `ImpactReviewService` |
| `ORG_NOT_EMPTY` | 409 | `ConflictException(ORG_NOT_EMPTY)` | `OrganizationService` |
| `TICK_IN_PROGRESS` | 409 | `ConflictException(TICK_IN_PROGRESS)` | `SchedulerController` |
| `INTERVAL_TOO_SHORT` | 422 | `UnprocessableException(INTERVAL_TOO_SHORT)` | `WatchService` |
| `SOURCE_FETCH_BLOCKED` | 422 | `source.application.SourceFetchBlockedException` | `SafeFetchProxy` |
| `SOURCE_FETCH_FAILED` | 422 | `source.application.SourceFetchFailedException` (`retryable` true for retryable causes) | fetcher |
| `ROBOTS_DISALLOWED` | 422 | `source.application.RobotsDisallowedException` | `RobotsPolicyService` |
| `UNSUPPORTED_CONTENT` | 422 | `content.application.UnsupportedContentException` | `SourceAdapterFactory` |
| `SECTION_NOT_FOUND` | 422 | `UnprocessableException(SECTION_NOT_FOUND)` | `DependencyService` |
| `CHECK_RATE_LIMITED` | 429 | `RateLimitedException(CHECK_RATE_LIMITED, retryAfter)` + `Retry-After` | manual check (FR-WCH-5) |
| `RATE_LIMITED` | 429 | `RateLimitedException(RATE_LIMITED, retryAfter)` + `Retry-After` | login, preview, waitlist, demo login (NFR-SEC-7) |
| `INTERNAL_ERROR` | 500 | any other `Exception` (logged with stack trace) | handler fallback |

The PRD lists this as the *initial* set; new codes need a PRD change first. `ErrorCode` is an enum in `shared/api/ErrorCode.java`; exceptions carry an `ErrorCode`, never a free-text code.

## Transactions
- `@Transactional` on application services only; `readOnly = true` for queries. `spring.jpa.open-in-view=false`.
- **Never** hold a transaction across outbound HTTP (fetch, robots, webhook). Pattern: short claim tx → fetch → record tx → dispatch (see [architecture.md](architecture.md#execution-model-dec-08-prd-113)).
- The single core-loop transaction is `monitor/application/CheckOutcomeService.record(...)`: version + sections → change event + evidence → impacts → notifications → outbox rows (§9.1).
- Uniqueness races are settled by DB constraints; catch `DataIntegrityViolationException` and map to the right 409 or treat as idempotent no-op (change-event fingerprint, NFR-REL-1).
- No `@Async`, no `CompletableFuture` for business work, no `@TransactionalEventListener` that defers work beyond the request (DEC-08).

## Method security
- `@EnableMethodSecurity` in `identity/infrastructure/SecurityConfig`.
- Every write declares its role on the service method (§6.3 layer 2) and its resource check (layer 3):

```java
@PreAuthorize("hasRole('MAINTAINER') and @access.canWriteProject(#projectId)")
public WatchResponse create(UUID projectId, CreateWatchRequest req) { ... }

@PreAuthorize("hasAnyRole('MAINTAINER','REVIEWER') and @access.canReadImpact(#impactId)")
public ImpactResponse review(UUID impactId, ReviewImpactRequest req) { ... }
```

- Reads use `@access.canRead…` (non-member → 404). Owner-only org actions use `@access.requireOwner(#orgId)`. Admin services use `hasRole('ADMIN')`.
- Compile with `-parameters` (the Spring Boot parent does) so `#projectId` resolves.

## Configuration (`sourceripple.*`, PRD §13)
- One `@ConfigurationProperties("sourceripple")` record tree in `shared/infrastructure/SourceRippleProperties`, validated with `@Validated`. Groups: `publicBaseUrl`, `encryptionKey`, `admin`, `metrics`, `tick`, `scheduler` (`mode`, `claimBatch`, `lease`, `tickBudget`), `watch` (`defaultIntervalHours`, `minIntervalHours`, `perProject`, `perOrganization`), `organizationsPerUser`, `sources.maxActive`, `fetch` (timeouts, caps, redirects, concurrency, `perHostGap`, `allowPrivateTargets`, ports), `rateLimit` (preview, manualCheck, waitlist, demoLogin, login), `webhook` (`timeout`, `attempts`), `retention.versionsPerSource`, `demo` (`enabled`, `webhookSecret`, `userPassword`).
- Env vars map by relaxed binding (`SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS` → `sourceripple.fetch.allow-private-targets`). Defaults equal §13; full table in [local-setup-and-environments.md](../operations/local-setup-and-environments.md#environment-variables).
- Secrets have no defaults in `application.yml`; dev values live only in `.env.example` and `application-local.yml`.

## Logging and request IDs (FR-API-3, NFR-OBS-1)
- Structured JSON logging in `prod` (Spring Boot structured logging); readable console locally.
- `RequestIdFilter` puts `requestId` in MDC; add `userId` when authenticated and `sourceId` in monitor code.
- Never log page content, passwords, secrets, tokens, `Authorization`, `Cookie`, `X-SR-Signature`, `X-SR-Tick-Token` (NFR-SEC-4).

## Time
Inject `java.time.Clock` (bean in `shared/infrastructure/TimeConfig`); never call `Instant.now()` directly in services. Tests use `Clock.fixed` or a mutable test clock.

## Flyway migration plan
Files live in area folders under `backend/src/main/resources/db/migration/` (Flyway scans sub-folders of `classpath:db/migration` recursively; the 6-files rule applies here too), named `V<n>__<snake_case>.sql`. Version numbers are global across folders. Merged migrations are immutable (§9, NFR-MAINT-1); fixes go in a new migration.

| Version | File (folder/) | Phase / owner | Tables and indexes (PRD §9.1) |
|---|---|---|---|
| V1 | `core/V1__identity.sql` | 2 / Ajoy | `roles` (+ seed ADMIN, MAINTAINER, REVIEWER), `users`, `user_roles` |
| V2 | `core/V2__spring_session.sql` | 2 / Ajoy | `spring_session`, `spring_session_attributes` |
| V3 | `core/V3__workspace.sql` | 2 / Ajoy | `organizations`, `organization_members`, `projects` |
| V4 | `monitoring/V4__sources_and_watches.sql` | 5 / Ajoy | `sources`, `watches`, `source_versions`, `source_sections`, `monitor_runs` + 3 indexes |
| V5 | `core/V5__app_settings.sql` | 5 / Lead | `app_settings` |
| V6 | `monitoring/V6__changes_dependencies_impacts.sql` | 6 / Ajoy | `change_events`, `change_event_sections`, `dependencies`, `impacts` + 3 indexes |
| V7 | `integrations/V7__notifications_and_webhooks.sql` | 8 / Imrul | `notifications`, `webhook_endpoints`, `webhook_deliveries` + 2 indexes |
| V8 | `integrations/V8__api_keys.sql` | 8 / Lead | `api_keys` |
| V9 | `marketing/V9__waitlist.sql` | 9 / Lead | `waitlist_signups` |

Total: 20 domain tables + `app_settings` + 2 session tables. Numbers are a plan, not a reservation: when two PRs race, the second to merge renumbers its **unmerged** file to the next free number. After pulling a renumbered migration locally, reset the dev DB (`docker compose down -v`).
