package io.sourceripple.source.infrastructure.fetch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sourceripple.content.application.UnsupportedContentException;
import io.sourceripple.shared.domain.GlobalFetchLimits;
import io.sourceripple.shared.domain.TestFetchLimits;
import io.sourceripple.source.application.fetch.FetchPurpose;
import io.sourceripple.source.application.fetch.FetchRequest;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.failure.SourceFetchBlockedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.AddressPolicy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Dns;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OkHttpWebFetcherTest {

    private final MockWebServer server = new MockWebServer();

    @BeforeEach
    void start() throws IOException {
        server.start(0);
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void bodyOverTheCompressedCapAbortsWhileStreaming() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "text/html")
                .chunkedBody("x".repeat(4096), 512)
                .build());

        assertThatThrownBy(() -> fetcher(limits(1024, 1024 * 1024)).fetch(get("/big")))
                .isInstanceOf(SourceFetchFailedException.class)
                .hasMessageContaining("larger");
    }

    @Test
    void declaredLengthOverTheCapAbortsBeforeReading() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "text/html")
                .body("y".repeat(2048))
                .build());

        assertThatThrownBy(() -> fetcher(limits(1024, 1024 * 1024)).fetch(get("/declared")))
                .isInstanceOf(SourceFetchFailedException.class);
    }

    @Test
    void gzipBombStopsAtTheDecodedCap() throws IOException {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "text/html")
                .addHeader("Content-Encoding", "gzip")
                .body(gzip(new byte[200 * 1024]))
                .build());

        assertThatThrownBy(() -> fetcher(limits(1024 * 1024, 10 * 1024)).fetch(get("/bomb")))
                .isInstanceOf(SourceFetchFailedException.class);
    }

    @Test
    void decodesGzipWithinTheCaps() throws IOException {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "text/html; charset=utf-8")
                .addHeader("Content-Encoding", "gzip")
                .body(gzip("<h1>Payments</h1>".getBytes()))
                .build());

        FetchResult result = fetcher(TestFetchLimits.defaults()).fetch(get("/gzip"));

        assertThat(new String(result.body(), result.charset())).isEqualTo("<h1>Payments</h1>");
        assertThat(result.mediaType()).isEqualTo("text/html");
    }

    @Test
    void rejectsAContentTypeOutsideTheAllowList() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "image/png")
                .body("png")
                .build());

        assertThatThrownBy(() -> fetcher(TestFetchLimits.defaults()).fetch(get("/logo.png")))
                .isInstanceOf(UnsupportedContentException.class);
    }

    @Test
    void sendsTheBotUserAgentAndConditionalHeadersAndAccepts304() throws InterruptedException {
        server.enqueue(new MockResponse.Builder().code(304).build());

        FetchResult result = fetcher(TestFetchLimits.defaults())
                .fetch(FetchRequest.conditional(
                        server.url("/page").toString(),
                        "\"v1\"",
                        "Tue, 17 Nov 2026 10:00:00 GMT",
                        FetchPurpose.SCHEDULED));

        RecordedRequest recorded = server.takeRequest();
        assertThat(result.isNotModified()).isTrue();
        assertThat(recorded.getHeaders().get("User-Agent")).startsWith("SourceRippleBot/");
        assertThat(recorded.getHeaders().get("If-None-Match")).isEqualTo("\"v1\"");
        assertThat(recorded.getHeaders().get("If-Modified-Since")).isEqualTo("Tue, 17 Nov 2026 10:00:00 GMT");
    }

    @Test
    void returnsRedirectsWithoutFollowingThem() {
        server.enqueue(new MockResponse.Builder().code(302).addHeader("Location", "/next").build());

        FetchResult result = fetcher(TestFetchLimits.defaults()).fetch(get("/start"));

        assertThat(result.isRedirect()).isTrue();
        assertThat(result.location()).isEqualTo("/next");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void guardedDnsBlocksLocalhostWhenPrivateTargetsAreOff() {
        AddressPolicy strict = new AddressPolicy(false);
        OkHttpWebFetcher guarded = new OkHttpWebFetcher(
                TestFetchLimits.defaults(),
                new GuardedDns(strict, Dns.SYSTEM),
                new RemoteAddressInterceptor(strict),
                "SourceRippleBot/test");

        assertThatThrownBy(() -> guarded.fetch(FetchRequest.get(
                        "http://localhost:" + server.getPort() + "/", FetchPurpose.PREVIEW)))
                .isInstanceOf(SourceFetchBlockedException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void aRequestTimeoutCutsASlowResponseShort() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "text/html")
                .body("<p>late</p>")
                .headersDelay(2, TimeUnit.SECONDS)
                .build());

        FetchRequest request = get("/slow").withTimeout(Duration.ofMillis(200));

        assertThatThrownBy(() -> fetcher(TestFetchLimits.defaults()).fetch(request))
                .isInstanceOfSatisfying(SourceFetchFailedException.class, ex -> assertThat(ex.retryable()).isTrue());
    }

    private FetchRequest get(String path) {
        return FetchRequest.get(server.url(path).toString(), FetchPurpose.PREVIEW);
    }

    private static OkHttpWebFetcher fetcher(GlobalFetchLimits limits) {
        AddressPolicy permissive = new AddressPolicy(true);
        return new OkHttpWebFetcher(
                limits,
                new GuardedDns(permissive, Dns.SYSTEM),
                new RemoteAddressInterceptor(permissive),
                FetchConfig.userAgent(null));
    }

    private static GlobalFetchLimits limits(long compressedCap, long decodedCap) {
        return TestFetchLimits.build(TestFetchLimits.builder().byteCaps(compressedCap, decodedCap));
    }

    private static Buffer gzip(byte[] plain) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(plain);
        }
        return new Buffer().write(bytes.toByteArray());
    }
}
