# Phase 4 · Safe fetching and extraction

## Goal
Every outgoing request goes through one SSRF-safe, size-limited, robots-aware fetcher, and fetched HTML or RSS becomes a `NormalizedDocument` of stable, hashed sections. A maintainer can preview a URL and see its sections. This phase delivers five of the six design patterns: Adapter, Factory, Proxy, Flyweight and Singleton.

## People
- **Owner:** Ajoy
- **Reviewers:** Lead and Imrul, alternating (Lead reviews the Proxy/SSRF PRs; security is the risk here)

## Prerequisites
- Phase 3 exit criteria met (projects exist, error envelope, request IDs).

## PRD references
NFR-SEC-1 (all 7 points) · FR-EXT-1..4, FR-EXT-9 (detection used by preview) · FR-WCH-1 · FR-MON-3 (User-Agent, conditional headers) · FR-MON-7 (robots) · §11.5 Adapter, Factory, Proxy, Flyweight, Singleton · §13 configuration defaults · NFR-SEC-7 (preview rate limit) · DEC-06.

## Ordered task checklist

### A. Configuration and the Singleton
- [ ] `shared/infrastructure/SourceRippleProperties.java` (`@ConfigurationProperties("sourceripple")`) with a `fetch` group: timeouts 5/10/15 s, caps 2 MB / 5 MB, redirects 3, concurrency 2, allowed ports, content types, `allowPrivateTargets` (§13).
- [ ] Startup guard: in profile `prod`, `allowPrivateTargets=true` fails startup with a clear message (NFR-SEC-1.7).
- [ ] `shared/domain/GlobalFetchLimits.java`: GoF Singleton. Immutable, private constructor, built once from properties at startup, `getInstance()`, also exposed as a Spring bean. Read by the proxy, fetcher and (Phase 5) scheduler (§11.5). The relation to Spring singleton beans is explained in `docs/diagrams/pattern-singleton.md` (Phase 10), not in code comments.

### B. URL validation (NFR-SEC-1.1, 1.2)
- [ ] `source/domain/UrlCanonicalizer.java`: allow `http`/`https`; reject userinfo, fragment-only, ports outside 80/443/8080/8443 (configurable); lower-case host, strip default port, drop fragment, keep query.
- [ ] `source/domain/AddressPolicy.java`: forbidden IPv4/IPv6 ranges (loopback, RFC 1918, link-local incl. `169.254.169.254`, CGNAT `100.64/10`, multicast, broadcast, unspecified, ULA `fc00::/7`, `::ffff:` mapped private, documentation ranges). Used for IP literals at parse time and for resolved addresses.

### C. Fetcher and the Proxy (NFR-SEC-1.3–1.6, DEC-06)
- [ ] `source/application/WebFetcher.java` interface: `FetchResult fetch(FetchRequest)`; `FetchRequest{url, etag?, lastModified?, purpose}`; `FetchResult{status, finalUrl, contentType, body, etag, lastModified, bytes, durationMs}`.
- [ ] `source/infrastructure/fetch/OkHttpWebFetcher.java`: OkHttp client with `GuardedDns` (resolve, drop forbidden, fail if none left), `RemoteAddressInterceptor` network interceptor (re-check connected socket address before sending), redirects **off** in OkHttp, streaming byte caps, content-type allow-list, User-Agent `SourceRippleBot/<app version> (+<repo URL>)`, conditional GET headers (FR-MON-3).
- [ ] `source/infrastructure/fetch/SafeFetchProxy.java` `implements WebFetcher`, wrapping `OkHttpWebFetcher`: canonicalize, reject forbidden IP literals, check robots, acquire the concurrency permit, follow redirects manually (≤ 3, each hop re-validated), then delegate. Blocked → `SourceFetchBlockedException` (422 `SOURCE_FETCH_BLOCKED`), **without calling the delegate** (U12).
- [ ] Wire `SafeFetchProxy` as the only `WebFetcher` bean other modules can inject (`@Primary`); the raw OkHttp fetcher is package-private or qualified.

### D. robots.txt (FR-MON-7)
- [ ] `source/application/RobotsPolicyService.java`: fetch `/robots.txt` through the **same** `SafeFetchProxy` path (robots check skipped for the robots request itself), parse with crawler-commons for agent `SourceRippleBot`, cache per host 24 h in memory. Disallow → `ROBOTS_DISALLOWED`.

### E. Extraction (FR-EXT-1..4) — `content` module
- [ ] `content/domain/NormalizedDocument.java`, `NormalizedSection.java` (`key, title, text, sha256`), `content/domain/SourceAdapter.java` interface.
- [ ] `content/domain/ExtractionRuleSet.java` (immutable: removed tags, removed ARIA roles, main-content selectors, heading levels) and `content/application/ExtractionRuleSetPool.java` (Flyweight: `get(signature)` returns a shared instance; default HTML and RSS sets plus per-mode variants) (§11.5).
- [ ] `content/infrastructure/HtmlPageAdapter.java` (Jsoup): cleaning per FR-EXT-2, split at `h1`–`h4`, keys from `id`/anchor else slugified heading path, `-2`/`-3` duplicates, `_intro` (FR-EXT-3); tables as `cell | cell` rows; `pre`/`code` kept.
- [ ] `content/infrastructure/RssFeedAdapter.java` (Jsoup `Parser.xmlParser()`): one section per item, key = GUID or link (FR-EXT-1). Also accepts Atom entries (`application/atom+xml` is allowed by NFR-SEC-1.5).
- [ ] `content/application/SourceAdapterFactory.java`: choose by content type, then sniff the body (`<rss`, `<feed`, `<html`); unknown → `UNSUPPORTED_CONTENT` (§11.5 Factory).
- [ ] `content/domain/TextNormalizer.java`: NFC, collapse whitespace, strip zero-width chars, trim; SHA-256 hex (FR-EXT-4).
- [ ] JS-rendered detection helper (`< 200` chars and a script-mounted root) → warning `LIKELY_JS_RENDERED` (FR-EXT-9); decoded size > 1 MB → warning `LARGE_PAGE` (FR-WCH-1).

