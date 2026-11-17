# Phase 2 · Data model, identity and access

## Goal
Users can register, log in, log out and read `/auth/me` with server sessions in PostgreSQL. Three roles and organization membership exist, and the three enforcement layers (URL rules, method security, `AccessPolicy`) are in place and proven by tests.

## People
- **Owner:** Ajoy
- **Reviewers:** Lead and Imrul, alternating (Lead first)

## Prerequisites
- Phase 1 exit criteria met (protected branches, CI green, Compose starts app and db).
- Spring Boot line pinned (Q5).

## PRD references
§6 roles, rules R1–R7, matrix §6.2, enforcement §6.3 · FR-AUTH-1..7 · NFR-SEC-2, NFR-SEC-3, NFR-SEC-4 (logging masks) · §9 conventions and tables 1–6 · §10.1–§10.2 (status codes, envelope) · FR-API-1/2 (minimal, completed in Phase 3) · FR-ADM-1 · NFR-SEC-7 (login limit) · TST-R1..R5.

## Ordered task checklist

### A. Schema (see [backend-conventions.md](../../guides/engineering/backend-conventions.md#flyway-migration-plan))
- [ ] `db/migration/core/V1__identity.sql`: `roles` (seed ADMIN, MAINTAINER, REVIEWER), `users` (email unique on `lower(email)`), `user_roles` composite PK (§9.1 #1–3).
- [ ] `core/V2__spring_session.sql`: Spring Session JDBC PostgreSQL schema copied from the `spring-session-jdbc` jar; set `spring.session.jdbc.initialize-schema: never` (§9.1).
- [ ] `core/V3__workspace.sql`: `organizations`, `organization_members` (composite PK, `added_at`), `projects` with unique `(organization_id, slug)` (§9.1 #4–6). Projects exist now because `AccessPolicy` resolves project → organization.

### B. Entities and repositories
- [ ] `identity/domain/User.java` with `@ManyToMany Set<Role> roles` via `user_roles` (R1, §9.1 relationship coverage); `Role.java`, `RoleName.java` enum; `UserRepository` (`findByEmailIgnoreCase`), `RoleRepository`.
- [ ] `workspace/domain/Organization.java` with owner (`@ManyToOne User`) and `@ManyToMany Set<User> members` via `organization_members`; `Project.java` (`@ManyToOne Organization`); `OrganizationRepository`, `ProjectRepository`. Services come in Phase 3.

### C. Authentication (FR-AUTH, NFR-SEC-2)
- [ ] `identity/application/AuthService.java`: `register` (normalize email, BCrypt hash strength 10–12, assign `MAINTAINER` or `REVIEWER`; `accountType=ADMIN` → `ROLE_NOT_ALLOWED` 400; duplicate → `EMAIL_TAKEN` 409; password ≥ 10 chars) (FR-AUTH-1, R2).
- [ ] `identity/api/AuthController.java`: `POST /api/v1/auth/register` (201 + `Location`), `POST /login` (200 `{user, roles}`), `POST /logout` (204), `GET /me` (profile, roles, organizations) (FR-AUTH-1..4). DTOs in `identity/api/dto/` (`RegisterRequest`, `LoginRequest`, `MeResponse`, `UserResponse`) with Bean Validation.
- [ ] Login: authenticate with `AuthenticationManager`, **rotate the session ID** (`request.changeSessionId()` or a session-fixation strategy), then **save the `SecurityContext` explicitly** through `HttpSessionSecurityContextRepository` (NFR-SEC-2). Invalid credentials → 401 generic message; disabled → 401 `ACCOUNT_DISABLED` (FR-AUTH-2).
- [ ] `identity/infrastructure/SessionConfig.java`: Spring Session JDBC; cookie HttpOnly, `Secure` in `prod`, `SameSite=Lax`; idle timeout 8 h (FR-AUTH-5).
- [ ] Logout via Spring Security `logout` at `/api/v1/auth/logout` with a 204 success handler; delete the `SESSION` cookie (FR-AUTH-3).
- [ ] `identity/application/BootstrapAdminInitializer.java` (`ApplicationRunner`): if no ADMIN exists and both `SOURCERIPPLE_ADMIN_EMAIL`/`_PASSWORD` are set, create one; otherwise log a warning (R3).
- [ ] `identity/application/UserAccountService.java`: `setEnabled` deletes the user's sessions via `FindByIndexNameSessionRepository` (FR-AUTH-7); `replaceRoles` enforces R4 (used by the FR-ADM-1 API in section G).

### D. Security filter chain and CSRF (§6.3 layer 1, FR-AUTH-6)
- [ ] `identity/infrastructure/SecurityConfig.java`: session chain with the URL rules of §6.3 (`/api/v1/auth/register|login|demo-login`, `/api/v1/public/**`, `/actuator/health|info`, `/v3/api-docs/**`, `/swagger-ui/**`, SPA assets and marketing routes public; `/api/v1/admin/**` `hasRole('ADMIN')`; rest authenticated). Tick, metrics and API-key chains are added in Phases 5, 9 and 8 with `@Order` before this one.
- [ ] CSRF: SPA cookie-to-header (`XSRF-TOKEN` / `X-XSRF-TOKEN`) using the documented setup for the pinned Spring Security version (built-in SPA option, or `CookieCsrfTokenRepository.withHttpOnlyFalse()` + SPA request handler + `CsrfCookieFilter`). CORS disabled (NFR-SEC-8).
- [ ] `JsonAuthenticationEntryPoint` (401 `UNAUTHENTICATED`) and `JsonAccessDeniedHandler` (403 `FORBIDDEN`) writing the §10.2 envelope; no login page or redirects.
- [ ] `DatabaseUserDetailsService`: authorities `ROLE_<name>`; disabled users rejected.

### E. Authorization layers 2 and 3 (§6.3, NFR-SEC-3)
- [ ] `@EnableMethodSecurity`; convention for every write method in [backend-conventions.md](../../guides/engineering/backend-conventions.md#method-security).
- [ ] `identity/application/AccessPolicy.java`, bean name `access`: `canReadOrganization`, `requireOwner`, `canReadProject`, `canWriteProject`, and generic `resolve(ResourceType, id)`; non-member → `NotFoundException` (404), member without role → `ForbiddenException` (403); never trusts a client `organizationId`.
- [ ] Ports in `identity/application/`: `MembershipLookup` (implemented by `workspace/application/WorkspaceMembershipLookup`) and `OrganizationResolver` (one bean per resource type; `ProjectOrganizationResolver` now, watch/impact/change/webhook/api-key resolvers added in their phases). See [architecture.md](../../guides/engineering/architecture.md#cross-module-calls).

### F. Minimal shared error handling (completed in Phase 3)
- [ ] `shared/api/ApiError.java` (envelope §10.2), `shared/api/ErrorCode.java` (all codes from §10.2), `shared/api/GlobalExceptionHandler.java` handling validation (400 `VALIDATION_FAILED` + `fieldErrors`), `NotFoundException`, `ForbiddenException`, `ConflictException`, `BadRequestException`; never returns stack traces (FR-API-1/2).
- [ ] Log masking for `password`, `secret`, `Authorization`, `Cookie` (NFR-SEC-4).

### G. Users and roles admin API (FR-ADM-1) and CI smoke
- [ ] `admin/application/AdminUserService.java` + `admin/api/AdminController.java`: `GET /api/v1/admin/users` (list, search, paginated), `PATCH /admin/users/{id}` (enabled, via `UserAccountService.setEnabled`), `PUT /admin/users/{id}/roles` (R4: can't drop own ADMIN, ≥ 1 enabled ADMIN remains). Phase 8 adds FR-ADM-2..4.
- [ ] Login rate limit: 10 attempts per 15 min per IP and per email → 429 `RATE_LIMITED` with `Retry-After` (NFR-SEC-7), using `shared/infrastructure/RateLimiter`.
- [ ] `ci.yml` `docker` job: add curl smoke for register → login (with CSRF cookie) → `/auth/me` (§15.3).

## Suggested PR breakdown
| # | Branch | Scope | Reviewer |
|---|---|---|---|
| 1 | `feature/p2-identity-schema` | A, B | Lead |
| 2 | `feature/p2-auth-endpoints` | C, F, U1–U3, I1, I2 | Imrul |
| 3 | `feature/p2-security-chain` | D, I3, CI smoke | Lead |
| 4 | `feature/p2-access-policy` | E, U8 | Imrul |
| 5 | `feature/p2-admin-users` | G (FR-ADM-1, login limit), I5 | Lead |

## Tests to write
| ID | Class | Intent |
|---|---|---|
| U1 | `identity/application/AuthServiceTest` | Register hashes with BCrypt (verify encoder called, raw never stored) and assigns the chosen role |
| U2 | `AuthServiceTest` | `accountType=ADMIN` → `ROLE_NOT_ALLOWED` |
| U3 | `AuthServiceTest` | `Foo@x.io` after `foo@x.io` → `EMAIL_TAKEN` |
| U8 | `identity/application/AccessPolicyTest` | Non-member → NotFound; member without role → Forbidden; member with role passes |
| I1 | `identity/api/AuthFlowIT` | Register → login → `/me` 200 → logout 204 → `/me` 401 |
| I2 | `identity/api/AuthValidationIT` | Bad password → 401; invalid register body → 400 with `fieldErrors` |
| I3 | `identity/api/UnauthenticatedAccessIT` | No session on a protected endpoint → 401 envelope |
| I5 | `admin/api/AdminAccessIT` | MAINTAINER `GET /admin/users` → 403; ADMIN → 200 |

I4 and I6 are written in Phases 5 and 6, when watches and impacts exist (PRD §21.2).

Recipes: [testing.md](../../guides/engineering/testing.md).

## Exit criteria
- [ ] U1–U3, U8, I1–I3, I5 green in CI.
- [ ] Admin can list users, change roles (R4 guard holds) and disable a user, whose session ends on the next request (FR-AUTH-7).
- [ ] Passwords in `users.password_hash` start with `$2` (BCrypt); none appear in logs.
- [ ] Session survives an app restart in Compose (stored in `spring_session`).
- [ ] CSRF: a state-changing request without `X-XSRF-TOKEN` → 403.
- [ ] Bootstrap admin created from env vars; no hard-coded credentials.

## Evidence to capture (§19.1)
- Database screenshot of BCrypt hashes (`SELECT email, left(password_hash, 7) FROM users`).
- CI run showing I1–I5 passing; Swagger UI is Phase 3.

## Risks and gotchas
- **Spring Security 6+ does not save the context for custom JSON login.** Without `securityContextRepository.saveContext(...)`, the next request is anonymous (NFR-SEC-2).
- **Session fixation** isn't applied automatically to a custom endpoint. Rotate the ID yourself.
- **`hasRole('ADMIN')` expects authority `ROLE_ADMIN`.** Store `ADMIN` in `roles.name`, prefix when building authorities.
- **Filter-level errors skip `@RestControllerAdvice`.** 401/403 from the chain must be written by the entry point and denied handler.
- **CSRF token is deferred** in Spring Security 6+: unless something reads it, no `XSRF-TOKEN` cookie is set. The SPA's first `GET /auth/me` must trigger it (`CsrfCookieFilter` or the built-in SPA config).
- **MockMvc + Spring Session JDBC:** carry the `SESSION` cookie between requests manually in I1; use `.with(csrf())` for writes.
- **404 vs 403 order.** Role annotations run before membership checks, so a non-member without the role gets 403; that leaks nothing because it doesn't depend on the resource. Reads must return 404 for non-members (I6).
- **Login limit keys** are both the client IP and the normalized email (NFR-SEC-7); behind a proxy the IP comes from trusted forwarded headers (Phase 9 sets this for prod).
