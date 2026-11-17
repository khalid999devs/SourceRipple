# Testing guide

Implements PRD §14 (TST-R1..R7). Test IDs and owners are tracked in [PHASE_PLAN.md](../../PHASE_PLAN.md#test-id-ownership-tracker-prd-14).

## Two suites
| Suite | Name pattern | Runner | Context | Count (PRD) |
|---|---|---|---|---|
| Unit (service layer) | `*Test` | Surefire (`mvn test`) | **No Spring context.** JUnit 5 + Mockito + AssertJ | ≥ 20 required, 31 planned (U1–U31) |
| Integration (controller layer) | `*IT` | Failsafe (`mvn verify`) | `@SpringBootTest` + `@AutoConfigureMockMvc` + MockMvc + Testcontainers PostgreSQL | ≥ 10 required, 17 planned (I1–I17) |

`./mvnw verify` runs both. It runs in CI on every PR and every push to `develop`/`main`; red blocks merge (TST-R3). Test IDs are **not** written in source: the ID → class mapping lives in the tracker and in PR descriptions. Name test methods after the behavior (`flagsOnlyDependenciesOnChangedSection`).

## Unit tests (service layer)
- Plain JUnit 5 with `@ExtendWith(MockitoExtension.class)`; mock repositories and ports with `@Mock`, build the service with `@InjectMocks` or the constructor.
- Test real logic: transitions, classification, matching, hashing. Don't test getters or Spring wiring.
- Fixed time: pass `Clock.fixed(...)`; seeded `Random` for jitter (U30).
- Fixtures from `src/test/resources/fixtures/` (HTML/RSS); load with a small `Fixtures.read("payments-v2.html")` helper in `io.sourceripple.support`.
- Fetcher-level unit tests (U13) may use `MockWebServer` without Spring.

```java
@ExtendWith(MockitoExtension.class)
class SafeFetchProxyTest {
  @Mock WebFetcher delegate;
  SafeFetchProxy proxy;

  @BeforeEach
  void setUp() { proxy = TestFetch.proxyBlockingPrivateTargets(delegate); }

  @Test
  void blocksMetadataAddressWithoutCallingDelegate() {
    assertThatThrownBy(() -> proxy.fetch(FetchRequest.get("http://169.254.169.254/latest")))
        .isInstanceOf(SourceFetchBlockedException.class);
    verify(delegate, never()).fetch(any());
  }
}
```

## Integration tests (controller layer)
Shared base in `backend/src/test/java/io/sourceripple/support/`:

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
  @Bean @ServiceConnection
  PostgreSQLContainer<?> postgres() { return new PostgreSQLContainer<>("postgres:17-alpine"); }
}

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
  @Autowired protected MockMvc mvc;
  // helpers: registerAndLogin(role) -> Cookie SESSION, csrf(), json(...)
}
```
- Every IT extends `AbstractIntegrationTest`, so the Spring context (and the container) is cached and shared. Only tests that need different properties (I13, I9) create their own context.
- Flyway runs the real migrations against the container; `ddl-auto=validate` catches entity/schema drift.
- Writes use `.with(csrf())`. Session flows carry the `SESSION` cookie between requests (Spring Session JDBC).
- Use `@MockitoBean` (never the deprecated `@MockBean`) when a Spring test must mock a bean (TST-R5), e.g. a `Clock`.
- Each test creates its own users/orgs with unique emails; don't depend on test order. Clean tables only if a test truly needs an empty DB.

## External HTTP (TST-R4)
- **Never** call the real internet. Source pages and webhook receivers use `okhttp3.mockwebserver.MockWebServer` or the demo fixture controller.
- `application-test.yml` sets `sourceripple.fetch.allow-private-targets: true` (MockWebServer listens on 127.0.0.1). This flag is only true in `local` and `test`; `prod` refuses to start with it (NFR-SEC-1.7).
- I13 proves the guard with the flag **off**: `@TestPropertySource(properties = "sourceripple.fetch.allow-private-targets=false")`, then assert 422 `SOURCE_FETCH_BLOCKED` and `server.getRequestCount() == 0`.
- Webhook ITs (I12): enqueue `MockResponse().setResponseCode(500)` then `200`; read both requests with `takeRequest()`, assert the same `X-SR-Event-Id` and verify `X-SR-Signature` with the test secret. Trigger the retry by advancing the injected clock and calling the due-delivery path; never `Thread.sleep`.

## Golden flow IT
`GoldenFlowIT` (I9) fetches the demo fixture over real HTTP, so it needs a listening port and a base URL that points at it:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class GoldenFlowIT extends AbstractIntegrationTest {
  static final int PORT = TestSocketUtils.findAvailableTcpPort();
  @DynamicPropertySource static void props(DynamicPropertyRegistry r) {
    r.add("server.port", () -> PORT);
    r.add("sourceripple.public-base-url", () -> "http://localhost:" + PORT);
    r.add("sourceripple.demo.enabled", () -> true);
  }
}
```
Steps: seeded data (or test setup) per Appendix A → `PUT /admin/demo/fixture v2` as ADMIN → `POST /watches/{paymentsWatch}/check` as maintainer → assert 1 change event with sections `[payments/parameters]`, 2 impacts (MATCHED, NEW) for payment-service and checkout-module, 0 for Fitness → re-check → `UNCHANGED`/`NOT_MODIFIED`. Phase 8 adds: one delivery DELIVERED and the receiver event `signatureValid=true`.

## Surefire / Failsafe / JaCoCo (pom.xml)
- Surefire includes `**/*Test.java`; Failsafe includes `**/*IT.java` with goals `integration-test` and `verify`.
- JaCoCo `prepare-agent` for both suites (`prepare-agent` + `prepare-agent-integration`), `report` + `report-integration` (or `merge`) in `verify`. CI uploads `target/site/jacoco*/`, `target/surefire-reports/` and `target/failsafe-reports/` (TST-R6).
- Soft target ≥ 70% line coverage for services in `monitor`, `diff`, `impact`, `notify`, `identity`. Don't add coverage gates that fail the build; report it.
- CI publishes a test summary with unit and IT counts (§15.3) from the Surefire/Failsafe XML.

## Running locally
| Command (in `backend/`) | Runs |
|---|---|
| `./mvnw test` | Unit tests only (fast, no Docker) |
| `./mvnw verify` | Unit + integration (needs Docker for Testcontainers) |
| `./mvnw -Dit.test=GoldenFlowIT verify` | One IT |
| `./mvnw -Dtest=ImpactServiceTest test` | One unit class |

## Failing-then-fixed CI evidence (TST-R7)
Owned by the Lead in Phase 10; the demo shows it at 9:00–11:00 (§20.2).
1. Branch `fix/p10-impact-assertion-demo` from `develop`.
2. Commit 1: `test(impact): assert three impacts in golden flow` — change the I9 expectation from 2 to 3 impacts (intentionally wrong). Push and open a PR into `develop`.
3. CI `backend` goes red; the summary names `GoldenFlowIT`. Copy the run URL.
4. Commit 2: `fix(impact): restore expected impact count` — revert the expectation. Push; CI goes green. Copy the run URL.
5. Get the approval and squash-merge (the squash keeps the history clean; the PR keeps both runs). Put both URLs in the PR description and in the README testing section.
Never produce the red run by pushing to a protected branch.

## Frontend
The gate is lint, typecheck, build (§14). Vitest + Testing Library and a Playwright smoke test are P2; if added, put tests next to components as `*.test.tsx` and keep them out of the CI gate until agreed.