### F. Preview endpoint (FR-WCH-1)
- [ ] `source/application/WatchPreviewService.java`: `@PreAuthorize("hasRole('MAINTAINER') and @access.canWriteProject(#projectId)")`; fetch → adapt → `{canonicalUrl, kind, title, sections[{key,title,excerpt}], warnings[]}`; saves nothing.
- [ ] `source/api/WatchController.java` with `POST /api/v1/projects/{projectId}/watches/preview` (the rest of the controller comes in Phase 5).
- [ ] `shared/infrastructure/RateLimiter.java` (Bucket4j or in-memory token bucket): preview 10/min/user → 429 `RATE_LIMITED` with `Retry-After` (NFR-SEC-7).
- [ ] Test fixtures in `src/test/resources/fixtures/`: `payments-v1.html`, `payments-v2.html`, `nav-footer-only-change.html`, `feed-v1.xml`, `feed-v2.xml`, `js-shell.html`.

## Suggested PR breakdown
| # | Branch | Scope | Reviewer |
|---|---|---|---|
| 1 | `feature/p4-url-canonicalizer` | A, B, U11 | Lead |
| 2 | `feature/p4-safe-fetch-proxy` | C, D, U12, U13 | Imrul (+ Lead for security) |
| 3 | `feature/p4-adapters-factory` | E, U14–U16, U28 | Lead |
| 4 | `feature/p4-watch-preview` | F | Imrul |

## Tests to write
| ID | Class | Intent |
|---|---|---|
| U11 | `source/domain/UrlCanonicalizerTest` | Normalizes host/port/fragment; rejects `ftp:`, userinfo, `http://10.0.0.1`, `http://[::1]` |
| U12 | `source/infrastructure/fetch/SafeFetchProxyTest` | Loopback, private and `169.254.169.254` blocked; `verify(delegate, never()).fetch(any())` |
| U13 | `SafeFetchProxyTest` + `OkHttpWebFetcherTest` | Body > cap aborts; `image/png` rejected (MockWebServer, no Spring) |
| U14 | `content/application/SourceAdapterFactoryTest` | `text/html` → HTML; `application/xml` + `<rss` → RSS; sniffing when type is generic |
| U15 | `content/infrastructure/HtmlPageAdapterTest` | Same keys across runs; nav/footer edit leaves section hashes unchanged |
| U16 | `content/infrastructure/RssFeedAdapterTest` | Items → sections keyed by GUID, fallback to link |
| U28 | `content/application/ExtractionRuleSetPoolTest` | Equal signatures → `isSameAs` instance; different → different |

## Exit criteria
- [ ] Preview of the HTML fixture returns sections including `payments/parameters` (or the key derived from its heading id); preview of the RSS fixture returns one section per item.
- [ ] Private, loopback, metadata and IP-literal targets blocked (U11, U12); U11–U16, U28 green. I13 follows in Phase 5.
- [ ] App refuses to start in `prod` with `SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS=true`.

## Evidence to capture
- Swagger "Try it out" of a blocked preview showing the `SOURCE_FETCH_BLOCKED` envelope (demo segment 8:00, §20.2).
- Code links for each pattern class, for the Phase 10 diagrams.

## Risks and gotchas
- **OkHttp skips `Dns` for IP-literal hosts.** Private IP literals must be rejected in `UrlCanonicalizer`/`AddressPolicy` before OkHttp sees them (NFR-SEC-1.3).
- **DNS rebinding.** Only the addresses returned by `GuardedDns` are dialed; the network interceptor re-checks `chain.connection().socket().inetAddress`.
- **Redirects** must be disabled in OkHttp (`followRedirects(false)`, `followSslRedirects(false)`) or hops skip validation.
- **Compressed vs decoded caps.** OkHttp's transparent gzip hides compressed size. Either set `Accept-Encoding` yourself and decompress with a counting, capped stream, or count wire bytes in a network interceptor. Both caps must abort mid-stream (no decompression bombs).
- **Tests run with `allow-private-targets=true`** (MockWebServer is on 127.0.0.1). I13 (Phase 5) overrides it to `false` in its own context.
- **Robots for the robots request** must not recurse.
- **Flyweight keys** must be value-based (normalized signature string or record), or the pool never shares.
- **The Singleton holds config only**, no mutable state; tests build their own via a package-private factory.
- `LARGE_PAGE` warns when the decoded size is over 1 MB (FR-WCH-1); keep the threshold in `sourceripple.fetch`.
