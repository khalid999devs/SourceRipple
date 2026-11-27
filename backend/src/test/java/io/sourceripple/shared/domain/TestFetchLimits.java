package io.sourceripple.shared.domain;

import java.time.Duration;
import java.util.Set;

public final class TestFetchLimits {

    private TestFetchLimits() {}

    public static GlobalFetchLimits.Builder builder() {
        return GlobalFetchLimits.builder()
                .timeouts(Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(15))
                .byteCaps(2L * 1024 * 1024, 5L * 1024 * 1024)
                .maxRedirects(3)
                .concurrency(2)
                .perHostGap(Duration.ofSeconds(10))
                .allowedPorts(Set.of(80, 443, 8080, 8443))
                .allowedContentTypes(Set.of(
                        "text/html",
                        "application/xhtml+xml",
                        "application/rss+xml",
                        "application/atom+xml",
                        "application/xml",
                        "text/xml"))
                .largePageBytes(1024L * 1024);
    }

    public static GlobalFetchLimits build(GlobalFetchLimits.Builder builder) {
        return GlobalFetchLimits.detached(builder);
    }

    public static GlobalFetchLimits defaults() {
        return GlobalFetchLimits.detached(builder());
    }
}
